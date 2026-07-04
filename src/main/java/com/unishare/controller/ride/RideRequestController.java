package com.unishare.controller.ride;

import com.unishare.dto.response.PageResponse;
import com.unishare.dto.ride.RidePassengerDto;
import com.unishare.dto.ride.RideRequestCreateDto;
import com.unishare.entity.ride.RideRequest;
import com.unishare.service.ride.RideRequestService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/rides")
public class RideRequestController {

    private final RideRequestService rideRequestService;

    @PostMapping("/{rideId}/request")
    public PageResponse<RideRequest> requestRide(
            @PathVariable UUID rideId,
            @RequestBody RideRequestCreateDto dto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        rideRequestService.requestRide(rideId, dto);

        Page<RideRequest> result =
                rideRequestService.getMyRideRequests(page, size);

        return mapPage(result);
    }

    @GetMapping("/my-requests")
    public PageResponse<RideRequest> getMyRideRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Page<RideRequest> result =
                rideRequestService.getMyRideRequests(page, size);

        return mapPage(result);
    }

    @PostMapping("/{requestId}/approve")
    public PageResponse<RideRequest> approveRequest(
            @PathVariable UUID requestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        rideRequestService.approveRideRequest(requestId);

        Page<RideRequest> result =
                rideRequestService.getMyRideRequests(page, size);

        return mapPage(result);
    }

    @PostMapping("/{requestId}/decline")
    public PageResponse<RideRequest> declineRequest(
            @PathVariable UUID requestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        rideRequestService.declineRideRequest(requestId);

        Page<RideRequest> result =
                rideRequestService.getMyRideRequests(page, size);

        return mapPage(result);
    }

    @PostMapping("/{requestId}/cancel")
    public PageResponse<RideRequest> cancelRequest(
            @PathVariable UUID requestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        rideRequestService.cancelRideRequest(requestId);

        Page<RideRequest> result =
                rideRequestService.getMyRideRequests(page, size);

        return mapPage(result);
    }

    @GetMapping("/{rideId}/requests")
    public PageResponse<RideRequest> getRideRequests(
            @PathVariable UUID rideId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Page<RideRequest> result =
                rideRequestService.getRideRequestsForMyRide(rideId, page, size);

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

    @GetMapping("/{rideId}/passengers")
    public PageResponse<RidePassengerDto> getPassengers(
            @PathVariable UUID rideId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Page<RidePassengerDto> result =
                rideRequestService.getConfirmedPassengers(rideId, page, size);

        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }
}