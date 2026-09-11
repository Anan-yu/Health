package com.rayk.health.goldbean.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("gold_region_profit")
public class GoldRegionProfitEntity {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long tenantId;
    private Long regionId;
    private Long ownerUserId;
    private BigDecimal amount;
    private BigDecimal ownerAmount;
    private BigDecimal retainedAmount;
    private String status;
    private String idempotencyKey;
    private LocalDateTime settledAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
    private Integer version;
}
