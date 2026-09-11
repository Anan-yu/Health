package com.rayk.health.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("health_tree_hole_feedback")
public class HealthTreeHoleFeedbackEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long patientId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer recordedDays;
    private Integer entryCount;
    private String content;
    private String riskLevel;
    private String recommendedAction;
    private String model;
    private LocalDateTime generatedAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
    private Integer version;
}
