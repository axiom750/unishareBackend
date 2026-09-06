package com.unishare.controller.auth;

import com.unishare.service.auth.GoogleOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;

@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final GoogleOAuthService googleOAuthService;

    @Value("${google.clientId}")
    private String clientId;

    @Value("${google.redirectURL}")
    private String redirectUri;

    /**
     * Initiates Google OAuth flow by redirecting to Google's authorization page
     * Frontend calls this endpoint: GET /api/auth/google
     */
    @GetMapping
    public ResponseEntity<Void> initiateGoogleLogin() {
        String googleAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=openid%20email%20profile"
                + "&access_type=offline"
                + "&prompt=consent";

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, googleAuthUrl)
                .build();
    }

    /**
     * Handles the callback from Google OAuth
     * Google redirects here after user authorization
     */
    @GetMapping("/callback")
    public ResponseEntity<String> handleGoogleCallback(@RequestParam String code) {
        return googleOAuthService.handleCallback(code);
    }
}