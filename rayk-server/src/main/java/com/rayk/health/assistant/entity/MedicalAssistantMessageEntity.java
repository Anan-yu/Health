package com.rayk.health.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("medical_assistant_message")
public class MedicalAssistantMessageEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long patientId;
    private Long conversationId;
    private String role;
    private String content;
    private String riskLevel;
    private Integer emergency;
    private String recommendedAction;
    private String citationsJson;
    private String usedContextJson;
    private String followupQuestionsJson;
    private String model;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
    private Integer version;
}
