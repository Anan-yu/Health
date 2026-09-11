package com.rayk.health.assistant.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.assistant.dto.CreateMedicalAssistantConversationRequest;
import com.rayk.health.assistant.dto.SendMedicalAssistantMessageRequest;
import com.rayk.health.assistant.entity.MedicalAssistantConversationEntity;
import com.rayk.health.assistant.entity.MedicalAssistantMessageEntity;
import com.rayk.health.assistant.entity.HealthTreeHoleFeedbackEntity;
import com.rayk.health.assistant.mapper.MedicalAssistantConversationMapper;
import com.rayk.health.assistant.mapper.MedicalAssistantMessageMapper;
import com.rayk.health.assistant.mapper.HealthTreeHoleFeedbackMapper;
import com.rayk.health.assistant.vo.MedicalAssistantConversationVo;
import com.rayk.health.assistant.vo.MedicalAssistantMessageVo;
import com.rayk.health.assistant.vo.HealthTreeHoleFeedbackVo;
import com.rayk.health.assessment.application.WorkflowApplicationService;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.integration.ai.AiDtos;
import com.rayk.health.integration.ai.AiServiceClient;
import com.rayk.health.healthscan.application.HealthScanContextService;
import com.rayk.health.membership.application.MembershipEntitlementService;
import com.rayk.health.patient.application.DataScopeService;
import com.rayk.health.patient.application.HealthProfileService;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.patient.mapper.PatientMapper;
import com.rayk.health.patient.vo.HealthProfileVo;
import com.rayk.health.report.vo.HealthReportVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.tenant.TenantContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.scheduler.Schedulers;

/** Customer-scoped conversation orchestration for 健康助手. */
@Service
public class MedicalAssistantApplicationService {
    private static final Logger log = LoggerFactory.getLogger(MedicalAssistantApplicationService.class);
    private static final String MODEL = "qwen3.8-flash";
    private static final String BENEFIT_CODE = "AI_MEDICAL_ASSISTANT";
    private static final String TREE_HOLE_BENEFIT_CODE =
            MembershipEntitlementService.HEALTH_TREE_HOLE_BENEFIT_CODE;
    private static final String BIZ_TYPE = "MEDICAL_ASSISTANT";
    private static final String TREE_HOLE_CONVERSATION_TYPE = "HEALTH_TREE_HOLE";
    private static final String TREE_HOLE_AI_MODE = "HEALTH_TREE_HOLE";
    private static final String TREE_HOLE_FEEDBACK_AI_MODE = "HEALTH_TREE_HOLE_FEEDBACK";
    private static final String TREE_HOLE_BIZ_TYPE = "HEALTH_TREE_HOLE";
    private static final int TREE_HOLE_PERIOD_DAYS = 7;
    private static final String DISCLAIMER =
            "健康助手仅用于健康管理与就医沟通参考，不构成医学诊断、处方或急救替代。";

    private final MedicalAssistantConversationMapper conversationMapper;
    private final MedicalAssistantMessageMapper messageMapper;
    private final PatientMapper patientMapper;
    private final DataScopeService dataScopeService;
    private final HealthProfileService healthProfileService;
    private final HealthScanContextService healthScanContextService;
    private final WorkflowApplicationService workflowService;
    private final MembershipEntitlementService membershipEntitlementService;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;
    private final HealthTreeHoleFeedbackMapper feedbackMapper;

