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

    private static PermissionDefinition definition(UUID id, String name, List<String> reachable) {
        return definition(id, name, reachable, PermissionDomain.APPLICATION);
    }

    private static PermissionDefinition definition(UUID id, String name, List<String> reachable, PermissionDomain domain) {
        return new PermissionDefinition(id, name, name + " description", "rides", reachable, domain);
    }

    private static Permission stored(UUID id, String name, PermissionStatus status) {
        LocalDateTime then = LocalDateTime.now().minusDays(1);
        Permission permission = Permission.builder()
                .id(id).displayName(name).description(name + " description").baseEntity("rides")
                .status(status).createdAt(then).updatedAt(then).build();
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

    @Test
    @DisplayName("conflicting definitions for one UUID fail")
    void conflictingDefinitionsFail() {

        assertThrows(IllegalStateException.class, () -> synchronizer.synchronize(List.of(
                definition(PERMISSION_ID, "A", List.of()),
                definition(PERMISSION_ID, "B", List.of())
        )));
    }
}
