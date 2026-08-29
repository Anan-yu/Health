import json
from decimal import Decimal
from typing import Any

import httpx

from app.clinical.timeline import ClinicalContextBuilder
from app.interpretation.service import (
    DeepSeekSettings,
    InterpretationService,
)
from app.interpretation.qwen_vision import QwenVisionSettings
from app.knowledge.service import (
    KNOWLEDGE_BASE_VERSION,
    MedicalKnowledgeRetriever,
)
from app.schemas.assessment import (
    AssessmentRequest,
    ModelResult,
    PatientContext,
    ReportImage,
    VisionImageAnalysis,
    VisionImageFinding,
    VisionImagePage,
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


def test_knowledge_retriever_returns_helicobacter_treatment_guideline() -> None:
    request = AssessmentRequest(
        taskId="TASK_HP",
        patientId="PATIENT_HP",
        indicators=[],
        findings=[
            OcrFinding(
                section="呼气试验",
                item="检查小结",
                result="C14呼气试验阳性，提示幽门螺杆菌感染可能。",
            )
        ],
    )
    results = [
        ModelResult(
            modelCode="GUT_BARRIER",
            modelName="消化与肠道健康",
            status="EVALUATED",
            score=70,
            riskLevel="ATTENTION",
            evidence=["C14呼气试验结果需要结合消化专科复核"],
            supportingIndicators=[],
            missingIndicators=[],
            recommendations=["建议消化内科评估。"],
        )
    ]

    reference_ids = {
        item.reference_id for item in MedicalKnowledgeRetriever().retrieve(request, results)
    }

    assert "CSGE-HP-2022-001" in reference_ids
    assert "TCM-HP-2026-001" in reference_ids


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
    assert timeline["analysisFocus"]["diagnosticSummaryFacts"] == (
        timeline["analysisFocus"]["reportConclusions"]
    )
    assert "考虑息肉样变" in timeline["analysisFocus"]["diagnosticSummaryFacts"][0]["result"]
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
            max_attempts=3,
            retry_backoff_seconds=0,
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
    assert "diagnosticSummaryFacts" in user_message["data"]["healthTimeline"]["analysisFocus"]
    assert "久坐办公，工作日常在外就餐" in serialized
    assert "青霉素过敏" in serialized
    assert "正在服用医生开具的降压药" in serialized
    assert "上腹部彩超（肝胆胰脾）" in serialized
    assert "肝实质回声细密增强" in serialized
    assert "考虑息肉样变" in serialized
    assert "TASK_SENSITIVE" not in serialized
    assert "PATIENT_SENSITIVE" not in serialized
    assert fake.last_payload["thinking"] == {"type": "disabled"}


class _FakeVisionClient:
    def __init__(self, content: dict[str, Any]) -> None:
        self.content = content
        self.last_system_prompt: str | None = None
        self.last_user_payload: dict[str, Any] | None = None
        self.last_report_images: list[ReportImage] | None = None
        self.image_analysis_calls = 0
        self.synthesis_calls = 0
        self.seen_image_pages: list[list[int]] = []

    def generate(self, **kwargs: Any) -> str:
        self.image_analysis_calls += 1
        self.last_system_prompt = kwargs["system_prompt"]
        self.last_user_payload = kwargs["user_payload"]
        self.last_report_images = kwargs["report_images"]
        self.seen_image_pages.append([image.page for image in self.last_report_images])
        return json.dumps(
            {
                "pages": [
                    {
                        "page": image.page,
                        "pageSummary": f"第{image.page}页体检事实",
                        "findings": [
                            {
                                "category": "检验",
                                "item": "空腹血糖",
                                "result": "6.4",
                                "unit": "mmol/L",
                                "referenceRange": "3.9-6.1",
                                "abnormalFlag": "高",
                            }
                        ],
                        "uncertainties": [],
                    }
                    for image in self.last_report_images
                ]
            },
            ensure_ascii=False,
        )

    def generate_text(self, **kwargs: Any) -> str:
        self.synthesis_calls += 1
        self.last_system_prompt = kwargs["system_prompt"]
        self.last_user_payload = kwargs["user_payload"]
        return json.dumps(self.content, ensure_ascii=False)


def test_qwen_image_stage_feeds_deepseek_final_without_signed_urls_in_text() -> None:
    request = _request().model_copy(
        update={
            "model": "deepseek-v4-pro",
            "report_images": [
                ReportImage(
                    page=1,
                    mimeType="image/jpeg",
                    downloadUrl="https://minio.internal/reports/page-1?X-Amz-Signature=secret",
                ),
                ReportImage(
                    page=2,
                    mimeType="image/png",
                    downloadUrl="https://minio.internal/reports/page-2?X-Amz-Signature=secret",
                ),
            ]
        }
    )
    vision = _FakeVisionClient(_generated_content())
    deepseek = _FakeClient(_generated_content())
    service = InterpretationService(
        settings=DeepSeekSettings(
            enabled=True,
            api_key="test-deepseek-key",
            base_url="https://example.invalid",
            model="deepseek-v4-flash",
            timeout_seconds=1,
            max_tokens=2000,
            thinking_enabled=False,
        ),
        vision_settings=QwenVisionSettings(
            enabled=True,
            api_key="test-qwen-key",
            workspace_id="",
            base_url="https://example.invalid/v1",
            model="qwen3.8-flash",
            timeout_seconds=1,
            max_tokens=2000,
            max_images=50,
        ),
        client=deepseek,  # type: ignore[arg-type]
        vision_client=vision,  # type: ignore[arg-type]
    )

    result = service.interpret(request, _results())

    assert result.status == "SUCCESS"
    assert result.source == "DEEPSEEK"
    assert result.model == "deepseek-v4-pro"
    assert vision.image_analysis_calls == 1
    assert vision.synthesis_calls == 0
    assert vision.last_report_images is not None
    assert [image.download_url for image in vision.last_report_images] == [
        "https://minio.internal/reports/page-1?X-Amz-Signature=secret",
        "https://minio.internal/reports/page-2?X-Amz-Signature=secret",
    ]
    assert deepseek.last_payload is not None
    assert deepseek.last_payload["model"] == "deepseek-v4-pro"
    final_payload = json.loads(deepseek.last_payload["messages"][1]["content"])
    serialized = json.dumps(final_payload, ensure_ascii=False)
    assert "IMAGE:PAGE:1" in serialized
    assert "IMAGE:PAGE:2" in serialized
    assert "X-Amz-Signature=secret" not in serialized
    assert "VISION_FACTS_TO_REPORT_SYNTHESIS" in serialized
    assert "健康档案、健康拍和Qwen图片事实必须联合分析" in serialized
    assert "第1页体检事实" in serialized
    assert "imageAnalysis" in final_payload


def test_qwen_image_batches_all_pages_before_deepseek_report_synthesis() -> None:
    request = _request().model_copy(
        update={
            "report_images": [
                ReportImage(
                    page=page,
                    mimeType="image/jpeg",
                    downloadUrl=f"https://minio.internal/reports/page-{page}",
                )
                for page in range(1, 6)
            ]
        }
    )
    vision = _FakeVisionClient(_generated_content())
    deepseek = _FakeClient(_generated_content())
    service = InterpretationService(
        settings=DeepSeekSettings(
            enabled=True,
            api_key="test-deepseek-key",
            base_url="https://example.invalid",
            model="deepseek-v4-flash",
            timeout_seconds=1,
            max_tokens=2000,
            thinking_enabled=False,
        ),
        vision_settings=QwenVisionSettings(
            enabled=True,
            api_key="test-qwen-key",
            workspace_id="",
            base_url="https://example.invalid/v1",
            model="qwen3.8-flash",
            timeout_seconds=1,
            max_tokens=2000,
            max_images=50,
            image_batch_size=2,
            image_batch_attempts=1,
        ),
        client=deepseek,  # type: ignore[arg-type]
        vision_client=vision,  # type: ignore[arg-type]
    )

    result = service.interpret(request, _results())

    assert result.status == "SUCCESS"
    assert vision.seen_image_pages == [[1, 2], [3, 4], [5]]
    assert vision.synthesis_calls == 0
    assert deepseek.last_payload is not None
    final_payload = json.loads(deepseek.last_payload["messages"][1]["content"])
    image_analysis = final_payload["imageAnalysis"]
    assert isinstance(image_analysis, dict)
    assert [page["page"] for page in image_analysis["pages"]] == [1, 2, 3, 4, 5]


def test_interpret_with_analysis_returns_page_scoped_image_facts() -> None:
    request = _request().model_copy(
        update={
            "report_images": [
                ReportImage(
                    page=1,
                    mimeType="image/jpeg",
                    downloadUrl="https://minio.internal/reports/page-1",
                ),
            ]
        }
    )
    vision = _FakeVisionClient(_generated_content())
    deepseek = _FakeClient(_generated_content())
    service = InterpretationService(
        settings=DeepSeekSettings(
            enabled=True,
            api_key="test-deepseek-key",
            base_url="https://example.invalid",
            model="deepseek-v4-flash",
            timeout_seconds=1,
            max_tokens=2000,
            thinking_enabled=False,
        ),
        vision_settings=QwenVisionSettings(
            enabled=True,
            api_key="test-qwen-key",
            workspace_id="",
            base_url="https://example.invalid/v1",
            model="qwen3.8-flash",
            timeout_seconds=1,
            max_tokens=2000,
            max_images=50,
        ),
        client=deepseek,  # type: ignore[arg-type]
        vision_client=vision,  # type: ignore[arg-type]
    )

    result = service.interpret_with_analysis(request, _results())

    assert result.interpretation.status == "SUCCESS"
    assert result.image_analysis is not None
    assert [page.page for page in result.image_analysis.pages] == [1]
    finding = result.image_analysis.pages[0].findings[0]
    assert finding.item == "空腹血糖"
    assert finding.abnormal_flag == "高"


def test_knowledge_retriever_grounds_query_in_image_analysis_facts() -> None:
    request = AssessmentRequest(
        taskId="TASK_IMG_ONLY",
        patientId="PATIENT_IMG_ONLY",
        indicators=[],  # 图片报告跳过 OCR，无结构化指标
        findings=[],
    )
    image_analysis = VisionImageAnalysis(
        pages=[
            VisionImagePage(
                page=1,
                pageSummary="第1页体检事实",
                findings=[
                    VisionImageFinding(
                        category="检验",
                        item="空腹血糖",
                        result="6.4",
                        unit="mmol/L",
                        referenceRange="3.9-6.1",
                        abnormalFlag="高",
                    )
                ],
                uncertainties=[],
            )
        ]
    )

    references = MedicalKnowledgeRetriever().retrieve(
        request, [], image_analysis=image_analysis
    )
    reference_ids = {item.reference_id for item in references}

    assert "NHC-HYPERGLYCEMIA-2024-001" in reference_ids


def test_tcm_medication_reference_is_specific_only_with_matching_rag_evidence() -> None:
    hp_reference = InterpretationService._evidence_backed_tcm_medication_reference(
        "幽门螺杆菌感染风险",
        ["TCM-HP-2026-001"],
    )
    assert hp_reference
    assert "半夏泻心汤" in hp_reference[0]
    assert "不替代幽门螺杆菌规范根除治疗" in hp_reference[0]
    assert InterpretationService._evidence_backed_tcm_medication_reference(
        "幽门螺杆菌感染风险",
        ["NHC-LAB-GENERAL-001"],
    ) == []


def test_generic_tcm_placeholder_is_detected_for_legacy_reports() -> None:
    assert InterpretationService._is_generic_tcm_medication_reference(
        ["本次证据未支持具体中药方剂，需中医师辨证后处方。"]
    )
    assert not InterpretationService._is_generic_tcm_medication_reference(
        ["可与中医师讨论半夏泻心汤颗粒，具体是否使用由医师辨证确认。"]
    )


def test_diagnostic_reference_uses_report_summary_and_carries_safe_care_plans() -> None:
    content = _generated_content()
    content["diagnosticReferences"] = [
        {
            "conditionName": "胆囊息肉样变待排",
            "assessment": "POSSIBLE",
            "rationale": "原报告检查小结提示胆囊壁稍强回声，考虑息肉样变，需结合临床资料复核。",
            "indicatorCodes": [],
            "patientFactIds": ["EXAM:001:SUMMARY:001", "EXAM:001:OBS:001"],
            "evidenceIds": ["NHC-LAB-GENERAL-001"],
            "supportingEvidence": ["上腹部彩超检查小结提示胆囊壁稍强回声，考虑息肉样变。"],
            "contradictingEvidence": [],
            "confirmationAdvice": ["由专科结合症状和复查影像进一步判断。"],
            "treatmentPlan": [
                "消化内科复核胆囊超声，并完成症状、胆囊炎和结石风险评估。",
                "根据复查影像和症状分层，确定观察、药物控制或外科评估的适用路径。",
                "按专科安排复查影像，观察病灶变化和相关症状。",
            ],
            "nutritionInterventionPlan": ["记录高脂饮食摄入，由营养专业人员结合复查结果调整饮食。"],
            "westernMedicineApproach": [
                "由消化内科复核超声并完成炎症、结石和症状风险分层。",
                "根据复查结果评估观察随访或进一步专科处理路径。",
            ],
            "traditionalChineseMedicineApproach": [
                "如有调理需求，由中医师辨证评估体质和症状后制定非药物调养方向。",
            ],
            "westernMedicineMedicationPlan": [
                "如复核确认需要处理，由专科依据证据选择相应药物类别并核对禁忌。",
            ],
            "traditionalChineseMedicineMedicationPlan": [
                "如适合中医辅助，由中医师根据证型选择清热化湿或健脾和胃等治法方向。",
            ],
            "integratedTreatmentNotes": [
                "中西医方案由医生统筹，结合过敏史、当前用药和复查变化动态调整。",
            ],
            "recommendedDepartment": "消化内科或肝胆外科",
        }
    ]
    result = _service(_FakeClient(content)).interpret(_request(), _results())

    assert result.status == "SUCCESS"
    assert result.diagnostic_references[0].patient_fact_ids == [
        "EXAM:001:SUMMARY:001",
        "EXAM:001:OBS:001",
    ]
    assert result.diagnostic_references[0].treatment_plan
    assert result.diagnostic_references[0].nutrition_intervention_plan
    assert result.diagnostic_references[0].western_medicine_approach
    assert result.diagnostic_references[0].traditional_chinese_medicine_approach
    assert result.diagnostic_references[0].western_medicine_medication_plan
    assert result.diagnostic_references[0].traditional_chinese_medicine_medication_plan
    assert result.diagnostic_references[0].integrated_treatment_notes


def test_weak_optional_diagnostic_candidate_does_not_discard_deepseek_report() -> None:
    content = _generated_content(
        "空腹血糖的本次结果已核对，当前更适合先复查并结合饮食和体重趋势管理。"
    )
    content["diagnosticReferences"] = [
        {
            "conditionName": "糖代谢问题待排",
            "assessment": "POSSIBLE",
            "rationale": "需要结合后续复查和完整资料进一步判断，当前不能直接下结论。",
            "indicatorCodes": ["fasting_glucose"],
            "patientFactIds": ["LAB:fasting_glucose"],
            "evidenceIds": ["NHC-HYPERGLYCEMIA-2024-001"],
            "supportingEvidence": ["空腹血糖本次结果高于报告参考上限。"],
            "contradictingEvidence": [],
            "confirmationAdvice": ["结合空腹血糖和糖化血红蛋白复查。"],
            "treatmentPlan": ["先复查相关指标并记录体重变化。"],
            "nutritionInterventionPlan": ["记录一周饮食和运动情况。"],
            "westernMedicineApproach": ["由医生结合复查结果判断后续路径。"],
            "traditionalChineseMedicineApproach": [],
            "westernMedicineMedicationPlan": [],
            "traditionalChineseMedicineMedicationPlan": [],
            "integratedTreatmentNotes": [],
            "recommendedDepartment": "内分泌科",
        }
    ]
    fake = _FakeClient(content)

    result = _service(fake).interpret(_request(), _results())

    assert result.status == "SUCCESS"
    assert result.source == "DEEPSEEK"
    assert result.fallback_reason is None
    assert fake.call_count == 1
    assert "空腹血糖" in result.summary
    assert result.diagnostic_references == []
    assert result.cross_model_findings


def test_rule_fallback_abnormal_explanations_are_indicator_specific() -> None:
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

    result = service.interpret(_request(), _results())
    explanations = {item.title: item for item in result.abnormal_explanations}

    assert result.source == "RULE_FALLBACK"
    assert "糖代谢指标" in explanations["空腹血糖"].explanation
    assert "糖化血红蛋白" in explanations["空腹血糖"].next_step
    assert "这说明本次指标与报告参考区间存在偏离" not in explanations["空腹血糖"].explanation


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


class _TimeoutOnceClient(_FakeClient):
    def post(self, *args: Any, **kwargs: Any) -> _FakeResponse:
        if self.call_count == 0:
            self.call_count += 1
            raise httpx.ReadTimeout("temporary upstream timeout")
        return super().post(*args, **kwargs)


def test_network_timeout_is_retried_before_falling_back() -> None:
    fake = _TimeoutOnceClient(_generated_content())

    result = _service(fake).interpret(_request(), _results())

    assert result.status == "SUCCESS"
    assert result.source == "DEEPSEEK"
    assert result.generation_attempts == 2
    assert fake.call_count == 2


def test_safe_medical_boundaries_and_bmi_explanation_are_not_rejected() -> None:
    content = _generated_content(
        "BMI 24.2（超重）需要结合体重趋势观察；不要自行停药，应由医生评估当前药物。"
    )

    result = _service(_FakeClient(content)).interpret(_request(), _results())

    assert result.status == "SUCCESS"
    assert result.source == "DEEPSEEK"


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
