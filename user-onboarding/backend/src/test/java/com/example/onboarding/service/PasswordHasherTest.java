package com.example.onboarding.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void hashIsSaltedSoSamePasswordHashesDifferently() {
        String first = hasher.hash("s3cret-pass");
        String second = hasher.hash("s3cret-pass");

        assertThat(first).isNotEqualTo(second);
        assertThat(first).startsWith("pbkdf2$210000$");
    }

    @Test
    void matchesAcceptsCorrectPasswordAndRejectsWrongOne() {
        String stored = hasher.hash("s3cret-pass");

        assertThat(hasher.matches("s3cret-pass", stored)).isTrue();
        assertThat(hasher.matches("wrong-pass", stored)).isFalse();
    }

    @Test
    void matchesHandlesGarbageInput() {
        assertThat(hasher.matches("s3cret-pass", null)).isFalse();
        assertThat(hasher.matches("s3cret-pass", "not-a-hash")).isFalse();
    }
}
