import json
import logging
import os
from dataclasses import dataclass
from typing import Any, Literal

import httpx
from pydantic import Field

from app.core.constants import DISCLAIMER
from app.schemas.assessment import (
    AssessmentRequest,
    ComprehensiveInterpretation,
    CrossModelFinding,
    ModelResult,
)
from app.schemas.common import RaykModel

logger = logging.getLogger(__name__)


def _env_bool(name: str, default: bool = False) -> bool:
    value = os.getenv(name)
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


@dataclass(frozen=True)
class DeepSeekSettings:
    enabled: bool
    api_key: str
    base_url: str
    model: str
    timeout_seconds: float
    max_tokens: int
    thinking_enabled: bool

    @classmethod
    def from_env(cls) -> "DeepSeekSettings":
        return cls(
            enabled=_env_bool("DEEPSEEK_ENABLED"),
            api_key=os.getenv("DEEPSEEK_API_KEY", "").strip(),
            base_url=os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com").rstrip("/"),
            model=os.getenv("DEEPSEEK_MODEL", "deepseek-v4-flash").strip(),
            timeout_seconds=float(os.getenv("DEEPSEEK_TIMEOUT_SECONDS", "30")),
            max_tokens=int(os.getenv("DEEPSEEK_MAX_TOKENS", "2000")),
            thinking_enabled=_env_bool("DEEPSEEK_THINKING_ENABLED"),
        )


class DeepSeekGeneratedInterpretation(RaykModel):
    summary: str = Field(min_length=1, max_length=1000)
    priority_concerns: list[str] = Field(default_factory=list, alias="priorityConcerns", max_length=10)
    cross_model_findings: list[CrossModelFinding] = Field(
        default_factory=list, alias="crossModelFindings", max_length=10
    )
    recommendations: list[str] = Field(default_factory=list, max_length=20)
    missing_data_advice: list[str] = Field(
        default_factory=list, alias="missingDataAdvice", max_length=20
    )
    followup_questions: list[str] = Field(
        default_factory=list, alias="followupQuestions", max_length=20
    )
    red_flags: list[str] = Field(default_factory=list, alias="redFlags", max_length=10)
    uncertainty: str = Field(min_length=1, max_length=500)


