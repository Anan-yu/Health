package com.rayk.health.common.api;

import java.util.List;

public record PageResponse<T>(List<T> records, long total, long page, long size) {
    public static <T> PageResponse<T> of(List<T> records) {
        return new PageResponse<>(records, records.size(), 1, Math.max(records.size(), 1));
    }
}

