import json
from io import BytesIO

import httpx
from PIL import Image

from app.interpretation.qwen_vision import QwenVisionClient, QwenVisionSettings
from app.schemas.assessment import ReportImage


def test_qwen_vision_settings_default_to_qwen37_flash(monkeypatch) -> None:
    monkeypatch.delenv("QWEN_VISION_MODEL", raising=False)

    settings = QwenVisionSettings.from_env()

    assert settings.model == "qwen3.7-flash-2026-07-15"


def test_qwen_vision_client_sends_text_and_each_report_page_as_multimodal_content() -> None:
    captured: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        if request.method == "GET":
            return httpx.Response(200, request=request, content=b"image-bytes")
        captured["url"] = str(request.url)
        captured["body"] = json.loads(request.content.decode("utf-8"))
        return httpx.Response(
            200,
            request=request,
            json={
                "choices": [
                    {
                        "finish_reason": "stop",
                        "message": {"content": '{"summary":"ok"}'},
                    }
                ]
            },
        )

    settings = QwenVisionSettings(
        enabled=True,
        api_key="test-key",
        workspace_id="",
        base_url="https://example.invalid/compatible-mode/v1",
        model="qwen3.7-flash-2026-07-15",
        timeout_seconds=10,
        max_tokens=2000,
        max_images=50,
    )
    result = QwenVisionClient(settings, transport=httpx.MockTransport(handler)).generate(
        system_prompt="system",
        user_payload={"mode": "VISION_DIRECT_REPORT", "pageFacts": ["IMAGE:PAGE:1"]},
        report_images=[
            ReportImage(
                page=1,
                mimeType="image/jpeg",
                downloadUrl="https://minio.internal/page-1?signature=secret",
            ),
            ReportImage(
                page=2,
                mimeType="image/png",
                downloadUrl="https://minio.internal/page-2?signature=secret",
            ),
        ],
        thinking_enabled=False,
    )

    assert result == '{"summary":"ok"}'
    assert captured["url"] == "https://example.invalid/compatible-mode/v1/chat/completions"
    body = captured["body"]
    assert isinstance(body, dict)
    assert body["model"] == "qwen3.7-flash-2026-07-15"
    assert body["response_format"] == {"type": "json_object"}
    assert body["enable_thinking"] is False
    assert "minio.internal" not in json.dumps(body, ensure_ascii=False)
    user_content = body["messages"][1]["content"]
    assert user_content[0]["type"] == "text"
    assert "VISION_DIRECT_REPORT" in user_content[0]["text"]
    assert [item["image_url"]["url"] for item in user_content[1:]] == [
        "data:image/jpeg;base64,aW1hZ2UtYnl0ZXM=",
        "data:image/png;base64,aW1hZ2UtYnl0ZXM=",
    ]


def test_qwen_vision_client_reencodes_large_images_before_sending() -> None:
    source = Image.effect_noise((1600, 1200), 80).convert("RGB")
    source_bytes = BytesIO()
    source.save(source_bytes, format="JPEG", quality=95)
    original = source_bytes.getvalue()
    captured: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        if request.method == "GET":
            return httpx.Response(200, request=request, content=original)
        captured["body"] = json.loads(request.content.decode("utf-8"))
        return httpx.Response(
            200,
            request=request,
            json={
                "choices": [
                    {
                        "finish_reason": "stop",
                        "message": {"content": '{"summary":"ok"}'},
                    }
                ]
            },
        )

    settings = QwenVisionSettings(
        enabled=True,
        api_key="test-key",
        workspace_id="",
        base_url="https://example.invalid/compatible-mode/v1",
        model="qwen3.7-flash-2026-07-15",
        timeout_seconds=10,
        max_tokens=2000,
        max_images=50,
        max_image_bytes=20_000,
        max_total_image_bytes=20_000,
        max_image_pixels=2_000_000,
    )
    QwenVisionClient(settings, transport=httpx.MockTransport(handler)).generate(
        system_prompt="system",
        user_payload={"mode": "VISION_DIRECT_REPORT"},
        report_images=[
            ReportImage(
                page=1,
                mimeType="image/jpeg",
                downloadUrl="https://minio.internal/page-1?signature=secret",
            )
        ],
        thinking_enabled=False,
    )

    body = captured["body"]
    assert isinstance(body, dict)
    image_url = body["messages"][1]["content"][1]["image_url"]["url"]
    assert image_url.startswith("data:image/jpeg;base64,")
    assert len(image_url) < len("data:image/jpeg;base64,") + len(original) * 4 // 3


def test_qwen_vision_client_retries_without_unsupported_structured_output() -> None:
    post_count = 0
    request_bodies: list[dict[str, object]] = []

    def handler(request: httpx.Request) -> httpx.Response:
        nonlocal post_count
        if request.method == "GET":
            return httpx.Response(200, request=request, content=b"image-bytes")
        post_count += 1
        body = json.loads(request.content.decode("utf-8"))
        request_bodies.append(body)
        if post_count == 1:
            return httpx.Response(
                422,
                request=request,
                json={"code": "invalid_parameter"},
            )
        return httpx.Response(
            200,
            request=request,
            json={
                "choices": [
                    {
                        "finish_reason": "stop",
                        "message": {"content": '{"summary":"ok"}'},
                    }
                ]
            },
        )

    settings = QwenVisionSettings(
        enabled=True,
        api_key="test-key",
        workspace_id="",
        base_url="https://example.invalid/compatible-mode/v1",
        model="qwen3.7-flash-2026-07-15",
        timeout_seconds=10,
        max_tokens=2000,
        max_images=50,
    )
    result = QwenVisionClient(settings, transport=httpx.MockTransport(handler)).generate(
        system_prompt="system",
        user_payload={"mode": "VISION_DIRECT_REPORT"},
        report_images=[
            ReportImage(
                page=1,
                mimeType="image/jpeg",
                downloadUrl="https://minio.internal/page-1?signature=secret",
            )
        ],
        thinking_enabled=False,
    )

    assert result == '{"summary":"ok"}'
    assert post_count == 2
    assert "response_format" in request_bodies[0]
    assert "response_format" not in request_bodies[1]
