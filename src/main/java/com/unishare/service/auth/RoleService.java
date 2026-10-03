package com.unishare.service.auth;

import com.unishare.UnishareApplication;
import com.unishare.annotation.GodRole;
import com.unishare.annotation.uniShareRole;
import com.unishare.entity.rbac.Role;
import com.unishare.repository.auth.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for managing roles in the RBAC system.
 * Queries roles from the database using IDs declared in @uniShareRole annotations.
 * 
 * Flow:
 *
 * @uniShareRole / @GodRole annotation
 *          ↓
 * UnishareApplication
 *          ↓
 * SecurityDeclarationSynchronizer
 *          ↓
 *     roles table
 *          ↓
 *     RoleService
 *          ↓
 *    Query by ID
 *          ↓
 *   Return Role entity
 * 
 * Why query by ID instead of name?
 * - UUIDs are immutable (frozen in annotations)
 * - Names can be changed in annotations without breaking existing data
 * - ID-based queries are more stable and performant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    /**
     * All normal roles declared by @uniShareRole.
     *
     * LinkedHashMap is intentionally used so declaration order
     * from UnishareApplication is preserved.
     */
    private static final Map<UUID, RoleDeclaration> DECLARED_ROLES =
            loadDeclaredRoles();

    /**
     * GodRole declaration.
     */
    private static final RoleDeclaration DECLARED_GOD_ROLE =
            loadGodRole();

    /**
     * Reads normal roles ONLY from UnishareApplication.
     */
    private static Map<UUID, RoleDeclaration> loadDeclaredRoles() {

        Map<UUID, RoleDeclaration> declarations =
                new LinkedHashMap<>();

        uniShareRole[] annotations =
                UnishareApplication.class
                        .getAnnotationsByType(
                                uniShareRole.class
                        );

        if (annotations.length == 0) {

            throw new IllegalStateException(
                    "[RBAC] No @uniShareRole declarations found on "
                            + UnishareApplication.class.getName()
            );
        }

        for (uniShareRole annotation : annotations) {

            UUID id;

            try {

                id = UUID.fromString(
                        annotation.id()
                );

            } catch (IllegalArgumentException exception) {

                throw new IllegalStateException(
                        "[RBAC] Invalid role UUID declared in @uniShareRole: "
                                + annotation.id(),
                        exception
                );
            }

            if (declarations.containsKey(id)) {

                throw new IllegalStateException(
                        "[RBAC] Duplicate @uniShareRole UUID declared: "
                                + id
                );
            }

            declarations.put(
                    id,
                    new RoleDeclaration(
                            id,
                            annotation.name(),
                            annotation.description()
                    )
            );
        }

        return Collections.unmodifiableMap(
                declarations
        );
    }

    /**
     * Reads @GodRole ONLY from UnishareApplication.
     */
    private static RoleDeclaration loadGodRole() {

        GodRole annotation =
                UnishareApplication.class
                        .getAnnotation(
                                GodRole.class
                        );

        if (annotation == null) {

            throw new IllegalStateException(
                    "[RBAC] @GodRole declaration is missing from "
                            + UnishareApplication.class.getName()
            );
        }

        UUID id;

        try {

            id = UUID.fromString(
                    annotation.id()
            );

        } catch (IllegalArgumentException exception) {

            throw new IllegalStateException(
                    "[RBAC] Invalid @GodRole UUID: "
                            + annotation.id(),
                    exception
            );
        }

        return new RoleDeclaration(
                id,
                annotation.name(),
                annotation.description()
        );
    }

    /**
     * Gets a role entity by its annotation-declared UUID.
     *
     * The UUID must originate from @uniShareRole or @GodRole.
     */
    @Transactional(readOnly = true)
    public Role getRoleById(UUID roleId) {

        if (!isDeclaredRole(roleId)) {

            throw new IllegalArgumentException(
                    "[RBAC] Role UUID is not declared by "
                            + UnishareApplication.class.getName()
                            + ": "
                            + roleId
            );
        }

        return roleRepository
                .findById(roleId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "[RBAC] Declared role does not exist "
                                        + "in the database: "
                                        + roleId
                                        + ". Security synchronization "
                                        + "must run before role lookup."
                        )
                );
    }

    /**
     * Gets the first declared normal role.
     *
     * IMPORTANT:
     *
     * Since the current @uniShareRole annotation does not contain
     * a separate "defaultForNewUsers" property, the first normal
     * @uniShareRole declaration is the default role declaration.
     *
     * Therefore, with:
     *
     * @uniShareRole(USER)
     * @uniShareRole(MODERATOR)
     * @uniShareRole(ADMIN)
     *
     * the first declaration is used for new users.
     *
     * No "USER" string is declared in this service.
     */
    @Transactional(readOnly = true)
    public Set<Role> getDefaultUserRoles() {

        if (DECLARED_ROLES.isEmpty()) {

            throw new IllegalStateException(
                    "[RBAC] No normal roles declared on "
                            + UnishareApplication.class.getName()
            );
        }

        UUID defaultRoleId =
                DECLARED_ROLES
                        .keySet()
                        .iterator()
                        .next();

        Role defaultRole =
                getRoleById(defaultRoleId);

        Set<Role> roles =
                new HashSet<>();

        roles.add(defaultRole);

        log.debug(
                "[RBAC] Default role resolved from annotation | id={} | name={}",
                defaultRole.getId(),
                defaultRole.getName()
        );

        return roles;
    }

    /**
     * Checks whether the supplied role belongs to the set of
     * annotation-declared normal roles.
     */
    public boolean isDeclaredNormalRole(
            UUID roleId
    ) {

        return roleId != null
                && DECLARED_ROLES.containsKey(roleId);
    }

    /**
     * Checks whether the supplied role is the declared GodRole.
     */
    public boolean isDeclaredGodRole(
            UUID roleId
    ) {

        return roleId != null
                && DECLARED_GOD_ROLE.id().equals(roleId);
    }

    /**
     * Checks whether a UUID is declared anywhere in the security
     * role declarations.
     */
    public boolean isDeclaredRole(
            UUID roleId
    ) {

        return isDeclaredNormalRole(roleId)
                || isDeclaredGodRole(roleId);
    }

    /**
     * Checks whether a user has a role by UUID.
     *
     * UUID is the stable role identity.
     */
    public boolean hasRole(
            Set<Role> userRoles,
            UUID roleId
    ) {

        if (userRoles == null
                || userRoles.isEmpty()
                || roleId == null) {

            return false;
        }

        return userRoles.stream()
                .map(Role::getId)
                .anyMatch(roleId::equals);
    }

    /**
     * Checks whether a user has a role using the name that
     * originated from the caller's declared/runtime data.
     *
     * This does not define any role name itself.
     */
    public boolean hasRole(
            Set<Role> userRoles,
            String roleName
    ) {

        if (userRoles == null
                || userRoles.isEmpty()
                || roleName == null) {

            return false;
        }

        return userRoles.stream()
                .anyMatch(role ->
                        roleName.equals(
                                role.getName()
                        )
                );
    }

    /**
     * Returns the primary role name for JWT generation.
     *
     * There is NO hardcoded role priority here.
     *
     * Primary-role ordering follows the declaration order
     * on UnishareApplication.
     *
     * GodRole is considered according to its own declaration
     * and is placed before normal roles.
     */
    public String getPrimaryRoleName(
            Set<Role> userRoles
    ) {

        if (userRoles == null
                || userRoles.isEmpty()) {

            throw new IllegalStateException(
                    "[RBAC] Cannot determine primary role: "
                            + "user has no roles"
            );
        }

        /*
         * GodRole is the root control-plane role.
         *
         * Its identity comes from @GodRole.
         */
        for (Role role : userRoles) {

            if (DECLARED_GOD_ROLE.id()
                    .equals(role.getId())) {

                return role.getName();
            }
        }

        /*
         * For normal roles, use the order in which they are
         * declared on UnishareApplication.
         */
        for (UUID declaredRoleId :
                DECLARED_ROLES.keySet()) {

            for (Role userRole :
                    userRoles) {

                if (declaredRoleId.equals(
                        userRole.getId()
                )) {

                    return userRole.getName();
                }
            }
        }

        /*
         * A user's role must always originate from the
         * annotation-driven synchronized role set.
         *
         * Therefore an undeclared role is an invariant violation.
         */
        throw new IllegalStateException(
                "[RBAC] User contains a role that is not declared "
                        + "on "
                        + UnishareApplication.class.getName()
        );
    }

    /**
     * Returns the annotation declaration for a normal role.
     */
    public Optional<RoleDeclaration>
    getDeclaredRole(
            UUID roleId
    ) {

        return Optional.ofNullable(
                DECLARED_ROLES.get(roleId)
        );
    }

    /**
     * Returns the @GodRole declaration.
     */
    public RoleDeclaration getDeclaredGodRole() {

        return DECLARED_GOD_ROLE;
    }

    /**
     * Returns all normal role declarations.
     *
     * The returned list follows the declaration order
     * from UnishareApplication.
     */
    public List<RoleDeclaration>
    getDeclaredRoles() {

        return List.copyOf(
                DECLARED_ROLES.values()
        );
    }

    /**
     * Gets the annotation-declared role ID by its declared name.
     *
     * The name is NOT declared in this service.
     *
     * This method exists only for compatibility with code that
     * already receives a role name externally.
     */
    public Optional<UUID>
    getRoleIdFromAnnotation(
            String roleName
    ) {

        if (roleName == null) {
            return Optional.empty();
        }

        for (RoleDeclaration declaration :
                DECLARED_ROLES.values()) {

            if (roleName.equals(
                    declaration.name()
            )) {

                return Optional.of(
                        declaration.id()
                );
            }
        }

        if (roleName.equals(
                DECLARED_GOD_ROLE.name()
        )) {

            return Optional.of(
                    DECLARED_GOD_ROLE.id()
            );
        }

        return Optional.empty();
    }

    /**
     * Finds a database role by name.
     *
     * This should NOT be used to discover whether a role exists.
     *
     * Role existence is determined by annotation declarations.
     *
     * Kept only for compatibility with existing code.
     */
    @Transactional(readOnly = true)
    public Optional<Role>
    findRoleByName(
            String roleName
    ) {

        return roleRepository.findByName(
                roleName
        );
    }

    /**
     * Immutable runtime representation of a role declaration.
     *
     * Values originate directly from annotations.
     */
    public record RoleDeclaration(
            UUID id,
            String name,
            String description
    ) {
    }
}
