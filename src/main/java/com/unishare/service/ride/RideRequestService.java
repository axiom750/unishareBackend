package com.unishare.service.ride;

import com.unishare.dto.ride.RidePassengerDto;
import com.unishare.dto.ride.RideRequestCreateDto;
import com.unishare.entity.ride.Ride;
import com.unishare.entity.ride.RideRequest;
import com.unishare.enums.ride.RideRequestStatus;
import com.unishare.enums.ride.RideStatus;
import com.unishare.repository.ride.RideRepository;
import com.unishare.repository.ride.RideRequestRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RideRequestService {

    private final RideRepository rideRepository;
    private final RideRequestRepository rideRequestRepository;

    public void requestRide(UUID rideId, RideRequestCreateDto dto) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new RuntimeException("Unauthorized");
        }
        Long passengerId = (Long) auth.getPrincipal();

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (ride.getStatus() != RideStatus.ACTIVE) {
            throw new RuntimeException("Ride not active");
        }

        if (ride.getOrganizerId().equals(passengerId)) {
            throw new RuntimeException("Cannot request your own ride");
        }

        if (dto.getSeatsRequested() > ride.getAvailableSeats()) {
            throw new RuntimeException("Not enough seats available");
        }

        Optional<RideRequest> existing =
                rideRequestRepository.findByRideIdAndPassengerIdAndStatus(
                        rideId,
                        passengerId,
                        RideRequestStatus.PENDING
                );

        if (existing.isPresent()) {
            throw new RuntimeException("You already have a pending request");
        }

        RideRequest request = new RideRequest();

        request.setRideId(rideId);
        request.setPassengerId(passengerId);
        request.setSeatsRequested(dto.getSeatsRequested());
        request.setMessage(dto.getMessage());
        request.setStatus(RideRequestStatus.PENDING);

        rideRequestRepository.save(request);
    }

    public Page<RideRequest> getMyRideRequests(int page, int size) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new RuntimeException("Unauthorized");
        }

        Long passengerId = (Long) auth.getPrincipal();

        Pageable pageable = PageRequest.of(page, size);

        return rideRequestRepository.findByPassengerId(passengerId, pageable);
    }

    @Transactional
    public void approveRideRequest(UUID requestId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getPrincipal() instanceof Long organizerId)) {
            throw new RuntimeException("Unauthorized");
        }

        RideRequest request = rideRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Ride request not found"));

        Ride ride = rideRepository.findById(request.getRideId())
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (!ride.getOrganizerId().equals(organizerId)) {
            throw new RuntimeException("You are not the ride organizer");
        }

        if (request.getStatus() != RideRequestStatus.PENDING) {
            throw new RuntimeException("Request already processed");
        }

        if (request.getSeatsRequested() > ride.getAvailableSeats()) {
            throw new RuntimeException("Not enough seats available");
        }

        request.setStatus(RideRequestStatus.CONFIRMED);

        ride.setAvailableSeats(
                (short)(ride.getAvailableSeats() - request.getSeatsRequested()
        ));

        if (ride.getAvailableSeats() == 0) {
            ride.setStatus(RideStatus.FULL);
        }

        rideRequestRepository.save(request);
        rideRepository.save(ride);
    }
    @Transactional
    public void declineRideRequest(UUID requestId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getPrincipal() instanceof Long organizerId)) {
            throw new RuntimeException("Unauthorized");
        }

        RideRequest request = rideRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Ride request not found"));

        Ride ride = rideRepository.findById(request.getRideId())
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (!ride.getOrganizerId().equals(organizerId)) {
            throw new RuntimeException("You are not the ride organizer");
        }

        if (request.getStatus() != RideRequestStatus.PENDING) {
            throw new RuntimeException("Request already processed");
        }

        request.setStatus(RideRequestStatus.DECLINED);

        rideRequestRepository.save(request);
    }

    @Transactional
    public void cancelRideRequest(UUID requestId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getPrincipal() instanceof Long passengerId)) {
            throw new RuntimeException("Unauthorized");
        }

        RideRequest request = rideRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Ride request not found"));

        if (!request.getPassengerId().equals(passengerId)) {
            throw new RuntimeException("You cannot cancel this request");
        }

        if (request.getStatus() == RideRequestStatus.CONFIRMED) {

            Ride ride = rideRepository.findById(request.getRideId())
                    .orElseThrow(() -> new RuntimeException("Ride not found"));

            ride.setAvailableSeats(
                    (short) (ride.getAvailableSeats() + request.getSeatsRequested()
            ));

            rideRepository.save(ride);
        }

        request.setStatus(RideRequestStatus.CANCELLED);

        rideRequestRepository.save(request);
    }

    public Page<RideRequest> getRideRequests(UUID rideId, int page, int size) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getPrincipal() instanceof Long organizerId)) {
            throw new RuntimeException("Unauthorized");
        }

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (!ride.getOrganizerId().equals(organizerId)) {
            throw new RuntimeException("Not your ride");
        }

        Pageable pageable = PageRequest.of(page, size);

        return rideRequestRepository.findByRideId(rideId, pageable);
    }

    public Page<RideRequest> getRideRequestsForMyRide(
            UUID rideId,
            int page,
            int size
    ) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getPrincipal() instanceof Long organizerId)) {
            throw new RuntimeException("Unauthorized");
        }

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (!ride.getOrganizerId().equals(organizerId)) {
            throw new RuntimeException("You are not the organizer of this ride");
        }

        Pageable pageable = PageRequest.of(page, size);

        return rideRequestRepository.findByRideId(rideId, pageable);
    }

    public Page<RidePassengerDto> getConfirmedPassengers(
            UUID rideId,
            int page,
            int size
    ) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getPrincipal() instanceof Long organizerId)) {
            throw new RuntimeException("Unauthorized");
        }

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (!ride.getOrganizerId().equals(organizerId)) {
            throw new RuntimeException("Not your ride");
        }

        Pageable pageable = PageRequest.of(page, size);

        return rideRequestRepository.findConfirmedPassengers(
                rideId,
                pageable
        );
    }
}
