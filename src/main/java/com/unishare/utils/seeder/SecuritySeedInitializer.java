package com.unishare.utils.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Orchestrates security seeding on application startup.
 * 
 * Execution order:
 * 1. DefaultRoleSeeder - creates/validates system roles
 * 2. SuperuserBootstrap - assigns/validates SUPERUSER
 * 
 * Runs BEFORE PermissionRegistryInitializer to ensure roles exist
 * before permission scanning begins.
 */
@Slf4j
@Component
@Order(1) // Run before PermissionRegistryInitializer (default order)
@RequiredArgsConstructor
public class SecuritySeedInitializer implements CommandLineRunner {

    private final SecuritySeedProperties securitySeedProperties;
    private final DefaultRoleSeeder defaultRoleSeeder;
    private final SuperuserBootstrap superuserBootstrap;

    @Override
    public void run(String... args) {
        
        if (!securitySeedProperties.getSeed().isEnabled()) {
            log.info("[SECURITY] Security seeding is DISABLED");
            return;
        }
        
        log.info("[SECURITY] Security seeding started");
        
        try {
            // Step 1: Seed default roles (USER, MODERATOR, ADMIN, SUPERUSER)
            defaultRoleSeeder.seedDefaultRoles();
            
            // Step 2: Bootstrap/validate SUPERUSER assignment
            superuserBootstrap.bootstrapOrValidateSuperuser();
            
            log.info("[SECURITY] Security seeding completed successfully");
            
        } catch (Exception e) {
            log.error("[SECURITY] Security seeding FAILED", e);
            throw new IllegalStateException(
                "[SECURITY] Critical security seeding failure. Application startup blocked.", 
                e
            );
        }
    }
}