class InterpretationService:
    def __init__(
        self,
        settings: DeepSeekSettings | None = None,
        client: httpx.Client | None = None,
    ) -> None:
        self.settings = settings or DeepSeekSettings.from_env()
        self.client = client or httpx.Client(timeout=self.settings.timeout_seconds)

    def interpret(
        self, request: AssessmentRequest, results: list[ModelResult]
    ) -> ComprehensiveInterpretation:
        if not self.settings.enabled or not self.settings.api_key:
            return self._fallback(results, status="DISABLED")
        try:
            generated = self._call_deepseek(request, results)
            self._validate_indicator_citations(request, generated)
            return ComprehensiveInterpretation(
                status="SUCCESS",
                source="DEEPSEEK",
                model=self.settings.model,
                disclaimer=DISCLAIMER,
                **generated.model_dump(),
            )
        except (httpx.HTTPError, KeyError, IndexError, TypeError, ValueError) as exception:
            logger.warning("DeepSeek interpretation failed: %s", type(exception).__name__)
            return self._fallback(results, status="FALLBACK")

    def _call_deepseek(
        self, request: AssessmentRequest, results: list[ModelResult]
    ) -> DeepSeekGeneratedInterpretation:
        schema = DeepSeekGeneratedInterpretation.model_json_schema(by_alias=True)
        payload = {
            "model": self.settings.model,
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "你是健康管理报告辅助解读引擎，不是医生。只能根据输入的确定性规则结果"
                        "做解释，不得重新评分、不得诊断疾病、不得开药或建议停药。所有结论必须"
                        "说明不确定性；需要医学判断时提示医生复核。不得在输出中出现模型具体名称"
                        "或内部模型代码，可直接描述指标与健康问题。只输出符合指定结构的JSON。"
                    ),
                },
                {
                    "role": "user",
                    "content": json.dumps(
                        {
                            "task": "生成多维健康管理综合解读",
                            "outputSchema": schema,
                            "data": self._sanitized_payload(request, results),
                            "constraints": [
                                "不得生成输入中不存在的指标代码",
                                "不得包含疾病确诊结论",
                                "不得给出药物、剂量、停药或治疗方案",
                                "高风险内容只能表述为需要医生优先复核",
                                "数据不足的模型不能解释为低风险",
                                "输出内容不得出现任何模型具体名称或内部模型代码",
                            ],
                        },
                        ensure_ascii=False,
                        default=str,
                    ),
                },
            ],
            "response_format": {"type": "json_object"},
            "temperature": 0.2,
            "max_tokens": self.settings.max_tokens,
            "thinking": {
                "type": "enabled" if self.settings.thinking_enabled else "disabled"
            },
        }
        response = self.client.post(
            f"{self.settings.base_url}/chat/completions",
            headers={
                "Authorization": f"Bearer {self.settings.api_key}",
                "Content-Type": "application/json",
            },
            json=payload,
        )
        response.raise_for_status()
        body: dict[str, Any] = response.json()
        choice = body["choices"][0]
        if choice.get("finish_reason") != "stop":
            raise ValueError("DeepSeek response was not completed")
        content = choice["message"]["content"]
        if not isinstance(content, str) or not content.strip():
            raise ValueError("DeepSeek returned empty content")
        return DeepSeekGeneratedInterpretation.model_validate_json(content)

    @staticmethod
    def _sanitized_payload(
        request: AssessmentRequest, results: list[ModelResult]
    ) -> dict[str, Any]:
        context = request.patient_context
        return {
            "patientContext": (
                context.model_dump(by_alias=True, exclude_none=True, mode="json")
                if context is not None
                else None
            ),
            "indicators": [
                {
                    "code": item.code,
                    "value": str(item.value),
                    "unit": item.unit,
                    "referenceLow": (
                        str(item.reference_low) if item.reference_low is not None else None
                    ),
                    "referenceHigh": (
                        str(item.reference_high) if item.reference_high is not None else None
                    ),
                }
                for item in request.indicators
                if item.code
            ],
            "ruleResults": [item.model_dump(by_alias=True, mode="json") for item in results],
        }

    @staticmethod
    def _validate_indicator_citations(
        request: AssessmentRequest, generated: DeepSeekGeneratedInterpretation
    ) -> None:
        allowed = {item.code for item in request.indicators if item.code}
        cited = {
            code
            for finding in generated.cross_model_findings
            for code in finding.indicator_codes
        }
        unknown = cited - allowed
        if unknown:
            raise ValueError("DeepSeek cited indicators absent from input")

    @staticmethod
    def _fallback(
        results: list[ModelResult], status: Literal["DISABLED", "FALLBACK"]
    ) -> ComprehensiveInterpretation:
        evaluated = [item for item in results if item.status == "EVALUATED"]
        concerns = [
            f"评估维度{index:02d}：{item.evidence[0]}"
            for index, item in enumerate(evaluated, start=1)
            if item.risk_level in {"ATTENTION", "HIGH"}
        ]
        high = [
            f"评估维度{index:02d}"
            for index, item in enumerate(evaluated, start=1)
            if item.risk_level == "HIGH"
        ]
        insufficient = [item for item in results if item.status == "INSUFFICIENT_DATA"]
        recommendations = list(
            dict.fromkeys(
                recommendation
                for item in evaluated
                for recommendation in item.recommendations
            )
        )[:8]
        if concerns:
            summary = f"规则评估提示优先关注：{'；'.join(concerns[:3])}。结论需由医生复核。"
        elif evaluated:
            summary = "已完成具备足够数据的规则评估，暂未触发重点关注规则，仍需结合完整资料由医生复核。"
        else:
            summary = "当前数据不足以完成有效模型评估，请补充必要指标后再由专业人员复核。"
        missing_advice = [
            f"评估维度{index:02d}数据不足：建议补充{'、'.join(item.missing_indicators[:5])}"
            for index, item in enumerate(insufficient, start=1)
        ][:8]
        uncertainty = (
            f"本次检验报告未覆盖{len(insufficient)}个专项评估维度；这些维度不代表低风险，"
            "如有症状或医生判断需要，应补充相应的专项检查。"
            if insufficient
            else "规则结果仅反映本次已确认指标，不包含症状、完整病史和全部临床信息。"
        )
        return ComprehensiveInterpretation(
            status=status,
            source="RULE_FALLBACK",
            model=None,
            summary=summary,
            priority_concerns=concerns[:10],
            cross_model_findings=[],
            recommendations=recommendations,
            missing_data_advice=missing_advice,
            followup_questions=["近期是否有明显不适、用药变化或生活方式变化？"],
            red_flags=[f"{name}结果需要医生优先复核" for name in high],
            uncertainty=uncertainty,
            disclaimer=DISCLAIMER,
        )
