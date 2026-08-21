package com.rayk.health.membership.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("membership_plan_benefit")
public class MembershipPlanBenefitEntity {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long planId;
    private String benefitCode;
    private Integer quotaValue;
    private Boolean enabled;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
    private Integer version;
}
