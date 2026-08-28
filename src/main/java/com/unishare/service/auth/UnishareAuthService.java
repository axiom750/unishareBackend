package com.unishare.service.auth;

import com.unishare.dto.auth.LoginRequest;
import com.unishare.dto.auth.RegisterRequest;
import com.unishare.entity.user.User;
import com.unishare.enums.auth.AuthProvider;
import com.unishare.enums.user.Roles;
import com.unishare.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UnishareAuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${production.status}")
    private boolean isProduction;

    public ResponseEntity<?> registerAndLogin(RegisterRequest request) {

        // Check if email already exists
        if (userService.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.ok(
                    java.util.Map.of(
                            "success", false,
                            "message", "Email already registered"
                    )
            );
        }

        User newUser = new User();
        newUser.setUsername(request.getEffectiveUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setActive(true);
        newUser.setRole(Roles.USER);
        newUser.setAuthProvider(AuthProvider.UNISHARE);
        
        // Set university if provided
        if (request.getUniversity() != null && !request.getUniversity().isEmpty()) {
            newUser.setUniversityName(request.getUniversity());
        }
        
        // Set security question and answer if provided
        if (request.getSecurityQuestion() != null && !request.getSecurityQuestion().isEmpty()) {
            newUser.setSecurityQuestion(request.getSecurityQuestion());
        }
        if (request.getSecurityAnswer() != null && !request.getSecurityAnswer().isEmpty()) {
            newUser.setSecurityAnswer(request.getSecurityAnswer());
        }

        User savedUser = userService.save(newUser);

        return buildLoginResponse(savedUser, "Registration successful! Welcome to UniShare.");
    }

    public ResponseEntity<?> login(LoginRequest request) {

        User user = userService.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.ok(
                    java.util.Map.of(
                            "success", false,
                            "message", "User not found"
                    )
            );
        }

        if (user.getAuthProvider() != AuthProvider.UNISHARE) {
            return ResponseEntity.ok(
                    java.util.Map.of(
                            "success", false,
                            "message", "Please use " + user.getAuthProvider() + " to login"
                    )
            );
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.ok(
                    java.util.Map.of(
                            "success", false,
                            "message", "Invalid password"
                    )
            );
        }

        return buildLoginResponse(user, "Login successful");
    }


    private ResponseEntity<?> buildLoginResponse(User user, String message) {

        String jwt = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        ResponseCookie cookie = ResponseCookie.from("token", jwt)
                .httpOnly(true)
                .secure(isProduction)
                .domain("localhost")  // Set for entire localhost domain
                .path("/")
                .maxAge(60 * 60 * 24)
                .sameSite(isProduction ? "None" : "Lax")
                .build();

        // Return format expected by frontend: { success: true, user: {...}, message: '...' }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(
                        java.util.Map.of(
                                "success", true,
                                "message", message,
                                "user", java.util.Map.of(
                                        "id", user.getId(),
                                        "email", user.getEmail(),
                                        "username", user.getUsername() != null ? user.getUsername() : "",
                                        "role", user.getRole(),
                                        "authProvider", user.getAuthProvider(),
                                        "active", user.isActive(),
                                        "userBio", user.getUserBio() != null ? user.getUserBio() : "",
                                        "userProfilePictureURL", user.getUserProfilePictureURL() != null ? user.getUserProfilePictureURL() : "",
                                        "universityName", user.getUniversityName() != null ? user.getUniversityName() : ""
                                )
                        )
                );
    }
}