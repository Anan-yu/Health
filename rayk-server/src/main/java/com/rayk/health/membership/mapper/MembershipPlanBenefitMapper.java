package com.rayk.health.membership.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rayk.health.membership.entity.MembershipPlanBenefitEntity;

@InterceptorIgnore(tenantLine = "true")
public interface MembershipPlanBenefitMapper extends BaseMapper<MembershipPlanBenefitEntity> {}
