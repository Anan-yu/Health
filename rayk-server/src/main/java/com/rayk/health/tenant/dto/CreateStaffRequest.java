package com.rayk.health.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** A tenant administrator pre-registers staff by their mobile number. */
public record CreateStaffRequest(
        @NotBlank(message = "姓名不能为空") String displayName,
        @NotBlank(message = "手机号不能为空")
                @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的中国大陆手机号")
                String phone,
        @NotBlank
                @Pattern(regexp = "^(DOCTOR|HEALTH_MANAGER)$", message = "仅可录入医生或健康管理师")
                String roleCode) {}
