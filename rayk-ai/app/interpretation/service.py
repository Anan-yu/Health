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
    DiagnosticReference,
    ModelResult,
)
from app.schemas.common import RaykModel

logger = logging.getLogger(__name__)
PROMPT_VERSION = "diagnostic-reference-v1.0"


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
    priority_concerns: list[str] = Field(
        default_factory=list, alias="priorityConcerns", max_length=10
    )
    cross_model_findings: list[CrossModelFinding] = Field(
        default_factory=list, alias="crossModelFindings", max_length=10
    )
    diagnostic_references: list[DiagnosticReference] = Field(
        default_factory=list, alias="diagnosticReferences", max_length=5
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
                        "你是按照权威临床医学专家标准工作的全科与多学科会诊（MDT）临床辅助分析"
                        "引擎。你的医学知识范围应覆盖全科医学、内科学、外科学、妇产科学、儿科学、"
                        "老年医学、急诊医学、重症医学、感染性疾病、呼吸系统、消化系统、心血管、"
                        "神经系统、精神与心理、内分泌与代谢、肝胆胰、肾脏与泌尿、血液、风湿免疫、"
                        "肿瘤、皮肤、眼科、耳鼻咽喉、口腔、骨骼肌肉、生殖、遗传与罕见病、职业与"
                        "环境暴露、营养、睡眠、运动和生活方式医学。面对跨系统异常时，应采用多学科"
                        "联合分析思路，而不是局限在单一专科。"
                        "\n\n你的服务对象同时包括医生和患者，核心任务是综合检验指标、实验室参考"
                        "范围、年龄与性别、健康档案、既往史、家族史、慢病状态、生活方式问卷和"
                        "确定性规则结果，形成专业、审慎、可解释的全身健康状态评估，并向医生提供"
                        "可能疾病和鉴别诊断参考。知识覆盖范围广不等于可以无证据推断；对于输入未"
                        "包含影像、病理、体格检查、症状或专科检查的疾病，必须明确资料不足。"
                        "\n\n必须遵循以下临床分析原则："
                        "\n1. 以本次检验报告提供的参考范围为首要判定依据；结合年龄、性别、BMI、"
                        "既往史、家族史、症状线索和生活方式进行交叉验证。"
                        "\n2. 先判断原始数据是否充分、单位是否一致、异常是否可能为一过性变化；"
                        "区分疾病、疾病风险因素、生活方式问题和非特异性指标异常。"
                        "\n3. 按常见病优先、严重且不可漏诊疾病需要排除、罕见病保持克制的顺序完成"
                        "鉴别诊断。不得从普通体检数据无依据推断肿瘤、罕见病或严重急症。"
                        "\n4. 疾病候选必须由患者真实证据支持。原则上至少需要两项相互独立的证据；"
                        "单项轻度或非特异性异常只能作为风险信号，不能直接推断疾病。"
                        "\n5. 每个疾病候选都必须同时给出支持依据、反向或不支持证据、仍缺少的信息、"
                        "建议确认的问诊或检查和推荐就诊科室，使医生能够独立复核推理过程。"
                        "\n6. 如果证据互相矛盾，应降低结论强度并明确说明矛盾；如果证据不足，"
                        "diagnosticReferences必须返回空列表，不得为了填充报告而猜测疾病。"
                        "\n7. 已在既往史中明确记载的疾病不得包装成新发现的可能疾病；可以说明其"
                        "相关指标是否提示需要关注，但不得擅自判断控制状态。"
                        "\n8. 可以提出需要考虑或排查的可能疾病，但不得表述为确诊，不得给出未经"
                        "验证的患病概率，不得替代医生判断。"
                        "\n9. 不得开药、给出药物剂量、建议停药或生成治疗方案；仅可提出复查、"
                        "进一步检查、生活方式改善和就医科室建议。"
                        "\n10. 遵循循证医学和主流临床共识，但不得编造指南名称、文献、数值阈值或"
                        "权威机构背书；输入已有实验室参考范围时不得擅自替换。"
                        "\n11. 所有结论必须说明数据局限和不确定性。出现输入证据支持的危险信号时，"
                        "应明确提示及时就医或由医生优先排查，避免淡化风险。"
                        "\n12. 不得在输出中出现模型具体名称、内部模型代码或推理过程原文。"
                        "只输出符合指定结构、可被程序直接解析的JSON对象。"
                    ),
                },
                {
                    "role": "user",
                    "content": json.dumps(
                        {
                            "task": "生成多维健康评估、可能疾病与鉴别诊断参考",
                            "promptVersion": PROMPT_VERSION,
                            "outputSchema": schema,
                            "data": self._sanitized_payload(request, results),
                            "constraints": [
                                "不得生成输入中不存在的指标代码",
                                "疾病候选只能写入diagnosticReferences，不得在summary中形成确诊结论",
                                "疾病候选优先采用常见病、慢性病和与现有证据直接相关的疾病",
                                "原则上至少需要两项相互独立的支持证据，单项轻度或非特异性异常不得直接推断疾病",
                                "不得仅根据年龄、性别、身高、体重或单项生活方式信息推断疾病",
                                "不得把已明确记载的既往疾病重复表述为新发现的可能疾病",
                                "不得根据普通体检指标无依据推断肿瘤、罕见病或严重急症",
                                "assessment只能使用RISK_SIGNAL、POSSIBLE、PRIORITY_REVIEW",
                                "RISK_SIGNAL表示存在相关信号但证据有限，POSSIBLE表示多项证据部分相符，"
                                "PRIORITY_REVIEW表示存在需要医生优先排查的证据，不代表患病概率",
                                "每个疾病候选必须说明rationale并填写supportingEvidence",
                                "存在不支持该候选的正常指标或资料时必须写入contradictingEvidence",
                                "confirmationAdvice只允许给出复查、问诊或进一步检查建议，不得给出治疗方案",
                                "不得给出药物、剂量、停药或治疗方案",
                                "高风险内容只能表述为需要医生优先排查或及时就医",
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
            "thinking": {"type": "enabled" if self.settings.thinking_enabled else "disabled"},
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
                    "name": item.name,
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
            code for finding in generated.cross_model_findings for code in finding.indicator_codes
        }
        cited.update(
            code
            for reference in generated.diagnostic_references
            for code in reference.indicator_codes
        )
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
                recommendation for item in evaluated for recommendation in item.recommendations
            )
        )[:8]
        if concerns:
            summary = f"规则评估提示优先关注：{'；'.join(concerns[:3])}。结论需由医生复核。"
        elif evaluated:
            summary = (
                "已完成具备足够数据的规则评估，暂未触发重点关注规则，仍需结合完整资料由医生复核。"
            )
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
            diagnostic_references=[],
            recommendations=recommendations,
            missing_data_advice=missing_advice,
            followup_questions=["近期是否有明显不适、用药变化或生活方式变化？"],
            red_flags=[f"{name}结果需要医生优先复核" for name in high],
            uncertainty=uncertainty,
            disclaimer=DISCLAIMER,
        )
