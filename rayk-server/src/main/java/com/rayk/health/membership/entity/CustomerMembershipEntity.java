package com.rayk.health.membership.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("customer_membership")
public class CustomerMembershipEntity {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long tenantId;
    private Long customerId;
    private Long planId;
    private String status;
    private LocalDateTime startAt;
    private LocalDateTime expireAt;
    private String source;
    private String orderNo;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
    private Integer version;
}
