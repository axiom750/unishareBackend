package com.unishare.utils.seeder;

import com.unishare.entity.auth.Role;
import com.unishare.repository.auth.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

/**
 * Seeds the four system roles with frozen UUID v7 identifiers.
 * 
 * Ensures that:
 * - USER, MODERATOR, ADMIN, SUPERUSER roles exist
 * - Each has its frozen UUID from DefaultRoles
 * - Role names match expected values
 * - Operation is idempotent across restarts
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultRoleSeeder {

    private final RoleRepository roleRepository;

    /**
     * Seeds all four system roles.
     * Validates that frozen UUIDs map to correct role names.
     */
    @Transactional
    public void seedDefaultRoles() {
        log.info("[SECURITY] Default role seeding started");
        
        seedRole(DefaultRoles.USER_ID, DefaultRoles.USER);
        seedRole(DefaultRoles.MODERATOR_ID, DefaultRoles.MODERATOR);
        seedRole(DefaultRoles.ADMIN_ID, DefaultRoles.ADMIN);
        seedRole(DefaultRoles.SUPERUSER_ID, DefaultRoles.SUPERUSER);
        
        log.info("[SECURITY] Default role seeding completed");
    }

    /**
     * Seeds a single role with frozen UUID.
     * 
     * @param frozenId The frozen UUID v7 for this role
     * @param expectedName The expected role name
     */
    private void seedRole(UUID frozenId, String expectedName) {
        Optional<Role> existingRole = roleRepository.findById(frozenId);
        
        if (existingRole.isPresent()) {
            Role role = existingRole.get();
            
            // Verify name matches - this is a critical security invariant
            if (!role.getName().equals(expectedName)) {
                throw new IllegalStateException(
                    String.format(
                        "[SECURITY] CRITICAL: Frozen role UUID %s has incorrect name. " +
                        "Expected: %s, Found: %s. This indicates database corruption or tampering.",
                        frozenId, expectedName, role.getName()
                    )
                );
            }
            
            log.debug("[SECURITY] Default role verified | id={} | name={}", frozenId, expectedName);
            return;
        }
        
        // Create new role with frozen UUID
        Role newRole = Role.builder()
                .id(frozenId)
                .name(expectedName)
                .users(new HashSet<>())
                .permissions(new HashSet<>())
                .build();
        
        roleRepository.save(newRole);
        
        log.info("[SECURITY] Default role created | id={} | name={}", frozenId, expectedName);
    }

    /**
     * Validates that all four system roles exist with correct UUIDs.
     * This can be called on startup to ensure database integrity.
     */
    @Transactional(readOnly = true)
    public void validateSystemRoles() {
        log.debug("[SECURITY] Validating system roles");
        
        validateRole(DefaultRoles.USER_ID, DefaultRoles.USER);
        validateRole(DefaultRoles.MODERATOR_ID, DefaultRoles.MODERATOR);
        validateRole(DefaultRoles.ADMIN_ID, DefaultRoles.ADMIN);
        validateRole(DefaultRoles.SUPERUSER_ID, DefaultRoles.SUPERUSER);
        
        log.debug("[SECURITY] System roles validated successfully");
    }

    private void validateRole(UUID frozenId, String expectedName) {
        Role role = roleRepository.findById(frozenId)
                .orElseThrow(() -> new IllegalStateException(
                    String.format(
                        "[SECURITY] System role missing | id=%s | name=%s", 
                        frozenId, expectedName
                    )
                ));
        
        if (!role.getName().equals(expectedName)) {
            throw new IllegalStateException(
                String.format(
                    "[SECURITY] System role name mismatch | id=%s | expected=%s | found=%s",
                    frozenId, expectedName, role.getName()
                )
            );
        }
    }
}
