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

        userService.findByEmail(request.getEmail())
                .ifPresent(user -> {
                    throw new RuntimeException("Email already registered");
                });

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setActive(true);
        newUser.setRole(Roles.USER);
        newUser.setAuthProvider(AuthProvider.UNISHARE);
        newUser.setSecurityQuestion(request.getSecurityQuestion());
        newUser.setSecurityAnswer(request.getSecurityAnswer());

        User savedUser = userService.save(newUser);

        return buildLoginResponse(savedUser, "Welcome to UniShare");
    }

    public ResponseEntity<?> login(LoginRequest request) {

        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getAuthProvider() != AuthProvider.UNISHARE) {
            throw new RuntimeException("Use OAuth login");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
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
                .path("/")
                .maxAge(60 * 60 * 24)
                .sameSite(isProduction ? "None" : "Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(
                        java.util.Map.of(
                                "message", message,
                                "email", user.getEmail(),
                                "role", user.getRole()
                        )
                );
    }
}