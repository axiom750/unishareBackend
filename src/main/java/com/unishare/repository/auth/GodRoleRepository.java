package com.unishare.repository.auth;

import com.unishare.entity.rbac.GodRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for the control-plane GodRole.
 *
 * Mutations (save/delete) are reserved for SecurityDeclarationSynchronizer;
 * everything else may only read. Enforced by ControlPlaneLifecycleOwnershipTest.
 */
public interface GodRoleRepository extends JpaRepository<GodRole, UUID> {

    Optional<GodRole> findByName(String name);
}
