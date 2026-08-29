package com.rayk.health.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("mall_order")
public class MallOrderEntity {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long tenantId;
    private Long customerId;
    private String orderNo;
    private String status;
    private Integer amountCent;
    private String paymentChannel;
    private String transactionId;
    private LocalDateTime paymentNotifyAt;
    private LocalDateTime paidAt;
    private LocalDateTime expiresAt;
    private String cancelReason;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
    private Integer version;
}
