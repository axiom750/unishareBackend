package com.unishare.controller.auth;

import com.unishare.service.auth.GithubOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/github")
@RequiredArgsConstructor
public class GithubAuthController {

    private final GithubOAuthService githubOAuthService;

    /**
     * Starts GitHub OAuth login.
     */
    @GetMapping
    public ResponseEntity<Void> initiateGithubLogin() {
        return githubOAuthService.initiateLogin();
    }

    /**
     * Handles GitHub OAuth callback.
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> handleGithubCallback(
            @RequestParam(name = "code") String code
    ) {
        return githubOAuthService.handleCallback(code);
    }
}