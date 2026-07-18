from pydantic import Field

from app.schemas.common import RaykModel
from app.schemas.indicator import IndicatorInput


class AssessmentRequest(RaykModel):
    task_id: str = Field(alias="taskId", min_length=1)
    patient_id: str = Field(alias="patientId", min_length=1)
    indicators: list[IndicatorInput]


class ModelResult(RaykModel):
    model_code: str = Field(alias="modelCode")
    model_name: str = Field(alias="modelName")
    score: int = Field(ge=0, le=100)
    risk_level: str = Field(alias="riskLevel")
    evidence: list[str]
    missing_indicators: list[str] = Field(alias="missingIndicators")
    recommendations: list[str]


class AssessmentData(RaykModel):
    task_id: str = Field(alias="taskId")
    model_version: str = Field(alias="modelVersion")
    status: str
    disclaimer: str
    results: list[ModelResult]
