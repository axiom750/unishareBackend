package com.unishare.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private String bio;
    private String profilePictureUrl;
    private String universityName;
    private LocalDate dateOfBirth;
}
