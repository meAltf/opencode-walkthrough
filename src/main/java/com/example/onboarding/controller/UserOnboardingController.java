package com.example.onboarding.controller;

import com.example.onboarding.dto.ApiResponse;
import com.example.onboarding.dto.OnboardingRequest;
import com.example.onboarding.dto.OnboardingResponse;
import com.example.onboarding.service.UserOnboardingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Validated
public class UserOnboardingController {

    private static final Logger log = LoggerFactory.getLogger(UserOnboardingController.class);

    private final UserOnboardingService onboardingService;

    public UserOnboardingController(UserOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/onboarding")
    public ResponseEntity<ApiResponse<OnboardingResponse>> onboard(
            @Valid @RequestBody OnboardingRequest request) {
        log.info("POST /api/v1/users/onboarding");
        OnboardingResponse onboarded = onboardingService.onboard(request);
        return ResponseEntity
                .created(URI.create("/api/v1/users/onboarding/" + onboarded.id()))
                .body(ApiResponse.success("ONB_201_USER_ONBOARDED", "user onboarding completed", onboarded));
    }

    @GetMapping("/onboarding/{id}")
    public ResponseEntity<ApiResponse<OnboardingResponse>> getById(@PathVariable UUID id) {
        log.info("GET /api/v1/users/onboarding/{}", id);
        return ResponseEntity.ok(ApiResponse.success(
                "ONB_200_USER_FOUND", "user found", onboardingService.findById(id)));
    }

    @GetMapping("/onboarding")
    public ResponseEntity<ApiResponse<OnboardingResponse>> getByEmail(
            @RequestParam @NotBlank @Email String email) {
        log.info("GET /api/v1/users/onboarding?email={}", email);
        return ResponseEntity.ok(ApiResponse.success(
                "ONB_200_USER_FOUND", "user found", onboardingService.findByEmail(email)));
    }
}
