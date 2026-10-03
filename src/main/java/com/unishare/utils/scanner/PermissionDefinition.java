package com.unishare.utils.scanner;

import com.unishare.enums.rbac.PermissionDomain;

import java.util.List;
import java.util.UUID;

/**
 * Immutable representation of a discovered permission from controller scanning.
 * This is NOT a JPA entity - it's the scanner's output.
 *
 * {@code name} is the canonical authority extracted from the handler's
 * {@code @PreAuthorize("hasAuthority('...')")}; {@code displayName} is the UI label.
 */
public record PermissionDefinition(
        UUID id,
        String name,
        String displayName,
        String description,
        String baseEntity,
        List<String> reachableEntities,
        PermissionDomain domain
) {
    /**
     * Compact constructor with validation.
     */
    public PermissionDefinition {
        if (id == null) {
            throw new IllegalArgumentException("Permission ID cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Permission name cannot be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Permission displayName cannot be blank");
        }
        if (domain == null) {
            throw new IllegalArgumentException("Permission domain cannot be null");
        }
        if (baseEntity == null || baseEntity.isBlank()) {
            throw new IllegalArgumentException("Permission baseEntity cannot be blank");
        }
        // Ensure immutable defensive copy
        reachableEntities = reachableEntities == null ? List.of() : List.copyOf(reachableEntities);
    }
}
