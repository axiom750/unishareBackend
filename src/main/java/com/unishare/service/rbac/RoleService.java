package com.unishare.service.rbac;

import com.unishare.entity.rbac.Role;
import com.unishare.repository.auth.RoleRepository;
import com.unishare.utils.scanner.SecurityDeclarations;
import com.unishare.utils.scanner.uniShareRoleDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Runtime service for NORMAL RBAC roles only (@uniShareRole -> roles).
 *
 * Knows nothing about the control-plane GodRole/GodAccount and is read-only:
 * it never creates roles and never declares role names, UUIDs, descriptions
 * or priorities. Identity comes from {@link SecurityDeclarations}; entities
 * come from the database after synchronization.
 *
 * Declaration order on UnishareApplication:
 * - the first @uniShareRole is the default role for new users
 * - later @uniShareRole declarations rank higher for the primary role
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final SecurityDeclarations declarations;

    /**
     * Loads a declared normal role entity by its frozen UUID.
     */
    @Transactional(readOnly = true)
    public Role getRoleById(UUID roleId) {

        if (!declarations.isNormalRole(roleId)) {
            throw new IllegalArgumentException(
                    "[RBAC] Role UUID is not a declared @uniShareRole: " + roleId
            );
        }

        return roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalStateException(
                        "[RBAC] Declared role " + roleId + " does not exist in the database. "
                                + "Security seeding must run before role lookup."
                ));
    }

    /**
     * Roles assigned to every new normal user: the first declared @uniShareRole.
     */
    @Transactional(readOnly = true)
    public Set<Role> getDefaultUserRoles() {

        Role defaultRole = getRoleById(declarations.defaultRole().id());

        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);

        return roles;
    }

    public boolean hasRole(Set<Role> userRoles, UUID roleId) {

        if (userRoles == null || roleId == null) {
            return false;
        }

        return userRoles.stream()
                .map(Role::getId)
                .anyMatch(roleId::equals);
    }

    public boolean hasRole(Set<Role> userRoles, String roleName) {

        if (userRoles == null || roleName == null) {
            return false;
        }

        return userRoles.stream()
                .map(Role::getName)
                .anyMatch(roleName::equals);
    }

    /**
     * Primary role name used for the JWT role claim.
     *
     * Order: normal roles from last declared to first declared, then (for
     * undeclared legacy roles) alphabetical. A user without roles gets the
     * default role name.
     */
    public String getPrimaryRoleName(Set<Role> userRoles) {

        if (userRoles == null || userRoles.isEmpty()) {
            return declarations.defaultRole().name();
        }

        List<uniShareRoleDefinition> declared = declarations.requireRoles();

        for (int i = declared.size() - 1; i >= 0; i--) {
            UUID declaredId = declared.get(i).id();
            for (Role role : userRoles) {
                if (declaredId.equals(role.getId())) {
                    return role.getName();
                }
            }
        }

        return userRoles.stream()
                .map(Role::getName)
                .sorted()
                .findFirst()
                .orElseThrow();
    }
}
