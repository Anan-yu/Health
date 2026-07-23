from abc import ABC, abstractmethod
from decimal import Decimal
from typing import Literal, TypedDict

from app.schemas.assessment import AssessmentRequest, ModelResult, PatientContext
from app.schemas.indicator import IndicatorInput

MODEL_VERSION = "RULE_3.0.0"
MODEL_ITEM_VERSION = "3.0.0"


class RuleDefinition(TypedDict, total=False):
    code: str
    condition: Literal["HIGH", "LOW"]
    threshold: Decimal
    unit: str
    evidence: str
    penalty: int


class ModelDefinition(TypedDict, total=False):
    model_code: str
    model_name: str
    indicators: list[str]
    minimum_indicators: int
    rules: list[RuleDefinition]
    base_score: int
    recommendations: list[str]
    requires_gender: bool
    requires_age: bool


class RuleEngine(ABC):
    @abstractmethod
    def evaluate(
        self, request: AssessmentRequest, model_codes: list[str] | None = None
    ) -> list[ModelResult]: ...


def _rule(
    code: str,
    condition: Literal["HIGH", "LOW"],
    threshold: str,
    evidence: str,
    penalty: int,
    unit: str = "",
) -> RuleDefinition:
    return {
        "code": code,
        "condition": condition,
        "threshold": Decimal(threshold),
        "unit": unit,
        "evidence": evidence,
        "penalty": penalty,
    }


