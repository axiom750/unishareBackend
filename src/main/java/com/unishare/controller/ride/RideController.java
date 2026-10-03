package com.unishare.controller.ride;

import com.unishare.annotation.Permission;
import com.unishare.dto.response.MessageResponse;
import com.unishare.dto.response.PageResponse;
import com.unishare.dto.request.ride.RideCreateRequest;
import com.unishare.entity.ride.Ride;
import com.unishare.entity.ride.RideRequest;
import com.unishare.enums.rbac.PermissionDomain;
import com.unishare.service.ride.RideRequestService;
import com.unishare.service.ride.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;
    private final RideRequestService rideRequestService;

    @Permission(
            id = "0199c5c4-7b2a-7abc-9e31-4f5a7e8d21c4",
            displayName = "Create Ride",
            description = "Allows a user to create a ride.",
            baseEntity = Ride.class,
            reachableEntities = {},
            domain = PermissionDomain.APPLICATION
    )
    @PreAuthorize("hasAuthority('RIDE_CREATE')")
    @PostMapping
    public ResponseEntity<Ride> createRide(@Valid @RequestBody RideCreateRequest request) {

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

    @Permission(
            id = "01a10260-9594-7180-8758-4d48b398f3e4",
            displayName = "Update Ride",
            description = "Allows a user to update the details of a ride.",
            baseEntity = Ride.class,
            reachableEntities = {},
            domain = PermissionDomain.APPLICATION
    )
    @PreAuthorize("hasAuthority('RIDE_UPDATE')")
    @PatchMapping("/{id}")
    public Ride updateRide(
            @PathVariable UUID id,
            @RequestBody RideCreateRequest request
    ) {
        return rideService.updateRide(id, request);
    }

    @Permission(
            id = "01a10260-9595-7495-9a08-b613d99845f7",
            displayName = "Delete Ride",
            description = "Allows a user to cancel a ride, which also cancels all of its ride requests.",
            baseEntity = Ride.class,
            reachableEntities = {RideRequest.class},
            domain = PermissionDomain.APPLICATION
    )
    @PreAuthorize("hasAuthority('RIDE_DELETE')")
    @DeleteMapping("/{id}")
    public MessageResponse deleteRide(@PathVariable UUID id) {
        rideService.deleteRide(id);
        return new MessageResponse("Ride deleted permanently");
    }

    @Permission(
            id = "01a1026a-654c-7c0b-a1cb-f4fde2de2a84",
            displayName = "View Own Rides",
            description = "Allows a user to list the rides they organize.",
            baseEntity = Ride.class,
            reachableEntities = {},
            domain = PermissionDomain.APPLICATION
    )
    @PreAuthorize("hasAuthority('RIDE_VIEW_OWN')")
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