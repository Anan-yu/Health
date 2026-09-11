package com.rayk.health.goldbean.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("gold_member_trade")
public class GoldBeanTradeEntity {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long tenantId;
    private String tradeNo;
    private Long listingId;
    private Long sellerUserId;
    private Long buyerUserId;
    private String bucket;
    private String buyerCreditMode;
    private BigDecimal quantity;
    private Long unitPriceCent;
    private Long totalAmount;
    /** Buyer virtual-payment unit price; unitPriceCent/totalAmount remain seller settlement values. */
    private Long paymentUnitPriceCent;
    private Long paymentAmount;
    private String transactionId;
    private String transferBatchNo;
    private String transferDetailNo;
    private String transferWechatState;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String transferPackageInfo;
    private LocalDateTime transferLastCheckedAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime transferNextRetryAt;
    private String status;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String failureReason;
    private LocalDateTime paidAt;
    private LocalDateTime transferredAt;
    private LocalDateTime expiresAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
    private Integer version;
}
