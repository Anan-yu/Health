from pydantic import Field

from app.schemas.common import RaykModel
from app.schemas.indicator import IndicatorInput


class OcrRecognizeRequest(RaykModel):
    file_id: str = Field(alias="fileId", min_length=1)
    object_name: str = Field(alias="objectName", min_length=1)
    mime_type: str = Field(alias="mimeType", pattern=r"^(image/|application/pdf)")


class OcrRecognizeData(RaykModel):
    engine: str
    status: str
    confidence: float
    indicators: list[IndicatorInput]
    warnings: list[str]
