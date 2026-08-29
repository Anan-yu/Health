import base64
import json
import logging
import os
from dataclasses import dataclass
from io import BytesIO
from typing import Any

import httpx
from PIL import Image, ImageOps

from app.schemas.assessment import ReportImage


class QwenVisionError(RuntimeError):
    def __init__(
        self,
        message: str,
        *,
        retryable: bool = True,
        status_code: int | None = None,
    ) -> None:
        super().__init__(message)
        self.retryable = retryable
        self.status_code = status_code


logger = logging.getLogger(__name__)


def _env_bool(name: str, default: bool = False) -> bool:
    value = os.getenv(name)
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


@dataclass(frozen=True)
class QwenVisionSettings:
    enabled: bool
    api_key: str
    workspace_id: str
    base_url: str
    model: str
    timeout_seconds: float
    max_tokens: int
    max_images: int
    max_image_bytes: int = 2_000_000
    max_total_image_bytes: int = 8_000_000
    max_image_pixels: int = 12_000_000
    image_batch_size: int = 4
    image_batch_attempts: int = 2
    image_analysis_max_tokens: int = 8_000

    @classmethod
    def from_env(cls) -> "QwenVisionSettings":
        return cls(
            enabled=_env_bool("QWEN_VISION_ENABLED"),
            api_key=os.getenv("QWEN_VISION_API_KEY", "").strip(),
            workspace_id=os.getenv("QWEN_VISION_WORKSPACE_ID", "").strip(),
            base_url=os.getenv("QWEN_VISION_BASE_URL", "").strip(),
            model=os.getenv("QWEN_VISION_MODEL", "qwen3.8-flash").strip(),
            timeout_seconds=max(10.0, float(os.getenv("QWEN_VISION_TIMEOUT_SECONDS", "120"))),
            max_tokens=min(
                max(1, int(os.getenv("QWEN_VISION_MAX_TOKENS", "16000"))),
                64 * 1024,
            ),
            max_images=max(1, min(int(os.getenv("QWEN_VISION_MAX_IMAGES", "50")), 50)),
            max_image_bytes=max(200_000, int(os.getenv("QWEN_VISION_MAX_IMAGE_BYTES", "2000000"))),
            max_total_image_bytes=max(
                1_000_000,
                int(os.getenv("QWEN_VISION_MAX_TOTAL_IMAGE_BYTES", "8000000")),
            ),
            max_image_pixels=max(
                1_000_000, int(os.getenv("QWEN_VISION_MAX_IMAGE_PIXELS", "12000000"))
            ),
            image_batch_size=max(1, min(int(os.getenv("QWEN_VISION_BATCH_SIZE", "4")), 8)),
            image_batch_attempts=max(1, min(int(os.getenv("QWEN_VISION_BATCH_ATTEMPTS", "2")), 3)),
            image_analysis_max_tokens=min(
                max(1, int(os.getenv("QWEN_VISION_IMAGE_ANALYSIS_MAX_TOKENS", "8000"))),
                32 * 1024,
            ),
        )

    @property
    def configured(self) -> bool:
        return self.enabled and bool(self.api_key) and bool(self.model)

    @property
    def compatible_base_url(self) -> str:
        if self.base_url:
            return self.base_url.rstrip("/")
        if self.workspace_id:
            return f"https://{self.workspace_id}.cn-beijing.maas.aliyuncs.com" "/compatible-mode/v1"
        return "https://dashscope.aliyuncs.com/compatible-mode/v1"

    @property
    def compatible_base_urls(self) -> list[str]:
        if self.base_url:
            return [self.base_url.rstrip("/")]
        public_url = "https://dashscope.aliyuncs.com/compatible-mode/v1"
        if not self.workspace_id:
            return [public_url]
        return [self.compatible_base_url, public_url]


