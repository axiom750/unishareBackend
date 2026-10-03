package com.unishare.utils.scanner;

import java.util.UUID;

/**
 * Immutable representation of a discovered @GodRole declaration.
 * Scanned from UnishareApplication.class ONLY.
 */
public record GodRoleDefinition(
        UUID id,
        String name,
        String description
) {
    /**
     * Compact constructor with validation.
     */
    public GodRoleDefinition {
        if (id == null) {
            throw new IllegalArgumentException("GodRole ID cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("GodRole name cannot be blank");
        }
        if (description == null) {
            description = "";
        }
    }
}
