package com.unishare.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Request Logging Filter for UniShare Backend
 * 
 * Provides comprehensive HTTP request/response logging with:
 * - Unique trace ID generation for each request (UUID)
 * - MDC (Mapped Diagnostic Context) for thread-safe trace ID propagation
 * - X-Trace-Id response header for client-side correlation
 * - Request duration measurement
 * - Appropriate log levels based on HTTP status codes
 * - Clean, structured log format
 * 
 * Security:
 * - Does NOT log sensitive data (passwords, tokens, Authorization headers, cookies, request bodies)
 * - Only logs safe request metadata (method, URI, status, duration)
 * 
 * @Order(Ordered.HIGHEST_PRECEDENCE) ensures this runs first in filter chain
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Generate unique trace ID for this request
        String traceId = UUID.randomUUID().toString();
        
        // Store in MDC for thread-safe access across all logs in this request
        MDC.put(TRACE_ID_KEY, traceId);
        
        // Add to response header for client-side correlation
        response.setHeader("X-Trace-Id", traceId);

        long startTime = System.currentTimeMillis();

        try {
            // Continue filter chain
            filterChain.doFilter(request, response);

        } finally {
            // Log request completion with appropriate level based on status code
            logRequestCompletion(request, response, startTime);
            
            // Clean up MDC to prevent memory leaks
            MDC.remove(TRACE_ID_KEY);
        }
    }

    /**
     * Log HTTP request completion with appropriate log level based on status code.
     * 
     * Log Levels:
     * - ERROR (5xx): Server errors
     * - WARN (4xx): Client errors (validation, authentication, authorization)
     * - INFO (2xx, 3xx): Successful requests
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @param startTime Request start time in milliseconds
     */
    private void logRequestCompletion(HttpServletRequest request, HttpServletResponse response, long startTime) {
        try {
            long duration = System.currentTimeMillis() - startTime;
            String method = request.getMethod();
            String uri = request.getRequestURI();
            int status = response.getStatus();

            // Log at appropriate level based on status code
            if (status >= 500) {
                log.error("[HTTP] {} {} | status={} | duration={}ms", method, uri, status, duration);
            } else if (status >= 400) {
                log.warn("[HTTP] {} {} | status={} | duration={}ms", method, uri, status, duration);
            } else {
                log.info("[HTTP] {} {} | status={} | duration={}ms", method, uri, status, duration);
            }

        } catch (Exception e) {
            // Don't let logging failures break the application
            log.error("[HTTP] Failed to log request completion: {}", e.getMessage());
        }
    }
}
