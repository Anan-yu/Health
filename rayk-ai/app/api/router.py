import time

from fastapi import APIRouter

from app.core.constants import DISCLAIMER
from app.core.request_context import get_request_id
from app.interpretation.service import InterpretationService
from app.normalization.service import IndicatorNormalizationService
from app.ocr.service import build_ocr_service
from app.report.service import DemoReportService
from app.schemas.assessment import AssessmentData, AssessmentRequest
from app.schemas.common import ApiResponse
from app.schemas.indicator import NormalizationData, NormalizationRequest
from app.schemas.ocr import OcrRecognizeData, OcrRecognizeRequest
from app.schemas.report import ReportGenerateData, ReportGenerateRequest
from app.scoring.engine import MODEL_VERSION, DemoRuleEngine

router = APIRouter(prefix="/api/v1")
ocr_service = build_ocr_service()
normalization_service = IndicatorNormalizationService()
rule_engine = DemoRuleEngine()
interpretation_service = InterpretationService()
report_service = DemoReportService()


def ok(data: object) -> ApiResponse[object]:
    return ApiResponse(request_id=get_request_id(), timestamp=int(time.time() * 1000), data=data)


@router.post("/ocr/recognize", response_model=ApiResponse[OcrRecognizeData])
def recognize(request: OcrRecognizeRequest) -> ApiResponse[object]:
    return ok(ocr_service.recognize(request))


@router.post("/indicators/normalize", response_model=ApiResponse[NormalizationData])
def normalize(request: NormalizationRequest) -> ApiResponse[object]:
    data = NormalizationData(
        indicators=[normalization_service.normalize(item) for item in request.indicators]
    )
    return ok(data)


@router.post("/assessments/evaluate", response_model=ApiResponse[AssessmentData])
def evaluate(request: AssessmentRequest) -> ApiResponse[object]:
    results = rule_engine.evaluate(request, model_codes=request.model_codes)
    data = AssessmentData(
        task_id=request.task_id,
        model_version=MODEL_VERSION,
        status="SUCCESS",
        disclaimer=DISCLAIMER,
        results=results,
        interpretation=interpretation_service.interpret(request, results),
    )
    return ok(data)


@router.post("/reports/generate", response_model=ApiResponse[ReportGenerateData])
def generate_report(request: ReportGenerateRequest) -> ApiResponse[object]:
    return ok(report_service.generate(request))
