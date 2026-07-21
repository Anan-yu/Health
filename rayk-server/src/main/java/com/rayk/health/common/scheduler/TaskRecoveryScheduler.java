package com.rayk.health.common.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.assessment.entity.AiTaskEntity;
import com.rayk.health.assessment.mapper.AiTaskMapper;
import com.rayk.health.common.config.TaskProperties;
import com.rayk.health.laboratory.application.OcrTaskService.OcrTaskCreated;
import com.rayk.health.laboratory.entity.LabReportEntity;
import com.rayk.health.laboratory.entity.LabReportFileEntity;
import com.rayk.health.laboratory.mapper.LabReportFileMapper;
import com.rayk.health.laboratory.mapper.LabReportMapper;
import com.rayk.health.tenant.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 异步任务恢复调度器：定期扫描卡住的 AI 任务并执行恢复操作。
 *
 * <ul>
 *   <li>PROCESSING 超时（默认 10 分钟）：标记为 FAILED，同步更新 lab_report 状态
 *   <li>PENDING 卡住（默认 2 分钟）：重新发布事件触发重试
 * </ul>
 *
 * <p>支持 LAB_REPORT_OCR 和 HEALTH_ASSESSMENT 两种任务类型。
 */
@Component
public class TaskRecoveryScheduler {
    private static final Logger log = LoggerFactory.getLogger(TaskRecoveryScheduler.class);

    private final AiTaskMapper taskMapper;
    private final LabReportMapper reportMapper;
    private final LabReportFileMapper fileMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final TaskProperties taskProperties;

    public TaskRecoveryScheduler(
            AiTaskMapper taskMapper,
            LabReportMapper reportMapper,
            LabReportFileMapper fileMapper,
            ApplicationEventPublisher eventPublisher,
            PlatformTransactionManager transactionManager,
            TaskProperties taskProperties) {
        this.taskMapper = taskMapper;
        this.reportMapper = reportMapper;
        this.fileMapper = fileMapper;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.taskProperties = taskProperties;
    }

    @Scheduled(fixedDelayString = "${rayk.task.recovery-interval-ms:60000}")
    public void recoverStuckTasks() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime processingCutoff = now.minusMinutes(taskProperties.processingTimeoutMinutes());
        LocalDateTime pendingCutoff = now.minusMinutes(taskProperties.pendingTimeoutMinutes());

        List<AiTaskEntity> timedOut = taskMapper.selectTimedOutProcessing(processingCutoff);
        List<AiTaskEntity> stuckPending = taskMapper.selectStuckPending(pendingCutoff);

        if (!timedOut.isEmpty()) {
            log.info("任务恢复：发现 {} 个超时 PROCESSING 任务", timedOut.size());
        }
        if (!stuckPending.isEmpty()) {
            log.info("任务恢复：发现 {} 个卡住的 PENDING 任务", stuckPending.size());
        }

