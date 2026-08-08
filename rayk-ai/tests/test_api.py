from base64 import b64decode
from io import BytesIO

import pdfplumber
from fastapi.testclient import TestClient

from app.core.constants import DISCLAIMER
from app.main import app
from app.report.service import DemoReportService
from app.schemas.report import ReportGenerateRequest

client = TestClient(app)


def test_health_propagates_request_id() -> None:
    response = client.get("/health", headers={"X-Request-Id": "test-request-id"})
    assert response.status_code == 200
    assert response.headers["X-Request-Id"] == "test-request-id"
    assert response.json()["data"]["status"] == "UP"


def test_demo_assessment_contains_disclaimer() -> None:
    response = client.post(
        "/api/v1/assessments/evaluate",
        json={
            "taskId": "TASK_001",
            "patientId": "10001",
            "indicators": [
                {"code": "fasting_glucose", "name": "空腹血糖", "value": 6.2, "unit": "mmol/L"}
            ],
        },
    )
    assert response.status_code == 200
    body = response.json()
    assert body["data"]["status"] == "SUCCESS"
    assert body["data"]["disclaimer"] == DISCLAIMER
    assert body["data"]["results"][0]["riskLevel"] == "ATTENTION"


def test_assessment_flags_confirmed_bilirubin_and_electrolyte_abnormalities() -> None:
    response = client.post(
        "/api/v1/assessments/evaluate",
        json={
            "taskId": "TASK_BIOCHEMISTRY_001",
            "patientId": "10001",
            "modelCodes": ["LIVER_METABOLIC", "KIDNEY_ELECTROLYTE"],
            "indicators": [
                {"code": "alt", "name": "丙氨酸氨基转移酶", "value": 14.4, "unit": "U/L"},
                {"code": "ast", "name": "天门冬氨酸氨基转移酶", "value": 19.5, "unit": "U/L"},
                {"code": "ggt", "name": "谷酰转肽酶", "value": 16, "unit": "U/L"},
                {
                    "code": "total_bilirubin",
                    "name": "总胆红素",
                    "value": 22.7,
                    "unit": "μmol/L",
                    "referenceHigh": 22,
                },
                {
                    "code": "direct_bilirubin",
                    "name": "直接胆红素",
                    "value": 8.8,
                    "unit": "μmol/L",
                    "referenceHigh": 6,
                },
                {"code": "albumin", "name": "白蛋白", "value": 48.9, "unit": "g/L"},
                {"code": "creatinine", "name": "肌酐", "value": 75.4, "unit": "μmol/L"},
                {"code": "urea", "name": "尿素", "value": 2.84, "unit": "mmol/L"},
                {"code": "uric_acid", "name": "尿酸", "value": 301.6, "unit": "μmol/L"},
                {"code": "sodium", "name": "钠离子", "value": 140, "unit": "mmol/L"},
                {"code": "potassium", "name": "钾离子", "value": 4.6, "unit": "mmol/L"},
                {"code": "chloride", "name": "氯离子", "value": 108, "unit": "mmol/L"},
                {
                    "code": "bicarbonate",
                    "name": "碳酸氢根",
                    "value": 31.4,
                    "unit": "mmol/L",
                    "referenceHigh": 30,
                },
                {"code": "calcium", "name": "钙离子", "value": 2.39, "unit": "mmol/L"},
            ],
        },
    )

    assert response.status_code == 200
    results = {item["modelCode"]: item for item in response.json()["data"]["results"]}
    assert results["LIVER_METABOLIC"]["riskLevel"] == "ATTENTION"
    assert results["KIDNEY_ELECTROLYTE"]["riskLevel"] == "ATTENTION"
    assert results["LIVER_METABOLIC"]["dataCompleteness"] == 100
    assert results["KIDNEY_ELECTROLYTE"]["dataCompleteness"] >= 85


def test_mental_emotional_dimension_uses_questionnaire_context() -> None:
    response = client.post(
        "/api/v1/assessments/evaluate",
        json={
            "taskId": "TASK_MENTAL_001",
            "patientId": "10001",
            "modelCodes": ["MENTAL_EMOTIONAL"],
            "indicators": [],
            "patientContext": {
                "stressLevel": "MEDIUM",
                "moodStatus": "FAIR",
                "fearLevel": "LOW",
            },
        },
    )

    assert response.status_code == 200
    result = response.json()["data"]["results"][0]
    assert result["modelCode"] == "MENTAL_EMOTIONAL"
    assert result["status"] == "EVALUATED"
    assert result["riskLevel"] == "ATTENTION"
    assert result["dataCompleteness"] == 100
    assert "压力水平：MEDIUM" in result["evidence"]


