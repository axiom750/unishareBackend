
package com.unishare.dto.request.rbac;

import jakarta.validation.constraints.NotBlank;

public record ControlPlaneLoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
