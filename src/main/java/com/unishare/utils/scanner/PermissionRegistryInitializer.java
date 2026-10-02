package com.unishare.utils.scanner;

import com.unishare.utils.seeder.SecuritySeedProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initializes the permission registry by scanning and synchronizing permissions.
 * Runs AFTER SecuritySeedInitializer to ensure roles exist before permissions are registered.
 */
@Slf4j
@Component
@Order(2) // Run after SecuritySeedInitializer (Order=1)
@RequiredArgsConstructor
public class PermissionRegistryInitializer implements ApplicationRunner {

    private final PermissionScanner permissionScanner;
    private final PermissionSynchronizer permissionSynchronizer;
    private final SecuritySeedProperties securitySeedProperties;

    @Override
    public void run(ApplicationArguments args) {
        
        if (!securitySeedProperties.getPermission().getScanner().isEnabled()) {
            log.info("[SECURITY] Permission scanner is DISABLED");
            return;
        }
        
        log.info("[SECURITY] Permission scanner is ENABLED");

        List<PermissionDefinition> permissions =
                permissionScanner.scan();

        permissionSynchronizer.synchronize(permissions);
    }
}

