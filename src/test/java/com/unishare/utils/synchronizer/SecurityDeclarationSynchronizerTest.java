package com.unishare.utils.synchronizer;

import com.unishare.entity.rbac.GodAccount;
import com.unishare.entity.rbac.GodAccountRole;
import com.unishare.entity.rbac.GodRole;
import com.unishare.entity.rbac.GodRolePermission;
import com.unishare.entity.rbac.Permission;
import com.unishare.enums.rbac.PermissionDomain;
import com.unishare.enums.rbac.PermissionStatus;
import com.unishare.entity.rbac.Role;
import com.unishare.repository.auth.GodAccountRepository;
import com.unishare.repository.auth.GodAccountRoleRepository;
import com.unishare.repository.auth.GodRolePermissionRepository;
import com.unishare.repository.auth.GodRoleRepository;
import com.unishare.repository.auth.PermissionRepository;
import com.unishare.repository.auth.RoleRepository;
import com.unishare.repository.user.UserRepository;
import com.unishare.utils.scanner.GodAccountDefinition;
import com.unishare.utils.scanner.GodRoleDefinition;
import com.unishare.utils.scanner.SecurityDeclarations;
import com.unishare.utils.scanner.uniShareRoleDefinition;
import com.unishare.utils.seeder.SecuritySeedProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Backed by in-memory "tables" so repeated runs behave like repeated startups.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SecurityDeclarationSynchronizer")
class SecurityDeclarationSynchronizerTest {

    private static final UUID GOD_ACCOUNT_ID = UUID.fromString("01a0fe4b-f2ea-7000-8000-00000000000a");
    private static final UUID GOD_ROLE_ID = UUID.fromString("01a0fe4b-f2ea-7000-8000-00000000000b");
    private static final UUID MEMBER_ID = UUID.fromString("01a0fe4b-f2ea-7000-8000-00000000000c");
    private static final UUID STAFF_ID = UUID.fromString("01a0fe4b-f2ea-7000-8000-00000000000d");
    private static final UUID OTHER_ID = UUID.fromString("01a0fe4b-f2ea-7000-8000-0000000000ff");

    @Mock private GodAccountRepository godAccountRepository;
    @Mock private GodRoleRepository godRoleRepository;
    @Mock private GodAccountRoleRepository godAccountRoleRepository;
    @Mock private GodRolePermissionRepository godRolePermissionRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private SecurityDeclarationSynchronizer synchronizer;

    // in-memory tables
    private final Map<UUID, GodAccount> godAccounts = new LinkedHashMap<>();
    private final Map<UUID, GodRole> godRoles = new LinkedHashMap<>();
    private final Map<UUID, Role> roles = new LinkedHashMap<>();
    private final List<GodAccountRole> godAccountRoles = new ArrayList<>();
    private final Map<UUID, Permission> permissions = new LinkedHashMap<>();
    private final List<GodRolePermission> godRolePermissions = new ArrayList<>();

    private final SecuritySeedProperties properties = new SecuritySeedProperties();
    private SecuritySeedProperties.Seeding seeding;
    private SecurityDeclarations declarations;

    @BeforeEach
    void setUp() {

        synchronizer = new SecurityDeclarationSynchronizer(
                godAccountRepository, godRoleRepository, godAccountRoleRepository,
                godRolePermissionRepository, permissionRepository,
                roleRepository, userRepository, passwordEncoder, properties
        );
        properties.getGodAccount().setInitialPassword("initial-secret");
        seeding = properties.getSeeding();

        declarations = new SecurityDeclarations(
                new GodAccountDefinition(GOD_ACCOUNT_ID, "god", "god@example.com", "God"),
                new GodRoleDefinition(GOD_ROLE_ID, "ROOT", "root role"),
                List.of(
                        new uniShareRoleDefinition(MEMBER_ID, "MEMBER", "member role"),
                        new uniShareRoleDefinition(STAFF_ID, "STAFF", "staff role")
                )
        );

        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "bcrypt:" + inv.getArgument(0));

        when(godAccountRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(godAccounts.get(inv.<UUID>getArgument(0))));
        when(godAccountRepository.findAll()).thenAnswer(inv -> new ArrayList<>(godAccounts.values()));
        when(godAccountRepository.findByUsername(anyString())).thenAnswer(inv -> godAccounts.values().stream()
                .filter(a -> a.getUsername().equals(inv.getArgument(0))).findFirst());
        when(godAccountRepository.findByEmail(anyString())).thenAnswer(inv -> godAccounts.values().stream()
                .filter(a -> a.getEmail().equals(inv.getArgument(0))).findFirst());
        when(godAccountRepository.save(any())).thenAnswer(inv -> put(godAccounts, inv.<GodAccount>getArgument(0).getId(), inv.getArgument(0)));

