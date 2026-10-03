package com.unishare.controller.ride;

import com.unishare.dto.response.PageResponse;
import com.unishare.dto.request.ride.RidePassengerDto;
import com.unishare.dto.request.ride.RideRequestCreateDto;
import com.unishare.annotation.Permission;
import com.unishare.entity.ride.Ride;
import com.unishare.entity.ride.RideRequest;
import com.unishare.entity.user.User;
import com.unishare.entity.user.UserProfile;
import com.unishare.enums.rbac.PermissionDomain;
import com.unishare.service.ride.RideRequestService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/rides")
public class RideRequestController {

    private final RideRequestService rideRequestService;

    @Permission(
            id = "01a10260-9596-749e-8f93-9114ac891dde",
            displayName = "Request Ride Seat",
            description = "Allows a user to request seats on a ride.",
            baseEntity = RideRequest.class,
            reachableEntities = {Ride.class},
            domain = PermissionDomain.APPLICATION
    )
    @PreAuthorize("hasAuthority('RIDE_REQUEST_CREATE')")
    @PostMapping("/{rideId}/request")
    public PageResponse<RideRequest> requestRide(
            @PathVariable UUID rideId,
            @RequestBody RideRequestCreateDto dto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        rideRequestService.requestRide(rideId, dto);
        Page<RideRequest> result = rideRequestService.getMyRideRequests(page, size);
        return mapPage(result);
    }

    @Permission(
            id = "01a1026a-654d-7618-aee4-bfb14c736d9e",
            displayName = "View Own Ride Requests",
            description = "Allows a passenger to list their own seat requests.",
            baseEntity = RideRequest.class,
            reachableEntities = {},
            domain = PermissionDomain.APPLICATION
    )
    @PreAuthorize("hasAuthority('RIDE_REQUEST_VIEW_OWN')")
    @GetMapping("/my-requests")
    public PageResponse<RideRequest> getMyRideRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Page<RideRequest> result = rideRequestService.getMyRideRequests(page, size);
        return mapPage(result);
    }

    @Permission(
            id = "01a10260-9597-703f-89d5-a1462899bcb4",
            displayName = "Approve Ride Request",
            description = "Allows a ride owner to approve a seat request, reducing the ride's available seats.",
            baseEntity = RideRequest.class,
            reachableEntities = {Ride.class},
            domain = PermissionDomain.APPLICATION
    )
    @PreAuthorize("hasAuthority('RIDE_REQUEST_APPROVE')")
    @PostMapping("/{requestId}/approve")
    public PageResponse<RideRequest> approveRequest(
            @PathVariable UUID requestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        rideRequestService.approveRideRequest(requestId);
        Page<RideRequest> result = rideRequestService.getMyRideRequests(page, size);
        return mapPage(result);
    }

    @Permission(
            id = "01a10260-9598-7bdc-b1d6-722407d5dcf8",
            displayName = "Decline Ride Request",
            description = "Allows a ride owner to decline a seat request.",
            baseEntity = RideRequest.class,
            reachableEntities = {Ride.class},
            domain = PermissionDomain.APPLICATION
    )
    @PreAuthorize("hasAuthority('RIDE_REQUEST_DECLINE')")
    @PostMapping("/{requestId}/decline")
    public PageResponse<RideRequest> declineRequest(
            @PathVariable UUID requestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        rideRequestService.declineRideRequest(requestId);
        Page<RideRequest> result = rideRequestService.getMyRideRequests(page, size);
        return mapPage(result);
    }

    @Permission(
            id = "01a10260-9599-7195-a004-8ebce2f2baf5",
            displayName = "Cancel Ride Request",
            description = "Allows a passenger to cancel their seat request, restoring seats if it was confirmed.",
            baseEntity = RideRequest.class,
            reachableEntities = {Ride.class},
            domain = PermissionDomain.APPLICATION
    )
    @PreAuthorize("hasAuthority('RIDE_REQUEST_CANCEL')")
    @PostMapping("/{requestId}/cancel")
    public PageResponse<RideRequest> cancelRequest(
            @PathVariable UUID requestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        rideRequestService.cancelRideRequest(requestId);
        Page<RideRequest> result = rideRequestService.getMyRideRequests(page, size);
        return mapPage(result);
    }

    @Permission(
            id = "01a1026a-654e-7ecd-92dd-4696991e8289",
            displayName = "View Ride Requests",
            description = "Allows a ride organizer to list the seat requests for their own ride.",
            baseEntity = RideRequest.class,
            reachableEntities = {Ride.class},
            domain = PermissionDomain.APPLICATION
    )
    @PreAuthorize("hasAuthority('RIDE_REQUEST_VIEW')")
    @GetMapping("/{rideId}/requests")
    public PageResponse<RideRequest> getRideRequests(
            @PathVariable UUID rideId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Page<RideRequest> result = rideRequestService.getRideRequestsForMyRide(rideId, page, size);
        return mapPage(result);
    }

    private PageResponse<RideRequest> mapPage(Page<RideRequest> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Permission(
            id = "01a1026a-654f-73e7-ab3e-d27ba7b030ce",
            displayName = "View Ride Passengers",
            description = "Allows a ride organizer to list the confirmed passengers (with contact details) of their own ride.",
            baseEntity = RideRequest.class,
            reachableEntities = {Ride.class, User.class, UserProfile.class},
            domain = PermissionDomain.APPLICATION
    )
    @PreAuthorize("hasAuthority('RIDE_PASSENGER_VIEW')")
    @GetMapping("/{rideId}/passengers")
    public PageResponse<RidePassengerDto> getPassengers(
            @PathVariable UUID rideId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Page<RidePassengerDto> result = rideRequestService.getConfirmedPassengers(rideId, page, size);

        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }
}