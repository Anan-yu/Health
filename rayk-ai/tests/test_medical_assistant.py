import json
from typing import Any

import httpx

from app.assistant.service import (
    ASSISTANT_DISCLAIMER,
    MedicalAssistantService,
    MedicalAssistantSettings,
)
from app.interpretation.service import DeepSeekSettings
from app.schemas.assistant import MedicalAssistantRequest


class _FakeResponse:
    def __init__(self, data: dict[str, Any]) -> None:
        self.data = data

    def raise_for_status(self) -> None:
        return None

    def json(self) -> dict[str, Any]:
        return {
            "choices": [
                {
                    "message": {
                        "content": json.dumps(self.data, ensure_ascii=False),
                    }
                }
            ]
        }


class _FakeClient:
    def __init__(self, data: dict[str, Any]) -> None:
        self.data = data
        self.payload: dict[str, Any] | None = None

    def post(
        self,
        _url: str,
        *,
        headers: dict[str, str],
        json: dict[str, Any],
    ) -> _FakeResponse:
        assert headers["Authorization"] == "Bearer test-key"
        self.payload = json
        return _FakeResponse(self.data)


class _FakeStreamResponse:
    def __init__(self, lines: list[str]) -> None:
        self.lines = lines

    def __enter__(self) -> "_FakeStreamResponse":
        return self

    def __exit__(self, *_args: object) -> None:
        return None

    def raise_for_status(self) -> None:
        return None

    def iter_lines(self) -> list[str]:
        return self.lines


class _FakeStreamingClient:
    def __init__(self, data: dict[str, Any]) -> None:
        content = json.dumps(data, ensure_ascii=False)
        self.payload: dict[str, Any] | None = None
        self.lines = [
            f'data: {json.dumps({"choices": [{"delta": {"content": chunk}}]}, ensure_ascii=False)}'
            for chunk in (content[index : index + 11] for index in range(0, len(content), 11))
        ] + ["data: [DONE]"]

    def stream(
        self,
        _method: str,
        _url: str,
        *,
        headers: dict[str, str],
        json: dict[str, Any],
    ) -> _FakeStreamResponse:
        assert headers["Authorization"] == "Bearer test-key"
        self.payload = json
        return _FakeStreamResponse(self.lines)


class _FailingStreamingClient:
    def stream(self, *_args: Any, **_kwargs: Any) -> Any:
        raise httpx.ConnectTimeout("qwen connection timed out")


def _settings(*, enabled: bool = True) -> DeepSeekSettings:
    return DeepSeekSettings(
        enabled=enabled,
        api_key="test-key" if enabled else "",
        base_url="https://example.invalid",
        model="qwen3.8-flash",
        timeout_seconds=1,
        max_tokens=1600,
        thinking_enabled=False,
    )


def _request(content: str = "最近报告里哪项需要优先复查？") -> MedicalAssistantRequest:
    return MedicalAssistantRequest(
        conversationId="1",
        patientId="2",
        patientName="测试用户",
        messages=[{"role": "USER", "content": content}],
        patientContext={
            "gender": "FEMALE",
            "age": 42,
            "medicalHistory": "高血压",
            "cameraHeartRate": 76,
        },
        latestReportSummary="最近一次报告提示血脂部分指标偏高。",
        latestAssessmentSnapshot='{"summary":"需要关注血脂"}',
        model="qwen3.8-flash",
        thinkingEnabled=False,
    )


def test_qwen_assistant_settings_use_fixed_model_and_qwen_endpoint(monkeypatch: Any) -> None:
    monkeypatch.setenv("QWEN_ASSISTANT_ENABLED", "true")
    monkeypatch.setenv("QWEN_ASSISTANT_API_KEY", "qwen-test-key")
    monkeypatch.setenv(
        "QWEN_ASSISTANT_BASE_URL", "https://dashscope.example/compatible-mode/v1"
    )
    monkeypatch.delenv("QWEN_ASSISTANT_MODEL", raising=False)

    settings = MedicalAssistantSettings.from_env()

    assert settings.enabled is True
    assert settings.api_key == "qwen-test-key"
    assert settings.base_url.endswith("/compatible-mode/v1")
    assert settings.model == "qwen3.8-flash"


def test_emergency_guard_returns_urgent_without_model_call() -> None:
    client = _FakeClient({})
    result = MedicalAssistantService(settings=_settings(enabled=False), client=client).answer(
        _request("我突然胸痛并且喘不上气")
    )

    assert result.risk_level == "URGENT"
    assert result.emergency is True
    assert result.model == "safety-guard"
    assert client.payload is None


