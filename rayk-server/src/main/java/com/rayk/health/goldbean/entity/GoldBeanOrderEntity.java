package com.rayk.health.goldbean.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("gold_member_order")
public class GoldBeanOrderEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long customerId;

    private String orderNo;

    private String orderType;

    private String status;

    private Integer amountCent;

    /** Buyer virtual-payment amount; amountCent remains the business/settlement amount. */
    private Integer paymentAmountCent;

    private BigDecimal goldBeanQuantity;

    private String registrationReferralCode;

    private Long registrationReferrerId;

    private String registrationCity;

    private String registrationFeeRecipient;

    private String settlementStatus;

    private String settlementBatchNo;

    private String settlementDetailNo;

    private String settlementWechatState;

    private String settlementPackageInfo;

    private LocalDateTime settlementLastCheckedAt;

    private LocalDateTime settlementNextRetryAt;

    private String settlementFailureReason;

    private LocalDateTime settledAt;

    private Long platformInviteId;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String platformSlotKey;

    private String productId;

    private String paymentChannel;

    private String transactionId;

    private LocalDateTime paymentNotifyAt;

    private LocalDateTime paidAt;

    private LocalDateTime expiresAt;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    private Integer version;
}
