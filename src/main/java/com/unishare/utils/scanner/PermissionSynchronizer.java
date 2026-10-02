package com.unishare.utils.scanner;

import com.unishare.entity.auth.Permission;
import com.unishare.entity.auth.Role;
import com.unishare.enums.auth.PermissionStatus;
import com.unishare.repository.auth.PermissionRepository;
import com.unishare.repository.auth.RoleRepository;
import com.unishare.utils.seeder.DefaultRoles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Synchronizes discovered permissions with the database.
 * 
 * MANDATORY AUTOMATIC SUPERUSER ASSIGNMENT:
 * Every new permission discovered is AUTOMATICALLY and MANDATORILY assigned to the SUPERUSER role.
 * This is a CRITICAL security mechanism that CANNOT be disabled or skipped.
 * 
 * RULES:
 * - New permissions → SUPERUSER AUTOMATICALLY (ALWAYS)
 * - Existing permissions → NO CHANGES to role assignments
 * - Other roles (USER, MODERATOR, ADMIN) → NEVER automatically assigned
 * - If SUPERUSER role is missing → FAIL startup (critical security violation)
 * 
 * This ensures SUPERUSER always has access to all system permissions by default.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionSynchronizer {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public void synchronize(List<PermissionDefinition> discoveredPermissions) {

        LocalDateTime now = LocalDateTime.now();

        List<Permission> existingPermissions = permissionRepository.findAll();

        Map<UUID, Permission> existingById = new HashMap<>();

        for (Permission permission : existingPermissions) {
            existingById.put(permission.getId(), permission);
        }

        Map<UUID, PermissionDefinition> discoveredById = new HashMap<>();

        for (PermissionDefinition definition : discoveredPermissions) {

            PermissionDefinition previous =
                    discoveredById.put(
                            definition.id(),
                            definition
                    );

            if (previous != null && !previous.equals(definition)) {
                throw new IllegalStateException(
                        "Conflicting permission definitions found for UUID: "
                                + definition.id()
                );
            }
        }

        // Get SUPERUSER role for MANDATORY automatic assignment to new permissions
        // This is ALWAYS required - cannot be disabled
        Role superuserRole = roleRepository.findById(DefaultRoles.SUPERUSER_ID)
                .orElseThrow(() -> new IllegalStateException(
                    "[SECURITY] CRITICAL: SUPERUSER role not found. " +
                    "Permission synchronization cannot proceed without SUPERUSER role. " +
                    "Ensure DefaultRoleSeeder has run successfully."
                ));
        
        log.info("[SECURITY] SUPERUSER role loaded for automatic permission assignment | roleId={}", 
                DefaultRoles.SUPERUSER_ID);

        /*
         * Create or update discovered permissions.
         */
        for (PermissionDefinition definition : discoveredPermissions) {

            Permission existing = existingById.get(definition.id());

            if (existing == null) {

                Permission permission = Permission.builder()
                        .id(definition.id())
                        .displayName(definition.displayName())
                        .description(definition.description())
                        .baseEntity(definition.baseEntity())
                        .status(PermissionStatus.ACTIVE)
                        .createdAt(now)
                        .updatedAt(now)
                        .lastSeenAt(now)
                        .build();

                // Set reachableEntities as comma-separated string
                permission.setReachableEntitiesFromCollection(definition.reachableEntities());

                permissionRepository.save(permission);

                log.info(
                        "[SECURITY] Permission registered | id={} | name={}",
                        definition.id(),
                        definition.displayName()
                );

                // MANDATORY AUTOMATIC SUPERUSER ASSIGNMENT for ALL NEW permissions
                // This ALWAYS happens - cannot be disabled or skipped
                assignPermissionToSuperuser(permission, superuserRole);

                continue;
            }

            updatePermission(existing, definition, now);
        }

        /*
         * Permissions existing in the database but no longer
         * discovered from controller annotations are deprecated.
         */
        for (Permission existing : existingPermissions) {

            if (!discoveredById.containsKey(existing.getId())) {

                if (existing.getStatus() != PermissionStatus.DEPRECATED) {

                    existing.setStatus(
                            PermissionStatus.DEPRECATED
                    );

                    existing.setUpdatedAt(now);

                    log.warn(
                            "[SECURITY] Permission deprecated | id={} | name={}",
                            existing.getId(),
                            existing.getDisplayName()
                    );
                }
            }
        }

        log.info(
                "[SECURITY] Permission synchronization completed | discovered={} | existing={}",
                discoveredPermissions.size(),
                existingPermissions.size()
        );
    }

    /**
     * Assigns a permission to the SUPERUSER role.
     * 
     * MANDATORY OPERATION - Cannot be disabled or skipped.
     * IDEMPOTENT: Only creates the mapping if it doesn't already exist.
     * 
     * CRITICAL SECURITY RULE: Only SUPERUSER receives automatic assignments.
     * USER, MODERATOR, and ADMIN are NEVER automatically assigned.
     * 
     * This ensures SUPERUSER always has complete system access by default.
     * Manual permission grants to other roles must be done explicitly through admin interfaces.
     * 
     * @param permission The permission to assign
     * @param superuserRole The SUPERUSER role (must use DefaultRoles.SUPERUSER_ID)
     */
    private void assignPermissionToSuperuser(Permission permission, Role superuserRole) {
        
        // Idempotent check: only assign if mapping doesn't exist
        if (superuserRole.getPermissions().contains(permission)) {
            log.debug(
                    "[SECURITY] Permission already assigned to SUPERUSER (idempotent) | permissionId={} | permissionName={}",
                    permission.getId(),
                    permission.getDisplayName()
            );
            return;
        }
        
        log.info(
                "[SECURITY] Assigning new permission to SUPERUSER | permissionId={} | permissionName={} | roleId={}",
                permission.getId(),
                permission.getDisplayName(),
                superuserRole.getId()
        );
        
        // Create the role-permission mapping
        superuserRole.getPermissions().add(permission);
        permission.getRoles().add(superuserRole);
        
        // Save the role (cascades to role_permissions join table)
        roleRepository.save(superuserRole);
        
        log.info(
                "[SECURITY] ✓ New permission successfully assigned to SUPERUSER | permissionId={} | permissionName={} | roleId={}",
                permission.getId(),
                permission.getDisplayName(),
                superuserRole.getId()
        );
    }

    private void updatePermission(Permission existing, PermissionDefinition definition, LocalDateTime now) {

        boolean changed = false;

        // Only update if displayName changed
        if (!existing.getDisplayName().equals(definition.displayName())) {
            existing.setDisplayName(definition.displayName());
            changed = true;
        }

        // Only update if description changed
        if (!existing.getDescription().equals(definition.description())) {
            existing.setDescription(definition.description());
            changed = true;
        }

        // Only update if baseEntity changed
        if (!existing.getBaseEntity().equals(definition.baseEntity())) {
            existing.setBaseEntity(definition.baseEntity());
            changed = true;
        }

        // Synchronize reachableEntities - compare and update if changed
        Set<String> existingEntitiesSet = existing.getReachableEntitiesAsSet();
        Set<String> newReachableEntities = new HashSet<>(definition.reachableEntities());
        
        if (!existingEntitiesSet.equals(newReachableEntities)) {
            existing.setReachableEntitiesFromCollection(newReachableEntities);
            changed = true;
        }

        // If a deprecated permission appears again in a controller, reactivate it
        if (existing.getStatus() != PermissionStatus.ACTIVE) {
            existing.setStatus(PermissionStatus.ACTIVE);
            changed = true;
            log.info(
                    "[SECURITY] Permission reactivated | id={} | name={}",
                    existing.getId(),
                    existing.getDisplayName()
            );
        }

        // Always update lastSeenAt (this is expected for active permissions)
        existing.setLastSeenAt(now);

        // Only update updatedAt if something actually changed
        if (changed) {
            existing.setUpdatedAt(now);
            log.debug(
                    "[SECURITY] Permission updated | id={} | name={}",
                    existing.getId(),
                    existing.getDisplayName()
            );
        }

        // JPA will handle the merge automatically - no need to call save() for managed entities
    }
}