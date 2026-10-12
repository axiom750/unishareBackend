package com.unishare.repository.rbac;

import com.unishare.entity.rbac.Permission;
import com.unishare.enums.rbac.PermissionDomain;
import com.unishare.enums.rbac.PermissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PermissionRepository
        extends JpaRepository<Permission, UUID> {

    List<Permission> findByDomainAndStatus(PermissionDomain domain, PermissionStatus status);

    List<Permission> findAllByOrderByNameAsc();

    List<Permission> findByDomainOrderByNameAsc(
            PermissionDomain domain
    );

    List<Permission> findByStatusOrderByNameAsc(
            PermissionStatus status
    );

    List<Permission> findByDomainAndStatusOrderByNameAsc(
            PermissionDomain domain,
            PermissionStatus status
    );
}
