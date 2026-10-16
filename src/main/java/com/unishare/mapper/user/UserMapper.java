
package com.unishare.mapper.user;

import com.unishare.dto.response.user.UserProfileResponse;
import com.unishare.dto.response.user.UserResponse;
import com.unishare.entity.rbac.Role;
import com.unishare.entity.user.User;
import com.unishare.entity.user.UserProfile;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {

        UserProfileResponse profileResponse = null;

        UserProfile profile = user.getProfile();

        if (profile != null) {
            profileResponse = UserProfileResponse.builder()
                    .bio(profile.getBio())
                    .profilePictureUrl(
                            profile.getProfilePictureUrl())
                    .universityName(
                            profile.getUniversityName())
                    .dateOfBirth(profile.getDateOfBirth())
                    .build();
        }

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .authProvider(user.getAuthProvider())
                .active(user.isActive())
                .profile(profileResponse)
                .build();
    }
}
