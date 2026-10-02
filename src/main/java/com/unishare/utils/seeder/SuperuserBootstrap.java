package com.unishare.utils.seeder;

import com.unishare.entity.auth.Role;
import com.unishare.entity.user.User;
import com.unishare.repository.auth.RoleRepository;
import com.unishare.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Handles SUPERUSER role bootstrap and validation.
 * 
 * CRITICAL INVARIANTS:
 * - Exactly ONE user must have SUPERUSER role
 * - SUPERUSER assignment happens ONCE during initial bootstrap
 * - SUPERUSER cannot be assigned through normal role APIs
 * - Zero or multiple SUPERUSER users = security violation
 * 
 * Bootstrap process:
 * 1. First startup: Assigns SUPERUSER to configured email
 * 2. Subsequent startups: Validates exactly-one invariant
 * 3. Invariant violation: Fails startup loudly
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuperuserBootstrap {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SecuritySeedProperties securitySeedProperties;

    /**
     * Executes SUPERUSER bootstrap or validation.
     * 
     * If no SUPERUSER exists: Bootstrap to configured email
     * If exactly one SUPERUSER exists: Validate and continue
     * If multiple SUPERUSER exist: FAIL LOUDLY
     */
    @Transactional
    public void bootstrapOrValidateSuperuser() {
        
        // Check if SUPERUSER bootstrap is enabled
        if (!securitySeedProperties.getSuperuser().isEnabled()) {
            log.info("[SECURITY] SUPERUSER bootstrap is DISABLED");
            log.info("[SECURITY] Set unishare.security.superuser.enabled=true to enable SUPERUSER bootstrap");
            return;
        }
        
        log.info("[SECURITY] SUPERUSER bootstrap/validation process started");
        
        // Get SUPERUSER role
        Role superuserRole = roleRepository.findById(DefaultRoles.SUPERUSER_ID)
                .orElseThrow(() -> new IllegalStateException(
                    "[SECURITY] SUPERUSER role not found. DefaultRoleSeeder must run first."
                ));
        
        log.info("[SECURITY] SUPERUSER role loaded | roleId={} | roleName={}", 
                superuserRole.getId(), 
                superuserRole.getName());
        
        // Find all users with SUPERUSER role
        log.info("[SECURITY] Checking for existing SUPERUSER assignments...");
        List<User> superusers = findUsersWithRole(superuserRole);
        
        int superuserCount = superusers.size();
        log.info("[SECURITY] Found {} user(s) with SUPERUSER role", superuserCount);
        
        if (superuserCount == 0) {
            log.info("[SECURITY] No SUPERUSER found - initiating first-time bootstrap");
            // First bootstrap - assign SUPERUSER
            performInitialBootstrap(superuserRole);
            return;
        }
        
        if (superuserCount == 1) {
            // Exactly one SUPERUSER - validate and continue
            User superuser = superusers.get(0);
            log.info("[SECURITY] ✓ SUPERUSER invariant validated (exactly 1 SUPERUSER found)");
            log.info("[SECURITY] Current SUPERUSER | userId={} | email={}", 
                    superuser.getId(), 
                    superuser.getEmail());
            return;
        }
        
        // Multiple SUPERUSER users - CRITICAL SECURITY VIOLATION
        log.error("[SECURITY] ✗ CRITICAL: Multiple SUPERUSER users detected | count={}", superuserCount);
        superusers.forEach(user -> 
            log.error("[SECURITY] SUPERUSER violation | userId={} | email={}", user.getId(), user.getEmail())
        );
        
        throw new IllegalStateException(
            String.format(
                "[SECURITY] CRITICAL SECURITY VIOLATION: Multiple users have SUPERUSER role (found %d). " +
                "This indicates database corruption or unauthorized privilege escalation. " +
                "Manual intervention required. Application startup blocked.",
                superuserCount
            )
        );
    }

    /**
     * Performs initial SUPERUSER bootstrap to configured email.
     */
    private void performInitialBootstrap(Role superuserRole) {
        
        String superuserEmail = securitySeedProperties.getSuperuser().getEmail();
        
        if (superuserEmail == null || superuserEmail.isBlank()) {
            log.warn("[SECURITY] SUPERUSER bootstrap skipped - no email configured");
            log.warn("[SECURITY] Set unishare.security.superuser.email to enable SUPERUSER bootstrap");
            return;
        }
        
        log.info("[SECURITY] SUPERUSER bootstrap started | targetEmail={}", superuserEmail);
        log.info("[SECURITY] Looking up user account for SUPERUSER assignment...");
        
        // Find user by configured email
        Optional<User> userOptional = userRepository.findByEmail(superuserEmail.trim());
        
        if (userOptional.isEmpty()) {
            log.error("[SECURITY] SUPERUSER bootstrap failed - user not found | email={}", superuserEmail);
            throw new IllegalStateException(
                String.format(
                    "[SECURITY] SUPERUSER bootstrap failed: User with email '%s' does not exist. " +
                    "Create the user account first, then restart the application.",
                    superuserEmail
                )
            );
        }
        
        User user = userOptional.get();
        log.info("[SECURITY] User account found | userId={} | email={}", user.getId(), user.getEmail());
        
        // Check if user already has SUPERUSER (shouldn't happen but defensive check)
        if (user.getRoles().contains(superuserRole)) {
            log.info("[SECURITY] User already has SUPERUSER role | userId={} | email={}", user.getId(), user.getEmail());
            log.info("[SECURITY] SUPERUSER bootstrap already complete - no action needed");
            return;
        }
        
        log.info("[SECURITY] Assigning SUPERUSER role to user...");
        log.info("[SECURITY] Role assignment | roleId={} | roleName={} | userId={} | userEmail={}", 
                superuserRole.getId(), 
                superuserRole.getName(), 
                user.getId(), 
                user.getEmail());
        
        // Assign SUPERUSER role
        user.getRoles().add(superuserRole);
        userRepository.save(user);
        
        log.info("[SECURITY] ✓ SUPERUSER role successfully assigned | userId={} | userEmail={} | roleId={}", 
                user.getId(), 
                user.getEmail(), 
                superuserRole.getId());
        log.warn("[SECURITY] ⚠ SUPERUSER role assigned. This is a ONE-TIME operation.");
        log.info("[SECURITY] SUPERUSER bootstrap completed successfully");
    }

    /**
     * Finds all users who have the specified role.
     */
    private List<User> findUsersWithRole(Role role) {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().contains(role))
                .toList();
    }

    /**
     * Validates that exactly one user has SUPERUSER role.
     * Read-only validation for periodic checks.
     */
    @Transactional(readOnly = true)
    public void validateSuperuserInvariant() {
        
        Role superuserRole = roleRepository.findById(DefaultRoles.SUPERUSER_ID)
                .orElseThrow(() -> new IllegalStateException(
                    "[SECURITY] SUPERUSER role not found"
                ));
        
        List<User> superusers = findUsersWithRole(superuserRole);
        
        if (superusers.size() != 1) {
            throw new IllegalStateException(
                String.format(
                    "[SECURITY] SUPERUSER invariant violation: Expected exactly 1 SUPERUSER, found %d",
                    superusers.size()
                )
            );
        }
    }
}
