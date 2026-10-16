
package com.unishare.service.auth;

import com.unishare.dto.request.auth.LoginRequest;
import com.unishare.dto.request.auth.RegisterRequest;
import com.unishare.dto.response.auth.AuthResponseDTO;
import com.unishare.dto.response.user.UserDTO;
import com.unishare.entity.user.User;
import com.unishare.enums.auth.AuthProvider;
import com.unishare.service.rbac.RoleService;
import com.unishare.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnishareAuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    @Value("${production.status}")
    private boolean isProduction;

    @Transactional
    public ResponseEntity<AuthResponseDTO> registerAndLogin(
            RegisterRequest request
    ) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null;

        log.info("[AUTH] Registration attempt for email domain: {}", getEmailDomain(email));

        if (userService.findByEmail(email).isPresent()) {
            log.warn("[AUTH] Registration failed - email already registered: {}", getEmailDomain(email));

            return ResponseEntity.ok(
                    AuthResponseDTO.builder()
                            .success(false)
                            .message("Email already registered")
                            .build()
            );
        }

        User newUser = new User();
        newUser.setUsername(request.getEffectiveUsername());
        newUser.setEmail(email);
        newUser.setPassword(
                passwordEncoder.encode(request.getPassword())
        );
        newUser.setActive(true);
        newUser.setRoles(roleService.getDefaultUserRoles());
        newUser.setAuthProvider(AuthProvider.UNISHARE);

        User savedUser = userService.save(newUser);

        log.info("[AUTH] User registered successfully - ID: {}, domain: {}", savedUser.getId(), getEmailDomain(savedUser.getEmail()));

        return buildLoginResponse(
                savedUser,
                "Registration successful! Welcome to UniShare."
        );
    }

    @Transactional(readOnly = true)
    public ResponseEntity<AuthResponseDTO> login(
            LoginRequest request
    ) {
        log.info("[AUTH] Login attempt for email domain: {}", getEmailDomain(request.getEmail()));

        User user = userService.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            log.warn("[AUTH] Login failed - user not found: {}", getEmailDomain(request.getEmail()));

            return ResponseEntity.ok(
                    AuthResponseDTO.builder()
                            .success(false)
                            .message("Invalid email or password")
                            .build()
            );
        }

        if (user.getAuthProvider() != AuthProvider.UNISHARE) {
            log.warn("[AUTH] Login failed - wrong auth provider for user ID: {}", user.getId());

            return ResponseEntity.ok(
                    AuthResponseDTO.builder()
                            .success(false)
                            .message("Please use " + user.getAuthProvider() + " to login")
                            .build()
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            log.warn("[AUTH] Login failed - invalid password for user ID: {}", user.getId());

            return ResponseEntity.ok(
                    AuthResponseDTO.builder()
                            .success(false)
                            .message("Invalid email or password")
                            .build()
            );
        }

        log.info("[AUTH] User logged in successfully - ID: {}", user.getId());

        return buildLoginResponse(user, "Login successful");
    }

    private ResponseEntity<AuthResponseDTO> buildLoginResponse(
            User user,
            String message
    ) {
        log.debug("[AUTH] Building JWT for user ID: {}", user.getId());

        String primaryRole = roleService.getPrimaryRoleName(user.getRoles());

        String jwt = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                primaryRole
        );

        ResponseCookie.ResponseCookieBuilder cookieBuilder =
                ResponseCookie.from("token", jwt)
                        .httpOnly(true)
                        .secure(isProduction)
                        .path("/")
                        .maxAge(60 * 60 * 24)
                        .sameSite(isProduction ? "None" : "Lax");

        if (!isProduction) {
            cookieBuilder.domain("localhost");
        }

        ResponseCookie cookie = cookieBuilder.build();

        UserDTO userDTO = UserDTO.fromEntity(user);

        AuthResponseDTO response = AuthResponseDTO.builder()
                .success(true)
                .message(message)
                .user(userDTO)
                .build();

        log.debug("[AUTH] JWT cookie created for user ID: {}", user.getId());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    private String getEmailDomain(String email) {
        if (email == null || !email.contains("@")) {
            return "unknown";
        }

        return email.substring(email.indexOf("@"));
    }
}
