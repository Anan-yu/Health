package com.rayk.health.goldbean.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("gold_member_account")
public class GoldBeanAccountEntity {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long tenantId;
    private Long userId;
    private String memberLevel;
    private String historicalLevel;
    private Integer directReferralCount;
    private Long referrerId;
    private String referralCode;
    private String registrationFeeStatus;
    private String registrationFeeRecipient;
    private Integer registrationFeeCent;
    private LocalDateTime registrationFeePaidAt;
    private String city;
    private BigDecimal digitalBankBalance;
    private BigDecimal tradingBalance;
    private LocalDateTime dailyRewardStartAt;
    private Integer dailyRewardDays;
    private LocalDateTime dailyRewardLastAt;
    private LocalDateTime protectionStartedAt;
    private LocalDateTime protectionUntil;
    private LocalDateTime lastProtectionReferralAt;
    private LocalDateTime lastLevelDropAt;
    private Integer tradeLimitPercent;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
    private Integer version;
}
