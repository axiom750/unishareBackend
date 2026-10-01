package com.unishare.dto.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.unishare.entity.user.User;
import com.unishare.entity.user.UserProfile;
import com.unishare.enums.auth.AuthProvider;
import com.unishare.enums.user.Roles;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {

    private Long id;

    private String email;

    private String username;

    private Roles role;

    private AuthProvider authProvider;

    private boolean active;

    private UserProfileDTO profile;


    public static UserDTO fromEntity(User user) {

        if (user == null) {
            return null;
        }

        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .authProvider(user.getAuthProvider())
                .active(user.isActive())
                .profile(UserProfileDTO.fromEntity(user.getProfile()))
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserProfileDTO {

        private String bio;

        private String profilePictureUrl;

        private String universityName;

        private LocalDate dateOfBirth;


        public static UserProfileDTO fromEntity(
                UserProfile profile) {

            if (profile == null) {
                return null;
            }

            return UserProfileDTO.builder()
                    .bio(profile.getBio())
                    .profilePictureUrl(
                            profile.getProfilePictureUrl()
                    )
                    .universityName(
                            profile.getUniversityName()
                    )
                    .dateOfBirth(
                            profile.getDateOfBirth()
                    )
                    .build();
        }
    }
}