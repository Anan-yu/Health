import base64
import io
import os
import re
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from html.parser import HTMLParser
from pathlib import Path
from typing import Any, ClassVar

import httpx
from PIL import Image, ImageOps

QWEN_OCR_PROMPT = (
    "你是医疗体检报告的忠实转录引擎。\n"
    "请完整识别本页所有可见内容，尤其是检验表格和体检结论。\n"
    "严格遵守：\n"
    "1. 不得推测、补全、换算、诊断或改写原文；看不清的字段留空。\n"
    "2. 不要把姓名、性别、年龄、电话、日期、医院、科室、病区、住院号、门诊号、"
    "床位号、床号、报告号、条码号、检查号、检验号、样本号、申请单号、仪器型号、"
    "设备编号等个人或报告管理信息输出为‘指标’或‘发现’。\n"
    "3. 只有当页面没有任何检查类别标题、属于纯检验表格时，检验指标才逐行输出："
    "指标<TAB>项目名称<TAB>结果<TAB>单位<TAB>参考下限<TAB>参考上限<TAB>异常标记。\n"
    "4. 体检报告中只要原文存在检查类别标题，该类别下的全部项目（包括数值结果、"
    "非数值结果、影像描述和检查小结）都必须依原文顺序逐行输出："
    "发现<TAB>原检查类别<TAB>原项目名称<TAB>原文完整结果。检查类别和项目名称"
    "必须逐字采用原报告原文，禁止自行改名、合并、拆分、排序或根据项目名称猜测归类；"
    "原文完整结果必须保留数值、单位、参考范围、异常标记和文字结论，不得删减、改写"
    "或补充；同一项目不得再重复输出为‘指标’。不得使用门诊号、床位号等管理字段作为"
    "检查类别或项目。\n"
    "5. 保持原始单位；参考范围只有一侧时，缺失的一侧留空。\n"
    "6. 除上述两种制表符分隔行外不要输出解释、标题、Markdown 表格或代码块。\n"
    "请逐项检查页面左右栏、上下区域，避免漏项。"
)


class QwenOcrError(RuntimeError):
    pass


@dataclass(frozen=True)
class QwenDocument:
    lines: list[str]
    tables: list[list[list[str]]]


class _QwenHtmlParser(HTMLParser):
    """Extract visible text and table geometry from Qwen's document HTML."""

    BLOCK_TAGS: ClassVar[set[str]] = {"p", "li", "h1", "h2", "h3", "h4", "h5", "h6"}

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.lines: list[str] = []
        self.tables: list[list[list[str]]] = []
        self._ignored_depth = 0
        self._block_parts: list[str] | None = None
        self._table: list[list[str]] | None = None
        self._row: list[str] | None = None
        self._cell_parts: list[str] | None = None

    @staticmethod
    def _text(parts: list[str]) -> str:
        return re.sub(r"\s+", " ", "".join(parts)).strip()

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        del attrs
        tag = tag.lower()
        if tag in {"script", "style"}:
            self._ignored_depth += 1
            return
        if self._ignored_depth:
            return
        if tag == "table":
            self._table = []
        elif tag == "tr" and self._table is not None:
            self._row = []
        elif tag in {"td", "th"} and self._row is not None:
            self._cell_parts = []
        elif tag in self.BLOCK_TAGS and self._table is None:
            self._block_parts = []
        elif tag == "br":
            if self._cell_parts is not None:
                self._cell_parts.append(" ")
            elif self._block_parts is not None:
                self._block_parts.append(" ")

    def handle_endtag(self, tag: str) -> None:
        tag = tag.lower()
        if tag in {"script", "style"}:
            self._ignored_depth = max(0, self._ignored_depth - 1)
            return
        if self._ignored_depth:
            return
        if tag in {"td", "th"} and self._cell_parts is not None:
            if self._row is not None:
                self._row.append(self._text(self._cell_parts))
            self._cell_parts = None
        elif tag == "tr" and self._row is not None:
            if self._table is not None and any(self._row):
                self._table.append(self._row)
            self._row = None
        elif tag == "table" and self._table is not None:
            if self._table:
                self.tables.append(self._table)
            self._table = None
        elif tag in self.BLOCK_TAGS and self._block_parts is not None:
            line = self._text(self._block_parts)
            if line:
                self.lines.append(line)
            self._block_parts = None

    def handle_data(self, data: str) -> None:
        if self._ignored_depth:
            return
        if self._cell_parts is not None:
            self._cell_parts.append(data)
        elif self._block_parts is not None:
            self._block_parts.append(data)


def extract_qwen_document(value: str) -> QwenDocument:
    """Accept either requested TSV text or Qwen's native HTML transcription."""

    text = value.strip()
    text = re.sub(r"^```(?:html)?\s*", "", text, flags=re.IGNORECASE)
    text = re.sub(r"\s*```$", "", text)
    if not re.search(r"<(?:html|body|table|div|p|h[1-6])\b", text, re.IGNORECASE):
        return QwenDocument(lines=text.splitlines(), tables=[])
    parser = _QwenHtmlParser()
    parser.feed(text)
    parser.close()
    return QwenDocument(lines=parser.lines, tables=parser.tables)


