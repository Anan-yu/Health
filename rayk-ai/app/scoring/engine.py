from abc import ABC, abstractmethod
from decimal import Decimal

from app.schemas.assessment import AssessmentRequest, ModelResult


class RuleEngine(ABC):
    @abstractmethod
    def evaluate(self, request: AssessmentRequest) -> list[ModelResult]: ...


class DemoRuleEngine(RuleEngine):
    def evaluate(self, request: AssessmentRequest) -> list[ModelResult]:
        values = {(item.code or item.name): item.value for item in request.indicators}
        glucose = values.get("fasting_glucose")
        crp = values.get("crp")
        glucose_attention = glucose is not None and glucose > Decimal("6.1")
        inflammation_attention = crp is not None and crp > Decimal("3.0")
        return [
            ModelResult(
                model_code="GLUCOSE_METABOLISM",
                model_name="糖代谢评估",
                score=68 if glucose_attention else 86,
                risk_level="ATTENTION" if glucose_attention else "LOW",
                evidence=(
                    ["示例：空腹血糖高于演示关注阈值"]
                    if glucose_attention
                    else ["示例：现有糖代谢指标未触发演示关注规则"]
                ),
                missing_indicators=([] if "fasting_insulin" in values else ["fasting_insulin"]),
                recommendations=["建议由专业人员结合完整健康信息进行审核"],
            ),
            ModelResult(
                model_code="INFLAMMATION",
                model_name="慢性炎症评估",
                score=65 if inflammation_attention else 88,
                risk_level="ATTENTION" if inflammation_attention else "LOW",
                evidence=(
                    ["示例：C反应蛋白高于演示关注阈值"]
                    if inflammation_attention
                    else ["示例：现有炎症指标未触发演示关注规则"]
                ),
                missing_indicators=[],
                recommendations=["保持规律作息，并由健康管理人员复核完整指标"],
            ),
        ]
