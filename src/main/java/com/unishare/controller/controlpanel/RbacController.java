
package com.unishare.controller.controlpanel;

import com.unishare.annotation.Permission;
import com.unishare.dto.request.rbac.RevokeRolePermissionsRequest;
import com.unishare.dto.request.rbac.GrantRolePermissionsRequest;
import com.unishare.dto.response.rbac.PermissionResponse;
import com.unishare.dto.response.rbac.RoleResponse;
import com.unishare.dto.response.rbac.UpdateRolePermissionsResponse;
import com.unishare.entity.rbac.Role;
import com.unishare.enums.rbac.PermissionDomain;
import com.unishare.service.rbac.RbacControlPanelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/controlpanel/rbac")
@RequiredArgsConstructor
public class RbacController {

    private final RbacControlPanelService rbacControlPanelService;

    @GetMapping("/permissions")
    @Permission(
            id = "0199a7b2-6f4c-7d83-9a21-5e8c3b4f7d62",
            displayName = "View Control Panel Permissions",
            description = "Allows viewing registered RBAC permissions.",
            domain = PermissionDomain.CONTROL_PLANE,
            baseEntity = Role.class
    )
    @PreAuthorize("hasAuthority('VIEW_PERMISSIONS')")
    public List<PermissionResponse> getPermissions() {
        return rbacControlPanelService.getPermissions();
    }

    @GetMapping("/roles")
    @Permission(
            id = "0199a7b2-6f4c-7e91-8b34-6a2d5f9c1037",
            displayName = "View Control Panel Roles",
            description = "Allows viewing normal UniShare roles.",
            domain = PermissionDomain.CONTROL_PLANE,
            baseEntity = Role.class
    )
    @PreAuthorize("hasAuthority('VIEW_ROLES')")
    public List<RoleResponse> getRoles() {
        return rbacControlPanelService.getRoles();
    }

    @GetMapping("/roles/{roleId}/permissions")
    @Permission(
            id = "0199a7b2-6f4c-7f25-a842-3d9e6b1c5074",
            displayName = "View Role Permissions",
            description = "Allows viewing permissions assigned to a normal UniShare role.",
            domain = PermissionDomain.CONTROL_PLANE,
            baseEntity = Role.class
    )
    @PreAuthorize("hasAuthority('VIEW_ROLE_PERMISSIONS')")
    public List<PermissionResponse> getRolePermissions(
            @PathVariable UUID roleId
    ) {
        return rbacControlPanelService.getRolePermissions(roleId);
    }

    @PutMapping("/roles/{roleId}/permissions/grant")
    @Permission(
            id = "0199a7b2-6f4c-7033-9c45-7b3e6a1d2048",
            displayName = "Grant Role Permissions",
            description = "Allows replacing permissions assigned to a normal UniShare role.",
            domain = PermissionDomain.CONTROL_PLANE,
            baseEntity = Role.class
    )
    @PreAuthorize("hasAuthority('GRANT_ROLE_PERMISSIONS')")
    public ResponseEntity<UpdateRolePermissionsResponse> grantRolePermissions(
            @PathVariable UUID roleId,
            @Valid @RequestBody GrantRolePermissionsRequest request
    ) {
        return rbacControlPanelService.grantRolePermissions(
                roleId,
                request.permissionIds()
        );
    }

    @PutMapping("/roles/{roleId}/permissions/revoke")
    @Permission(
            id = "0199a7b2-6f4c-7044-8d56-8c4f7b2e3059",
            displayName = "Revoke Role Permissions",
            description = "Allows revoking selected permissions from a normal UniShare role.",
            domain = PermissionDomain.CONTROL_PLANE,
            baseEntity = Role.class
    )
    @PreAuthorize("hasAuthority('REVOKE_ROLE_PERMISSIONS')")
    public ResponseEntity<UpdateRolePermissionsResponse> revokeRolePermissions(
            @PathVariable UUID roleId,
            @Valid @RequestBody RevokeRolePermissionsRequest request
    ) {
        return rbacControlPanelService.revokeRolePermissions(
                roleId,
                request.permissionIds()
        );
    }
}
