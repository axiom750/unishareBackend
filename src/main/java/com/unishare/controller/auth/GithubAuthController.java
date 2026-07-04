package com.unishare.controller.auth;

import com.unishare.service.auth.GithubOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/github")
@RequiredArgsConstructor
public class GithubAuthController {

    private final GithubOAuthService githubOAuthService;

    @GetMapping("/callback")
    public ResponseEntity<Void> handleGithubCallback(@RequestParam String code) {
        return githubOAuthService.handleCallback(code);
    }
}