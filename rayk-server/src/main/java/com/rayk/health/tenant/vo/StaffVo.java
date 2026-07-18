package com.rayk.health.tenant.vo;

import java.util.List;

public record StaffVo(String id, String displayName, List<String> roles, String status) {}

