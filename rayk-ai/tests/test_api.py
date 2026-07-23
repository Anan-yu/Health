from base64 import b64decode

from fastapi.testclient import TestClient

from app.core.constants import DISCLAIMER
from app.main import app

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
    assert body["data"]["results"][0]["riskLevel"] == "INSUFFICIENT_DATA"


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
    assert "可能疾病与诊断参考" in data["sections"]
    assert b64decode(data["pdfBase64"]).startswith(b"%PDF-")
