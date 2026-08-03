import json
from decimal import Decimal
from typing import Any

from app.clinical.timeline import ClinicalContextBuilder
from app.interpretation.service import (
    DeepSeekSettings,
    InterpretationService,
)
from app.knowledge.service import (
    KNOWLEDGE_BASE_VERSION,
    MedicalKnowledgeRetriever,
)
from app.schemas.assessment import (
    AssessmentRequest,
    ModelResult,
    PatientContext,
)
from app.schemas.indicator import IndicatorInput
from app.schemas.ocr import OcrFinding
from app.scoring.engine import HealthRuleEngine


def _request() -> AssessmentRequest:
    return AssessmentRequest(
        taskId="TASK_SENSITIVE",
        patientId="PATIENT_SENSITIVE",
        indicators=[
            IndicatorInput(
                code="fasting_glucose",
                name="空腹血糖",
                value=Decimal("6.4"),
                unit="mmol/L",
                referenceLow=Decimal("3.9"),
                referenceHigh=Decimal("6.1"),
            ),
            IndicatorInput(
                code="hba1c",
                name="糖化血红蛋白",
                value=Decimal("5.9"),
                unit="%",
                referenceLow=Decimal("4.0"),
                referenceHigh=Decimal("6.0"),
            ),
        ],
        findings=[
            OcrFinding(
                section="上腹部彩超（肝胆胰脾）",
                item="肝脏",
                result="肝实质回声细密增强，分布均匀。",
            ),
            OcrFinding(
                section="上腹部彩超（肝胆胰脾）",
                item="检查小结",
                result="脂肪肝；胆囊壁稍强回声（考虑息肉样变）；胆囊壁毛糙。",
            ),
        ],
        patientContext=PatientContext(
            gender="FEMALE",
            age=38,
            heightCm=Decimal("165"),
            weightKg=Decimal("66"),
            lifestyleSummary="久坐办公，工作日常在外就餐",
            familyHistory="父亲有糖尿病",
            allergyHistory="青霉素过敏",
            currentMedications="正在服用医生开具的降压药",
            sleepQuality="POOR",
            stressLevel="MEDIUM",
        ),
    )


def _results() -> list[ModelResult]:
    return [
        ModelResult(
            modelCode="GLUCOSE_METABOLISM",
            modelName="糖代谢健康",
            status="EVALUATED",
            score=78,
            riskLevel="ATTENTION",
            dataCompleteness=80,
            confidence="HIGH",
            evidence=["空腹血糖高于本次报告参考上限"],
            supportingIndicators=["fasting_glucose", "hba1c"],
            missingIndicators=[],
            recommendations=["结合饮食、运动和体重变化持续观察"],
        )
    ]


def test_knowledge_retriever_returns_relevant_versioned_references() -> None:
    references = MedicalKnowledgeRetriever().retrieve(_request(), _results())
    reference_ids = {item.reference_id for item in references}

    assert "NHC-LAB-GENERAL-001" in reference_ids
    assert "NHC-HYPERGLYCEMIA-2024-001" in reference_ids
    assert "WHO-MENTAL-HEALTH-001" not in reference_ids
    assert len(references) <= 6
    assert all(item.authority_level == "A" for item in references)
    assert any(item.retrieval_score > 0 for item in references)
    assert all(
        item.to_prompt_dict()["knowledgeBaseVersion"] == KNOWLEDGE_BASE_VERSION
        for item in references
    )


def test_knowledge_corpus_covers_all_health_dimensions() -> None:
    retriever = MedicalKnowledgeRetriever()
    covered_model_codes = {
        model_code for reference in retriever.references for model_code in reference.model_codes
    }
    assert covered_model_codes >= {
        "GLUCOSE_METABOLISM",
        "LIPID_CARDIOVASCULAR",
        "CHRONIC_INFLAMMATION",
        "LIVER_METABOLIC",
        "KIDNEY_ELECTROLYTE",
        "HEMATOLOGY_ANEMIA",
        "THYROID_HORMONE",
        "BODY_COMPOSITION",
        "HPA_ADRENAL",
        "NUTRITION_MICRONUTRIENT",
        "GUT_BARRIER",
        "MENTAL_EMOTIONAL",
    }


