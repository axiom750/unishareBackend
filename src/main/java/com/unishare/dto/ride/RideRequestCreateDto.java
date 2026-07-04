package com.unishare.dto.ride;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RideRequestCreateDto {
    @Min(1)
    private short seatsRequested;

    private String message;
}
