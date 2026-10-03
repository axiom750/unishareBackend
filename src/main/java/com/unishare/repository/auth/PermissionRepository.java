package com.unishare.repository.auth;

import com.unishare.entity.rbac.Permission;
import com.unishare.enums.rbac.PermissionDomain;
import com.unishare.enums.rbac.PermissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PermissionRepository
        extends JpaRepository<Permission, UUID> {

    List<Permission> findByDomainAndStatus(PermissionDomain domain, PermissionStatus status);
}
