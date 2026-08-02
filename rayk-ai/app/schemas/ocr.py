from decimal import Decimal

from pydantic import Field

from app.schemas.common import RaykModel
from app.schemas.indicator import IndicatorInput


class OcrRecognizeRequest(RaykModel):
    task_id: str | None = Field(default=None, alias="taskId")
    file_id: str = Field(alias="fileId", min_length=1)
    object_name: str = Field(alias="objectName", min_length=1)
    mime_type: str = Field(alias="mimeType", pattern=r"^(image/|application/pdf)")
    download_url: str | None = Field(default=None, alias="downloadUrl")


class OcrFinding(RaykModel):
    section: str
    item: str
    result: str


class OcrRecognizeData(RaykModel):
    engine: str
    status: str
    confidence: Decimal
    indicators: list[IndicatorInput]
    findings: list[OcrFinding] = Field(default_factory=list)
    raw_lines: list[str] = Field(alias="rawLines")
    warnings: list[str]
