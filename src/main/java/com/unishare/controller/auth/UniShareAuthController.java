package com.unishare.controller.auth;

import com.unishare.dto.auth.LoginRequest;
import com.unishare.dto.auth.RegisterRequest;
import com.unishare.dto.response.ApiResponse;
import com.unishare.dto.response.AuthResponseDTO;
import com.unishare.dto.response.UserDTO;
import com.unishare.service.auth.UnishareAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UniShareAuthController {

    private final UnishareAuthService unishareAuthService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser() {
        return unishareAuthService.getCurrentUser();
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequest request) {
        return unishareAuthService.registerAndLogin(request);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequest request) {
        return unishareAuthService.login(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return unishareAuthService.logout();
    }
}