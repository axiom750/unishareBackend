package com.unishare.repository.auth;

import com.unishare.entity.rbac.GodAccount;
import com.unishare.entity.rbac.GodAccountRole;
import com.unishare.entity.rbac.GodRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for GodAccount -> GodRole assignments.
 *
 * Mutations (save/delete) are reserved for SecurityDeclarationSynchronizer;
 * everything else may only read. Enforced by ControlPlaneLifecycleOwnershipTest.
 */
public interface GodAccountRoleRepository extends JpaRepository<GodAccountRole, Long> {

    List<GodAccountRole> findByGodAccount(GodAccount godAccount);

    List<GodAccountRole> findByGodRole(GodRole godRole);
}