@dataclass(frozen=True)
class QwenOcrSettings:
    enabled: bool
    api_key: str
    workspace_id: str
    base_url: str
    model: str
    timeout_seconds: float
    max_pixels: int
    max_pages: int
    concurrency: int = 3

    @classmethod
    def from_env(cls) -> "QwenOcrSettings":
        return cls(
            enabled=os.getenv("QWEN_OCR_ENABLED", "false").strip().lower()
            in {"1", "true", "yes", "on"},
            api_key=os.getenv("QWEN_OCR_API_KEY", "").strip(),
            workspace_id=os.getenv("QWEN_OCR_WORKSPACE_ID", "").strip(),
            base_url=os.getenv("QWEN_OCR_BASE_URL", "").strip(),
            model=os.getenv("QWEN_OCR_MODEL", "qwen3.5-ocr").strip(),
            timeout_seconds=float(os.getenv("QWEN_OCR_TIMEOUT_SECONDS", "180")),
            max_pixels=int(os.getenv("QWEN_OCR_MAX_PIXELS", "16000000")),
            max_pages=max(1, int(os.getenv("QWEN_OCR_MAX_PAGES", "50"))),
            concurrency=max(1, min(6, int(os.getenv("QWEN_OCR_CONCURRENCY", "3")))),
        )

    @property
    def configured(self) -> bool:
        return self.enabled and bool(self.api_key)

    @property
    def compatible_base_url(self) -> str:
        if self.base_url:
            return self.base_url.rstrip("/")
        if self.workspace_id:
            return (
                f"https://{self.workspace_id}.cn-beijing.maas.aliyuncs.com"
                "/compatible-mode/v1"
            )
        return "https://dashscope.aliyuncs.com/compatible-mode/v1"

    @property
    def compatible_base_urls(self) -> list[str]:
        """Return the private workspace endpoint first and public API as fallback."""

        if self.base_url:
            return [self.base_url.rstrip("/")]
        public_url = "https://dashscope.aliyuncs.com/compatible-mode/v1"
        if not self.workspace_id:
            return [public_url]
        return [self.compatible_base_url, public_url]


class QwenOcrClient:
    MAX_BASE64_BYTES = 10 * 1024 * 1024

    def __init__(
        self,
        settings: QwenOcrSettings | None = None,
        transport: httpx.BaseTransport | None = None,
    ) -> None:
        self.settings = settings or QwenOcrSettings.from_env()
        self.transport = transport

    @property
    def enabled(self) -> bool:
        return self.settings.configured

    def recognize_images(self, paths: list[Path]) -> list[str]:
        if not self.enabled:
            return []
        selected = paths[: self.settings.max_pages]
        if len(selected) <= 1 or self.settings.concurrency <= 1:
            return [self._recognize_image(path) for path in selected]
        with ThreadPoolExecutor(max_workers=self.settings.concurrency) as executor:
            return list(executor.map(self._recognize_image, selected))

    def _recognize_image(self, path: Path) -> str:
        image_url = self._image_data_url(path)
        payload: dict[str, Any] = {
            "model": self.settings.model,
            "messages": [
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "image_url",
                            "image_url": {"url": image_url},
                            "max_pixels": self.settings.max_pixels,
                        },
                        {"type": "text", "text": QWEN_OCR_PROMPT},
                    ],
                }
            ],
            "temperature": 0,
            "max_tokens": 16384,
        }
        last_error: Exception | None = None
        body: dict[str, Any] | None = None
        with httpx.Client(
            timeout=self.settings.timeout_seconds,
            transport=self.transport,
        ) as client:
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
                    response.raise_for_status()
                    body = response.json()
                    break
                except (httpx.HTTPError, ValueError) as exc:
                    last_error = exc
        if body is None:
            raise QwenOcrError("Qwen3.5-OCR request failed") from last_error
        try:
            content = body["choices"][0]["message"]["content"]
        except (KeyError, IndexError, TypeError) as exc:
            raise QwenOcrError("Qwen3.5-OCR returned an invalid response") from exc
        if isinstance(content, list):
            content = "\n".join(
                str(item.get("text", "")) if isinstance(item, dict) else str(item)
                for item in content
            )
        result = str(content).strip()
        if not result:
            raise QwenOcrError("Qwen3.5-OCR returned no text")
        return result

    def _image_data_url(self, path: Path) -> str:
        with Image.open(path) as source:
            image = ImageOps.exif_transpose(source).convert("RGB")
            longest = max(image.size)
            if longest > 5200:
                image.thumbnail((5200, 5200), Image.Resampling.LANCZOS)
            quality = 94
            while True:
                buffer = io.BytesIO()
                image.save(buffer, format="JPEG", quality=quality, optimize=True)
                encoded = base64.b64encode(buffer.getvalue())
                if len(encoded) <= self.MAX_BASE64_BYTES or quality <= 68:
                    break
                quality -= 6
            if len(encoded) > self.MAX_BASE64_BYTES:
                raise QwenOcrError("Image is too large for Qwen3.5-OCR Base64 input")
        return "data:image/jpeg;base64," + encoded.decode("ascii")
