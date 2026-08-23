from typing import Literal

from pydantic import Field

from app.schemas.assessment import PatientContext
from app.schemas.common import RaykModel


class MedicalAssistantMessage(RaykModel):
    role: Literal["USER", "ASSISTANT"]
    content: str = Field(min_length=1, max_length=4000)


class MedicalAssistantRequest(RaykModel):
    conversation_id: str = Field(alias="conversationId", min_length=1, max_length=80)
    patient_id: str = Field(alias="patientId", min_length=1, max_length=80)
    patient_name: str | None = Field(default=None, alias="patientName", max_length=80)
    messages: list[MedicalAssistantMessage] = Field(min_length=1, max_length=20)
    patient_context: PatientContext | None = Field(default=None, alias="patientContext")
    latest_report_summary: str | None = Field(
        default=None, alias="latestReportSummary", max_length=2000
    )
    latest_assessment_snapshot: str | None = Field(
        default=None, alias="latestAssessmentSnapshot", max_length=50000
    )
    model: str | None = Field(default=None, max_length=80)
    thinking_enabled: bool | None = Field(default=False, alias="thinkingEnabled")


class MedicalAssistantData(RaykModel):
    reply: str = Field(min_length=1, max_length=4000)
    risk_level: Literal["INFO", "ATTENTION", "URGENT"] = Field(alias="riskLevel")
    emergency: bool = False
    recommended_action: str | None = Field(default=None, alias="recommendedAction", max_length=800)
    citations: list[str] = Field(default_factory=list, max_length=10)
    used_context: list[str] = Field(default_factory=list, alias="usedContext", max_length=10)
    followup_questions: list[str] = Field(
        default_factory=list, alias="followupQuestions", max_length=5
    )
    disclaimer: str = Field(min_length=1, max_length=500)
    model: str | None = None
