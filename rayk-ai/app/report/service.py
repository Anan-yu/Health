import base64
import re
from html import escape
from io import BytesIO

from reportlab.lib import colors  # type: ignore[import-untyped]
from reportlab.lib.enums import TA_CENTER  # type: ignore[import-untyped]
from reportlab.lib.pagesizes import A4  # type: ignore[import-untyped]
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet  # type: ignore[import-untyped]
from reportlab.lib.units import mm  # type: ignore[import-untyped]
from reportlab.pdfbase import pdfmetrics  # type: ignore[import-untyped]
from reportlab.pdfbase.ttfonts import TTFont  # type: ignore[import-untyped]
from reportlab.pdfgen.canvas import Canvas  # type: ignore[import-untyped]
from reportlab.platypus import Paragraph, SimpleDocTemplate, Spacer  # type: ignore[import-untyped]

from app.core.constants import DISCLAIMER
from app.schemas.report import ReportGenerateData, ReportGenerateRequest

_REPORT_FONT = "WQYMicroHei"
_REPORT_BOLD_FONT = "WQYZenHei"
_REPORT_FONT_PATH = "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc"
_REPORT_BOLD_FONT_PATH = "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc"


class DemoReportService:
    """Produces the same concise health report used by customers and doctors."""

    def generate(self, request: ReportGenerateRequest) -> ReportGenerateData:
        focus_count = sum(
            result.status == "EVALUATED" and result.risk_level in {"ATTENTION", "HIGH"}
            for result in request.results
        )
        has_effective_data = bool(request.indicators) or any(
            result.status == "EVALUATED" for result in request.results
        )
        rule_summary = (
            f"本次有{focus_count}个健康方向需要关注，请结合重点发现和下一步行动持续管理。"
            if focus_count
            else (
                "当前已确认的数据未触发重点关注规则，建议按需复评。"
                if has_effective_data
                else "当前数据不足以形成有效健康结论，建议补充资料后复评。"
            )
        )
        summary = (
            request.interpretation.summary if request.interpretation is not None else rule_summary
        )
        title = f"{request.patient_display_name}的健康评估报告"
        sections = ["整体健康状态", "本次重点发现"]
        if request.interpretation and request.interpretation.diagnostic_references:
            sections.append("疾病推断参考")
        sections.extend(["建议补充的信息", "下一步健康行动", "报告限制与免责声明"])
        return ReportGenerateData(
            title=title,
            summary=summary,
            sections=sections,
            disclaimer=DISCLAIMER,
            pdf_base64=base64.b64encode(self._build_pdf(request, title, summary)).decode("ascii"),
        )

    def _build_pdf(self, request: ReportGenerateRequest, title: str, summary: str) -> bytes:
        self._register_report_fonts()
        styles = getSampleStyleSheet()
        normal = ParagraphStyle(
            "RaykNormal",
            parent=styles["BodyText"],
            fontName=_REPORT_FONT,
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
            author="致宇健康",
        )
        story = [
            Paragraph(self._safe(title), title_style),
            Paragraph(f"报告日期：{self._safe(request.published_at or '-')}", small),
            Spacer(1, 5 * mm),
            Paragraph("一、整体健康状态", heading),
        ]
        profile_summary = self._profile_summary(request)
        if profile_summary:
            story.extend([Paragraph(profile_summary, small), Spacer(1, 2 * mm)])
        story.extend(
            [
                Paragraph(
                    "<b>综合结论：</b>" + self._safe(self._display_text(summary)),
                    normal,
                ),
                Spacer(1, 1.5 * mm),
                Paragraph(
                    "<b>本次资料覆盖：</b>" + self._safe(self._coverage_summary(request)),
                    small,
                ),
                Paragraph(
                    "<b>检验概况：</b>" + self._safe(self._indicator_summary(request)),
                    small,
                ),
            ]
        )
        health_background = self._health_background(request)
        if health_background:
            story.append(
                Paragraph(
                    "<b>健康背景：</b>" + self._safe(health_background),
                    small,
                )
            )
        interpretation = request.interpretation
        priority_concerns = interpretation.priority_concerns if interpretation is not None else []
        if not priority_concerns:
            priority_concerns = [
                evidence
                for result in request.results
                if result.status == "EVALUATED" and result.risk_level in {"ATTENTION", "HIGH"}
                for evidence in result.evidence[:1]
            ]
        story.extend([Spacer(1, 3 * mm), Paragraph("二、本次重点发现", heading)])
        if priority_concerns:
            for concern in priority_concerns[:8]:
                story.append(Paragraph("• " + self._safe(self._display_text(concern)), normal))
        elif request.indicators:
            story.append(Paragraph("当前已确认的数据未触发重点关注规则。", normal))
        else:
            story.append(Paragraph("当前数据不足，尚不能形成有效的重点发现。", normal))

        diagnostic_references = (
            interpretation.diagnostic_references if interpretation is not None else []
        )
        section_number = 3
        if diagnostic_references:
            story.extend(
                [
                    Spacer(1, 3 * mm),
                    Paragraph("三、疾病推断参考", heading),
                    Paragraph(
                        "以下内容用于帮助医生确定进一步问诊、检查和健康管理重点，不代表疾病诊断或治疗处方。",
                        small,
                    ),
                ]
            )
            for reference in diagnostic_references[:5]:
                story.extend(
                    [
                        Paragraph(
                            f"可能疾病：<b>{self._safe(reference.condition_name)}</b>"
                            f" · {self._assessment_label(reference.assessment)}",
                            normal,
                        ),
                        Paragraph(
                            f"<b>综合判断：</b>"
                            f"{self._safe(self._display_text(reference.rationale))}",
                            small,
                        ),
                        Paragraph(
                            "<b>主要线索：</b>"
                            + self._safe(
                                self._display_text(
                                    "；".join(reference.supporting_evidence)
                                    or "当前仅有有限的相关风险信号"
                                )
                            ),
                            small,
                        ),
                    ]
                )
                if reference.recommended_department:
                    story.append(
                        Paragraph(
                            "建议咨询科室："
                            f"<b>{self._safe(reference.recommended_department)}</b>",
                            small,
                        )
                    )
                story.extend(
                    [
                        Paragraph(
                            "<b>疾病治疗方案：</b>" + self._safe(self._treatment_plan(reference)),
                            small,
                        ),
                        Paragraph(
                            "<b>营养干预修复方案：</b>"
                            + self._safe(self._nutrition_intervention_plan(reference)),
                            small,
                        ),
                    ]
                )
                story.append(Spacer(1, 3 * mm))
            section_number = 4

        number_labels = {3: "三", 4: "四", 5: "五", 6: "六"}
        missing_data = interpretation.missing_data_advice if interpretation is not None else []
        story.extend(
            [
                Spacer(1, 3 * mm),
                Paragraph(f"{number_labels[section_number]}、建议补充的信息", heading),
            ]
        )
        if missing_data:
            for item in missing_data[:8]:
                story.append(Paragraph("• " + self._safe(self._display_text(item)), normal))
        else:
            story.append(Paragraph("当前没有额外的重点补充项，后续按医生意见复查。", normal))

        section_number += 1
        actions = interpretation.recommendations if interpretation is not None else []
        if not actions:
            actions = [
                recommendation
                for result in request.results
                if result.status == "EVALUATED" and result.risk_level in {"ATTENTION", "HIGH"}
                for recommendation in result.recommendations[:1]
            ]
        story.extend(
            [
                Spacer(1, 3 * mm),
                Paragraph(f"{number_labels[section_number]}、下一步健康行动", heading),
            ]
        )
        if actions:
            for item in actions[:5]:
                story.append(Paragraph("• " + self._safe(self._display_text(item)), normal))
        else:
            story.append(Paragraph("补充有效数据后，再制定与重点问题对应的健康行动。", normal))

        section_number += 1
        uncertainty = (
            interpretation.uncertainty
            if interpretation is not None
            else "本报告仅覆盖当前已提供的资料，不能据此诊断疾病或决定药物治疗。"
        )
        story.extend(
            [
                Spacer(1, 3 * mm),
                Paragraph(f"{number_labels[section_number]}、报告限制与免责声明", heading),
                Paragraph(
                    "<b>当前不能说明：</b>" + self._safe(self._display_text(uncertainty)), normal
                ),
                Spacer(1, 2 * mm),
                Paragraph("<b>免责声明：</b>" + self._safe(DISCLAIMER), small),
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
        canvas.setFont(_REPORT_FONT, 7)
        canvas.setFillColor(colors.HexColor("#718096"))
        canvas.drawString(document.leftMargin, 8 * mm, "致宇健康评估报告")
        canvas.drawRightString(
            A4[0] - document.rightMargin,
            8 * mm,
            f"第 {canvas.getPageNumber()} 页",
        )
        canvas.restoreState()

    @staticmethod
    def _register_report_fonts() -> None:
        registered = set(pdfmetrics.getRegisteredFontNames())
        if _REPORT_FONT not in registered:
            pdfmetrics.registerFont(TTFont(_REPORT_FONT, _REPORT_FONT_PATH))
        if _REPORT_BOLD_FONT not in registered:
            pdfmetrics.registerFont(TTFont(_REPORT_BOLD_FONT, _REPORT_BOLD_FONT_PATH))
        pdfmetrics.registerFontFamily(
            _REPORT_FONT,
            normal=_REPORT_FONT,
            bold=_REPORT_BOLD_FONT,
            italic=_REPORT_FONT,
            boldItalic=_REPORT_BOLD_FONT,
        )

    @staticmethod
    def _safe(value: object) -> str:
        return escape(str(value)).replace("\n", "<br/>")

    @staticmethod
    def _display_text(value: str) -> str:
        cleaned = re.sub(
            r"\s*[（(][A-Za-z][A-Za-z0-9_]*\s*=\s*[^）)]*[）)]",
            "",
            value,
        )
        cleaned = re.sub(
            r"\b[A-Za-z][A-Za-z0-9_]*\s*=\s*[-+]?\d+(?:\.\d+)?" r"(?:\s*[A-Za-z/%^0-9]+)?\b",
            "",
            cleaned,
        )
        cleaned = re.sub(r"\b[a-z]+(?:_[a-z0-9]+)+\b", "", cleaned)
        translations = (
            ("VERY_HIGH", "很高"),
            ("VERY_POOR", "很差"),
            ("VERY_GOOD", "很好"),
            ("EXCELLENT", "优秀"),
            ("SOMETIMES", "有时"),
            ("3_5_PER_WEEK", "每周3至5次"),
            ("1_2_PER_WEEK", "每周1至2次"),
            ("OCCASIONAL", "偶尔"),
            ("REGULAR", "经常"),
            ("DAILY", "几乎每天"),
            ("RARELY", "很少"),
            ("ALWAYS", "总是"),
            ("NEVER", "从不"),
            ("MEDIUM", "中等"),
            ("NORMAL", "正常"),
            ("ABNORMAL", "异常"),
            ("CURRENT", "当前"),
            ("FORMER", "既往"),
            ("POOR", "较差"),
            ("FAIR", "一般"),
            ("GOOD", "良好"),
            ("HIGH", "较高"),
            ("LOW", "较低"),
            ("NONE", "无"),
            ("YES", "有"),
            ("NO", "无"),
            ("BMI", "体质指数"),
        )
        for source, target in translations:
            cleaned = cleaned.replace(source, target)
        return re.sub(r"\s+", " ", cleaned).strip()

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
            parts.append(f"体质指数：{context.bmi}")
        return cls._safe("　".join(parts))

    @classmethod
    def _coverage_summary(cls, request: ReportGenerateRequest) -> str:
        indicator_count = len(request.indicators)
        evaluated_count = sum(result.status == "EVALUATED" for result in request.results)
        focus_count = sum(
            result.status == "EVALUATED" and result.risk_level in {"ATTENTION", "HIGH"}
            for result in request.results
        )
        parts = [f"共纳入{indicator_count}项检验指标"]
        if request.results:
            parts.append(f"{evaluated_count}个健康维度具备有效数据")
            parts.append(f"其中{focus_count}个方向建议持续关注")
        if request.patient_context is not None:
            parts.append("健康档案和问卷信息已共同纳入评估")
        return "，".join(parts) + "。"

    @staticmethod
    def _indicator_summary(request: ReportGenerateRequest) -> str:
        normal_count = 0
        abnormal_count = 0
        unclassified_count = 0
        for indicator in request.indicators:
            has_reference = (
                indicator.reference_low is not None or indicator.reference_high is not None
            )
            if not has_reference:
                unclassified_count += 1
                continue
            below = (
                indicator.reference_low is not None and indicator.value < indicator.reference_low
            )
            above = (
                indicator.reference_high is not None and indicator.value > indicator.reference_high
            )
            if below or above:
                abnormal_count += 1
            else:
                normal_count += 1
        parts: list[str] = []
        if normal_count:
            parts.append(f"{normal_count}项处于原报告参考范围")
        if abnormal_count:
            parts.append(f"{abnormal_count}项超出原报告参考范围")
        if unclassified_count:
            parts.append(f"{unclassified_count}项因缺少参考范围暂不判定")
        return ("，".join(parts) if parts else "本次没有可用于参考范围比较的检验指标") + "。"

    @classmethod
    def _health_background(cls, request: ReportGenerateRequest) -> str:
        context = request.patient_context
        if context is None:
            return ""
        parts: list[str] = []
        medical_history = cls._meaningful_text(context.medical_history)
        if medical_history:
            parts.append(f"既往情况：{medical_history}")
        family_history = cls._meaningful_text(context.family_history, keep_none=True)
        if family_history:
            parts.append(f"家族史：{family_history}")
        if not medical_history:
            chronic_fields = (
                ("糖尿病情况", context.diabetes_status),
                ("高血压情况", context.hypertension_status),
                ("血脂情况", context.dyslipidemia_status),
                ("脂肪肝情况", context.fatty_liver_status),
            )
            for label, value in chronic_fields:
                display = cls._meaningful_text(value)
                if display:
                    parts.append(f"{label}：{display}")
        lifestyle_fields = (
            ("吸烟情况", context.smoking_status),
            ("饮酒情况", context.alcohol_status),
            ("运动频率", context.exercise_frequency),
            ("睡眠质量", context.sleep_quality),
            ("压力水平", context.stress_level),
            ("近期心情", context.mood_status),
        )
        for label, value in lifestyle_fields:
            display = cls._meaningful_text(value, keep_none=label in {"吸烟情况", "饮酒情况"})
            if display:
                parts.append(f"{label}：{display}")
        if context.sleep_hours is not None:
            parts.append(f"平均睡眠：{context.sleep_hours}小时")
        if context.recent_dietary_pattern:
            parts.append(f"近期饮食：{cls._display_text(context.recent_dietary_pattern)}")
        return "；".join(parts[:6]) + ("。" if parts else "")

    @classmethod
    def _meaningful_text(cls, value: object | None, keep_none: bool = False) -> str:
        if value is None or not str(value).strip():
            return ""
        display = cls._display_text(str(value))
        if not keep_none and display in {"无", "否", "未知", "未填写", "暂无"}:
            return ""
        return cls._without_terminal_punctuation(display)

    @classmethod
    def _current_focus(cls, request: ReportGenerateRequest) -> str:
        if request.interpretation is not None and request.interpretation.priority_concerns:
            concerns = [
                cls._without_terminal_punctuation(cls._display_text(item))
                for item in request.interpretation.priority_concerns[:3]
                if item.strip()
            ]
            if concerns:
                return "；".join(concerns) + "。"
        evidence = [
            cls._without_terminal_punctuation(cls._display_text(item.evidence[0]))
            for item in request.results
            if item.risk_level in {"ATTENTION", "HIGH"} and item.evidence
        ][:3]
        return "；".join(evidence) + ("。" if evidence else "")

    @classmethod
    def _management_direction(cls, request: ReportGenerateRequest) -> str:
        if request.interpretation is not None:
            recommendations = [
                cls._without_terminal_punctuation(cls._display_text(item))
                for item in request.interpretation.recommendations[:2]
                if item.strip()
            ]
            if recommendations:
                return "；".join(recommendations) + "。"
        recommendations = [
            cls._without_terminal_punctuation(cls._display_text(result.recommendations[0]))
            for result in request.results
            if result.risk_level in {"ATTENTION", "HIGH"} and result.recommendations
        ][:2]
        return "；".join(recommendations) + ("。" if recommendations else "")

    @staticmethod
    def _without_terminal_punctuation(value: str) -> str:
        return re.sub(r"[。；;，,\s]+$", "", value)

    @staticmethod
    def _assessment_label(value: str) -> str:
        return {
            "RISK_SIGNAL": "存在相关风险信号",
            "POSSIBLE": "建议结合临床排查",
            "PRIORITY_REVIEW": "建议医生优先排查",
        }.get(value, "建议结合临床排查")

    @classmethod
    def _treatment_plan(cls, reference: object) -> str:
        plans = getattr(reference, "treatment_plan", None) or []
        cleaned = [cls._display_text(str(item)) for item in plans if str(item).strip()]
        if cleaned:
            return "；".join(cleaned)
        condition = cls._display_text(str(getattr(reference, "condition_name", "") or ""))
        if "幽门螺杆菌" in condition:
            return (
                "消化内科先核对症状、既往根除史、药物过敏和近期抗菌药使用；如确认需要根除，"
                "进入含铋四联根除治疗的标准路径，由医生选择具体药物组合和疗程；疗程后按医嘱"
                "复查呼气试验或粪便抗原，确认是否根除成功。"
            )
        if "颈动脉" in condition or "粥样硬化" in condition or "斑块" in condition:
            return (
                "心血管内科应完成动脉粥样硬化心血管风险分层，复核血压、完整血脂、糖代谢和"
                "颈动脉超声；在确认斑块与总体风险后，开展血脂、血压、血糖及吸烟等危险因素的"
                "强化管理，并由医生判断是否需要降脂或抗血小板等药物治疗及复查安排。"
            )
        if "脂肪肝" in condition or "脂肪性肝病" in condition:
            return (
                "消化内科或肝病科应先排查饮酒、病毒性肝炎、药物和代谢共病，并结合肝功能、"
                "血糖血脂及肝纤维化风险分层；如伴超重或肥胖，以体重管理和代谢共病干预为核心，"
                "医生根据分层决定是否需要进一步药物治疗和肝脏随访。"
            )
        department = cls._display_text(str(getattr(reference, "recommended_department", "") or ""))
        destination = department or "相关专科"
        return (
            f"尽快至{destination}完成病因与严重程度分层，带齐原报告、当前用药和症状记录；"
            "由医生依据本次证据明确核心治疗类别、复查项目与疗效评估时间，避免自行用药或调整治疗。"
        )

    @classmethod
    def _nutrition_intervention_plan(cls, reference: object) -> str:
        plans = getattr(reference, "nutrition_intervention_plan", None) or []
        cleaned = [cls._display_text(str(item)) for item in plans if str(item).strip()]
        if cleaned:
            return "；".join(cleaned)
        return (
            "建议由临床营养师或相关专科结合体重、肝肾功能、过敏史、当前用药和复查结果制定"
            "个体化饮食方案；不自行高剂量补充营养素。"
        )

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
