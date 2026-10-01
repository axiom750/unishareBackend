package com.unishare.controller.password;

import com.unishare.dto.request.password.PasswordResetRequest;
import com.unishare.dto.request.password.PasswordUpdateRequest;
import com.unishare.dto.response.password.PasswordResetResponse;
import com.unishare.service.usermanagement.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-management")
@RequiredArgsConstructor
public class UserPasswordController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/password-update")
    public ResponseEntity<PasswordResetResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request
    ) {

        passwordResetService.requestPasswordReset(
                request.getEmail()
        );

        PasswordResetResponse response =
                new PasswordResetResponse();

        response.setMessage(
                "If an account exists for this email, " +
                        "a password reset link has been sent."
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-update/confirm")
    public ResponseEntity<PasswordResetResponse> updatePassword(
            @Valid @RequestBody PasswordUpdateRequest request
    ) {

        passwordResetService.resetPassword(
                request.getToken(),
                request.getNewPassword()
        );

        PasswordResetResponse response =
                new PasswordResetResponse();

        response.setMessage(
                "Password has been updated successfully."
        );

        return ResponseEntity.ok(response);
    }
}