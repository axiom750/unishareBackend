
package com.unishare.dto.request.rbac;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record RevokeRolePermissionsRequest(

        @NotEmpty(message = "At least one permission ID is required")
        @Size(
                max = 200,
                message = "At most 200 permissions can be revoked at once"
        )
        Set<UUID> permissionIds

) {
}
