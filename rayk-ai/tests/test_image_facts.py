from decimal import Decimal

from app.interpretation.image_facts import project_image_analysis
from app.interpretation.service import InterpretationService
from app.schemas.assessment import VisionImageAnalysis, VisionImageFinding, VisionImagePage
from app.schemas.assessment import AssessmentRequest
from app.scoring.engine import HealthRuleEngine


def test_project_image_analysis_preserves_facts_and_projects_known_numeric_indicators():
    analysis = VisionImageAnalysis(
        pages=[
            VisionImagePage(
                page=1,
                pageSummary="血脂结果需要结合完整资料复核",
                findings=[
                    VisionImageFinding(
                        category="生化检查",
                        item="总胆固醇",
                        result="6.2",
                        unit="mmol/L",
                        referenceRange="3.0--5.2",
                        abnormalFlag="↑",
                    ),
                    VisionImageFinding(
                        category="生化检查",
                        item="未标准化的检查项目",
                        result="阳性",
                        conclusion="建议结合医生意见复核",
                    ),
                ],
            )
        ]
    )

    indicators, findings = project_image_analysis(analysis)

    assert len(indicators) == 1
    assert indicators[0].code == "total_cholesterol"
    assert indicators[0].value == Decimal("6.2")
    assert indicators[0].reference_low == Decimal("3.0")
    assert indicators[0].reference_high == Decimal("5.2")
    assert any(item.item == "未标准化的检查项目" for item in findings)
    assert any(item.item == "检查小结" for item in findings)
    assert any(item.item == "页面小结" for item in findings)


def test_image_projection_feeds_health_dimension_rules():
    analysis = VisionImageAnalysis(
        pages=[
            VisionImagePage(
                page=1,
                findings=[
                    VisionImageFinding(
                        item="总胆固醇",
                        result="6.2",
                        unit="mmol/L",
                        referenceRange="3.0--5.2",
                    ),
                    VisionImageFinding(
                        item="低密度脂蛋白胆固醇",
                        result="4.0",
                        unit="mmol/L",
                        referenceRange="0--3.4",
                    ),
                ],
            )
        ]
    )
    request = AssessmentRequest(
        taskId="task-image",
        patientId="patient-image",
        indicators=[],
    )

    enriched = InterpretationService.enrich_request_with_image_analysis(request, analysis)
    results = HealthRuleEngine().evaluate(enriched, model_codes=["LIPID_CARDIOVASCULAR"])

    assert len(enriched.indicators) == 2
    assert results[0].status == "EVALUATED"
    assert results[0].risk_level == "ATTENTION"
    assert set(results[0].supporting_indicators) == {"total_cholesterol", "ldl"}
