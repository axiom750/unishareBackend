package com.unishare.repository.auth;

import com.unishare.entity.auth.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PermissionRepository
        extends JpaRepository<Permission, UUID> {
}