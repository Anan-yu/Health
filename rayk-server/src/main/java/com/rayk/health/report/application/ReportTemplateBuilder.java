package com.rayk.health.report.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.assessment.entity.HealthAssessmentEntity;
import com.rayk.health.indicator.entity.IndicatorValueEntity;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.report.entity.HealthReportEntity;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReportTemplateBuilder {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    private final ObjectMapper objectMapper;

    public ReportTemplateBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildHtml(
            PatientEntity patient,
            HealthAssessmentEntity assessment,
            HealthReportEntity report,
            List<IndicatorValueEntity> indicators) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("<title>").append(escapeHtml(report.getTitle())).append("</title>\n");
        html.append("<style>\n");
        html.append(css());
        html.append("</style>\n</head>\n<body>\n");

        // Header
        html.append("<div class=\"report-header\">\n");
        html.append("<h1>健康管理评估报告</h1>\n");
        html.append("<p class=\"report-no\">报告编号：").append(escapeHtml(report.getReportNo())).append("</p>\n");
        html.append("</div>\n");

        // Patient info
        html.append("<div class=\"section\">\n");
        html.append("<h2>一、基本信息</h2>\n");
        html.append("<table class=\"info-table\">\n");
        html.append("<tr><td class=\"label\">姓名</td><td>").append(escapeHtml(patient.getName())).append("</td>");
        html.append("<td class=\"label\">性别</td><td>").append(escapeHtml(genderText(patient.getGender()))).append("</td></tr>\n");
        if (patient.getBirthDate() != null) {
            html.append("<tr><td class=\"label\">出生日期</td><td>").append(patient.getBirthDate().format(DATE_FMT)).append("</td>");
        } else {
            html.append("<tr><td class=\"label\">出生日期</td><td>-</td>");
        }
        html.append("<td class=\"label\">报告日期</td><td>");
        if (report.getPublishedAt() != null) {
            html.append(report.getPublishedAt().toLocalDate().format(DATE_FMT));
        }
        html.append("</td></tr>\n");
        html.append("<tr><td class=\"label\">整体风险等级</td><td colspan=\"3\" class=\"risk-")
            .append(riskClass(assessment.getOverallRiskLevel())).append("\">")
            .append(escapeHtml(riskText(assessment.getOverallRiskLevel()))).append("</td></tr>\n");
        html.append("</table>\n</div>\n");

        // Model results
        html.append("<div class=\"section\">\n");
        html.append("<h2>二、评估模型结果</h2>\n");
        html.append(buildModelResults(assessment.getResultSnapshot()));
        html.append("</div>\n");

        // Indicator values
        html.append("<div class=\"section\">\n");
        html.append("<h2>三、检验指标数值</h2>\n");
        html.append(buildIndicatorTable(indicators));
        html.append("</div>\n");

        // Doctor opinion
        html.append("<div class=\"section\">\n");
        html.append("<h2>四、医生审核意见</h2>\n");
        html.append("<p class=\"opinion\">").append(escapeHtml(report.getDoctorOpinion() != null ? report.getDoctorOpinion() : "无")).append("</p>\n");
        html.append("</div>\n");

        // Summary
        html.append("<div class=\"section\">\n");
        html.append("<h2>五、报告摘要</h2>\n");
        html.append("<p>").append(escapeHtml(report.getSummary())).append("</p>\n");
        html.append("</div>\n");

        // Disclaimer
        html.append("<div class=\"disclaimer\">\n");
        html.append("<h3>重要声明</h3>\n");
        html.append("<p>").append(escapeHtml(report.getDisclaimer())).append("</p>\n");
        html.append("</div>\n");

        html.append("<div class=\"footer\">\n");
        html.append("<p>本报告由AI辅助生成，经医生人工审核。仅供健康管理参考，不构成医学诊断。</p>\n");
        html.append("<p>生成时间：");
        if (report.getPublishedAt() != null) {
            html.append(report.getPublishedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        html.append("</p>\n");
        html.append("</div>\n");

        html.append("</body>\n</html>");
        return html.toString();
    }

    private String buildModelResults(String resultSnapshot) {
        StringBuilder sb = new StringBuilder();
        try {
            JsonNode root = objectMapper.readTree(resultSnapshot);
            JsonNode results = root != null ? root.get("results") : null;
            if (results == null || !results.isArray() || results.isEmpty()) {
                return "<p>暂无评估模型结果数据。</p>\n";
            }
            sb.append("<table class=\"data-table\">\n");
            sb.append("<thead><tr><th>评估模型</th><th>分数</th><th>风险等级</th><th>证据摘要</th><th>建议</th></tr></thead>\n");
            sb.append("<tbody>\n");
            for (JsonNode item : results) {
                String modelCode = textOrDash(item, "modelCode");
                String modelName = textOrDash(item, "modelName");
                String score = item.has("score") ? item.get("score").asText() : "-";
                String riskLevel = textOrDash(item, "riskLevel");
                String evidence = textOrDash(item, "evidence");
                String recommendations = textOrDash(item, "recommendations");
                sb.append("<tr>");
                sb.append("<td>").append(escapeHtml(modelName.equals("-") ? modelCode : modelName)).append("</td>");
                sb.append("<td>").append(escapeHtml(score)).append("</td>");
                sb.append("<td class=\"risk-").append(riskClass(riskLevel)).append("\">")
                  .append(escapeHtml(riskText(riskLevel))).append("</td>");
                sb.append("<td class=\"cell-text\">").append(escapeHtml(truncate(evidence, 200))).append("</td>");
                sb.append("<td class=\"cell-text\">").append(escapeHtml(truncate(recommendations, 200))).append("</td>");
                sb.append("</tr>\n");
            }
            sb.append("</tbody>\n</table>\n");
        } catch (Exception e) {
            sb.append("<p>评估结果数据解析异常。</p>\n");
        }
        return sb.toString();
    }

    private String buildIndicatorTable(List<IndicatorValueEntity> indicators) {
        if (indicators == null || indicators.isEmpty()) {
            return "<p>暂无检验指标数据。</p>\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"data-table\">\n");
        sb.append("<thead><tr><th>指标名称</th><th>数值</th><th>单位</th><th>参考范围</th><th>异常标识</th></tr></thead>\n");
        sb.append("<tbody>\n");
        for (IndicatorValueEntity ind : indicators) {
            sb.append("<tr>");
            sb.append("<td>").append(escapeHtml(ind.getIndicatorName())).append("</td>");
            sb.append("<td>").append(formatValue(ind.getValue())).append("</td>");
            sb.append("<td>").append(escapeHtml(ind.getUnit() != null ? ind.getUnit() : "-")).append("</td>");
            sb.append("<td>").append(formatRange(ind.getReferenceLow(), ind.getReferenceHigh())).append("</td>");
            sb.append("<td class=\"flag-").append(flagClass(ind.getAbnormalFlag())).append("\">")
              .append(escapeHtml(flagText(ind.getAbnormalFlag()))).append("</td>");
            sb.append("</tr>\n");
        }
        sb.append("</tbody>\n</table>\n");
        return sb.toString();
    }

    private String css() {
        return """
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { font-family: "Microsoft YaHei", "PingFang SC", sans-serif; font-size: 14px; color: #333; line-height: 1.6; padding: 40px; max-width: 900px; margin: 0 auto; }
                .report-header { text-align: center; border-bottom: 3px solid #1a73e8; padding-bottom: 20px; margin-bottom: 30px; }
                .report-header h1 { font-size: 24px; color: #1a73e8; margin-bottom: 8px; }
                .report-no { color: #666; font-size: 13px; }
                .section { margin-bottom: 25px; }
                .section h2 { font-size: 16px; color: #1a73e8; border-left: 4px solid #1a73e8; padding-left: 10px; margin-bottom: 12px; }
                .info-table { width: 100%; border-collapse: collapse; }
                .info-table td { padding: 8px 12px; border: 1px solid #e0e0e0; }
                .info-table .label { background: #f5f7fa; font-weight: bold; width: 100px; }
                .data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
                .data-table th { background: #1a73e8; color: #fff; padding: 10px 8px; text-align: left; }
                .data-table td { padding: 8px; border: 1px solid #e0e0e0; vertical-align: top; }
                .data-table tbody tr:nth-child(even) { background: #f9fafb; }
                .cell-text { max-width: 200px; word-break: break-all; }
                .risk-LOW { color: #2e7d32; font-weight: bold; }
                .risk-MODERATE { color: #f57c00; font-weight: bold; }
                .risk-HIGH { color: #d32f2f; font-weight: bold; }
                .risk-ATTENTION { color: #f57c00; font-weight: bold; }
                .flag-NORMAL { color: #2e7d32; }
                .flag-HIGH { color: #d32f2f; font-weight: bold; }
                .flag-LOW { color: #1565c0; font-weight: bold; }
                .opinion { background: #f5f7fa; padding: 12px; border-radius: 4px; white-space: pre-wrap; }
                .disclaimer { background: #fff3e0; border: 1px solid #ffcc02; border-radius: 4px; padding: 15px; margin-top: 30px; }
                .disclaimer h3 { color: #e65100; margin-bottom: 8px; }
                .disclaimer p { color: #bf360c; font-size: 13px; }
                .footer { margin-top: 30px; padding-top: 15px; border-top: 1px solid #e0e0e0; text-align: center; color: #999; font-size: 12px; }
                @media print { body { padding: 20px; } .data-table th { background: #333 !important; -webkit-print-color-adjust: exact; } }
                """;
    }

    private String textOrDash(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return "-";
        }
        String text = node.get(field).asText();
        return text.isBlank() ? "-" : text;
    }

    private String formatValue(BigDecimal value) {
        if (value == null) return "-";
        return value.stripTrailingZeros().toPlainString();
    }

    private String formatRange(BigDecimal low, BigDecimal high) {
        if (low == null && high == null) return "-";
        String lowStr = low != null ? low.stripTrailingZeros().toPlainString() : "";
        String highStr = high != null ? high.stripTrailingZeros().toPlainString() : "";
        if (low != null && high != null) return lowStr + " ~ " + highStr;
        if (low != null) return ">= " + lowStr;
        return "<= " + highStr;
    }

    private String riskClass(String riskLevel) {
        if (riskLevel == null) return "LOW";
        return switch (riskLevel.toUpperCase()) {
            case "HIGH" -> "HIGH";
            case "MODERATE", "ATTENTION" -> "MODERATE";
            default -> "LOW";
        };
    }

    private String riskText(String riskLevel) {
        if (riskLevel == null) return "低";
        return switch (riskLevel.toUpperCase()) {
            case "HIGH" -> "高风险";
            case "MODERATE" -> "中风险";
            case "ATTENTION" -> "需关注";
            case "LOW" -> "低风险";
            default -> riskLevel;
        };
    }

    private String flagClass(String flag) {
        if (flag == null) return "NORMAL";
        return switch (flag.toUpperCase()) {
            case "HIGH" -> "HIGH";
            case "LOW" -> "LOW";
            default -> "NORMAL";
        };
    }

    private String flagText(String flag) {
        if (flag == null) return "正常";
        return switch (flag.toUpperCase()) {
            case "HIGH" -> "偏高";
            case "LOW" -> "偏低";
            case "NORMAL" -> "正常";
            default -> flag;
        };
    }

    private String genderText(String gender) {
        if (gender == null) return "-";
        return switch (gender.toUpperCase()) {
            case "MALE" -> "男";
            case "FEMALE" -> "女";
            default -> gender;
        };
    }

    private String truncate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
