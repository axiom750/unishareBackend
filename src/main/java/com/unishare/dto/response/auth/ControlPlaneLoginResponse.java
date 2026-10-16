
package com.unishare.dto.response.auth;

import java.util.List;
import java.util.UUID;

public record ControlPlaneLoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UUID accountId,
        String username,
        String email,
        String displayName,
        List<String> roles
) {
}