def test_assessment_rag_uses_only_abnormal_and_attention_topics() -> None:
    request = AssessmentRequest(
        taskId="TASK",
        patientId="PATIENT",
        indicators=[
            IndicatorInput(
                code="total_cholesterol",
                name="总胆固醇",
                value=Decimal("5.99"),
                unit="mmol/L",
                referenceHigh=Decimal("5.20"),
            )
        ],
        patientContext=PatientContext(
            bmi=Decimal("28.91"),
            stressLevel="HIGH",
            exerciseFrequency="1_2_PER_WEEK",
            recentDietaryPattern="近期外卖较多",
        ),
    )
    results = [
        ModelResult(
            modelCode="LIPID_CARDIOVASCULAR",
            modelName="血脂与心血管代谢",
            status="EVALUATED",
            score=80,
            riskLevel="ATTENTION",
            evidence=["总胆固醇高于本次报告参考上限"],
            supportingIndicators=["total_cholesterol"],
            missingIndicators=["ldl", "hdl", "triglyceride"],
            recommendations=["补充完整血脂"],
        ),
        ModelResult(
            modelCode="MENTAL_EMOTIONAL",
            modelName="心理与情绪健康",
            status="EVALUATED",
            score=95,
            riskLevel="LOW",
            evidence=["未触发关注规则"],
            supportingIndicators=[],
            missingIndicators=[],
            recommendations=["通用建议"],
        ),
    ]

    plan = MedicalKnowledgeRetriever._plan_assessment_query(request, results)

    assert plan.model_codes == frozenset({"LIPID_CARDIOVASCULAR"})
    assert plan.abnormal_indicator_codes == frozenset({"total_cholesterol"})
    assert "MENTAL_EMOTIONAL" not in plan.query_text
    assert "心理与情绪健康" not in plan.query_text
    assert "exercise_frequency" in plan.context_fields
    assert "recent_dietary_pattern" in plan.context_fields
    assert "近期外卖较多" in plan.query_text


def test_clinical_timeline_is_deidentified_and_calculates_bmi() -> None:
    timeline = ClinicalContextBuilder().build(_request(), _results())
    serialized = json.dumps(timeline, ensure_ascii=False)

    assert "TASK_SENSITIVE" not in serialized
    assert "PATIENT_SENSITIVE" not in serialized
    assert timeline["anthropometrics"]["calculatedBmi"] == "24.2"
    assert timeline["laboratorySnapshot"]["abnormalCount"] == 1
    assert timeline["abnormalFacts"][0]["displayText"] == (
        "空腹血糖为 6.4 mmol/L，高于本次报告参考上限 6.1 mmol/L。"
    )
    assert timeline["laboratorySnapshot"]["indicators"][0]["referenceStatus"] == "HIGH"
    assert timeline["healthProfileAndQuestionnaire"]["lifestyleSummary"] == (
        "久坐办公，工作日常在外就餐"
    )
    assert timeline["healthProfileAndQuestionnaire"]["allergyHistory"] == "青霉素过敏"
    assert timeline["healthProfileAndQuestionnaire"]["currentMedications"] == (
        "正在服用医生开具的降压药"
    )
    assert timeline["examinationSnapshot"]["sectionCount"] == 1
    assert timeline["examinationSnapshot"]["observationCount"] == 1
    assert timeline["examinationSnapshot"]["summaryCount"] == 1
    assert timeline["analysisFocus"]["abnormalFacts"]
    assert timeline["analysisFocus"]["profileSignals"]
    assert timeline["analysisFocus"]["reportConclusions"][0]["factId"] == ("EXAM:001:SUMMARY:001")
    assert any(fact["factId"] == "EXAM:001:OBS:001" for fact in timeline["patientFacts"])
    assert any(fact["factId"] == "EXAM:001:SUMMARY:001" for fact in timeline["patientFacts"])
    assert any(fact["factId"] == "DERIVED:BMI" for fact in timeline["patientFacts"])


