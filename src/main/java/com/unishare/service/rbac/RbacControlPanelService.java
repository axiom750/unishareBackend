
package com.unishare.service.rbac;

import com.unishare.dto.response.rbac.PermissionResponse;
import com.unishare.dto.response.rbac.RoleResponse;
import com.unishare.dto.response.rbac.UpdateRolePermissionsResponse;
import com.unishare.entity.rbac.Permission;
import com.unishare.entity.rbac.Role;
import com.unishare.repository.rbac.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RbacControlPanelService {

    private final PermissionRepository permissionRepository;
    private final RoleService roleService;

    public List<PermissionResponse> getPermissions() {
        return permissionRepository
                .findAllByOrderByNameAsc()
                .stream()
                .map(this::toPermissionResponse)
                .toList();
    }

    public List<RoleResponse> getRoles() {
        return roleService.getAllRoles()
                .stream()
                .map(this::toRoleResponse)
                .toList();
    }

    public List<PermissionResponse> getRolePermissions(UUID roleId) {
        return roleService.getRolePermissions(roleId)
                .stream()
                .sorted(
                        Comparator.comparing(
                                Permission::getName,
                                Comparator.nullsLast(
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                )
                .map(this::toPermissionResponse)
                .toList();
    }

    /**
     * Replaces all permissions assigned to a declared normal role.
     */
    @Transactional
    public ResponseEntity<UpdateRolePermissionsResponse> grantRolePermissions(
            UUID roleId,
            Set<UUID> permissionIds
    ) {

        if (roleId == null) {
            throw new IllegalArgumentException("Role ID is required");
        }

        if (permissionIds == null) {
            throw new IllegalArgumentException(
                    "Permission IDs must be provided"
            );
        }

        roleService.getRoleById(roleId);

        List<Permission> permissions =
                permissionRepository.findAllById(permissionIds);

        Set<UUID> foundIds = new HashSet<>();

        for (Permission permission : permissions) {
            foundIds.add(permission.getId());
        }

        Set<UUID> missingIds = new HashSet<>(permissionIds);
        missingIds.removeAll(foundIds);

        if (!missingIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unknown permission IDs: " + missingIds
            );
        }

        roleService.replaceRolePermissions(
                roleId,
                new HashSet<>(permissions)
        );

        UpdateRolePermissionsResponse response =
                new UpdateRolePermissionsResponse();

        response.setMessage("Permissions granted successfully");
        response.setRoleId(roleId);
        response.setPermissionIds(new HashSet<>(permissionIds));

        return ResponseEntity.ok(response);
    }

    /**
     * Revokes only the specified permissions from a declared normal role.
     */
    @Transactional
    public ResponseEntity<UpdateRolePermissionsResponse> revokeRolePermissions(
            UUID roleId,
            Set<UUID> permissionIds
    ) {

        if (roleId == null) {
            throw new IllegalArgumentException("Role ID is required");
        }

        if (permissionIds == null || permissionIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one permission ID is required"
            );
        }

        List<Permission> permissions =
                permissionRepository.findAllById(permissionIds);

        Set<UUID> foundIds = new HashSet<>();

        for (Permission permission : permissions) {
            foundIds.add(permission.getId());
        }

        Set<UUID> missingIds = new HashSet<>(permissionIds);
        missingIds.removeAll(foundIds);

        if (!missingIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unknown permission IDs: " + missingIds
            );
        }

        Set<UUID> revokedIds =
                roleService.revokeRolePermissions(roleId, permissionIds);

        UpdateRolePermissionsResponse response =
                new UpdateRolePermissionsResponse();

        response.setMessage("Permissions revoked successfully");
        response.setRoleId(roleId);
        response.setPermissionIds(revokedIds);

        return ResponseEntity.ok(response);
    }

    private PermissionResponse toPermissionResponse(
            Permission permission
    ) {
        return new PermissionResponse(
                permission.getId(),
                permission.getName(),
                permission.getDisplayName(),
                permission.getDescription(),
                permission.getDomain(),
                permission.getStatus()
        );
    }

    private RoleResponse toRoleResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription()
        );
    }
}
