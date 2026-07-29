from typing import Literal

from pydantic import Field

from app.schemas.assessment import PatientContext
from app.schemas.common import RaykModel


class FollowupActionFeedback(RaykModel):
    section: str = Field(min_length=1, max_length=50)
    action: str = Field(min_length=1, max_length=300)
    status: Literal["COMPLETED", "PARTIAL", "NOT_COMPLETED"]
    note: str | None = Field(default=None, max_length=300)


class FollowupAdjustmentRequest(RaykModel):
    patient_context: PatientContext | None = Field(default=None, alias="patientContext")
    cycle_no: int = Field(alias="cycleNo", ge=1, le=20)
    max_cycles: int = Field(alias="maxCycles", ge=1, le=20)
    completion_rate: int = Field(alias="completionRate", ge=0, le=100)
    feedback: str | None = Field(default=None, max_length=1000)
    actions: list[FollowupActionFeedback] = Field(min_length=1, max_length=30)


class FollowupActionSuggestion(RaykModel):
    section: str = Field(min_length=1, max_length=50)
    action: str = Field(min_length=1, max_length=300)


class FollowupAdjustmentData(RaykModel):
    decision: Literal["CONTINUE", "ADJUST", "TERMINATE"]
    decision_reason: str = Field(alias="decisionReason", min_length=1, max_length=500)
    feedback_summary: str = Field(alias="feedbackSummary", min_length=1, max_length=1000)
    next_actions: list[FollowupActionSuggestion] = Field(
        default_factory=list, alias="nextActions", max_length=20
    )
    source: Literal["DEEPSEEK", "RULE_FALLBACK"] = "DEEPSEEK"
    model: str | None = None
