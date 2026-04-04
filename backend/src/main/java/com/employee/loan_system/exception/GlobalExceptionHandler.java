package com.employee.loan_system.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.LinkedHashMap;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Map<String, Object> error = baseError(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI());
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        error.put("errors", fieldErrors);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateResourceException(
            DuplicateResourceException ex,
            HttpServletRequest request) {
        return new ResponseEntity<>(baseError(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        return new ResponseEntity<>(baseError(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRuleException(
            BusinessRuleException ex,
            HttpServletRequest request) {
        return new ResponseEntity<>(baseError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI()),
                HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        return new ResponseEntity<>(baseError(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {
        return new ResponseEntity<>(
                baseError(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", request.getRequestURI()),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException ex,
            HttpServletRequest request) {
        return new ResponseEntity<>(
                baseError(HttpStatus.CONFLICT,
                        "The resource was updated by another request. Reload the latest state and retry.",
                        request.getRequestURI()),
                HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSizeException(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {
        return new ResponseEntity<>(
                baseError(HttpStatus.PAYLOAD_TOO_LARGE, "File too large! Maximum size is 50MB", request.getRequestURI()),
                HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                baseError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request.getRequestURI()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private Map<String, Object> baseError(HttpStatus status, String message, String path) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", status.value());
        error.put("error", status.getReasonPhrase());
        error.put("message", message);
        error.put("path", path);
        return error;
    }
}

