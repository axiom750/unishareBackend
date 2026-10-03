package com.unishare.utils.scanner;

import java.util.UUID;

/**
 * Immutable representation of a discovered @uniShareRole declaration.
 * Scanned from UnishareApplication.class ONLY.
 */
public record uniShareRoleDefinition(
        UUID id,
        String name,
        String description
) {
    /**
     * Compact constructor with validation.
     */
    public uniShareRoleDefinition {
        if (id == null) {
            throw new IllegalArgumentException("uniShareRole ID cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("uniShareRole name cannot be blank");
        }
        if (description == null) {
            description = "";
        }
    }
}
