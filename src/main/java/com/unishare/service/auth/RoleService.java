package com.unishare.service.auth;

import com.unishare.entity.auth.Role;
import com.unishare.repository.auth.RoleRepository;
import com.unishare.utils.seeder.DefaultRoles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing roles in the RBAC system.
 * Uses frozen UUID v7 identifiers from DefaultRoles.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    /**
     * Get a role by its frozen UUID.
     * This is the primary way to retrieve system roles.
     *
     * @param roleId The frozen UUID v7 of the role
     * @return The Role entity
     * @throws IllegalStateException if role does not exist
     */
    @Transactional(readOnly = true)
    public Role getRoleById(UUID roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalStateException(
                    String.format("[RBAC] Role not found | id=%s", roleId)
                ));
    }

    /**
     * Get the USER role using its frozen UUID.
     *
     * @return USER role
     */
    @Transactional(readOnly = true)
    public Role getUserRole() {
        return getRoleById(DefaultRoles.USER_ID);
    }

    /**
     * Get the MODERATOR role using its frozen UUID.
     *
     * @return MODERATOR role
     */
    @Transactional(readOnly = true)
    public Role getModeratorRole() {
        return getRoleById(DefaultRoles.MODERATOR_ID);
    }

    /**
     * Get the ADMIN role using its frozen UUID.
     *
     * @return ADMIN role
     */
    @Transactional(readOnly = true)
    public Role getAdminRole() {
        return getRoleById(DefaultRoles.ADMIN_ID);
    }

    /**
     * Get the SUPERUSER role using its frozen UUID.
     *
     * @return SUPERUSER role
     */
    @Transactional(readOnly = true)
    public Role getSuperuserRole() {
        return getRoleById(DefaultRoles.SUPERUSER_ID);
    }

    /**
     * Get the default role set for new users (USER role).
     *
     * @return Set containing the default USER role
     */
    @Transactional(readOnly = true)
    public Set<Role> getDefaultUserRoles() {
        Role userRole = getUserRole();
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        log.debug("[RBAC] Assigned default USER role");
        return roles;
    }

    /**
     * Check if a user has a specific role by name.
     *
     * @param userRoles The user's roles
     * @param roleName The role name to check
     * @return true if user has the role
     */
    public boolean hasRole(Set<Role> userRoles, String roleName) {
        if (userRoles == null || userRoles.isEmpty()) {
            return false;
        }
        return userRoles.stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    /**
     * Check if a user has a specific role by UUID.
     *
     * @param userRoles The user's roles
     * @param roleId The frozen role UUID
     * @return true if user has the role
     */
    public boolean hasRole(Set<Role> userRoles, UUID roleId) {
        if (userRoles == null || userRoles.isEmpty()) {
            return false;
        }
        return userRoles.stream()
                .anyMatch(role -> role.getId().equals(roleId));
    }

    /**
     * Check if a user has USER role.
     */
    public boolean isUser(Set<Role> userRoles) {
        return hasRole(userRoles, DefaultRoles.USER_ID);
    }

    /**
     * Check if a user has MODERATOR role.
     */
    public boolean isModerator(Set<Role> userRoles) {
        return hasRole(userRoles, DefaultRoles.MODERATOR_ID);
    }

    /**
     * Check if a user has ADMIN role.
     */
    public boolean isAdmin(Set<Role> userRoles) {
        return hasRole(userRoles, DefaultRoles.ADMIN_ID);
    }

    /**
     * Check if a user has SUPERUSER role.
     */
    public boolean isSuperuser(Set<Role> userRoles) {
        return hasRole(userRoles, DefaultRoles.SUPERUSER_ID);
    }

    /**
     * Get the primary role name for backward compatibility.
     * Returns highest priority role, or "USER" if no roles.
     * 
     * Priority: SUPERUSER > ADMIN > MODERATOR > USER
     * 
     * Note: This is a migration helper for JWT claims.
     *
     * @param userRoles The user's roles
     * @return Primary role name
     */
    public String getPrimaryRoleName(Set<Role> userRoles) {
        if (userRoles == null || userRoles.isEmpty()) {
            return DefaultRoles.USER;
        }
        
        // Priority: SUPERUSER > ADMIN > MODERATOR > USER
        if (hasRole(userRoles, DefaultRoles.SUPERUSER_ID)) {
            return DefaultRoles.SUPERUSER;
        }
        if (hasRole(userRoles, DefaultRoles.ADMIN_ID)) {
            return DefaultRoles.ADMIN;
        }
        if (hasRole(userRoles, DefaultRoles.MODERATOR_ID)) {
            return DefaultRoles.MODERATOR;
        }
        if (hasRole(userRoles, DefaultRoles.USER_ID)) {
            return DefaultRoles.USER;
        }
        
        // Fallback to first role alphabetically
        return userRoles.stream()
                .map(Role::getName)
                .sorted()
                .findFirst()
                .orElse(DefaultRoles.USER);
    }
}
