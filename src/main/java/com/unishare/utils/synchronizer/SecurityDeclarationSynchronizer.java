package com.unishare.utils.synchronizer;

import com.unishare.entity.rbac.GodAccount;
import com.unishare.entity.rbac.GodAccountRole;
import com.unishare.entity.rbac.GodRole;
import com.unishare.entity.rbac.GodRolePermission;
import com.unishare.entity.rbac.Permission;
import com.unishare.entity.rbac.Role;
import com.unishare.repository.auth.GodAccountRepository;
import com.unishare.repository.auth.GodAccountRoleRepository;
import com.unishare.repository.auth.GodRolePermissionRepository;
import com.unishare.repository.auth.GodRoleRepository;
import com.unishare.repository.auth.PermissionRepository;
import com.unishare.repository.auth.RoleRepository;
import com.unishare.repository.user.UserRepository;
import com.unishare.enums.rbac.PermissionDomain;
import com.unishare.enums.rbac.PermissionStatus;
import com.unishare.utils.scanner.GodAccountDefinition;
import com.unishare.utils.scanner.GodRoleDefinition;
import com.unishare.utils.scanner.SecurityDeclarations;
import com.unishare.utils.scanner.uniShareRoleDefinition;
import com.unishare.utils.seeder.SecuritySeedProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * SOLE lifecycle owner of GodAccount, GodRole, GodAccountRole and
 * GodRolePermission, and the declaration-driven synchronizer for normal RBAC
 * roles.
 *
 * Consumes {@link SecurityDeclarations}; never scans annotations and never
 * reads identity from YAML. Two independent paths:
 *
 * Control plane:  GodAccount -> GodRole (god_roles) -> GodAccountRole
 *                 GodRole -> god_role_permissions -> Permission(CONTROL_PLANE)
 * Normal RBAC:    Role (roles)  -- knows nothing about GodRole
 *
 * {@link #synchronize} runs before permission synchronization;
 * {@link #synchronizeGodRolePermissions} runs after it, so grants only ever
 * reference persisted permissions.
 *
 * Identity is the frozen UUID. Entities are never deleted (only stale
 * god_role_permissions grant rows are revoked). Ambiguous or corrupt
 * state (identity conflicts, extra GodAccounts/GodRoles, conflicting
 * assignments, control-plane data inside the normal roles table) fails
 * startup instead of being repaired.
 *
 * GodAccount password: read ONLY when the declared GodAccount does not exist
 * yet, from GOD_ACCOUNT_INITIAL_PASSWORD (bound as
 * unishare.security.god-account.initial-password), BCrypt-encoded.
 * Existing password hashes are never read, replaced or required.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityDeclarationSynchronizer {

    private final GodAccountRepository godAccountRepository;
    private final GodRoleRepository godRoleRepository;
    private final GodAccountRoleRepository godAccountRoleRepository;
    private final GodRolePermissionRepository godRolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecuritySeedProperties properties;

    @Transactional
    public void synchronize(
            SecurityDeclarations declarations,
            SecuritySeedProperties.Seeding seeding
    ) {

        log.info("[SECURITY] Synchronization started");

        if (declarations.godRole() != null) {
            verifyGodRoleAbsentFromNormalRoles(declarations.godRole());
        }

        GodAccount godAccount = null;
        if (seeding.shouldSeedGodAccount()) {
            godAccount = synchronizeGodAccount(declarations.requireGodAccount());
            log.info("[SECURITY] GodAccount synchronized");
        }

        GodRole godRole = null;
        if (seeding.shouldSeedGodRole()) {
            godRole = synchronizeGodRole(declarations.requireGodRole());
            log.info("[SECURITY] GodRole synchronized | name={}", godRole.getName());
        }

        if (seeding.shouldSeedRoles()) {
            for (uniShareRoleDefinition definition : declarations.requireRoles()) {
                synchronizeRole(definition);
            }
            log.info("[SECURITY] Normal roles synchronized | count={}", declarations.roles().size());
        }

        if (seeding.shouldSeedGodAccountRole()) {
            synchronizeGodAccountRole(
                    require(godAccount, "GodAccount"),
                    require(godRole, "GodRole")
            );
            log.info("[SECURITY] GodAccountRole synchronized");
        }

        log.info("[SECURITY] Synchronization completed");
    }

    // ------------------------------------------------------------------ GodRole permissions

    /**
     * Reconciles god_role_permissions so the declared GodRole holds EXACTLY
     * the ACTIVE CONTROL_PLANE permissions:
     *
     * - missing grants are created (new CONTROL_PLANE permission, or
     *   APPLICATION -> CONTROL_PLANE domain change, or reactivation)
     * - grants for permissions that are no longer eligible are revoked
     *   (CONTROL_PLANE -> APPLICATION domain change, or DEPRECATED). Only the
     *   grant row is removed; the permission itself is kept per the
     *   DEPRECATED lifecycle policy.
     * - APPLICATION permissions are never granted to the GodRole.
     *
     * Must run after PermissionSynchronizer.
     */
    @Transactional
    public void synchronizeGodRolePermissions(SecurityDeclarations declarations) {

        GodRoleDefinition definition = declarations.requireGodRole();

        GodRole godRole = godRoleRepository.findById(definition.id())
                .orElseThrow(() -> new IllegalStateException(
                        "[SECURITY] Declared @GodRole " + definition.id() + " does not exist in god_roles. "
                                + "GodRole seeding must run before GodRole permission synchronization."
                ));

        Map<UUID, Permission> eligible = new HashMap<>();
        for (Permission permission : permissionRepository.findByDomainAndStatus(
                PermissionDomain.CONTROL_PLANE, PermissionStatus.ACTIVE)) {
            eligible.put(permission.getId(), permission);
        }

        Map<UUID, GodRolePermission> granted = new HashMap<>();
        int revoked = 0;

        for (GodRolePermission grant : godRolePermissionRepository.findByGodRole(godRole)) {

            Permission permission = grant.getPermission();

            if (granted.putIfAbsent(permission.getId(), grant) != null) {
                throw new IllegalStateException(
                        "[SECURITY] Duplicate god_role_permissions rows for GodRole " + godRole.getId()
                                + " and permission " + permission.getId() + ". Resolve manually."
                );
            }

            if (!eligible.containsKey(permission.getId())) {
                godRolePermissionRepository.delete(grant);
                revoked++;
                log.warn("[SECURITY] Control-plane grant revoked | godRole={} | permissionId={} | domain={} | status={}",
                        godRole.getName(), permission.getId(), permission.getDomain(), permission.getStatus());
            }
        }

        int added = 0;

        for (Permission permission : eligible.values()) {

            if (granted.containsKey(permission.getId())) {
                continue;
            }

            godRolePermissionRepository.save(
                    GodRolePermission.builder()
                            .godRole(godRole)
                            .permission(permission)
                            .build()
            );
            added++;

            log.info("[SECURITY] Control-plane permission granted | godRole={} | permissionId={} | name={}",
                    godRole.getName(), permission.getId(), permission.getDisplayName());
        }

        log.info("[SECURITY] Control-plane permissions synchronized | godRole={} | assigned={} | added={} | revoked={}",
                godRole.getName(), eligible.size(), added, revoked);
    }

    // ------------------------------------------------------------------ isolation

    /**
     * The GodRole must never exist in the normal roles table (legacy schema
     * stored SUPERUSER there). user_roles / role_permissions can only point at
     * roles rows, so checking roles covers them too. Never migrated silently.
     */
    private void verifyGodRoleAbsentFromNormalRoles(GodRoleDefinition godRole) {

        Optional<Role> legacy = roleRepository.findById(godRole.id())
                .or(() -> roleRepository.findByName(godRole.name()));

        if (legacy.isEmpty()) {
            return;
        }

        Role role = legacy.get();

        throw new IllegalStateException(
                "[SECURITY] Legacy control-plane data in the normal RBAC schema: roles row "
                        + role.getId() + " ('" + role.getName() + "') matches the declared @GodRole "
                        + godRole.id() + ". It is referenced by "
                        + userRepository.countByRoles_Id(role.getId()) + " user_roles row(s) and "
                        + role.getPermissions().size() + " role_permissions row(s). "
                        + "The GodRole lives in god_roles only; remove these rows "
                        + "(or recreate the schema) before starting."
        );
    }

    // ------------------------------------------------------------------ GodAccount

    private GodAccount synchronizeGodAccount(GodAccountDefinition definition) {

        verifyNoConflictingGodAccount(definition);

        Optional<GodAccount> existing = godAccountRepository.findById(definition.id());

        if (existing.isPresent()) {

            GodAccount godAccount = existing.get();
            boolean changed = false;

            if (!Objects.equals(godAccount.getUsername(), definition.username())) {
                godAccount.setUsername(definition.username());
                changed = true;
            }

            if (!Objects.equals(godAccount.getEmail(), definition.email())) {
                godAccount.setEmail(definition.email());
                changed = true;
            }

            if (!Objects.equals(godAccount.getDisplayName(), definition.displayName())) {
                godAccount.setDisplayName(definition.displayName());
                changed = true;
            }

            if (changed) {
                godAccountRepository.save(godAccount);
                log.info("[SECURITY] GodAccount metadata updated (password preserved) | id={}",
                        definition.id());
            }

            return godAccount;
        }

        String initialPassword = properties.getGodAccount().getInitialPassword();

        if (initialPassword == null || initialPassword.isBlank()) {
            throw new IllegalStateException(
                    "[SECURITY] GodAccount " + definition.id() + " does not exist and "
                            + "GOD_ACCOUNT_INITIAL_PASSWORD is not set. "
                            + "Set it for the first startup to create the GodAccount."
            );
        }

        GodAccount godAccount = GodAccount.builder()
                .id(definition.id())
                .username(definition.username())
                .email(definition.email())
                .password(passwordEncoder.encode(initialPassword))
                .displayName(definition.displayName())
                .active(true)
                .build();

        godAccountRepository.save(godAccount);

        log.info("[SECURITY] GodAccount created | id={}", definition.id());

        return godAccount;
    }

    /**
     * Exactly one GodAccount may exist, and the declared username/email must
     * not belong to a different id. Conflicts are never merged or deleted.
     */
    private void verifyNoConflictingGodAccount(GodAccountDefinition definition) {

        godAccountRepository.findByUsername(definition.username())
                .filter(other -> !other.getId().equals(definition.id()))
                .ifPresent(other -> {
                    throw new IllegalStateException(
                            "[SECURITY] Declared @GodAccount username is used by another GodAccount "
                                    + other.getId() + " (declared id " + definition.id() + ")."
                    );
                });

        godAccountRepository.findByEmail(definition.email())
                .filter(other -> !other.getId().equals(definition.id()))
                .ifPresent(other -> {
                    throw new IllegalStateException(
                            "[SECURITY] Declared @GodAccount email is used by another GodAccount "
                                    + other.getId() + " (declared id " + definition.id() + ")."
                    );
                });

        List<UUID> others = godAccountRepository.findAll().stream()
                .map(GodAccount::getId)
                .filter(id -> !id.equals(definition.id()))
                .toList();

        if (!others.isEmpty()) {
            throw new IllegalStateException(
                    "[SECURITY] Undeclared GodAccount(s) exist: " + others
                            + ". Only the declared @GodAccount " + definition.id() + " may exist."
            );
        }
    }

    // ------------------------------------------------------------------ GodRole

    private GodRole synchronizeGodRole(GodRoleDefinition definition) {

        godRoleRepository.findByName(definition.name())
                .filter(other -> !other.getId().equals(definition.id()))
                .ifPresent(other -> {
                    throw new IllegalStateException(
                            "[SECURITY] @GodRole name '" + definition.name() + "' is used by god_roles row "
                                    + other.getId() + " but is declared with id " + definition.id() + "."
                    );
                });

        List<UUID> others = godRoleRepository.findAll().stream()
                .map(GodRole::getId)
                .filter(id -> !id.equals(definition.id()))
                .toList();

        if (!others.isEmpty()) {
            throw new IllegalStateException(
                    "[SECURITY] Undeclared GodRole(s) exist: " + others
                            + ". Only the declared @GodRole " + definition.id() + " may exist."
            );
        }

        Optional<GodRole> existing = godRoleRepository.findById(definition.id());

        if (existing.isPresent()) {

            GodRole godRole = existing.get();
            boolean changed = false;

            if (!Objects.equals(godRole.getName(), definition.name())) {
                godRole.setName(definition.name());
                changed = true;
            }

            if (!Objects.equals(godRole.getDescription(), definition.description())) {
                godRole.setDescription(definition.description());
                changed = true;
            }

            if (changed) {
                godRoleRepository.save(godRole);
                log.info("[SECURITY] GodRole updated | id={} | name={}", definition.id(), definition.name());
            }

            return godRole;
        }

        GodRole godRole = GodRole.builder()
                .id(definition.id())
                .name(definition.name())
                .description(definition.description())
                .build();

        godRoleRepository.save(godRole);

        log.info("[SECURITY] GodRole created | id={} | name={}", definition.id(), definition.name());

        return godRole;
    }

    // ------------------------------------------------------------------ normal roles

    /**
     * Creates or updates a normal RBAC role by its frozen UUID. Name and
     * description are mutable metadata; identity is never decided by name.
     */
    private void synchronizeRole(uniShareRoleDefinition definition) {

        UUID id = definition.id();

        roleRepository.findByName(definition.name())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalStateException(
                            "[SECURITY] @uniShareRole name '" + definition.name() + "' is used by role "
                                    + other.getId() + " but is declared with id " + id
                                    + ". Role identity is the UUID; migrate the conflicting row manually."
                    );
                });

        Optional<Role> existing = roleRepository.findById(id);

        if (existing.isPresent()) {

            Role role = existing.get();
            boolean changed = false;

            if (!Objects.equals(role.getName(), definition.name())) {
                log.warn("[SECURITY] Role renamed from declaration | id={} | from={} | to={}",
                        id, role.getName(), definition.name());
                role.setName(definition.name());
                changed = true;
            }

            if (!Objects.equals(role.getDescription(), definition.description())) {
                role.setDescription(definition.description());
                changed = true;
            }

            if (changed) {
                roleRepository.save(role);
                log.info("[SECURITY] Role updated | id={} | name={}", id, definition.name());
            }

            return;
        }

        roleRepository.save(
                Role.builder()
                        .id(id)
                        .name(definition.name())
                        .description(definition.description())
                        .build()
        );

        log.info("[SECURITY] Role created | id={} | name={}", id, definition.name());
    }

    // ------------------------------------------------------------------ GodAccountRole

    /**
     * Ensures exactly: declared GodAccount -> GodAccountRole -> declared GodRole.
     */
    private void synchronizeGodAccountRole(GodAccount godAccount, GodRole godRole) {

        List<GodAccountRole> accountRoles = godAccountRoleRepository.findByGodAccount(godAccount);

        if (accountRoles.isEmpty()) {

            godAccountRoleRepository.save(
                    GodAccountRole.builder()
                            .godAccount(godAccount)
                            .godRole(godRole)
                            .build()
            );

            log.info("[SECURITY] GodAccountRole created | godAccountId={} | godRoleId={}",
                    godAccount.getId(), godRole.getId());

        } else if (accountRoles.size() > 1
                || !accountRoles.get(0).getGodRole().getId().equals(godRole.getId())) {

            throw new IllegalStateException(
                    "[SECURITY] GodAccount " + godAccount.getId()
                            + " must hold exactly the declared @GodRole " + godRole.getId()
                            + ", found " + accountRoles.size()
                            + " conflicting assignment(s). Resolve god_account_roles manually."
            );
        }

        long holders = godAccountRoleRepository.findByGodRole(godRole).size();

        if (holders != 1) {
            throw new IllegalStateException(
                    "[SECURITY] The declared @GodRole " + godRole.getId()
                            + " must belong to exactly one GodAccount, found " + holders
                            + ". Resolve god_account_roles manually."
            );
        }
    }

    private static <T> T require(T value, String what) {
        if (value == null) {
            throw new IllegalStateException(
                    "[SECURITY] " + what + " is required for the enabled seeding step but was not available"
            );
        }
        return value;
    }
}
