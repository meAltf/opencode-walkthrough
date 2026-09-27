package com.example.onboarding.service;

import com.example.onboarding.dto.OnboardingRequest;
import com.example.onboarding.dto.OnboardingResponse;

import java.util.UUID;

public interface UserOnboardingService {

    OnboardingResponse onboard(OnboardingRequest request);

    OnboardingResponse findById(UUID id);

    OnboardingResponse findByEmail(String email);
}
