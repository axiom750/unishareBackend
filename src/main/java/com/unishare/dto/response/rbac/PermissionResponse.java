package com.unishare.dto.response.rbac;

import com.unishare.enums.rbac.PermissionDomain;
import com.unishare.enums.rbac.PermissionStatus;

import java.util.UUID;

public record PermissionResponse(
        UUID id,
        String name,
        String displayName,
        String description,
        PermissionDomain domain,
        PermissionStatus status
) {
}