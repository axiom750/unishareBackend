package com.unishare.controller.auth;

import com.unishare.dto.auth.LoginRequest;
import com.unishare.dto.auth.RegisterRequest;
import com.unishare.service.auth.UnishareAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/unishare")
@RequiredArgsConstructor
public class UniShareAuthController {

    private final UnishareAuthService unishareAuthService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        return unishareAuthService.registerAndLogin(request);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return unishareAuthService.login(request);
    }
}