package com.unishare.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Startup Configuration Logger
 * 
 * Validates and logs application configuration at startup for production debugging.
 * 
 * SECURITY:
 * - Never logs actual secret values (passwords, API keys, tokens)
 * - Secrets show as [CONFIGURED] or [MISSING]
 * - Partial masking for identifiers (usernames, client IDs)
 * - Safe URL handling (no embedded passwords logged)
 */
@Slf4j
@Component
public class StartupConfigLogger {

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
        logRedis();
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

    private void logRedis() {
        log.info("Redis:");
        log.info("  Host: {}", redisHost);
        log.info("  Port: {}", redisPort);
        log.info("  Username: {}", redisUsername);
        log.info("  SSL: {}", redisSsl);
        log.info("  Password: {}", configuredStatus(redisPassword));
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
        StringBuilder missingConfigs = new StringBuilder();

        log.info("Configuration Status:");

        // Check each required configuration
        allConfigured &= logComponentStatus("Database", databaseUrl, databaseUsername, databasePassword);
        allConfigured &= logComponentStatus("Redis", redisHost, redisPassword);
        allConfigured &= logComponentStatus("Google OAuth", googleClientId, googleClientSecret);
        allConfigured &= logComponentStatus("GitHub OAuth", githubClientId, githubClientSecret);
        allConfigured &= logComponentStatus("JWT", jwtSecret);
        allConfigured &= logComponentStatus("Brevo", brevoApiKey);
        allConfigured &= logComponentStatus("Cloudinary", cloudinaryCloudName, cloudinaryApiKey, cloudinaryApiSecret);
        allConfigured &= logComponentStatus("Frontend URL", frontendUrl);
        allConfigured &= logComponentStatus("Backend URL", backendUrl);

        if (allConfigured) {
            log.info("✅ All configurations loaded successfully!");
        } else {
            log.error("❌ Configuration validation failed - check [MISSING] entries above");
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
