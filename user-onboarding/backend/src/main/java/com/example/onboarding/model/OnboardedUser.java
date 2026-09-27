package com.example.onboarding.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OnboardedUser(
        UUID id,
        String email,
        String fullName,
        String passwordHash,
        LocalDate dateOfBirth,
        OnboardingStatus status,
        Instant createdAt
) {
}