        when(godRoleRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(godRoles.get(inv.<UUID>getArgument(0))));
        when(godRoleRepository.findAll()).thenAnswer(inv -> new ArrayList<>(godRoles.values()));
        when(godRoleRepository.findByName(anyString())).thenAnswer(inv -> godRoles.values().stream()
                .filter(r -> r.getName().equals(inv.getArgument(0))).findFirst());
        when(godRoleRepository.save(any())).thenAnswer(inv -> put(godRoles, inv.<GodRole>getArgument(0).getId(), inv.getArgument(0)));

        when(roleRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(roles.get(inv.<UUID>getArgument(0))));
        when(roleRepository.findByName(anyString())).thenAnswer(inv -> roles.values().stream()
                .filter(r -> r.getName().equals(inv.getArgument(0))).findFirst());
        when(roleRepository.save(any())).thenAnswer(inv -> put(roles, inv.<Role>getArgument(0).getId(), inv.getArgument(0)));

        when(permissionRepository.findByDomainAndStatus(any(), any())).thenAnswer(inv -> permissions.values().stream()
                .filter(p -> p.getDomain() == inv.getArgument(0) && p.getStatus() == inv.getArgument(1)).toList());

        AtomicLong grantIds = new AtomicLong();
        when(godRolePermissionRepository.findByGodRole(any())).thenAnswer(inv -> godRolePermissions.stream()
                .filter(g -> g.getGodRole().getId().equals(inv.<GodRole>getArgument(0).getId())).toList());
        when(godRolePermissionRepository.save(any())).thenAnswer(inv -> {
            GodRolePermission grant = inv.getArgument(0);
            grant.setId(grantIds.incrementAndGet());
            godRolePermissions.add(grant);
            return grant;
        });
        doAnswer(inv -> godRolePermissions.remove(inv.<GodRolePermission>getArgument(0)))
                .when(godRolePermissionRepository).delete(any());