# These health-management rules are conservative, explainable defaults. Laboratory-provided
# reference intervals take precedence over fallback thresholds. They are not diagnostic rules.
MODEL_DEFINITIONS: list[ModelDefinition] = [
    {
        "model_code": "GLUCOSE_METABOLISM",
        "model_name": "糖代谢与胰岛素抵抗",
        "indicators": [
            "fasting_glucose",
            "fasting_insulin",
            "hba1c",
            "triglyceride",
            "hdl",
        ],
        "minimum_indicators": 2,
        "rules": [
            _rule("fasting_glucose", "HIGH", "6.1", "空腹血糖高于关注范围", 15, "mmol/L"),
            _rule("fasting_insulin", "HIGH", "25", "空腹胰岛素偏高", 12, "mIU/L"),
            _rule("hba1c", "HIGH", "6.0", "糖化血红蛋白偏高", 14, "%"),
            _rule("triglyceride", "HIGH", "1.7", "甘油三酯偏高", 8, "mmol/L"),
            _rule("hdl", "LOW", "1.0", "高密度脂蛋白偏低", 8, "mmol/L"),
        ],
        "base_score": 90,
        "recommendations": [
            "优先核对糖代谢相关指标并结合饮食、运动和体重变化综合评估",
            "建议在专业人员指导下制定可持续的饮食和运动计划",
            "按审核意见安排复查并观察指标趋势",
        ],
    },
    {
        "model_code": "LIPID_CARDIOVASCULAR",
        "model_name": "血脂与心血管代谢",
        "indicators": ["total_cholesterol", "ldl", "hdl", "triglyceride", "apob", "lpa"],
        "minimum_indicators": 2,
        "rules": [
            _rule("total_cholesterol", "HIGH", "5.2", "总胆固醇偏高", 10, "mmol/L"),
            _rule("ldl", "HIGH", "3.4", "低密度脂蛋白偏高", 14, "mmol/L"),
            _rule("hdl", "LOW", "1.0", "高密度脂蛋白偏低", 10, "mmol/L"),
            _rule("triglyceride", "HIGH", "1.7", "甘油三酯偏高", 10, "mmol/L"),
            _rule("apob", "HIGH", "1.1", "载脂蛋白B偏高", 12, "g/L"),
            _rule("lpa", "HIGH", "75", "脂蛋白(a)高于关注范围", 12, "nmol/L"),
        ],
        "base_score": 90,
        "recommendations": [
            "结合血压、吸烟史、家族史和既往心血管情况进行专业复核",
            "关注膳食脂肪结构、体重管理和规律运动",
            "按医生建议复查血脂相关指标",
        ],
    },
    {
        "model_code": "CHRONIC_INFLAMMATION",
        "model_name": "慢性炎症与免疫负荷",
        "indicators": ["crp", "hs_crp", "esr", "homocysteine", "ferritin", "wbc"],
        "minimum_indicators": 2,
        "rules": [
            _rule("crp", "HIGH", "3.0", "C反应蛋白高于关注范围", 16, "mg/L"),
            _rule("hs_crp", "HIGH", "3.0", "高敏C反应蛋白高于关注范围", 16, "mg/L"),
            _rule("esr", "HIGH", "20", "红细胞沉降率偏高", 8, "mm/h"),
            _rule("homocysteine", "HIGH", "15", "同型半胱氨酸偏高", 12, "umol/L"),
            _rule("ferritin", "HIGH", "300", "铁蛋白偏高，需结合炎症和铁代谢复核", 8, "ng/mL"),
            _rule("wbc", "HIGH", "10", "白细胞计数偏高", 8, "10^9/L"),
        ],
        "base_score": 90,
        "recommendations": [
            "炎症指标受感染、运动和采样状态等因素影响，建议结合近期情况复核",
            "保持规律睡眠、适量运动和均衡饮食",
            "持续异常时由医生判断是否需要进一步检查",
        ],
    },
    {
        "model_code": "LIVER_METABOLIC",
        "model_name": "肝脏与代谢负担",
        "indicators": [
            "alt",
            "ast",
            "ggt",
            "total_bilirubin",
            "direct_bilirubin",
            "albumin",
        ],
        "minimum_indicators": 2,
        "rules": [
            _rule("alt", "HIGH", "40", "丙氨酸氨基转移酶偏高", 14, "U/L"),
            _rule("ast", "HIGH", "40", "天门冬氨酸氨基转移酶偏高", 12, "U/L"),
            _rule("ggt", "HIGH", "60", "γ-谷氨酰转移酶偏高", 12, "U/L"),
            _rule("total_bilirubin", "HIGH", "21", "总胆红素偏高", 10, "umol/L"),
            _rule("direct_bilirubin", "HIGH", "7", "直接胆红素偏高", 8, "umol/L"),
            _rule("albumin", "LOW", "35", "白蛋白偏低", 12, "g/L"),
        ],
        "base_score": 90,
        "recommendations": [
            "结合饮酒、用药、体重及影像资料由医生复核肝脏相关风险",
            "避免自行使用可能增加肝脏负担的药物或补充剂",
            "按专业意见安排复查",
        ],
    },
    {
        "model_code": "KIDNEY_ELECTROLYTE",
        "model_name": "肾功能与电解质",
        "indicators": [
            "creatinine",
            "egfr",
            "urea",
            "uric_acid",
            "sodium",
            "potassium",
            "chloride",
            "bicarbonate",
            "calcium",
        ],
        "minimum_indicators": 2,
        "rules": [
            _rule("creatinine", "HIGH", "115", "肌酐偏高", 14, "umol/L"),
            _rule("egfr", "LOW", "60", "估算肾小球滤过率偏低", 18, "mL/min/1.73m2"),
            _rule("urea", "HIGH", "8.2", "尿素偏高", 8, "mmol/L"),
            _rule("uric_acid", "HIGH", "420", "尿酸偏高", 10, "umol/L"),
            _rule("sodium", "HIGH", "145", "血钠高于参考范围", 10, "mmol/L"),
            _rule("sodium", "LOW", "135", "血钠低于参考范围", 10, "mmol/L"),
            _rule("potassium", "HIGH", "5.5", "血钾高于参考范围", 14, "mmol/L"),
            _rule("potassium", "LOW", "3.5", "血钾低于参考范围", 14, "mmol/L"),
            _rule("chloride", "HIGH", "110", "血氯高于参考范围", 8, "mmol/L"),
            _rule("chloride", "LOW", "99", "血氯低于参考范围", 8, "mmol/L"),
            _rule("bicarbonate", "HIGH", "30", "碳酸氢根高于参考范围", 8, "mmol/L"),
            _rule("bicarbonate", "LOW", "22", "碳酸氢根低于参考范围", 8, "mmol/L"),
            _rule("calcium", "HIGH", "2.6", "血钙高于参考范围", 10, "mmol/L"),
            _rule("calcium", "LOW", "2.1", "血钙低于参考范围", 10, "mmol/L"),
        ],
        "base_score": 90,
        "recommendations": [
            "肾功能和电解质异常需结合年龄、饮水、用药及基础疾病由医生复核",
            "不要根据单次结果自行调整处方药或大量补充电解质",
            "按医生建议进行复查或进一步检查",
        ],
    },
    {
        "model_code": "HEMATOLOGY_ANEMIA",
        "model_name": "血液与贫血相关风险",
        "indicators": ["hemoglobin", "rbc", "mcv", "mch", "ferritin", "vitamin_b12", "folate"],
        "minimum_indicators": 3,
        "rules": [
            _rule("hemoglobin", "LOW", "120", "血红蛋白偏低", 16, "g/L"),
            _rule("rbc", "LOW", "3.8", "红细胞计数偏低", 10, "10^12/L"),
            _rule("mcv", "LOW", "80", "平均红细胞体积偏低", 8, "fL"),
            _rule("mcv", "HIGH", "100", "平均红细胞体积偏高", 8, "fL"),
            _rule("mch", "LOW", "27", "平均红细胞血红蛋白量偏低", 8, "pg"),
            _rule("ferritin", "LOW", "15", "铁蛋白偏低", 12, "ng/mL"),
            _rule("vitamin_b12", "LOW", "200", "维生素B12偏低", 10, "pg/mL"),
            _rule("folate", "LOW", "4", "叶酸偏低", 10, "ng/mL"),
        ],
        "base_score": 90,
        "recommendations": [
            "结合血常规、铁代谢、维生素B12和叶酸结果综合判断",
            "不建议在原因不明时自行长期补铁或使用大剂量补充剂",
            "由医生结合症状和既往史复核",
        ],
    },
    {
        "model_code": "THYROID_HORMONE",
        "model_name": "甲状腺功能",
        "indicators": ["tsh", "ft3", "ft4", "tpo_ab", "tg_ab"],
        "minimum_indicators": 2,
        "rules": [
            _rule("tsh", "HIGH", "4.2", "促甲状腺激素偏高", 15, "mIU/L"),
            _rule("tsh", "LOW", "0.27", "促甲状腺激素偏低", 15, "mIU/L"),
            _rule("ft3", "LOW", "3.1", "游离T3偏低", 10, "pmol/L"),
            _rule("ft4", "LOW", "12", "游离T4偏低", 10, "pmol/L"),
            _rule("tpo_ab", "HIGH", "34", "甲状腺过氧化物酶抗体偏高", 10, "IU/mL"),
            _rule("tg_ab", "HIGH", "115", "甲状腺球蛋白抗体偏高", 10, "IU/mL"),
        ],
        "base_score": 90,
        "recommendations": [
            "结合年龄、症状、用药和甲状腺影像资料由医生综合判断",
            "不要根据本评估自行调整甲状腺相关处方药",
            "按专业意见复查甲状腺功能",
        ],
    },
    {
        "model_code": "BODY_COMPOSITION",
        "model_name": "体重与身体成分",
        "indicators": [
            "bmi",
            "waist_risk_score",
            "recent_weight_change_kg",
            "exercise_frequency_score",
        ],
        "minimum_indicators": 2,
        "rules": [
            _rule("bmi", "LOW", "18.5", "BMI低于常用健康参考范围", 12),
            _rule("bmi", "HIGH", "24", "BMI高于常用健康参考范围", 8),
            _rule("bmi", "HIGH", "28", "BMI达到肥胖风险关注范围", 8),
            _rule("waist_risk_score", "HIGH", "0", "腰围达到腹型肥胖风险关注范围", 10),
            _rule("recent_weight_change_kg", "LOW", "-5", "近三个月体重下降较明显", 8, "kg"),
            _rule("recent_weight_change_kg", "HIGH", "5", "近三个月体重增加较明显", 8, "kg"),
            _rule("exercise_frequency_score", "LOW", "1", "当前运动频率偏低", 8),
        ],
        "base_score": 92,
        "recommendations": [
            "结合体重、腰围和运动情况制定可持续的饮食与运动计划",
            "建议定期记录体重和腰围变化，关注趋势而非单次波动",
            "体重短期明显变化或伴随不适时，应由医生进一步判断原因",
        ],
    },
    {
        "model_code": "HPA_ADRENAL",
        "model_name": "HPA压力、睡眠与恢复",
        "indicators": ["cortisol_am", "cortisol_pm", "dhea_s", "sleep_hours", "hrv"],
        "minimum_indicators": 1,
        "rules": [
            _rule("cortisol_am", "HIGH", "25", "晨间皮质醇偏高", 14, "ug/dL"),
            _rule("cortisol_am", "LOW", "5", "晨间皮质醇偏低", 12, "ug/dL"),
            _rule("cortisol_pm", "HIGH", "10", "晚间皮质醇偏高", 12, "ug/dL"),
            _rule("dhea_s", "LOW", "50", "DHEA-S偏低", 10, "ug/dL"),
            _rule("sleep_hours", "LOW", "6", "近期平均睡眠时间偏短", 8, "h"),
        ],
        "base_score": 88,
        "recommendations": [
            "优先改善规律作息、睡眠环境和可持续的压力管理方式",
            "皮质醇结果需结合采样时间、药物和近期应激状态解释",
            "持续不适时由专业人员进一步评估",
        ],
    },
    {
        "model_code": "NUTRITION_MICRONUTRIENT",
        "model_name": "营养与微量元素",
        "indicators": [
            "vitamin_d",
            "vitamin_b12",
            "folate",
            "ferritin",
            "zinc",
            "magnesium",
            "calcium",
            "prealbumin",
        ],
        "minimum_indicators": 2,
        "rules": [
            _rule("vitamin_d", "LOW", "20", "维生素D偏低", 12, "ng/mL"),
            _rule("vitamin_b12", "LOW", "200", "维生素B12偏低", 10, "pg/mL"),
            _rule("folate", "LOW", "4", "叶酸偏低", 10, "ng/mL"),
            _rule("ferritin", "LOW", "15", "铁蛋白偏低", 10, "ng/mL"),
            _rule("zinc", "LOW", "70", "锌低于参考范围", 8, "ug/dL"),
            _rule("magnesium", "LOW", "0.75", "镁低于参考范围", 8, "mmol/L"),
            _rule("calcium", "LOW", "2.1", "钙低于参考范围", 8, "mmol/L"),
            _rule("prealbumin", "LOW", "200", "前白蛋白偏低", 12, "mg/L"),
        ],
        "base_score": 90,
        "recommendations": [
            "优先通过膳食评估和确认后的检验结果识别营养缺口",
            "补充剂剂量和周期应结合饮食、用药及医生意见确定",
            "避免多个复合产品造成重复或过量摄入",
        ],
    },
    {
        "model_code": "GUT_BARRIER",
        "model_name": "消化与肠道屏障",
        "indicators": [
            "zonulin",
            "calprotectin",
            "occult_blood",
            "stool_ph",
            "digestive_symptom_score",
        ],
        "minimum_indicators": 2,
        "rules": [
            _rule("zonulin", "HIGH", "100", "连蛋白高于检验参考范围", 12, "ng/mL"),
            _rule("calprotectin", "HIGH", "50", "粪便钙卫蛋白偏高", 16, "ug/g"),
            _rule("occult_blood", "HIGH", "0", "便潜血结果需要关注", 18),
            _rule("stool_ph", "HIGH", "7.5", "粪便pH高于参考范围", 6),
            _rule("digestive_symptom_score", "HIGH", "6", "消化道症状评分较高", 10),
        ],
        "base_score": 88,
        "recommendations": [
            "结合消化道症状、饮食和正规检验结果综合评估",
            "出现便潜血等需要关注的结果时应由医生判断下一步检查",
            "不根据单一肠道指标自行进行所谓排毒或极端饮食",
        ],
    },
    {
        "model_code": "MENTAL_EMOTIONAL",
        "model_name": "心理与情绪健康",
        "indicators": ["stress_level_score", "mood_status_score", "fear_level_score"],
        "minimum_indicators": 2,
        "rules": [
            _rule("stress_level_score", "HIGH", "1", "近期压力水平需要关注", 10),
            _rule("mood_status_score", "HIGH", "1", "近期心情状态需要关注", 12),
            _rule("fear_level_score", "HIGH", "1", "恐惧或焦虑感受需要关注", 10),
        ],
        "base_score": 92,
        "recommendations": [
            "保持规律作息，并通过可持续的放松活动减轻压力",
            "记录持续的情绪、睡眠和压力变化，结合健康随访观察趋势",
            "若情绪困扰持续影响日常生活，建议寻求专业支持",
        ],
    },
]


