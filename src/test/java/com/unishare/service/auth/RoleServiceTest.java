package com.unishare.service.auth;

import com.unishare.entity.rbac.Role;
import com.unishare.repository.auth.RoleRepository;
import com.unishare.service.rbac.RoleService;
import com.unishare.utils.scanner.SecurityDeclarationScanner;
import com.unishare.utils.scanner.SecurityDeclarations;
import com.unishare.utils.scanner.uniShareRoleDefinition;
import com.unishare.utils.seeder.SecuritySeedProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Uses the real declarations scanned from UnishareApplication.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService")
class RoleServiceTest {

    @Mock private RoleRepository roleRepository;

    private SecurityDeclarations declarations;
    private RoleService roleService;

    @BeforeEach
    void setUp() {
        declarations = new SecurityDeclarationScanner().scan(new SecuritySeedProperties.Scanning());
        roleService = new RoleService(roleRepository, declarations);
    }

    private static Role entity(uniShareRoleDefinition definition) {
        return Role.builder().id(definition.id()).name(definition.name()).build();
    }

    @Test
    @DisplayName("R. new users receive the first declared @uniShareRole, loaded by UUID")
    void defaultRoleIsFirstDeclared() {

        uniShareRoleDefinition first = declarations.roles().get(0);
        when(roleRepository.findById(first.id())).thenReturn(Optional.of(entity(first)));

        Set<Role> roles = roleService.getDefaultUserRoles();

        assertEquals(1, roles.size());
        assertEquals(first.id(), roles.iterator().next().getId());
        verify(roleRepository).findById(first.id());
        verify(roleRepository, never()).findByName(any());
    }

    @Test
    @DisplayName("F. default roles never include the GodRole")
    void defaultRolesExcludeGodRole() {

        uniShareRoleDefinition first = declarations.roles().get(0);
        when(roleRepository.findById(first.id())).thenReturn(Optional.of(entity(first)));

        Set<Role> roles = roleService.getDefaultUserRoles();

        assertFalse(roleService.hasRole(roles, declarations.godRole().id()));
        verify(roleRepository, never()).findById(declarations.godRole().id());
    }

    @Test
    @DisplayName("R. missing synchronized default role fails clearly")
    void missingDefaultRoleFails() {

        when(roleRepository.findById(any())).thenReturn(Optional.empty());

        IllegalStateException error = assertThrows(IllegalStateException.class, roleService::getDefaultUserRoles);
        assertTrue(error.getMessage().contains("Security seeding must run"));
    }

    @Test
    @DisplayName("undeclared role UUIDs are rejected")
    void undeclaredRoleRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> roleService.getRoleById(UUID.fromString("01a0fe4b-f2ea-7000-8000-0000000000ff")));
    }

    @Test
    @DisplayName("primary role: last declared > ... > first declared (normal roles only)")
    void primaryRolePriority() {

        List<uniShareRoleDefinition> roles = declarations.roles();
        Role first = entity(roles.get(0));
        Role last = entity(roles.get(roles.size() - 1));

        assertEquals(first.getName(), roleService.getPrimaryRoleName(Set.of(first)));
        assertEquals(last.getName(), roleService.getPrimaryRoleName(Set.of(first, last)));
        assertEquals(first.getName(), roleService.getPrimaryRoleName(Set.of()));
    }

    @Test
    @DisplayName("scanning disabled: RBAC lookups fail with a clear message")
    void scanningDisabled() {

        RoleService service = new RoleService(roleRepository, SecurityDeclarations.empty());

        IllegalStateException error = assertThrows(IllegalStateException.class, service::getDefaultUserRoles);
        assertTrue(error.getMessage().contains("scanning.roles"));
    }
}
