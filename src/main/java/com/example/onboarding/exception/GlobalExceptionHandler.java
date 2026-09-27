package com.example.onboarding.exception;

import com.example.onboarding.dto.ApiResponse;
import com.example.onboarding.dto.FieldErrorDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final Set<String> SENSITIVE_FIELDS = Set.of("password", "secret", "token");

    @ExceptionHandler(OnboardingException.class)
    public ResponseEntity<ApiResponse<Object>> handleOnboarding(OnboardingException ex) {
        log.warn("onboarding error code={} status={} message={}", ex.getCode(), ex.getStatus().value(), ex.getMessage());
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleBodyValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorDto> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDto(
                        fe.getField(),
                        fe.getDefaultMessage(),
                        safeValue(fe.getField(), fe.getRejectedValue())))
                .toList();
        ex.getBindingResult().getGlobalErrors().forEach(ge ->
                log.debug("global validation error: {}", ge.getDefaultMessage()));
        log.warn("request body validation failed with {} field error(s)", fieldErrors.size());
        return build(HttpStatus.BAD_REQUEST, "ONB_400_VALIDATION_FAILED", "request validation failed", fieldErrors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleParamValidation(HandlerMethodValidationException ex) {
        List<FieldErrorDto> fieldErrors = ex.getParameterValidationResults().stream()
                .map(result -> {
                    String field = result.getMethodParameter().getParameterName();
                    Object rejected = result.getArgument();
                    return result.getResolvableErrors().stream()
                            .map(error -> new FieldErrorDto(
                                    field, error.getDefaultMessage(), safeValue(field, rejected)))
                            .toList();
                })
                .flatMap(List::stream)
                .toList();
        log.warn("parameter validation failed with {} error(s)", fieldErrors.size());
        return build(HttpStatus.BAD_REQUEST, "ONB_400_VALIDATION_FAILED", "request validation failed", fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldErrorDto> fieldErrors = ex.getConstraintViolations().stream()
                .map(violation -> new FieldErrorDto(
                        lastNode(violation),
                        violation.getMessage(),
                        safeValue(lastNode(violation), violation.getInvalidValue())))
                .toList();
        log.warn("constraint violation: {}", fieldErrors);
        return build(HttpStatus.BAD_REQUEST, "ONB_400_VALIDATION_FAILED", "request validation failed", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("malformed request body: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "ONB_400_MALFORMED_BODY", "request body is missing or malformed", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception ex) {
        log.error("unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "ONB_500_INTERNAL_ERROR", "an unexpected error occurred", null);
    }

    private static ResponseEntity<ApiResponse<Object>> build(
            HttpStatus status, String code, String message, Object data) {
        return ResponseEntity.status(status).body(ApiResponse.failure(code, message, data));
    }

    private static String lastNode(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int idx = path.lastIndexOf('.');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    private static Object safeValue(String field, Object rejected) {
        if (rejected == null) {
            return null;
        }
        String normalized = field == null ? "" : field.toLowerCase(Locale.ROOT);
        return SENSITIVE_FIELDS.stream().anyMatch(normalized::contains) ? "***REDACTED***" : rejected;
    }
}
