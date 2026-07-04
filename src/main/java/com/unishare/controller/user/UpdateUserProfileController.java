package com.unishare.controller.user;

import com.unishare.dto.user.UpdateUserProfileRequest;
import com.unishare.service.user.ProfileUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/update")
@RequiredArgsConstructor
public class UpdateUserProfileController {

    private final ProfileUpdateService profileUpdateService;

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