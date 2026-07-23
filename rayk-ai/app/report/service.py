import base64
from html import escape
from io import BytesIO

from reportlab.lib import colors  # type: ignore[import-untyped]
from reportlab.lib.enums import TA_CENTER  # type: ignore[import-untyped]
from reportlab.lib.pagesizes import A4  # type: ignore[import-untyped]
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet  # type: ignore[import-untyped]
from reportlab.lib.units import mm  # type: ignore[import-untyped]
from reportlab.pdfbase import pdfmetrics  # type: ignore[import-untyped]
from reportlab.pdfbase.cidfonts import UnicodeCIDFont  # type: ignore[import-untyped]
from reportlab.pdfgen.canvas import Canvas  # type: ignore[import-untyped]
from reportlab.platypus import (  # type: ignore[import-untyped]
    Paragraph,
    SimpleDocTemplate,
    Spacer,
)

from app.schemas.report import ReportGenerateData, ReportGenerateRequest


class DemoReportService:
    """Produces the same concise health report used by customers and doctors."""

    def generate(self, request: ReportGenerateRequest) -> ReportGenerateData:
        focus_count = sum(result.risk_level in {"ATTENTION", "HIGH"} for result in request.results)
        rule_summary = (
            f"本次发现{focus_count}项需要优先关注的健康方向，已生成对应的健康随访计划。"
            if focus_count
            else "当前已确认的数据整体平稳，建议保持良好生活习惯并按需完成健康随访。"
        )
        summary = (
            request.interpretation.summary if request.interpretation is not None else rule_summary
        )
        title = f"{request.patient_display_name}的健康评估报告"
        return ReportGenerateData(
            title=title,
            summary=summary,
            sections=[
                "整体健康状态",
                "可能疾病与诊断参考",
                "重点健康问题",
                "健康随访",
            ],
            disclaimer="",
            pdf_base64=base64.b64encode(self._build_pdf(request, title, summary)).decode("ascii"),
        )

    def _build_pdf(self, request: ReportGenerateRequest, title: str, summary: str) -> bytes:
        pdfmetrics.registerFont(UnicodeCIDFont("STSong-Light"))
        styles = getSampleStyleSheet()
        normal = ParagraphStyle(
            "RaykNormal",
            parent=styles["BodyText"],
            fontName="STSong-Light",
            fontSize=9,
            leading=15,
            textColor=colors.HexColor("#1F2937"),
        )
        heading = ParagraphStyle(
            "RaykHeading",
            parent=normal,
            fontSize=13,
            leading=20,
            textColor=colors.HexColor("#0F766E"),
            spaceBefore=8,
            spaceAfter=5,
        )
        title_style = ParagraphStyle(
            "RaykTitle",
            parent=normal,
            alignment=TA_CENTER,
            fontSize=19,
            leading=28,
            textColor=colors.HexColor("#0F4C45"),
            spaceAfter=6,
        )
        small = ParagraphStyle("RaykSmall", parent=normal, fontSize=8, leading=12)
        stream = BytesIO()
        document = SimpleDocTemplate(
            stream,
            pagesize=A4,
            rightMargin=16 * mm,
            leftMargin=16 * mm,
            topMargin=14 * mm,
            bottomMargin=16 * mm,
            title=title,
            author="Rayk AI",
        )
        story = [
            Paragraph(self._safe(title), title_style),
            Paragraph(f"报告编号：{self._safe(request.report_no)}", small),
            Paragraph(f"报告日期：{self._safe(request.published_at or '-')}", small),
            Spacer(1, 5 * mm),
            Paragraph("一、整体健康状态", heading),
        ]
        profile_summary = self._profile_summary(request)
        if profile_summary:
            story.extend([Paragraph(profile_summary, small), Spacer(1, 2 * mm)])
        story.append(Paragraph(self._safe(summary), normal))
        if request.interpretation is not None:
            if request.interpretation.red_flags:
                story.extend(
                    [
                        Spacer(1, 2 * mm),
                        Paragraph(
                            "<b>需优先关注：</b>"
                            + self._safe("；".join(request.interpretation.red_flags[:3])),
                            small,
                        ),
                    ]
                )
            story.extend(
                [
                    Spacer(1, 2 * mm),
                    Paragraph(
                        "<b>结果局限：</b>" + self._safe(request.interpretation.uncertainty),
                        small,
                    ),
                ]
            )

        story.extend(
            [
                Spacer(1, 3 * mm),
                Paragraph("二、可能疾病与诊断参考", heading),
                Paragraph(
                    "以下内容是基于现有检验指标、健康档案和问卷信息形成的鉴别诊断线索，"
                    "用于帮助医生确定进一步问诊和检查方向，不是疾病确诊结果。",
                    small,
                ),
                Spacer(1, 2 * mm),
            ]
        )
        diagnostic_references = (
            request.interpretation.diagnostic_references
            if request.interpretation is not None
            else []
        )
        if diagnostic_references:
            for reference in diagnostic_references[:5]:
                story.extend(
                    [
                        Paragraph(
                            f"<b>{self._safe(reference.condition_name)}</b>"
                            f" · {self._assessment_label(reference.assessment)}",
                            normal,
                        ),
                        Paragraph(
                            f"<b>综合判断：</b>{self._safe(reference.rationale)}",
                            small,
                        ),
                        Paragraph(
                            "<b>主要线索：</b>"
                            + self._safe(
                                "；".join(reference.supporting_evidence)
                                or "当前仅有有限的相关风险信号"
                            ),
                            small,
                        ),
                    ]
                )
                if reference.recommended_department:
                    story.append(
                        Paragraph(
                            f"<b>建议咨询：</b>{self._safe(reference.recommended_department)}",
                            small,
                        )
                    )
                story.append(Spacer(1, 3 * mm))
        else:
            story.append(
                Paragraph(
                    "当前资料不足以形成有证据支持的疾病候选。该结果不代表已排除疾病；"
                    "如存在不适症状，仍应由医生结合完整病史和进一步检查判断。",
                    normal,
                )
            )

        story.extend([Spacer(1, 3 * mm), Paragraph("三、重点健康问题", heading)])
        focus = [item for item in request.results if item.risk_level in {"ATTENTION", "HIGH"}][:3]
        if focus:
            for result in focus:
                evidence = "；".join(result.evidence[:3]) or "本次数据提示该方向需要持续关注"
                next_step = (
                    result.recommendations[0]
                    if result.recommendations
                    else "结合后续健康随访持续观察变化"
                )
                level = "建议重点关注" if result.risk_level == "HIGH" else "建议改善"
                story.extend(
                    [
                        Paragraph(
                            f"<b>{self._safe(self._focus_name(result.model_code))}</b>"
                            f" · {level}",
                            normal,
                        ),
                        Paragraph(f"主要发现：{self._safe(evidence)}", small),
                        Paragraph(f"下一步：{self._safe(next_step)}", small),
                        Spacer(1, 2 * mm),
                    ]
                )
        else:
            story.append(Paragraph("当前已确认的数据未提示需要优先改善的健康问题。", normal))
        story.extend(
            [
                Spacer(1, 3 * mm),
                Paragraph("四、健康随访", heading),
                Paragraph(
                    "已根据本次重点健康问题生成本周健康计划，请在健康随访中完成任务并提交反馈。",
                    normal,
                ),
            ]
        )
        document.build(
            story,
            onFirstPage=self._add_page_footer,
            onLaterPages=self._add_page_footer,
        )
        return stream.getvalue()

    @staticmethod
    def _add_page_footer(canvas: Canvas, document: SimpleDocTemplate) -> None:
        canvas.saveState()
        canvas.setFont("STSong-Light", 7)
        canvas.setFillColor(colors.HexColor("#718096"))
        canvas.drawString(document.leftMargin, 8 * mm, "Rayk AI 健康评估报告")
        canvas.drawRightString(
            A4[0] - document.rightMargin,
            8 * mm,
            f"第 {canvas.getPageNumber()} 页",
        )
        canvas.restoreState()

    @staticmethod
    def _safe(value: object) -> str:
        return escape(str(value)).replace("\n", "<br/>")

    @classmethod
    def _profile_summary(cls, request: ReportGenerateRequest) -> str:
        context = request.patient_context
        if context is None:
            return ""
        parts: list[str] = []
        gender = {"MALE": "男", "FEMALE": "女"}.get(context.gender, "")
        if gender:
            parts.append(f"性别：{gender}")
        if context.age is not None:
            parts.append(f"年龄：{context.age}岁")
        if context.bmi is not None:
            parts.append(f"BMI：{context.bmi}")
        return cls._safe("　".join(parts))

    @staticmethod
    def _assessment_label(value: str) -> str:
        return {
            "RISK_SIGNAL": "存在相关风险信号",
            "POSSIBLE": "建议结合临床排查",
            "PRIORITY_REVIEW": "建议医生优先排查",
        }.get(value, "建议结合临床排查")

    @staticmethod
    def _focus_name(model_code: str) -> str:
        return {
            "GLUCOSE_METABOLISM": "糖代谢健康",
            "LIPID_CARDIOVASCULAR": "心血管与血脂健康",
            "CHRONIC_INFLAMMATION": "炎症相关健康",
            "LIVER_METABOLIC": "肝脏与代谢健康",
            "KIDNEY_ELECTROLYTE": "肾脏与电解质健康",
            "HEMATOLOGY_ANEMIA": "血液与营养状态",
            "THYROID_HORMONE": "甲状腺健康",
            "BODY_COMPOSITION": "体重与身体成分",
            "HPA_ADRENAL": "睡眠与恢复",
            "NUTRITION_MICRONUTRIENT": "营养状态",
            "GUT_BARRIER": "消化与肠道健康",
            "MENTAL_EMOTIONAL": "心理与情绪健康",
        }.get(model_code, "健康状态关注")
