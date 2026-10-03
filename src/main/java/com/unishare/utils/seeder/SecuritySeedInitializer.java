package com.unishare.utils.seeder;

import com.unishare.utils.scanner.PermissionDefinition;
import com.unishare.utils.scanner.PermissionScanner;
import com.unishare.utils.scanner.PermissionSynchronizer;
import com.unishare.utils.scanner.SecurityDeclarations;
import com.unishare.utils.synchronizer.SecurityDeclarationSynchronizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The single security initialization flow.
 *
 * Context creation (fail fast, before traffic):
 *   SecuritySeedProperties.validate()  -> invalid flag combinations block startup
 *   SecurityDeclarationScanner.scan()  -> SecurityDeclarations bean
 *
 * This runner (after the context is refreshed, so JPA and the
 * requestMappingHandlerMapping are fully initialized):
 *   1. SecurityDeclarationSynchronizer -> GodAccount, GodRole, GodAccountRole (control plane) + roles (normal RBAC)
 *   2. PermissionScanner               -> @Permission on controller handler methods
 *   3. PermissionSynchronizer          -> permissions registry (APPLICATION + CONTROL_PLANE)
 *   4. SecurityDeclarationSynchronizer -> god_role_permissions (GodRole -> ACTIVE CONTROL_PLANE permissions)
 *
 * All database writes run in ONE transaction, so a failure in any step
 * leaves no partially synchronized security state.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecuritySeedInitializer implements ApplicationRunner {

    private final SecuritySeedProperties properties;
    private final SecurityDeclarations declarations;
    private final SecurityDeclarationSynchronizer declarationSynchronizer;
    private final PermissionScanner permissionScanner;
    private final PermissionSynchronizer permissionSynchronizer;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        log.info("[SECURITY] Security initialization started");

        SecuritySeedProperties.Scanning scanning = properties.getScanning();
        SecuritySeedProperties.Seeding seeding = properties.getSeeding();

        log.info("[SECURITY] scanning: enabled={} god-account={} god-role={} roles={} permissions={}",
                scanning.isEnabled(), scanning.isGodAccount(), scanning.isGodRole(), scanning.isRoles(), scanning.isPermissions());
        log.info("[SECURITY] seeding: enabled={} god-account={} god-role={} roles={} permissions={} god-account-role={}",
                seeding.isEnabled(), seeding.isGodAccount(), seeding.isGodRole(), seeding.isRoles(),
                seeding.isPermissions(), seeding.isGodAccountRole());

        if (seeding.isEnabled()) {
            declarationSynchronizer.synchronize(declarations, seeding);
        } else {
            log.info("[SECURITY] Seeding disabled - declarations validated but not synchronized");
        }

        if (scanning.shouldScanPermissions()) {

            log.info("[SECURITY] Permission scanning started");

            List<PermissionDefinition> permissions = permissionScanner.scan();

            if (seeding.shouldSeedPermissions()) {
                permissionSynchronizer.synchronize(permissions);
            } else {
                log.info("[SECURITY] Permission seeding disabled - permissions validated but not synchronized");
            }
        } else {
            log.info("[SECURITY] Permission scanning disabled");
        }

        // Must run AFTER permission synchronization: grants only reference persisted permissions.
        if (seeding.shouldSeedGodRole() && seeding.shouldSeedPermissions()) {
            declarationSynchronizer.synchronizeGodRolePermissions(declarations);
        } else {
            log.info("[SECURITY] GodRole permission synchronization skipped (requires god-role and permissions seeding)");
        }

        log.info("[SECURITY] Security initialization completed");
    }
}
