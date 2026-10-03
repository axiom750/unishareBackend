package com.unishare.utils.scanner;

import java.util.List;
import java.util.UUID;

/**
 * Aggregate of all security declarations scanned from UnishareApplication.class.
 *
 * Two independent authorization domains:
 * - control plane: {@code godAccount} + {@code godRole}
 * - normal RBAC:   {@code roles} (@uniShareRole)
 *
 * Services consume this record; they never re-read annotations and never
 * declare role names or UUIDs themselves.
 *
 * godAccount / godRole are null and roles is empty when the corresponding
 * scanning flag is disabled.
 *
 * Normal-role declaration order is meaningful:
 * - the FIRST declared @uniShareRole is the default role for new users
 * - later declarations rank higher when choosing a user's primary role
 */
public record SecurityDeclarations(
        GodAccountDefinition godAccount,
        GodRoleDefinition godRole,
        List<uniShareRoleDefinition> roles
) {

    public SecurityDeclarations {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public static SecurityDeclarations empty() {
        return new SecurityDeclarations(null, null, List.of());
    }

    /**
     * The first declared @uniShareRole, used as the default role for new users.
     */
    public uniShareRoleDefinition defaultRole() {
        return requireRoles().get(0);
    }

    public GodAccountDefinition requireGodAccount() {
        if (godAccount == null) {
            throw new IllegalStateException(
                    "[SECURITY] @GodAccount was not scanned. "
                            + "Enable unishare.security.scanning.god-account."
            );
        }
        return godAccount;
    }

    public GodRoleDefinition requireGodRole() {
        if (godRole == null) {
            throw new IllegalStateException(
                    "[SECURITY] @GodRole was not scanned. "
                            + "Enable unishare.security.scanning.god-role."
            );
        }
        return godRole;
    }

    public List<uniShareRoleDefinition> requireRoles() {
        if (roles.isEmpty()) {
            throw new IllegalStateException(
                    "[SECURITY] @uniShareRole declarations were not scanned. "
                            + "Enable unishare.security.scanning.roles to use RBAC."
            );
        }
        return roles;
    }

    public boolean isNormalRole(UUID roleId) {
        return roles.stream().anyMatch(role -> role.id().equals(roleId));
    }
}
