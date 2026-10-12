
package com.unishare.service.auth;

import com.unishare.dto.request.auth.LoginRequest;
import com.unishare.dto.request.auth.RegisterRequest;
import com.unishare.dto.response.api.ApiResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        log.info(
                "[AUTH] Registration attempt for email domain: {}",
                getEmailDomain(request.getEmail())
        );

        if (userService.findByEmail(request.getEmail()).isPresent()) {
            log.warn(
                    "[AUTH] Registration failed - email already registered: {}",
                    getEmailDomain(request.getEmail())
            );

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
        newUser.setPassword(
                passwordEncoder.encode(request.getPassword())
        );
        newUser.setActive(true);
        newUser.setRoles(roleService.getDefaultUserRoles());
        newUser.setAuthProvider(AuthProvider.UNISHARE);

        User savedUser = userService.save(newUser);

        log.info(
                "[AUTH] User registered successfully - ID: {}, domain: {}",
                savedUser.getId(),
                getEmailDomain(savedUser.getEmail())
        );

        return buildLoginResponse(
                savedUser,
                "Registration successful! Welcome to UniShare."
        );
    }

    @Transactional(readOnly = true)
    public ResponseEntity<AuthResponseDTO> login(
            LoginRequest request
    ) {
        log.info(
                "[AUTH] Login attempt for email domain: {}",
                getEmailDomain(request.getEmail())
        );

        User user = userService.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null) {
            log.warn(
                    "[AUTH] Login failed - user not found: {}",
                    getEmailDomain(request.getEmail())
            );

            return ResponseEntity.ok(
                    AuthResponseDTO.builder()
                            .success(false)
                            .message("User not found")
                            .build()
            );
        }

        if (user.getAuthProvider() != AuthProvider.UNISHARE) {
            log.warn(
                    "[AUTH] Login failed - wrong auth provider for user ID: {}",
                    user.getId()
            );

            return ResponseEntity.ok(
                    AuthResponseDTO.builder()
                            .success(false)
                            .message(
                                    "Please use "
                                            + user.getAuthProvider()
                                            + " to login"
                            )
                            .build()
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            log.warn(
                    "[AUTH] Login failed - invalid password for user ID: {}",
                    user.getId()
            );

            return ResponseEntity.ok(
                    AuthResponseDTO.builder()
                            .success(false)
                            .message("Invalid password")
                            .build()
            );
        }

        log.info(
                "[AUTH] User logged in successfully - ID: {}",
                user.getId()
        );

        return buildLoginResponse(user, "Login successful");
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {

            log.debug("[AUTH] No authenticated principal found");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(
                            ApiResponse.success(
                                    null,
                                    "No user authenticated"
                            )
                    );
        }

        try {
            Long userId = Long.parseLong(authentication.getName());

            User user = userService.findById(userId).orElse(null);

            if (user == null) {
                log.warn(
                        "[AUTH] Authenticated user ID {} not found",
                        userId
                );

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(
                                ApiResponse.success(
                                        null,
                                        "User not found"
                                )
                        );
            }

            UserDTO userDTO = UserDTO.fromEntity(user);

            log.debug(
                    "[AUTH] Current user retrieved - ID: {}",
                    userId
            );

            return ResponseEntity.ok(
                    ApiResponse.success(
                            userDTO,
                            "User retrieved successfully"
                    )
            );

        } catch (NumberFormatException exception) {
            log.warn("[AUTH] Authentication principal is not a user ID");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(
                            ApiResponse.success(
                                    null,
                                    "Invalid authentication"
                            )
                    );

        } catch (Exception exception) {
            log.error(
                    "[AUTH] Failed to retrieve current user",
                    exception
            );

            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR
            ).body(
                    ApiResponse.success(
                            null,
                            "Failed to retrieve current user"
                    )
            );
        }
    }

    public ResponseEntity<ApiResponse<Void>> logout() {
        log.info("[AUTH] User logout requested");

        ResponseCookie.ResponseCookieBuilder cookieBuilder =
                ResponseCookie.from("token", "")
                        .httpOnly(true)
                        .secure(isProduction)
                        .path("/")
                        .maxAge(0)
                        .sameSite(isProduction ? "None" : "Lax");

        if (!isProduction) {
            cookieBuilder.domain("localhost");
        }

        ResponseCookie cookie = cookieBuilder.build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(
                        ApiResponse.success(
                                null,
                                "Logged out successfully"
                        )
                );
    }

    private ResponseEntity<AuthResponseDTO> buildLoginResponse(
            User user,
            String message
    ) {
        log.debug(
                "[AUTH] Building JWT for user ID: {}",
                user.getId()
        );

        String primaryRole =
                roleService.getPrimaryRoleName(user.getRoles());

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

        log.debug(
                "[AUTH] JWT cookie created for user ID: {}",
                user.getId()
        );

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
