package com.rayk.health.laboratory.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.assessment.entity.AiTaskEntity;
import com.rayk.health.assessment.application.WorkflowApplicationService;
import com.rayk.health.assessment.mapper.AiTaskMapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.common.util.TaskIdempotencyGuard;
import com.rayk.health.indicator.entity.IndicatorValueEntity;
import com.rayk.health.indicator.mapper.IndicatorValueMapper;
import com.rayk.health.integration.ai.AiDtos;
import com.rayk.health.integration.ai.AiServiceClient;
import com.rayk.health.laboratory.entity.LabReportEntity;
import com.rayk.health.laboratory.entity.LabReportFileEntity;
import com.rayk.health.laboratory.mapper.LabReportFileMapper;
import com.rayk.health.laboratory.mapper.LabReportMapper;
import com.rayk.health.laboratory.vo.OcrTaskVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.tenant.TenantContext;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OcrTaskService {
    private static final String TASK_TYPE = "LAB_REPORT_OCR";
    private static final Set<String> ACTIVE_STATUSES = Set.of("PENDING", "PROCESSING");
    private final AiTaskMapper taskMapper;
    private final WorkflowApplicationService workflowApplicationService;
    private final LabReportMapper reportMapper;
    private final LabReportFileMapper fileMapper;
    private final IndicatorValueMapper indicatorMapper;
    private final AiServiceClient aiServiceClient;
    private final MinioClient minioClient;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final TaskIdempotencyGuard idempotencyGuard;

    public OcrTaskService(
            AiTaskMapper taskMapper,
            WorkflowApplicationService workflowApplicationService,
            LabReportMapper reportMapper,
            LabReportFileMapper fileMapper,
            IndicatorValueMapper indicatorMapper,
            AiServiceClient aiServiceClient,
            MinioClient minioClient,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            PlatformTransactionManager transactionManager,
            TaskIdempotencyGuard idempotencyGuard) {
        this.taskMapper = taskMapper;
        this.workflowApplicationService = workflowApplicationService;
        this.reportMapper = reportMapper;
        this.fileMapper = fileMapper;
        this.indicatorMapper = indicatorMapper;
        this.aiServiceClient = aiServiceClient;
        this.minioClient = minioClient;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.idempotencyGuard = idempotencyGuard;
    }

    @Transactional
    public OcrTaskVo start(long reportId, long fileId) {
        CurrentPrincipal current = CurrentUser.require();
        AiTaskEntity active = latestEntity(reportId);
        if (active != null && ACTIVE_STATUSES.contains(active.getStatus())) {
            return toVo(active, fileId);
        }
        return createTask(reportId, fileId, current, attemptCount(reportId) + 1);
    }

    public OcrTaskVo latest(long reportId) {
        AiTaskEntity task = latestEntity(reportId);
        if (task == null) {
            throw new BusinessException(ErrorCode.OCR_TASK_NOT_FOUND);
        }
        LabReportFileEntity file = latestFile(reportId);
        return toVo(task, file == null ? null : file.getId());
    }

    @Transactional
    @PreAuthorize("hasAuthority('self:lab-report') and principal.workbench == 'CUSTOMER'")
    public OcrTaskVo retry(long reportId) {
        AiTaskEntity active = latestEntity(reportId);
        if (active != null && ACTIVE_STATUSES.contains(active.getStatus())) {
            throw new BusinessException(ErrorCode.OCR_TASK_PROCESSING);
        }
        LabReportEntity report = requireReport(reportId);
        if (Set.of("CONFIRMED", "AI_PROCESSING", "REVIEWING", "PUBLISHED")
                .contains(report.getStatus())) {
            throw new BusinessException(ErrorCode.LAB_REPORT_INVALID_STATUS);
        }
        LabReportFileEntity file = latestFile(reportId);
        if (file == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        return createTask(
                reportId,
                file.getId(),
                CurrentUser.require(),
                Math.max(1, attemptCount(reportId) + 1));
    }

    @Async("ocrTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void process(OcrTaskCreated event) {
        TenantContext.execute(
                event.tenantId(),
                () -> {
                    ProcessingContext context =
                            transactionTemplate.execute(
                                    status -> prepare(event.taskId(), event.fileId()));
                    if (context == null) {
                        return;
                    }
                    AiTaskEntity task = context.task();
                    LabReportEntity report = context.report();
                    LabReportFileEntity file = context.file();
                    try {
                        String downloadUrl =
                                minioClient.getPresignedObjectUrl(
                                        GetPresignedObjectUrlArgs.builder()
                                                .method(Method.GET)
                                                .bucket(file.getBucketName())
                                                .object(file.getObjectPath())
                                                .expiry(600)
                                                .build());
                        AiDtos.OcrRecognizeData result =
                                aiServiceClient.recognize(
                                        new AiDtos.OcrRecognizeRequest(
                                                task.getTaskCode(),
                                                String.valueOf(file.getId()),
                                                file.getOriginalName(),
                                                file.getMimeType(),
                                                downloadUrl));
                        transactionTemplate.executeWithoutResult(
                                status -> {
                                    try {
                                        applyDraftIndicators(report, task, result);
                                    } catch (Exception exception) {
                                        throw new OcrPersistenceException(exception);
                                    }
                                });
                        try {
                            workflowApplicationService.submitAiAutomatically(
                                    report.getId(), event.tenantId());
                        } catch (RuntimeException ignored) {
                            // submitAiAutomatically records the assessment failure on the report.
                            // OCR has succeeded and must not be retried solely because AI is unavailable.
                        }
                    } catch (Exception exception) {
                        transactionTemplate.executeWithoutResult(
                                status -> markFailed(task, report, "识别服务调用失败，请稍后重试"));
                    }
                });
    }

    private ProcessingContext prepare(long taskId, long fileId) {
        if (!idempotencyGuard.tryAcquire(taskId, "PENDING", "PROCESSING")) {
            return null;
        }
        AiTaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            return null;
        }
        LabReportEntity report = reportMapper.selectById(task.getReportId());
        LabReportFileEntity file = fileMapper.selectById(fileId);
        if (report == null || file == null || !file.getReportId().equals(report.getId())) {
            markFailed(task, report, "报告文件不存在");
            return null;
        }
        report.setStatus("OCR_PROCESSING");
        report.setFailureReason(null);
        touch(report, task.getCreatedBy());
        reportMapper.updateById(report);
        return new ProcessingContext(task, report, file);
    }

    private OcrTaskVo createTask(
            long reportId, long fileId, CurrentPrincipal current, int attemptCount) {
        LabReportEntity report = requireReport(reportId);
        LabReportFileEntity file = fileMapper.selectById(fileId);
        if (file == null
                || file.getReportId() != reportId
                || !"STORED".equals(file.getStatus())) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        AiTaskEntity task = new AiTaskEntity();
        LocalDateTime now = LocalDateTime.now();
        task.setTenantId(current.tenantId());
        task.setReportId(reportId);
        task.setPatientId(report.getPatientId());
        task.setTaskCode("OCR_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        task.setTaskType(TASK_TYPE);
        task.setStatus("PENDING");
        task.setAttemptCount(attemptCount);
        task.setCreatedBy(current.userId());
        task.setCreatedAt(now);
        task.setUpdatedBy(current.userId());
        task.setUpdatedAt(now);
        task.setDeleted(0);
        task.setVersion(0);
        taskMapper.insert(task);
        report.setStatus("OCR_PENDING");
        report.setFailureReason(null);
        touch(report, current.userId());
        reportMapper.updateById(report);
        eventPublisher.publishEvent(new OcrTaskCreated(task.getId(), fileId, current.tenantId()));
        return toVo(task, fileId);
    }

    private void applyDraftIndicators(
            LabReportEntity report, AiTaskEntity task, AiDtos.OcrRecognizeData result)
            throws Exception {
        indicatorMapper.update(
                null,
                new LambdaUpdateWrapper<IndicatorValueEntity>()
                        .eq(IndicatorValueEntity::getReportId, report.getId())
                        .eq(IndicatorValueEntity::getManuallyConfirmed, 0)
                        .set(IndicatorValueEntity::getDeleted, 1));
        int index = 0;
        for (AiDtos.Indicator item : result.indicators()) {
            index++;
            IndicatorValueEntity entity = new IndicatorValueEntity();
            entity.setTenantId(report.getTenantId());
            entity.setReportId(report.getId());
            entity.setPatientId(report.getPatientId());
            entity.setIndicatorCode(
                    item.code() == null || item.code().isBlank()
                            ? "unrecognized_" + index
                            : item.code());
            entity.setIndicatorName(item.name());
            entity.setValue(item.value());
            entity.setUnit(item.unit());
            entity.setReferenceLow(item.referenceLow());
            entity.setReferenceHigh(item.referenceHigh());
            entity.setAbnormalFlag(abnormal(item));
            entity.setManuallyConfirmed(1);
            auditNew(entity, task.getCreatedBy());
            indicatorMapper.insert(entity);
        }
        String snapshot = objectMapper.writeValueAsString(result);
        report.setOcrSnapshot(snapshot);
        report.setStatus("CONFIRMED");
        report.setFailureReason(null);
        touch(report, task.getCreatedBy());
        reportMapper.updateById(report);

        task.setStatus("SUCCESS");
        task.setEngineVersion(result.engine());
        task.setConfidence(result.confidence());
        task.setResultSnapshot(snapshot);
        task.setErrorMessage(null);
        task.setFinishedAt(LocalDateTime.now());
        touch(task, task.getCreatedBy());
        taskMapper.updateById(task);
    }

    private void markFailed(AiTaskEntity task, LabReportEntity report, String reason) {
        LocalDateTime now = LocalDateTime.now();
        task.setStatus("FAILED");
        task.setErrorMessage(reason);
        task.setFinishedAt(now);
        task.setUpdatedBy(task.getCreatedBy());
        task.setUpdatedAt(now);
        taskMapper.updateById(task);
        if (report != null) {
            report.setStatus("OCR_FAILED");
            report.setFailureReason(reason);
            touch(report, task.getCreatedBy());
            reportMapper.updateById(report);
        }
    }

    private String abnormal(AiDtos.Indicator input) {
        BigDecimal value = input.value();
        if (input.referenceLow() != null && value.compareTo(input.referenceLow()) < 0) {
            return "LOW";
        }
        if (input.referenceHigh() != null && value.compareTo(input.referenceHigh()) > 0) {
            return "HIGH";
        }
        return "NORMAL";
    }

    private AiTaskEntity latestEntity(long reportId) {
        return taskMapper.selectOne(
                new LambdaQueryWrapper<AiTaskEntity>()
                        .eq(AiTaskEntity::getReportId, reportId)
                        .eq(AiTaskEntity::getTaskType, TASK_TYPE)
                        .eq(AiTaskEntity::getDeleted, 0)
                        .orderByDesc(AiTaskEntity::getCreatedAt)
                        .last("LIMIT 1"));
    }

    private LabReportFileEntity latestFile(long reportId) {
        return fileMapper.selectOne(
                new LambdaQueryWrapper<LabReportFileEntity>()
                        .eq(LabReportFileEntity::getReportId, reportId)
                        .eq(LabReportFileEntity::getStatus, "STORED")
                        .eq(LabReportFileEntity::getDeleted, 0)
                        .orderByDesc(LabReportFileEntity::getCreatedAt)
                        .last("LIMIT 1"));
    }

    private int attemptCount(long reportId) {
        return Math.toIntExact(
                taskMapper.selectCount(
                        new LambdaQueryWrapper<AiTaskEntity>()
                                .eq(AiTaskEntity::getReportId, reportId)
                                .eq(AiTaskEntity::getTaskType, TASK_TYPE)
                                .eq(AiTaskEntity::getDeleted, 0)));
    }

    private LabReportEntity requireReport(long reportId) {
        LabReportEntity report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.LAB_REPORT_NOT_FOUND);
        }
        return report;
    }

    private OcrTaskVo toVo(AiTaskEntity task, Long fileId) {
        List<String> warnings = new ArrayList<>();
        int indicatorCount = 0;
        if (task.getResultSnapshot() != null) {
            try {
                JsonNode root = objectMapper.readTree(task.getResultSnapshot());
                JsonNode warningNode = root.path("warnings");
                if (warningNode.isArray()) {
                    warningNode.forEach(item -> warnings.add(item.asText()));
                }
                indicatorCount = root.path("indicators").size();
            } catch (Exception ignored) {
                warnings.add("识别结果快照暂时无法解析");
            }
        }
        return new OcrTaskVo(
                String.valueOf(task.getId()),
                String.valueOf(task.getReportId()),
                fileId == null ? null : String.valueOf(fileId),
                task.getTaskCode(),
                task.getStatus(),
                task.getEngineVersion(),
                task.getConfidence(),
                task.getAttemptCount() == null ? 1 : task.getAttemptCount(),
                indicatorCount,
                warnings,
                task.getErrorMessage(),
                task.getStartedAt(),
                task.getFinishedAt(),
                task.getCreatedAt());
    }

    private void auditNew(Object entity, long userId) {
        LocalDateTime now = LocalDateTime.now();
        if (entity instanceof IndicatorValueEntity value) {
            value.setCreatedBy(userId);
            value.setCreatedAt(now);
            value.setUpdatedBy(userId);
            value.setUpdatedAt(now);
            value.setDeleted(0);
            value.setVersion(0);
        }
    }

    private void touch(Object entity, long userId) {
        if (entity instanceof AiTaskEntity task) {
            task.setUpdatedBy(userId);
            task.setUpdatedAt(LocalDateTime.now());
        } else if (entity instanceof LabReportEntity report) {
            report.setUpdatedBy(userId);
            report.setUpdatedAt(LocalDateTime.now());
        }
    }

    public record OcrTaskCreated(long taskId, long fileId, long tenantId) {}

    private record ProcessingContext(
            AiTaskEntity task, LabReportEntity report, LabReportFileEntity file) {}

    private static final class OcrPersistenceException extends RuntimeException {
        private OcrPersistenceException(Throwable cause) {
            super(cause);
        }
    }
}
