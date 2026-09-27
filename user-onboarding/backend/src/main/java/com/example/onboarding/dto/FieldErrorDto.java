package com.example.onboarding.dto;

public record FieldErrorDto(
        String field,
        String message,
        Object rejectedValue
) {
}
