package com.unishare.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class EnvironmentValidator implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Environment env = event.getEnvironment();
        
        log.info("========================================");
        log.info("🔍 Validating Spring Configuration Properties");
        log.info("========================================");
        
        List<String> missingProps = new ArrayList<>();
        
        // Map of property name -> Spring property path
        Map<String, String> requiredProperties = Map.ofEntries(
            Map.entry("spring.datasource.url", "Database URL"),
            Map.entry("spring.datasource.username", "Database Username"),
            Map.entry("spring.datasource.password", "Database Password"),
            Map.entry("jwt.secret", "JWT Secret"),
            Map.entry("frontend.url", "Frontend URL"),
            Map.entry("google.clientId", "Google Client ID"),
            Map.entry("google.clientSecret", "Google Client Secret"),
            Map.entry("github.clientId", "GitHub Client ID"),
            Map.entry("github.clientSecret", "GitHub Client Secret"),
            Map.entry("cloudinary.cloud-name", "Cloudinary Cloud Name"),
            Map.entry("cloudinary.api-key", "Cloudinary API Key"),
            Map.entry("cloudinary.api-secret", "Cloudinary API Secret"),
            Map.entry("brevo.api-key", "Brevo API Key"),
            Map.entry("mail.from-email", "Mail From Email")
        );
        
        log.info("📋 Checking {} required configuration properties...", requiredProperties.size());
        
        for (Map.Entry<String, String> entry : requiredProperties.entrySet()) {
            String propKey = entry.getKey();
            String displayName = entry.getValue();
            String value = env.getProperty(propKey);
            
            if (value == null || value.trim().isEmpty()) {
                missingProps.add(displayName + " (" + propKey + ")");
                log.warn("  ⚠️  Missing or empty: {}", displayName);
            } else {
                log.info("  ✅ Found: {}", displayName);
            }
        }
        
        if (!missingProps.isEmpty()) {
            log.warn("========================================");
            log.warn("⚠️  {} configuration property(ies) missing or empty:", missingProps.size());
            missingProps.forEach(prop -> log.warn("  - {}", prop));
            log.warn("========================================");
            log.warn("⚠️  Application may fail at runtime if these are required");
            log.warn("⚠️  Set properties in application.yaml or via environment variables");
            log.warn("========================================");
        } else {
            log.info("========================================");
            log.info("✅ All required configuration properties are set!");
            log.info("========================================");
        }
    }
}
