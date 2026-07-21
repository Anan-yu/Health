package com.rayk.health.review.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rayk.health.assessment.entity.HealthAssessmentEntity;
import com.rayk.health.assessment.mapper.HealthAssessmentMapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.patient.application.DataScopeService;
import com.rayk.health.review.dto.ReviewEditRequest;
import com.rayk.health.review.entity.AssessmentReviewEntity;
import com.rayk.health.review.mapper.AssessmentReviewMapper;
import com.rayk.health.review.vo.ReviewTaskVo;
import com.rayk.health.assessment.vo.AssessmentVo;
import com.rayk.health.patient.converter.PatientConverter;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.system.aspect.Audited;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewEditService {
    private static final DateTimeFormatter AUDIT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AssessmentReviewMapper reviewMapper;
    private final HealthAssessmentMapper assessmentMapper;
    private final DataScopeService dataScopeService;
    private final PatientConverter patientConverter;
    private final ObjectMapper objectMapper;

    public ReviewEditService(
            AssessmentReviewMapper reviewMapper,
            HealthAssessmentMapper assessmentMapper,
            DataScopeService dataScopeService,
            PatientConverter patientConverter,
            ObjectMapper objectMapper) {
        this.reviewMapper = reviewMapper;
        this.assessmentMapper = assessmentMapper;
        this.dataScopeService = dataScopeService;
        this.patientConverter = patientConverter;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @PreAuthorize("hasAuthority('assessment:review')")
    @Audited(operationType = "EDIT_REVIEW", resourceType = "ASSESSMENT_REVIEW")
    public ReviewTaskVo editReview(long reviewId, ReviewEditRequest request) {
        AssessmentReviewEntity review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ErrorCode.REVIEW_INVALID_STATUS);
        }
        dataScopeService.requirePatient(review.getPatientId());
        if (!"WAITING_REVIEW".equals(review.getStatus())) {
            throw new BusinessException(ErrorCode.REVIEW_INVALID_STATUS);
        }

        CurrentPrincipal current = CurrentUser.require();
        HealthAssessmentEntity assessment = assessmentMapper.selectById(review.getAssessmentId());
        if (assessment == null) {
            throw new BusinessException(ErrorCode.LAB_REPORT_NOT_FOUND);
        }

        String modifiedSnapshot = applyEdits(assessment.getResultSnapshot(), request.items());
        assessment.setResultSnapshot(modifiedSnapshot);
        assessment.setOverallRiskLevel(overallRiskLevel(modifiedSnapshot));
        assessment.setUpdatedBy(current.userId());
        assessment.setUpdatedAt(LocalDateTime.now());
        assessmentMapper.updateById(assessment);

        String auditEntry = buildAuditEntry(current, request);
        String existingOpinion = review.getReviewOpinion() == null ? "" : review.getReviewOpinion();
        String newOpinion = existingOpinion.isEmpty() ? auditEntry : existingOpinion + "\n" + auditEntry;
        if (request.overallOpinion() != null && !request.overallOpinion().isBlank()) {
            newOpinion = newOpinion + "\n[综合意见] " + request.overallOpinion();
        }
        review.setReviewOpinion(newOpinion);
        review.setReviewerId(current.userId());
        review.setUpdatedBy(current.userId());
        review.setUpdatedAt(LocalDateTime.now());
        reviewMapper.updateById(review);

        return toReviewVo(review, assessment);
    }

    private String applyEdits(String resultSnapshot, List<ReviewEditRequest.ReviewItemEdit> items) {
        try {
            JsonNode root = objectMapper.readTree(resultSnapshot);
            if (root == null || !root.isObject()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
            ObjectNode rootObj = (ObjectNode) root;
            JsonNode resultsNode = rootObj.get("results");
            if (resultsNode == null || !resultsNode.isArray()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
            ArrayNode results = (ArrayNode) resultsNode;
            Set<String> editedCodes = new HashSet<>();
            for (ReviewEditRequest.ReviewItemEdit edit : items) {
                if (!editedCodes.add(edit.modelCode())) {
                    throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
                }
                boolean found = false;
                for (int i = 0; i < results.size(); i++) {
                    JsonNode item = results.get(i);
                    if (item.isObject()) {
                        ObjectNode obj = (ObjectNode) item;
                        String modelCode = obj.has("modelCode") ? obj.get("modelCode").asText() : null;
                        if (edit.modelCode().equals(modelCode)) {
                            obj.put("riskLevel", edit.riskLevel());
                            obj.set("evidence", objectMapper.valueToTree(edit.evidence()));
                            obj.set(
                                    "recommendations",
                                    objectMapper.valueToTree(edit.recommendations()));
                            obj.put("doctorEdited", true);
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
                }
            }
            return objectMapper.writeValueAsString(rootObj);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private String overallRiskLevel(String resultSnapshot) {
        try {
            JsonNode results = objectMapper.readTree(resultSnapshot).path("results");
            boolean hasAttention = false;
            boolean hasLow = false;
            for (JsonNode item : results) {
                String riskLevel = item.path("riskLevel").asText();
                if ("HIGH".equals(riskLevel)) {
                    return "HIGH";
                }
                if ("ATTENTION".equals(riskLevel)) {
                    hasAttention = true;
                }
                if ("LOW".equals(riskLevel)) {
                    hasLow = true;
                }
            }
            if (hasAttention) {
                return "ATTENTION";
            }
            return hasLow ? "LOW" : "INSUFFICIENT_DATA";
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private String buildAuditEntry(CurrentPrincipal current, ReviewEditRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("[编辑记录] ")
          .append(LocalDateTime.now().format(AUDIT_FMT))
          .append(" 医生(ID:").append(current.userId()).append(") 修改了 ")
          .append(request.items().size()).append(" 项评估结果: ");
        List<String> codes = new ArrayList<>();
        for (ReviewEditRequest.ReviewItemEdit item : request.items()) {
            codes.add(item.modelCode() + "->" + item.riskLevel());
        }
        sb.append(String.join(", ", codes));
        return sb.toString();
    }

    private ReviewTaskVo toReviewVo(AssessmentReviewEntity review, HealthAssessmentEntity assessment) {
        JsonNode results;
        try {
            results = objectMapper.readTree(assessment.getResultSnapshot());
        } catch (JsonProcessingException e) {
            results = objectMapper.createObjectNode();
        }
        AssessmentVo assessmentVo = new AssessmentVo(
                String.valueOf(assessment.getId()),
                String.valueOf(assessment.getReportId()),
                String.valueOf(assessment.getPatientId()),
                assessment.getModelVersion(),
                assessment.getStatus(),
                assessment.getOverallRiskLevel(),
                results,
                assessment.getDisclaimer(),
                assessment.getCreatedAt());
        return new ReviewTaskVo(
                String.valueOf(review.getId()),
                review.getStatus(),
                review.getReviewOpinion(),
                review.getReviewedAt(),
                patientConverter.toVo(dataScopeService.requirePatient(review.getPatientId())),
                assessmentVo);
    }
}
