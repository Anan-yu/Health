from abc import ABC, abstractmethod
from decimal import Decimal
from typing import TypedDict

from app.schemas.assessment import AssessmentRequest, ModelResult

MODEL_VERSION = "DEMO_2.0.0"

DISCLAIMER = (
    "本评估结果由演示规则引擎生成，仅供健康管理参考，不构成任何医学诊断或治疗建议。"
    "如有健康疑问，请咨询专业医疗机构。"
)


class RuleDefinition(TypedDict):
    code: str
    condition: str
    threshold: Decimal
    evidence: str
    penalty: int


class ModelDefinition(TypedDict):
    model_code: str
    model_name: str
    indicators: list[str]
    rules: list[RuleDefinition]
    base_score: int
    recommendations: list[str]


class RuleEngine(ABC):
    @abstractmethod
    def evaluate(self, request: AssessmentRequest) -> list[ModelResult]: ...


# ---------------------------------------------------------------------------
# Model definitions: each model specifies its applicable indicators and rules.
# A rule fires when the indicator value exceeds (GT) or is below (LT) threshold.
# ---------------------------------------------------------------------------

MODEL_DEFINITIONS: list[ModelDefinition] = [
    {
        "model_code": "GLUCOSE_METABOLISM",
        "model_name": "糖代谢失衡评估",
        "indicators": ["fasting_glucose", "fasting_insulin", "hba1c", "triglyceride", "hdl"],
        "rules": [
            {
                "code": "fasting_glucose",
                "condition": "GT",
                "threshold": Decimal("6.1"),
                "evidence": "空腹血糖高于参考上限",
                "penalty": 15,
            },
            {
                "code": "fasting_insulin",
                "condition": "GT",
                "threshold": Decimal("25.0"),
                "evidence": "空腹胰岛素偏高，提示胰岛素抵抗",
                "penalty": 12,
            },
            {
                "code": "hba1c",
                "condition": "GT",
                "threshold": Decimal("6.0"),
                "evidence": "糖化血红蛋白偏高",
                "penalty": 14,
            },
            {
                "code": "triglyceride",
                "condition": "GT",
                "threshold": Decimal("1.7"),
                "evidence": "甘油三酯高于参考上限",
                "penalty": 8,
            },
            {
                "code": "hdl",
                "condition": "LT",
                "threshold": Decimal("1.0"),
                "evidence": "高密度脂蛋白偏低",
                "penalty": 8,
            },
        ],
        "base_score": 90,
        "recommendations": [
            "建议控制精制碳水化合物摄入，增加膳食纤维",
            "保持规律有氧运动，每周至少150分钟",
            "建议由专业人员结合完整健康信息进行审核",
        ],
    },
    {
        "model_code": "CHRONIC_INFLAMMATION",
        "model_name": "慢性炎症与血管老化",
        "indicators": ["crp", "homocysteine", "ferritin"],
        "rules": [
            {
                "code": "crp",
                "condition": "GT",
                "threshold": Decimal("3.0"),
                "evidence": "C反应蛋白高于关注阈值",
                "penalty": 18,
            },
            {
                "code": "homocysteine",
                "condition": "GT",
                "threshold": Decimal("15.0"),
                "evidence": "同型半胱氨酸偏高，提示血管损伤风险",
                "penalty": 12,
            },
            {
                "code": "ferritin",
                "condition": "GT",
                "threshold": Decimal("300.0"),
                "evidence": "铁蛋白偏高，可能与慢性炎症相关",
                "penalty": 10,
            },
        ],
        "base_score": 90,
        "recommendations": [
            "建议增加富含Omega-3脂肪酸的食物摄入",
            "保持规律作息，减少慢性压力",
            "保持规律作息，并由健康管理人员复核完整指标",
        ],
    },
    {
        "model_code": "HPA_ADRENAL",
        "model_name": "HPA肾上腺压力轴",
        "indicators": ["cortisol_am", "cortisol_pm", "dhea_s"],
        "rules": [
            {
                "code": "cortisol_am",
                "condition": "GT",
                "threshold": Decimal("25.0"),
                "evidence": "晨起皮质醇偏高，提示HPA轴过度激活",
                "penalty": 15,
            },
            {
                "code": "cortisol_pm",
                "condition": "GT",
                "threshold": Decimal("10.0"),
                "evidence": "晚间皮质醇偏高，昼夜节律可能紊乱",
                "penalty": 12,
            },
            {
                "code": "dhea_s",
                "condition": "LT",
                "threshold": Decimal("50.0"),
                "evidence": "DHEA-S偏低，提示肾上腺储备不足",
                "penalty": 10,
            },
        ],
        "base_score": 88,
        "recommendations": [
            "建议进行规律的压力管理练习（冥想、深呼吸）",
            "保证充足睡眠，维持规律昼夜节律",
            "建议由专业人员结合完整健康信息进行审核",
        ],
    },
    {
        "model_code": "THYROID_HORMONE",
        "model_name": "甲状腺与性激素内分泌",
        "indicators": ["tsh", "ft3", "ft4", "estradiol", "testosterone"],
        "rules": [
            {
                "code": "tsh",
                "condition": "GT",
                "threshold": Decimal("4.2"),
                "evidence": "TSH高于参考上限，提示甲状腺功能减退倾向",
                "penalty": 15,
            },
            {
                "code": "ft3",
                "condition": "LT",
                "threshold": Decimal("3.1"),
                "evidence": "游离T3偏低",
                "penalty": 10,
            },
            {
                "code": "ft4",
                "condition": "LT",
                "threshold": Decimal("12.0"),
                "evidence": "游离T4偏低",
                "penalty": 10,
            },
            {
                "code": "estradiol",
                "condition": "LT",
                "threshold": Decimal("20.0"),
                "evidence": "雌二醇偏低，提示性激素水平不足",
                "penalty": 8,
            },
            {
                "code": "testosterone",
                "condition": "LT",
                "threshold": Decimal("3.0"),
                "evidence": "睾酮偏低",
                "penalty": 8,
            },
        ],
        "base_score": 88,
        "recommendations": [
            "建议定期复查甲状腺功能全套",
            "保证充足的碘、硒等微量元素摄入",
            "建议由内分泌专科医师结合临床表现综合判断",
        ],
    },
    {
        "model_code": "DETOX_GUT",
        "model_name": "重金属/肝脏解毒/肠道屏障",
        "indicators": ["alt", "ast", "ggt", "zonulin", "heavy_metal_panel"],
        "rules": [
            {
                "code": "alt",
                "condition": "GT",
                "threshold": Decimal("40.0"),
                "evidence": "ALT高于参考上限，提示肝细胞损伤",
                "penalty": 14,
            },
            {
                "code": "ast",
                "condition": "GT",
                "threshold": Decimal("40.0"),
                "evidence": "AST高于参考上限",
                "penalty": 10,
            },
            {
                "code": "ggt",
                "condition": "GT",
                "threshold": Decimal("60.0"),
                "evidence": "GGT偏高，提示肝脏解毒负担增加",
                "penalty": 10,
            },
            {
                "code": "zonulin",
                "condition": "GT",
                "threshold": Decimal("100.0"),
                "evidence": "连蛋白偏高，提示肠道屏障通透性增加",
                "penalty": 12,
            },
            {
                "code": "heavy_metal_panel",
                "condition": "GT",
                "threshold": Decimal("1.0"),
                "evidence": "重金属检测值偏高",
                "penalty": 15,
            },
        ],
        "base_score": 88,
        "recommendations": [
            "建议减少酒精摄入，避免肝毒性药物",
            "增加十字花科蔬菜摄入，支持肝脏解毒通路",
            "建议由专业人员结合完整健康信息进行审核",
        ],
    },
]


