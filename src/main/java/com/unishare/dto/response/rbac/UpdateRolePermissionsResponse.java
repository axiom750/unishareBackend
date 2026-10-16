
package com.unishare.dto.response.rbac;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRolePermissionsResponse {

    private String message;

    private UUID roleId;

    private Set<UUID> permissionIds;
}
