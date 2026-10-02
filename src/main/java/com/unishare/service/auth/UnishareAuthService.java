package com.unishare.service.auth;

import com.unishare.dto.request.auth.LoginRequest;
import com.unishare.dto.request.auth.RegisterRequest;
import com.unishare.dto.response.api.ApiResponse;
import com.unishare.dto.response.auth.AuthResponseDTO;
import com.unishare.dto.response.user.UserDTO;
import com.unishare.entity.user.User;
import com.unishare.enums.auth.AuthProvider;
import com.unishare.enums.user.Roles;
import com.unishare.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnishareAuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${production.status}")
    private boolean isProduction;

    public ResponseEntity<AuthResponseDTO> registerAndLogin(RegisterRequest request) {
        log.info("[AUTH] Registration attempt for email domain: {}", getEmailDomain(request.getEmail()));
        
        // Check if email already exists
        if (userService.findByEmail(request.getEmail()).isPresent()) {
            log.warn("[AUTH] Registration failed - email already registered: {}", getEmailDomain(request.getEmail()));
            return ResponseEntity.ok(
                    AuthResponseDTO.builder()
                            .success(false)
                            .message("Email already registered")
                            .build()
            );
        }

        User newUser = new User();
        newUser.setUsername(request.getEffectiveUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setActive(true);
        newUser.setRole(Roles.USER);
        newUser.setAuthProvider(AuthProvider.UNISHARE);

        User savedUser = userService.save(newUser);
        log.info("[AUTH] User registered successfully - ID: {}, email domain: {}", 
                savedUser.getId(), getEmailDomain(savedUser.getEmail()));
        
        return buildLoginResponse(savedUser, "Registration successful! Welcome to UniShare.");
    }

    public ResponseEntity<AuthResponseDTO> login(LoginRequest request) {
        log.info("[AUTH] Login attempt for email domain: {}", getEmailDomain(request.getEmail()));
        
        User user = userService.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            log.warn("[AUTH] Login failed - user not found: {}", getEmailDomain(request.getEmail()));
            return ResponseEntity.ok(
                    AuthResponseDTO.builder()
                            .success(false)
                            .message("User not found")
                            .build()
            );
        }

        if (user.getAuthProvider() != AuthProvider.UNISHARE) {
            log.warn("[AUTH] Login failed - wrong auth provider {} for user ID: {}", 
                    user.getAuthProvider(), user.getId());
            return ResponseEntity.ok(
                    AuthResponseDTO.builder()
                            .success(false)
                            .message("Please use " + user.getAuthProvider() + " to login")
                            .build()
            );
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("[AUTH] Login failed - invalid password for user ID: {}", user.getId());
            return ResponseEntity.ok(
                    AuthResponseDTO.builder()
                            .success(false)
                            .message("Invalid password")
                            .build()
            );
        }

        log.info("[AUTH] User logged in successfully - ID: {}", user.getId());
        return buildLoginResponse(user, "Login successful");
    }

    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                log.debug("[AUTH] Get current user - no authentication found");
                return ResponseEntity.ok(ApiResponse.success(null, "No user authenticated"));
            }

            String userIdStr = authentication.getName();
            Long userId = Long.parseLong(userIdStr);
            
            User user = userService.findById(userId).orElse(null);
            
            if (user == null) {
                log.warn("[AUTH] Get current user - user ID {} not found in database", userId);
                return ResponseEntity.ok(ApiResponse.success(null, "User not found"));
            }

            log.debug("[AUTH] Current user retrieved - ID: {}", userId);
            UserDTO userDTO = UserDTO.fromEntity(user);
            return ResponseEntity.ok(ApiResponse.success(userDTO, "User retrieved successfully"));
            
        } catch (Exception e) {
            log.error("[AUTH] Error retrieving current user: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.success(null, "No user authenticated"));
        }
    }

    public ResponseEntity<ApiResponse<Void>> logout() {
        log.info("[AUTH] User logout requested");
        
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(isProduction)
                .path("/")
                .maxAge(0)
                .sameSite(isProduction ? "None" : "Lax");
        
        // Only set domain for local development
        if (!isProduction) {
            cookieBuilder.domain("localhost");
        }
        
        ResponseCookie cookie = cookieBuilder.build();

        log.debug("[AUTH] Logout successful - JWT cookie cleared");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(null, "Logged out successfully"));
    }

    private ResponseEntity<AuthResponseDTO> buildLoginResponse(User user, String message) {
        log.debug("[AUTH] Building login response with JWT for user ID: {}", user.getId());
        
        String jwt = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("token", jwt)
                .httpOnly(true)
                .secure(isProduction)
                .path("/")
                .maxAge(60 * 60 * 24)
                .sameSite(isProduction ? "None" : "Lax");
        
        // Only set domain for local development
        if (!isProduction) {
            cookieBuilder.domain("localhost");
        }
        // For production, don't set domain to allow cross-domain cookies
        
        ResponseCookie cookie = cookieBuilder.build();

        UserDTO userDTO = UserDTO.fromEntity(user);
        
        AuthResponseDTO response = AuthResponseDTO.builder()
                .success(true)
                .message(message)
                .user(userDTO)
                .build();

        log.debug("[AUTH] JWT cookie set with {} security", isProduction ? "production" : "development");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }
    
    /**
     * Extract email domain for logging (never log full email for privacy)
     */
    private String getEmailDomain(String email) {
        if (email == null || !email.contains("@")) {
            return "unknown";
        }
        return email.substring(email.indexOf("@"));
    }
}
