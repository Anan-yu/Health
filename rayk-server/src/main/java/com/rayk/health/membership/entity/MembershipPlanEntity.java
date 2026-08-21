package com.rayk.health.membership.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("membership_plan")
public class MembershipPlanEntity {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private String planCode;
    private String planName;
    private Integer durationDays;
    private Integer priceCent;
    private Integer originalPriceCent;
    private String status;
    private String description;
    private Integer sortOrder;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
    private Integer version;
}
