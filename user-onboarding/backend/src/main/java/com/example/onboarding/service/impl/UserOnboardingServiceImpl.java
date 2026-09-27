package com.example.onboarding.service.impl;

import com.example.onboarding.dto.OnboardingRequest;
import com.example.onboarding.dto.OnboardingResponse;
import com.example.onboarding.exception.EmailAlreadyRegisteredException;
import com.example.onboarding.exception.UserNotFoundException;
import com.example.onboarding.model.OnboardedUser;
import com.example.onboarding.model.OnboardingStatus;
import com.example.onboarding.service.PasswordHasher;
import com.example.onboarding.service.UserOnboardingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation. Swap the two maps for a JPA repository to persist.
 */
@Service
public class UserOnboardingServiceImpl implements UserOnboardingService {

    private static final Logger log = LoggerFactory.getLogger(UserOnboardingServiceImpl.class);

    private final Map<UUID, OnboardedUser> usersById = new ConcurrentHashMap<>();
    private final Map<String, UUID> idsByEmail = new ConcurrentHashMap<>();

    private final PasswordHasher passwordHasher;

    public UserOnboardingServiceImpl(PasswordHasher passwordHasher) {
        this.passwordHasher = passwordHasher;
    }

    @Override
    public OnboardingResponse onboard(OnboardingRequest request) {
        String email = normalizeEmail(request.email());
        log.info("onboarding requested email={}", email);

        UUID id = UUID.randomUUID();
        OnboardedUser user = new OnboardedUser(
                id,
                email,
                request.fullName().trim(),
                passwordHasher.hash(request.password()),
                request.dateOfBirth(),
                OnboardingStatus.COMPLETED,
                Instant.now());

        UUID existing = idsByEmail.putIfAbsent(email, id);
        if (existing != null) {
            log.warn("onboarding rejected reason=email_already_registered email={}", email);
            throw new EmailAlreadyRegisteredException(email);
        }
        usersById.put(id, user);

        log.info("onboarding completed userId={} email={} status={}", id, email, user.status());
        return toResponse(user);
    }

    @Override
    public OnboardingResponse findById(UUID id) {
        OnboardedUser user = usersById.get(id);
        if (user == null) {
            log.warn("lookup failed reason=user_not_found userId={}", id);
            throw new UserNotFoundException(id);
        }
        return toResponse(user);
    }

    @Override
    public OnboardingResponse findByEmail(String email) {
        String normalized = normalizeEmail(email);
        UUID id = idsByEmail.get(normalized);
        if (id == null) {
            log.warn("lookup failed reason=user_not_found email={}", normalized);
            throw new UserNotFoundException(normalized);
        }
        return toResponse(usersById.get(id));
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static OnboardingResponse toResponse(OnboardedUser user) {
        return new OnboardingResponse(
                user.id(),
                user.email(),
                user.fullName(),
                user.dateOfBirth(),
                user.status(),
                user.createdAt());
    }
}