    public MedicalAssistantApplicationService(
            MedicalAssistantConversationMapper conversationMapper,
            MedicalAssistantMessageMapper messageMapper,
            PatientMapper patientMapper,
            DataScopeService dataScopeService,
            HealthProfileService healthProfileService,
            HealthScanContextService healthScanContextService,
            WorkflowApplicationService workflowService,
            MembershipEntitlementService membershipEntitlementService,
            AiServiceClient aiServiceClient,
            ObjectMapper objectMapper,
            HealthTreeHoleFeedbackMapper feedbackMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.patientMapper = patientMapper;
        this.dataScopeService = dataScopeService;
        this.healthProfileService = healthProfileService;
        this.healthScanContextService = healthScanContextService;
        this.workflowService = workflowService;
        this.membershipEntitlementService = membershipEntitlementService;
        this.aiServiceClient = aiServiceClient;
        this.objectMapper = objectMapper;
        this.feedbackMapper = feedbackMapper;
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    @Transactional
    public MedicalAssistantConversationVo createConversation(
            CreateMedicalAssistantConversationRequest request) {
        return createConversation(request, "MEDICAL_ASSISTANT");
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    @Transactional
    public MedicalAssistantConversationVo createTreeHoleConversation(
            CreateMedicalAssistantConversationRequest request) {
        return createConversation(request, TREE_HOLE_CONVERSATION_TYPE);
    }

    private MedicalAssistantConversationVo createConversation(
            CreateMedicalAssistantConversationRequest request, String conversationType) {
        CurrentPrincipal current = CurrentUser.require();
        PatientEntity patient = currentPatient();
        LocalDateTime now = LocalDateTime.now();
        MedicalAssistantConversationEntity conversation = new MedicalAssistantConversationEntity();
        conversation.setTenantId(current.tenantId());
        conversation.setPatientId(patient.getId());
        conversation.setTitle(
                normalizeTitle(
                        request == null ? null : request.title(), conversationType));
        conversation.setStatus("ACTIVE");
        conversation.setModel(MODEL);
        conversation.setConversationType(conversationType);
        auditNew(conversation, current.userId(), now);
        conversationMapper.insert(conversation);
        return toConversation(conversation, List.of());
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public List<MedicalAssistantConversationVo> listConversations() {
        return listConversations("MEDICAL_ASSISTANT");
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public List<MedicalAssistantConversationVo> listTreeHoleConversations() {
        return listConversations(TREE_HOLE_CONVERSATION_TYPE);
    }

    private List<MedicalAssistantConversationVo> listConversations(String conversationType) {
        CurrentPrincipal current = CurrentUser.require();
        PatientEntity patient = currentPatient();
        return conversationMapper
                .selectList(
                        new LambdaQueryWrapper<MedicalAssistantConversationEntity>()
                                .eq(MedicalAssistantConversationEntity::getTenantId, current.tenantId())
                                .eq(MedicalAssistantConversationEntity::getPatientId, patient.getId())
                                .eq(MedicalAssistantConversationEntity::getDeleted, 0)
                                .eq(MedicalAssistantConversationEntity::getConversationType, conversationType)
                                .orderByDesc(MedicalAssistantConversationEntity::getUpdatedAt)
                                .last("LIMIT 50"))
                .stream()
                .map(item -> toConversation(item, List.of()))
                .toList();
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public MedicalAssistantConversationVo getConversation(long conversationId) {
        return getConversation(conversationId, "MEDICAL_ASSISTANT");
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public MedicalAssistantConversationVo getTreeHoleConversation(long conversationId) {
        return getConversation(conversationId, TREE_HOLE_CONVERSATION_TYPE);
    }

    private MedicalAssistantConversationVo getConversation(long conversationId, String conversationType) {
        MedicalAssistantConversationEntity conversation = ownedConversation(conversationId, conversationType);
        return toConversation(conversation, loadMessages(conversationId));
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    @Transactional
    public void deleteConversation(long conversationId) {
        deleteConversation(conversationId, "MEDICAL_ASSISTANT");
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    @Transactional
    public void deleteTreeHoleConversation(long conversationId) {
        deleteConversation(conversationId, TREE_HOLE_CONVERSATION_TYPE);
    }

    private void deleteConversation(long conversationId, String conversationType) {
        CurrentPrincipal current = CurrentUser.require();
        MedicalAssistantConversationEntity conversation = ownedConversation(conversationId, conversationType);
        messageMapper.delete(
                new LambdaQueryWrapper<MedicalAssistantMessageEntity>()
                        .eq(MedicalAssistantMessageEntity::getConversationId, conversationId)
                        .eq(MedicalAssistantMessageEntity::getTenantId, current.tenantId())
                        .eq(MedicalAssistantMessageEntity::getPatientId, conversation.getPatientId()));
        conversationMapper.delete(
                new LambdaQueryWrapper<MedicalAssistantConversationEntity>()
                        .eq(MedicalAssistantConversationEntity::getId, conversationId)
                        .eq(MedicalAssistantConversationEntity::getTenantId, current.tenantId())
                        .eq(MedicalAssistantConversationEntity::getPatientId, conversation.getPatientId()));
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    @Transactional
    public MedicalAssistantConversationVo sendMessage(
            long conversationId, SendMedicalAssistantMessageRequest request) {
        return sendMessage(conversationId, request, "MEDICAL_ASSISTANT");
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    @Transactional
    public MedicalAssistantConversationVo sendTreeHoleMessage(
            long conversationId, SendMedicalAssistantMessageRequest request) {
        return sendMessage(conversationId, request, TREE_HOLE_CONVERSATION_TYPE);
    }

    private MedicalAssistantConversationVo sendMessage(
            long conversationId, SendMedicalAssistantMessageRequest request, String conversationType) {
        MedicalAssistantConversationEntity conversation = ownedConversation(conversationId, conversationType);
        CurrentPrincipal current = CurrentUser.require();
        String content = request == null || request.content() == null ? "" : request.content().trim();
        if (content.isBlank()) {
            throw new com.rayk.health.common.exception.BusinessException(
                    com.rayk.health.common.exception.ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
        String usageKey =
                BENEFIT_CODE
                        + ":"
                        + current.userId()
                        + ":"
                        + conversationId
                        + ":"
                        + UUID.randomUUID();
        MembershipEntitlementService.UsageReservation usage =
                membershipEntitlementService.reserve(
                        benefitCode(conversationType),
                        TREE_HOLE_CONVERSATION_TYPE.equals(conversationType) ? TREE_HOLE_BIZ_TYPE : BIZ_TYPE,
                        String.valueOf(conversationId),
                        usageKey);
        try {
            LocalDateTime now = LocalDateTime.now();
            insertUserMessage(conversation, content, current, now);
            PatientEntity patient = dataScopeService.requirePatient(conversation.getPatientId());
            HealthProfileVo profile = healthProfileService.getProfile(patient.getId());
            HealthScanContextService.LatestVitals vitals =
                    healthScanContextService.latest(current.tenantId(), patient.getId());
            HealthReportVo latestReport = latestReport();
            List<MedicalAssistantMessageEntity> history = loadEntities(conversationId, 12);
            AiDtos.MedicalAssistantData answer =
                    aiServiceClient.answerMedicalAssistant(
                            new AiDtos.MedicalAssistantRequest(
                                    String.valueOf(conversationId),
                                    String.valueOf(patient.getId()),
                                    patient.getName(),
                                    history.stream()
                                            .map(item -> new AiDtos.MedicalAssistantMessage(item.getRole(), item.getContent()))
                                            .toList(),
                                    toPatientContext(patient, profile, vitals),
                                    latestReport == null ? null : reportSummary(latestReport),
                                    latestReport == null || latestReport.assessment() == null
                                            ? null
                                            : limit(
                                                    Objects.toString(
                                                            latestReport.assessment().results(), ""),
                                                    50000),
                                    MODEL,
                                    false,
                                    aiMode(conversationType)));
            insertAssistantMessage(conversation, answer, current, now);
            conversation.setUpdatedBy(current.userId());
            conversation.setUpdatedAt(LocalDateTime.now());
            if (isDefaultConversationTitle(conversation.getTitle())) {
                conversation.setTitle(shortTitle(content));
            }
            conversationMapper.updateById(conversation);
            membershipEntitlementService.confirm(usage.usageId());
            return toConversation(conversation, loadMessages(conversationId));
        } catch (RuntimeException exception) {
            membershipEntitlementService.release(usage.usageId());
            throw exception;
        }
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public SseEmitter streamMessage(
            long conversationId, SendMedicalAssistantMessageRequest request) {
        return streamMessage(conversationId, request, "MEDICAL_ASSISTANT");
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public SseEmitter streamTreeHoleMessage(
            long conversationId, SendMedicalAssistantMessageRequest request) {
        return streamMessage(conversationId, request, TREE_HOLE_CONVERSATION_TYPE);
    }

    private SseEmitter streamMessage(
            long conversationId, SendMedicalAssistantMessageRequest request, String conversationType) {
        StreamContext context = prepareStream(conversationId, request, conversationType);
        SseEmitter emitter = new SseEmitter(150_000L);
        AtomicBoolean settled = new AtomicBoolean(false);
        Runnable release = () -> releaseUsage(context);
        emitter.onTimeout(
                () -> {
                    if (settled.compareAndSet(false, true)) {
                        release.run();
                        emitter.complete();
                    }
                });
        emitter.onCompletion(
                () -> {
                    if (settled.compareAndSet(false, true)) release.run();
                });

        aiServiceClient
                .streamMedicalAssistant(context.aiRequest())
                .publishOn(Schedulers.boundedElastic())
                .subscribe(
                        event ->
                                executeWithTenant(
                                        context.current().tenantId(),
                                        () -> consumeStreamEvent(context, emitter, settled, event)),
                        error ->
                                executeWithTenant(
                                        context.current().tenantId(),
                                        () -> failStream(context, emitter, settled)),
                        () -> {
                            executeWithTenant(
                                    context.current().tenantId(),
                                    () -> {
                                        if (settled.compareAndSet(false, true)) {
                                            release.run();
                                            sendStreamError(emitter);
                                        }
                                    });
                        });
        return emitter;
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    @Transactional(readOnly = true)
    public HealthTreeHoleFeedbackVo treeHoleFeedback() {
        CurrentPrincipal current = CurrentUser.require();
        PatientEntity patient = currentPatient();
        return toTreeHoleFeedbackVo(treeHoleFeedbackState(current, patient));
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    @Transactional
    public HealthTreeHoleFeedbackVo generateTreeHoleFeedback() {
        CurrentPrincipal current = CurrentUser.require();
        PatientEntity patient = currentPatient();
        TreeHoleFeedbackState state = treeHoleFeedbackState(current, patient);
        if (!state.canGenerate()) {
            throw new BusinessException(ErrorCode.HEALTH_TREE_HOLE_FEEDBACK_NOT_READY);
        }
        return generateTreeHoleFeedback(current, patient, state);
    }

    /**
     * Generates a due feedback for a scheduled customer scan. Returning false means that the
     * customer's seven-day window is not due yet or has already been generated.
     */
    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    @Transactional
    public boolean generateTreeHoleFeedbackIfDue(long patientId) {
        CurrentPrincipal current = CurrentUser.require();
        PatientEntity patient = dataScopeService.requirePatient(patientId);
        TreeHoleFeedbackState state = treeHoleFeedbackState(current, patient);
        if (!state.canGenerate()
                || findTreeHoleFeedback(
                                current, patient.getId(), state.currentPeriodStart(), state.currentPeriodEnd())
                        != null) {
            return false;
        }
        generateTreeHoleFeedback(current, patient, state);
        return true;
    }

    private HealthTreeHoleFeedbackVo generateTreeHoleFeedback(
            CurrentPrincipal current, PatientEntity patient, TreeHoleFeedbackState state) {
        HealthTreeHoleFeedbackEntity existing =
                findTreeHoleFeedback(
                        current, patient.getId(), state.currentPeriodStart(), state.currentPeriodEnd());
        if (existing != null) {
            return treeHoleFeedback();
        }

        String periodKey = state.currentPeriodStart().toString();
        String usageKey =
                TREE_HOLE_BENEFIT_CODE
                        + ":"
                        + current.userId()
                        + ":TREE_HOLE_FEEDBACK:"
                        + periodKey;
        MembershipEntitlementService.UsageReservation usage =
                membershipEntitlementService.reserve(
                        TREE_HOLE_BENEFIT_CODE,
                        TREE_HOLE_BIZ_TYPE,
                        String.valueOf(patient.getId()) + ":" + periodKey,
                        usageKey);
        try {
            HealthProfileVo profile = healthProfileService.getProfile(patient.getId());
            HealthScanContextService.LatestVitals vitals =
                    healthScanContextService.latest(current.tenantId(), patient.getId());
            HealthReportVo latestReport = latestReport();
            String feedbackPrompt =
                    treeHoleFeedbackPrompt(
                            state.currentPeriodStart(), state.currentPeriodEnd(), state.entries());
            AiDtos.MedicalAssistantData answer =
                    aiServiceClient.answerMedicalAssistant(
                            new AiDtos.MedicalAssistantRequest(
                                    "tree-hole-feedback:" + patient.getId() + ":" + periodKey,
                                    String.valueOf(patient.getId()),
                                    patient.getName(),
                                    List.of(new AiDtos.MedicalAssistantMessage("USER", feedbackPrompt)),
                                    toPatientContext(patient, profile, vitals),
                                    latestReport == null ? null : reportSummary(latestReport),
                                    latestReport == null || latestReport.assessment() == null
                                            ? null
                                            : limit(
                                                    Objects.toString(
                                                            latestReport.assessment().results(), ""),
                                                    50000),
                                    MODEL,
                                    false,
                                    TREE_HOLE_FEEDBACK_AI_MODE));
            if (answer == null || answer.reply() == null || answer.reply().isBlank()) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }

            LocalDateTime now = LocalDateTime.now();
            HealthTreeHoleFeedbackEntity feedback = new HealthTreeHoleFeedbackEntity();
            feedback.setTenantId(current.tenantId());
            feedback.setPatientId(patient.getId());
            feedback.setPeriodStart(state.currentPeriodStart());
            feedback.setPeriodEnd(state.currentPeriodEnd());
            feedback.setRecordedDays(state.recordedDays());
            feedback.setEntryCount(state.entryCount());
            feedback.setContent(limit(answer.reply().trim(), 12000));
            feedback.setRiskLevel(answer.riskLevel());
            feedback.setRecommendedAction(limit(answer.recommendedAction(), 800));
            feedback.setModel(answer.model() == null ? MODEL : answer.model());
            feedback.setGeneratedAt(now);
            feedback.setCreatedBy(current.userId());
            feedback.setCreatedAt(now);
            feedback.setUpdatedBy(current.userId());
            feedback.setUpdatedAt(now);
            feedback.setDeleted(0);
            feedback.setVersion(0);
            feedbackMapper.insert(feedback);
            membershipEntitlementService.confirm(usage.usageId());
            return treeHoleFeedback();
        } catch (RuntimeException exception) {
            membershipEntitlementService.release(usage.usageId());
            throw exception;
        }
    }

    private TreeHoleFeedbackState treeHoleFeedbackState(
            CurrentPrincipal current, PatientEntity patient) {
        HealthTreeHoleFeedbackEntity latest = latestTreeHoleFeedback(current, patient.getId());
        List<MedicalAssistantMessageEntity> allEntries =
                loadTreeHoleUserEntries(current, patient.getId());
        LocalDate anchor =
                latest == null || latest.getPeriodEnd() == null
                        ? null
                        : latest.getPeriodEnd().plusDays(1);
        List<MedicalAssistantMessageEntity> candidates =
                allEntries.stream()
                        .filter(item -> item.getCreatedAt() != null)
                        .filter(
                                item ->
                                        anchor == null
                                                || !item.getCreatedAt().toLocalDate().isBefore(anchor))
                        .toList();
        LocalDate periodStart =
                candidates.stream()
                        .map(item -> item.getCreatedAt().toLocalDate())
                        .findFirst()
                        .orElse(null);
        LocalDate periodEnd =
                periodStart == null ? null : periodStart.plusDays(TREE_HOLE_PERIOD_DAYS - 1L);
        List<MedicalAssistantMessageEntity> periodEntries =
                periodStart == null
                        ? List.of()
                        : candidates.stream()
                                .filter(
                                        item ->
                                                !item.getCreatedAt().toLocalDate().isAfter(periodEnd))
                                .toList();
        Set<LocalDate> recordedDates = new HashSet<>();
        periodEntries.forEach(item -> recordedDates.add(item.getCreatedAt().toLocalDate()));
        boolean canGenerate =
                periodStart != null
                        && periodEnd != null
                        && !LocalDate.now().isBefore(periodEnd)
                        && !periodEntries.isEmpty();
        return new TreeHoleFeedbackState(
                latest,
                periodStart,
                periodEnd,
                periodEnd,
                periodEntries,
                recordedDates.size(),
                periodEntries.size(),
                canGenerate);
    }

    private HealthTreeHoleFeedbackEntity latestTreeHoleFeedback(
            CurrentPrincipal current, long patientId) {
        return feedbackMapper.selectOne(
                new LambdaQueryWrapper<HealthTreeHoleFeedbackEntity>()
                        .eq(HealthTreeHoleFeedbackEntity::getTenantId, current.tenantId())
                        .eq(HealthTreeHoleFeedbackEntity::getPatientId, patientId)
                        .eq(HealthTreeHoleFeedbackEntity::getDeleted, 0)
                        .orderByDesc(HealthTreeHoleFeedbackEntity::getPeriodEnd)
                        .last("LIMIT 1"));
    }

    private HealthTreeHoleFeedbackEntity findTreeHoleFeedback(
            CurrentPrincipal current, long patientId, LocalDate periodStart, LocalDate periodEnd) {
        return feedbackMapper.selectOne(
                new LambdaQueryWrapper<HealthTreeHoleFeedbackEntity>()
                        .eq(HealthTreeHoleFeedbackEntity::getTenantId, current.tenantId())
                        .eq(HealthTreeHoleFeedbackEntity::getPatientId, patientId)
                        .eq(HealthTreeHoleFeedbackEntity::getPeriodStart, periodStart)
                        .eq(HealthTreeHoleFeedbackEntity::getPeriodEnd, periodEnd)
                        .eq(HealthTreeHoleFeedbackEntity::getDeleted, 0)
                        .last("LIMIT 1"));
    }

    private List<MedicalAssistantMessageEntity> loadTreeHoleUserEntries(
            CurrentPrincipal current, long patientId) {
        List<Long> conversationIds =
                conversationMapper
                        .selectList(
                                new LambdaQueryWrapper<MedicalAssistantConversationEntity>()
                                        .eq(MedicalAssistantConversationEntity::getTenantId, current.tenantId())
                                        .eq(MedicalAssistantConversationEntity::getPatientId, patientId)
                                        .eq(MedicalAssistantConversationEntity::getConversationType, TREE_HOLE_CONVERSATION_TYPE)
                                        .eq(MedicalAssistantConversationEntity::getDeleted, 0)
                                        .select(MedicalAssistantConversationEntity::getId))
                        .stream()
                        .map(MedicalAssistantConversationEntity::getId)
                        .filter(Objects::nonNull)
                        .toList();
        if (conversationIds.isEmpty()) return List.of();
        return messageMapper.selectList(
                new LambdaQueryWrapper<MedicalAssistantMessageEntity>()
                        .in(MedicalAssistantMessageEntity::getConversationId, conversationIds)
                        .eq(MedicalAssistantMessageEntity::getPatientId, patientId)
                        .eq(MedicalAssistantMessageEntity::getTenantId, current.tenantId())
                        .eq(MedicalAssistantMessageEntity::getRole, "USER")
                        .eq(MedicalAssistantMessageEntity::getDeleted, 0)
                        .orderByAsc(MedicalAssistantMessageEntity::getCreatedAt)
                        .last("LIMIT 1000"));
    }

    private String treeHoleFeedbackPrompt(
            LocalDate periodStart,
            LocalDate periodEnd,
            List<MedicalAssistantMessageEntity> entries) {
        StringBuilder prompt =
                new StringBuilder(
                        "请根据以下健康树洞记录生成七天阶段反馈。周期："
                                + periodStart
                                + " 至 "
                                + periodEnd
                                + "。\n"
                                + "请只使用记录中明确出现的内容，先总结整体状态，再指出值得观察的变化，最后给出下一周一个小行动。"
                                + "如果记录出现明确危险信号，才提示及时就医；不要确诊、开药或虚构缺失信息。\n\n记录：\n");
        for (MedicalAssistantMessageEntity entry : entries) {
            String content = Objects.toString(entry.getContent(), "").replaceAll("\\s+", " ").trim();
            if (content.isBlank()) continue;
            prompt.append("- ")
                    .append(entry.getCreatedAt().toLocalDate())
                    .append("：")
                    .append(limit(content, 420))
                    .append('\n');
        }
        return limit(prompt.toString(), 3800);
    }

    private HealthTreeHoleFeedbackVo toTreeHoleFeedbackVo(TreeHoleFeedbackState state) {
        HealthTreeHoleFeedbackEntity latest = state.latest();
        return new HealthTreeHoleFeedbackVo(
                state.canGenerate(),
                latest != null,
                state.recordedDays(),
                state.entryCount(),
                state.currentPeriodStart(),
                state.currentPeriodEnd(),
                state.nextFeedbackDate(),
                latest == null ? null : latest.getPeriodStart(),
                latest == null ? null : latest.getPeriodEnd(),
                latest == null ? null : latest.getContent(),
                latest == null ? null : latest.getRiskLevel(),
                latest == null ? null : latest.getRecommendedAction(),
                latest == null ? null : latest.getModel(),
                latest == null ? null : latest.getGeneratedAt(),
                DISCLAIMER);
    }

    private record TreeHoleFeedbackState(
            HealthTreeHoleFeedbackEntity latest,
            LocalDate currentPeriodStart,
            LocalDate currentPeriodEnd,
            LocalDate nextFeedbackDate,
            List<MedicalAssistantMessageEntity> entries,
            int recordedDays,
            int entryCount,
            boolean canGenerate) {}

    private StreamContext prepareStream(
            long conversationId,
            SendMedicalAssistantMessageRequest request,
            String conversationType) {
        MedicalAssistantConversationEntity conversation = ownedConversation(conversationId, conversationType);
        CurrentPrincipal current = CurrentUser.require();
        String content = request == null || request.content() == null ? "" : request.content().trim();
        if (content.isBlank()) {
            throw new com.rayk.health.common.exception.BusinessException(
                    com.rayk.health.common.exception.ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
        String usageKey =
                BENEFIT_CODE
                        + ":"
                        + current.userId()
                        + ":"
                        + conversationId
                        + ":"
                        + UUID.randomUUID();
        MembershipEntitlementService.UsageReservation usage =
                membershipEntitlementService.reserve(
                        benefitCode(conversationType),
                        TREE_HOLE_CONVERSATION_TYPE.equals(conversationType) ? TREE_HOLE_BIZ_TYPE : BIZ_TYPE,
                        String.valueOf(conversationId),
                        usageKey);
        try {
            LocalDateTime now = LocalDateTime.now();
            insertUserMessage(conversation, content, current, now);
            PatientEntity patient = dataScopeService.requirePatient(conversation.getPatientId());
            HealthProfileVo profile = healthProfileService.getProfile(patient.getId());
            HealthScanContextService.LatestVitals vitals =
                    healthScanContextService.latest(current.tenantId(), patient.getId());
            HealthReportVo latestReport = latestReport();
            List<MedicalAssistantMessageEntity> history = loadEntities(conversationId, 12);
            AiDtos.MedicalAssistantRequest aiRequest =
                    new AiDtos.MedicalAssistantRequest(
                            String.valueOf(conversationId),
                            String.valueOf(patient.getId()),
                            patient.getName(),
                            history.stream()
                                    .map(
                                            item ->
                                                    new AiDtos.MedicalAssistantMessage(
                                                            item.getRole(), item.getContent()))
                                    .toList(),
                            toPatientContext(patient, profile, vitals),
                            latestReport == null ? null : reportSummary(latestReport),
                            latestReport == null || latestReport.assessment() == null
                                    ? null
                                    : limit(
                                            Objects.toString(
                                                    latestReport.assessment().results(), ""),
                                            50000),
                            MODEL,
                            false,
                            aiMode(conversationType));
            return new StreamContext(conversation, current, content, usage, aiRequest);
        } catch (RuntimeException exception) {
            membershipEntitlementService.release(usage.usageId());
            throw exception;
        }
    }

    private void consumeStreamEvent(
            StreamContext context, SseEmitter emitter, AtomicBoolean settled, String event) {
        if (settled.get()) return;
        String eventType = "unknown";
        try {
            JsonNode root = objectMapper.readTree(event);
            String type = root.path("type").asText();
            eventType = type.isBlank() ? "unknown" : type;
            if ("delta".equals(type)) {
                String content = root.path("content").asText("");
                if (!content.isBlank()) {
                    sendStreamEvent(emitter, Map.of("type", "delta", "content", content));
                }
                return;
            }
            if ("error".equals(type)) {
                failStream(context, emitter, settled);
                return;
            }
            if (!"done".equals(type) || !root.hasNonNull("data")) return;
            if (!settled.compareAndSet(false, true)) return;
            AiDtos.MedicalAssistantData answer =
                    objectMapper.treeToValue(root.get("data"), AiDtos.MedicalAssistantData.class);
            MedicalAssistantConversationVo conversation = completeStream(context, answer);
            sendStreamEvent(
                    emitter,
                    Map.of("type", "done", "conversation", conversation));
            emitter.complete();
        } catch (Exception exception) {
            log.warn(
                    "Medical assistant stream event failed eventType={} exceptionType={} message={}",
                    eventType,
                    exception.getClass().getSimpleName(),
                    safeExceptionMessage(exception));
            if (settled.get()) {
                releaseUsage(context);
                sendStreamError(emitter);
            } else {
                failStream(context, emitter, settled);
            }
        }
    }

    private MedicalAssistantConversationVo completeStream(
            StreamContext context, AiDtos.MedicalAssistantData answer) {
        LocalDateTime now = LocalDateTime.now();
        insertAssistantMessage(context.conversation(), answer, context.current(), now);
        context.conversation().setUpdatedBy(context.current().userId());
        context.conversation().setUpdatedAt(now);
        if (isDefaultConversationTitle(context.conversation().getTitle())) {
            context.conversation().setTitle(shortTitle(context.content()));
        }
        conversationMapper.updateById(context.conversation());
        confirmUsage(context);
        return toConversation(
                context.conversation(), loadMessages(context.conversation().getId()));
    }

    private void failStream(
            StreamContext context, SseEmitter emitter, AtomicBoolean settled) {
        if (!settled.compareAndSet(false, true)) return;
        releaseUsage(context);
        sendStreamError(emitter);
    }

    private void releaseUsage(StreamContext context) {
        try {
            executeWithTenant(
                    context.current().tenantId(),
                    () -> membershipEntitlementService.release(context.usage().usageId()));
        } catch (RuntimeException exception) {
            log.warn(
                    "Medical assistant usage release failed usageId={} exceptionType={}",
                    context.usage().usageId(),
                    exception.getClass().getSimpleName());
        }
    }

    private void confirmUsage(StreamContext context) {
        executeWithTenant(
                context.current().tenantId(),
                () -> membershipEntitlementService.confirm(context.usage().usageId()));
    }

    private void executeWithTenant(long tenantId, Runnable action) {
        Long previous = TenantContext.get();
        try {
            TenantContext.set(tenantId);
            action.run();
        } finally {
            if (previous == null) {
                TenantContext.clear();
            } else {
                TenantContext.set(previous);
            }
        }
    }

    private void sendStreamError(SseEmitter emitter) {
        try {
            sendStreamEvent(
                    emitter,
                    Map.of(
                            "type",
                            "error",
                            "message",
                            "健康助手暂时未完成回答，请稍后重试。"));
        } catch (Exception exception) {
            log.debug(
                    "Medical assistant stream error event could not be sent: {}",
                    exception.getClass().getSimpleName());
        } finally {
            emitter.complete();
        }
    }

    private String safeExceptionMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return "none";
        message = message.replaceAll("\\s+", " ").trim();
        return message.length() <= 240 ? message : message.substring(0, 240);
    }

    private void sendStreamEvent(SseEmitter emitter, Map<String, ?> payload) {
        try {
            emitter.send(SseEmitter.event().data(new LinkedHashMap<>(payload)));
        } catch (Exception exception) {
            throw new IllegalStateException("medical_assistant_stream_send_failed", exception);
        }
    }

    private record StreamContext(
            MedicalAssistantConversationEntity conversation,
            CurrentPrincipal current,
            String content,
            MembershipEntitlementService.UsageReservation usage,
            AiDtos.MedicalAssistantRequest aiRequest) {}

    private PatientEntity currentPatient() {
        List<PatientEntity> patients =
                dataScopeService.readScoped(
                        () ->
                                patientMapper.selectList(
                                        dataScopeService.scopedPatients().orderByAsc(PatientEntity::getId)));
        return patients.stream()
                .findFirst()
                .orElseThrow(
                        () -> new com.rayk.health.common.exception.BusinessException(
                                com.rayk.health.common.exception.ErrorCode.PATIENT_NOT_FOUND));
    }

    private MedicalAssistantConversationEntity ownedConversation(
            long conversationId, String conversationType) {
        CurrentPrincipal current = CurrentUser.require();
        PatientEntity patient = currentPatient();
        MedicalAssistantConversationEntity conversation =
                conversationMapper.selectOne(
                        new LambdaQueryWrapper<MedicalAssistantConversationEntity>()
                                .eq(MedicalAssistantConversationEntity::getId, conversationId)
                                .eq(MedicalAssistantConversationEntity::getTenantId, current.tenantId())
                                .eq(MedicalAssistantConversationEntity::getPatientId, patient.getId())
                                .eq(MedicalAssistantConversationEntity::getDeleted, 0)
                                .eq(MedicalAssistantConversationEntity::getConversationType, conversationType)
                                .last("LIMIT 1"));
        if (conversation == null) {
            throw new com.rayk.health.common.exception.BusinessException(
                    com.rayk.health.common.exception.ErrorCode.MEDICAL_ASSISTANT_CONVERSATION_NOT_FOUND);
        }
        return conversation;
    }

    private List<MedicalAssistantMessageEntity> loadEntities(long conversationId, int limit) {
        List<MedicalAssistantMessageEntity> messages =
                messageMapper.selectList(
                        new LambdaQueryWrapper<MedicalAssistantMessageEntity>()
                                .eq(MedicalAssistantMessageEntity::getConversationId, conversationId)
                                .eq(MedicalAssistantMessageEntity::getDeleted, 0)
                                .orderByDesc(MedicalAssistantMessageEntity::getCreatedAt)
                                .last("LIMIT " + Math.max(1, Math.min(limit, 50))));
        Collections.reverse(messages);
        return messages;
    }

    private List<MedicalAssistantMessageVo> loadMessages(long conversationId) {
        return loadEntities(conversationId, 50).stream().map(this::toMessageVo).toList();
    }

    private void insertUserMessage(
            MedicalAssistantConversationEntity conversation,
            String content,
            CurrentPrincipal current,
            LocalDateTime now) {
        MedicalAssistantMessageEntity message = baseMessage(conversation, current, now);
        message.setRole("USER");
        message.setContent(content);
        message.setRiskLevel("INFO");
        message.setEmergency(0);
        message.setCitationsJson("[]");
        message.setUsedContextJson("[]");
        message.setFollowupQuestionsJson("[]");
        messageMapper.insert(message);
    }

    private void insertAssistantMessage(
            MedicalAssistantConversationEntity conversation,
            AiDtos.MedicalAssistantData answer,
            CurrentPrincipal current,
            LocalDateTime now) {
        MedicalAssistantMessageEntity message = baseMessage(conversation, current, now);
        message.setRole("ASSISTANT");
        message.setContent(answer.reply());
        message.setRiskLevel(answer.riskLevel());
        message.setEmergency(answer.emergency() ? 1 : 0);
        message.setRecommendedAction(answer.recommendedAction());
        message.setCitationsJson(writeJson(answer.citations()));
        message.setUsedContextJson(writeJson(answer.usedContext()));
        message.setFollowupQuestionsJson(writeJson(answer.followupQuestions()));
        message.setModel(answer.model() == null ? MODEL : answer.model());
        messageMapper.insert(message);
    }

    private MedicalAssistantMessageEntity baseMessage(
            MedicalAssistantConversationEntity conversation,
            CurrentPrincipal current,
            LocalDateTime now) {
        MedicalAssistantMessageEntity message = new MedicalAssistantMessageEntity();
        message.setTenantId(current.tenantId());
        message.setPatientId(conversation.getPatientId());
        message.setConversationId(conversation.getId());
        message.setCreatedBy(current.userId());
        message.setCreatedAt(now);
        message.setUpdatedBy(current.userId());
        message.setUpdatedAt(now);
        message.setDeleted(0);
        message.setVersion(0);
        return message;
    }

    private AiDtos.PatientContext toPatientContext(
            PatientEntity patient,
            HealthProfileVo profile,
            HealthScanContextService.LatestVitals vitals) {
        Integer age =
                patient.getBirthDate() == null
                        ? null
                        : Period.between(patient.getBirthDate(), LocalDate.now()).getYears();
        String gender =
                List.of("MALE", "FEMALE").contains(patient.getGender())
                        ? patient.getGender()
                        : "UNKNOWN";
        return new AiDtos.PatientContext(
                gender,
                age,
                profile.heightCm(),
                profile.weightKg(),
                profile.waistCm(),
                profile.recentWeightChangeKg(),
                profile.bmi(),
                profile.lifestyleSummary(),
                profile.medicalHistory(),
                profile.familyHistory(),
                profile.allergyHistory(),
                profile.currentMedications(),
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
                vitals.heartRate(),
                vitals.heartRateVariability(),
                vitals.oxygenSaturation(),
                vitals.respirationRate(),
                vitals.systolicBloodPressure(),
                vitals.diastolicBloodPressure(),
                vitals.stressHrv(),
                vitals.qualityScore(),
                vitals.completedAt() == null ? null : vitals.completedAt().toString());
    }

    private HealthReportVo latestReport() {
        try {
            return workflowService.listHealthReports().stream().findFirst().orElse(null);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String reportSummary(HealthReportVo report) {
        String title = Objects.toString(report.title(), "最近一次健康报告");
        String summary = Objects.toString(report.summary(), "");
        return limit(title + "：" + summary, 2000);
    }

    private MedicalAssistantConversationVo toConversation(
            MedicalAssistantConversationEntity conversation,
            List<MedicalAssistantMessageVo> messages) {
        return new MedicalAssistantConversationVo(
                String.valueOf(conversation.getId()),
                conversation.getTitle(),
                conversation.getStatus(),
                conversation.getModel(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messages);
    }

    private MedicalAssistantMessageVo toMessageVo(MedicalAssistantMessageEntity message) {
        return new MedicalAssistantMessageVo(
                String.valueOf(message.getId()),
                message.getRole(),
                message.getContent(),
                message.getRiskLevel(),
                Integer.valueOf(1).equals(message.getEmergency()),
                message.getRecommendedAction(),
                readList(message.getCitationsJson()),
                readList(message.getUsedContextJson()),
                readList(message.getFollowupQuestionsJson()),
                message.getModel(),
                message.getCreatedAt());
    }

    private List<String> readList(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException ignored) {
            return List.of();
        }
    }

    private String writeJson(List<String> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException ignored) {
            return "[]";
        }
    }

    private void auditNew(
            MedicalAssistantConversationEntity conversation, long userId, LocalDateTime now) {
        conversation.setCreatedBy(userId);
        conversation.setCreatedAt(now);
        conversation.setUpdatedBy(userId);
        conversation.setUpdatedAt(now);
        conversation.setDeleted(0);
        conversation.setVersion(0);
    }

    private String normalizeTitle(String value, String conversationType) {
        if (value == null || value.isBlank()) {
            return TREE_HOLE_CONVERSATION_TYPE.equals(conversationType) ? "新的健康树洞" : "新的健康咨询";
        }
        return shortTitle(value.trim());
    }

    private boolean isDefaultConversationTitle(String value) {
        return "新的健康咨询".equals(value) || "新的健康树洞".equals(value);
    }

    private String aiMode(String conversationType) {
        return TREE_HOLE_CONVERSATION_TYPE.equals(conversationType)
                ? TREE_HOLE_AI_MODE
                : "MEDICAL_ASSISTANT";
    }

    private String benefitCode(String conversationType) {
        return TREE_HOLE_CONVERSATION_TYPE.equals(conversationType)
                ? TREE_HOLE_BENEFIT_CODE
                : BENEFIT_CODE;
    }

    private String shortTitle(String value) {
        return value.length() <= 20 ? value : value.substring(0, 20) + "…";
    }

    private String limit(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
