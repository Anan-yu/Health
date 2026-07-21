from typing import Literal

from pydantic import Field

from app.schemas.common import RaykModel
from app.schemas.indicator import IndicatorInput


class PatientContext(RaykModel):
    gender: Literal["MALE", "FEMALE", "UNKNOWN"] = "UNKNOWN"
    age: int | None = Field(default=None, ge=0, le=120)


class AssessmentRequest(RaykModel):
    task_id: str = Field(alias="taskId", min_length=1)
    patient_id: str = Field(alias="patientId", min_length=1)
    indicators: list[IndicatorInput]
    model_codes: list[str] | None = Field(default=None, alias="modelCodes")
    patient_context: PatientContext | None = Field(default=None, alias="patientContext")


class ModelResult(RaykModel):
    model_code: str = Field(alias="modelCode")
    model_name: str = Field(alias="modelName")
    model_version: str = Field(default="3.0.0", alias="modelVersion")
    status: Literal["EVALUATED", "INSUFFICIENT_DATA"] = "EVALUATED"
    score: int | None = Field(default=None, ge=0, le=100)
    risk_level: str = Field(alias="riskLevel")
    data_completeness: int = Field(default=100, alias="dataCompleteness", ge=0, le=100)
    confidence: Literal["HIGH", "MEDIUM", "LOW"] = "MEDIUM"
    evidence: list[str]
    supporting_indicators: list[str] = Field(default_factory=list, alias="supportingIndicators")
    missing_indicators: list[str] = Field(alias="missingIndicators")
    recommendations: list[str]


class CrossModelFinding(RaykModel):
    title: str = Field(min_length=1, max_length=100)
    indicator_codes: list[str] = Field(default_factory=list, alias="indicatorCodes", max_length=20)
    explanation: str = Field(min_length=1, max_length=500)


class ComprehensiveInterpretation(RaykModel):
    status: Literal["SUCCESS", "DISABLED", "FALLBACK"]
    source: Literal["DEEPSEEK", "RULE_FALLBACK"]
    model: str | None = None
    summary: str = Field(min_length=1, max_length=1000)
    priority_concerns: list[str] = Field(default_factory=list, alias="priorityConcerns", max_length=10)
    cross_model_findings: list[CrossModelFinding] = Field(
        default_factory=list, alias="crossModelFindings", max_length=10
    )
    recommendations: list[str] = Field(default_factory=list, max_length=20)
    missing_data_advice: list[str] = Field(
        default_factory=list, alias="missingDataAdvice", max_length=20
    )
    followup_questions: list[str] = Field(
        default_factory=list, alias="followupQuestions", max_length=20
    )
    red_flags: list[str] = Field(default_factory=list, alias="redFlags", max_length=10)
    uncertainty: str = Field(min_length=1, max_length=500)
    disclaimer: str = Field(min_length=1, max_length=500)


class AssessmentData(RaykModel):
    task_id: str = Field(alias="taskId")
    model_version: str = Field(alias="modelVersion")
    status: str
    disclaimer: str
    results: list[ModelResult]
    interpretation: ComprehensiveInterpretation
