package com.rayk.health.goldbean.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("gold_robot_redemption")
public class GoldRobotRedemptionEntity {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long tenantId;
    private Long userId;
    private String redemptionNo;
    private String entitlementCode;
    private BigDecimal goldBeanCost;
    private String groupName;
    private String groupQrImageUrl;
    private String clientRequestId;
    private String status;
    private LocalDateTime redeemedAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
    private Integer version;
}
