package com.rayk.health.common.util;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rayk.health.assessment.entity.AiTaskEntity;
import com.rayk.health.assessment.mapper.AiTaskMapper;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 数据库级幂等保护：通过乐观锁（version + status 双重条件）防止同一任务被多线程/多实例重复处理。
 *
 * <p>核心语义：UPDATE ai_task SET status=:newStatus, version=version+1 WHERE id=:taskId AND
 * status=:expectedStatus AND version=:currentVersion。仅当恰好 1 行被更新时视为获取成功。
 */
@Component
public class TaskIdempotencyGuard {
    private static final Logger log = LoggerFactory.getLogger(TaskIdempotencyGuard.class);

    private final AiTaskMapper taskMapper;

    public TaskIdempotencyGuard(AiTaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    /**
     * 尝试原子性地将任务从 expectedStatus 转换为 newStatus。
     *
     * @param taskId 任务 ID
     * @param expectedStatus 期望的当前状态（如 PENDING）
     * @param newStatus 目标状态（如 PROCESSING）
     * @return true 表示成功获取处理权；false 表示另一线程/实例已抢先处理
     */
    public boolean tryAcquire(long taskId, String expectedStatus, String newStatus) {
        AiTaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("幂等保护：任务 {} 不存在，跳过", taskId);
            return false;
        }
        if (!expectedStatus.equals(task.getStatus())) {
            log.debug("幂等保护：任务 {} 当前状态为 {}，非期望状态 {}，跳过",
                    taskId, task.getStatus(), expectedStatus);
            return false;
        }
        int currentVersion = task.getVersion() == null ? 0 : task.getVersion();
        LambdaUpdateWrapper<AiTaskEntity> wrapper =
                new LambdaUpdateWrapper<AiTaskEntity>()
                        .eq(AiTaskEntity::getId, taskId)
                        .eq(AiTaskEntity::getStatus, expectedStatus)
                        .eq(AiTaskEntity::getVersion, currentVersion)
                        .set(AiTaskEntity::getStatus, newStatus)
                        .set(AiTaskEntity::getVersion, currentVersion + 1)
                        .set(AiTaskEntity::getStartedAt, LocalDateTime.now())
                        .set(AiTaskEntity::getUpdatedAt, LocalDateTime.now());
        int updated = taskMapper.update(null, wrapper);
        if (updated == 1) {
            log.debug("幂等保护：任务 {} 成功从 {} 转换为 {}", taskId, expectedStatus, newStatus);
            return true;
        }
        log.info("幂等保护：任务 {} 状态转换竞争失败（已被其他线程处理）", taskId);
        return false;
    }
}
