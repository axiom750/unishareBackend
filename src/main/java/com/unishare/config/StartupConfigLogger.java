package com.unishare.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StartupConfigLogger {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${server.port}")
    private String serverPort;

    @Value("${DATABASE_URL:not-set}")
    private String databaseUrl;

    @Value("${DATABASE_USERNAME:not-set}")
    private String databaseUsername;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${google.clientId}")
    private String googleClientId;

    @Value("${google.redirectURL}")
    private String googleRedirectUrl;

    @Value("${github.clientId}")
    private String githubClientId;

    @Value("${github.redirectUrl}")
    private String githubRedirectUrl;

    @Value("${github.tokenUrl}")
    private String githubTokenUrl;

    @Value("${github.userUrl}")
    private String githubUserUrl;

    @Value("${cloudinary.cloud-name}")
    private String cloudinaryCloudName;

    @Value("${production.status}")
    private boolean isProduction;

    @EventListener(ApplicationReadyEvent.class)
    public void logConfiguration() {
        log.info("========================================");
        log.info("🚀 UniShare Backend Started Successfully");
        log.info("========================================");
        log.info("📋 Configuration Summary:");
        log.info("  ├─ Active Profile: {}", activeProfile);
        log.info("  ├─ Server Port: {}", serverPort);
        log.info("  ├─ Production Mode: {}", isProduction);
        log.info("  └─ Frontend URL: {}", frontendUrl);
        
        log.info("========================================");
        log.info("🗄️  Database Configuration:");
        log.info("  ├─ URL: {}", maskSensitiveUrl(databaseUrl));
        log.info("  └─ Username: {}", maskMiddle(databaseUsername));
        
        log.info("========================================");
        log.info("🔐 OAuth Configuration:");
        log.info("  ├─ Google Client ID: {}", maskMiddle(googleClientId));
        log.info("  ├─ Google Redirect: {}", googleRedirectUrl);
        log.info("  ├─ GitHub Client ID: {}", maskMiddle(githubClientId));
        log.info("  ├─ GitHub Redirect: {}", githubRedirectUrl);
        log.info("  ├─ GitHub Token URL: {}", githubTokenUrl);
        log.info("  └─ GitHub User URL: {}", githubUserUrl);
        
        log.info("========================================");
        log.info("☁️  Cloudinary Configuration:");
        log.info("  └─ Cloud Name: {}", cloudinaryCloudName);
        
        log.info("========================================");
        log.info("✅ All configurations loaded successfully!");
        log.info("========================================");
    }

    private String maskMiddle(String value) {
        if (value == null || value.equals("not-set") || value.length() < 8) {
            return value;
        }
        int visibleChars = 4;
        String start = value.substring(0, visibleChars);
        String end = value.substring(value.length() - visibleChars);
        return start + "****" + end;
    }

    private String maskSensitiveUrl(String url) {
        if (url == null || url.equals("not-set")) {
            return url;
        }
        // Mask password in JDBC URL
        return url.replaceAll("password=[^&;]+", "password=****");
    }
}
