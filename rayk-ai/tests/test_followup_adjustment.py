import json
from typing import Any

from app.followup.service import FollowupAdjustmentService
from app.interpretation.service import DeepSeekSettings
from app.schemas.followup import FollowupAdjustmentRequest


class _FakeResponse:
    def __init__(self, content: dict[str, object]) -> None:
        self.content = content

    def raise_for_status(self) -> None:
        return None

    def json(self) -> dict[str, object]:
        return {
            "choices": [
                {
                    "finish_reason": "stop",
                    "message": {"content": json.dumps(self.content, ensure_ascii=False)},
                }
            ]
        }


class _FakeClient:
    def __init__(self, content: dict[str, object]) -> None:
        self.content = content
        self.last_payload: dict[str, Any] | None = None

    def post(
        self,
        _url: str,
        *,
        headers: dict[str, str],
        json: dict[str, Any],
    ) -> _FakeResponse:
        assert headers["Authorization"] == "Bearer test-key"
        self.last_payload = json
        return _FakeResponse(self.content)


class _SequentialFakeClient:
    def __init__(self, contents: list[dict[str, object]]) -> None:
        self.contents = contents
        self.payloads: list[dict[str, Any]] = []

    def post(
        self,
        _url: str,
        *,
        headers: dict[str, str],
        json: dict[str, Any],
    ) -> _FakeResponse:
        assert headers["Authorization"] == "Bearer test-key"
        self.payloads.append(json)
        return _FakeResponse(self.contents[len(self.payloads) - 1])


def _request(
    *,
    completion_rate: int = 75,
    feedback: str = "最近膝盖疼，快走很难坚持，希望换成轻一些的运动。",
) -> FollowupAdjustmentRequest:
    return FollowupAdjustmentRequest(
        cycleNo=1,
        maxCycles=4,
        completionRate=completion_rate,
        feedback=feedback,
        patientContext={
            "gender": "FEMALE",
            "age": 52,
            "medicalHistory": "高血压",
            "exerciseFrequency": "RARELY",
        },
        actions=[
            {
                "section": "运动行动",
                "action": "每周快走5次，每次30分钟。",
                "status": "PARTIAL",
                "note": "走路时膝盖疼，只完成两次。",
            },
            {
                "section": "作息行动",
                "action": "每晚争取睡足7小时。",
                "status": "COMPLETED",
                "note": "可以坚持。",
            },
        ],
    )


def _settings(*, enabled: bool = True) -> DeepSeekSettings:
    return DeepSeekSettings(
        enabled=enabled,
        api_key="test-key" if enabled else "",
        base_url="https://example.invalid",
        model="deepseek-v4-flash",
        timeout_seconds=1,
        max_tokens=1600,
        thinking_enabled=False,
    )


def test_uses_text_feedback_and_body_feeling_to_adjust_actions() -> None:
    fake = _FakeClient(
        {
            "decision": "ADJUST",
            "decisionReason": "用户反馈膝盖疼且快走难以坚持，需要降低运动冲击和频次。",
            "feedbackSummary": "睡眠行动已完成，运动行动因膝盖疼仅部分完成。",
            "nextActions": [
                {
                    "section": "运动行动",
                    "action": "本周改为坐姿抬腿或舒缓拉伸，每周3次，每次10分钟；不适时停止。",
                },
                {
                    "section": "监测行动",
                    "action": "每天记录膝盖不适出现的时间、活动和持续时长。",
                },
            ],
        }
    )
    result = FollowupAdjustmentService(
        settings=_settings(), client=fake  # type: ignore[arg-type]
    ).adjust(_request())

    assert result.source == "DEEPSEEK"
    assert result.decision == "ADJUST"
    assert "膝盖疼" in result.decision_reason
    assert any("坐姿抬腿" in action.action for action in result.next_actions)
    assert fake.last_payload is not None
    prompt = json.loads(fake.last_payload["messages"][1]["content"])
    assert "膝盖疼" in json.dumps(prompt["data"], ensure_ascii=False)


def test_disabled_ai_adjusts_when_notes_contain_difficulty() -> None:
    result = FollowupAdjustmentService(settings=_settings(enabled=False)).adjust(
        _request(completion_rate=80)
    )

    assert result.source == "RULE_FALLBACK"
    assert result.decision == "ADJUST"
    assert any("结合“走路时膝盖疼" in action.action for action in result.next_actions)
    assert all("睡足7小时" not in action.action for action in result.next_actions)


