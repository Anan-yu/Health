from decimal import Decimal

from pydantic import Field

from app.schemas.assessment import ComprehensiveInterpretation, ModelResult, PatientContext
from app.schemas.common import RaykModel


class ReportIndicator(RaykModel):
    code: str | None = None
    name: str
    value: Decimal
    unit: str
    reference_low: Decimal | None = Field(default=None, alias="referenceLow")
    reference_high: Decimal | None = Field(default=None, alias="referenceHigh")


class ReportGenerateRequest(RaykModel):
    assessment_id: str = Field(alias="assessmentId")
    patient_display_name: str = Field(alias="patientDisplayName")
    report_no: str = Field(alias="reportNo")
    published_at: str | None = Field(default=None, alias="publishedAt")
    doctor_opinion: str | None = Field(default=None, alias="doctorOpinion")
    indicators: list[ReportIndicator] = Field(default_factory=list)
    results: list[ModelResult]
    interpretation: ComprehensiveInterpretation | None = None
    patient_context: PatientContext | None = Field(default=None, alias="patientContext")


class ReportGenerateData(RaykModel):
    title: str
    summary: str
    sections: list[str]
    disclaimer: str
    pdf_base64: str = Field(alias="pdfBase64")