        for (AiTaskEntity task : timedOut) {
            handleTimedOutTask(task);
        }
        for (AiTaskEntity task : stuckPending) {
            handleStuckPendingTask(task);
        }
    }

    private void handleTimedOutTask(AiTaskEntity task) {
        try {
            TenantContext.execute(
                    task.getTenantId(),
                    () ->
                            transactionTemplate.executeWithoutResult(
                                    status -> markTimedOut(task)));
            log.info("任务恢复：任务 {} (类型={}) 已标记为超时失败", task.getId(), task.getTaskType());
        } catch (Exception exception) {
            log.error("任务恢复：处理超时任务 {} 时发生异常", task.getId(), exception);
        }
    }

    private void markTimedOut(AiTaskEntity task) {
        AiTaskEntity current = taskMapper.selectById(task.getId());
        if (current == null || !"PROCESSING".equals(current.getStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        current.setStatus("FAILED");
        current.setErrorMessage("任务执行超时");
        current.setFinishedAt(now);
        current.setUpdatedBy(current.getCreatedBy());
        current.setUpdatedAt(now);
        taskMapper.updateById(current);

        if (current.getReportId() != null) {
            LabReportEntity report = reportMapper.selectById(current.getReportId());
            if (report != null && isProcessingStatus(report.getStatus())) {
                report.setStatus(failedStatusForTaskType(current.getTaskType()));
                report.setFailureReason("任务执行超时");
                report.setUpdatedBy(current.getCreatedBy());
                report.setUpdatedAt(now);
                reportMapper.updateById(report);
            }
        }
    }

    private void handleStuckPendingTask(AiTaskEntity task) {
        try {
            if ("LAB_REPORT_OCR".equals(task.getTaskType())) {
                retryOcrTask(task);
            } else if ("HEALTH_ASSESSMENT".equals(task.getTaskType())) {
                retryAssessmentTask(task);
            } else {
                log.warn("任务恢复：任务 {} 的任务类型 {} 不支持重试，标记为失败",
                        task.getId(), task.getTaskType());
                TenantContext.execute(
                        task.getTenantId(),
                        () ->
                                transactionTemplate.executeWithoutResult(
                                        status -> markFailedUnknownType(task)));
            }
        } catch (Exception exception) {
            log.error("任务恢复：重试 PENDING 任务 {} 时发生异常", task.getId(), exception);
        }
    }

    private void retryOcrTask(AiTaskEntity task) {
        final Long[] fileIdHolder = {null};
        TenantContext.execute(
                task.getTenantId(),
                () -> fileIdHolder[0] = findLatestFileId(task.getReportId()));
        Long fileId = fileIdHolder[0];
        if (fileId == null) {
            log.warn("任务恢复：任务 {} 关联的报告 {} 无可用文件，标记为失败",
                    task.getId(), task.getReportId());
            TenantContext.execute(
                    task.getTenantId(),
                    () ->
                            transactionTemplate.executeWithoutResult(
                                    status -> markFailedNoFile(task)));
            return;
        }
        // 必须在事务内发布事件，因为 OcrTaskService.process() 使用 @TransactionalEventListener(AFTER_COMMIT)
        TenantContext.execute(
                task.getTenantId(),
                () ->
                        transactionTemplate.executeWithoutResult(
                                status ->
                                        eventPublisher.publishEvent(
                                                new OcrTaskCreated(
                                                        task.getId(), fileId, task.getTenantId()))));
        log.info("任务恢复：已重新发布 OCR 任务 {} 的处理事件", task.getId());
    }

    private void retryAssessmentTask(AiTaskEntity task) {
        // HEALTH_ASSESSMENT 任务无法通过事件重试（需要用户交互确认指标后同步调用），标记为失败
        TenantContext.execute(
                task.getTenantId(),
                () ->
                        transactionTemplate.executeWithoutResult(
                                status -> markFailedAssessment(task)));
        log.info("任务恢复：HEALTH_ASSESSMENT 任务 {} 无法自动重试，已标记为失败", task.getId());
    }

    private void markFailedNoFile(AiTaskEntity task) {
        AiTaskEntity current = taskMapper.selectById(task.getId());
        if (current == null || !"PENDING".equals(current.getStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        current.setStatus("FAILED");
        current.setErrorMessage("报告文件不存在，无法重试");
        current.setFinishedAt(now);
        current.setUpdatedBy(current.getCreatedBy());
        current.setUpdatedAt(now);
        taskMapper.updateById(current);

        if (current.getReportId() != null) {
            LabReportEntity report = reportMapper.selectById(current.getReportId());
            if (report != null && "OCR_PENDING".equals(report.getStatus())) {
                report.setStatus("OCR_FAILED");
                report.setFailureReason("报告文件不存在，无法重试");
                report.setUpdatedBy(current.getCreatedBy());
                report.setUpdatedAt(now);
                reportMapper.updateById(report);
            }
        }
    }

    private void markFailedAssessment(AiTaskEntity task) {
        AiTaskEntity current = taskMapper.selectById(task.getId());
        if (current == null || !"PENDING".equals(current.getStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        current.setStatus("FAILED");
        current.setErrorMessage("任务调度异常，请重新提交评估");
        current.setFinishedAt(now);
        current.setUpdatedBy(current.getCreatedBy());
        current.setUpdatedAt(now);
        taskMapper.updateById(current);

        if (current.getReportId() != null) {
            LabReportEntity report = reportMapper.selectById(current.getReportId());
            if (report != null && "AI_PROCESSING".equals(report.getStatus())) {
                report.setStatus("FAILED");
                report.setFailureReason("任务调度异常，请重新提交评估");
                report.setUpdatedBy(current.getCreatedBy());
                report.setUpdatedAt(now);
                reportMapper.updateById(report);
            }
        }
    }

    private void markFailedUnknownType(AiTaskEntity task) {
        AiTaskEntity current = taskMapper.selectById(task.getId());
        if (current == null || !"PENDING".equals(current.getStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        current.setStatus("FAILED");
        current.setErrorMessage("未知任务类型，无法自动恢复");
        current.setFinishedAt(now);
        current.setUpdatedBy(current.getCreatedBy());
        current.setUpdatedAt(now);
        taskMapper.updateById(current);
    }

    private Long findLatestFileId(long reportId) {
        LabReportFileEntity file =
                fileMapper.selectOne(
                        new LambdaQueryWrapper<LabReportFileEntity>()
                                .eq(LabReportFileEntity::getReportId, reportId)
                                .eq(LabReportFileEntity::getStatus, "STORED")
                                .eq(LabReportFileEntity::getDeleted, 0)
                                .orderByDesc(LabReportFileEntity::getCreatedAt)
                                .last("LIMIT 1"));
        return file == null ? null : file.getId();
    }

    private boolean isProcessingStatus(String reportStatus) {
        return "OCR_PROCESSING".equals(reportStatus) || "AI_PROCESSING".equals(reportStatus);
    }

    private String failedStatusForTaskType(String taskType) {
        if ("HEALTH_ASSESSMENT".equals(taskType)) {
            return "FAILED";
        }
        return "OCR_FAILED";
    }
}
