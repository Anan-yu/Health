package com.rayk.health.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import org.slf4j.MDC;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(int code, String message, T data, String requestId, long timestamp) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data, currentRequestId(), Instant.now().toEpochMilli());
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, currentRequestId(), Instant.now().toEpochMilli());
    }

    private static String currentRequestId() {
        String id = MDC.get("requestId");
        return id == null ? "unknown" : id;
    }
}
