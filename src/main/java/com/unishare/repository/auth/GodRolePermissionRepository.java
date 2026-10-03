package com.unishare.repository.auth;

import com.unishare.entity.rbac.GodRole;
import com.unishare.entity.rbac.GodRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for GodRole -> Permission grants (god_role_permissions).
 *
 * Mutations (save/delete) are reserved for SecurityDeclarationSynchronizer;
 * everything else may only read. Enforced by ControlPlaneLifecycleOwnershipTest.
 */
public interface GodRolePermissionRepository extends JpaRepository<GodRolePermission, Long> {

    List<GodRolePermission> findByGodRole(GodRole godRole);
}
