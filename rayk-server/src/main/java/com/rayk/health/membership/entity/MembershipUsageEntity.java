package com.rayk.health.membership.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("membership_usage")
public class MembershipUsageEntity {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long tenantId;
    private Long customerId;
    private Long membershipId;
    private String benefitCode;
    private String bizType;
    private String bizId;
    private Integer amount;
    private String usageStatus;
    private String idempotencyKey;
    private LocalDateTime reservedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime releasedAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
    private Integer version;
}