class _FakeResponse:
    def __init__(self, content: dict[str, Any], finish_reason: str = "stop") -> None:
        self.content = content
        self.finish_reason = finish_reason

    def raise_for_status(self) -> None:
        return None

    def json(self) -> dict[str, Any]:
        return {
            "choices": [
                {
                    "finish_reason": self.finish_reason,
                    "message": {"content": json.dumps(self.content, ensure_ascii=False)},
                }
            ]
        }


class _FakeClient:
    def __init__(
        self,
        content: dict[str, Any] | list[dict[str, Any]],
        finish_reasons: list[str] | None = None,
    ) -> None:
        self.contents = content if isinstance(content, list) else [content]
        self.finish_reasons = finish_reasons or ["stop"]
        self.call_count = 0
        self.last_payload: dict[str, Any] | None = None

    def post(self, *args: Any, **kwargs: Any) -> _FakeResponse:
        self.last_payload = kwargs["json"]
        index = self.call_count
        content = self.contents[min(index, len(self.contents) - 1)]
        finish_reason = self.finish_reasons[min(index, len(self.finish_reasons) - 1)]
        self.call_count += 1
        return _FakeResponse(content, finish_reason)


def _generated_content(summary: str = "当前存在糖代谢风险信号，建议持续观察。") -> dict[str, Any]:
    return {
        "summary": summary,
        "priorityConcerns": ["空腹血糖高于本次报告参考上限"],
        "crossModelFindings": [
            {
                "title": "糖代谢指标需关注",
                "indicatorCodes": ["fasting_glucose", "hba1c"],
                "patientFactIds": ["LAB:fasting_glucose", "LAB:hba1c"],
                "evidenceIds": ["NHC-HYPERGLYCEMIA-2024-001"],
                "explanation": "空腹血糖异常，需要结合后续复测观察。",
            }
        ],
        "diagnosticReferences": [],
        "recommendations": ["保持规律饮食与运动，并记录体重变化"],
        "missingDataAdvice": [],
        "followupQuestions": ["近期饮食和体重是否有明显变化？"],
        "redFlags": [],
        "uncertainty": "本次结果需要结合复测和完整临床资料综合判断。",
    }


def _service(client: _FakeClient) -> InterpretationService:
    return InterpretationService(
        settings=DeepSeekSettings(
            enabled=True,
            api_key="test-key",
            base_url="https://example.invalid",
            model="deepseek-v4-flash",
            timeout_seconds=1,
            max_tokens=2000,
            thinking_enabled=False,
        ),
        client=client,  # type: ignore[arg-type]
    )


def test_vertical_prompt_contains_grounding_without_direct_identifiers() -> None:
    fake = _FakeClient(_generated_content())
    result = _service(fake).interpret(_request(), _results())

    assert result.status == "SUCCESS"
    assert fake.last_payload is not None
    user_message = json.loads(fake.last_payload["messages"][1]["content"])
    serialized = json.dumps(user_message["data"], ensure_ascii=False)
    assert "healthTimeline" in user_message["data"]
    assert "evidenceBundle" in user_message["data"]
    assert user_message["data"]["evidenceBundle"]["knowledgeBaseVersion"] == KNOWLEDGE_BASE_VERSION
    assert user_message["data"]["evidenceBundle"]["evidence"]
    assert "patientFacts" in user_message["data"]["healthTimeline"]
    assert "examinationSnapshot" in user_message["data"]["healthTimeline"]
    assert "久坐办公，工作日常在外就餐" in serialized
    assert "青霉素过敏" in serialized
    assert "正在服用医生开具的降压药" in serialized
    assert "上腹部彩超（肝胆胰脾）" in serialized
    assert "肝实质回声细密增强" in serialized
    assert "考虑息肉样变" in serialized
    assert "TASK_SENSITIVE" not in serialized
    assert "PATIENT_SENSITIVE" not in serialized
    assert fake.last_payload["thinking"] == {"type": "disabled"}


