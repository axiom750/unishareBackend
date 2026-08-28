package com.unishare.controller.user;

import com.unishare.entity.user.User;
import com.unishare.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Value("${production.status}")
    private boolean isProduction;

    @GetMapping("/me")
    public ResponseEntity<?> getUserLoginStatus(Authentication authentication) {

        // Check if user is authenticated and not anonymous
        if (authentication == null 
            || !authentication.isAuthenticated() 
            || "anonymousUser".equals(authentication.getPrincipal())
            || authentication.getPrincipal() == null) {
            // Return format expected by frontend when not authenticated
            // Use HashMap instead of Map.of() because Map.of() doesn't allow null values
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("user", null);
            return ResponseEntity.ok(response);
        }

        String email = null;
        try {
            email = authentication.getName();
        } catch (Exception e) {
            // If we can't get the name, treat as not authenticated
            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "user", (Object) null
                    )
            );
        }

        if (email == null || email.isEmpty()) {
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("user", null);
            return ResponseEntity.ok(response);
        }

        User user = userService.findByEmail(email)
                .orElse(null);

        if (user == null) {
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("user", null);
            return ResponseEntity.ok(response);
        }

        // Return format expected by frontend: { success: true, user: {...} }
        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "user", Map.of(
                                "id", user.getId(),
                                "email", user.getEmail(),
                                "username", user.getUsername() != null ? user.getUsername() : "",
                                "role", user.getRole().name(),
                                "authProvider", user.getAuthProvider().name(),
                                "active", user.isActive(),
                                "userBio", user.getUserBio() != null ? user.getUserBio() : "",
                                "userProfilePictureURL", user.getUserProfilePictureURL() != null ? user.getUserProfilePictureURL() : "",
                                "universityName", user.getUniversityName() != null ? user.getUniversityName() : ""
                        )
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {

        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(isProduction)
                .domain("localhost")  // Set for entire localhost domain
                .path("/")
                .maxAge(0)
                .sameSite(isProduction ? "None" : "Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("success", true, "message", "Logged out successfully"));
    }
}