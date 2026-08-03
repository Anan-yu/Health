from decimal import ROUND_HALF_UP, Decimal
from typing import Any

from app.schemas.assessment import AssessmentRequest, ModelResult

_SUMMARY_LABELS = {
    "小结",
    "检查小结",
    "诊断意见",
    "诊断结论",
    "检查结论",
    "影像结论",
    "印象",
    "提示",
    "结论",
}

_CAMERA_LIMITATION = "仅供趋势参考，不能替代医疗设备测量。"


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


def _normalized_label(value: str) -> str:
    return "".join(value.strip().rstrip("：:").split())


def _abnormal_display_text(indicator: dict[str, Any]) -> str:
    name = indicator["name"]
    value = indicator["value"]
    unit = f" {indicator['unit']}" if indicator["unit"] else ""
    if indicator["referenceStatus"] == "HIGH":
        return (
            f"{name}为 {value}{unit}，高于本次报告参考上限 " f"{indicator['referenceHigh']}{unit}。"
        )
    return f"{name}为 {value}{unit}，低于本次报告参考下限 " f"{indicator['referenceLow']}{unit}。"


def _structured_examinations(request: AssessmentRequest) -> list[dict[str, Any]]:
    """Keep qualitative observations and report conclusions separate and traceable."""
    sections: list[dict[str, Any]] = []
    section_indexes: dict[str, int] = {}
    seen: set[tuple[str, str, str]] = set()

    for finding in request.findings:
        section = finding.section.strip() or "其他体检结果"
        item = finding.item.strip()
        result = finding.result.strip()
        if not item or not result:
            continue
        fingerprint = (section, item, result)
        if fingerprint in seen:
            continue
        seen.add(fingerprint)

        section_index = section_indexes.get(section)
        if section_index is None:
            section_index = len(sections)
            section_indexes[section] = section_index
            sections.append(
                {
                    "sectionId": f"EXAM:{section_index + 1:03d}",
                    "category": section,
                    "observations": [],
                    "summaries": [],
                }
            )

        target = sections[section_index]
        is_summary = _normalized_label(item) in _SUMMARY_LABELS
        collection_name = "summaries" if is_summary else "observations"
        ordinal = len(target[collection_name]) + 1
        fact_kind = "SUMMARY" if is_summary else "OBS"
        target[collection_name].append(
            {
                "factId": f"{target['sectionId']}:{fact_kind}:{ordinal:03d}",
                "item": item,
                "result": result,
                "sourceQualifier": "原报告检查小结" if is_summary else "原报告检查所见",
            }
        )

    return sections


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
                "factId": f"LAB:{item.code}",
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
        abnormal_facts = [
            {
                "factId": item["factId"],
                "displayName": item["name"],
                "value": item["value"],
                "unit": item["unit"],
                "referenceLow": item["referenceLow"],
                "referenceHigh": item["referenceHigh"],
                "referenceStatus": item["referenceStatus"],
                "displayText": _abnormal_display_text(item),
            }
            for item in abnormal
        ]
        examination_sections = _structured_examinations(request)
        context_payload = (
            context.model_dump(by_alias=True, exclude_none=True, mode="json")
            if context is not None
            else {}
        )
        context_payload.pop("bmi", None)
        canonical_context_payload = (
            context.model_dump(by_alias=False, exclude_none=True, mode="json")
            if context is not None
            else {}
        )
        canonical_context_payload.pop("bmi", None)
        patient_facts: list[dict[str, Any]] = []
        for key, value in canonical_context_payload.items():
            if key.startswith("camera_"):
                if key == "camera_completed_at":
                    continue
                camera_fact = {
                    "factId": f"FACE:{key}",
                    "category": "健康拍体征",
                    "field": key,
                    "value": value,
                    "sourceType": "FACE_CAMERA_ESTIMATION",
                    "sourceLabel": "健康拍摄像头估算",
                    "evidenceLevel": "SUPPLEMENTARY",
                    "usableForDiagnosis": False,
                    "limitation": _CAMERA_LIMITATION,
                }
                completed_at = canonical_context_payload.get("camera_completed_at")
                if completed_at:
                    camera_fact["completedAt"] = completed_at
                patient_facts.append(camera_fact)
                continue
            patient_facts.append(
                {
                    "factId": f"PROFILE:{key}",
                    "category": "健康档案与问卷",
                    "field": key,
                    "value": value,
                }
            )
        if calculated_bmi is not None:
            patient_facts.append(
                {
                    "factId": "DERIVED:BMI",
                    "category": "身体测量",
                    "field": "bmi",
                    "value": _decimal_text(calculated_bmi),
                    "unit": "kg/m²",
                    "sourceQualifier": "根据身高和体重计算",
                }
            )
        patient_facts.extend(
            {
                "factId": item["factId"],
                "category": "检验指标",
                "field": item["code"],
                "value": item["value"],
                "unit": item["unit"],
                "referenceStatus": item["referenceStatus"],
            }
            for item in indicators
        )
        for section in examination_sections:
            for finding in section["observations"]:
                patient_facts.append(
                    {
                        "factId": finding["factId"],
                        "category": "非数值检查所见",
                        "section": section["category"],
                        "field": finding["item"],
                        "value": finding["result"],
                        "sourceQualifier": finding["sourceQualifier"],
                    }
                )
            for summary in section["summaries"]:
                patient_facts.append(
                    {
                        "factId": summary["factId"],
                        "category": "原报告检查小结",
                        "section": section["category"],
                        "field": summary["item"],
                        "value": summary["result"],
                        "sourceQualifier": summary["sourceQualifier"],
                    }
                )

        focus_profile_fields = {
            "lifestyle_summary",
            "medical_history",
            "family_history",
            "allergy_history",
            "current_medications",
            "hypertension_status",
            "diabetes_status",
            "dyslipidemia_status",
            "fatty_liver_status",
            "smoking_status",
            "alcohol_status",
            "exercise_frequency",
            "sleep_quality",
            "sleep_hours",
            "stress_level",
            "dietary_preference",
            "recent_dietary_pattern",
        }
        profile_signals = [
            {
                "factId": f"PROFILE:{key}",
                "field": key,
                "value": value,
            }
            for key, value in canonical_context_payload.items()
            if key in focus_profile_fields
        ]
        report_conclusions = [
            {
                "factId": summary["factId"],
                "section": section["category"],
                "item": summary["item"],
                "result": summary["result"],
            }
            for section in examination_sections
            for summary in section["summaries"]
        ]
        attention_results = [
            {
                "modelName": item.model_name,
                "riskLevel": item.risk_level,
                "evidence": item.evidence,
                "missingIndicators": item.missing_indicators,
            }
            for item in results
            if item.status == "EVALUATED" and item.risk_level in {"ATTENTION", "HIGH"}
        ]

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
            "patientFacts": patient_facts,
            "abnormalFacts": abnormal_facts,
            "analysisFocus": {
                "instruction": "先围绕本区生成结论，再到完整快照核对，不按资料顺序逐项复述。",
                "abnormalFacts": abnormal_facts,
                "profileSignals": profile_signals,
                "reportConclusions": report_conclusions,
                "attentionResults": attention_results,
            },
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
            "examinationSnapshot": {
                "sectionCount": len(examination_sections),
                "observationCount": sum(
                    len(section["observations"]) for section in examination_sections
                ),
                "summaryCount": sum(len(section["summaries"]) for section in examination_sections),
                "sections": examination_sections,
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
                indicators, examination_sections, calculated_bmi, results
            ),
        }

    @staticmethod
    def _deterministic_findings(
        indicators: list[dict[str, Any]],
        examination_sections: list[dict[str, Any]],
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
        if examination_sections:
            observation_count = sum(
                len(section["observations"]) for section in examination_sections
            )
            summary_count = sum(len(section["summaries"]) for section in examination_sections)
            findings.append(
                f"原报告另含{len(examination_sections)}个非数值检查类目、"
                f"{observation_count}项检查所见和{summary_count}项检查小结。"
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