def _normalized_unit(value: str) -> str:
    return value.strip().lower().replace("μ", "u").replace("µ", "u").replace(" ", "")


def _rule_threshold(indicator: IndicatorInput, rule: RuleDefinition) -> Decimal | None:
    if rule["condition"] == "HIGH" and indicator.reference_high is not None:
        return indicator.reference_high
    if rule["condition"] == "LOW" and indicator.reference_low is not None:
        return indicator.reference_low
    expected_unit = rule.get("unit", "")
    if expected_unit and _normalized_unit(indicator.unit) != _normalized_unit(expected_unit):
        return None
    return rule["threshold"]


def _fires(value: Decimal, condition: str, threshold: Decimal) -> bool:
    return value > threshold if condition == "HIGH" else value < threshold


def _context_level(value: str | None, mapping: dict[str, int]) -> Decimal | None:
    if not value:
        return None
    score = mapping.get(value.strip().upper())
    return Decimal(score) if score is not None else None


class HealthRuleEngine(RuleEngine):
    def evaluate(
        self,
        request: AssessmentRequest,
        model_codes: list[str] | None = None,
    ) -> list[ModelResult]:
        values: dict[str, IndicatorInput] = {
            item.code: item for item in request.indicators if item.code
        }
        context = request.patient_context
        if context is not None and context.sleep_hours is not None and "sleep_hours" not in values:
            values["sleep_hours"] = IndicatorInput(
                code="sleep_hours",
                name="平均睡眠时长",
                value=context.sleep_hours,
                unit="h",
            )
        if context is not None:
            if context.bmi is not None and "bmi" not in values:
                values["bmi"] = IndicatorInput(
                    code="bmi", name="身体质量指数", value=context.bmi, unit="kg/m2"
                )
            if context.waist_cm is not None and "waist_risk_score" not in values:
                waist_limit = Decimal("85") if context.gender == "FEMALE" else Decimal("90")
                values["waist_risk_score"] = IndicatorInput(
                    code="waist_risk_score",
                    name="腰围风险",
                    value=Decimal("1") if context.waist_cm >= waist_limit else Decimal("0"),
                    unit="score",
                )
            if (
                context.recent_weight_change_kg is not None
                and "recent_weight_change_kg" not in values
            ):
                values["recent_weight_change_kg"] = IndicatorInput(
                    code="recent_weight_change_kg",
                    name="近三个月体重变化",
                    value=context.recent_weight_change_kg,
                    unit="kg",
                )
            exercise_score = _context_level(
                context.exercise_frequency,
                {"RARELY": 0, "1_2_PER_WEEK": 1, "3_5_PER_WEEK": 2, "DAILY": 3},
            )
            if exercise_score is not None and "exercise_frequency_score" not in values:
                values["exercise_frequency_score"] = IndicatorInput(
                    code="exercise_frequency_score",
                    name="运动频率",
                    value=exercise_score,
                    unit="score",
                )
            context_scores = (
                (
                    "stress_level_score",
                    "近期压力水平",
                    _context_level(
                        context.stress_level,
                        {"NONE": 0, "LOW": 1, "MEDIUM": 2, "HIGH": 3, "VERY_HIGH": 4},
                    ),
                ),
                (
                    "mood_status_score",
                    "近期心情状态",
                    _context_level(
                        context.mood_status,
                        {
                            "EXCELLENT": 0,
                            "VERY_GOOD": 0,
                            "GOOD": 1,
                            "FAIR": 2,
                            "POOR": 3,
                            "VERY_POOR": 4,
                        },
                    ),
                ),
                (
                    "fear_level_score",
                    "恐惧或焦虑感受",
                    _context_level(
                        context.fear_level,
                        {"NONE": 0, "LOW": 1, "MEDIUM": 2, "HIGH": 3, "VERY_HIGH": 4},
                    ),
                ),
            )
            for code, name, score in context_scores:
                if score is not None and code not in values:
                    values[code] = IndicatorInput(code=code, name=name, value=score, unit="score")
        definitions = MODEL_DEFINITIONS
        if model_codes:
            selected = set(model_codes)
            definitions = [item for item in definitions if item["model_code"] in selected]

        return [self._evaluate_model(request, values, model) for model in definitions]

    def _evaluate_model(
        self,
        request: AssessmentRequest,
        values: dict[str, IndicatorInput],
        model: ModelDefinition,
    ) -> ModelResult:
        expected = model["indicators"]
        present = [code for code in expected if code in values]
        missing = [code for code in expected if code not in values]
        completeness = round(len(present) * 100 / len(expected)) if expected else 0

        context = request.patient_context
        missing_context: list[str] = []
        if model.get("requires_gender") and (context is None or context.gender == "UNKNOWN"):
            missing_context.append("gender")
        if model.get("requires_age") and (context is None or context.age is None):
            missing_context.append("age")

        if len(present) < model["minimum_indicators"] or missing_context:
            reasons = []
            if len(present) < model["minimum_indicators"]:
                reasons.append(
                    f"至少需要{model['minimum_indicators']}项相关指标，当前仅有{len(present)}项"
                )
            if missing_context:
                reasons.append(f"缺少必要个体信息：{'、'.join(missing_context)}")
            return ModelResult(
                model_code=model["model_code"],
                model_name=model["model_name"],
                model_version=MODEL_ITEM_VERSION,
                status="INSUFFICIENT_DATA",
                score=None,
                risk_level="INSUFFICIENT_DATA",
                data_completeness=completeness,
                confidence="LOW",
                evidence=["；".join(reasons)],
                supporting_indicators=present,
                missing_indicators=[*missing, *missing_context],
                recommendations=["补充必要数据后再完成该评估维度"],
            )

        evidence: list[str] = []
        total_penalty = 0
        for rule in model["rules"]:
            indicator = values.get(rule["code"])
            if indicator is None:
                continue
            threshold = _rule_threshold(indicator, rule)
            if threshold is None:
                continue
            if _fires(indicator.value, rule["condition"], threshold):
                evidence.append(
                    f"{rule['evidence']}（{rule['code']}={indicator.value} {indicator.unit}）"
                )
                total_penalty += rule["penalty"]

        score = max(0, min(100, model["base_score"] - total_penalty))
        # Any laboratory value outside its confirmed reference interval deserves a visible
        # follow-up flag.  The previous score-only cut-off marked a single confirmed
        # abnormality (for example bilirubin above the laboratory upper limit) as LOW.
        risk_level = "HIGH" if score < 60 else "ATTENTION" if evidence else "LOW"
        confidence: Literal["HIGH", "MEDIUM", "LOW"] = "HIGH" if completeness >= 80 else "MEDIUM"
        if not evidence:
            evidence = ["已提供指标未触发该评估维度关注规则"]
        evidence.extend(self._context_evidence(model["model_code"], context))

        return ModelResult(
            model_code=model["model_code"],
            model_name=model["model_name"],
            model_version=MODEL_ITEM_VERSION,
            status="EVALUATED",
            score=score,
            risk_level=risk_level,
            data_completeness=completeness,
            confidence=confidence,
            evidence=evidence,
            supporting_indicators=present,
            missing_indicators=missing,
            recommendations=model["recommendations"],
        )

    @staticmethod
    def _context_evidence(model_code: str, context: PatientContext | None) -> list[str]:
        if context is None:
            return []
        evidence: list[str] = []
        if model_code == "GLUCOSE_METABOLISM":
            if context.diabetes_status == "YES":
                evidence.append("已填写糖尿病既往诊断")
            if context.family_history:
                evidence.append("已纳入家族病史信息")
            if context.bmi is not None:
                evidence.append(f"BMI 为 {context.bmi}")
        elif model_code == "LIPID_CARDIOVASCULAR":
            if context.hypertension_status == "YES":
                evidence.append("已填写高血压既往诊断")
            if context.dyslipidemia_status == "YES":
                evidence.append("已填写血脂异常既往诊断")
            if context.smoking_status == "CURRENT":
                evidence.append("当前吸烟情况已纳入评估")
        elif model_code == "LIVER_METABOLIC" and context.fatty_liver_status == "YES":
            evidence.append("已填写脂肪肝既往诊断")
        elif model_code == "BODY_COMPOSITION":
            if context.height_cm is not None:
                evidence.append(f"身高：{context.height_cm} cm")
            if context.weight_kg is not None:
                evidence.append(f"体重：{context.weight_kg} kg")
            if context.bmi is not None:
                evidence.append(f"BMI：{context.bmi}")
            if context.waist_cm is not None:
                evidence.append(f"腰围：{context.waist_cm} cm")
            if context.recent_weight_change_kg is not None:
                evidence.append(f"近三个月体重变化：{context.recent_weight_change_kg} kg")
            if context.exercise_frequency:
                evidence.append(f"运动频率：{context.exercise_frequency}")
        elif model_code == "HPA_ADRENAL":
            if context.sleep_quality:
                evidence.append(f"睡眠质量：{context.sleep_quality}")
        elif model_code == "MENTAL_EMOTIONAL":
            for label, value in (
                ("压力水平", context.stress_level),
                ("近期心情", context.mood_status),
                ("恐惧或焦虑感受", context.fear_level),
            ):
                if value:
                    evidence.append(f"{label}：{value}")
        elif model_code == "NUTRITION_MICRONUTRIENT":
            if context.recent_dietary_pattern:
                evidence.append("已纳入近三周饮食结构")
            elif context.dietary_preference:
                evidence.append("已纳入日常饮食偏好")
        return evidence


# Kept as a compatibility alias for existing imports.
DemoRuleEngine = HealthRuleEngine