def test_body_composition_dimension_uses_profile_and_exercise_context() -> None:
    response = client.post(
        "/api/v1/assessments/evaluate",
        json={
            "taskId": "TASK_BODY_001",
            "patientId": "10001",
            "modelCodes": ["BODY_COMPOSITION"],
            "indicators": [],
            "patientContext": {
                "gender": "FEMALE",
                "heightCm": 165,
                "weightKg": 75,
                "waistCm": 92,
                "recentWeightChangeKg": 6,
                "bmi": 27.5,
                "exerciseFrequency": "RARELY",
            },
        },
    )

    assert response.status_code == 200
    result = response.json()["data"]["results"][0]
    assert result["modelCode"] == "BODY_COMPOSITION"
    assert result["status"] == "EVALUATED"
    assert result["riskLevel"] == "HIGH"
    assert result["dataCompleteness"] == 100
    assert "腰围：92 cm" in result["evidence"]


def test_mock_ocr_returns_confirmation_state() -> None:
    response = client.post(
        "/api/v1/ocr/recognize",
        json={"fileId": "FILE_001", "objectName": "demo.pdf", "mimeType": "application/pdf"},
    )
    assert response.status_code == 200
    assert response.json()["data"]["status"] == "WAITING_CONFIRMATION"


def test_report_generation_returns_a_real_pdf() -> None:
    response = client.post(
        "/api/v1/reports/generate",
        json={
            "assessmentId": "ASSESSMENT_001",
            "patientDisplayName": "测试客户",
            "reportNo": "HR_TEST_001",
            "publishedAt": "2026-07-20",
            "doctorOpinion": "建议保持规律复查。",
            "indicators": [
                {
                    "code": "fasting_glucose",
                    "name": "空腹血糖",
                    "value": 6.2,
                    "unit": "mmol/L",
                    "referenceLow": 3.9,
                    "referenceHigh": 6.1,
                }
            ],
            "results": [
                {
                    "modelCode": "GLUCOSE_METABOLISM",
                    "modelName": "糖代谢失衡评估",
                    "score": 75,
                    "riskLevel": "ATTENTION",
                    "evidence": ["空腹血糖高于参考上限"],
                    "missingIndicators": [],
                    "recommendations": ["控制精制碳水化合物摄入"],
                }
            ],
            "interpretation": {
                "status": "SUCCESS",
                "source": "DEEPSEEK",
                "model": "configured-model",
                "summary": "空腹血糖轻度高于参考范围，结合现有资料建议关注糖代谢状态。",
                "priorityConcerns": ["空腹血糖轻度升高"],
                "crossModelFindings": [],
                "diagnosticReferences": [
                    {
                        "conditionName": "糖代谢异常",
                        "assessment": "RISK_SIGNAL",
                        "rationale": "空腹血糖轻度高于本次报告参考上限。",
                        "indicatorCodes": ["fasting_glucose"],
                        "supportingEvidence": ["空腹血糖6.2 mmol/L，高于参考上限6.1 mmol/L"],
                        "contradictingEvidence": ["缺少糖化血红蛋白和重复空腹血糖结果"],
                        "confirmationAdvice": ["复查空腹血糖并结合糖化血红蛋白综合判断"],
                        "treatmentPlan": [
                            "建议由内分泌科或全科结合复查结果确认后续诊疗路径，不自行用药。"
                        ],
                        "nutritionInterventionPlan": [
                            "减少含糖饮料和精制主食摄入，记录一周饮食后由营养专业人员调整。"
                        ],
                        "westernMedicineApproach": [
                            "由内分泌科结合复查结果完成糖代谢风险分层并确定随访路径。"
                        ],
                        "traditionalChineseMedicineApproach": [
                            "如考虑中医调理，由中医师辨证评估体质后制定非药物调养方向。"
                        ],
                        "westernMedicineMedicationPlan": [
                            "如复核确认需要处理，由医生依据证据选择相应药物类别并核对禁忌。"
                        ],
                        "traditionalChineseMedicineMedicationPlan": [
                            "如适合中医辅助，由中医师根据证型选择相应治法方向。"
                        ],
                        "integratedTreatmentNotes": [
                            "中西医方案由医生统筹，结合当前用药、过敏史和复查指标变化调整。"
                        ],
                        "recommendedDepartment": "内分泌科或全科",
                    }
                ],
                "recommendations": ["减少精制糖和含糖饮料摄入"],
                "missingDataAdvice": ["建议补充糖化血红蛋白"],
                "followupQuestions": ["近期是否有明显口渴或体重变化？"],
                "redFlags": [],
                "uncertainty": "单次轻度异常不能用于确诊。",
                "disclaimer": "辅助诊断参考不能替代医生的最终判断。",
            },
        },
    )
    assert response.status_code == 200
    data = response.json()["data"]
    assert "疾病推断参考" in data["sections"]
    assert "需要进一步确认的健康方向" not in data["sections"]
    assert "可能疾病与诊断参考" not in data["sections"]
    assert "本次重点发现" in data["sections"]
    assert "建议补充的信息" in data["sections"]
    assert "下一步健康行动" in data["sections"]
    assert "报告限制与免责声明" in data["sections"]
    assert data["summary"] == ("空腹血糖轻度高于参考范围，结合现有资料建议关注糖代谢状态。")
    assert data["disclaimer"] == DISCLAIMER
    assert "检验指标明细" not in data["sections"]
    assert "优先改善方向" not in data["sections"]
    pdf_bytes = b64decode(data["pdfBase64"])
    assert pdf_bytes.startswith(b"%PDF-")
    with pdfplumber.open(BytesIO(pdf_bytes)) as document:
        pdf_text = "\n".join(page.extract_text() or "" for page in document.pages)
        bold_font_names = {
            char["fontname"]
            for page in document.pages
            for char in page.chars
            if char["text"] in set("糖代谢异常内分泌科或全科")
        }
    assert "报告编号" not in pdf_text
    assert "三、疾病推断参考" in pdf_text
    assert "可能疾病：糖代谢异常" in pdf_text
    assert "建议咨询科室：内分泌科或全科" in pdf_text
    assert "疾病治疗方案" in pdf_text
    assert "营养干预修复方案" in pdf_text
    assert any("WenQuanYiZenHei" in font_name for font_name in bold_font_names)


