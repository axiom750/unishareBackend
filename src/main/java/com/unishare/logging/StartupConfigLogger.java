package com.unishare.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Startup Configuration Logger
 * 
 * Validates and logs application configuration at startup for production debugging.
 * Includes real Redis connectivity test to verify Redis is actually reachable.
 * 
 * SECURITY:
 * - Never logs actual secret values (passwords, API keys, tokens)
 * - Secrets show as [CONFIGURED] or [MISSING]
 * - Partial masking for identifiers (usernames, client IDs)
 * - Safe URL handling (no embedded passwords logged)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupConfigLogger {

    private final RedisConnectionFactory redisConnectionFactory;

    // Environment & Server
    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${server.port}")
    private String serverPort;

    @Value("${production.status:false}")
    private boolean isProduction;

    // URLs
    @Value("${frontend.url}")
    private String frontendUrl;
    
    @Value("${BACKEND_URL:}")
    private String backendUrl;

    // Database
    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.datasource.password:}")
    private String databasePassword;

    // Redis
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private String redisPort;

    @Value("${spring.data.redis.username:default}")
    private String redisUsername;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private String redisSsl;
    
    @Value("${spring.data.redis.timeout:2000ms}")
    private String redisTimeout;

    // Google OAuth
    @Value("${google.clientId}")
    private String googleClientId;

    @Value("${google.clientSecret:}")
    private String googleClientSecret;

    @Value("${google.redirectURL}")
    private String googleRedirectUrl;

    // GitHub OAuth
    @Value("${github.clientId}")
    private String githubClientId;

    @Value("${github.clientSecret:}")
    private String githubClientSecret;

    @Value("${github.redirectUrl}")
    private String githubRedirectUrl;

    // JWT
    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.expiry:86400000}")
    private String jwtExpiry;

    // Brevo
    @Value("${brevo.api-key:}")
    private String brevoApiKey;

    // Mail
    @Value("${mail.from-email:}")
    private String mailFromEmail;

    @Value("${mail.from-name:UniShare}")
    private String mailFromName;
    
    @Value("${MAIL_HOST:smtp.gmail.com}")
    private String mailHost;

    // Cloudinary
    @Value("${cloudinary.cloud-name}")
    private String cloudinaryCloudName;

    @Value("${cloudinary.api-key:}")
    private String cloudinaryApiKey;

    @Value("${cloudinary.api-secret:}")
    private String cloudinaryApiSecret;

    @EventListener(ApplicationReadyEvent.class)
    public void logConfiguration() {
        log.info("========================================");
        log.info("UniShare Configuration Validation");
        log.info("========================================");
        
        logEnvironment();
        logDatabase();
        logRedisConfigAndTest();
        logFrontendBackend();
        logGoogleOAuth();
        logGitHubOAuth();
        logJWT();
        logBrevo();
        logMail();
        logCloudinary();
        logConfigurationStatus();
        
        log.info("========================================");
        log.info("Configuration Validation Complete");
        log.info("========================================");
    }

    private void logEnvironment() {
        log.info("Environment:");
        log.info("  Active Profile: {}", activeProfile);
        log.info("  Server Port: {}", serverPort);
        log.info("  Production Mode: {}", isProduction);
    }

    private void logDatabase() {
        log.info("Database:");
        log.info("  URL: {}", maskDatabaseUrl(databaseUrl));
        log.info("  Username: {}", mask(databaseUsername));
        log.info("  Password: {}", configuredStatus(databasePassword));
    }

    /**
     * Redis configuration and connectivity test
     */
    private void logRedisConfigAndTest() {
        log.info("============================================================");
        log.info("REDIS CONFIGURATION");
        log.info("============================================================");
        log.info("Host        : {}", redisHost);
        log.info("Port        : {}", redisPort);
        log.info("Username    : {}", redisUsername);
        log.info("SSL         : {}", redisSsl);
        log.info("Timeout     : {}", redisTimeout);
        log.info("Password    : {}", configuredStatus(redisPassword));
        
        // Perform actual connectivity test
        testRedisConnectivity();
    }

    /**
     * Test actual Redis connectivity with PING command
     */
    private void testRedisConnectivity() {
        log.info("============================================================");
        log.info("REDIS CONNECTIVITY TEST");
        log.info("============================================================");
        
        long startTime = System.nanoTime();
        RedisConnection connection = null;
        
        try {
            connection = redisConnectionFactory.getConnection();
            
            // Execute PING command
            String pingResponse = connection.ping();
            
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            
            log.info("Status      : CONNECTED");
            log.info("Ping        : {}", pingResponse != null ? pingResponse : "PONG");
            log.info("Duration    : {} ms", duration);
            log.info("============================================================");
            
        } catch (Exception e) {
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            
            String errorType = classifyRedisError(e);
            String errorMessage = sanitizeErrorMessage(e);
            
            log.error("Status      : FAILED");
            log.error("Duration    : {} ms", duration);
            log.error("Error Type  : {}", errorType);
            log.error("Error       : {}", errorMessage);
            log.error("============================================================");
            
            // Log full exception for debugging (but still don't expose secrets)
            log.error("[REDIS] Connection test failed", e);
            
        } finally {
            // Clean up connection
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    log.warn("[REDIS] Error closing test connection: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Classify Redis connection errors
     */
    private String classifyRedisError(Exception e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        String className = e.getClass().getSimpleName();
        
        if (message.contains("timeout") || message.contains("timed out")) {
            return "CONNECTION_TIMEOUT";
        }
        
        if (message.contains("refused") || message.contains("connection refused")) {
            return "CONNECTION_REFUSED";
        }
        
        if (message.contains("unknown host") || message.contains("name or service not known")) {
            return "DNS_RESOLUTION_FAILURE";
        }
        
        if (message.contains("ssl") || message.contains("handshake")) {
            return "SSL_HANDSHAKE_FAILURE";
        }
        
        if (message.contains("auth") || message.contains("authentication") || message.contains("noauth")) {
            return "AUTHENTICATION_FAILURE";
        }
        
        if (className.contains("Connection")) {
            return "CONNECTION_FAILURE";
        }
        
        return "UNKNOWN";
    }

    /**
     * Sanitize error message for logging (remove any potential secrets)
     */
    private String sanitizeErrorMessage(Exception e) {
        if (e == null) {
            return "Unknown error";
        }
        
        String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        
        // Remove any potential password/token from error message
        message = message.replaceAll("password=[^\\s&]+", "password=***");
        message = message.replaceAll("token=[^\\s&]+", "token=***");
        message = message.replaceAll("auth=[^\\s&]+", "auth=***");
        
        // Limit message length
        if (message.length() > 200) {
            message = message.substring(0, 197) + "...";
        }
        
        return message;
    }

    private void logFrontendBackend() {
        log.info("Frontend:");
        log.info("  URL: {}", frontendUrl);
        log.info("Backend:");
        log.info("  URL: {}", isConfigured(backendUrl) ? backendUrl : "[MISSING]");
    }

    private void logGoogleOAuth() {
        log.info("Google OAuth:");
        log.info("  Client ID: {}", mask(googleClientId));
        log.info("  Client Secret: {}", configuredStatus(googleClientSecret));
        log.info("  Redirect URL: {}", googleRedirectUrl);
    }

    private void logGitHubOAuth() {
        log.info("GitHub OAuth:");
        log.info("  Client ID: {}", mask(githubClientId));
        log.info("  Client Secret: {}", configuredStatus(githubClientSecret));
        log.info("  Redirect URL: {}", githubRedirectUrl);
    }

    private void logJWT() {
        log.info("JWT:");
        log.info("  Secret: {}", configuredStatus(jwtSecret));
        log.info("  Expiry: {} ms", jwtExpiry);
    }

    private void logBrevo() {
        log.info("Brevo:");
        log.info("  API Key: {}", configuredStatus(brevoApiKey));
    }

    private void logMail() {
        log.info("Mail:");
        log.info("  From Email: {}", mailFromEmail);
        log.info("  From Name: {}", mailFromName);
        log.info("  SMTP Host: {}", mailHost);
    }

    private void logCloudinary() {
        log.info("Cloudinary:");
        log.info("  Cloud Name: {}", cloudinaryCloudName);
        log.info("  API Key: {}", configuredStatus(cloudinaryApiKey));
        log.info("  API Secret: {}", configuredStatus(cloudinaryApiSecret));
    }

    private void logConfigurationStatus() {
        boolean allConfigured = true;

        log.info("============================================================");
        log.info("STARTUP CONFIGURATION STATUS");
        log.info("============================================================");

        // Check each required configuration
        allConfigured &= logComponentStatus("Database        ", databaseUrl, databaseUsername, databasePassword);
        allConfigured &= logComponentStatus("Redis Config    ", redisHost, redisPassword);
        
        // Test Redis connectivity separately
        boolean redisConnected = testRedisConnection();
        log.info("  Redis Connection: {}", redisConnected ? "CONNECTED" : "FAILED");
        allConfigured &= redisConnected;
        
        allConfigured &= logComponentStatus("Google OAuth    ", googleClientId, googleClientSecret);
        allConfigured &= logComponentStatus("GitHub OAuth    ", githubClientId, githubClientSecret);
        allConfigured &= logComponentStatus("JWT             ", jwtSecret);
        allConfigured &= logComponentStatus("Brevo           ", brevoApiKey);
        allConfigured &= logComponentStatus("Cloudinary      ", cloudinaryCloudName, cloudinaryApiKey, cloudinaryApiSecret);
        allConfigured &= logComponentStatus("Frontend URL    ", frontendUrl);
        allConfigured &= logComponentStatus("Backend URL     ", backendUrl);

        log.info("============================================================");
        
        if (allConfigured) {
            log.info("✅ All configurations loaded and services connected!");
        } else {
            log.error("❌ Configuration validation failed - check [MISSING]/FAILED entries above");
        }
    }

    /**
     * Quick Redis connection test for status summary
     */
    private boolean testRedisConnection() {
        try {
            RedisConnection connection = redisConnectionFactory.getConnection();
            try {
                connection.ping();
                return true;
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if component configuration is complete
     */
    private boolean logComponentStatus(String componentName, String... values) {
        boolean configured = true;
        for (String value : values) {
            if (!isConfigured(value)) {
                configured = false;
                break;
            }
        }
        
        String status = configured ? "CONFIGURED" : "[MISSING]";
        log.info("  {}: {}", componentName, status);
        
        return configured;
    }

    /**
     * Check if a configuration value is present
     */
    private boolean isConfigured(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Show [CONFIGURED] or [MISSING] for secret values
     * NEVER logs the actual secret
     */
    private String configuredStatus(String value) {
        return isConfigured(value) ? "[CONFIGURED]" : "[MISSING]";
    }

    /**
     * Partially mask identifiers (usernames, client IDs)
     * Shows first 4 and last 4 characters
     */
    private String mask(String value) {
        if (!isConfigured(value)) {
            return "[MISSING]";
        }
        
        if (value.length() <= 8) {
            return "****";
        }
        
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    /**
     * Safely mask database URL
     * Removes any embedded password from JDBC URLs
     */
    private String maskDatabaseUrl(String url) {
        if (!isConfigured(url)) {
            return "[MISSING]";
        }
        
        // Remove password from JDBC URL if present
        String masked = url.replaceAll("password=[^&;]+", "password=****");
        
        // Extract just the host/database portion for cleaner logs
        // Example: jdbc:postgresql://host:5432/database -> host:5432/database
        if (masked.startsWith("jdbc:postgresql://")) {
            String remainder = masked.substring("jdbc:postgresql://".length());
            int queryStart = remainder.indexOf('?');
            if (queryStart > 0) {
                return "jdbc:postgresql://" + remainder.substring(0, queryStart);
            }
            return "jdbc:postgresql://" + remainder;
        }
        
        return masked;
    }
}
