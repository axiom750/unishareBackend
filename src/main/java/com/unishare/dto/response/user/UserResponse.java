package com.unishare.dto.response.user;

import com.unishare.enums.auth.AuthProvider;
import com.unishare.enums.user.Roles;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private Roles role;
    private AuthProvider authProvider;
    private boolean active;
    private UserProfileResponse profile;
}
