package com.rayk.health.indicator.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("indicator_value")
public class IndicatorValueEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long reportId;
    private Long patientId;
    private String indicatorCode;
    private String indicatorName;
    private BigDecimal value;
    private String unit;
    private BigDecimal referenceLow;
    private BigDecimal referenceHigh;
    private String abnormalFlag;
    private Integer manuallyConfirmed;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic private Integer deleted;
    private Integer version;
}