def test_emergency_guard_streams_delta_and_done_events() -> None:
    events = list(
        MedicalAssistantService(settings=_settings(enabled=False)).stream_answer(
            _request("我现在胸痛并伴随呼吸困难")
        )
    )

    assert any('"type": "delta"' in event for event in events)
    assert any('"type": "done"' in event for event in events)


def test_current_time_query_uses_beijing_server_clock_without_model_call() -> None:
    client = _FakeClient({})
    result = MedicalAssistantService(settings=_settings(), client=client).answer(
        _request("现在几点了？")
    )

    assert result.model == "system-clock"
    assert "北京时间" in result.reply
    assert "现在是" in result.reply
    assert client.payload is None


def test_live_weather_query_is_explicitly_rejected_without_external_tool() -> None:
    client = _FakeClient({})
    result = MedicalAssistantService(settings=_settings(), client=client).answer(
        _request("今天的天气怎么样？")
    )

    assert result.model == "capability-guard"
    assert "没有联网天气" in result.reply
    assert client.payload is None


def test_current_time_query_streams_without_model_call() -> None:
    client = _FakeStreamingClient({})
    events = list(
        MedicalAssistantService(settings=_settings(), client=client).stream_answer(
            _request("当前时间是多少？")
        )
    )
    payloads = [json.loads(event.split("data:", 1)[1]) for event in events]

    assert "北京时间" in "".join(
        item["content"] for item in payloads if item["type"] == "delta"
    )
    assert payloads[-1]["type"] == "done"
    assert client.payload is None


def test_disabled_service_explains_configuration_state() -> None:
    result = MedicalAssistantService(settings=_settings(enabled=False)).answer(_request())

    assert result.risk_level == "INFO"
    assert "服务配置" in result.reply
    assert result.disclaimer == ASSISTANT_DISCLAIMER


def test_deepseek_answer_is_structured_and_grounded() -> None:
    client = _FakeClient(
        {
            "reply": "先核对血脂报告中的甘油三酯和低密度脂蛋白，并把异常值、参考范围带给医生复核。",
            "riskLevel": "ATTENTION",
            "emergency": False,
            "recommendedAction": "预约全科或心内科，携带原始报告并确认是否空腹采血。",
            "citations": ["最近一次健康报告"],
            "usedContext": ["当前对话", "健康档案", "最近一次健康评估"],
            "followupQuestions": ["报告中的低密度脂蛋白是多少？"],
            "disclaimer": ASSISTANT_DISCLAIMER,
        }
    )
    result = MedicalAssistantService(settings=_settings(), client=client).answer(_request())

    assert result.risk_level == "ATTENTION"
    assert result.model == "qwen3.8-flash"
    assert result.followup_questions
    assert client.payload is not None
    prompt = json.loads(client.payload["messages"][1]["content"])
    assert prompt["patientContext"]["medicalHistory"] == "高血压"
    assert prompt["latestReportSummary"]
    assert prompt["outputSchema"]


def test_stream_answer_emits_reply_deltas_and_structured_done_event() -> None:
    client = _FakeStreamingClient(
        {
            "reply": "先核对报告中的甘油三酯和低密度脂蛋白。",
            "riskLevel": "ATTENTION",
            "emergency": False,
            "citations": ["最近一次健康报告"],
            "usedContext": ["当前对话", "最近一次健康评估"],
            "followupQuestions": [],
            "disclaimer": ASSISTANT_DISCLAIMER,
        }
    )
    events = list(MedicalAssistantService(settings=_settings(), client=client).stream_answer(_request()))
    payloads = [json.loads(event.split("data:", 1)[1]) for event in events]

    assert "核对报告中的甘油三酯" in "".join(
        item["content"] for item in payloads if item["type"] == "delta"
    )
    assert payloads[-1]["type"] == "done"
    assert payloads[-1]["data"]["model"] == "qwen3.8-flash"
    assert client.payload and client.payload["stream"] is True


def test_stream_answer_turns_upstream_failure_into_terminal_error_event() -> None:
    events = list(
        MedicalAssistantService(
            settings=_settings(), client=_FailingStreamingClient()
        ).stream_answer(_request())
    )
    payloads = [json.loads(event.split("data:", 1)[1]) for event in events]

    assert payloads[-1]["type"] == "error"
    assert "稍后重试" in payloads[-1]["message"]
