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
import com.rayk.health.assistant.mapper.MedicalAssistantConversationMapper;
import com.rayk.health.assistant.mapper.MedicalAssistantMessageMapper;
import com.rayk.health.assistant.vo.MedicalAssistantConversationVo;
import com.rayk.health.assistant.vo.MedicalAssistantMessageVo;
import com.rayk.health.assessment.application.WorkflowApplicationService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private static final String MODEL = "qwen3.7-flash-2026-07-15";
    private static final String BENEFIT_CODE = "AI_MEDICAL_ASSISTANT";
    private static final String BIZ_TYPE = "MEDICAL_ASSISTANT";
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
            ObjectMapper objectMapper) {
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
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    @Transactional
    public MedicalAssistantConversationVo createConversation(
            CreateMedicalAssistantConversationRequest request) {
        CurrentPrincipal current = CurrentUser.require();
        PatientEntity patient = currentPatient();
        LocalDateTime now = LocalDateTime.now();
        MedicalAssistantConversationEntity conversation = new MedicalAssistantConversationEntity();
        conversation.setTenantId(current.tenantId());
        conversation.setPatientId(patient.getId());
        conversation.setTitle(normalizeTitle(request == null ? null : request.title()));
        conversation.setStatus("ACTIVE");
        conversation.setModel(MODEL);
        auditNew(conversation, current.userId(), now);
        conversationMapper.insert(conversation);
        return toConversation(conversation, List.of());
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public List<MedicalAssistantConversationVo> listConversations() {
        CurrentPrincipal current = CurrentUser.require();
        PatientEntity patient = currentPatient();
        return conversationMapper
                .selectList(
                        new LambdaQueryWrapper<MedicalAssistantConversationEntity>()
                                .eq(MedicalAssistantConversationEntity::getTenantId, current.tenantId())
                                .eq(MedicalAssistantConversationEntity::getPatientId, patient.getId())
                                .eq(MedicalAssistantConversationEntity::getDeleted, 0)
                                .orderByDesc(MedicalAssistantConversationEntity::getUpdatedAt)
                                .last("LIMIT 50"))
                .stream()
                .map(item -> toConversation(item, List.of()))
                .toList();
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public MedicalAssistantConversationVo getConversation(long conversationId) {
        MedicalAssistantConversationEntity conversation = ownedConversation(conversationId);
        return toConversation(conversation, loadMessages(conversationId));
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    @Transactional
    public void deleteConversation(long conversationId) {
        CurrentPrincipal current = CurrentUser.require();
        MedicalAssistantConversationEntity conversation = ownedConversation(conversationId);
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
        MedicalAssistantConversationEntity conversation = ownedConversation(conversationId);
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
                        BENEFIT_CODE, BIZ_TYPE, String.valueOf(conversationId), usageKey);
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
                                    false));
            insertAssistantMessage(conversation, answer, current, now);
            conversation.setUpdatedBy(current.userId());
            conversation.setUpdatedAt(LocalDateTime.now());
            if ("新的健康咨询".equals(conversation.getTitle())) {
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
        StreamContext context = prepareStream(conversationId, request);
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

    private StreamContext prepareStream(
            long conversationId, SendMedicalAssistantMessageRequest request) {
        MedicalAssistantConversationEntity conversation = ownedConversation(conversationId);
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
                        BENEFIT_CODE, BIZ_TYPE, String.valueOf(conversationId), usageKey);
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
                            false);
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
        if ("新的健康咨询".equals(context.conversation().getTitle())) {
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

    private MedicalAssistantConversationEntity ownedConversation(long conversationId) {
        CurrentPrincipal current = CurrentUser.require();
        PatientEntity patient = currentPatient();
        MedicalAssistantConversationEntity conversation =
                conversationMapper.selectOne(
                        new LambdaQueryWrapper<MedicalAssistantConversationEntity>()
                                .eq(MedicalAssistantConversationEntity::getId, conversationId)
                                .eq(MedicalAssistantConversationEntity::getTenantId, current.tenantId())
                                .eq(MedicalAssistantConversationEntity::getPatientId, patient.getId())
                                .eq(MedicalAssistantConversationEntity::getDeleted, 0)
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

    private String normalizeTitle(String value) {
        return value == null || value.isBlank() ? "新的健康咨询" : shortTitle(value.trim());
    }

    private String shortTitle(String value) {
        return value.length() <= 20 ? value : value.substring(0, 20) + "…";
    }

    private String limit(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
