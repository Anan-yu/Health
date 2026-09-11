package com.rayk.health.goldbean.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("gold_platform_registration_invite")
public class GoldBeanPlatformInviteEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String inviteCodeHash;
    private String status;
    private String boundPhoneMasked;
    private String boundPhoneHash;
    private String reservedOrderNo;
    private String consumedOrderNo;
    private LocalDateTime expiresAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private LocalDateTime consumedAt;
    private LocalDateTime revokedAt;

    @TableLogic
    private Integer deleted;

    private Integer version;
}
