package com.unishare.dto.response.auth;

import com.unishare.enums.auth.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class AuthResponse {

    private Long id;
    private String email;
    private String role;
    private boolean isActive;
    private AuthProvider provider;

}
