package com.rayk.health.healthscan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("health_scan_task")
public class HealthScanTaskEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long patientId;
    private Long userId;
    private String outUserId;
    private String vendorDetectId;
    private String status;
    private String environment;
    private String pluginVersion;
    private String videoDigest;
    private String vendorCode;
    private String vendorMessage;
    private BigDecimal heartRate;
    private BigDecimal heartRateVariability;
    private BigDecimal oxygenSaturation;
    private BigDecimal respirationRate;
    private BigDecimal systolicBloodPressure;
    private BigDecimal diastolicBloodPressure;
    private BigDecimal stressHrv;
    private BigDecimal qualityScore;
    private String rawResultJson;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic private Integer deleted;
    private Integer version;
}