def test_truncated_deepseek_output_is_retried_with_repair_instruction() -> None:
    fake = _FakeClient(
        [_generated_content(), _generated_content("修复后的精炼综合解读。")],
        finish_reasons=["length", "stop"],
    )

    result = _service(fake).interpret(_request(), _results())

    assert result.status == "SUCCESS"
    assert result.source == "DEEPSEEK"
    assert result.generation_attempts == 2
    assert result.fallback_reason is None
    assert fake.call_count == 2
    assert fake.last_payload is not None
    repaired_message = json.loads(fake.last_payload["messages"][1]["content"])
    assert "finish_reason:length" in repaired_message["repairInstruction"]


def test_unsafe_medication_or_diagnosis_output_falls_back() -> None:
    fake = _FakeClient(_generated_content("已确诊为糖尿病，建议每日2次口服500mg药物。"))
    result = _service(fake).interpret(_request(), _results())

    assert result.status == "FALLBACK"
    assert result.source == "RULE_FALLBACK"


def test_internal_enums_or_unverified_bmi_label_fall_back() -> None:
    fake = _FakeClient(_generated_content("当前糖代谢状态为ATTENTION，BMI 24.2（超重）。"))
    result = _service(fake).interpret(_request(), _results())

    assert result.status == "FALLBACK"
    assert result.source == "RULE_FALLBACK"


def test_unknown_rag_evidence_citation_drops_untraceable_finding() -> None:
    content = _generated_content()
    content["crossModelFindings"][0]["evidenceIds"] = ["UNKNOWN-EVIDENCE"]
    fake = _FakeClient(content)

    result = _service(fake).interpret(_request(), _results())

    assert result.status == "SUCCESS"
    assert result.source == "DEEPSEEK"
    assert result.cross_model_findings == []


def test_unknown_indicator_citation_is_removed_without_discarding_valid_finding() -> None:
    content = _generated_content()
    content["crossModelFindings"][0]["indicatorCodes"].append("hallucinated_indicator")
    fake = _FakeClient(content)

    result = _service(fake).interpret(_request(), _results())

    assert result.status == "SUCCESS"
    assert result.source == "DEEPSEEK"
    assert result.cross_model_findings[0].indicator_codes == ["fasting_glucose", "hba1c"]


def test_single_mild_abnormality_does_not_force_diagnostic_references() -> None:
    fake = _FakeClient(_generated_content())

    result = _service(fake).interpret(_request(), _results())

    assert fake.call_count == 1
    assert result.status == "SUCCESS"
    assert result.diagnostic_references == []


def test_single_abnormal_fact_stays_a_priority_concern_without_disease_candidate() -> None:
    content = _generated_content()
    fake = _FakeClient(content)

    result = _service(fake).interpret(_request(), _results())

    assert result.status == "SUCCESS"
    assert result.priority_concerns == ["空腹血糖为 6.4 mmol/L，高于本次报告参考上限 6.1 mmol/L。"]
    assert result.diagnostic_references == []


def test_glucose_requires_a_core_indicator() -> None:
    request = AssessmentRequest(
        taskId="TASK",
        patientId="PATIENT",
        indicators=[
            IndicatorInput(
                code="triglyceride", name="甘油三酯", value=Decimal("2.0"), unit="mmol/L"
            ),
            IndicatorInput(code="hdl", name="高密度脂蛋白", value=Decimal("0.8"), unit="mmol/L"),
        ],
        modelCodes=["GLUCOSE_METABOLISM"],
    )

    result = HealthRuleEngine().evaluate(request, request.model_codes)[0]

    assert result.status == "INSUFFICIENT_DATA"
    assert result.risk_level == "INSUFFICIENT_DATA"
    assert not any("糖代谢异常" in item for item in result.evidence)


