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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OcrTaskService {
    private static final String TASK_TYPE = "LAB_REPORT_OCR";
    private static final Set<String> ACTIVE_STATUSES = Set.of("PENDING", "PROCESSING");
    private static final Logger log = LoggerFactory.getLogger(OcrTaskService.class);
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
    private final Executor ocrFileExecutor;

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
            TaskIdempotencyGuard idempotencyGuard,
            @Qualifier("ocrFileExecutor") Executor ocrFileExecutor) {
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
        this.ocrFileExecutor = ocrFileExecutor;
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
        if (Set.of("CONFIRMED", "AI_PROCESSING", "AI_FAILED", "REVIEWING", "PUBLISHED")
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
                    try {
                        AiDtos.OcrRecognizeData result = recognizeFiles(task, context.files());
                        if (!isUsableOcrResult(result)) {
                            transactionTemplate.executeWithoutResult(
                                    status -> markFailed(task, report, ocrFailureReason(result)));
                            submitImageAssessmentAfterOcrFailure(report, context.files(), event.tenantId());
                            return;
                        }
                        transactionTemplate.executeWithoutResult(
                                status -> {
                                    try {
                                        applyDraftIndicators(report, task, result);
                                    } catch (Exception exception) {
                                        throw new OcrPersistenceException(exception);
                                    }
                                });
                        updateReportProgress(
                                report.getId(),
                                task.getCreatedBy(),
                                78,
                                "识别完成，正在生成健康评估");
                        try {
                            workflowApplicationService.submitAiAutomatically(
                                    report.getId(), event.tenantId());
                        } catch (RuntimeException exception) {
                            // submitAiAutomatically records the assessment failure on the report.
                            // OCR has succeeded and must not be retried solely because AI is unavailable.
                            log.warn(
                                    "Automatic health assessment failed: reportId={} ocrTaskId={} exceptionType={} errorCode={}",
                                    report.getId(),
                                    task.getId(),
                                    exception.getClass().getSimpleName(),
                                    errorCode(exception));
                        }
                    } catch (Exception exception) {
                        transactionTemplate.executeWithoutResult(
                                status -> markFailed(task, report, "识别服务调用失败，请稍后重试"));
                        submitImageAssessmentAfterOcrFailure(report, context.files(), event.tenantId());
                    }
                });
    }

    @Async("ocrTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void assessImageReport(ImageReportSubmitted event) {
        TenantContext.execute(
                event.tenantId(),
                () -> {
                    try {
                        workflowApplicationService.submitAiAutomatically(
                                event.reportId(), event.tenantId());
                    } catch (RuntimeException exception) {
                        log.warn(
                                "Direct image health assessment failed after upload: reportId={} exceptionType={}",
                                event.reportId(),
                                exception.getClass().getSimpleName());
                    }
                });
    }

    private void submitImageAssessmentAfterOcrFailure(
            LabReportEntity report, List<LabReportFileEntity> files, long tenantId) {
        if (files.stream().noneMatch(this::isImageFile)) {
            return;
        }
        try {
            workflowApplicationService.submitAiAutomatically(report.getId(), tenantId);
        } catch (RuntimeException exception) {
            log.warn(
                    "Direct image health assessment failed after OCR failure: reportId={} exceptionType={}",
                    report.getId(),
                    exception.getClass().getSimpleName());
        }
    }

    private boolean isImageFile(LabReportFileEntity file) {
        return file != null
                && file.getMimeType() != null
                && file.getMimeType().toLowerCase(java.util.Locale.ROOT).startsWith("image/");
    }

    private String errorCode(RuntimeException exception) {
        if (exception instanceof BusinessException businessException) {
            return businessException.getErrorCode().name();
        }
        return "UNEXPECTED";
    }

    private boolean isUsableOcrResult(AiDtos.OcrRecognizeData result) {
        boolean hasStructuredContent =
                result != null
                        && ((result.indicators() != null && !result.indicators().isEmpty())
                                || (result.findings() != null && !result.findings().isEmpty()));
        return result != null
                && !"RETRY_REQUIRED".equals(result.status())
                && hasStructuredContent
                && result.confidence() != null
                && result.confidence().compareTo(new BigDecimal("0.55")) >= 0;
    }

    private String ocrFailureReason(AiDtos.OcrRecognizeData result) {
        if (result != null && result.warnings() != null) {
            return result.warnings().stream()
                    .filter(message -> message != null && !message.isBlank())
                    .reduce((first, second) -> second)
                    .orElse("报告识别可靠性不足，请使用清晰、完整、正向拍摄的报告重新上传");
        }
        return "报告识别可靠性不足，请使用清晰、完整、正向拍摄的报告重新上传";
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
        if (report == null) {
            markFailed(task, null, "报告文件不存在");
            return null;
        }
        List<LabReportFileEntity> files = storedFiles(report.getId());
        if (files.isEmpty()
                || files.stream().noneMatch(file -> file.getId().equals(fileId))) {
            markFailed(task, report, "报告文件不存在");
            return null;
        }
        report.setStatus("OCR_PROCESSING");
        report.setProcessingProgress(8);
        report.setProcessingMessage("正在准备识别文件");
        report.setFailureReason(null);
        touch(report, task.getCreatedBy());
        reportMapper.updateById(report);
        return new ProcessingContext(task, report, files);
    }

    /**
     * Recognizes every stored page in one report and merges the structured OCR
     * snapshots before indicators are persisted.  PDF uploads remain a single
     * request; photographed multi-page reports simply contribute one request
     * per image.
     */
    private AiDtos.OcrRecognizeData recognizeFiles(
            AiTaskEntity task, List<LabReportFileEntity> files) {
        List<AiDtos.Indicator> indicators = new ArrayList<>();
        List<AiDtos.OcrFinding> findings = new ArrayList<>();
        List<String> rawLines = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> engines = new ArrayList<>();
        BigDecimal confidenceTotal = BigDecimal.ZERO;
        int successfulFiles = 0;
        AtomicInteger completedFiles = new AtomicInteger();
        updateReportProgress(task.getReportId(), task.getCreatedBy(), 12, "正在读取报告内容");
        List<CompletableFuture<FileOcrResult>> requests =
                files.stream()
                        .map(
                                file -> {
                                    CompletableFuture<FileOcrResult> request =
                                            CompletableFuture.supplyAsync(
                                                    () -> recognizeFile(task, file), ocrFileExecutor);
                                    request.whenComplete(
                                            (ignored, failure) -> {
                                                int completed = completedFiles.incrementAndGet();
                                                int progress =
                                                        12
                                                                + (int)
                                                                        Math.round(
                                                                                completed
                                                                                        * 60.0
                                                                                        / Math.max(1, files.size()));
                                                try {
                                                    updateReportProgress(
                                                            task.getReportId(),
                                                            task.getCreatedBy(),
                                                            progress,
                                                            completed >= files.size()
                                                                    ? "报告识别完成，正在整理结果"
                                                                    : "正在读取报告内容");
                                                } catch (RuntimeException exception) {
                                                    log.warn(
                                                            "OCR progress update skipped: reportId={} errorType={}",
                                                            task.getReportId(),
                                                            exception.getClass().getSimpleName());
                                                }
                                            });
                                    return request;
                                })
                        .toList();
        for (CompletableFuture<FileOcrResult> request : requests) {
            FileOcrResult page;
            try {
                page = request.join();
            } catch (RuntimeException exception) {
                warnings.add("部分页面识别失败，请检查图片清晰度后重试");
                continue;
            }
            if (page.warning() != null) {
                warnings.add(page.warning());
                continue;
            }
            AiDtos.OcrRecognizeData result = page.result();
            if (!isUsableOcrResult(result)) {
                if (result != null && result.warnings() != null) {
                    warnings.addAll(result.warnings());
                }
                continue;
            }
            successfulFiles++;
            confidenceTotal = confidenceTotal.add(result.confidence());
            if (result.engine() != null && !result.engine().isBlank()) {
                engines.add(result.engine());
            }
            if (result.indicators() != null) {
                indicators.addAll(result.indicators());
            }
            if (result.findings() != null) {
                findings.addAll(result.findings());
            }
            if (result.rawLines() != null) {
                rawLines.addAll(result.rawLines());
            }
            if (result.warnings() != null) {
                warnings.addAll(result.warnings());
            }
        }
        if (successfulFiles == 0 || (indicators.isEmpty() && findings.isEmpty())) {
            return new AiDtos.OcrRecognizeData(
                    "MULTI_FILE", "RETRY_REQUIRED", BigDecimal.ZERO, List.of(), findings, rawLines, warnings);
        }
        return new AiDtos.OcrRecognizeData(
                mergeEngineVersions(engines),
                "SUCCESS",
                confidenceTotal.divide(BigDecimal.valueOf(successfulFiles), 4, java.math.RoundingMode.HALF_UP),
                indicators,
                findings,
                rawLines,
                warnings);
    }

    private FileOcrResult recognizeFile(AiTaskEntity task, LabReportFileEntity file) {
        try {
            String downloadUrl =
                    minioClient.getPresignedObjectUrl(
                            GetPresignedObjectUrlArgs.builder()
                                    .method(Method.GET)
                                    .bucket(file.getBucketName())
                                    .object(file.getObjectPath())
                                    .expiry(600)
                                    .build());
            return new FileOcrResult(
                    file,
                    aiServiceClient.recognize(
                            new AiDtos.OcrRecognizeRequest(
                                    task.getTaskCode(),
                                    String.valueOf(file.getId()),
                                    file.getOriginalName(),
                                    file.getMimeType(),
                                    downloadUrl)),
                    null);
        } catch (Exception exception) {
            return new FileOcrResult(file, null, "部分页面识别失败，请检查图片清晰度后重试");
        }
    }

    /** Keeps the audit field within ai_task.engine_version VARCHAR(80). */
    private String mergeEngineVersions(List<String> engines) {
        if (engines == null || engines.isEmpty()) {
            return "MULTI_FILE";
        }
        String merged =
                engines.stream()
                        .filter(engine -> engine != null && !engine.isBlank())
                        .distinct()
                        .collect(Collectors.joining("+"));
        if (merged.isBlank()) {
            return "MULTI_FILE";
        }
        return merged.length() <= 80 ? merged : merged.substring(0, 77) + "...";
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
        report.setProcessingProgress(0);
        report.setProcessingMessage("等待进入识别队列");
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
                        // A retry is a complete replacement of the OCR snapshot. The current
                        // automatic workflow marks recognized rows as confirmed immediately, so
                        // filtering on manuallyConfirmed would retain every prior attempt and
                        // duplicate the report indicators.
                        .eq(IndicatorValueEntity::getDeleted, 0)
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
            report.setProcessingMessage("识别失败，可重新上传清晰文件");
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

    private List<LabReportFileEntity> storedFiles(long reportId) {
        return fileMapper.selectList(
                new LambdaQueryWrapper<LabReportFileEntity>()
                        .eq(LabReportFileEntity::getReportId, reportId)
                        .eq(LabReportFileEntity::getStatus, "STORED")
                        .eq(LabReportFileEntity::getDeleted, 0)
                        .orderByAsc(LabReportFileEntity::getCreatedAt));
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

    private void updateReportProgress(long reportId, long userId, int progress, String message) {
        reportMapper.update(
                null,
                new LambdaUpdateWrapper<LabReportEntity>()
                        .eq(LabReportEntity::getId, reportId)
                        .eq(LabReportEntity::getDeleted, 0)
                        .set(
                                LabReportEntity::getProcessingProgress,
                                Math.max(0, Math.min(100, progress)))
                        .set(LabReportEntity::getProcessingMessage, message)
                        .set(LabReportEntity::getUpdatedBy, userId)
                        .set(LabReportEntity::getUpdatedAt, LocalDateTime.now()));
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

    /** Image-only reports bypass OCR and start the qwen direct-read assessment directly. */
    public record ImageReportSubmitted(long reportId, long tenantId) {}

    private record ProcessingContext(
            AiTaskEntity task, LabReportEntity report, List<LabReportFileEntity> files) {}

    private record FileOcrResult(
            LabReportFileEntity file, AiDtos.OcrRecognizeData result, String warning) {}

    private static final class OcrPersistenceException extends RuntimeException {
        private OcrPersistenceException(Throwable cause) {
            super(cause);
        }
    }
}
