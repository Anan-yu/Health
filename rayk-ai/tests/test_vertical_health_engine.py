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
        patientContext=PatientContext(
            gender="FEMALE",
            age=38,
            heightCm=Decimal("165"),
            weightKg=Decimal("66"),
            familyHistory="父亲有糖尿病",
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

    assert "GENERAL_LAB_INTERPRETATION" in reference_ids
    assert "GLUCOSE_METABOLISM" in reference_ids
    assert "SLEEP_STRESS_MOOD" in reference_ids
    assert all(
        item.to_prompt_dict()["knowledgeBaseVersion"] == KNOWLEDGE_BASE_VERSION
        for item in references
    )


def test_clinical_timeline_is_deidentified_and_calculates_bmi() -> None:
    timeline = ClinicalContextBuilder().build(_request(), _results())
    serialized = json.dumps(timeline, ensure_ascii=False)

    assert "TASK_SENSITIVE" not in serialized
    assert "PATIENT_SENSITIVE" not in serialized
    assert timeline["anthropometrics"]["calculatedBmi"] == "24.2"
    assert timeline["laboratorySnapshot"]["abnormalCount"] == 1
    assert timeline["laboratorySnapshot"]["indicators"][0]["referenceStatus"] == "HIGH"


class _FakeResponse:
    def __init__(self, content: dict[str, Any]) -> None:
        self.content = content

    def raise_for_status(self) -> None:
        return None

    def json(self) -> dict[str, Any]:
        return {
            "choices": [
                {
                    "finish_reason": "stop",
                    "message": {"content": json.dumps(self.content, ensure_ascii=False)},
                }
            ]
        }


class _FakeClient:
    def __init__(self, content: dict[str, Any]) -> None:
        self.content = content
        self.last_payload: dict[str, Any] | None = None

    def post(self, *args: Any, **kwargs: Any) -> _FakeResponse:
        self.last_payload = kwargs["json"]
        return _FakeResponse(self.content)


def _generated_content(summary: str = "当前存在糖代谢风险信号，建议持续观察。") -> dict[str, Any]:
    return {
        "summary": summary,
        "priorityConcerns": ["空腹血糖高于本次报告参考上限"],
        "crossModelFindings": [
            {
                "title": "糖代谢指标需关注",
                "indicatorCodes": ["fasting_glucose", "hba1c"],
                "explanation": "空腹血糖异常，需要结合后续复测观察。",
            }
        ],
        "diagnosticReferences": [
            {
                "conditionName": "糖代谢异常风险",
                "assessment": "RISK_SIGNAL",
                "rationale": "空腹血糖超出本次报告参考范围。",
                "indicatorCodes": ["fasting_glucose"],
                "supportingEvidence": ["空腹血糖6.4 mmol/L，高于参考上限6.1 mmol/L"],
                "contradictingEvidence": ["糖化血红蛋白仍在本次报告参考范围"],
                "confirmationAdvice": ["按医生建议复测相关指标"],
                "recommendedDepartment": "全科或内分泌科",
            }
        ],
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
    assert "medicalKnowledge" in user_message["data"]
    assert "TASK_SENSITIVE" not in serialized
    assert "PATIENT_SENSITIVE" not in serialized


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
