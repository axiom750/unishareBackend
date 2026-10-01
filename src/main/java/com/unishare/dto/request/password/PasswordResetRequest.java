package com.unishare.dto.request.password;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetRequest {

    @Email
    @NotBlank
    private String email;
}