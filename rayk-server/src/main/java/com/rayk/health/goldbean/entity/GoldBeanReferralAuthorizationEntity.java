package com.rayk.health.goldbean.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("gold_referral_authorization")
public class GoldBeanReferralAuthorizationEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long userId;
    private String appId;
    private String openid;
    private String transferSceneId;
    private String outAuthorizationNo;
    private String authorizationId;
    private String state;
    private String packageInfo;
    private LocalDateTime authorizationCreatedAt;
    private LocalDateTime authorizedAt;
    private LocalDateTime lastCheckedAt;
    private LocalDateTime nextRetryAt;
    private String failureReason;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    private Integer version;
}
