package com.unishare.controller.user;

import com.unishare.annotation.Permission;
import com.unishare.dto.request.password.PasswordResetRequest;
import com.unishare.dto.request.password.PasswordUpdateRequest;
import com.unishare.dto.request.user.UpdateUserProfileRequest;
import com.unishare.dto.response.password.PasswordResetResponse;
import com.unishare.dto.response.user.UserResponse;
import com.unishare.entity.user.User;
import com.unishare.entity.user.UserProfile;
import com.unishare.enums.rbac.PermissionDomain;
import com.unishare.idempotency.Idempotent;
import com.unishare.service.user.CurrentUserService;
import com.unishare.service.user.ProfileUpdateService;
import com.unishare.service.user.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final CurrentUserService currentUserService;
    private final ProfileUpdateService profileUpdateService;
    private final PasswordResetService passwordResetService;

    @Value("${production.status:false}")
    private boolean isProduction;

    /**
     * GET /api/me
     * Returns the currently authenticated user's information.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {

            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Authentication required"
            ));
        }

        UserResponse user = currentUserService.getCurrentUser(authentication);

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Authenticated user not found"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "user", user
        ));
    }

    /**
     * PUT /api/user/update/profile
     * Updates the authenticated user's own profile.
     */
    @Permission(
            id = "01a10260-959a-761b-8a25-d08aa827b2e4",
            displayName = "Update Own Profile",
            description = "Allows a user to update their own profile.",
            baseEntity = UserProfile.class,
            reachableEntities = {User.class},
            domain = PermissionDomain.APPLICATION
    )
    @PreAuthorize("hasAuthority('USER_PROFILE_UPDATE')")
    @PutMapping(
            value = "/update/profile",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<?> updateProfile(@ModelAttribute UpdateUserProfileRequest request,
            @RequestPart(
                    name = "profileImage",
                    required = false
            ) MultipartFile profileImage, Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {

            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Authentication required"
            ));
        }

        try {
            // Resolve the user from the authenticated principal.
            UserResponse currentUser = currentUserService.getCurrentUser(authentication);

            if (currentUser == null || currentUser.getId() == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "Authenticated user not found"
                ));
            }

            Long userId = currentUser.getId();

            String message = profileUpdateService.updateProfile(
                    userId,
                    request,
                    profileImage
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", message
            ));

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", exception.getMessage()
            ));
        }
    }

    /**
     *   user password change apis
     *   usage : - password forgot
     *             password update / change
     */


    @Idempotent
    @PostMapping("/password-update")
    public ResponseEntity<PasswordResetResponse> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {

        return passwordResetService.requestPasswordReset(request.getEmail());
    }

    @PostMapping("/password-update/confirm")
    public ResponseEntity<PasswordResetResponse> updatePassword(@Valid @RequestBody PasswordUpdateRequest request) {

        passwordResetService.resetPassword(
                request.getToken(),
                request.getNewPassword()
        );

        PasswordResetResponse response = new PasswordResetResponse();

        response.setMessage("Password has been updated successfully.");

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/user/account/delete
     *  remove the user account by user choice
     */

    @DeleteMapping("/account/delete")
    public ResponseEntity<?> deleteAccount(Authentication authentication) {

        return ResponseEntity.status(501).body(Map.of("message", "feature yet not implemented"));
    }

    /**
     * POST /api/user/logout
     * Clears the authentication cookie and request-local security context.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {

        SecurityContextHolder.clearContext();

        ResponseCookie.ResponseCookieBuilder cookieBuilder =
                ResponseCookie.from("token", "")
                        .httpOnly(true)
                        .secure(isProduction)
                        .path("/")
                        .maxAge(0)
                        .sameSite(isProduction ? "None" : "Lax");

        // Must match the Domain attribute used when issuing the cookie.
        if (!isProduction) {
            cookieBuilder.domain("localhost");
        }

        ResponseCookie cookie = cookieBuilder.build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of(
                        "success", true,
                        "message", "Logged out successfully"
                ));
    }

}