def test_unsafe_ai_advice_falls_back_to_rules() -> None:
    fake = _FakeClient(
        {
            "decision": "ADJUST",
            "decisionReason": "需要调整。",
            "feedbackSummary": "存在执行困难。",
            "nextActions": [
                {
                    "section": "监测行动",
                    "action": "每日口服500mg药物并自行停药观察。",
                }
            ],
        }
    )
    result = FollowupAdjustmentService(
        settings=_settings(), client=fake  # type: ignore[arg-type]
    ).adjust(_request())

    assert result.source == "RULE_FALLBACK"
    assert all("500mg" not in action.action for action in result.next_actions)


def test_retries_invalid_model_output_and_uses_repaired_result() -> None:
    fake = _SequentialFakeClient(
        [
            {
                "decision": "ADJUST",
                "decisionReason": "需要调整。",
                "feedbackSummary": "存在执行困难。",
                "nextActions": [{"section": "未知栏目", "action": "每周完成3次。"}],
            },
            {
                "decision": "ADJUST",
                "decisionReason": "缺少测量工具且近期疲倦，需要替换测量任务并降低运动强度。",
                "feedbackSummary": "存在一项未完成和一项因疲倦仅部分完成的行动。",
                "nextActions": [
                    {
                        "section": "记录行动",
                        "action": "每天记录一次三餐主食、甜食摄入和身体感受，连续7天。",
                    },
                    {
                        "section": "运动",
                        "action": "本周舒缓拉伸3次，每次10分钟。",
                    },
                ],
            },
        ]
    )

    result = FollowupAdjustmentService(
        settings=_settings(), client=fake  # type: ignore[arg-type]
    ).adjust(_request())

    assert result.source == "DEEPSEEK"
    assert result.decision == "ADJUST"
    assert len(fake.payloads) == 2
    assert len(fake.payloads[1]["messages"]) == 3
    assert {action.section for action in result.next_actions} == {
        "监测行动",
        "运动行动",
    }


def test_rule_fallback_understands_missing_equipment_and_fatigue() -> None:
    request = _equipment_and_fatigue_request()

    result = FollowupAdjustmentService(settings=_settings(enabled=False)).adjust(request)

    assert result.source == "RULE_FALLBACK"
    assert result.decision == "ADJUST"
    assert all("每晚睡足7小时" not in item.action for item in result.next_actions)
    assert any("不要求自行测量血糖" in item.action for item in result.next_actions)
    assert any("每周3次" in item.action for item in result.next_actions)


def test_model_result_is_guarded_against_unresolved_user_difficulty() -> None:
    fake = _FakeClient(
        {
            "decision": "CONTINUE",
            "decisionReason": "总体完成度较高。",
            "feedbackSummary": "大部分行动已经完成。",
            "nextActions": [
                {
                    "section": "监测行动",
                    "action": "每天测量并记录血糖。",
                },
                {
                    "section": "运动行动",
                    "action": "每周快走5次，每次30分钟。",
                },
            ],
        }
    )

    result = FollowupAdjustmentService(
        settings=_settings(), client=fake  # type: ignore[arg-type]
    ).adjust(_equipment_and_fatigue_request())

    assert result.decision == "ADJUST"
    assert any("不要求使用测量设备" in item.action for item in result.next_actions)
    assert any("每周3次" in item.action for item in result.next_actions)
    assert all("每天测量并记录血糖" not in item.action for item in result.next_actions)
    assert all("每周快走5次" not in item.action for item in result.next_actions)


def _equipment_and_fatigue_request() -> FollowupAdjustmentRequest:
    return FollowupAdjustmentRequest(
        cycleNo=2,
        maxCycles=4,
        completionRate=75,
        feedback="没有测量工具",
        patientContext={"gender": "MALE", "age": 38},
        actions=[
            {
                "section": "饮食行动",
                "action": "记录餐前或餐后血糖以及对应餐次主食摄入。",
                "status": "NOT_COMPLETED",
                "note": "无测量工具",
            },
            {
                "section": "运动行动",
                "action": "每周快走5次，每次30分钟。",
                "status": "PARTIAL",
                "note": "最近身体疲倦",
            },
            {
                "section": "作息行动",
                "action": "每晚睡足7小时。",
                "status": "COMPLETED",
            },
        ],
    )
