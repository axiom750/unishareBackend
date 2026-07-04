package com.unishare.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserProfileRequest {

    @Size(min = 3, max = 20)
    private String username;

    private String userBio;

    private String universityName;
}