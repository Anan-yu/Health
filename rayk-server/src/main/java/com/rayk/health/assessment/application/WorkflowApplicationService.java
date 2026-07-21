package com.rayk.health.assessment.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.assessment.entity.AiTaskEntity;
import com.rayk.health.assessment.entity.HealthAssessmentEntity;
import com.rayk.health.assessment.mapper.AiTaskMapper;
import com.rayk.health.assessment.mapper.HealthAssessmentMapper;
import com.rayk.health.assessment.vo.AssessmentVo;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.followup.dto.CreateFollowupRequest;
import com.rayk.health.followup.entity.FollowupTaskEntity;
import com.rayk.health.followup.mapper.FollowupTaskMapper;
import com.rayk.health.followup.vo.FollowupTaskVo;
import com.rayk.health.indicator.entity.IndicatorValueEntity;
import com.rayk.health.indicator.application.AssessmentModelService;
import com.rayk.health.indicator.mapper.IndicatorValueMapper;
import com.rayk.health.integration.ai.AiDtos;
import com.rayk.health.integration.ai.AiServiceClient;
import com.rayk.health.laboratory.dto.ConfirmIndicatorsRequest;
import com.rayk.health.laboratory.dto.CreateLabReportRequest;
import com.rayk.health.laboratory.dto.IndicatorInput;
import com.rayk.health.laboratory.entity.LabReportEntity;
import com.rayk.health.laboratory.mapper.LabReportMapper;
import com.rayk.health.laboratory.vo.IndicatorVo;
import com.rayk.health.laboratory.vo.LabReportVo;
import com.rayk.health.patient.application.DataScopeService;
import com.rayk.health.patient.converter.PatientConverter;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.patient.mapper.PatientMapper;
import com.rayk.health.report.application.PdfReportService;
import com.rayk.health.report.entity.HealthReportEntity;
import com.rayk.health.report.mapper.HealthReportMapper;
import com.rayk.health.report.vo.HealthReportVo;
import com.rayk.health.review.entity.AssessmentReviewEntity;
import com.rayk.health.review.mapper.AssessmentReviewMapper;
import com.rayk.health.review.vo.ReviewTaskVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.system.aspect.Audited;
import com.rayk.health.system.application.PrivacyConsentService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowApplicationService {
    public static final String DISCLAIMER =
            "该结果仅用于系统开发测试和健康管理参考，不构成医学诊断。";

    private final LabReportMapper labReportMapper;
    private final IndicatorValueMapper indicatorMapper;
    private final AiTaskMapper aiTaskMapper;
    private final HealthAssessmentMapper assessmentMapper;
    private final AssessmentReviewMapper reviewMapper;
    private final HealthReportMapper healthReportMapper;
    private final FollowupTaskMapper followupMapper;
    private final PatientMapper patientMapper;
    private final DataScopeService dataScopeService;
    private final PatientConverter patientConverter;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;
    private final PdfReportService pdfReportService;
    private final AssessmentModelService assessmentModelService;
    private final PrivacyConsentService privacyConsentService;

    public WorkflowApplicationService(
            LabReportMapper labReportMapper,
            IndicatorValueMapper indicatorMapper,
            AiTaskMapper aiTaskMapper,
            HealthAssessmentMapper assessmentMapper,
            AssessmentReviewMapper reviewMapper,
            HealthReportMapper healthReportMapper,
            FollowupTaskMapper followupMapper,
            PatientMapper patientMapper,
            DataScopeService dataScopeService,
            PatientConverter patientConverter,
            AiServiceClient aiServiceClient,
            ObjectMapper objectMapper,
            PdfReportService pdfReportService,
            AssessmentModelService assessmentModelService,
            PrivacyConsentService privacyConsentService) {
        this.labReportMapper = labReportMapper;
        this.indicatorMapper = indicatorMapper;
        this.aiTaskMapper = aiTaskMapper;
        this.assessmentMapper = assessmentMapper;
        this.reviewMapper = reviewMapper;
        this.healthReportMapper = healthReportMapper;
        this.followupMapper = followupMapper;
        this.patientMapper = patientMapper;
        this.dataScopeService = dataScopeService;
        this.patientConverter = patientConverter;
        this.aiServiceClient = aiServiceClient;
        this.objectMapper = objectMapper;
        this.pdfReportService = pdfReportService;
        this.assessmentModelService = assessmentModelService;
        this.privacyConsentService = privacyConsentService;
    }

    public List<Long> accessiblePatientIds() {
        return patientMapper.selectList(dataScopeService.scopedPatients()).stream()
                .map(PatientEntity::getId)
                .toList();
    }

    @PreAuthorize("hasAuthority('lab-report:manage') or (hasAuthority('self:lab-report') and principal.workbench == 'CUSTOMER')")
    @Audited(operationType = "CREATE_LAB_REPORT", resourceType = "LAB_REPORT")
    public LabReportVo createLabReport(CreateLabReportRequest request) {
        dataScopeService.requirePatient(request.patientId());
        privacyConsentService.requireConsent(
                request.patientId(), PrivacyConsentService.TYPE_DATA_COLLECTION);
        CurrentPrincipal current = CurrentUser.require();
        LabReportEntity report = new LabReportEntity();
        report.setTenantId(current.tenantId());
        report.setPatientId(request.patientId());
        report.setReportName(request.reportName());
        report.setReportDate(request.reportDate() == null ? LocalDate.now() : request.reportDate());
        report.setSourceType(request.sourceType() == null ? "SIMULATED_UPLOAD" : request.sourceType());
        report.setStatus("UPLOADED");
        auditNew(report, current.userId());
        labReportMapper.insert(report);
        return toLabReportVo(report);
    }

    public List<LabReportVo> listLabReports() {
        List<Long> patientIds = accessiblePatientIds();
        if (patientIds.isEmpty()) {
            return List.of();
        }
        return labReportMapper
                .selectList(
                        new LambdaQueryWrapper<LabReportEntity>()
                                .in(LabReportEntity::getPatientId, patientIds)
                                .orderByDesc(LabReportEntity::getCreatedAt))
                .stream()
                .map(this::toLabReportVo)
                .toList();
    }

    public LabReportVo getLabReport(long id) {
        return toLabReportVo(requireReport(id));
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('lab-report:manage', 'indicator:confirm') or (hasAuthority('self:lab-report') and principal.workbench == 'CUSTOMER')")
    @Audited(operationType = "REPLACE_INDICATORS", resourceType = "LAB_REPORT")
    public LabReportVo replaceIndicators(long reportId, ConfirmIndicatorsRequest request) {
        LabReportEntity report = requireReport(reportId);
        if (Set.of("AI_PROCESSING", "REVIEWING", "PUBLISHED").contains(report.getStatus())) {
            throw new BusinessException(ErrorCode.LAB_REPORT_INVALID_STATUS);
        }
        indicatorMapper.update(
                null,
                new LambdaUpdateWrapper<IndicatorValueEntity>()
                        .eq(IndicatorValueEntity::getReportId, reportId)
                        .set(IndicatorValueEntity::getDeleted, 1));
        CurrentPrincipal current = CurrentUser.require();
        request.indicators().forEach(item -> indicatorMapper.insert(toIndicator(report, item, current)));
        report.setStatus("WAITING_CONFIRMATION");
        touch(report, current.userId());
        labReportMapper.updateById(report);
        return toLabReportVo(report);
    }

    @PreAuthorize("hasAnyAuthority('lab-report:manage', 'indicator:confirm') or (hasAuthority('self:lab-report') and principal.workbench == 'CUSTOMER')")
    @Audited(operationType = "CONFIRM_INDICATORS", resourceType = "LAB_REPORT")
    public LabReportVo confirmIndicators(long reportId) {
        LabReportEntity report = requireReport(reportId);
        if (!"WAITING_CONFIRMATION".equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.LAB_REPORT_INVALID_STATUS);
        }
        List<IndicatorValueEntity> indicators = indicators(reportId);
        if (indicators.isEmpty()) {
            throw new BusinessException(ErrorCode.LAB_REPORT_INVALID_STATUS);
        }
        indicatorMapper.update(
                null,
                new LambdaUpdateWrapper<IndicatorValueEntity>()
                        .eq(IndicatorValueEntity::getReportId, reportId)
                        .set(IndicatorValueEntity::getManuallyConfirmed, 1));
        report.setStatus("CONFIRMED");
        touch(report, CurrentUser.require().userId());
        labReportMapper.updateById(report);
        return toLabReportVo(report);
    }

    @PreAuthorize("hasAnyAuthority('assessment:create', 'lab-report:manage') or (hasAuthority('self:assessment') and principal.workbench == 'CUSTOMER')")
    @Audited(operationType = "SUBMIT_ASSESSMENT", resourceType = "LAB_REPORT")
    public AssessmentVo submitAi(long reportId) {
        LabReportEntity report = requireReport(reportId);
        if (!"CONFIRMED".equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.LAB_REPORT_INVALID_STATUS);
        }
        privacyConsentService.requireConsent(
                report.getPatientId(), PrivacyConsentService.TYPE_HEALTH_ASSESSMENT);
        CurrentPrincipal current = CurrentUser.require();
        report.setStatus("AI_PROCESSING");
        touch(report, current.userId());
        labReportMapper.updateById(report);

        AiTaskEntity task = new AiTaskEntity();
        task.setTenantId(current.tenantId());
        task.setReportId(reportId);
        task.setPatientId(report.getPatientId());
        task.setTaskCode("TASK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        task.setTaskType("HEALTH_ASSESSMENT");
        task.setStatus("PROCESSING");
        task.setStartedAt(LocalDateTime.now());
        auditNew(task, current.userId());
        aiTaskMapper.insert(task);

        try {
            List<String> activeModelCodes = assessmentModelService.activeModelCodes();
            if (activeModelCodes.isEmpty()) {
                throw new BusinessException(ErrorCode.MODEL_CONFIG_NOT_FOUND);
            }
            PatientEntity patient = dataScopeService.requirePatient(report.getPatientId());
            Integer age =
                    patient.getBirthDate() == null
                            ? null
                            : Period.between(patient.getBirthDate(), LocalDate.now()).getYears();
            String gender =
                    Set.of("MALE", "FEMALE").contains(patient.getGender())
                            ? patient.getGender()
                            : "UNKNOWN";
            AiDtos.EvaluateRequest aiRequest =
                    new AiDtos.EvaluateRequest(
                            task.getTaskCode(),
                            String.valueOf(report.getPatientId()),
                            indicators(reportId).stream()
                                    .map(
                                            item ->
                                                    new AiDtos.Indicator(
                                                            item.getIndicatorCode(),
                                                            item.getIndicatorName(),
                                                            item.getValue(),
                                                            item.getUnit(),
                                                            item.getReferenceLow(),
                                                            item.getReferenceHigh()))
                                    .toList(),
                            activeModelCodes,
                            new AiDtos.PatientContext(gender, age));
            AiDtos.AssessmentData aiResult = aiServiceClient.evaluate(aiRequest);
            task.setStatus("SUCCESS");
            task.setFinishedAt(LocalDateTime.now());
            touch(task, current.userId());
            aiTaskMapper.updateById(task);

            HealthAssessmentEntity assessment = new HealthAssessmentEntity();
            assessment.setTenantId(current.tenantId());
            assessment.setAiTaskId(task.getId());
            assessment.setReportId(reportId);
            assessment.setPatientId(report.getPatientId());
            assessment.setModelVersion(aiResult.modelVersion());
            assessment.setStatus("SUCCESS");
            assessment.setOverallRiskLevel(overallRiskLevel(aiResult.results()));
            assessment.setResultSnapshot(objectMapper.writeValueAsString(aiResult));
            assessment.setDisclaimer(aiResult.disclaimer());
            auditNew(assessment, current.userId());
            assessmentMapper.insert(assessment);

            AssessmentReviewEntity review = new AssessmentReviewEntity();
            review.setTenantId(current.tenantId());
            review.setAssessmentId(assessment.getId());
            review.setPatientId(report.getPatientId());
            review.setStatus("WAITING_REVIEW");
            auditNew(review, current.userId());
            reviewMapper.insert(review);

            report.setStatus("REVIEWING");
            touch(report, current.userId());
            labReportMapper.updateById(report);
            return toAssessmentVo(assessment);
        } catch (JsonProcessingException | BusinessException exception) {
            task.setStatus("FAILED");
            task.setErrorMessage("AI服务调用失败或响应无法解析");
            task.setFinishedAt(LocalDateTime.now());
            touch(task, current.userId());
            aiTaskMapper.updateById(task);
            report.setStatus("FAILED");
            report.setFailureReason("AI服务暂时不可用");
            touch(report, current.userId());
            labReportMapper.updateById(report);
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    public List<AssessmentVo> listAssessments() {
        List<Long> patientIds = accessiblePatientIds();
        if (patientIds.isEmpty()) {
            return List.of();
        }
        return assessmentMapper
                .selectList(
                        new LambdaQueryWrapper<HealthAssessmentEntity>()
                                .in(HealthAssessmentEntity::getPatientId, patientIds)
                                .orderByDesc(HealthAssessmentEntity::getCreatedAt))
                .stream()
                .map(this::toAssessmentVo)
                .toList();
    }

    public AssessmentVo getAssessment(long id) {
        HealthAssessmentEntity entity = assessmentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.LAB_REPORT_NOT_FOUND);
        }
        dataScopeService.requirePatient(entity.getPatientId());
        return toAssessmentVo(entity);
    }

    @PreAuthorize("hasAuthority('assessment:review')")
    public List<ReviewTaskVo> listReviews() {
        return reviewMapper
                .selectList(
                        new LambdaQueryWrapper<AssessmentReviewEntity>()
                                .orderByDesc(AssessmentReviewEntity::getCreatedAt))
                .stream()
                .filter(item -> canAccess(item.getPatientId()))
                .map(this::toReviewVo)
                .toList();
    }

    @PreAuthorize("hasAuthority('assessment:review')")
    public ReviewTaskVo getReview(long id) {
        AssessmentReviewEntity review = requireReview(id);
        dataScopeService.requirePatient(review.getPatientId());
        return toReviewVo(review);
    }

    @PreAuthorize("hasAuthority('assessment:review')")
    @Transactional
    @Audited(operationType = "APPROVE_REVIEW", resourceType = "ASSESSMENT_REVIEW")
    public ReviewTaskVo approve(long id, String opinion) {
        return decide(id, opinion, "APPROVED");
    }

    @PreAuthorize("hasAuthority('assessment:review')")
    @Transactional
    @Audited(operationType = "REJECT_REVIEW", resourceType = "ASSESSMENT_REVIEW")
    public ReviewTaskVo reject(long id, String opinion) {
        return decide(id, opinion, "REJECTED");
    }

    @PreAuthorize("hasAuthority('report:publish')")
    @Transactional
    @Audited(operationType = "PUBLISH_HEALTH_REPORT", resourceType = "HEALTH_REPORT")
    public HealthReportVo publish(long reviewId) {
        AssessmentReviewEntity review = requireReview(reviewId);
        dataScopeService.requirePatient(review.getPatientId());
        if (!"APPROVED".equals(review.getStatus())) {
            throw new BusinessException(ErrorCode.REVIEW_INVALID_STATUS);
        }
        CurrentPrincipal current = CurrentUser.require();
        HealthAssessmentEntity assessment = assessmentMapper.selectById(review.getAssessmentId());
        PatientEntity patient = dataScopeService.requirePatient(review.getPatientId());
        HealthReportEntity report = new HealthReportEntity();
        report.setTenantId(current.tenantId());
        report.setPatientId(review.getPatientId());
        report.setAssessmentId(assessment.getId());
        report.setReportNo("HR" + System.currentTimeMillis());
        report.setTitle(patient.getName() + "的健康管理评估报告（演示）");
        report.setStatus("PUBLISHED");
        report.setSummary("演示健康评估已由医生人工审核，详情请查看结构化评估结果。仅供健康管理参考。");
        report.setDoctorOpinion(review.getReviewOpinion());
        report.setDisclaimer(DISCLAIMER);
        report.setPublishedAt(LocalDateTime.now());
        report.setPublishedBy(current.userId());
        auditNew(report, current.userId());
        healthReportMapper.insert(report);

        // A published report must have a downloadable artifact; storage failures roll back publication.
        pdfReportService.generateAndStore(report, assessment, patient, current.userId());

        review.setStatus("PUBLISHED");
        touch(review, current.userId());
        reviewMapper.updateById(review);
        LabReportEntity labReport = labReportMapper.selectById(assessment.getReportId());
        labReport.setStatus("PUBLISHED");
        touch(labReport, current.userId());
        labReportMapper.updateById(labReport);
        return toHealthReportVo(report);
    }

    public List<HealthReportVo> listHealthReports() {
        List<Long> patientIds = accessiblePatientIds();
        if (patientIds.isEmpty()) {
            return List.of();
        }
        return healthReportMapper
                .selectList(
                        new LambdaQueryWrapper<HealthReportEntity>()
                                .in(HealthReportEntity::getPatientId, patientIds)
                                .eq(HealthReportEntity::getStatus, "PUBLISHED")
                                .orderByDesc(HealthReportEntity::getPublishedAt))
                .stream()
                .map(this::toHealthReportVo)
                .toList();
    }

    public HealthReportVo getHealthReport(long id) {
        HealthReportEntity report = healthReportMapper.selectById(id);
        if (report == null || !"PUBLISHED".equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.LAB_REPORT_NOT_FOUND);
        }
        dataScopeService.requirePatient(report.getPatientId());
        return toHealthReportVo(report);
    }

    @PreAuthorize("hasAnyAuthority('followup:manage', 'followup:create')")
    @Audited(operationType = "CREATE_FOLLOWUP", resourceType = "FOLLOWUP_TASK")
    public FollowupTaskVo createFollowup(CreateFollowupRequest request) {
        dataScopeService.requirePatient(request.patientId());
        CurrentPrincipal current = CurrentUser.require();
        FollowupTaskEntity task = new FollowupTaskEntity();
        task.setTenantId(current.tenantId());
        task.setPatientId(request.patientId());
        task.setAssigneeId(request.assigneeId());
        task.setTitle(request.title());
        task.setContent(request.content());
        task.setDueDate(request.dueDate());
        task.setStatus("PENDING");
        auditNew(task, current.userId());
        followupMapper.insert(task);
        return toFollowupVo(task);
    }

    public List<FollowupTaskVo> listFollowups() {
        List<Long> patientIds = accessiblePatientIds();
        if (patientIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<FollowupTaskEntity> query =
                new LambdaQueryWrapper<FollowupTaskEntity>()
                        .in(FollowupTaskEntity::getPatientId, patientIds)
                        .orderByAsc(FollowupTaskEntity::getDueDate);
        if ("CUSTOMER".equals(CurrentUser.require().workbench())) {
            query.ne(FollowupTaskEntity::getStatus, "DRAFT");
        }
        return followupMapper
                .selectList(query)
                .stream()
                .map(this::toFollowupVo)
                .toList();
    }

    public FollowupTaskVo getFollowup(long id) {
        return toFollowupVo(requireFollowup(id));
    }

    @PreAuthorize("hasAnyAuthority('followup:manage', 'followup:create')")
    public FollowupTaskVo completeFollowup(long id) {
        FollowupTaskEntity task = requireFollowup(id);
        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.FOLLOWUP_INVALID_STATUS);
        }
        task.setStatus("COMPLETED");
        task.setCompletedAt(LocalDateTime.now());
        touch(task, CurrentUser.require().userId());
        followupMapper.updateById(task);
        return toFollowupVo(task);
    }

    @PreAuthorize("hasAuthority('self:followup') and principal.workbench == 'CUSTOMER'")
    public FollowupTaskVo feedback(long id, String feedback) {
        FollowupTaskEntity task = requireFollowup(id);
        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.FOLLOWUP_INVALID_STATUS);
        }
        task.setFeedback(feedback);
        task.setStatus("COMPLETED");
        task.setCompletedAt(LocalDateTime.now());
        touch(task, CurrentUser.require().userId());
        followupMapper.updateById(task);
        return toFollowupVo(task);
    }

    private ReviewTaskVo decide(long id, String opinion, String status) {
        AssessmentReviewEntity review = requireReview(id);
        dataScopeService.requirePatient(review.getPatientId());
        if (!"WAITING_REVIEW".equals(review.getStatus())) {
            throw new BusinessException(ErrorCode.REVIEW_INVALID_STATUS);
        }
        CurrentPrincipal current = CurrentUser.require();
        review.setStatus(status);
        review.setReviewOpinion(opinion);
        review.setReviewerId(current.userId());
        review.setReviewedAt(LocalDateTime.now());
        touch(review, current.userId());
        reviewMapper.updateById(review);
        if ("REJECTED".equals(status)) {
            HealthAssessmentEntity assessment = assessmentMapper.selectById(review.getAssessmentId());
            if (assessment == null) {
                throw new BusinessException(ErrorCode.LAB_REPORT_NOT_FOUND);
            }
            LabReportEntity labReport = labReportMapper.selectById(assessment.getReportId());
            if (labReport == null) {
                throw new BusinessException(ErrorCode.LAB_REPORT_NOT_FOUND);
            }
            labReport.setStatus("WAITING_CONFIRMATION");
            labReport.setFailureReason(opinion);
            touch(labReport, current.userId());
            labReportMapper.updateById(labReport);
        }
        return toReviewVo(review);
    }

    private LabReportEntity requireReport(long id) {
        LabReportEntity report = labReportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException(ErrorCode.LAB_REPORT_NOT_FOUND);
        }
        dataScopeService.requirePatient(report.getPatientId());
        return report;
    }

    private AssessmentReviewEntity requireReview(long id) {
        AssessmentReviewEntity review = reviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException(ErrorCode.REVIEW_INVALID_STATUS);
        }
        return review;
    }

    private FollowupTaskEntity requireFollowup(long id) {
        FollowupTaskEntity task = followupMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.FOLLOWUP_NOT_FOUND);
        }
        dataScopeService.requirePatient(task.getPatientId());
        return task;
    }

    private boolean canAccess(long patientId) {
        try {
            dataScopeService.requirePatient(patientId);
            return true;
        } catch (BusinessException ignored) {
            return false;
        }
    }

    private String overallRiskLevel(List<AiDtos.ModelResult> results) {
        if (results.stream().anyMatch(item -> "HIGH".equals(item.riskLevel()))) {
            return "HIGH";
        }
        if (results.stream().anyMatch(item -> "ATTENTION".equals(item.riskLevel()))) {
            return "ATTENTION";
        }
        if (results.stream().anyMatch(item -> "LOW".equals(item.riskLevel()))) {
            return "LOW";
        }
        return "INSUFFICIENT_DATA";
    }

    private List<IndicatorValueEntity> indicators(long reportId) {
        return indicatorMapper.selectList(
                new LambdaQueryWrapper<IndicatorValueEntity>()
                        .eq(IndicatorValueEntity::getReportId, reportId)
                        .eq(IndicatorValueEntity::getDeleted, 0));
    }

    private IndicatorValueEntity toIndicator(
            LabReportEntity report, IndicatorInput input, CurrentPrincipal current) {
        IndicatorValueEntity entity = new IndicatorValueEntity();
        entity.setTenantId(current.tenantId());
        entity.setReportId(report.getId());
        entity.setPatientId(report.getPatientId());
        entity.setIndicatorCode(input.code());
        entity.setIndicatorName(input.name());
        entity.setValue(input.value());
        entity.setUnit(input.unit());
        entity.setReferenceLow(input.referenceLow());
        entity.setReferenceHigh(input.referenceHigh());
        entity.setAbnormalFlag(abnormal(input));
        entity.setManuallyConfirmed(0);
        auditNew(entity, current.userId());
        return entity;
    }

    private String abnormal(IndicatorInput input) {
        BigDecimal value = input.value();
        if (input.referenceLow() != null && value.compareTo(input.referenceLow()) < 0) {
            return "LOW";
        }
        if (input.referenceHigh() != null && value.compareTo(input.referenceHigh()) > 0) {
            return "HIGH";
        }
        return "NORMAL";
    }

    private LabReportVo toLabReportVo(LabReportEntity report) {
        List<IndicatorVo> values =
                indicators(report.getId()).stream()
                        .map(
                                item ->
                                        new IndicatorVo(
                                                String.valueOf(item.getId()),
                                                item.getIndicatorCode(),
                                                item.getIndicatorName(),
                                                item.getValue(),
                                                item.getUnit(),
                                                item.getReferenceLow(),
                                                item.getReferenceHigh(),
                                                item.getAbnormalFlag(),
                                                item.getManuallyConfirmed() == 1))
                        .toList();
        return new LabReportVo(
                String.valueOf(report.getId()),
                String.valueOf(report.getPatientId()),
                report.getReportName(),
                report.getReportDate(),
                report.getStatus(),
                report.getSourceType(),
                values,
                report.getCreatedAt());
    }

    private AssessmentVo toAssessmentVo(HealthAssessmentEntity entity) {
        JsonNode results;
        try {
            results = objectMapper.readTree(entity.getResultSnapshot());
        } catch (JsonProcessingException exception) {
            results = objectMapper.createObjectNode();
        }
        return new AssessmentVo(
                String.valueOf(entity.getId()),
                String.valueOf(entity.getReportId()),
                String.valueOf(entity.getPatientId()),
                entity.getModelVersion(),
                entity.getStatus(),
                entity.getOverallRiskLevel(),
                results,
                entity.getDisclaimer(),
                entity.getCreatedAt());
    }

    private ReviewTaskVo toReviewVo(AssessmentReviewEntity review) {
        return new ReviewTaskVo(
                String.valueOf(review.getId()),
                review.getStatus(),
                review.getReviewOpinion(),
                review.getReviewedAt(),
                patientConverter.toVo(dataScopeService.requirePatient(review.getPatientId())),
                toAssessmentVo(assessmentMapper.selectById(review.getAssessmentId())));
    }

    private HealthReportVo toHealthReportVo(HealthReportEntity report) {
        return new HealthReportVo(
                String.valueOf(report.getId()),
                String.valueOf(report.getPatientId()),
                String.valueOf(report.getAssessmentId()),
                report.getReportNo(),
                report.getTitle(),
                report.getStatus(),
                report.getSummary(),
                report.getDoctorOpinion(),
                report.getDisclaimer(),
                report.getPublishedAt());
    }

    private FollowupTaskVo toFollowupVo(FollowupTaskEntity task) {
        return new FollowupTaskVo(
                String.valueOf(task.getId()),
                String.valueOf(task.getPatientId()),
                task.getTitle(),
                task.getContent(),
                task.getDueDate(),
                task.getStatus(),
                task.getFeedback(),
                task.getCompletedAt());
    }

    private void auditNew(Object entity, long userId) {
        LocalDateTime now = LocalDateTime.now();
        if (entity instanceof LabReportEntity value) {
            value.setCreatedBy(userId);
            value.setUpdatedBy(userId);
            value.setCreatedAt(now);
            value.setUpdatedAt(now);
            value.setDeleted(0);
            value.setVersion(0);
        } else if (entity instanceof IndicatorValueEntity value) {
            value.setCreatedBy(userId);
            value.setUpdatedBy(userId);
            value.setCreatedAt(now);
            value.setUpdatedAt(now);
            value.setDeleted(0);
            value.setVersion(0);
        } else if (entity instanceof AiTaskEntity value) {
            value.setCreatedBy(userId);
            value.setUpdatedBy(userId);
            value.setCreatedAt(now);
            value.setUpdatedAt(now);
            value.setDeleted(0);
            value.setVersion(0);
        } else if (entity instanceof HealthAssessmentEntity value) {
            value.setCreatedBy(userId);
            value.setUpdatedBy(userId);
            value.setCreatedAt(now);
            value.setUpdatedAt(now);
            value.setDeleted(0);
            value.setVersion(0);
        } else if (entity instanceof AssessmentReviewEntity value) {
            value.setCreatedBy(userId);
            value.setUpdatedBy(userId);
            value.setCreatedAt(now);
            value.setUpdatedAt(now);
            value.setDeleted(0);
            value.setVersion(0);
        } else if (entity instanceof HealthReportEntity value) {
            value.setCreatedBy(userId);
            value.setUpdatedBy(userId);
            value.setCreatedAt(now);
            value.setUpdatedAt(now);
            value.setDeleted(0);
            value.setVersion(0);
        } else if (entity instanceof FollowupTaskEntity value) {
            value.setCreatedBy(userId);
            value.setUpdatedBy(userId);
            value.setCreatedAt(now);
            value.setUpdatedAt(now);
            value.setDeleted(0);
            value.setVersion(0);
        }
    }

    private void touch(Object entity, long userId) {
        LocalDateTime now = LocalDateTime.now();
        if (entity instanceof LabReportEntity value) {
            value.setUpdatedBy(userId);
            value.setUpdatedAt(now);
        } else if (entity instanceof AiTaskEntity value) {
            value.setUpdatedBy(userId);
            value.setUpdatedAt(now);
        } else if (entity instanceof AssessmentReviewEntity value) {
            value.setUpdatedBy(userId);
            value.setUpdatedAt(now);
        } else if (entity instanceof FollowupTaskEntity value) {
            value.setUpdatedBy(userId);
            value.setUpdatedAt(now);
        }
    }
}
