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

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex, HttpServletRequest request) {
        return createErrorResponse(ex, request, HttpStatus.CONFLICT, "USER_ALREADY_EXISTS");
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
        log.error("Unhandled exception occurred [traceId: {}]: {}", traceId, ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "INTERNAL_SERVER_ERROR",
            ex.getMessage(),
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
