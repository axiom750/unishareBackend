package com.unishare.exception;

import com.unishare.dto.response.api.UniEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            WebRequest request
    ) {
        String path = request.getDescription(false).replace("uri=", "");
        
        // Extract field errors for logging (without rejected values)
        StringBuilder fieldsSummary = new StringBuilder();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            if (fieldsSummary.length() > 0) {
                fieldsSummary.append(", ");
            }
            fieldsSummary.append(fieldName);
        });
        
        // Concise validation log - NO rejected values logged
        log.warn("[HTTP] Validation failed | path={} | fields=[{}]", 
                path, fieldsSummary.toString());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", System.currentTimeMillis());
        errorDetails.put("status", 400);
        errorDetails.put("error", "Bad Request");
        errorDetails.put("path", path);

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        errorDetails.put("validationErrors", fieldErrors);

        UniEnvelope<Map<String, Object>> envelope = new UniEnvelope<>(
                errorDetails,
                false,
                "Validation failed"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(envelope);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request
    ) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", System.currentTimeMillis());
        errorDetails.put("status", 400);
        errorDetails.put("error", "Bad Request");
        errorDetails.put("message", ex.getMessage());
        errorDetails.put("path", request.getDescription(false).replace("uri=", ""));

        UniEnvelope<Map<String, Object>> envelope = new UniEnvelope<>(
                errorDetails,
                false,
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(envelope);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleIllegalStateException(
            IllegalStateException ex,
            WebRequest request
    ) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", System.currentTimeMillis());
        errorDetails.put("status", 409);
        errorDetails.put("error", "Conflict");
        errorDetails.put("message", ex.getMessage());
        errorDetails.put("path", request.getDescription(false).replace("uri=", ""));

        UniEnvelope<Map<String, Object>> envelope = new UniEnvelope<>(
                errorDetails,
                false,
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(envelope);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            WebRequest request
    ) {
        // Log the full exception server-side for debugging
        log.error("[DB] Data integrity violation: {}", ex.getMessage());

        // Return generic error to client without exposing SQL details
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", System.currentTimeMillis());
        errorDetails.put("status", 500);
        errorDetails.put("error", "Internal Server Error");
        errorDetails.put("message", "An error occurred while processing your request. Please try again.");
        errorDetails.put("path", request.getDescription(false).replace("uri=", ""));

        UniEnvelope<Map<String, Object>> envelope = new UniEnvelope<>(
                errorDetails,
                false,
                "An error occurred"
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(envelope);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleRuntimeException(
            RuntimeException ex,
            WebRequest request
    ) {
        // Log the full exception server-side
        log.error("[ERROR] Runtime exception: {}", ex.getMessage(), ex);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", System.currentTimeMillis());
        errorDetails.put("status", 500);
        errorDetails.put("error", "Internal Server Error");
        errorDetails.put("message", ex.getMessage());
        errorDetails.put("path", request.getDescription(false).replace("uri=", ""));

        UniEnvelope<Map<String, Object>> envelope = new UniEnvelope<>(
                errorDetails,
                false,
                "An error occurred"
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(envelope);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleGenericException(
            Exception ex,
            WebRequest request
    ) {
        // Log the full exception server-side
        log.error("[ERROR] Unexpected exception: {}", ex.getMessage(), ex);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", System.currentTimeMillis());
        errorDetails.put("status", 500);
        errorDetails.put("error", "Internal Server Error");
        errorDetails.put("message", "An unexpected error occurred");
        errorDetails.put("path", request.getDescription(false).replace("uri=", ""));

        UniEnvelope<Map<String, Object>> envelope = new UniEnvelope<>(
                errorDetails,
                false,
                "An unexpected error occurred"
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(envelope);
    }
    
    @ExceptionHandler(com.unishare.redis.RedisOperationException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleRedisException(
            com.unishare.redis.RedisOperationException ex,
            WebRequest request
    ) {
        // Redis operation already logged by RedisService, just log the handler invocation
        log.error("[ERROR] Redis operation failed - returning 500 to client");

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", System.currentTimeMillis());
        errorDetails.put("status", 500);
        errorDetails.put("error", "Internal Server Error");
        errorDetails.put("message", "A temporary service issue occurred. Please try again.");
        errorDetails.put("path", request.getDescription(false).replace("uri=", ""));

        UniEnvelope<Map<String, Object>> envelope = new UniEnvelope<>(
                errorDetails,
                false,
                "Service temporarily unavailable"
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(envelope);
    }
}
