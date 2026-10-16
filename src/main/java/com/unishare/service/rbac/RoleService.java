
package com.unishare.service.rbac;

import com.unishare.entity.rbac.Permission;
import com.unishare.entity.rbac.Role;
import com.unishare.repository.rbac.RoleRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final SecurityDeclarations declarations;

    @Transactional(readOnly = true)
    public Role getRoleById(UUID roleId) {

        if (!declarations.isNormalRole(roleId)) {
            throw new IllegalArgumentException(
                    "[RBAC] Role UUID is not a declared @uniShareRole: "
                            + roleId
            );
        }

        return roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalStateException(
                        "[RBAC] Declared role " + roleId
                                + " does not exist in the database. "
                                + "Security seeding must run before role lookup."
                ));
    }

    @Transactional(readOnly = true)
    public Set<Role> getDefaultUserRoles() {

        Role defaultRole = getRoleById(
                declarations.defaultRole().id()
        );

        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);

        return roles;
    }

    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {

        return declarations.requireRoles()
                .stream()
                .map(role -> roleRepository.findById(role.id())
                        .orElseThrow(() -> new IllegalStateException(
                                "[RBAC] Declared role " + role.id()
                                        + " does not exist in the database. "
                                        + "Security seeding must run before role lookup."
                        )))
                .toList();
    }

    @Transactional(readOnly = true)
    public Set<Permission> getRolePermissions(UUID roleId) {

        Role role = getRoleById(roleId);

        return new HashSet<>(role.getPermissions());
    }

    /**
     * Replaces all permissions assigned to a declared normal role.
     */
    @Transactional
    public void replaceRolePermissions(
            UUID roleId,
            Set<Permission> permissions
    ) {

        if (permissions == null) {
            throw new IllegalArgumentException(
                    "Permissions must not be null"
            );
        }

        Role role = getRoleById(roleId);

        role.getPermissions().clear();
        role.getPermissions().addAll(permissions);

        roleRepository.save(role);

        log.info(
                "[RBAC] Permissions replaced for role {} ({} permissions)",
                roleId,
                permissions.size()
        );
    }

    /**
     * Revokes only the specified permissions from a normal role.
     * All other assigned permissions remain unchanged.
     *
     * @return IDs of the permissions that were actually revoked
     */
    @Transactional
    public Set<UUID> revokeRolePermissions(
            UUID roleId,
            Set<UUID> permissionIds
    ) {

        if (roleId == null) {
            throw new IllegalArgumentException("Role ID is required");
        }

        if (permissionIds == null || permissionIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one permission ID is required"
            );
        }

        Role role = getRoleById(roleId);

        Set<Permission> assignedPermissions = role.getPermissions();
        Set<UUID> revokedIds = new HashSet<>();

        for (Permission permission : assignedPermissions) {
            if (permissionIds.contains(permission.getId())) {
                revokedIds.add(permission.getId());
            }
        }

        if (revokedIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "None of the supplied permissions are assigned to role "
                            + roleId
            );
        }

        assignedPermissions.removeIf(
                permission -> revokedIds.contains(permission.getId())
        );

        roleRepository.save(role);

        log.info(
                "[RBAC] Revoked {} permission(s) from role {}",
                revokedIds.size(),
                roleId
        );

        return revokedIds;
    }

    public boolean hasRole(Set<Role> userRoles, UUID roleId) {

        if (userRoles == null || userRoles.isEmpty()) {
            return false;
        }

        return userRoles.stream()
                .anyMatch(role -> role.getId().equals(roleId));
    }

    public boolean hasRole(Set<Role> userRoles, String roleName) {

        if (userRoles == null || userRoles.isEmpty()) {
            return false;
        }

        return userRoles.stream()
                .anyMatch(role ->
                        roleName.equalsIgnoreCase(role.getName())
                );
    }

    @Transactional(readOnly = true)
    public String getPrimaryRoleNameByUserId(Long userId) {

        List<Role> userRoles = roleRepository.findRolesByUserId(userId);

        return getPrimaryRoleName(new HashSet<>(userRoles));
    }

    public String getPrimaryRoleName(Set<Role> userRoles) {

        if (userRoles == null || userRoles.isEmpty()) {
            return declarations.defaultRole().name();
        }

        List<uniShareRoleDefinition> declared =
                declarations.requireRoles();

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
