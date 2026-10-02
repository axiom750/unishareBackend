package com.unishare.utils.scanner;

import com.unishare.entity.auth.Permission;
import com.unishare.entity.auth.Role;
import com.unishare.enums.auth.PermissionStatus;
import com.unishare.repository.auth.PermissionRepository;
import com.unishare.repository.auth.RoleRepository;
import com.unishare.utils.seeder.DefaultRoles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for PermissionSynchronizer - AUTOMATIC SUPERUSER PERMISSION ASSIGNMENT
 * 
 * CRITICAL REQUIREMENTS:
 * 1. New permission → SUPERUSER mapping created
 * 2. New permission → USER mapping NOT created
 * 3. New permission → MODERATOR mapping NOT created
 * 4. New permission → ADMIN mapping NOT created
 * 5. Existing mapping is not duplicated (idempotent)
 * 6. Existing role-permission mappings are not modified
 * 7. Permission synchronization remains idempotent
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionSynchronizer - SUPERUSER Auto-Assignment Tests")
class PermissionSynchronizerTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private PermissionSynchronizer synchronizer;

    private Role superuserRole;
    private Role userRole;
    private Role moderatorRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        // Create frozen role instances
        superuserRole = Role.builder()
                .id(DefaultRoles.SUPERUSER_ID)
                .name(DefaultRoles.SUPERUSER)
                .permissions(new HashSet<>())
                .build();

        userRole = Role.builder()
                .id(DefaultRoles.USER_ID)
                .name(DefaultRoles.USER)
                .permissions(new HashSet<>())
                .build();

        moderatorRole = Role.builder()
                .id(DefaultRoles.MODERATOR_ID)
                .name(DefaultRoles.MODERATOR)
                .permissions(new HashSet<>())
                .build();

        adminRole = Role.builder()
                .id(DefaultRoles.ADMIN_ID)
                .name(DefaultRoles.ADMIN)
                .permissions(new HashSet<>())
                .build();
    }

    @Test
    @DisplayName("NEW PERMISSION → SUPERUSER mapping created")
    void testNewPermissionAssignedToSuperuser() {
        // Arrange
        UUID permissionId = UUID.fromString("0199c5c4-7b2a-7abc-9e31-4f5a7e8d21c4");
        PermissionDefinition newPermission = new PermissionDefinition(
                permissionId,
                "Create Ride",
                "Allows creating a new ride",
                "rides",
                List.of("rides", "users", "vehicles")
        );

        when(permissionRepository.findAll()).thenReturn(List.of());
        when(roleRepository.findById(DefaultRoles.SUPERUSER_ID)).thenReturn(Optional.of(superuserRole));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        synchronizer.synchronize(List.of(newPermission));

        // Assert
        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(roleCaptor.capture());

        Role savedRole = roleCaptor.getValue();
        assertEquals(DefaultRoles.SUPERUSER_ID, savedRole.getId());
        assertEquals(1, savedRole.getPermissions().size());

        Permission assignedPermission = savedRole.getPermissions().iterator().next();
        assertEquals(permissionId, assignedPermission.getId());
        assertEquals("Create Ride", assignedPermission.getDisplayName());
    }

    @Test
    @DisplayName("NEW PERMISSION → USER mapping NOT created")
    void testNewPermissionNotAssignedToUser() {
        // Arrange
        UUID permissionId = UUID.fromString("0199c5c4-7b2a-7abc-9e31-4f5a7e8d21c5");
        PermissionDefinition newPermission = new PermissionDefinition(
                permissionId,
                "Delete Item",
                "Allows deleting an item",
                "items",
                List.of("items")
        );

        when(permissionRepository.findAll()).thenReturn(List.of());
        when(roleRepository.findById(DefaultRoles.SUPERUSER_ID)).thenReturn(Optional.of(superuserRole));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        synchronizer.synchronize(List.of(newPermission));

        // Assert
        // Verify USER role was NEVER accessed
        verify(roleRepository, never()).findById(DefaultRoles.USER_ID);
        
        // Verify only SUPERUSER role was saved
        verify(roleRepository, times(1)).save(any(Role.class));
        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(roleCaptor.capture());
        assertEquals(DefaultRoles.SUPERUSER_ID, roleCaptor.getValue().getId());
    }

    @Test
    @DisplayName("NEW PERMISSION → MODERATOR mapping NOT created")
    void testNewPermissionNotAssignedToModerator() {
        // Arrange
        UUID permissionId = UUID.fromString("0199c5c4-7b2a-7abc-9e31-4f5a7e8d21c6");
        PermissionDefinition newPermission = new PermissionDefinition(
                permissionId,
                "Update Profile",
                "Allows updating user profile",
                "users",
                List.of("users")
        );

        when(permissionRepository.findAll()).thenReturn(List.of());
        when(roleRepository.findById(DefaultRoles.SUPERUSER_ID)).thenReturn(Optional.of(superuserRole));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        synchronizer.synchronize(List.of(newPermission));

        // Assert
        // Verify MODERATOR role was NEVER accessed
        verify(roleRepository, never()).findById(DefaultRoles.MODERATOR_ID);
        
        // Verify only SUPERUSER role was saved
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    @DisplayName("NEW PERMISSION → ADMIN mapping NOT created")
    void testNewPermissionNotAssignedToAdmin() {
        // Arrange
        UUID permissionId = UUID.fromString("0199c5c4-7b2a-7abc-9e31-4f5a7e8d21c7");
        PermissionDefinition newPermission = new PermissionDefinition(
                permissionId,
                "View Reports",
                "Allows viewing system reports",
                "reports",
                List.of("reports")
        );

        when(permissionRepository.findAll()).thenReturn(List.of());
        when(roleRepository.findById(DefaultRoles.SUPERUSER_ID)).thenReturn(Optional.of(superuserRole));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        synchronizer.synchronize(List.of(newPermission));

        // Assert
        // Verify ADMIN role was NEVER accessed
        verify(roleRepository, never()).findById(DefaultRoles.ADMIN_ID);
        
        // Verify only SUPERUSER role was saved
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    @DisplayName("Existing mapping is NOT duplicated (idempotent)")
    void testExistingMappingNotDuplicated() {
        // Arrange
        UUID permissionId = UUID.fromString("0199c5c4-7b2a-7abc-9e31-4f5a7e8d21c8");
        
        Permission existingPermission = Permission.builder()
                .id(permissionId)
                .displayName("Create Ride")
                .description("Allows creating a new ride")
                .baseEntity("rides")
                .status(PermissionStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .lastSeenAt(LocalDateTime.now())
                .roles(new HashSet<>())
                .build();
        existingPermission.setReachableEntitiesFromCollection(List.of("rides", "users"));

        // Permission ALREADY assigned to SUPERUSER
        superuserRole.getPermissions().add(existingPermission);
        existingPermission.getRoles().add(superuserRole);

        PermissionDefinition discoveredPermission = new PermissionDefinition(
                permissionId,
                "Create Ride",
                "Allows creating a new ride",
                "rides",
                List.of("rides", "users")
        );

        when(permissionRepository.findAll()).thenReturn(List.of(existingPermission));
        when(roleRepository.findById(DefaultRoles.SUPERUSER_ID)).thenReturn(Optional.of(superuserRole));

        // Act
        synchronizer.synchronize(List.of(discoveredPermission));

        // Assert
        // Verify roleRepository.save() was NEVER called (no new assignment needed)
        verify(roleRepository, never()).save(any(Role.class));
        
        // Verify SUPERUSER still has exactly 1 permission (not duplicated)
        assertEquals(1, superuserRole.getPermissions().size());
    }

    @Test
    @DisplayName("Existing role-permission mappings are NOT modified")
    void testExistingMappingsNotModified() {
        // Arrange
        UUID permission1Id = UUID.fromString("0199c5c4-7b2a-7abc-9e31-4f5a7e8d21c9");
        UUID permission2Id = UUID.fromString("0199c5c4-7b2a-7abc-9e31-4f5a7e8d21ca");

        // Existing permission assigned to USER (manually assigned by admin)
        Permission existingUserPermission = Permission.builder()
                .id(permission1Id)
                .displayName("View Dashboard")
                .description("View user dashboard")
                .baseEntity("dashboards")
                .status(PermissionStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .lastSeenAt(LocalDateTime.now())
                .roles(new HashSet<>())
                .build();
        existingUserPermission.setReachableEntitiesFromCollection(List.of("dashboards"));

        // Manually assigned to USER role
        userRole.getPermissions().add(existingUserPermission);
        existingUserPermission.getRoles().add(userRole);

        // New permission being discovered
        PermissionDefinition newPermission = new PermissionDefinition(
                permission2Id,
                "Create Post",
                "Create a new post",
                "posts",
                List.of("posts")
        );

        when(permissionRepository.findAll()).thenReturn(List.of(existingUserPermission));
        when(roleRepository.findById(DefaultRoles.SUPERUSER_ID)).thenReturn(Optional.of(superuserRole));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        synchronizer.synchronize(List.of(
                new PermissionDefinition(
                        permission1Id,
                        "View Dashboard",
                        "View user dashboard",
                        "dashboards",
                        List.of("dashboards")
                ),
                newPermission
        ));

        // Assert
        // Verify USER role still has the existing permission
        assertTrue(userRole.getPermissions().contains(existingUserPermission));
        assertEquals(1, userRole.getPermissions().size());

        // Verify NEW permission was assigned ONLY to SUPERUSER
        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(roleCaptor.capture());
        assertEquals(DefaultRoles.SUPERUSER_ID, roleCaptor.getValue().getId());

        // Verify USER role was NOT saved (no changes to USER)
        verify(roleRepository, never()).findById(DefaultRoles.USER_ID);
    }

    @Test
    @DisplayName("Permission synchronization remains idempotent")
    void testSynchronizationIsIdempotent() {
        // Arrange
        UUID permissionId = UUID.fromString("0199c5c4-7b2a-7abc-9e31-4f5a7e8d21cb");
        PermissionDefinition permission = new PermissionDefinition(
                permissionId,
                "Upload File",
                "Upload a file to storage",
                "files",
                List.of("files")
        );

        when(permissionRepository.findAll())
                .thenReturn(List.of())
                .thenReturn(List.of()); // Second call

        when(roleRepository.findById(DefaultRoles.SUPERUSER_ID)).thenReturn(Optional.of(superuserRole));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act - Run synchronization TWICE
        synchronizer.synchronize(List.of(permission));
        
        // Reset mocks to track second call
        reset(permissionRepository, roleRepository);
        
        // Create existing permission for second run
        Permission existingPermission = Permission.builder()
                .id(permissionId)
                .displayName("Upload File")
                .description("Upload a file to storage")
                .baseEntity("files")
                .status(PermissionStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .lastSeenAt(LocalDateTime.now())
                .roles(new HashSet<>())
                .build();
        existingPermission.setReachableEntitiesFromCollection(List.of("files"));
        
        superuserRole.getPermissions().add(existingPermission);
        existingPermission.getRoles().add(superuserRole);
        
        when(permissionRepository.findAll()).thenReturn(List.of(existingPermission));
        when(roleRepository.findById(DefaultRoles.SUPERUSER_ID)).thenReturn(Optional.of(superuserRole));
        
        synchronizer.synchronize(List.of(permission));

        // Assert - Second run should NOT create new mappings
        verify(roleRepository, never()).save(any(Role.class));
        verify(permissionRepository, never()).save(any(Permission.class));
    }

    @Test
    @DisplayName("SUPERUSER role not found - CRITICAL FAILURE (not graceful)")
    void testSuperuserRoleNotFound() {
        // Arrange
        UUID permissionId = UUID.fromString("0199c5c4-7b2a-7abc-9e31-4f5a7e8d21cc");
        PermissionDefinition newPermission = new PermissionDefinition(
                permissionId,
                "Delete User",
                "Delete a user account",
                "users",
                List.of("users")
        );

        when(permissionRepository.findAll()).thenReturn(List.of());
        when(roleRepository.findById(DefaultRoles.SUPERUSER_ID)).thenReturn(Optional.empty());

        // Act & Assert - Should throw IllegalStateException (critical failure)
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> synchronizer.synchronize(List.of(newPermission))
        );

        // Verify error message
        assertTrue(exception.getMessage().contains("SUPERUSER role not found"));
        assertTrue(exception.getMessage().contains("CRITICAL"));
        
        // Verify permission was NOT saved (transaction should fail)
        verify(permissionRepository, never()).save(any(Permission.class));
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("Multiple new permissions - all assigned to SUPERUSER only")
    void testMultipleNewPermissionsAssignedToSuperuserOnly() {
        // Arrange
        UUID perm1Id = UUID.fromString("0199c5c4-7b2a-7abc-9e31-4f5a7e8d21cd");
        UUID perm2Id = UUID.fromString("0199c5c4-7b2a-7abc-9e31-4f5a7e8d21ce");
        UUID perm3Id = UUID.fromString("0199c5c4-7b2a-7abc-9e31-4f5a7e8d21cf");

        List<PermissionDefinition> newPermissions = List.of(
                new PermissionDefinition(perm1Id, "Create Event", "Create event", "events", List.of("events")),
                new PermissionDefinition(perm2Id, "Update Event", "Update event", "events", List.of("events")),
                new PermissionDefinition(perm3Id, "Delete Event", "Delete event", "events", List.of("events"))
        );

        when(permissionRepository.findAll()).thenReturn(List.of());
        when(roleRepository.findById(DefaultRoles.SUPERUSER_ID)).thenReturn(Optional.of(superuserRole));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        synchronizer.synchronize(newPermissions);

        // Assert
        // Verify SUPERUSER was saved 3 times (once per permission)
        verify(roleRepository, times(3)).save(any(Role.class));
        
        // Verify NO other roles were accessed
        verify(roleRepository, never()).findById(DefaultRoles.USER_ID);
        verify(roleRepository, never()).findById(DefaultRoles.MODERATOR_ID);
        verify(roleRepository, never()).findById(DefaultRoles.ADMIN_ID);
        
        // Verify SUPERUSER has all 3 permissions
        assertEquals(3, superuserRole.getPermissions().size());
    }
}
