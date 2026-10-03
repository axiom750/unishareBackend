package com.unishare.enums.rbac;

/**
 * Authorization domain of a {@link com.unishare.annotation.Permission}.
 *
 * APPLICATION   -> granted to normal Roles via role_permissions
 * CONTROL_PLANE -> granted to the GodRole via god_role_permissions
 *
 * Declared explicitly on @Permission; never inferred from names.
 * Persisted as STRING.
 */
public enum PermissionDomain {

    APPLICATION,

    CONTROL_PLANE
}
