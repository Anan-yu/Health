package com.rayk.health.common.scheduler;

import com.rayk.health.followup.entity.FollowupTaskEntity;
import com.rayk.health.followup.mapper.FollowupTaskMapper;
import com.rayk.health.tenant.TenantContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Generates an in-app reminder marker for due follow-ups and prevents abandoned cycles from
 * continuing forever.
 */
@Component
public class FollowupReminderScheduler {
    private static final int OVERDUE_PAUSE_DAYS = 14;
    private final FollowupTaskMapper followupTaskMapper;

    public FollowupReminderScheduler(FollowupTaskMapper followupTaskMapper) {
        this.followupTaskMapper = followupTaskMapper;
    }

    @Scheduled(
            cron = "${rayk.followup.reminder-cron:0 0 * * * *}",
            zone = "Asia/Shanghai")
    public void refreshReminders() {
        LocalDate today = LocalDate.now();
        List<FollowupTaskEntity> candidates =
                followupTaskMapper.selectReminderCandidates(today.plusDays(1));
        for (FollowupTaskEntity candidate : candidates) {
            TenantContext.execute(candidate.getTenantId(), () -> refresh(candidate.getId(), today));
        }
    }

    private void refresh(Long taskId, LocalDate today) {
        FollowupTaskEntity task = followupTaskMapper.selectById(taskId);
        if (task == null || !"PENDING".equals(task.getStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (task.getDueDate().isBefore(today.minusDays(OVERDUE_PAUSE_DAYS))) {
            task.setStatus("PAUSED");
            task.setDecision("PAUSE");
            task.setDecisionReason("截止日期后14天仍未提交反馈，本轮健康随访已暂停。");
        } else if (task.getLastRemindedAt() == null
                || !task.getLastRemindedAt().toLocalDate().equals(today)) {
            task.setReminderCount((task.getReminderCount() == null ? 0 : task.getReminderCount()) + 1);
            task.setLastRemindedAt(now);
        } else {
            return;
        }
        task.setUpdatedAt(now);
        task.setUpdatedBy(task.getCreatedBy());
        followupTaskMapper.updateById(task);
    }
}