def test_report_omits_empty_disease_section_and_keeps_summary_and_disclaimer_in_pdf() -> None:
    request = ReportGenerateRequest.model_validate(
        {
            "assessmentId": "ASSESSMENT_003",
            "patientDisplayName": "测试客户",
            "reportNo": "HR_TEST_003",
            "indicators": [
                {
                    "code": "total_cholesterol",
                    "name": "总胆固醇",
                    "value": 5.99,
                    "unit": "mmol/L",
                    "referenceHigh": 5.2,
                }
            ],
            "results": [],
            "interpretation": {
                "status": "DISABLED",
                "source": "RULE_FALLBACK",
                "summary": "本次主要需要关注血脂健康，总胆固醇高于本次报告参考上限。",
                "priorityConcerns": ["总胆固醇为5.99 mmol/L，高于本次报告参考上限5.20 mmol/L。"],
                "crossModelFindings": [],
                "diagnosticReferences": [],
                "recommendations": ["建议核对LDL-C、HDL-C和甘油三酯。"],
                "missingDataAdvice": ["完整血脂指标尚未提供。"],
                "followupQuestions": [],
                "redFlags": [],
                "uncertainty": "不能仅根据总胆固醇判断冠心病或是否需要药物治疗。",
                "disclaimer": DISCLAIMER,
            },
        }
    )

    generated = DemoReportService().generate(request)
    pdf_bytes = b64decode(generated.pdf_base64)
    with pdfplumber.open(BytesIO(pdf_bytes)) as document:
        pdf_text = "\n".join(page.extract_text() or "" for page in document.pages)

    assert generated.summary == request.interpretation.summary
    assert "疾病推断参考" not in generated.sections
    assert generated.disclaimer == DISCLAIMER
    assert request.interpretation.summary in pdf_text
    assert DISCLAIMER in pdf_text
    assert "total_cholesterol" not in pdf_text
    assert "可能疾病与诊断参考" not in pdf_text
    assert "报告编号" not in pdf_text


