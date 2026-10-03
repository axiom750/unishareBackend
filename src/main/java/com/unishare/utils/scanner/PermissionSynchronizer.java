package com.unishare.utils.scanner;

import com.unishare.entity.rbac.Permission;
import com.unishare.enums.rbac.PermissionStatus;
import com.unishare.repository.auth.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Synchronizes discovered @Permission declarations with the database.
 *
 * - Identity is the frozen permission UUID; metadata (including the canonical
 *   name from @PreAuthorize) is updated in place.
 * - Permission.name is unique across the registry; clashes fail startup.
 * - Permissions no longer discovered are marked DEPRECATED, never deleted.
 * - Deprecated permissions that reappear are reactivated.
 * - The PermissionDomain (APPLICATION / CONTROL_PLANE) comes from @Permission
 *   and is updated in place when the annotation changes.
 * - This class writes the permissions registry only. It never grants
 *   permissions: role_permissions is untouched, and god_role_permissions is
 *   reconciled afterwards by SecurityDeclarationSynchronizer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionSynchronizer {

    private final PermissionRepository permissionRepository;

    @Transactional
    public void synchronize(List<PermissionDefinition> discoveredPermissions) {

        LocalDateTime now = LocalDateTime.now();

        Map<UUID, PermissionDefinition> discoveredById = new HashMap<>();

        for (PermissionDefinition definition : discoveredPermissions) {

            PermissionDefinition previous = discoveredById.put(definition.id(), definition);

            if (previous != null && !previous.equals(definition)) {
                throw new IllegalStateException(
                        "Conflicting permission definitions found for UUID: " + definition.id()
                );
            }
        }

        List<Permission> existingPermissions = permissionRepository.findAll();

        Map<UUID, Permission> existingById = new HashMap<>();

        for (Permission permission : existingPermissions) {
            existingById.put(permission.getId(), permission);
        }

        verifyUniqueNames(discoveredById, existingPermissions);

        for (PermissionDefinition definition : discoveredById.values()) {

            Permission permission = existingById.get(definition.id());

            if (permission == null) {

                permission = Permission.builder()
                        .id(definition.id())
                        .name(definition.name())
                        .displayName(definition.displayName())
                        .description(definition.description())
                        .baseEntity(definition.baseEntity())
                        .domain(definition.domain())
                        .status(PermissionStatus.ACTIVE)
                        .createdAt(now)
                        .updatedAt(now)
                        .lastSeenAt(now)
                        .build();

                permission.setReachableEntitiesFromCollection(definition.reachableEntities());

                permissionRepository.save(permission);

                log.info("[SECURITY] Permission registered | id={} | name={} | displayName={} | domain={}",
                        definition.id(), definition.name(), definition.displayName(), definition.domain());

            } else {
                updatePermission(permission, definition, now);
            }
        }

        for (Permission existing : existingPermissions) {

            if (!discoveredById.containsKey(existing.getId())
                    && existing.getStatus() != PermissionStatus.DEPRECATED) {

                existing.setStatus(PermissionStatus.DEPRECATED);
                existing.setUpdatedAt(now);

                log.warn("[SECURITY] Permission deprecated | id={} | name={}",
                        existing.getId(), existing.getName());
            }
        }

        log.info("[SECURITY] Permissions synchronized | discovered={} | previouslyStored={}",
                discoveredById.size(), existingPermissions.size());
    }

    /**
     * Permission.name must be unique across the whole registry, including rows
     * not discovered in this run (DEPRECATED rows keep their historical name).
     * Evaluated on the state AFTER synchronization: discovered permissions take
     * their annotation name, every other row keeps its stored name. Any clash
     * fails startup; neither permission is overwritten.
     */
    private void verifyUniqueNames(
            Map<UUID, PermissionDefinition> discoveredById,
            List<Permission> existingPermissions
    ) {

        Map<String, UUID> idsByName = new HashMap<>();

        for (PermissionDefinition definition : discoveredById.values()) {
            claimName(idsByName, definition.name(), definition.id());
        }

        for (Permission existing : existingPermissions) {
            if (!discoveredById.containsKey(existing.getId()) && existing.getName() != null) {
                claimName(idsByName, existing.getName(), existing.getId());
            }
        }
    }

    private static void claimName(Map<String, UUID> idsByName, String name, UUID id) {

        UUID firstId = idsByName.putIfAbsent(name, id);

        if (firstId != null && !firstId.equals(id)) {
            throw new IllegalStateException(
                    "[SECURITY] Duplicate permission name | name=" + name
                            + " | firstId=" + firstId
                            + " | secondId=" + id
            );
        }
    }

    private void updatePermission(Permission existing, PermissionDefinition definition, LocalDateTime now) {

        boolean changed = false;

        if (!Objects.equals(existing.getName(), definition.name())) {
            log.warn("[SECURITY] Permission name changed | id={} | from={} | to={}",
                    existing.getId(), existing.getName(), definition.name());
            existing.setName(definition.name());
            changed = true;
        }

        if (!Objects.equals(existing.getDisplayName(), definition.displayName())) {
            existing.setDisplayName(definition.displayName());
            changed = true;
        }

        if (!Objects.equals(existing.getDescription(), definition.description())) {
            existing.setDescription(definition.description());
            changed = true;
        }

        if (!Objects.equals(existing.getBaseEntity(), definition.baseEntity())) {
            existing.setBaseEntity(definition.baseEntity());
            changed = true;
        }

        Set<String> newReachableEntities = new HashSet<>(definition.reachableEntities());

        if (!existing.getReachableEntitiesAsSet().equals(newReachableEntities)) {
            existing.setReachableEntitiesFromCollection(definition.reachableEntities());
            changed = true;
        }

        if (existing.getDomain() != definition.domain()) {
            log.warn("[SECURITY] Permission domain changed | id={} | from={} | to={}",
                    existing.getId(), existing.getDomain(), definition.domain());
            existing.setDomain(definition.domain());
            changed = true;
        }

        if (existing.getStatus() != PermissionStatus.ACTIVE) {
            existing.setStatus(PermissionStatus.ACTIVE);
            changed = true;
            log.info("[SECURITY] Permission reactivated | id={} | name={}",
                    existing.getId(), existing.getDisplayName());
        }

        existing.setLastSeenAt(now);

        if (changed) {
            existing.setUpdatedAt(now);
            log.info("[SECURITY] Permission updated | id={} | name={}",
                    existing.getId(), existing.getDisplayName());
        }
    }
}
