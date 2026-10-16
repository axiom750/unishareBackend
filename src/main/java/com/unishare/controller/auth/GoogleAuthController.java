package com.unishare.controller.auth;

import com.unishare.service.auth.GoogleOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final GoogleOAuthService googleOAuthService;

    @Value("${google.clientId}")
    private String clientId;

    @Value("${google.redirectURL}")
    private String redirectUri;

    @GetMapping
    public ResponseEntity<Void> initiateGoogleLogin() {

        String googleAuthUrl = UriComponentsBuilder
                .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .build()
                .encode()
                .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, googleAuthUrl)
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<String> handleGoogleCallback(
            @RequestParam("code") String code) {
        return googleOAuthService.handleCallback(code);
    }
}