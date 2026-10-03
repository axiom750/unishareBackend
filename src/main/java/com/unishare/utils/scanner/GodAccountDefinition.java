package com.unishare.utils.scanner;

import java.util.UUID;

/**
 * Immutable representation of a discovered @GodAccount declaration.
 * Scanned from UnishareApplication.class ONLY.
 */
public record GodAccountDefinition(
        UUID id,
        String username,
        String email,
        String displayName
) {
    /**
     * Compact constructor with validation.
     */
    public GodAccountDefinition {
        if (id == null) {
            throw new IllegalArgumentException("GodAccount ID cannot be null");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("GodAccount username cannot be blank");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("GodAccount email cannot be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("GodAccount displayName cannot be blank");
        }
    }
}
