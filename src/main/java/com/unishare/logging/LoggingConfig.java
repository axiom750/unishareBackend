package com.unishare.logging;

import org.springframework.context.annotation.Configuration;

/**
 * Logging Configuration for UniShare Backend
 * 
 * This configuration class works in conjunction with RequestLoggingFilter
 * to provide comprehensive request/response logging with trace ID correlation.
 * 
 * Features:
 * - Automatic trace ID generation for each request (via RequestLoggingFilter)
 * - MDC (Mapped Diagnostic Context) for thread-safe logging
 * - X-Trace-Id header injection for client-side correlation
 * - Request duration tracking
 * - HTTP method, URI, and status code logging
 * 
 * @see RequestLoggingFilter
 */
@Configuration
public class LoggingConfig {
    
    // Logging configuration is primarily handled by application-{local,prod}.yaml/logback-spring.xml
    // This class exists for future logging-related bean definitions if needed
    
    // Note: Request logging is handled by RequestLoggingFilter.java (standalone component)
    // DO NOT add duplicate filter implementations here
    
}
