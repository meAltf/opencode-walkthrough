package com.example.onboarding.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class UserNotFoundException extends OnboardingException {

    public UserNotFoundException(UUID id) {
        super("ONB_404_USER_NOT_FOUND", HttpStatus.NOT_FOUND, "no onboarded user with id " + id);
    }

    public UserNotFoundException(String email) {
        super("ONB_404_USER_NOT_FOUND", HttpStatus.NOT_FOUND, "no onboarded user with email " + email);
    }
}
