package com.rayk.health.membership.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rayk.health.membership.entity.MembershipBenefitEntity;

@InterceptorIgnore(tenantLine = "true")
public interface MembershipBenefitMapper extends BaseMapper<MembershipBenefitEntity> {}
