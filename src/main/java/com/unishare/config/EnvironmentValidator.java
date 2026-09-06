package com.unishare.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class EnvironmentValidator implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Environment env = event.getEnvironment();
        
        log.info("========================================");
        log.info("🔍 Validating Environment Variables");
        log.info("========================================");
        
        List<String> missingVars = new ArrayList<>();
        List<String> requiredVars = List.of(
            "SPRING_PROFILES_ACTIVE",
            "DATABASE_URL",
            "DATABASE_USERNAME",
            "DATABASE_PASSWORD",
            "JWT_SECRET",
            "FRONTEND_URL",
            "BACKEND_URL",
            "GOOGLE_CLIENT_ID",
            "GOOGLE_CLIENT_SECRET",
            "GITHUB_CLIENT_ID",
            "GITHUB_CLIENT_SECRET",
            "CLOUDINARY_CLOUD_NAME",
            "CLOUDINARY_API_KEY",
            "CLOUDINARY_API_SECRET"
        );
        
        log.info("📋 Checking {} required environment variables...", requiredVars.size());
        
        for (String var : requiredVars) {
            String value = env.getProperty(var);
            if (value == null || value.trim().isEmpty()) {
                missingVars.add(var);
                log.error("  ❌ Missing: {}", var);
            } else {
                log.info("  ✅ Found: {} = {}", var, maskValue(var, value));
            }
        }
        
        if (!missingVars.isEmpty()) {
            log.error("========================================");
            log.error("❌ Missing {} required environment variable(s):", missingVars.size());
            missingVars.forEach(var -> log.error("  - {}", var));
            log.error("========================================");
            log.error("⚠️  Application may fail to start!");
            log.error("⚠️  Please set all required environment variables");
            log.error("========================================");
        } else {
            log.info("========================================");
            log.info("✅ All required environment variables are set!");
            log.info("========================================");
        }
    }
    
    private String maskValue(String key, String value) {
        // Mask sensitive values
        if (key.contains("SECRET") || key.contains("PASSWORD") || key.contains("JWT")) {
            if (value.length() > 8) {
                return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
            }
            return "****";
        }
        // Show full value for non-sensitive keys
        return value;
    }
}
