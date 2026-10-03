package com.unishare.controller.user;

import com.unishare.dto.response.user.UserProfileResponse;
import com.unishare.dto.response.user.UserResponse;
import com.unishare.entity.rbac.Role;
import com.unishare.entity.user.User;
import com.unishare.entity.user.UserProfile;
import com.unishare.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Value("${production.status}")
    private boolean isProduction;

    @GetMapping("/me")
    public ResponseEntity<?> getUserLoginStatus(Authentication authentication) {


        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(
                authentication.getPrincipal())) {

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "user", (Object) null
                    )
            );
        }


        String email;

        try {
            email = authentication.getName();
        } catch (Exception e) {

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "user", (Object) null
                    )
            );
        }


        if (email == null || email.isBlank()) {

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "user", (Object) null
                    )
            );
        }

        Optional<User> userOptional =
                userService.findByEmail(email);

        if (userOptional.isEmpty()) {

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "user", (Object) null
                    )
            );
        }


        User user = userOptional.get();

        UserResponse userResponse =
                buildUserResponse(user);


        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "user", userResponse
                )
        );
    }

    private UserResponse buildUserResponse(User user) {

        UserProfileResponse profileResponse = null;

        UserProfile profile = user.getProfile();

        if (profile != null) {

            profileResponse =
                    UserProfileResponse.builder()
                            .bio(profile.getBio())
                            .profilePictureUrl(
                                    profile.getProfilePictureUrl()
                            )
                            .universityName(
                                    profile.getUniversityName()
                            )
                            .dateOfBirth(
                                    profile.getDateOfBirth()
                            )
                            .build();
        }

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .authProvider(user.getAuthProvider())
                .active(user.isActive())
                .profile(profileResponse)
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {

        ResponseCookie cookie =
                ResponseCookie.from("token", "")
                        .httpOnly(true)
                        .secure(isProduction)
                        .path("/")
                        .maxAge(0)
                        .sameSite(
                                isProduction
                                        ? "None"
                                        : "Lax"
                        )
                        .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookie.toString()
                )
                .body(
                        Map.of(
                                "success", true,
                                "message",
                                "Logged out successfully"
                        )
                );
    }
}