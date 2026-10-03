package com.unishare.controller.user;

import com.unishare.annotation.Permission;
import com.unishare.dto.request.user.UpdateUserProfileRequest;
import com.unishare.entity.user.User;
import com.unishare.entity.user.UserProfile;
import com.unishare.enums.rbac.PermissionDomain;
import com.unishare.service.user.ProfileUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/update")
@RequiredArgsConstructor
public class UpdateUserProfileController {

    private final ProfileUpdateService profileUpdateService;

    @Permission(
            id = "01a10260-959a-761b-8a25-d08aa827b2e4",
            displayName = "Update Own Profile",
            description = "Allows a user to update their own profile (username, bio, university, profile picture).",
            baseEntity = UserProfile.class,
            reachableEntities = {User.class},
            domain = PermissionDomain.APPLICATION
    )
    @PreAuthorize("hasAuthority('USER_PROFILE_UPDATE')")
    @PutMapping(value = "/profile", consumes = "multipart/form-data")
    public ResponseEntity<?> updateProfile(
            @ModelAttribute UpdateUserProfileRequest request,
            @RequestPart(required = false, name = "profileImage") MultipartFile profileImage,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        try {
            String message = profileUpdateService.updateProfile(
                            userId,
                            request,
                            profileImage
                    );

            return ResponseEntity.ok(Map.of("message", message));

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
}