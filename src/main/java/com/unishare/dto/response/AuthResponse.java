package com.unishare.dto.response;

import com.unishare.enums.auth.AuthProvider;
import com.unishare.enums.user.Roles;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class AuthResponse {

    private Long id;
    private String email;
    private Roles role;
    private boolean isActive;
    private AuthProvider provider;

}
