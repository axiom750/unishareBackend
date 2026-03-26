package com.unishare.unishare.service.ride;

import com.unishare.unishare.dto.response.PageResponse;
import com.unishare.unishare.dto.ride.RideCreateRequest;
import com.unishare.unishare.entity.ride.Ride;
import com.unishare.unishare.enums.ride.RideStatus;
import com.unishare.unishare.repository.ride.RideRepository;
import com.unishare.unishare.repository.ride.RideRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;
    private final RideRequestRepository rideRequestRepository;

    public Ride createRide(RideCreateRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Unauthorized");
        }

        Long userId = (Long) authentication.getPrincipal();

        // Date validation
        if (request.getRideDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Ride date cannot be in the past");
        }

        // Contact validation
        validateContactInfo(request.getContactInfo());

        Ride ride = new Ride();
        ride.setOrganizerId(userId);
        ride.setFromLocation(request.getFromLocation());
        ride.setToLocation(request.getToLocation());
        ride.setRideDate(request.getRideDate());
        ride.setRideTime(request.getRideTime());
        ride.setTotalSeats(request.getTotalSeats());
        ride.setAvailableSeats(request.getTotalSeats());
        ride.setPrice(request.getPrice());
        ride.setVehicleInfo(request.getVehicleInfo());
        ride.setDescription(request.getDescription());
        ride.setStatus(RideStatus.ACTIVE);
        ride.setContactInfo(request.getContactInfo());

        return rideRepository.save(ride);
    }

    private void validateContactInfo(Map<String, String> contactInfo) {

        if (contactInfo == null || contactInfo.isEmpty()) {
            throw new RuntimeException("At least one contact method required");
        }

        if (contactInfo.size() > 3) {
            throw new RuntimeException("Maximum 3 contact methods allowed");
        }

        Set<String> allowed = Set.of("email", "phone", "instaId");

        if (!allowed.containsAll(contactInfo.keySet())) {
            throw new RuntimeException("Invalid contact type");
        }
    }

    public PageResponse<Ride> getAvailableRides(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<Ride> ridePage =
                rideRepository.findByStatusAndRideDateGreaterThanEqual(
                        RideStatus.ACTIVE,
                        LocalDate.now(),
                        pageable
                );

        return new PageResponse<>(
                ridePage.getContent(),
                ridePage.getNumber(),
                ridePage.getSize(),
                ridePage.getTotalElements(),
                ridePage.getTotalPages()
        );
    }

    public Ride updateRide(UUID rideId, RideCreateRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Unauthorized");
        }

        Long userId = (Long) authentication.getPrincipal();

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new EntityNotFoundException("Ride not found"));


        if (!ride.getOrganizerId().equals(userId)) {
            throw new RuntimeException("You are not allowed to update this ride");
        }


        if (ride.getStatus() != RideStatus.ACTIVE) {
            throw new RuntimeException("Only active rides can be updated");
        }


        if (request.getRideDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Ride date cannot be in the past");
        }

        validateContactInfo(request.getContactInfo());


        int bookedSeats = ride.getTotalSeats() - ride.getAvailableSeats();

        if (request.getTotalSeats() < bookedSeats) {
            throw new RuntimeException("Total seats cannot be less than already booked seats");
        }

        // Update fields
        ride.setFromLocation(request.getFromLocation());
        ride.setToLocation(request.getToLocation());
        ride.setRideDate(request.getRideDate());
        ride.setRideTime(request.getRideTime());
        ride.setPrice(request.getPrice());
        ride.setVehicleInfo(request.getVehicleInfo());
        ride.setDescription(request.getDescription());
        ride.setTotalSeats(request.getTotalSeats());

        ride.setAvailableSeats((short)(request.getTotalSeats() - bookedSeats));

        ride.setContactInfo(request.getContactInfo());

        return rideRepository.save(ride);
    }

    public void deleteRide(UUID rideId) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Unauthorized");
        }

        Long userId = (Long) authentication.getPrincipal();

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (!ride.getOrganizerId().equals(userId)) {
            throw new RuntimeException("You are not allowed to delete this ride");
        }

        if (ride.getStatus() != RideStatus.ACTIVE) {
            throw new RuntimeException("Ride already inactive");
        }

        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancelledAt(LocalDateTime.now());
        // inform user this ride is canceled and mark the ride_request status as canceled .

        rideRepository.save(ride);
        rideRequestRepository.cancelAllRequestsByRideId(rideId);
    }

    public Page<Ride> getMyRides(int page, int size) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getPrincipal() instanceof Long organizerId)) {
            throw new RuntimeException("Unauthorized");
        }

        Pageable pageable = PageRequest.of(page, size);

        return rideRepository.findByOrganizerId(organizerId, pageable);
    }
}