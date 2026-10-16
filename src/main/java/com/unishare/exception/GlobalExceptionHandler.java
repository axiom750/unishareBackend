
package com.unishare.exception;

import com.unishare.dto.response.api.UniEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<UniEnvelope<Map<String, Object>>> buildErrorResponse(
            HttpStatus status,
            String message,
            String path,
            Map<String, Object> details,
            HttpHeaders headers) {

        Map<String, Object> errorDetails = new LinkedHashMap<>();

        errorDetails.put("timestamp", Instant.now().toString());
        errorDetails.put("status", status.value());
        errorDetails.put("error", status.getReasonPhrase());
        errorDetails.put("message", message);
        errorDetails.put("path", path);

        if (details != null && !details.isEmpty()) {
            errorDetails.putAll(details);
        }

        UniEnvelope<Map<String, Object>> body =
                new UniEnvelope<>(errorDetails, false, message);

        return new ResponseEntity<>(body, headers, status);
    }

    private ResponseEntity<UniEnvelope<Map<String, Object>>> error(
            HttpStatus status,
            String message,
            HttpServletRequest request) {

        return buildErrorResponse(
                status,
                message,
                request.getRequestURI(),
                null,
                new HttpHeaders()
        );
    }

    private ResponseEntity<UniEnvelope<Map<String, Object>>> error(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, Object> details) {

        return buildErrorResponse(
                status,
                message,
                request.getRequestURI(),
                details,
                new HttpHeaders()
        );
    }

    // --------------------------------------------------
    // 401 / 403 - Authentication and authorization
    // --------------------------------------------------

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        log.warn(
                "[HTTP] Access denied | method={} | path={}",
                request.getMethod(),
                request.getRequestURI()
        );

        return error(
                HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action.",
                request
        );
    }

    // --------------------------------------------------
    // 404 - Missing routes
    // --------------------------------------------------

    @ExceptionHandler({
            NoResourceFoundException.class,
            NoHandlerFoundException.class
    })
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleNotFound(
            Exception ex,
            HttpServletRequest request) {

        log.warn(
                "[HTTP] Route not found | method={} | path={}",
                request.getMethod(),
                request.getRequestURI()
        );

        return error(
                HttpStatus.NOT_FOUND,
                "The requested endpoint was not found.",
                request
        );
    }

    // --------------------------------------------------
    // 400 - Request validation
    // --------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.computeIfAbsent(
                    fieldError.getField(),
                    key -> new java.util.ArrayList<>()
            ).add(fieldError.getDefaultMessage() != null
                    ? fieldError.getDefaultMessage()
                    : "Invalid value");
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("fieldErrors", fieldErrors);

        log.warn(
                "[HTTP] Validation failed | path={} | fields={}",
                request.getRequestURI(),
                fieldErrors.keySet()
        );

        return error(
                HttpStatus.BAD_REQUEST,
                "Request validation failed.",
                request,
                details
        );
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleBindException(
            BindException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(
                    fieldError.getField(),
                    fieldError.getDefaultMessage() != null
                            ? fieldError.getDefaultMessage()
                            : "Invalid value"
            );
        }

        return error(
                HttpStatus.BAD_REQUEST,
                "Request validation failed.",
                request,
                Map.of("fieldErrors", fieldErrors)
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        List<String> violations = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath()
                        + ": " + violation.getMessage())
                .toList();

        return error(
                HttpStatus.BAD_REQUEST,
                "Request constraint validation failed.",
                request,
                Map.of("violations", violations)
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleUnreadableBody(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        return error(
                HttpStatus.BAD_REQUEST,
                "Request body is missing or malformed.",
                request
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        return error(
                HttpStatus.BAD_REQUEST,
                "Required parameter is missing: " + ex.getParameterName(),
                request
        );
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleMissingPart(
            MissingServletRequestPartException ex,
            HttpServletRequest request) {

        return error(
                HttpStatus.BAD_REQUEST,
                "Required request part is missing: " + ex.getRequestPartName(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        return error(
                HttpStatus.BAD_REQUEST,
                "Invalid value supplied for parameter: " + ex.getName(),
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        String message = ex.getMessage();

        return error(
                HttpStatus.BAD_REQUEST,
                message != null && !message.isBlank()
                        ? message
                        : "Invalid request argument.",
                request
        );
    }

    // --------------------------------------------------
    // 405 / 406 / 413 / 415 - HTTP and upload errors
    // --------------------------------------------------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        HttpHeaders headers = new HttpHeaders();

        if (ex.getSupportedHttpMethods() != null) {
            headers.setAllow(ex.getSupportedHttpMethods());
        }

        return buildErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method is not supported for this endpoint.",
                request.getRequestURI(),
                null,
                headers
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {

        return error(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "The request content type is not supported.",
                request
        );
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException ex,
            HttpServletRequest request) {

        return error(
                HttpStatus.NOT_ACCEPTABLE,
                "The requested response format is not available.",
                request
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleMaxUploadSize(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {

        return error(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Uploaded file or request exceeds the allowed size.",
                request
        );
    }

    // --------------------------------------------------
    // 409 - Database conflicts
    // --------------------------------------------------

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleOptimisticLocking(
            OptimisticLockingFailureException ex,
            HttpServletRequest request) {

        log.warn(
                "[HTTP] Concurrent modification | path={}",
                request.getRequestURI()
        );

        return error(
                HttpStatus.CONFLICT,
                "The resource was modified by another request. Please retry.",
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.warn(
                "[HTTP] Data integrity violation | path={} | exception={}",
                request.getRequestURI(),
                ex.getClass().getSimpleName()
        );

        return error(
                HttpStatus.CONFLICT,
                "The operation conflicts with existing data or a database constraint.",
                request
        );
    }

    // --------------------------------------------------
    // 500 - Unexpected errors
    // --------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<UniEnvelope<Map<String, Object>>> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request) {

        log.error(
                "[ERROR] Unexpected exception | method={} | path={}",
                request.getMethod(),
                request.getRequestURI(),
                ex
        );

        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred.",
                request
        );
    }
}
