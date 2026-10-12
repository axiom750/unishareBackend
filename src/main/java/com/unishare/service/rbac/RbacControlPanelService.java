package com.unishare.service.rbac;

import com.unishare.dto.response.rbac.PermissionResponse;
import com.unishare.dto.response.rbac.RoleResponse;
import com.unishare.entity.rbac.Permission;
import com.unishare.entity.rbac.Role;
import com.unishare.repository.rbac.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public List<PermissionResponse> getRolePermissions(
            java.util.UUID roleId
    ) {

        return roleService.getRolePermissions(roleId)
                .stream()
                .sorted(
                        java.util.Comparator.comparing(
                                Permission::getName,
                                java.util.Comparator.nullsLast(
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                )
                .map(this::toPermissionResponse)
                .toList();
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