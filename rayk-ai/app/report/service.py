import base64
from io import BytesIO

from reportlab.lib import colors  # type: ignore[import-untyped]
from reportlab.lib.enums import TA_CENTER  # type: ignore[import-untyped]
from reportlab.lib.pagesizes import A4  # type: ignore[import-untyped]
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet  # type: ignore[import-untyped]
from reportlab.lib.units import mm  # type: ignore[import-untyped]
from reportlab.pdfbase import pdfmetrics  # type: ignore[import-untyped]
from reportlab.pdfbase.cidfonts import UnicodeCIDFont  # type: ignore[import-untyped]
from reportlab.platypus import KeepTogether, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle  # type: ignore[import-untyped]

from app.schemas.report import ReportGenerateData, ReportGenerateRequest


class DemoReportService:
    """Produces the same concise health report used by customers and doctors."""

    def generate(self, request: ReportGenerateRequest) -> ReportGenerateData:
        focus_count = sum(result.risk_level in {"ATTENTION", "HIGH"} for result in request.results)
        summary = (
            f"本次发现{focus_count}项需要优先关注的健康方向，已生成对应的健康随访计划。"
            if focus_count
            else "当前已确认的数据整体平稳，建议保持良好生活习惯并按需完成健康随访。"
        )
        title = f"{request.patient_display_name}的健康评估报告"
        return ReportGenerateData(
            title=title,
            summary=summary,
            sections=["整体健康状态", "重点健康问题", "关键依据", "优先改善方向", "健康随访"],
            disclaimer="",
            pdf_base64=base64.b64encode(self._build_pdf(request, title, summary)).decode("ascii"),
        )

    def _build_pdf(self, request: ReportGenerateRequest, title: str, summary: str) -> bytes:
        pdfmetrics.registerFont(UnicodeCIDFont("STSong-Light"))
        styles = getSampleStyleSheet()
        normal = ParagraphStyle("RaykNormal", parent=styles["BodyText"], fontName="STSong-Light", fontSize=9, leading=15, textColor=colors.HexColor("#1F2937"))
        heading = ParagraphStyle("RaykHeading", parent=normal, fontSize=13, leading=20, textColor=colors.HexColor("#0F766E"), spaceBefore=8, spaceAfter=5)
        title_style = ParagraphStyle("RaykTitle", parent=normal, alignment=TA_CENTER, fontSize=19, leading=28, textColor=colors.HexColor("#0F4C45"), spaceAfter=6)
        small = ParagraphStyle("RaykSmall", parent=normal, fontSize=8, leading=12)
        stream = BytesIO()
        document = SimpleDocTemplate(stream, pagesize=A4, rightMargin=16 * mm, leftMargin=16 * mm, topMargin=14 * mm, bottomMargin=16 * mm, title=title, author="Rayk AI")
        story = [Paragraph(title, title_style), Paragraph(f"报告编号：{request.report_no}", small), Paragraph(f"报告日期：{request.published_at or '-'}", small), Spacer(1, 5 * mm), Paragraph("一、整体健康状态", heading), Paragraph(summary, normal)]
        story.extend([Spacer(1, 3 * mm), Paragraph("二、重点健康问题", heading)])
        focus = [item for item in request.results if item.risk_level in {"ATTENTION", "HIGH"}][:3]
        if focus:
            for result in focus:
                evidence = "；".join(result.evidence[:3]) or "本次数据提示该方向需要持续关注"
                next_step = result.recommendations[0] if result.recommendations else "结合后续健康随访持续观察变化"
                level = "建议重点关注" if result.risk_level == "HIGH" else "建议改善"
                story.extend([Paragraph(f"<b>{self._focus_name(result.model_code)}</b> · {level}", normal), Paragraph(f"依据：{evidence}", small), Paragraph(f"下一步：{next_step}", small), Spacer(1, 2 * mm)])
        else:
            story.append(Paragraph("当前已确认的数据未提示需要优先改善的健康问题。", normal))
        story.extend([Spacer(1, 3 * mm), Paragraph("三、关键依据", heading)])
        rows = [["指标", "结果", "参考范围"]]
        for indicator in request.indicators:
            low = "" if indicator.reference_low is None else str(indicator.reference_low)
            high = "" if indicator.reference_high is None else str(indicator.reference_high)
            rows.append([indicator.name, f"{indicator.value} {indicator.unit}", "-" if not low and not high else f"{low} - {high}"])
        story.append(self._table(rows, [58 * mm, 58 * mm, 44 * mm], normal))
        recommendations = list(dict.fromkeys(item for result in focus for item in result.recommendations))[:3]
        directions = [Paragraph("四、优先改善方向", heading)]
        for item in recommendations or ["保持规律作息、均衡饮食与适量运动，并在有新的检验报告时进行复评。"]:
            directions.append(Paragraph(f"• {item}", normal))
        story.extend([Spacer(1, 3 * mm), KeepTogether(directions), Spacer(1, 3 * mm), Paragraph("五、健康随访", heading), Paragraph("已根据本次重点健康问题生成本周健康计划，请在健康随访中完成任务并提交反馈。", normal)])
        document.build(story)
        return stream.getvalue()

    @staticmethod
    def _focus_name(model_code: str) -> str:
        return {
            "GLUCOSE_METABOLISM": "糖代谢健康", "LIPID_CARDIOVASCULAR": "心血管与血脂健康", "CHRONIC_INFLAMMATION": "炎症相关健康", "LIVER_METABOLIC": "肝脏与代谢健康", "KIDNEY_ELECTROLYTE": "肾脏与电解质健康", "HEMATOLOGY_ANEMIA": "血液与营养状态", "THYROID_HORMONE": "甲状腺健康", "SEX_HORMONE": "内分泌健康", "HPA_ADRENAL": "睡眠与恢复", "NUTRITION_MICRONUTRIENT": "营养状态", "GUT_BARRIER": "消化与肠道健康", "HEAVY_METAL_EXPOSURE": "环境暴露健康",
        }.get(model_code, "健康状态关注")

    @staticmethod
    def _table(rows: list[list[str]], widths: list[float], style: ParagraphStyle) -> Table:
        formatted = [[Paragraph(str(cell), style) for cell in row] for row in rows]
        table = Table(formatted, colWidths=widths, repeatRows=1)
        table.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#DDF3ED")), ("TEXTCOLOR", (0, 0), (-1, 0), colors.HexColor("#0F4C45")), ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#C8D8D3")), ("VALIGN", (0, 0), (-1, -1), "TOP"), ("LEFTPADDING", (0, 0), (-1, -1), 6), ("RIGHTPADDING", (0, 0), (-1, -1), 6), ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5)]))
        return table
