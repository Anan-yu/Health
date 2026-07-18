from app.core.constants import DISCLAIMER
from app.schemas.report import ReportGenerateData, ReportGenerateRequest


class DemoReportService:
    def generate(self, request: ReportGenerateRequest) -> ReportGenerateData:
        attention = [result.model_name for result in request.results if result.risk_level != "LOW"]
        summary = (
            f"演示评估中建议关注：{'、'.join(attention)}。结果必须由医生或健康管理师审核。"
            if attention
            else "演示评估未发现需重点关注的模型，仍需由专业人员审核。"
        )
        return ReportGenerateData(
            title=f"{request.patient_display_name}的健康管理评估报告（演示）",
            summary=summary,
            sections=["指标概览", "模型结果", "生活方式建议", "人工审核意见"],
            disclaimer=DISCLAIMER,
        )