def test_bmi_rules_are_mutually_exclusive_and_waist_is_not_guessed() -> None:
    request = AssessmentRequest(
        taskId="TASK",
        patientId="PATIENT",
        indicators=[],
        modelCodes=["BODY_COMPOSITION"],
        patientContext=PatientContext(
            gender="UNKNOWN", bmi=Decimal("28.91"), exerciseFrequency="3_5_PER_WEEK"
        ),
    )

    result = HealthRuleEngine().evaluate(request, request.model_codes)[0]

    bmi_evidence = [item for item in result.evidence if item.startswith("身体质量指数为")]
    assert len(bmi_evidence) == 1
    assert "28.91" in bmi_evidence[0]
    assert all("腹型肥胖" not in item for item in result.evidence)
    assert "waist_risk_score" not in result.supporting_indicators


def test_camera_facts_are_marked_supplementary_and_non_diagnostic() -> None:
    request = _request().model_copy(
        update={
            "patient_context": _request().patient_context.model_copy(
                update={"camera_systolic_blood_pressure": Decimal("145")}
            )
        }
    )
    timeline = ClinicalContextBuilder().build(request, _results())
    camera_fact = next(
        fact for fact in timeline["patientFacts"] if fact["factId"].startswith("FACE:")
    )

    assert camera_fact["sourceType"] == "FACE_CAMERA_ESTIMATION"
    assert camera_fact["evidenceLevel"] == "SUPPLEMENTARY"
    assert camera_fact["usableForDiagnosis"] is False
    assert "不能替代医疗设备测量" in camera_fact["limitation"]


def test_disabled_deepseek_fallback_preserves_single_abnormal_fact_and_omits_low_advice() -> None:
    request = AssessmentRequest(
        taskId="TASK",
        patientId="PATIENT",
        indicators=[
            IndicatorInput(
                code="total_cholesterol",
                name="总胆固醇",
                value=Decimal("5.99"),
                unit="mmol/L",
                referenceHigh=Decimal("5.20"),
            )
        ],
        patientContext=PatientContext(
            gender="MALE",
            age=60,
            bmi=Decimal("28.91"),
            cameraSystolicBloodPressure=Decimal("138"),
        ),
    )
    results = [
        ModelResult(
            modelCode="LIPID_CARDIOVASCULAR",
            modelName="血脂与心血管代谢",
            status="INSUFFICIENT_DATA",
            score=None,
            riskLevel="INSUFFICIENT_DATA",
            evidence=["至少需要2项相关指标，当前仅有1项"],
            supportingIndicators=["total_cholesterol"],
            missingIndicators=["ldl", "hdl", "triglyceride"],
            recommendations=["补充必要数据后再完成血脂评估"],
        ),
        ModelResult(
            modelCode="HPA_ADRENAL",
            modelName="睡眠与恢复",
            status="EVALUATED",
            score=90,
            riskLevel="LOW",
            evidence=["已提供指标未触发该评估维度关注规则"],
            supportingIndicators=["sleep_hours"],
            missingIndicators=[],
            recommendations=["这条LOW维度建议不应进入报告"],
        ),
    ]
    service = InterpretationService(
        settings=DeepSeekSettings(
            enabled=False,
            api_key="",
            base_url="https://example.invalid",
            model="deepseek-v4-flash",
            timeout_seconds=1,
            max_tokens=2000,
            thinking_enabled=False,
        )
    )

    result = service.interpret(request, results)
    serialized = json.dumps(result.model_dump(by_alias=True), ensure_ascii=False)

    assert result.source == "RULE_FALLBACK"
    assert result.diagnostic_references == []
    assert any("总胆固醇为 5.99" in item for item in result.priority_concerns)
    assert any("BMI为 28.91" in item for item in result.priority_concerns)
    assert any("LDL-C、HDL-C和甘油三酯" in item for item in result.recommendations)
    assert all("LOW维度建议" not in item for item in result.recommendations)
    assert "评估维度" not in serialized
    assert "total_cholesterol" not in serialized
    assert "waist_risk_score" not in serialized
    assert "腹型肥胖" not in result.summary
    assert "糖代谢异常" not in serialized
    assert "健康拍结果不能替代医疗设备测量" in result.uncertainty


