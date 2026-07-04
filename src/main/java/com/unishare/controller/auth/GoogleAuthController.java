package com.unishare.controller.auth;

import com.unishare.service.auth.GoogleOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/google")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final GoogleOAuthService googleOAuthService;

    @GetMapping("/callback")
    public ResponseEntity<Void> handleGoogleCallback(@RequestParam String code) {
        return googleOAuthService.handleCallback(code);
    }
}