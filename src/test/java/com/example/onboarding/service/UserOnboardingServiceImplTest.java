package com.example.onboarding.service;

import com.example.onboarding.dto.OnboardingRequest;
import com.example.onboarding.exception.EmailAlreadyRegisteredException;
import com.example.onboarding.exception.UserNotFoundException;
import com.example.onboarding.model.OnboardingStatus;
import com.example.onboarding.service.impl.UserOnboardingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserOnboardingServiceImplTest {

    private UserOnboardingService service;

    @BeforeEach
    void setUp() {
        service = new UserOnboardingServiceImpl(new PasswordHasher());
    }

    private static OnboardingRequest request(String email) {
        return new OnboardingRequest(
                email, "Ada Lovelace", "s3cret-pass", LocalDate.of(1990, 12, 10), true);
    }

    @Test
    void onboardsUserAndNormalizesEmail() {
        var response = service.onboard(request("  Ada@Example.COM "));

        assertThat(response.id()).isNotNull();
        assertThat(response.email()).isEqualTo("ada@example.com");
        assertThat(response.fullName()).isEqualTo("Ada Lovelace");
        assertThat(response.status()).isEqualTo(OnboardingStatus.COMPLETED);
    }

    @Test
    void rejectsDuplicateEmailRegardlessOfCase() {
        service.onboard(request("ada@example.com"));

        assertThatThrownBy(() -> service.onboard(request("ADA@example.com")))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessageContaining("ada@example.com");
    }

    @Test
    void neverExposesPasswordHashInResponse() {
        var response = service.onboard(request("ada@example.com"));

        assertThat(response.toString()).doesNotContain("pbkdf2");
    }

    @Test
    void findsUserByIdAndEmail() {
        var created = service.onboard(request("ada@example.com"));

        assertThat(service.findById(created.id())).isEqualTo(created);
        assertThat(service.findByEmail("ADA@EXAMPLE.COM")).isEqualTo(created);
    }

    @Test
    void throwsWhenUserMissing() {
        assertThatThrownBy(() -> service.findById(UUID.randomUUID()))
                .isInstanceOf(UserNotFoundException.class);
        assertThatThrownBy(() -> service.findByEmail("nobody@example.com"))
                .isInstanceOf(UserNotFoundException.class);
    }
}