def _check_rule(value: Decimal, condition: str, threshold: Decimal) -> bool:
    if condition == "GT":
        return value > threshold
    if condition == "LT":
        return value < threshold
    return False


class DemoRuleEngine(RuleEngine):
    def evaluate(
        self,
        request: AssessmentRequest,
        model_codes: list[str] | None = None,
    ) -> list[ModelResult]:
        values: dict[str, Decimal] = {
            (item.code or item.name): item.value for item in request.indicators
        }

        results: list[ModelResult] = []
        definitions = MODEL_DEFINITIONS
        if model_codes:
            code_set = set(model_codes)
            definitions = [d for d in definitions if d["model_code"] in code_set]

        for model_def in definitions:
            evidence: list[str] = []
            missing: list[str] = []
            total_penalty = 0

            for rule in model_def["rules"]:
                code = rule["code"]
                value = values.get(code)
                if value is None:
                    missing.append(code)
                    continue
                if _check_rule(value, rule["condition"], rule["threshold"]):
                    evidence.append(rule["evidence"])
                    total_penalty += rule["penalty"]

            score = max(0, min(100, model_def["base_score"] - total_penalty))
            if score >= 80:
                risk_level = "LOW"
            elif score >= 60:
                risk_level = "ATTENTION"
            else:
                risk_level = "HIGH"

            if not evidence:
                evidence = [f"现有{model_def['model_name']}相关指标未触发演示关注规则"]

            results.append(
                ModelResult(
                    model_code=model_def["model_code"],
                    model_name=model_def["model_name"],
                    score=score,
                    risk_level=risk_level,
                    evidence=evidence,
                    missing_indicators=missing,
                    recommendations=model_def["recommendations"],
                )
            )

        return results
