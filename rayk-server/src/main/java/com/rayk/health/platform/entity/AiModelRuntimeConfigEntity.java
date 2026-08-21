package com.rayk.health.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("ai_model_runtime_config")
public class AiModelRuntimeConfigEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String modelCode;
    private String modelName;
    private String provider;
    private String modelVersion;
    private String baseUrl;
    private Integer contextLengthTokens;
    private Integer maxOutputTokens;
    private Integer thinkingSupported;
    private Integer thinkingEnabled;
    private String status;
    private Integer selected;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic private Integer deleted;
    private Integer optimisticVersion;
}
