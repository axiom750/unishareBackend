package com.unishare.dto.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.unishare.entity.auth.Role;
import com.unishare.entity.user.User;
import com.unishare.entity.user.UserProfile;
import com.unishare.enums.auth.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {

    private Long id;

    private String email;

    private String username;

    /**
     * User's roles - represents the new N:N relationship.
     * Returns role names as strings for API compatibility.
     */
    private Set<String> roles;

    private AuthProvider authProvider;

    private boolean active;

    private UserProfileDTO profile;


    public static UserDTO fromEntity(User user) {

        if (user == null) {
            return null;
        }

        // Convert Set<Role> to Set<String> role names
        Set<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet())
                : Set.of();

        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .roles(roleNames)  // Use role names instead of enum
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