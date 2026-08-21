package com.rayk.health.membership.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("membership_order")
public class MembershipOrderEntity {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long tenantId;
    private Long customerId;
    private Long planId;
    private String orderNo;
    private String status;
    private Integer amountCent;
    private String paymentChannel;
    private String transactionId;
    private LocalDateTime paidAt;
    private LocalDateTime paymentNotifyAt;
    private LocalDateTime expireAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
    private Integer version;
}
