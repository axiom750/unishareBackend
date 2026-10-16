
package com.unishare.dto.request.rbac;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record GrantRolePermissionsRequest(

        @NotNull(message = "Permission IDs must be provided")
        @Size(
                max = 200,
                message = "A role cannot have more than 200 permissions"
        )
        Set<UUID> permissionIds

) {
}
