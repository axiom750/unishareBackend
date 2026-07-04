package com.unishare.service.user;

import com.unishare.dto.user.UpdateUserProfileRequest;
import com.unishare.entity.user.User;
import com.unishare.repository.user.UserRepository;
import com.unishare.service.media.MediaService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProfileUpdateService {

    private final UserRepository userRepository;
    private final MediaService mediaService;

    @Transactional
    public String updateProfile(
            Long userId,
            UpdateUserProfileRequest request,
            MultipartFile profileImage) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean updated = false;

        // USERNAME
        if (request != null && request.getUsername() != null) {

            String newUsername = request.getUsername().trim();

            if (newUsername.isBlank())
                throw new IllegalArgumentException("Username cannot be blank");

            if (!newUsername.matches("^[a-zA-Z0-9_]+$"))
                throw new IllegalArgumentException("Invalid username format");

            if (!newUsername.equals(user.getUsername())) {

                if (userRepository.existsByUsername(newUsername))
                    throw new IllegalArgumentException("Username already taken");

                user.setUsername(newUsername);
                updated = true;
            }
        }

        // BIO
        if (request != null &&
                request.getUserBio() != null &&
                !request.getUserBio().equals(user.getUserBio())) {

            user.setUserBio(request.getUserBio());
            updated = true;
        }

        //UNIVERSITY NAME
        if(request != null &&
                request.getUniversityName() != null &&
                !request.getUniversityName().equals(user.getUniversityName())){
            user.setUniversityName(request.getUniversityName());
            updated = true;
        }

        // PROFILE IMAGE
        if (profileImage != null && !profileImage.isEmpty()) {

            String folder = "unishare/users/" + userId;
            String publicId = "profile";

            Map<String, String> result =
                    mediaService.uploadImage(
                            profileImage,
                            folder,
                            publicId,
                            true
                    );

            user.setUserProfilePictureURL(result.get("url"));
            user.setProfilePicturePublicId(result.get("publicId"));

            updated = true;
        }

        if (!updated)
            return "No changes detected";

        return "Profile updated successfully";
    }
}