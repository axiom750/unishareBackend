package com.unishare.dto.ride;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@Getter
@Setter
public class RideCreateRequest {

    @NotBlank
    private String fromLocation;

    @NotBlank
    private String toLocation;

    @NotNull
    private LocalDate rideDate;

    @NotNull
    private LocalTime rideTime;

    @Min(1)
    private short totalSeats;

    @DecimalMin("0.0")
    private BigDecimal price;

    @NotBlank
    private String vehicleInfo;

    private String description;

    @NotNull
    private Map<String, String> contactInfo;
}