def test_deepseek_success_accepts_fixed_case_without_forced_disease_candidate() -> None:
    request = AssessmentRequest(
        taskId="TASK",
        patientId="PATIENT",
        indicators=[
            IndicatorInput(
                code="total_cholesterol",
                name="总胆固醇",
                value=Decimal("5.99"),
                unit="mmol/L",
                referenceHigh=Decimal("5.20"),
            )
        ],
        patientContext=PatientContext(
            gender="MALE",
            age=60,
            bmi=Decimal("28.91"),
            cameraSystolicBloodPressure=Decimal("138"),
        ),
    )
    results = [
        ModelResult(
            modelCode="LIPID_CARDIOVASCULAR",
            modelName="血脂与心血管代谢",
            status="INSUFFICIENT_DATA",
            score=None,
            riskLevel="INSUFFICIENT_DATA",
            evidence=["至少需要2项相关指标，当前仅有1项"],
            supportingIndicators=["total_cholesterol"],
            missingIndicators=["ldl", "hdl", "triglyceride"],
            recommendations=["补充必要数据后再完成血脂评估"],
        )
    ]
    content = {
        "summary": (
            "本次主要需要关注体重和血脂健康。总胆固醇高于本次报告参考上限，"
            "BMI处于需要关注的范围。由于缺少完整血脂、腰围、正规血压和糖代谢指标，"
            "目前不能完成完整风险评估。"
        ),
        "priorityConcerns": [
            "总胆固醇为 5.99 mmol/L，高于本次报告参考上限 5.20 mmol/L。",
            "BMI为 28.91 kg/m²，体重管理需要关注。",
        ],
        "crossModelFindings": [
            {
                "title": "单项血脂结果需要补充核对",
                "indicatorCodes": ["total_cholesterol"],
                "patientFactIds": ["LAB:total_cholesterol"],
                "evidenceIds": ["NHC-LAB-GENERAL-001"],
                "explanation": "当前只有总胆固醇结果，不能完成完整血脂评估。",
            }
        ],
        "diagnosticReferences": [],
        "recommendations": [
            "核对LDL-C、HDL-C和甘油三酯，以完成血脂评估。",
            "记录体重变化并补充腰围，以评估体重管理效果。",
            "使用正规设备复核血压，因为健康拍仅供趋势参考。",
        ],
        "missingDataAdvice": ["补充完整血脂、腰围和糖代谢核心指标。"],
        "followupQuestions": [],
        "redFlags": [],
        "uncertainty": (
            "不能仅根据总胆固醇判断冠心病，不能仅根据BMI判断腹型肥胖，"
            "健康拍结果不能替代医疗设备测量。"
        ),
    }
    fake = _FakeClient(content)

    result = _service(fake).interpret(request, results)
    user_text = "\n".join(
        [
            result.summary,
            *result.priority_concerns,
            *(item.explanation for item in result.cross_model_findings),
            *result.recommendations,
            *result.missing_data_advice,
            result.uncertainty,
        ]
    )

    assert result.status == "SUCCESS"
    assert result.diagnostic_references == []
    assert "评估维度" not in user_text
    assert "total_cholesterol" not in user_text
    assert "糖代谢异常" not in user_text
    assert "腹型肥胖" not in result.summary
