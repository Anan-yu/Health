package com.rayk.health.review.vo;

import com.rayk.health.assessment.vo.AssessmentVo;
import com.rayk.health.patient.vo.PatientVo;
import java.time.LocalDateTime;

public record ReviewTaskVo(
        String id,
        String status,
        String reviewOpinion,
        LocalDateTime reviewedAt,
        PatientVo patient,
        AssessmentVo assessment) {}

