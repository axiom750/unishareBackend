package com.unishare.repository.ride;

import com.unishare.dto.ride.RidePassengerDto;
import com.unishare.entity.ride.RideRequest;
import com.unishare.enums.ride.RideRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RideRequestRepository extends JpaRepository<RideRequest, UUID> {

    Page<RideRequest> findByRideId(UUID rideId,Pageable pageable);

    Optional<RideRequest> findByRideIdAndPassengerIdAndStatus(
            UUID rideId,
            Long passengerId,
            RideRequestStatus status
    );

    Page<RideRequest> findByPassengerId(
            Long passengerId,
            Pageable pageable
    );

    Page<RideRequest> findByRideIdAndStatus(
            UUID rideId,
            RideRequestStatus status,
            Pageable pageable
    );

    @Query("""
    SELECT new com.unishare.unishare.dto.ride.RidePassengerDto(
        u.id,
        u.email,
        u.username,
        u.userProfilePictureURL,
        u.userBio,
        rr.seatsRequested
    )
    FROM RideRequest rr
    JOIN User u ON rr.passengerId = u.id
    WHERE rr.rideId = :rideId
    AND rr.status = com.unishare.unishare.enums.ride.RideRequestStatus.CONFIRMED
    """)
    Page<RidePassengerDto> findConfirmedPassengers(UUID rideId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
    UPDATE RideRequest r
    SET r.status = com.unishare.unishare.enums.ride.RideRequestStatus.CANCELLED,
        r.respondedAt = CURRENT_TIMESTAMP
    WHERE r.rideId = :rideId
    """)
    void cancelAllRequestsByRideId(UUID rideId);

    @Modifying
    @Transactional
    @Query("""
    DELETE FROM RideRequest r
    WHERE r.status = com.unishare.unishare.enums.ride.RideRequestStatus.CANCELLED
    AND r.respondedAt < :threshold
    """)
    void deleteExpiredCancelledRequests(LocalDateTime threshold);
}
