package com.unishare.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    // Support both username and firstName/lastName from frontend
    private String username;
    
    private String firstName;
    
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6)
    private String password;
    
    // Helper method to get the username (either provided or constructed from firstName/lastName)
    public String getEffectiveUsername() {
        if (username != null && !username.isEmpty()) {
            return username;
        }
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        if (firstName != null) {
            return firstName;
        }
        return email.split("@")[0]; // Fallback to email prefix
    }
}