class QwenVisionClient:
    """Call Qwen's multimodal chat endpoint with the report pages as primary evidence."""

    def __init__(
        self,
        settings: QwenVisionSettings | None = None,
        transport: httpx.BaseTransport | None = None,
    ) -> None:
        self.settings = settings or QwenVisionSettings.from_env()
        self.transport = transport

    def generate(
        self,
        *,
        system_prompt: str,
        user_payload: dict[str, Any],
        report_images: list[ReportImage],
        thinking_enabled: bool,
        operation: str = "vision_report",
        max_tokens: int | None = None,
    ) -> str:
        selected_images = report_images[: self.settings.max_images]
        if not selected_images:
            raise QwenVisionError("Qwen vision requires at least one report image")

        payload = self._base_payload(
            system_prompt=system_prompt,
            user_text=json.dumps(user_payload, ensure_ascii=False, default=str),
            thinking_enabled=thinking_enabled,
            max_tokens=max_tokens,
        )
        timeout = httpx.Timeout(
            self.settings.timeout_seconds,
            connect=min(15.0, self.settings.timeout_seconds),
        )
        with httpx.Client(
            timeout=timeout, transport=self.transport, follow_redirects=True
        ) as client:
            downloaded_images = [
                (image, self._download_image(client, image)) for image in selected_images
            ]
            original_bytes = sum(len(content) for _, content in downloaded_images)
            target_bytes = self.settings.max_image_bytes
            if original_bytes > self.settings.max_total_image_bytes:
                target_bytes = max(
                    300_000,
                    min(
                        target_bytes,
                        self.settings.max_total_image_bytes // len(downloaded_images),
                    ),
                )
            prepared_images = [
                self._prepare_image(image, content, target_bytes)
                for image, content in downloaded_images
            ]
            encoded_bytes = sum(size for _, size in prepared_images)
            logger.info(
                "Qwen vision request prepared operation=%s model=%s images=%s "
                "originalBytes=%s encodedBytes=%s responseFormat=json",
                operation,
                self.settings.model,
                len(prepared_images),
                original_bytes,
                encoded_bytes,
            )
            content = payload["messages"][1]["content"]
            assert isinstance(content, list)
            content.extend(
                {
                    "type": "image_url",
                    "image_url": {"url": data_url},
                }
                for data_url, _ in prepared_images
            )
            body = self._request_payload(client, payload, operation)
        return self._extract_content(body)

    def _base_payload(
        self,
        *,
        system_prompt: str,
        user_text: str,
        thinking_enabled: bool,
        max_tokens: int | None,
    ) -> dict[str, Any]:
        payload: dict[str, Any] = {
            "model": self.settings.model,
            "messages": [
                {"role": "system", "content": system_prompt},
                {
                    "role": "user",
                    "content": [{"type": "text", "text": user_text}],
                },
            ],
            "response_format": {"type": "json_object"},
            "temperature": 0.1,
            "max_tokens": max_tokens or self.settings.max_tokens,
        }
        # qwen3.8-flash supports switching thinking mode for image requests. If a
        # provider rejects this optional parameter, _request_payload retries without it.
        payload["enable_thinking"] = thinking_enabled
        return payload

    def _request_payload(
        self,
        client: httpx.Client,
        payload: dict[str, Any],
        operation: str,
    ) -> dict[str, Any]:
        last_error: Exception | None = None
        for base_url in self.settings.compatible_base_urls:
            endpoint = f"{base_url}/chat/completions"
            try:
                response = client.post(
                    endpoint,
                    headers={
                        "Authorization": f"Bearer {self.settings.api_key}",
                        "Content-Type": "application/json",
                    },
                    json=payload,
                )
                if response.status_code in {400, 422} and (
                    "enable_thinking" in payload or "response_format" in payload
                ):
                    retry_payload = dict(payload)
                    retry_payload.pop("enable_thinking", None)
                    retry_payload.pop("response_format", None)
                    response = client.post(
                        endpoint,
                        headers={
                            "Authorization": f"Bearer {self.settings.api_key}",
                            "Content-Type": "application/json",
                        },
                        json=retry_payload,
                    )
                if response.status_code >= 400:
                    provider_code = self._provider_error_code(response)
                    retryable = response.status_code == 429 or response.status_code >= 500
                    last_error = QwenVisionError(
                        f"qwen_http_{response.status_code}",
                        retryable=retryable,
                        status_code=response.status_code,
                    )
                    logger.warning(
                        "Qwen request rejected operation=%s status=%s providerCode=%s "
                        "requestIdPresent=%s retryable=%s",
                        operation,
                        response.status_code,
                        provider_code,
                        bool(response.headers.get("x-request-id")),
                        retryable,
                    )
                    continue
                response.raise_for_status()
                decoded = response.json()
                if not isinstance(decoded, dict):
                    raise ValueError("response is not an object")
                return decoded
            except httpx.TimeoutException as exc:
                last_error = QwenVisionError("qwen_timeout", retryable=True)
                logger.warning(
                    "Qwen request timed out operation=%s timeoutSeconds=%s errorType=%s",
                    operation,
                    self.settings.timeout_seconds,
                    type(exc).__name__,
                )
            except httpx.HTTPError as exc:
                last_error = QwenVisionError("qwen_network_error", retryable=True)
                logger.warning(
                    "Qwen request failed operation=%s errorType=%s",
                    operation,
                    type(exc).__name__,
                )
            except ValueError as exc:
                last_error = QwenVisionError("qwen_invalid_json", retryable=False)
                logger.warning(
                    "Qwen response was not valid JSON operation=%s errorType=%s",
                    operation,
                    type(exc).__name__,
                )
        if isinstance(last_error, QwenVisionError):
            raise last_error
        raise QwenVisionError("qwen_request_failed") from last_error

    @staticmethod
    def _extract_content(body: dict[str, Any]) -> str:
        try:
            choice = body["choices"][0]
            finish_reason = choice.get("finish_reason")
            if finish_reason == "length":
                raise QwenVisionError("qwen_finish_length", retryable=True)
            if finish_reason not in {None, "stop"}:
                raise ValueError(f"finish_reason:{finish_reason}")
            content_value = choice["message"]["content"]
            if isinstance(content_value, list):
                content_value = "\n".join(
                    str(item.get("text", "")) if isinstance(item, dict) else str(item)
                    for item in content_value
                )
            result = str(content_value).strip()
        except QwenVisionError:
            raise
        except (KeyError, IndexError, TypeError, ValueError) as exc:
            raise QwenVisionError("qwen_invalid_response", retryable=False) from exc
        if not result:
            raise QwenVisionError("qwen_empty_response", retryable=True)
        return result

    @staticmethod
    def _download_image(client: httpx.Client, image: ReportImage) -> bytes:
        """Download protected storage content without forwarding its signed URL to Qwen."""

        response = client.get(image.download_url)
        response.raise_for_status()
        if not response.content:
            raise QwenVisionError(f"empty report image page={image.page}", retryable=False)
        return response.content

    def _prepare_image(
        self, image: ReportImage, content: bytes, target_bytes: int
    ) -> tuple[str, int]:
        """Keep report text readable while bounding the multimodal request body."""

        needs_reencode = len(content) > target_bytes
        try:
            with Image.open(BytesIO(content)) as opened:
                width, height = opened.size
                needs_reencode = needs_reencode or width * height > self.settings.max_image_pixels
                if not needs_reencode:
                    return self._data_url(image.mime_type, content), len(content)

                prepared = ImageOps.exif_transpose(opened).convert("RGB")
        except Exception:
            # A valid report image should be handled by Pillow. If an unusual but
            # accepted image encoding cannot be re-encoded, keep the original bytes
            # instead of silently dropping a report page.
            return self._data_url(image.mime_type, content), len(content)

        if prepared.width * prepared.height > self.settings.max_image_pixels:
            scale = (self.settings.max_image_pixels / (prepared.width * prepared.height)) ** 0.5
            prepared.thumbnail(
                (max(1, int(prepared.width * scale)), max(1, int(prepared.height * scale))),
                Image.Resampling.LANCZOS,
            )

        encoded = content
        for quality in (92, 88, 84, 80, 76):
            buffer = BytesIO()
            prepared.save(
                buffer,
                format="JPEG",
                quality=quality,
                optimize=True,
                progressive=True,
            )
            encoded = buffer.getvalue()
            if len(encoded) <= target_bytes:
                break
        return self._data_url("image/jpeg", encoded), len(encoded)

    @staticmethod
    def _data_url(mime_type: str, content: bytes) -> str:
        encoded = base64.b64encode(content).decode("ascii")
        return f"data:{mime_type};base64,{encoded}"

    @staticmethod
    def _provider_error_code(response: httpx.Response) -> str:
        try:
            payload = response.json()
        except ValueError:
            return "none"
        if not isinstance(payload, dict):
            return "none"
        error = payload.get("error")
        if isinstance(error, dict):
            value = error.get("code")
        else:
            value = payload.get("code")
        if value is None:
            return "none"
        return str(value)[:80]
