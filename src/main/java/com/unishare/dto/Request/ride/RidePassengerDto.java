package com.unishare.dto.Request.ride;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RidePassengerDto {

    private Long passengerId;
    private String email;
    private String username;
    private String userProfilePictureUrl;
    private String userBio;
    private int seatsRequested;

}