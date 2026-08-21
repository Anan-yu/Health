import logging
import time

from fastapi import APIRouter

from app.core.constants import DISCLAIMER
from app.core.request_context import get_request_id
from app.followup.service import FollowupAdjustmentService
from app.interpretation.service import InterpretationService
from app.normalization.service import IndicatorNormalizationService
from app.ocr.service import build_ocr_service
from app.report.service import DemoReportService
from app.schemas.assessment import AssessmentData, AssessmentRequest
from app.schemas.common import ApiResponse
from app.schemas.followup import FollowupAdjustmentData, FollowupAdjustmentRequest
from app.schemas.indicator import NormalizationData, NormalizationRequest
from app.schemas.ocr import OcrRecognizeData, OcrRecognizeRequest
from app.schemas.report import ReportGenerateData, ReportGenerateRequest
from app.scoring.engine import MODEL_VERSION, DemoRuleEngine

router = APIRouter(prefix="/api/v1")
ocr_service = build_ocr_service()
normalization_service = IndicatorNormalizationService()
rule_engine = DemoRuleEngine()
interpretation_service = InterpretationService()
followup_adjustment_service = FollowupAdjustmentService()
report_service = DemoReportService()
logger = logging.getLogger(__name__)


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
    assessment_request = request
    image_analysis = None
    if request.report_images and interpretation_service.vision_settings.configured:
        try:
            image_analysis = interpretation_service.analyze_report_images(request)
            assessment_request = interpretation_service.enrich_request_with_image_analysis(
                request, image_analysis
            )
        except Exception as exception:
            # The interpretation service retains its existing safe fallback on a failed
            # image-read attempt. Do not make the HTTP endpoint fail before it can return
            # the patient's profile and a traceable degraded result.
            logger.warning(
                "Image assessment preparation failed errorType=%s",
                type(exception).__name__,
            )
    results = rule_engine.evaluate(assessment_request, model_codes=request.model_codes)
    interpretation = interpretation_service.interpret_with_analysis(
        assessment_request, results, image_analysis=image_analysis
    )
    data = AssessmentData(
        task_id=request.task_id,
        model_version=MODEL_VERSION,
        status="SUCCESS",
        disclaimer=DISCLAIMER,
        results=results,
        interpretation=interpretation.interpretation,
        patient_context=assessment_request.patient_context,
        image_analysis=interpretation.image_analysis,
    )
    return ok(data)


@router.post("/reports/generate", response_model=ApiResponse[ReportGenerateData])
def generate_report(request: ReportGenerateRequest) -> ApiResponse[object]:
    return ok(report_service.generate(request))


@router.post("/followups/adjust", response_model=ApiResponse[FollowupAdjustmentData])
def adjust_followup(request: FollowupAdjustmentRequest) -> ApiResponse[object]:
    return ok(followup_adjustment_service.adjust(request))
