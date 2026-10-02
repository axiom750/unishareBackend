package com.unishare.dto.response.user;

import com.unishare.enums.auth.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private Set<String> roles;  // Changed from single Roles enum to Set<String>
    private AuthProvider authProvider;
    private boolean active;
    private UserProfileResponse profile;
}
