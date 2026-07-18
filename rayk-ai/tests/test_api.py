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

