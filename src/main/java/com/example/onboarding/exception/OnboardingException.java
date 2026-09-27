package com.example.onboarding.exception;

import org.springframework.http.HttpStatus;

public abstract class OnboardingException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    protected OnboardingException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    protected OnboardingException(String code, HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
