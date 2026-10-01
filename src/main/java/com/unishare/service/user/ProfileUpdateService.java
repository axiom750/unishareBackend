package com.unishare.service.user;

import com.unishare.dto.request.user.UpdateUserProfileRequest;
import com.unishare.entity.user.User;
import com.unishare.entity.user.UserProfile;
import com.unishare.repository.user.UserProfileRepository;
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
    private final UserProfileRepository userProfileRepository;
    private final MediaService mediaService;

    @Transactional
    public String updateProfile(
            Long userId,
            UpdateUserProfileRequest request,
            MultipartFile profileImage) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        UserProfile profile = userProfileRepository
                .findByUser(user)
                .orElseGet(() -> UserProfile.builder()
                        .user(user)
                        .build());

        boolean updated = false;

        if (request != null &&
                request.getUsername() != null) {

            String newUsername =
                    request.getUsername().trim();

            if (newUsername.isBlank()) {
                throw new IllegalArgumentException(
                        "Username cannot be blank"
                );
            }

            if (!newUsername.matches("^[a-zA-Z0-9_]+$")) {
                throw new IllegalArgumentException(
                        "Invalid username format"
                );
            }

            if (!newUsername.equals(user.getUsername())) {

                if (userRepository.existsByUsername(newUsername)) {
                    throw new IllegalArgumentException(
                            "Username already taken"
                    );
                }

                user.setUsername(newUsername);
                updated = true;
            }
        }

        if (request != null &&
                request.getBio() != null) {

            String newBio = request.getBio().trim();

            if (!newBio.equals(profile.getBio())) {

                profile.setBio(newBio);
                updated = true;
            }
        }

        if (request != null &&
                request.getUniversityName() != null) {

            String newUniversity =
                    request.getUniversityName().trim();

            if (!newUniversity.equals(
                    profile.getUniversityName())) {

                profile.setUniversityName(newUniversity);
                updated = true;
            }
        }

        if (profileImage != null &&
                !profileImage.isEmpty()) {

            String folder =
                    "unishare/users/" + userId;

            String publicId = "profile";

            Map<String, String> result =
                    mediaService.uploadImage(
                            profileImage,
                            folder,
                            publicId,
                            true
                    );

            profile.setProfilePictureUrl(
                    result.get("url")
            );

            profile.setProfilePicturePublicId(
                    result.get("publicId")
            );

            updated = true;
        }


        if (!updated) {
            return "No changes detected";
        }

        userRepository.save(user);
        userProfileRepository.save(profile);

        return "Profile updated successfully";
    }
}