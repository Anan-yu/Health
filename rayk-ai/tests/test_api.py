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
    assert body["data"]["results"][0]["riskLevel"] == "ATTENTION"


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
        },
    )
    assert response.status_code == 200
    assert b64decode(response.json()["data"]["pdfBase64"]).startswith(b"%PDF-")
