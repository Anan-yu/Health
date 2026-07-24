package com.rayk.health.followup.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("followup_task")
public class FollowupTaskEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long patientId;
    private Long planId;
    private Long parentTaskId;
    private Integer cycleNo;
    private Integer maxCycles;
    private Long assigneeId;
    private String title;
    private String content;
    private LocalDate dueDate;
    private String status;
    private String feedback;
    private Integer completionRate;
    private String feedbackDetail;
    private String decision;
    private String decisionReason;
    private Integer reminderCount;
    private LocalDateTime lastRemindedAt;
    private LocalDateTime completedAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic private Integer deleted;
    private Integer version;
}
