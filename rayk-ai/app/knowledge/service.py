from dataclasses import dataclass

from app.schemas.assessment import AssessmentRequest, ModelResult

KNOWLEDGE_BASE_VERSION = "ZHIYU_MEDICAL_KB_1.0.0"


@dataclass(frozen=True)
class MedicalKnowledgeReference:
    reference_id: str
    title: str
    guidance: str
    source_name: str
    source_url: str
    model_codes: frozenset[str] = frozenset()
    indicator_codes: frozenset[str] = frozenset()
    context_fields: frozenset[str] = frozenset()

    def to_prompt_dict(self) -> dict[str, str]:
        return {
            "referenceId": self.reference_id,
            "title": self.title,
            "guidance": self.guidance,
            "sourceName": self.source_name,
            "sourceUrl": self.source_url,
            "knowledgeBaseVersion": KNOWLEDGE_BASE_VERSION,
        }


_GENERAL = MedicalKnowledgeReference(
    reference_id="GENERAL_LAB_INTERPRETATION",
    title="检验结果综合解释原则",
    guidance=(
        "以本次检验机构提供的参考区间为首要依据；单项轻度异常应结合复测、采样状态、"
        "症状、既往史和相关指标综合判断。正常结果不排除所有疾病，异常结果也不等于确诊。"
    ),
    source_name="国家卫生健康委员会",
    source_url="https://www.nhc.gov.cn/",
)

_SAFETY = MedicalKnowledgeReference(
    reference_id="CLINICAL_SAFETY_BOUNDARY",
    title="健康评估安全边界",
    guidance=(
        "输出用于健康管理和临床辅助参考，不可替代面诊、体格检查或医生诊断；出现明确危险信号时，"
        "只提示及时就医或医生优先复核，不提供药物剂量、停药或治疗方案。"
    ),
    source_name="世界卫生组织",
    source_url="https://www.who.int/health-topics/digital-health",
)

