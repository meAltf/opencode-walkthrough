package com.example.onboarding.dto;

import java.time.Instant;

/**
 * Standardized envelope returned by every endpoint in this API.
 */
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        Instant timestamp
) {

    public static <T> ApiResponse<T> success(String code, String message, T data) {
        return new ApiResponse<>(true, code, message, data, Instant.now());
    }

    public static <T> ApiResponse<T> failure(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, data, Instant.now());
    }
}
