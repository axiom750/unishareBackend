package com.unishare.utils.seeder;

import com.unishare.utils.scanner.PermissionDefinition;
import com.unishare.utils.scanner.PermissionScanner;
import com.unishare.utils.scanner.PermissionSynchronizer;
import com.unishare.utils.scanner.SecurityDeclarations;
import com.unishare.utils.synchronizer.SecurityDeclarationSynchronizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("SecuritySeedInitializer startup order")
class SecuritySeedInitializerTest {

    private final SecuritySeedProperties properties = new SecuritySeedProperties();
    private final SecurityDeclarations declarations = SecurityDeclarations.empty();
    private final SecurityDeclarationSynchronizer declarationSynchronizer = mock(SecurityDeclarationSynchronizer.class);
    private final PermissionScanner permissionScanner = mock(PermissionScanner.class);
    private final PermissionSynchronizer permissionSynchronizer = mock(PermissionSynchronizer.class);

    private final SecuritySeedInitializer initializer = new SecuritySeedInitializer(
            properties, declarations, declarationSynchronizer, permissionScanner, permissionSynchronizer
    );

    @Test
    @DisplayName("declarations -> permission scan -> permission sync -> GodRole permissions")
    void runsInRequiredOrder() {

        List<PermissionDefinition> scanned = List.of();
        when(permissionScanner.scan()).thenReturn(scanned);

        initializer.run(null);

        InOrder order = inOrder(declarationSynchronizer, permissionScanner, permissionSynchronizer);
        order.verify(declarationSynchronizer).synchronize(declarations, properties.getSeeding());
        order.verify(permissionScanner).scan();
        order.verify(permissionSynchronizer).synchronize(scanned);
        order.verify(declarationSynchronizer).synchronizeGodRolePermissions(declarations);
    }

    @Test
    @DisplayName("seeding disabled: nothing is written, permissions are still scanned")
    void seedingDisabled() {

        properties.getSeeding().setEnabled(false);
        when(permissionScanner.scan()).thenReturn(List.of());

        initializer.run(null);

        verify(permissionScanner).scan();
        verifyNoInteractions(declarationSynchronizer, permissionSynchronizer);
    }

    @Test
    @DisplayName("GodRole permissions are not synchronized when permission seeding is off")
    void godRolePermissionsNeedPermissionSeeding() {

        properties.getSeeding().setPermissions(false);
        when(permissionScanner.scan()).thenReturn(List.of());

        initializer.run(null);

        verify(declarationSynchronizer).synchronize(any(), any());
        verify(declarationSynchronizer, never()).synchronizeGodRolePermissions(any());
    }
}