        AtomicLong ids = new AtomicLong();
        when(godAccountRoleRepository.findByGodAccount(any())).thenAnswer(inv -> godAccountRoles.stream()
                .filter(gar -> gar.getGodAccount().getId().equals(inv.<GodAccount>getArgument(0).getId())).toList());
        when(godAccountRoleRepository.findByGodRole(any())).thenAnswer(inv -> godAccountRoles.stream()
                .filter(gar -> gar.getGodRole().getId().equals(inv.<GodRole>getArgument(0).getId())).toList());
        when(godAccountRoleRepository.save(any())).thenAnswer(inv -> {
            GodAccountRole gar = inv.getArgument(0);
            gar.setId(ids.incrementAndGet());
            godAccountRoles.add(gar);
            return gar;
        });
    }

    private static <T> T put(Map<UUID, T> table, UUID id, T row) {
        table.put(id, row);
        return row;
    }

    private void sync() {
        synchronizer.synchronize(declarations, seeding);
    }

    // ---------------------------------------------------------------- B. GodRole

    @Test
    @DisplayName("B. creates the GodRole in god_roles with the declared UUID")
    void createsGodRole() {

        sync();

        assertEquals(Set.of(GOD_ROLE_ID), godRoles.keySet());
        assertEquals("ROOT", godRoles.get(GOD_ROLE_ID).getName());
        assertEquals("root role", godRoles.get(GOD_ROLE_ID).getDescription());
    }

    @Test
    @DisplayName("B. updates GodRole metadata without changing its UUID")
    void updatesGodRole() {

        godRoles.put(GOD_ROLE_ID, GodRole.builder().id(GOD_ROLE_ID).name("ROOT").description("old").build());

        sync();

        assertEquals(1, godRoles.size());
        assertEquals("root role", godRoles.get(GOD_ROLE_ID).getDescription());
        assertEquals(GOD_ROLE_ID, godRoles.get(GOD_ROLE_ID).getId());
    }

    @Test
    @DisplayName("B. an undeclared GodRole fails startup")
    void undeclaredGodRoleFails() {

        godRoles.put(OTHER_ID, GodRole.builder().id(OTHER_ID).name("SHADOW").build());

        assertTrue(assertThrows(IllegalStateException.class, this::sync).getMessage().contains("Undeclared GodRole"));
    }

    @Test
    @DisplayName("B. GodRole name held by another UUID fails startup")
    void godRoleNameConflictFails() {

        godRoles.put(OTHER_ID, GodRole.builder().id(OTHER_ID).name("ROOT").build());

        assertThrows(IllegalStateException.class, this::sync);
        assertFalse(godRoles.containsKey(GOD_ROLE_ID));
    }

    // ---------------------------------------------------------------- C. GodAccount

    @Test
    @DisplayName("C. creates the GodAccount with a BCrypt-encoded initial password")
    void createsGodAccount() {

        sync();

        GodAccount account = godAccounts.get(GOD_ACCOUNT_ID);
        assertEquals("god", account.getUsername());
        assertEquals("god@example.com", account.getEmail());
        assertEquals("God", account.getDisplayName());
        assertEquals("bcrypt:initial-secret", account.getPassword());
        verify(passwordEncoder, times(1)).encode("initial-secret");
    }

    @Test
    @DisplayName("C. existing GodAccount: metadata synchronized, password untouched, initial password not required")
    void existingGodAccountKeepsPassword() {

        godAccounts.put(GOD_ACCOUNT_ID, GodAccount.builder()
                .id(GOD_ACCOUNT_ID).username("old").email("old@example.com")
                .displayName("Old").password("existing-hash").build());
        properties.getGodAccount().setInitialPassword(null);

        sync();

        GodAccount account = godAccounts.get(GOD_ACCOUNT_ID);
        assertEquals("god", account.getUsername());
        assertEquals("god@example.com", account.getEmail());
        assertEquals("existing-hash", account.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("C. missing initial password fails only when the GodAccount must be created")
    void missingInitialPasswordFails() {

        properties.getGodAccount().setInitialPassword(" ");

        IllegalStateException error = assertThrows(IllegalStateException.class, this::sync);

        assertTrue(error.getMessage().contains("GOD_ACCOUNT_INITIAL_PASSWORD"));
        assertTrue(godAccounts.isEmpty());
    }

    @Test
    @DisplayName("C. declared username owned by another GodAccount fails")
    void usernameConflictFails() {

        godAccounts.put(OTHER_ID, GodAccount.builder().id(OTHER_ID).username("god")
                .email("other@example.com").displayName("x").password("h").build());

        assertTrue(assertThrows(IllegalStateException.class, this::sync).getMessage().contains("username"));
        assertEquals(Set.of(OTHER_ID), godAccounts.keySet());
    }

    @Test
    @DisplayName("C. declared email owned by another GodAccount fails")
    void emailConflictFails() {

        godAccounts.put(OTHER_ID, GodAccount.builder().id(OTHER_ID).username("other")
                .email("god@example.com").displayName("x").password("h").build());

        assertTrue(assertThrows(IllegalStateException.class, this::sync).getMessage().contains("email"));
    }

    @Test
    @DisplayName("C. an undeclared GodAccount fails and is never deleted")
    void undeclaredGodAccountFails() {

        godAccounts.put(OTHER_ID, GodAccount.builder().id(OTHER_ID).username("x")
                .email("x@x").displayName("x").password("h").build());

        assertThrows(IllegalStateException.class, this::sync);
        assertTrue(godAccounts.containsKey(OTHER_ID));
        verify(godAccountRepository, never()).delete(any());
    }

    // ---------------------------------------------------------------- D. GodAccountRole

    @Test
    @DisplayName("D. creates GodAccount -> GodRole relationship")
    void createsGodAccountRole() {

        sync();

        assertEquals(1, godAccountRoles.size());
        assertEquals(GOD_ACCOUNT_ID, godAccountRoles.get(0).getGodAccount().getId());
        assertEquals(GOD_ROLE_ID, godAccountRoles.get(0).getGodRole().getId());
    }

    @Test
    @DisplayName("D. duplicate assignments fail without repair")
    void duplicateAssignmentsFail() {

        sync();
        godAccountRoles.add(GodAccountRole.builder()
                .godAccount(godAccounts.get(GOD_ACCOUNT_ID)).godRole(godRoles.get(GOD_ROLE_ID)).build());

        assertThrows(IllegalStateException.class, this::sync);
        assertEquals(2, godAccountRoles.size());
        verify(godAccountRoleRepository, never()).delete(any());
    }

    @Test
    @DisplayName("D. GodAccount holding an undeclared GodRole fails without repair")
    void conflictingGodRoleFails() {

        sync();
        godAccountRoles.get(0).setGodRole(GodRole.builder().id(OTHER_ID).name("SHADOW").build());

        assertThrows(IllegalStateException.class, this::sync);
        assertEquals(OTHER_ID, godAccountRoles.get(0).getGodRole().getId());
    }

    // ---------------------------------------------------------------- E. normal roles

    @Test
    @DisplayName("E. normal roles go to roles; the GodRole is never stored there")
    void normalRolesSeparated() {

        sync();

        assertEquals(Set.of(MEMBER_ID, STAFF_ID), roles.keySet());
        assertFalse(roles.containsKey(GOD_ROLE_ID));
        assertTrue(roles.values().stream().noneMatch(r -> r.getName().equals("ROOT")));
        assertFalse(godRoles.containsKey(MEMBER_ID));
        assertFalse(godRoles.containsKey(STAFF_ID));
    }

    @Test
    @DisplayName("E. updates normal role metadata, keeping its UUID")
    void updatesNormalRole() {

        roles.put(MEMBER_ID, Role.builder().id(MEMBER_ID).name("OLD").description("old").build());

        sync();

        assertEquals("MEMBER", roles.get(MEMBER_ID).getName());
        assertEquals("member role", roles.get(MEMBER_ID).getDescription());
    }

    @Test
    @DisplayName("E. normal role name held by another UUID fails")
    void normalRoleNameConflictFails() {

        roles.put(OTHER_ID, Role.builder().id(OTHER_ID).name("MEMBER").build());

        assertTrue(assertThrows(IllegalStateException.class, this::sync)
                .getMessage().contains("'MEMBER' is used by role " + OTHER_ID));
    }

    @Test
    @DisplayName("F. legacy GodRole row in the normal roles table fails startup, nothing migrated")
    void legacyGodRoleInRolesFails() {

        Role legacy = Role.builder().id(GOD_ROLE_ID).name("ROOT").build();
        roles.put(GOD_ROLE_ID, legacy);
        when(userRepository.countByRoles_Id(GOD_ROLE_ID)).thenReturn(1L);

        IllegalStateException error = assertThrows(IllegalStateException.class, this::sync);

        assertTrue(error.getMessage().contains("Legacy control-plane data"));
        assertTrue(error.getMessage().contains("1 user_roles row(s)"));
        assertSame(legacy, roles.get(GOD_ROLE_ID));
        assertTrue(godRoles.isEmpty());
        verify(roleRepository, never()).delete(any());
    }

    @Test
    @DisplayName("F. synchronization never touches normal users")
    void neverTouchesUsers() {

        sync();

        verify(userRepository, never()).save(any());
        verify(userRepository, never()).saveAll(any());
    }

    // ---------------------------------------------------------------- F/G/I. GodRole permissions

    private static final UUID CREATE_RIDE = UUID.fromString("0199c5c4-7b2a-7abc-9e31-4f5a7e8d2101");
    private static final UUID ROLE_CREATE = UUID.fromString("0199c5c4-7b2a-7abc-9e31-4f5a7e8d2102");
    private static final UUID AUDIT_VIEW = UUID.fromString("0199c5c4-7b2a-7abc-9e31-4f5a7e8d2103");

    private Permission permission(UUID id, String name, PermissionDomain domain) {
        Permission permission = Permission.builder()
                .id(id).displayName(name).baseEntity("x").domain(domain).status(PermissionStatus.ACTIVE).build();
        permissions.put(id, permission);
        return permission;
    }

    private Set<UUID> grantedToGodRole() {
        return godRolePermissions.stream()
                .peek(g -> assertEquals(GOD_ROLE_ID, g.getGodRole().getId()))
                .map(g -> g.getPermission().getId())
                .collect(java.util.stream.Collectors.toSet());
    }

    /** Full startup order: declarations -> (permissions already synced) -> god role permissions. */
    private void startup() {
        sync();
        synchronizer.synchronizeGodRolePermissions(declarations);
    }

    @Test
    @DisplayName("F. SUPERUSER gets CONTROL_PLANE permissions only, never APPLICATION")
    void godRoleGetsControlPlaneOnly() {

        permission(CREATE_RIDE, "Create Ride", PermissionDomain.APPLICATION);
        permission(ROLE_CREATE, "Role Create", PermissionDomain.CONTROL_PLANE);

        startup();

        assertEquals(Set.of(ROLE_CREATE), grantedToGodRole());
    }

    @Test
    @DisplayName("G. a new CONTROL_PLANE permission is granted on the next startup; a new APPLICATION one is not")
    void newPermissionsOnNextStartup() {

        permission(ROLE_CREATE, "Role Create", PermissionDomain.CONTROL_PLANE);
        startup();

        permission(AUDIT_VIEW, "Audit Log View", PermissionDomain.CONTROL_PLANE);
        permission(CREATE_RIDE, "Create Ride", PermissionDomain.APPLICATION);
        startup();

        assertEquals(Set.of(ROLE_CREATE, AUDIT_VIEW), grantedToGodRole());
    }

    @Test
    @DisplayName("I. APPLICATION -> CONTROL_PLANE domain change grants the permission")
    void domainChangeToControlPlaneGrants() {

        Permission p = permission(CREATE_RIDE, "Create Ride", PermissionDomain.APPLICATION);
        startup();
        assertTrue(grantedToGodRole().isEmpty());

        p.setDomain(PermissionDomain.CONTROL_PLANE);
        startup();

        assertEquals(Set.of(CREATE_RIDE), grantedToGodRole());
    }

    @Test
    @DisplayName("I. CONTROL_PLANE -> APPLICATION domain change revokes the grant, keeps the permission")
    void domainChangeToApplicationRevokes() {

        Permission p = permission(ROLE_CREATE, "Role Create", PermissionDomain.CONTROL_PLANE);
        startup();
        assertEquals(Set.of(ROLE_CREATE), grantedToGodRole());

        p.setDomain(PermissionDomain.APPLICATION);
        startup();

        assertTrue(grantedToGodRole().isEmpty());
        assertTrue(permissions.containsKey(ROLE_CREATE));
    }

    @Test
    @DisplayName("deprecated CONTROL_PLANE permission: grant revoked, permission kept; reactivation re-grants")
    void deprecatedControlPlanePermission() {

        Permission p = permission(ROLE_CREATE, "Role Create", PermissionDomain.CONTROL_PLANE);
        startup();

        p.setStatus(PermissionStatus.DEPRECATED);
        startup();
        assertTrue(grantedToGodRole().isEmpty());
        assertTrue(permissions.containsKey(ROLE_CREATE));

        p.setStatus(PermissionStatus.ACTIVE);
        startup();
        assertEquals(Set.of(ROLE_CREATE), grantedToGodRole());
    }

    @Test
    @DisplayName("H. GodRole permission sync is idempotent")
    void godRolePermissionsIdempotent() {

        permission(ROLE_CREATE, "Role Create", PermissionDomain.CONTROL_PLANE);
        permission(CREATE_RIDE, "Create Ride", PermissionDomain.APPLICATION);

        startup();
        clearInvocations(godRolePermissionRepository);
        startup();
        startup();

        assertEquals(1, godRolePermissions.size());
        verify(godRolePermissionRepository, never()).save(any());
        verify(godRolePermissionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("duplicate god_role_permissions rows fail without repair")
    void duplicateGrantsFail() {

        Permission p = permission(ROLE_CREATE, "Role Create", PermissionDomain.CONTROL_PLANE);
        startup();
        godRolePermissions.add(GodRolePermission.builder().godRole(godRoles.get(GOD_ROLE_ID)).permission(p).build());

        assertThrows(IllegalStateException.class, () -> synchronizer.synchronizeGodRolePermissions(declarations));
        assertEquals(2, godRolePermissions.size());
    }

    @Test
    @DisplayName("GodRole permission sync without a synchronized GodRole fails")
    void godRolePermissionsNeedGodRole() {
        assertThrows(IllegalStateException.class, () -> synchronizer.synchronizeGodRolePermissions(declarations));
    }

    // ---------------------------------------------------------------- G. idempotency

    @Test
    @DisplayName("G. a second startup creates and changes nothing")
    void idempotentSecondRun() {

        sync();

        String password = godAccounts.get(GOD_ACCOUNT_ID).getPassword();
        clearInvocations(godAccountRepository, godRoleRepository, godAccountRoleRepository, roleRepository, passwordEncoder);

        sync();

        assertEquals(1, godAccounts.size());
        assertEquals(1, godRoles.size());
        assertEquals(1, godAccountRoles.size());
        assertEquals(Set.of(MEMBER_ID, STAFF_ID), roles.keySet());
        assertEquals(password, godAccounts.get(GOD_ACCOUNT_ID).getPassword());

        verify(godAccountRepository, never()).save(any());
        verify(godRoleRepository, never()).save(any());
        verify(godAccountRoleRepository, never()).save(any());
        verify(roleRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("disabled seeding flags skip the corresponding writes")
    void respectsSeedingFlags() {

        seeding.setGodAccount(false);
        seeding.setGodRole(false);
        seeding.setGodAccountRole(false);

        sync();

        assertTrue(godAccounts.isEmpty());
        assertTrue(godRoles.isEmpty());
        assertTrue(godAccountRoles.isEmpty());
        assertEquals(2, roles.size());
    }
}
