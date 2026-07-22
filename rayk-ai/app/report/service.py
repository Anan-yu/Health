import base64
from io import BytesIO

from reportlab.lib import colors  # type: ignore[import-untyped]
from reportlab.lib.enums import TA_CENTER  # type: ignore[import-untyped]
from reportlab.lib.pagesizes import A4  # type: ignore[import-untyped]
from reportlab.lib.styles import (  # type: ignore[import-untyped]
    ParagraphStyle,
    getSampleStyleSheet,
)
from reportlab.lib.units import mm  # type: ignore[import-untyped]
from reportlab.pdfbase import pdfmetrics  # type: ignore[import-untyped]
from reportlab.pdfbase.cidfonts import UnicodeCIDFont  # type: ignore[import-untyped]
from reportlab.platypus import (  # type: ignore[import-untyped]
    KeepTogether,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)

from app.schemas.report import ReportGenerateData, ReportGenerateRequest


class DemoReportService:
    """Produces a real, downloadable PDF health-management report artifact."""

    def generate(self, request: ReportGenerateRequest) -> ReportGenerateData:
        attention_count = sum(
            result.risk_level in {"ATTENTION", "HIGH"} for result in request.results
        )
        summary = (
            request.interpretation.summary
            if request.interpretation is not None
            else (
                f"本次评估有{attention_count}个健康维度建议重点关注，结果须由医生审核。"
                if attention_count
                else "本次评估未发现需重点关注的健康维度，仍建议由医生审核。"
            )
        )
        title = f"{request.patient_display_name}的健康管理评估报告（演示）"
        return ReportGenerateData(
            title=title,
            summary=summary,
            sections=["指标概览", "评估结果", "生活方式建议", "医生审核意见"],
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
            author="RayK A1",
        )
        story = [
            Paragraph(title, title_style),
            Paragraph(f"报告编号：{request.report_no}", small),
            Paragraph(f"报告日期：{request.published_at or '-'}", small),
            Spacer(1, 5 * mm),
            Paragraph("一、评估摘要", heading),
            Paragraph(summary, normal),
        ]
        if request.interpretation is not None:
            story.append(Paragraph(f"不确定性：{request.interpretation.uncertainty}", small))
        story.extend([Spacer(1, 3 * mm), Paragraph("二、评估结果", heading)])

        result_rows = [["评估维度", "评分", "风险等级", "主要依据"]]
        for index, result in enumerate(request.results, start=1):
            evidence = "；".join(result.evidence) or "未触发关注规则"
            score = "-" if result.score is None else str(result.score)
            result_rows.append([f"维度 {index:02d}", score, result.risk_level, evidence])
        story.append(self._table(result_rows, [39 * mm, 16 * mm, 23 * mm, 82 * mm], normal))

        story.extend([Spacer(1, 3 * mm), Paragraph("三、指标概览", heading)])
        indicator_rows = [["指标", "数值", "参考范围"]]
        for indicator in request.indicators:
            low = "" if indicator.reference_low is None else str(indicator.reference_low)
            high = "" if indicator.reference_high is None else str(indicator.reference_high)
            reference = "-" if not low and not high else f"{low} - {high}"
            indicator_rows.append(
                [indicator.name, f"{indicator.value} {indicator.unit}", reference]
            )
        story.append(self._table(indicator_rows, [58 * mm, 58 * mm, 44 * mm], normal))

        recommendations = [
            recommendation
            for result in request.results
            for recommendation in result.recommendations
        ]
        recommendation_section = [Paragraph("四、生活方式建议", heading)]
        for recommendation in dict.fromkeys(recommendations):
            recommendation_section.append(Paragraph(f"• {recommendation}", normal))
        story.extend([Spacer(1, 3 * mm), KeepTogether(recommendation_section)])

        story.extend(
            [
                Spacer(1, 3 * mm),
                Paragraph("五、医生审核意见", heading),
                Paragraph(request.doctor_opinion or "暂未填写。", normal),
            ]
        )
        document.build(story)
        return stream.getvalue()

    @staticmethod
    def _table(rows: list[list[str]], widths: list[float], style: ParagraphStyle) -> Table:
        formatted = [[Paragraph(str(cell), style) for cell in row] for row in rows]
        table = Table(formatted, colWidths=widths, repeatRows=1)
        table.setStyle(
            TableStyle(
                [
                    ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#DDF3ED")),
                    ("TEXTCOLOR", (0, 0), (-1, 0), colors.HexColor("#0F4C45")),
                    ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#C8D8D3")),
                    ("VALIGN", (0, 0), (-1, -1), "TOP"),
                    ("LEFTPADDING", (0, 0), (-1, -1), 6),
                    ("RIGHTPADDING", (0, 0), (-1, -1), 6),
                    ("TOPPADDING", (0, 0), (-1, -1), 5),
                    ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
                ]
            )
        )
        return table
