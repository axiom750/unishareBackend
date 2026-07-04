package com.unishare.controller.ride;

import com.unishare.dto.response.PageResponse;
import com.unishare.dto.ride.RideCreateRequest;
import com.unishare.entity.ride.Ride;
import com.unishare.service.ride.RideRequestService;
import com.unishare.service.ride.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;
    private final RideRequestService rideRequestService;

    @PostMapping
    public ResponseEntity<Ride> createRide(
            @Valid @RequestBody RideCreateRequest request) {

        Ride ride = rideService.createRide(request);

        return ResponseEntity.ok(ride);
    }

    @GetMapping
    public PageResponse<Ride> getAvailableRides(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return rideService.getAvailableRides(page, size);
    }

    @PatchMapping("/{id}")
    public Ride updateRide(
            @PathVariable UUID id,
            @RequestBody RideCreateRequest request
    ) {
        return rideService.updateRide(id, request);
    }

    @DeleteMapping("/{id}")
    public String deleteRide(@PathVariable UUID id) {
        rideService.deleteRide(id);
        return "Ride deleted permanently";
    }

    @GetMapping("/my-rides")
    public PageResponse<Ride> getMyRides(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Page<Ride> result = rideService.getMyRides(page, size);

        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }
}