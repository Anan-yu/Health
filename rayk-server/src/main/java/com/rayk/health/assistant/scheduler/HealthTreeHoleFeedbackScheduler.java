package com.rayk.health.assistant.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rayk.health.assistant.application.MedicalAssistantApplicationService;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.patient.mapper.PatientMapper;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.tenant.TenantContext;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Generates due seven-day tree-hole summaries without requiring the customer to open the page. */
@Component
public class HealthTreeHoleFeedbackScheduler {
    private static final Logger log = LoggerFactory.getLogger(HealthTreeHoleFeedbackScheduler.class);
    private static final String TREE_HOLE_CONVERSATION_TYPE = "HEALTH_TREE_HOLE";

    private final PatientMapper patientMapper;
    private final MedicalAssistantApplicationService assistantService;

    public HealthTreeHoleFeedbackScheduler(
            PatientMapper patientMapper,
            MedicalAssistantApplicationService assistantService) {
        this.patientMapper = patientMapper;
        this.assistantService = assistantService;
    }

    @Scheduled(
            cron = "${rayk.health-tree-hole.feedback-cron:0 15 2 * * *}",
            zone = "Asia/Shanghai")
    public void generateDueFeedback() {
        List<PatientEntity> candidates = findCandidates();
        int generated = 0;
        int failed = 0;
        for (PatientEntity candidate : candidates) {
            if (candidate.getTenantId() == null || candidate.getUserId() == null) continue;
            try {
                boolean[] generatedForPatient = {false};
                TenantContext.execute(
                        candidate.getTenantId(),
                        () ->
                                runAsCustomer(
                                        candidate,
                                        () ->
                                                generatedForPatient[0] =
                                                        assistantService.generateTreeHoleFeedbackIfDue(
                                                                candidate.getId())));
                if (generatedForPatient[0]) generated++;
            } catch (BusinessException exception) {
                // Expired free trials are expected and should not fill the scheduler logs.
                if (exception.getErrorCode()
                        != ErrorCode.HEALTH_TREE_HOLE_TRIAL_EXPIRED) {
                    failed++;
                    log.warn(
                            "健康树洞七天反馈生成跳过：tenantId={}, patientId={}, errorCode={}",
                            candidate.getTenantId(),
                            candidate.getId(),
                            exception.getErrorCode().code());
                }
            } catch (RuntimeException exception) {
                failed++;
                log.error(
                        "健康树洞七天反馈生成失败：tenantId={}, patientId={}, exceptionType={}",
                        candidate.getTenantId(),
                        candidate.getId(),
                        exception.getClass().getSimpleName());
            }
        }
        if (!candidates.isEmpty()) {
            log.info(
                    "健康树洞七天反馈定时任务完成：扫描={}，生成={}，失败={}",
                    candidates.size(),
                    generated,
                    failed);
        }
    }

    private List<PatientEntity> findCandidates() {
        return TenantContext.executeReadWithoutTenant(
                () ->
                        patientMapper.selectList(
                                new QueryWrapper<PatientEntity>()
                                        .select("id", "tenant_id", "user_id")
                                        .eq("deleted", 0)
                                        .inSql(
                                                "id",
                                                "SELECT DISTINCT patient_id FROM medical_assistant_conversation "
                                                        + "WHERE conversation_type = '"
                                                        + TREE_HOLE_CONVERSATION_TYPE
                                                        + "' AND deleted = 0")
                                        .orderByAsc("id")));
    }

    private void runAsCustomer(PatientEntity patient, Runnable action) {
        SecurityContext previousContext = SecurityContextHolder.getContext();
        SecurityContext schedulerContext = SecurityContextHolder.createEmptyContext();
        CurrentPrincipal principal =
                new CurrentPrincipal(
                        "health-tree-hole-scheduler",
                        "health-tree-hole-scheduler",
                        patient.getUserId(),
                        patient.getTenantId(),
                        List.of("CUSTOMER"),
                        List.of("self:health-record"),
                        "CUSTOMER");
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("self:health-record")));
        schedulerContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(schedulerContext);
        try {
            action.run();
        } finally {
            SecurityContextHolder.setContext(previousContext);
        }
    }
}
