package com.unishare.exception;

import com.unishare.dto.response.api.UniEnvelope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            WebRequest request
    ) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", System.currentTimeMillis());
        errorDetails.put("status", 400);
        errorDetails.put("error", "Bad Request");
        errorDetails.put("path", request.getDescription(false).replace("uri=", ""));

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

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleRuntimeException(
            RuntimeException ex,
            WebRequest request
    ) {
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
}
