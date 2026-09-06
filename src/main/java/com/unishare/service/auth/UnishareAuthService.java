package com.unishare.service.auth;

import com.unishare.dto.auth.LoginRequest;
import com.unishare.dto.auth.RegisterRequest;
import com.unishare.dto.response.ApiResponse;
import com.unishare.dto.response.AuthResponseDTO;
import com.unishare.dto.response.UserDTO;
import com.unishare.entity.user.User;
import com.unishare.enums.auth.AuthProvider;
import com.unishare.enums.user.Roles;
import com.unishare.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    public ResponseEntity<AuthResponseDTO> registerAndLogin(RegisterRequest request) {
        // Check if email already exists
        if (userService.findByEmail(request.getEmail()).isPresent()) {
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

    public ResponseEntity<AuthResponseDTO> login(LoginRequest request) {
        User user = userService.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            return ResponseEntity.ok(
                    AuthResponseDTO.builder()
                            .success(false)
                            .message("User not found")
                            .build()
            );
        }

        if (user.getAuthProvider() != AuthProvider.UNISHARE) {
            return ResponseEntity.ok(
                    AuthResponseDTO.builder()
                            .success(false)
                            .message("Please use " + user.getAuthProvider() + " to login")
                            .build()
            );
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.ok(
                    AuthResponseDTO.builder()
                            .success(false)
                            .message("Invalid password")
                            .build()
            );
        }

        return buildLoginResponse(user, "Login successful");
    }

    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.ok(ApiResponse.success(null, "No user authenticated"));
            }

            String userIdStr = authentication.getName();
            Long userId = Long.parseLong(userIdStr);
            
            User user = userService.findById(userId).orElse(null);
            
            if (user == null) {
                return ResponseEntity.ok(ApiResponse.success(null, "User not found"));
            }

            UserDTO userDTO = UserDTO.fromEntity(user);
            return ResponseEntity.ok(ApiResponse.success(userDTO, "User retrieved successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success(null, "No user authenticated"));
        }
    }

    public ResponseEntity<ApiResponse<Void>> logout() {
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(isProduction)
                .domain("localhost")
                .path("/")
                .maxAge(0)
                .sameSite(isProduction ? "None" : "Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(null, "Logged out successfully"));
    }

    private ResponseEntity<AuthResponseDTO> buildLoginResponse(User user, String message) {
        String jwt = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        ResponseCookie cookie = ResponseCookie.from("token", jwt)
                .httpOnly(true)
                .secure(isProduction)
                .domain("localhost")
                .path("/")
                .maxAge(60 * 60 * 24)
                .sameSite(isProduction ? "None" : "Lax")
                .build();

        UserDTO userDTO = UserDTO.fromEntity(user);
        
        AuthResponseDTO response = AuthResponseDTO.builder()
                .success(true)
                .message(message)
                .user(userDTO)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }
}