def test_report_backfills_a_specific_helicobacter_treatment_path_for_legacy_data() -> None:
    request = ReportGenerateRequest.model_validate(
        {
            "assessmentId": "ASSESSMENT_HP_001",
            "patientDisplayName": "测试客户",
            "reportNo": "HR_TEST_HP_001",
            "indicators": [],
            "results": [],
            "interpretation": {
                "status": "SUCCESS",
                "source": "DEEPSEEK",
                "summary": "呼气试验结果提示需要消化专科复核。",
                "priorityConcerns": ["C14呼气试验结果需要结合专科判断。"],
                "crossModelFindings": [],
                "diagnosticReferences": [
                    {
                        "conditionName": "幽门螺杆菌感染",
                        "assessment": "POSSIBLE",
                        "rationale": "C14呼气试验结果提示幽门螺杆菌感染可能。",
                        "indicatorCodes": [],
                        "supportingEvidence": ["C14呼气试验结果高于本次报告参考上限。"],
                        "contradictingEvidence": [],
                        "confirmationAdvice": ["消化内科结合病史和检查复核。"],
                        "westernMedicineApproach": [
                            "由消化内科结合病史和复查结果评估后续根除治疗路径。"
                        ],
                        "traditionalChineseMedicineApproach": [
                            "如有调理需求，由中医师辨证评估后提供辅助调养方向。"
                        ],
                        "westernMedicineMedicationPlan": [
                            "如复核确认需要根除，由消化内科依据证据选择抗菌药、铋剂和抑酸药类别。"
                        ],
                        "traditionalChineseMedicineMedicationPlan": [
                            "如适合中医辅助，由中医师辨证后选择清热化湿或健脾和胃等治法方向。"
                        ],
                        "integratedTreatmentNotes": [
                            "中西医方案由医生统筹，先核对过敏史、当前用药和复查安排。"
                        ],
                        "recommendedDepartment": "消化内科",
                    }
                ],
                "recommendations": ["预约消化内科复核。"],
                "missingDataAdvice": [],
                "followupQuestions": [],
                "redFlags": [],
                "uncertainty": "本报告不构成疾病诊断。",
                "disclaimer": DISCLAIMER,
            },
        }
    )

    pdf_bytes = b64decode(DemoReportService().generate(request).pdf_base64)
    with pdfplumber.open(BytesIO(pdf_bytes)) as document:
        pdf_text = "\n".join(page.extract_text() or "" for page in document.pages)

    assert "含铋四联根除治疗" in pdf_text
    assert "复查呼气试验或粪便抗原" in pdf_text
    assert "中西医结合治疗建议" in pdf_text
    assert "西医治疗思路" in pdf_text
    assert "西医药物治疗参考" in pdf_text


def test_report_overall_health_section_uses_real_coverage_and_profile_data() -> None:
    request = ReportGenerateRequest.model_validate(
        {
            "assessmentId": "ASSESSMENT_002",
            "patientDisplayName": "测试客户",
            "reportNo": "HR_TEST_002",
            "indicators": [
                {
                    "code": "total_bilirubin",
                    "name": "总胆红素",
                    "value": 22.7,
                    "unit": "μmol/L",
                    "referenceLow": 3,
                    "referenceHigh": 22,
                },
                {
                    "code": "albumin",
                    "name": "白蛋白",
                    "value": 48.9,
                    "unit": "g/L",
                    "referenceLow": 40,
                    "referenceHigh": 55,
                },
            ],
            "results": [
                {
                    "modelCode": "LIVER_METABOLIC",
                    "modelName": "肝脏与代谢健康",
                    "status": "EVALUATED",
                    "score": 72,
                    "riskLevel": "ATTENTION",
                    "evidence": ["总胆红素高于参考范围"],
                    "missingIndicators": [],
                    "recommendations": ["结合完整资料持续观察相关指标"],
                }
            ],
            "patientContext": {
                "gender": "MALE",
                "age": 23,
                "bmi": 22.2,
                "exerciseFrequency": "3_5_PER_WEEK",
                "sleepQuality": "GOOD",
            },
        }
    )

    assert DemoReportService._coverage_summary(request) == (
        "共纳入2项检验指标，1个健康维度具备有效数据，"
        "其中1个方向建议持续关注，健康档案和问卷信息已共同纳入评估。"
    )
    assert DemoReportService._indicator_summary(request) == (
        "1项处于原报告参考范围，1项超出原报告参考范围。"
    )
    assert DemoReportService._health_background(request) == (
        "运动频率：每周3至5次；睡眠质量：良好。"
    )
