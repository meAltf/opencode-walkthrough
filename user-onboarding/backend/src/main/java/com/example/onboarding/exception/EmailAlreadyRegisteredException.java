package com.example.onboarding.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyRegisteredException extends OnboardingException {

    public EmailAlreadyRegisteredException(String email) {
        super("ONB_409_EMAIL_EXISTS", HttpStatus.CONFLICT, "email is already registered: " + email);
    }
}
