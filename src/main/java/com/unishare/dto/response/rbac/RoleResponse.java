package com.unishare.dto.response.rbac;

import java.util.UUID;

public record RoleResponse(
        UUID id,
        String name,
        String description
) {
}