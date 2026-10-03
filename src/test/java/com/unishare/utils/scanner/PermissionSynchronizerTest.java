package com.unishare.utils.scanner;

import com.unishare.entity.rbac.Permission;
import com.unishare.entity.rbac.Role;
import com.unishare.enums.rbac.PermissionDomain;
import com.unishare.enums.rbac.PermissionStatus;
import com.unishare.repository.auth.PermissionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Permission synchronization (normal RBAC only).
 *
 * - frozen UUIDs, metadata updated in place
 * - undiscovered permissions deprecated, never deleted
 * - role -> permission grants are never changed automatically
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionSynchronizer")
class PermissionSynchronizerTest {

    private static final UUID MEMBER_ID = UUID.fromString("01a0fe4b-f2ea-7000-8000-00000000000c");
    private static final UUID PERMISSION_ID = UUID.fromString("0199c5c4-7b2a-7abc-9e31-4f5a7e8d21c4");
    private static final UUID OTHER_PERMISSION_ID = UUID.fromString("0199c5c4-7b2a-7abc-9e31-4f5a7e8d21c5");

    @Mock private PermissionRepository permissionRepository;

    @InjectMocks private PermissionSynchronizer synchronizer;

    /** Test-only stand-in for the @PreAuthorize authority: "Create Ride" -> "CREATE_RIDE". */
    private static String authority(String displayName) {
        return displayName.toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private static PermissionDefinition definition(UUID id, String displayName, List<String> reachable) {
        return definition(id, displayName, reachable, PermissionDomain.APPLICATION);
    }

    private static PermissionDefinition definition(UUID id, String displayName, List<String> reachable, PermissionDomain domain) {
        return new PermissionDefinition(id, authority(displayName), displayName, displayName + " description",
                "rides", reachable, domain);
    }

    private static Permission stored(UUID id, String displayName, PermissionStatus status) {
        LocalDateTime then = LocalDateTime.now().minusDays(1);
        Permission permission = Permission.builder()
                .id(id).name(authority(displayName)).displayName(displayName).description(displayName + " description")
                .baseEntity("rides").status(status).createdAt(then).updatedAt(then).build();
        permission.setReachableEntitiesFromCollection(List.of());
        return permission;
    }

    @Test
    @DisplayName("new permission is registered with its frozen UUID and no automatic grants")
    void newPermissionRegistered() {

        when(permissionRepository.findAll()).thenReturn(List.of());

        synchronizer.synchronize(List.of(definition(PERMISSION_ID, "Create Ride", List.of("rides", "users"))));

        verify(permissionRepository).save(argThat(p ->
                p.getId().equals(PERMISSION_ID)
                        && p.getStatus() == PermissionStatus.ACTIVE
                        && p.getReachableEntitiesAsSet().equals(Set.of("rides", "users"))
                        && p.getRoles().isEmpty()));
    }

    @Test
    @DisplayName("existing permission metadata is updated in place and reachable entities replaced")
    void existingPermissionUpdated() {

        Permission existing = stored(PERMISSION_ID, "Old Name", PermissionStatus.ACTIVE);
        existing.setReachableEntitiesFromCollection(List.of("legacy"));

        when(permissionRepository.findAll()).thenReturn(List.of(existing));

        synchronizer.synchronize(List.of(definition(PERMISSION_ID, "Create Ride", List.of("users"))));

        assertEquals("Create Ride", existing.getDisplayName());
        assertEquals(Set.of("users"), existing.getReachableEntitiesAsSet());
        assertNotNull(existing.getLastSeenAt());
        verify(permissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("undiscovered permission is deprecated, not deleted; existing grants untouched")
    void undiscoveredPermissionDeprecated() {

        Permission orphan = stored(PERMISSION_ID, "Old", PermissionStatus.ACTIVE);
        Role member = Role.builder().id(MEMBER_ID).name("MEMBER").build();
        member.getPermissions().add(orphan);

        when(permissionRepository.findAll()).thenReturn(List.of(orphan));

        synchronizer.synchronize(List.of());

        assertEquals(PermissionStatus.DEPRECATED, orphan.getStatus());
        assertTrue(member.getPermissions().contains(orphan));
        verify(permissionRepository, never()).delete(any());
        verify(permissionRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deprecated permission that reappears is reactivated")
    void deprecatedPermissionReactivated() {

        Permission deprecated = stored(PERMISSION_ID, "Create Ride", PermissionStatus.DEPRECATED);

        when(permissionRepository.findAll()).thenReturn(List.of(deprecated));

        synchronizer.synchronize(List.of(definition(PERMISSION_ID, "Create Ride", List.of())));

        assertEquals(PermissionStatus.ACTIVE, deprecated.getStatus());
    }

    @Test
    @DisplayName("second run with unchanged permissions writes nothing (idempotent)")
    void idempotent() {

        Permission existing = stored(PERMISSION_ID, "Create Ride", PermissionStatus.ACTIVE);
        LocalDateTime updatedAt = existing.getUpdatedAt();

        when(permissionRepository.findAll()).thenReturn(List.of(existing));

        synchronizer.synchronize(List.of(definition(PERMISSION_ID, "Create Ride", List.of())));

        assertEquals(updatedAt, existing.getUpdatedAt());
        verify(permissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("D. domain from the annotation is persisted on new permissions")
    void domainPersistedOnCreate() {

        when(permissionRepository.findAll()).thenReturn(List.of());

        synchronizer.synchronize(List.of(
                definition(PERMISSION_ID, "Create Ride", List.of()),
                definition(OTHER_PERMISSION_ID, "Role Create", List.of(), PermissionDomain.CONTROL_PLANE)
        ));

        verify(permissionRepository).save(argThat(p ->
                p.getId().equals(PERMISSION_ID) && p.getDomain() == PermissionDomain.APPLICATION));
        verify(permissionRepository).save(argThat(p ->
                p.getId().equals(OTHER_PERMISSION_ID) && p.getDomain() == PermissionDomain.CONTROL_PLANE));
    }

    @Test
    @DisplayName("I. domain change on the annotation updates the existing permission in place")
    void domainChangeUpdatesPermission() {

        Permission existing = stored(PERMISSION_ID, "Create Ride", PermissionStatus.ACTIVE);
        LocalDateTime updatedAt = existing.getUpdatedAt();

        when(permissionRepository.findAll()).thenReturn(List.of(existing));

        synchronizer.synchronize(List.of(
                definition(PERMISSION_ID, "Create Ride", List.of(), PermissionDomain.CONTROL_PLANE)));

        assertEquals(PermissionDomain.CONTROL_PLANE, existing.getDomain());
        assertNotEquals(updatedAt, existing.getUpdatedAt());
        assertEquals(PERMISSION_ID, existing.getId());
    }

    // ---------------------------------------------------------------- Permission.name

    @Test
    @DisplayName("7. PermissionDefinition.name is persisted as Permission.name")
    void namePersisted() {

        when(permissionRepository.findAll()).thenReturn(List.of());

        synchronizer.synchronize(List.of(new PermissionDefinition(PERMISSION_ID, "RIDE_CREATE", "Create Ride",
                "Allows creating a ride", "rides", List.of(), PermissionDomain.APPLICATION)));

        verify(permissionRepository).save(argThat(p ->
                p.getId().equals(PERMISSION_ID)
                        && "RIDE_CREATE".equals(p.getName())
                        && "Create Ride".equals(p.getDisplayName())
                        && "Allows creating a ride".equals(p.getDescription())
                        && p.getDomain() == PermissionDomain.APPLICATION
                        && p.getStatus() == PermissionStatus.ACTIVE));
    }

    @Test
    @DisplayName("8. two runs over a stateful registry leave exactly one row with the same name")
    void idempotentWithName() {

        Map<UUID, Permission> table = new LinkedHashMap<>();
        when(permissionRepository.findAll()).thenAnswer(inv -> new ArrayList<>(table.values()));
        when(permissionRepository.save(any())).thenAnswer(inv -> {
            Permission p = inv.getArgument(0);
            table.put(p.getId(), p);
            return p;
        });

        PermissionDefinition rideCreate = new PermissionDefinition(PERMISSION_ID, "RIDE_CREATE", "Create Ride",
                "Allows creating a ride", "rides", List.of(), PermissionDomain.APPLICATION);

        synchronizer.synchronize(List.of(rideCreate));
        synchronizer.synchronize(List.of(rideCreate));

        assertEquals(1, table.size());
        assertEquals("RIDE_CREATE", table.get(PERMISSION_ID).getName());
        verify(permissionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("name changed on @PreAuthorize updates the same row in place (UUID is identity)")
    void nameChangeUpdatesInPlace() {

        Permission existing = stored(PERMISSION_ID, "Create Ride", PermissionStatus.ACTIVE);
        when(permissionRepository.findAll()).thenReturn(List.of(existing));

        synchronizer.synchronize(List.of(new PermissionDefinition(PERMISSION_ID, "RIDE_CREATE_V2", "Create Ride",
                "Create Ride description", "rides", List.of(), PermissionDomain.APPLICATION)));

        assertEquals("RIDE_CREATE_V2", existing.getName());
        assertEquals(PERMISSION_ID, existing.getId());
        verify(permissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("6. new id claiming a name already held by another stored row fails; nothing overwritten")
    void duplicateNameWithStoredRowFails() {

        Permission existing = stored(PERMISSION_ID, "Create Ride", PermissionStatus.ACTIVE);
        when(permissionRepository.findAll()).thenReturn(List.of(existing));

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> synchronizer.synchronize(List.of(
                definition(PERMISSION_ID, "Create Ride", List.of()),
                new PermissionDefinition(OTHER_PERMISSION_ID, "CREATE_RIDE", "Another", "x", "rides",
                        List.of(), PermissionDomain.APPLICATION)
        )));

        assertTrue(error.getMessage().contains("Duplicate permission name"), error.getMessage());
        assertTrue(error.getMessage().contains("name=CREATE_RIDE"), error.getMessage());
        assertTrue(error.getMessage().contains(PERMISSION_ID.toString()), error.getMessage());
        assertTrue(error.getMessage().contains(OTHER_PERMISSION_ID.toString()), error.getMessage());
        assertEquals("CREATE_RIDE", existing.getName());
        verify(permissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("6. a DEPRECATED row keeps its name; a new id reusing it fails")
    void deprecatedNameIsReserved() {

        Permission deprecated = stored(PERMISSION_ID, "Create Ride", PermissionStatus.DEPRECATED);
        when(permissionRepository.findAll()).thenReturn(List.of(deprecated));

        assertThrows(IllegalStateException.class, () -> synchronizer.synchronize(List.of(
                new PermissionDefinition(OTHER_PERMISSION_ID, "CREATE_RIDE", "Create Ride", "x", "rides",
                        List.of(), PermissionDomain.APPLICATION))));

        assertEquals(PermissionStatus.DEPRECATED, deprecated.getStatus());
        verify(permissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("10. removed permission becomes DEPRECATED and keeps its name")
    void deprecatedKeepsName() {

        Permission existing = stored(PERMISSION_ID, "Create Ride", PermissionStatus.ACTIVE);
        when(permissionRepository.findAll()).thenReturn(List.of(existing));

        synchronizer.synchronize(List.of());

        assertEquals(PermissionStatus.DEPRECATED, existing.getStatus());
        assertEquals("CREATE_RIDE", existing.getName());
    }

    @Test
    @DisplayName("conflicting definitions for one UUID fail")
    void conflictingDefinitionsFail() {

        assertThrows(IllegalStateException.class, () -> synchronizer.synchronize(List.of(
                definition(PERMISSION_ID, "A", List.of()),
                definition(PERMISSION_ID, "B", List.of())
        )));
    }
}
