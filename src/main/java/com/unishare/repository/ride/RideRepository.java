package com.unishare.repository.ride;

import com.unishare.entity.ride.Ride;
import com.unishare.enums.ride.RideStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RideRepository extends JpaRepository<Ride, UUID> {


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Ride r WHERE r.id = :id")
    Optional<Ride> findByIdForUpdate(@Param("id") UUID id);

    Page<Ride> findByStatusAndRideDateGreaterThanEqual(
            RideStatus status,
            LocalDate date,
            Pageable pageable
    );

    void deleteByStatusAndCancelledAtBefore(
            RideStatus status,
            LocalDateTime time
    );

    Page<Ride> findByOrganizerId(
            Long organizerId,
            Pageable pageable
    );
}