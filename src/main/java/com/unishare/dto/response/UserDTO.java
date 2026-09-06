package com.unishare.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.unishare.entity.user.User;
import com.unishare.enums.auth.AuthProvider;
import com.unishare.enums.user.Roles;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {
    
    private Long id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private Roles role;
    private AuthProvider authProvider;
    private boolean active;
    private String userBio;
    private String userProfilePictureURL;
    private String university;
    
    public static UserDTO fromEntity(User user) {
        if (user == null) {
            return null;
        }
        
        String[] nameParts = splitName(user.getUsername());
        
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(nameParts[0])
                .lastName(nameParts[1])
                .role(user.getRole())
                .authProvider(user.getAuthProvider())
                .active(user.isActive())
                .userBio(user.getUserBio())
                .userProfilePictureURL(user.getUserProfilePictureURL())
                .university(user.getUniversityName())
                .build();
    }
    
    private static String[] splitName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return new String[]{"", ""};
        }
        
        String[] parts = fullName.trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "";
        
        return new String[]{firstName, lastName};
    }
}
