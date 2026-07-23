package com.rayk.health.report.vo;

import java.time.LocalDateTime;
import com.rayk.health.assessment.vo.AssessmentVo;

public record HealthReportVo(
        String id,
        String patientId,
        String assessmentId,
        String reportNo,
        String title,
        String status,
        String summary,
        String doctorOpinion,
        String disclaimer,
        LocalDateTime publishedAt,
        AssessmentVo assessment) {}

