from pydantic import Field

from app.schemas.common import RaykModel
from app.schemas.assessment import ModelResult


class ReportGenerateRequest(RaykModel):
    assessment_id: str = Field(alias="assessmentId")
    patient_display_name: str = Field(alias="patientDisplayName")
    results: list[ModelResult]


class ReportGenerateData(RaykModel):
    title: str
    summary: str
    sections: list[str]
    disclaimer: str