_REFERENCES: tuple[MedicalKnowledgeReference, ...] = (
    MedicalKnowledgeReference(
        "GLUCOSE_METABOLISM",
        "糖代谢健康评估",
        "结合空腹血糖、糖化血红蛋白、胰岛素、体重变化、家族史与生活方式判断糖代谢风险信号；单次结果不用于确诊。",
        "世界卫生组织",
        "https://www.who.int/health-topics/diabetes",
        frozenset({"GLUCOSE_METABOLISM"}),
        frozenset({"fasting_glucose", "fasting_insulin", "hba1c"}),
        frozenset({"diabetes_status", "family_history", "recent_weight_change_kg"}),
    ),
    MedicalKnowledgeReference(
        "CARDIOVASCULAR_LIPID",
        "心血管与血脂健康评估",
        "血脂指标需结合血压、吸烟、糖代谢、家族史和既往心血管情况综合评估，不依据单个血脂结果推断疾病。",
        "世界卫生组织",
        "https://www.who.int/health-topics/cardiovascular-diseases",
        frozenset({"LIPID_CARDIOVASCULAR"}),
        frozenset({"total_cholesterol", "ldl", "hdl", "triglyceride", "apob", "lpa"}),
        frozenset({"hypertension_status", "smoking_status", "family_history"}),
    ),
    MedicalKnowledgeReference(
        "LIVER_METABOLIC",
        "肝脏与代谢健康评估",
        "肝酶、胆红素和蛋白指标应结合饮酒、用药、体重、既往史及必要的影像资料解释；轻度异常需关注复测与变化趋势。",
        "世界卫生组织",
        "https://www.who.int/health-topics/hepatitis",
        frozenset({"LIVER_METABOLIC"}),
        frozenset({"alt", "ast", "ggt", "total_bilirubin", "direct_bilirubin", "albumin"}),
        frozenset({"alcohol_status", "fatty_liver_status", "medical_history"}),
    ),
    MedicalKnowledgeReference(
        "KIDNEY_ELECTROLYTE",
        "肾脏与电解质健康评估",
        "肌酐、估算肾小球滤过率、尿素和电解质应结合年龄、体液状态、既往病史与复测结果解释；明显电解质异常需优先复核。",
        "世界卫生组织",
        "https://www.who.int/news-room/fact-sheets/detail/the-top-10-causes-of-death",
        frozenset({"KIDNEY_ELECTROLYTE"}),
        frozenset(
            {
                "creatinine",
                "egfr",
                "urea",
                "uric_acid",
                "sodium",
                "potassium",
                "chloride",
                "bicarbonate",
            }
        ),
        frozenset({"age", "medical_history", "hypertension_status", "diabetes_status"}),
    ),
    MedicalKnowledgeReference(
        "HEMATOLOGY_NUTRITION",
        "血液与营养健康评估",
        "血细胞、血红蛋白、铁代谢、维生素和蛋白指标需结合性别、年龄、饮食结构、失血线索与炎症状态综合判断。",
        "世界卫生组织",
        "https://www.who.int/health-topics/anaemia",
        frozenset({"HEMATOLOGY_ANEMIA", "NUTRITION_MICRONUTRIENT"}),
        frozenset({"hemoglobin", "rbc", "mcv", "ferritin", "vitamin_b12", "folate", "albumin"}),
        frozenset({"gender", "age", "dietary_preference", "recent_dietary_pattern"}),
    ),
    MedicalKnowledgeReference(
        "THYROID_ENDOCRINE",
        "甲状腺与内分泌健康评估",
        "甲状腺功能结果需按指标组合、实验室参考区间、症状和既往史综合解释，不以单项指标作疾病结论。",
        "世界卫生组织",
        "https://www.who.int/health-topics/endocrine-disrupting-chemicals",
        frozenset({"THYROID_HORMONE"}),
        frozenset({"tsh", "ft3", "ft4", "t3", "t4"}),
        frozenset({"medical_history", "family_history"}),
    ),
    MedicalKnowledgeReference(
        "INFLAMMATION_IMMUNE",
        "炎症与免疫健康评估",
        "炎症指标可受感染、运动、采样状态和慢性情况影响，非特异性升高只能作为风险信号，需结合症状与复测。",
        "世界卫生组织",
        "https://www.who.int/health-topics/infectious-diseases",
        frozenset({"CHRONIC_INFLAMMATION"}),
        frozenset({"crp", "hs_crp", "esr", "wbc", "ferritin"}),
        frozenset({"medical_history", "sleep_quality", "stress_level"}),
    ),
    MedicalKnowledgeReference(
        "BODY_COMPOSITION_ACTIVITY",
        "体重与身体成分健康评估",
        "身高、体重、腰围、体重变化和运动情况共同反映身体成分风险；BMI是筛查指标，不能单独代表个体健康状态。",
        "世界卫生组织",
        "https://www.who.int/health-topics/obesity",
        frozenset({"BODY_COMPOSITION"}),
        frozenset(),
        frozenset(
            {
                "height_cm",
                "weight_kg",
                "waist_cm",
                "recent_weight_change_kg",
                "exercise_frequency",
            }
        ),
    ),
    MedicalKnowledgeReference(
        "SLEEP_STRESS_MOOD",
        "睡眠、压力与情绪健康评估",
        "睡眠、压力、情绪和恐惧感应结合持续时间、日常功能影响和变化趋势解释；问卷信息用于健康管理，不等同于精神疾病诊断。",
        "世界卫生组织",
        "https://www.who.int/health-topics/mental-health",
        frozenset({"HPA_ADRENAL", "MENTAL_EMOTIONAL"}),
        frozenset(),
        frozenset({"sleep_quality", "sleep_hours", "stress_level", "mood_status", "fear_level"}),
    ),
    MedicalKnowledgeReference(
        "DIET_DIGESTIVE",
        "饮食与消化健康评估",
        "饮食结构应关注持续模式、食物多样性、能量平衡和个体耐受，结合消化相关指标与既往情况制定可执行的健康建议。",
        "世界卫生组织",
        "https://www.who.int/news-room/fact-sheets/detail/healthy-diet",
        frozenset({"GUT_BARRIER", "NUTRITION_MICRONUTRIENT"}),
        frozenset(),
        frozenset({"dietary_preference", "recent_dietary_pattern", "medical_history"}),
    ),
)


class MedicalKnowledgeRetriever:
    def retrieve(
        self,
        request: AssessmentRequest,
        results: list[ModelResult],
        limit: int = 8,
    ) -> list[MedicalKnowledgeReference]:
        indicator_codes = {item.code for item in request.indicators if item.code}
        evaluated_models = {item.model_code for item in results if item.status == "EVALUATED"}
        context_values = (
            request.patient_context.model_dump(exclude_none=True)
            if request.patient_context is not None
            else {}
        )
        populated_context = {
            key
            for key, value in context_values.items()
            if value not in {"", "UNKNOWN", None} and value != []
        }

        ranked: list[tuple[int, MedicalKnowledgeReference]] = []
        for reference in _REFERENCES:
            score = (
                len(reference.model_codes & evaluated_models) * 5
                + len(reference.indicator_codes & indicator_codes) * 2
                + len(reference.context_fields & populated_context)
            )
            if score:
                ranked.append((score, reference))
        ranked.sort(key=lambda item: (-item[0], item[1].reference_id))
        selected = [_GENERAL, *[item[1] for item in ranked[: max(0, limit - 2)]], _SAFETY]
        return list(dict.fromkeys(selected))
