package com.example.onboarding.dto;

import com.example.onboarding.model.OnboardingStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OnboardingResponse(
        UUID id,
        String email,
        String fullName,
        LocalDate dateOfBirth,
        OnboardingStatus status,
        Instant createdAt
) {
}
