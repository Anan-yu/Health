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
import com.rayk.health.followup.entity.FollowupTaskEntity;
import com.rayk.health.followup.dto.FollowupActionFeedback;
import com.rayk.health.followup.dto.FollowupFeedbackRequest;
import com.rayk.health.followup.mapper.FollowupTaskMapper;
import com.rayk.health.followup.application.NutritionFollowupPlanService;
import com.rayk.health.followup.vo.FollowupTaskVo;
import com.rayk.health.healthscan.application.HealthScanContextService;
import com.rayk.health.indicator.entity.IndicatorValueEntity;
import com.rayk.health.indicator.application.AssessmentModelService;
import com.rayk.health.indicator.mapper.IndicatorValueMapper;
import com.rayk.health.integration.ai.AiDtos;
import com.rayk.health.integration.ai.AiServiceClient;
import com.rayk.health.laboratory.application.LabIndicatorVisibility;
import com.rayk.health.laboratory.dto.ConfirmIndicatorsRequest;
import com.rayk.health.laboratory.dto.CreateLabReportRequest;
import com.rayk.health.laboratory.dto.IndicatorInput;
import com.rayk.health.laboratory.entity.LabReportEntity;
import com.rayk.health.laboratory.mapper.LabReportMapper;
import com.rayk.health.laboratory.vo.IndicatorVo;
import com.rayk.health.laboratory.vo.LabReportVo;
import com.rayk.health.laboratory.vo.OcrFindingVo;
import com.rayk.health.patient.application.DataScopeService;
import com.rayk.health.patient.converter.PatientConverter;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.patient.application.HealthProfileService;
import com.rayk.health.patient.vo.HealthProfileVo;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowApplicationService {
    public static final String DISCLAIMER =
            "该结果仅用于健康管理参考，不构成医学诊断。";

    private final LabReportMapper labReportMapper;
    private final IndicatorValueMapper indicatorMapper;
    private final AiTaskMapper aiTaskMapper;
    private final HealthAssessmentMapper assessmentMapper;
    private final AssessmentReviewMapper reviewMapper;
    private final HealthReportMapper healthReportMapper;
    private final FollowupTaskMapper followupMapper;
    private final PatientMapper patientMapper;
    private final HealthProfileService healthProfileService;
    private final DataScopeService dataScopeService;
    private final PatientConverter patientConverter;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;
    private final PdfReportService pdfReportService;
    private final AssessmentModelService assessmentModelService;
    private final PrivacyConsentService privacyConsentService;
    private final NutritionFollowupPlanService nutritionFollowupPlanService;
    private final HealthScanContextService healthScanContextService;

    public WorkflowApplicationService(
            LabReportMapper labReportMapper,
            IndicatorValueMapper indicatorMapper,
            AiTaskMapper aiTaskMapper,
            HealthAssessmentMapper assessmentMapper,
            AssessmentReviewMapper reviewMapper,
            HealthReportMapper healthReportMapper,
            FollowupTaskMapper followupMapper,
            PatientMapper patientMapper,
            HealthProfileService healthProfileService,
            DataScopeService dataScopeService,
            PatientConverter patientConverter,
            AiServiceClient aiServiceClient,
            ObjectMapper objectMapper,
            PdfReportService pdfReportService,
            AssessmentModelService assessmentModelService,
            PrivacyConsentService privacyConsentService,
            NutritionFollowupPlanService nutritionFollowupPlanService,
            HealthScanContextService healthScanContextService) {
        this.labReportMapper = labReportMapper;
        this.indicatorMapper = indicatorMapper;
        this.aiTaskMapper = aiTaskMapper;
        this.assessmentMapper = assessmentMapper;
        this.reviewMapper = reviewMapper;
        this.healthReportMapper = healthReportMapper;
        this.followupMapper = followupMapper;
        this.patientMapper = patientMapper;
        this.healthProfileService = healthProfileService;
        this.dataScopeService = dataScopeService;
        this.patientConverter = patientConverter;
        this.aiServiceClient = aiServiceClient;
        this.objectMapper = objectMapper;
        this.pdfReportService = pdfReportService;
        this.assessmentModelService = assessmentModelService;
        this.privacyConsentService = privacyConsentService;
        this.nutritionFollowupPlanService = nutritionFollowupPlanService;
        this.healthScanContextService = healthScanContextService;
    }

    public List<Long> accessiblePatientIds() {
        return dataScopeService.readScoped(
                () -> patientMapper.selectList(dataScopeService.scopedPatients()).stream()
                        .map(PatientEntity::getId)
                        .toList());
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
        return dataScopeService.readScoped(
                () -> labReportMapper
                        .selectList(
                                new LambdaQueryWrapper<LabReportEntity>()
                                        .in(LabReportEntity::getPatientId, patientIds)
                                        .orderByDesc(LabReportEntity::getCreatedAt))
                        .stream()
                        .map(this::toLabReportVo)
                        .toList());
    }

    public LabReportVo getLabReport(long id) {
        return toLabReportVo(requireReport(id));
    }

    @Transactional
    @PreAuthorize("hasAuthority('self:lab-report') and principal.workbench == 'CUSTOMER'")
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

    @PreAuthorize("hasAuthority('self:lab-report') and principal.workbench == 'CUSTOMER'")
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

    @PreAuthorize("hasAuthority('self:assessment') and principal.workbench == 'CUSTOMER'")
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
            HealthProfileVo profile = healthProfileService.getProfile(patient.getId());
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
                            ocrFindings(report).stream()
                                    .map(
                                            item ->
                                                    new AiDtos.OcrFinding(
                                                            item.section(),
                                                            item.item(),
                                                            item.result()))
                                    .toList(),
                            activeModelCodes,
                            toPatientContext(
                                    gender,
                                    age,
                                    profile,
                                    healthScanContextService.latest(
                                            current.tenantId(), patient.getId())));
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

            publishAutomatically(assessment, patient, current);
            report.setStatus("PUBLISHED");
            report.setFailureReason(null);
            touch(report, current.userId());
            labReportMapper.updateById(report);
            return toAssessmentVo(assessment);
        } catch (JsonProcessingException | RuntimeException exception) {
            task.setStatus("FAILED");
            task.setErrorMessage("AI评估或健康报告生成失败");
            task.setFinishedAt(LocalDateTime.now());
            touch(task, current.userId());
            aiTaskMapper.updateById(task);
            report.setStatus("FAILED");
            report.setFailureReason("健康评估报告生成失败，请稍后重试");
            touch(report, current.userId());
            labReportMapper.updateById(report);
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    @PreAuthorize("hasAuthority('self:assessment') and principal.workbench == 'CUSTOMER'")
    public HealthReportVo recoverHealthReport(long assessmentId) {
        HealthAssessmentEntity assessment = assessmentMapper.selectById(assessmentId);
        if (assessment == null || !"SUCCESS".equals(assessment.getStatus())) {
            throw new BusinessException(ErrorCode.LAB_REPORT_NOT_FOUND);
        }
        PatientEntity patient = dataScopeService.requirePatient(assessment.getPatientId());
        HealthReportEntity existing =
                healthReportMapper.selectOne(
                        new LambdaQueryWrapper<HealthReportEntity>()
                                .eq(HealthReportEntity::getAssessmentId, assessmentId)
                                .eq(HealthReportEntity::getDeleted, 0)
                                .last("LIMIT 1"));
        if (existing != null) {
            return toHealthReportVo(existing);
        }

        CurrentPrincipal current = CurrentUser.require();
        HealthReportEntity report = publishAutomatically(assessment, patient, current);
        LabReportEntity labReport = labReportMapper.selectById(assessment.getReportId());
        if (labReport != null) {
            labReport.setStatus("PUBLISHED");
            labReport.setFailureReason(null);
            touch(labReport, current.userId());
            labReportMapper.updateById(labReport);
        }
        return toHealthReportVo(report);
    }

    public List<AssessmentVo> listAssessments() {
        List<Long> patientIds = accessiblePatientIds();
        if (patientIds.isEmpty()) {
            return List.of();
        }
        return dataScopeService.readScoped(
                () -> assessmentMapper
                        .selectList(
                                new LambdaQueryWrapper<HealthAssessmentEntity>()
                                        .in(HealthAssessmentEntity::getPatientId, patientIds)
                                        .orderByDesc(HealthAssessmentEntity::getCreatedAt))
                        .stream()
                        .map(this::toAssessmentVo)
                        .toList());
    }

    public AssessmentVo getAssessment(long id) {
        HealthAssessmentEntity entity =
                dataScopeService.readScoped(() -> assessmentMapper.selectById(id));
        if (entity == null) {
            throw new BusinessException(ErrorCode.LAB_REPORT_NOT_FOUND);
        }
        dataScopeService.requirePatient(entity.getPatientId());
        return dataScopeService.readScoped(() -> toAssessmentVo(entity));
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
        return listHealthReports(null);
    }

    public List<HealthReportVo> listHealthReports(Long patientId) {
        List<Long> patientIds = accessiblePatientIds();
        if (patientIds.isEmpty()) {
            return List.of();
        }
        if (patientId != null) {
            dataScopeService.requirePatient(patientId);
            patientIds = List.of(patientId);
        }
        List<Long> scopedPatientIds = patientIds;
        return dataScopeService.readScoped(
                () -> healthReportMapper
                        .selectList(
                                new LambdaQueryWrapper<HealthReportEntity>()
                                        .in(HealthReportEntity::getPatientId, scopedPatientIds)
                                        .eq(HealthReportEntity::getStatus, "PUBLISHED")
                                        .orderByDesc(HealthReportEntity::getPublishedAt))
                        .stream()
                        .map(this::toHealthReportVo)
                        .toList());
    }

    public HealthReportVo getHealthReport(long id) {
        HealthReportEntity report =
                dataScopeService.readScoped(() -> healthReportMapper.selectById(id));
        if (report == null || !"PUBLISHED".equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.LAB_REPORT_NOT_FOUND);
        }
        dataScopeService.requirePatient(report.getPatientId());
        return dataScopeService.readScoped(() -> toHealthReportVo(report));
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
        return dataScopeService.readScoped(
                () -> followupMapper.selectList(query).stream()
                        .map(this::toFollowupVo)
                        .toList());
    }

    public FollowupTaskVo getFollowup(long id) {
        return toFollowupVo(requireFollowup(id));
    }

    @PreAuthorize("hasAuthority('self:followup') and principal.workbench == 'CUSTOMER'")
    public FollowupTaskVo feedback(long id, FollowupFeedbackRequest request) {
        FollowupTaskEntity task = requireFollowup(id);
        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.FOLLOWUP_INVALID_STATUS);
        }
        int completionRate = calculateCompletionRate(request.actions());
        int cycleNo = task.getCycleNo() == null ? 1 : task.getCycleNo();
        int maxCycles = task.getMaxCycles() == null ? 4 : task.getMaxCycles();
        boolean previousCycleReachedTarget = hasPreviousSuccessfulCycle(task);
        AiDtos.FollowupAdjustmentData adjustment;
        if (cycleNo >= maxCycles) {
            adjustment =
                    terminalFollowupAdjustment(
                            "已达到最多" + maxCycles + "期，本轮健康随访结束。");
        } else if (completionRate >= 80 && previousCycleReachedTarget) {
            adjustment =
                    terminalFollowupAdjustment(
                            "连续两期完成度达到80%，本轮健康随访目标已达成。");
        } else {
            adjustment =
                    adjustFollowupWithAi(
                            task, request, cycleNo, maxCycles, completionRate);
        }
        task.setFeedback(buildFeedbackSummary(request));
        task.setFeedbackDetail(writeFeedbackDetail(request.actions()));
        task.setCompletionRate(completionRate);
        task.setDecision(adjustment.decision());
        task.setDecisionReason(adjustment.decisionReason());
        task.setStatus("COMPLETED");
        task.setCompletedAt(LocalDateTime.now());
        touch(task, CurrentUser.require().userId());
        followupMapper.updateById(task);
        if (!"TERMINATE".equals(adjustment.decision())) {
            createNextAiFollowup(
                    task, request.actions(), adjustment, CurrentUser.require());
        }
        return toFollowupVo(task);
    }

    /** Runs the same assessment pipeline after OCR completes, without requiring customer confirmation. */
    public AssessmentVo submitAiAutomatically(long reportId, long tenantId) {
        LabReportEntity report = requireReport(reportId);
        PatientEntity patient = patientMapper.selectById(report.getPatientId());
        if (patient == null || patient.getUserId() == null) {
            throw new BusinessException(ErrorCode.PATIENT_NOT_FOUND);
        }
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext automatedContext = SecurityContextHolder.createEmptyContext();
        CurrentPrincipal automatedUser =
                new CurrentPrincipal(
                        "system-ocr-" + reportId,
                        "system-ocr",
                        patient.getUserId(),
                        tenantId,
                        List.of("CUSTOMER"),
                        List.of("self:assessment", "self:health-record"),
                        "CUSTOMER");
        automatedContext.setAuthentication(
                new UsernamePasswordAuthenticationToken(automatedUser, null, List.of()));
        SecurityContextHolder.setContext(automatedContext);
        try {
            return submitAi(reportId);
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    /** Current AI follow-up policy: a completed feedback closes this task and opens the next check-in. */
    private int calculateCompletionRate(List<FollowupActionFeedback> actions) {
        return (int)
                Math.round(
                        actions.stream()
                                .mapToInt(
                                        action ->
                                                switch (action.status()) {
                                                    case "COMPLETED" -> 100;
                                                    case "PARTIAL" -> 50;
                                                    default -> 0;
                                                })
                                .average()
                                .orElse(0));
    }

    private boolean hasPreviousSuccessfulCycle(FollowupTaskEntity current) {
        List<FollowupTaskEntity> previous =
                followupMapper.selectList(
                        new LambdaQueryWrapper<FollowupTaskEntity>()
                                .eq(FollowupTaskEntity::getPatientId, current.getPatientId())
                                .eq(FollowupTaskEntity::getStatus, "COMPLETED")
                                .ne(FollowupTaskEntity::getId, current.getId())
                                .orderByDesc(FollowupTaskEntity::getCompletedAt)
                                .last("LIMIT 1"));
        return !previous.isEmpty()
                && previous.getFirst().getCompletionRate() != null
                && previous.getFirst().getCompletionRate() >= 80;
    }

    private String writeFeedbackDetail(List<FollowupActionFeedback> actions) {
        try {
            return objectMapper.writeValueAsString(actions);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize follow-up feedback", exception);
        }
    }

    private String buildFeedbackSummary(FollowupFeedbackRequest request) {
        long completed =
                request.actions().stream()
                        .filter(action -> "COMPLETED".equals(action.status()))
                        .count();
        long partial =
                request.actions().stream()
                        .filter(action -> "PARTIAL".equals(action.status()))
                        .count();
        long pending = request.actions().size() - completed - partial;
        String summary =
                "逐项反馈：已完成"
                        + completed
                        + "项，部分完成"
                        + partial
                        + "项，未完成"
                        + pending
                        + "项。";
        return request.feedback() == null || request.feedback().isBlank()
                ? summary
                : summary + " 补充说明：" + request.feedback().trim();
    }

    private AiDtos.FollowupAdjustmentData adjustFollowupWithAi(
            FollowupTaskEntity task,
            FollowupFeedbackRequest request,
            int cycleNo,
            int maxCycles,
            int completionRate) {
        try {
            PatientEntity patient = dataScopeService.requirePatient(task.getPatientId());
            HealthProfileVo profile = healthProfileService.getProfile(task.getPatientId());
            Integer age =
                    patient.getBirthDate() == null
                            ? null
                            : Period.between(patient.getBirthDate(), LocalDate.now()).getYears();
            String gender =
                    Set.of("MALE", "FEMALE").contains(patient.getGender())
                            ? patient.getGender()
                            : "UNKNOWN";
            AiDtos.FollowupAdjustmentData result =
                    aiServiceClient.adjustFollowup(
                             new AiDtos.FollowupAdjustmentRequest(
                                     toPatientContext(
                                             gender,
                                             age,
                                             profile,
                                             healthScanContextService.latest(
                                                     task.getTenantId(), task.getPatientId())),
                                    cycleNo,
                                    maxCycles,
                                    completionRate,
                                    request.feedback(),
                                    request.actions().stream()
                                            .map(
                                                    action ->
                                                            new AiDtos.FollowupActionFeedback(
                                                                    action.section(),
                                                                    action.action(),
                                                                    action.status(),
                                                                    action.note()))
                                            .toList()));
            if (!Set.of("CONTINUE", "ADJUST", "TERMINATE").contains(result.decision())) {
                return localFollowupAdjustment(
                        request.actions(), completionRate, request.feedback());
            }
            return result;
        } catch (BusinessException exception) {
            return localFollowupAdjustment(
                    request.actions(), completionRate, request.feedback());
        }
    }

    private AiDtos.FollowupAdjustmentData localFollowupAdjustment(
            List<FollowupActionFeedback> actions, int completionRate, String feedback) {
        boolean hasUnfinished =
                actions.stream().anyMatch(action -> !"COMPLETED".equals(action.status()));
        boolean adjusted =
                completionRate < 80
                        || hasUnfinished
                        || containsExecutionDifficulty(actions, feedback);
        String decision = adjusted ? "ADJUST" : "CONTINUE";
        String reason =
                adjusted
                        ? "本期存在未完成行动、身体不适或执行困难，下一期将降低负担并聚焦可完成项目。"
                        : "本期执行情况稳定，进入下一期健康随访。";
        List<AiDtos.FollowupActionSuggestion> suggestions =
                actions.stream()
                        .filter(action -> !adjusted || !"COMPLETED".equals(action.status()))
                        .map(
                                action ->
                                        new AiDtos.FollowupActionSuggestion(
                                                action.section(),
                                                adjusted
                                                        ? adaptLocalFollowupAction(
                                                                action, feedback)
                                                        : action.action()))
                        .toList();
        return new AiDtos.FollowupAdjustmentData(
                decision, reason, "", suggestions, "RULE_FALLBACK", null);
    }

    private boolean containsExecutionDifficulty(
            List<FollowupActionFeedback> actions, String feedback) {
        Stream<String> notes =
                actions.stream()
                        .map(FollowupActionFeedback::note)
                        .filter(Objects::nonNull)
                        .map(String::trim);
        Stream<String> overall =
                feedback == null || feedback.isBlank()
                        ? Stream.empty()
                        : Stream.of(feedback.trim());
        return Stream.concat(notes, overall)
                .anyMatch(
                        note ->
                                Stream.of(
                                                "困难",
                                                "做不到",
                                                "没时间",
                                                "太累",
                                                "很累",
                                                "疲倦",
                                                "疲劳",
                                                "乏力",
                                                "体力不足",
                                                "不舒服",
                                                "疼",
                                                "痛",
                                                "头晕",
                                                "气促",
                                                "失眠",
                                                "压力大",
                                                "无法",
                                                "没有测量工具",
                                                "无测量工具",
                                                "没测量工具",
                                                "没有设备",
                                                "无设备",
                                                "没设备",
                                                "没有血糖仪",
                                                "没有血压计",
                                                "不能测量")
                                        .anyMatch(note::contains));
    }

    private String adaptLocalFollowupAction(
            FollowupActionFeedback action, String overallFeedback) {
        String context =
                (action.note() == null ? "" : action.note())
                        + " "
                        + (overallFeedback == null ? "" : overallFeedback);
        boolean lacksEquipment =
                Stream.of(
                                "没有测量工具",
                                "无测量工具",
                                "没测量工具",
                                "没有设备",
                                "无设备",
                                "没设备",
                                "没有血糖仪",
                                "没有血压计",
                                "无法测量",
                                "不能测量")
                        .anyMatch(context::contains);
        boolean fatigued =
                Stream.of("疲倦", "疲劳", "乏力", "体力不足", "很累", "太累", "容易累")
                        .anyMatch(context::contains);

        if (lacksEquipment && action.action().contains("血糖")) {
            return "本期暂不要求自行测量血糖；每天记录一次三餐主食、甜食摄入和身体感受，连续完成7天。";
        }
        if (lacksEquipment && action.action().contains("血压")) {
            return "本期暂不要求自行测量血压；每天记录一次作息、活动和身体感受，连续完成7天。";
        }
        if (lacksEquipment && action.action().contains("测量")) {
            return "本期暂不要求使用缺少的测量工具；每天记录一次相关行动和身体感受，连续完成7天。";
        }
        if (fatigued && "运动行动".equals(action.section())) {
            return "本周改为低强度步行或舒缓拉伸，每周3次，每次10至15分钟；疲倦加重时休息并记录身体感受。";
        }
        if (action.note() != null && !action.note().isBlank()) {
            return "结合“" + action.note().trim() + "”，先完成原行动的简化版本：" + action.action();
        }
        return "先从原行动约一半的频次开始：" + action.action();
    }

    private AiDtos.FollowupAdjustmentData terminalFollowupAdjustment(String reason) {
        return new AiDtos.FollowupAdjustmentData(
                "TERMINATE", reason, "", List.of(), "RULE_FALLBACK", null);
    }

    private void createNextAiFollowup(
            FollowupTaskEntity completed,
            List<FollowupActionFeedback> actions,
            AiDtos.FollowupAdjustmentData adjustment,
            CurrentPrincipal current) {
        long existingPending =
                followupMapper.selectCount(
                        new LambdaQueryWrapper<FollowupTaskEntity>()
                                .eq(FollowupTaskEntity::getPatientId, completed.getPatientId())
                                .eq(FollowupTaskEntity::getStatus, "PENDING"));
        if (existingPending > 0) {
            return;
        }
        FollowupTaskEntity next = new FollowupTaskEntity();
        next.setTenantId(completed.getTenantId());
        next.setPatientId(completed.getPatientId());
        next.setParentTaskId(completed.getId());
        next.setCycleNo((completed.getCycleNo() == null ? 1 : completed.getCycleNo()) + 1);
        next.setMaxCycles(completed.getMaxCycles() == null ? 4 : completed.getMaxCycles());
        next.setAssigneeId(null);
        next.setTitle("健康随访（第" + next.getCycleNo() + "期）");
        HealthProfileVo profile = healthProfileService.getProfile(completed.getPatientId());
        next.setContent(
                nutritionFollowupPlanService.buildNextPlan(
                        profile,
                        actions,
                        adjustment.nextActions(),
                        "ADJUST".equals(adjustment.decision()),
                        next.getCycleNo()));
        next.setDueDate(LocalDate.now().plusDays(14));
        next.setStatus("PENDING");
        next.setReminderCount(0);
        auditNew(next, current.userId());
        followupMapper.insert(next);
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
        LabReportEntity report =
                dataScopeService.readScoped(() -> labReportMapper.selectById(id));
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
        FollowupTaskEntity task =
                dataScopeService.readScoped(() -> followupMapper.selectById(id));
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
                        .eq(IndicatorValueEntity::getDeleted, 0))
                .stream()
                .filter(item -> LabIndicatorVisibility.isVisible(item.getIndicatorName()))
                .toList();
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
                ocrFindings(report),
                report.getCreatedAt());
    }

    private List<OcrFindingVo> ocrFindings(LabReportEntity report) {
        if (report.getOcrSnapshot() == null || report.getOcrSnapshot().isBlank()) {
            return List.of();
        }
        try {
            JsonNode findings = objectMapper.readTree(report.getOcrSnapshot()).path("findings");
            if (!findings.isArray()) {
                return List.of();
            }
            List<OcrFindingVo> values = new ArrayList<>();
            findings.forEach(
                    node -> {
                        String section = node.path("section").asText("体检结果").trim();
                        String item = node.path("item").asText("").trim();
                        String result = node.path("result").asText("").trim();
                        if (!item.isBlank()
                                && !result.isBlank()
                                && LabIndicatorVisibility.isFindingVisible(item)) {
                            values.add(
                                    new OcrFindingVo(
                                            section.isBlank() ? "体检结果" : section,
                                            item,
                                            result));
                        }
                    });
            return List.copyOf(values);
        } catch (JsonProcessingException exception) {
            return List.of();
        }
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

    private AiDtos.PatientContext toPatientContext(
            String gender, Integer age, HealthProfileVo profile) {
        return toPatientContext(
                gender, age, profile, HealthScanContextService.LatestVitals.empty());
    }

    private AiDtos.PatientContext toPatientContext(
            String gender,
            Integer age,
            HealthProfileVo profile,
            HealthScanContextService.LatestVitals latestVitals) {
        return new AiDtos.PatientContext(
                gender,
                age,
                profile.heightCm(),
                profile.weightKg(),
                profile.waistCm(),
                profile.recentWeightChangeKg(),
                profile.bmi(),
                profile.medicalHistory(),
                profile.familyHistory(),
                profile.diabetesStatus(),
                profile.hypertensionStatus(),
                profile.dyslipidemiaStatus(),
                profile.fattyLiverStatus(),
                profile.smokingStatus(),
                profile.alcoholStatus(),
                profile.exerciseFrequency(),
                profile.sleepQuality(),
                profile.sleepHours(),
                profile.stressLevel(),
                profile.moodStatus(),
                profile.fearLevel(),
                profile.dietaryPreference(),
                profile.recentDietaryPattern(),
                latestVitals.heartRate(),
                latestVitals.heartRateVariability(),
                latestVitals.oxygenSaturation(),
                latestVitals.respirationRate(),
                latestVitals.systolicBloodPressure(),
                latestVitals.diastolicBloodPressure(),
                latestVitals.stressHrv(),
                latestVitals.qualityScore());
    }

    private HealthReportEntity publishAutomatically(
            HealthAssessmentEntity assessment, PatientEntity patient, CurrentPrincipal current) {
        HealthReportEntity report = new HealthReportEntity();
        report.setTenantId(current.tenantId());
        report.setPatientId(patient.getId());
        report.setAssessmentId(assessment.getId());
        report.setReportNo("HR" + System.currentTimeMillis());
        report.setTitle(patient.getName() + "的健康管理评估报告");
        report.setStatus("PUBLISHED");
        report.setSummary("AI 初评已生成，可结合健康管理计划持续跟进。");
        report.setDoctorOpinion(null);
        report.setDisclaimer(null);
        report.setPublishedAt(LocalDateTime.now());
        report.setPublishedBy(current.userId());
        auditNew(report, current.userId());
        healthReportMapper.insert(report);
        pdfReportService.generateAndStore(report, assessment, patient, current.userId());
        createAiFollowup(assessment, patient, current);
        return report;
    }

    private void createAiFollowup(
            HealthAssessmentEntity assessment, PatientEntity patient, CurrentPrincipal current) {
        List<FollowupTaskEntity> unfinished =
                followupMapper.selectList(
                        new LambdaQueryWrapper<FollowupTaskEntity>()
                                .eq(FollowupTaskEntity::getPatientId, patient.getId())
                                .eq(FollowupTaskEntity::getStatus, "PENDING"));
        for (FollowupTaskEntity existing : unfinished) {
            existing.setStatus("CANCELLED");
            existing.setDecision("TERMINATE");
            existing.setDecisionReason("检测到新的健康报告，旧计划已结束并重新评估。");
            touch(existing, current.userId());
            followupMapper.updateById(existing);
        }
        String risk = assessment.getOverallRiskLevel();
        int days = "HIGH".equals(risk) ? 3 : "ATTENTION".equals(risk) ? 7 : 14;
        FollowupTaskEntity task = new FollowupTaskEntity();
        task.setTenantId(current.tenantId());
        task.setPatientId(patient.getId());
        task.setCycleNo(1);
        task.setMaxCycles(4);
        task.setAssigneeId(null);
        task.setTitle("本周健康计划");
        HealthProfileVo profile = healthProfileService.getProfile(patient.getId());
        task.setContent(nutritionFollowupPlanService.buildInitialPlan(assessment, profile, 1));
        task.setDueDate(LocalDate.now().plusDays(days));
        task.setStatus("PENDING");
        task.setReminderCount(0);
        auditNew(task, current.userId());
        followupMapper.insert(task);
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
                patientName(report.getPatientId()),
                String.valueOf(report.getAssessmentId()),
                report.getReportNo(),
                report.getTitle(),
                report.getStatus(),
                report.getSummary(),
                report.getDoctorOpinion(),
                report.getDisclaimer(),
                report.getPublishedAt(),
                toAssessmentVo(assessmentMapper.selectById(report.getAssessmentId())));
    }

    private FollowupTaskVo toFollowupVo(FollowupTaskEntity task) {
        return new FollowupTaskVo(
                String.valueOf(task.getId()),
                String.valueOf(task.getPatientId()),
                patientName(task.getPatientId()),
                task.getTitle(),
                task.getContent(),
                task.getDueDate(),
                task.getStatus(),
                task.getFeedback(),
                task.getFeedbackDetail(),
                task.getCompletedAt(),
                task.getCycleNo(),
                task.getMaxCycles(),
                task.getCompletionRate(),
                task.getDecision(),
                task.getDecisionReason(),
                task.getReminderCount(),
                task.getLastRemindedAt());
    }

    private String patientName(Long patientId) {
        PatientEntity patient =
                dataScopeService.readScoped(() -> patientMapper.selectById(patientId));
        return patient == null || patient.getName() == null || patient.getName().isBlank()
                ? "未命名用户"
                : patient.getName();
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
