package com.example.bank.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleStatus(org.springframework.web.server.ResponseStatusException ex,
                                                     HttpServletRequest request) {
        return createErrorResponse(ex, request, HttpStatus.valueOf(ex.getStatusCode().value()),
                "REQUEST_REJECTED", ex.getReason());
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataConflict(Exception ex, HttpServletRequest request) {
        return createErrorResponse(ex, request, HttpStatus.CONFLICT, "DATA_CONFLICT", "Request conflicts with existing data");
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(Exception ex, HttpServletRequest request) {
        return createErrorResponse(ex, request, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid credentials or token");
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(Exception ex, HttpServletRequest request) {
        return createErrorResponse(ex, request, HttpStatus.FORBIDDEN, "FORBIDDEN", "Access is denied");
    }

    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleConflict(Exception ex, HttpServletRequest request) {
        return createErrorResponse(ex, request, HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "Resource was updated concurrently; retry the operation");
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception ex, HttpServletRequest request) {
        return createErrorResponse(ex, request, HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Malformed request body");
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex, HttpServletRequest request) {
        return createErrorResponse(ex, request, HttpStatus.CONFLICT, "USER_ALREADY_EXISTS");
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        return createErrorResponse(ex, request, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        return createErrorResponse(ex, request, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, HttpServletRequest request) {
        UUID traceId = UUID.randomUUID();
        // Exception messages/causes can contain SQL parameters or credential-bearing input.
        log.error("Unhandled exception [traceId: {}] [type: {}]", traceId, ex.getClass().getName());

        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred",
            request.getRequestURI(),
            traceId
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(Exception ex, HttpServletRequest request, HttpStatus status, String errorCode) {
        return createErrorResponse(ex, request, status, errorCode, ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(Exception ex, HttpServletRequest request, HttpStatus status, String errorCode, String message) {
        UUID traceId = UUID.randomUUID();
        log.warn("{} occurred [traceId: {}]: {}", errorCode, traceId, message);

        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                status.value(),
                errorCode,
                message,
                request.getRequestURI(),
                traceId
        );
        return new ResponseEntity<>(error, status);
    }
}
