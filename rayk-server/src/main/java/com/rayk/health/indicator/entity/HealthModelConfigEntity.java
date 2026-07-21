package com.rayk.health.indicator.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("health_model_config")
public class HealthModelConfigEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String modelCode;
    private String modelName;
    private String modelCategory;
    private String version;
    private String status;
    private String configSnapshot;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic private Integer deleted;
    private Integer optimisticVersion;
}
