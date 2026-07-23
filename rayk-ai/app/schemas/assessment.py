from decimal import Decimal
from typing import Literal

from pydantic import Field

from app.schemas.common import RaykModel
from app.schemas.indicator import IndicatorInput


class PatientContext(RaykModel):
    gender: Literal["MALE", "FEMALE", "UNKNOWN"] = "UNKNOWN"
    age: int | None = Field(default=None, ge=0, le=120)
    height_cm: Decimal | None = Field(default=None, alias="heightCm")
    weight_kg: Decimal | None = Field(default=None, alias="weightKg")
    bmi: Decimal | None = None
    medical_history: str | None = Field(default=None, alias="medicalHistory")
    family_history: str | None = Field(default=None, alias="familyHistory")
    diabetes_status: str | None = Field(default=None, alias="diabetesStatus")
    hypertension_status: str | None = Field(default=None, alias="hypertensionStatus")
    dyslipidemia_status: str | None = Field(default=None, alias="dyslipidemiaStatus")
    fatty_liver_status: str | None = Field(default=None, alias="fattyLiverStatus")
    smoking_status: str | None = Field(default=None, alias="smokingStatus")
    alcohol_status: str | None = Field(default=None, alias="alcoholStatus")
    exercise_frequency: str | None = Field(default=None, alias="exerciseFrequency")
    sleep_quality: str | None = Field(default=None, alias="sleepQuality")
    sleep_hours: Decimal | None = Field(default=None, alias="sleepHours", ge=0, le=24)
    stress_level: str | None = Field(default=None, alias="stressLevel")
    mood_status: str | None = Field(default=None, alias="moodStatus")
    fear_level: str | None = Field(default=None, alias="fearLevel")
    dietary_preference: str | None = Field(default=None, alias="dietaryPreference")
    recent_dietary_pattern: str | None = Field(default=None, alias="recentDietaryPattern")


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
    patient_context: PatientContext | None = Field(default=None, alias="patientContext")
