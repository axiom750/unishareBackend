package com.unishare.dto.request.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {

    @Size(
            min = 3,
            max = 50,
            message = "Username must be between 3 and 50 characters"
    )
    @Pattern(
            regexp = "^[a-zA-Z0-9_]+$",
            message = "Username can only contain letters, numbers and underscores"
    )
    private String username;

    @Size(
            max = 500,
            message = "Bio cannot exceed 500 characters"
    )
    private String bio;

    @Size(
            max = 200,
            message = "University name cannot exceed 200 characters"
    )
    private String universityName;

}