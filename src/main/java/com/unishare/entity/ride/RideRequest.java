package com.unishare.entity.ride;


import com.unishare.enums.ride.RideRequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "ride_requests",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"ride_id", "passenger_id"})
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RideRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ride_id", nullable = false)
    private UUID rideId;

    @Column(name = "passenger_id", nullable = false)
    private Long passengerId;

    @Column(nullable = false)
    private short seatsRequested;

    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideRequestStatus status;

    private LocalDateTime respondedAt;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        status = RideRequestStatus.PENDING;
    }

    @PreUpdate
    public void onUpdate() {
        respondedAt = LocalDateTime.now();
    }
}
