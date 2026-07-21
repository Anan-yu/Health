package com.rayk.health.assessment.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rayk.health.assessment.entity.AiTaskEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface AiTaskMapper extends BaseMapper<AiTaskEntity> {

    /**
     * 查找超时的 PROCESSING 任务（跨租户扫描，定时恢复专用）。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM ai_task WHERE status = 'PROCESSING' AND started_at < #{cutoff} AND deleted = 0")
    List<AiTaskEntity> selectTimedOutProcessing(@Param("cutoff") LocalDateTime cutoff);

    /**
     * 查找卡住的 PENDING 任务（跨租户扫描，定时恢复专用）。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM ai_task WHERE status = 'PENDING' AND created_at < #{cutoff} AND deleted = 0")
    List<AiTaskEntity> selectStuckPending(@Param("cutoff") LocalDateTime cutoff);

    /**
     * 乐观锁状态转换（幂等保护核心 SQL）。
     * 仅当 id、status、version 三重条件同时满足时才更新，返回受影响行数。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE ai_task SET status = #{newStatus}, version = version + 1, "
            + "started_at = #{startedAt}, updated_at = #{updatedAt} "
            + "WHERE id = #{taskId} AND status = #{expectedStatus} AND version = #{expectedVersion} AND deleted = 0")
    int compareAndSetStatus(@Param("taskId") long taskId,
                            @Param("expectedStatus") String expectedStatus,
                            @Param("expectedVersion") int expectedVersion,
                            @Param("newStatus") String newStatus,
                            @Param("startedAt") LocalDateTime startedAt,
                            @Param("updatedAt") LocalDateTime updatedAt);
}

