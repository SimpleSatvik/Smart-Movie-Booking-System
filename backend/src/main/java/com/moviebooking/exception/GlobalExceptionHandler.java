package com.moviebooking.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("IllegalStateException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String specificCause = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        log.warn("DataIntegrityViolationException details: {}", specificCause);

        String userMsg = "A concurrent conflict occurred. The resource is already in use or already listed.";
        if (specificCause != null) {
            if (specificCause.contains("bookings_status_check")) {
                userMsg = "Cannot update booking status: Invalid status constraint in database.";
            } else if (specificCause.contains("idx_unique_active_resale_seat")) {
                userMsg = "This ticket seat is already actively listed on the marketplace.";
            } else if (specificCause.contains("resale_listings_sfas_id_key") || specificCause.contains("uk_resale_listings_sfas_id")) {
                userMsg = "This seat already has a resale listing record in the system.";
            }
        }
        return buildErrorResponse(HttpStatus.CONFLICT, "CONFLICT", userMsg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String firstError = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("Validation failed. Please verify submitted fields.");
        log.warn("MethodArgumentNotValidException: {}", firstError);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", firstError);
    }

    @ExceptionHandler(org.springframework.transaction.TransactionException.class)
    public ResponseEntity<Map<String, Object>> handleTransactionException(org.springframework.transaction.TransactionException ex) {
        log.error("Transaction exception: ", ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "TRANSACTION_ERROR", 
                "Operation could not be committed. Please verify your data and retry.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("HttpMessageNotReadableException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Malformed request payload. Please verify your submitted data.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unhandled exception intercepted: ", ex);
        String msg = (ex.getMessage() != null && !ex.getMessage().isBlank()) 
                ? ex.getMessage() 
                : "An unexpected error occurred while processing your request. Please try again.";
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", msg);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String errorName, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", status.value());
        error.put("error", errorName);
        error.put("message", message);
        return ResponseEntity.status(status).body(error);
    }
}
