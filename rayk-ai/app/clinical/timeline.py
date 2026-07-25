from decimal import ROUND_HALF_UP, Decimal
from typing import Any

from app.schemas.assessment import AssessmentRequest, ModelResult


def _decimal_text(value: Decimal | None) -> str | None:
    if value is None:
        return None
    return format(value.normalize(), "f")


def _reference_status(
    value: Decimal, reference_low: Decimal | None, reference_high: Decimal | None
) -> str:
    if reference_low is not None and value < reference_low:
        return "LOW"
    if reference_high is not None and value > reference_high:
        return "HIGH"
    if reference_low is not None or reference_high is not None:
        return "WITHIN_RANGE"
    return "UNKNOWN"


class ClinicalContextBuilder:
    """Builds a de-identified, deterministic health snapshot for model grounding."""

    def build(self, request: AssessmentRequest, results: list[ModelResult]) -> dict[str, Any]:
        context = request.patient_context
        calculated_bmi: Decimal | None = None
        if context is not None:
            calculated_bmi = context.bmi
            if (
                calculated_bmi is None
                and context.height_cm is not None
                and context.weight_kg is not None
                and context.height_cm > 0
            ):
                height_m = context.height_cm / Decimal("100")
                calculated_bmi = (context.weight_kg / (height_m * height_m)).quantize(
                    Decimal("0.1"), rounding=ROUND_HALF_UP
                )

        indicators = [
            {
                "code": item.code,
                "name": item.name,
                "value": str(item.value),
                "unit": item.unit,
                "referenceLow": _decimal_text(item.reference_low),
                "referenceHigh": _decimal_text(item.reference_high),
                "referenceStatus": _reference_status(
                    item.value, item.reference_low, item.reference_high
                ),
            }
            for item in request.indicators
            if item.code
        ]
        abnormal = [item for item in indicators if item["referenceStatus"] in {"HIGH", "LOW"}]
        context_payload = (
            context.model_dump(by_alias=True, exclude_none=True, mode="json")
            if context is not None
            else {}
        )
        context_payload.pop("bmi", None)

        return {
            "demographics": {
                "gender": context.gender if context is not None else "UNKNOWN",
                "age": context.age if context is not None else None,
            },
            "anthropometrics": {
                "heightCm": (_decimal_text(context.height_cm) if context is not None else None),
                "weightKg": (_decimal_text(context.weight_kg) if context is not None else None),
                "waistCm": _decimal_text(context.waist_cm) if context is not None else None,
                "calculatedBmi": _decimal_text(calculated_bmi),
                "recentWeightChangeKg": (
                    _decimal_text(context.recent_weight_change_kg) if context is not None else None
                ),
            },
            "healthProfileAndQuestionnaire": context_payload,
            "laboratorySnapshot": {
                "totalCount": len(indicators),
                "abnormalCount": len(abnormal),
                "withinRangeCount": sum(
                    item["referenceStatus"] == "WITHIN_RANGE" for item in indicators
                ),
                "withoutReferenceCount": sum(
                    item["referenceStatus"] == "UNKNOWN" for item in indicators
                ),
                "indicators": indicators,
            },
            "ruleAssessmentSnapshot": {
                "evaluatedCount": sum(item.status == "EVALUATED" for item in results),
                "insufficientDataCount": sum(
                    item.status == "INSUFFICIENT_DATA" for item in results
                ),
                "attentionCount": sum(
                    item.status == "EVALUATED" and item.risk_level in {"ATTENTION", "HIGH"}
                    for item in results
                ),
                "results": [item.model_dump(by_alias=True, mode="json") for item in results],
            },
            "deterministicFindings": self._deterministic_findings(
                indicators, calculated_bmi, results
            ),
        }

    @staticmethod
    def _deterministic_findings(
        indicators: list[dict[str, Any]],
        calculated_bmi: Decimal | None,
        results: list[ModelResult],
    ) -> list[str]:
        findings: list[str] = []
        abnormal_count = sum(item["referenceStatus"] in {"HIGH", "LOW"} for item in indicators)
        if indicators:
            findings.append(
                f"本次共有{len(indicators)}项已标准化检验指标，其中"
                f"{abnormal_count}项超出报告参考区间。"
            )
        if calculated_bmi is not None:
            findings.append(f"根据身高和体重计算的BMI为{_decimal_text(calculated_bmi)}。")
        insufficient_count = sum(item.status == "INSUFFICIENT_DATA" for item in results)
        if insufficient_count:
            findings.append(
                f"共有{insufficient_count}个健康维度因数据不足未参与有效评分，"
                "不得将其解释为低风险。"
            )
        return findings
