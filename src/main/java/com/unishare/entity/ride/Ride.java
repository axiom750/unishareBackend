package com.unishare.entity.ride;


import com.unishare.enums.ride.RideStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "rides",
        indexes = {
                @Index(name = "idx_ride_organizer", columnList = "organizer_id"),
                @Index(name = "idx_rides_status_date", columnList = "status, ride_date"),
                @Index(name = "idx_cancelled_at", columnList = "cancelledAt")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organizer_id", nullable = false)
    private Long organizerId;

    @Column(name = "from_location", nullable = false)
    private String fromLocation;

    @Column(name = "to_location", nullable = false)
    private String toLocation;

    @Column(name = "ride_date", nullable = false)
    private LocalDate rideDate;

    @Column(name = "ride_time", nullable = false)
    private LocalTime rideTime;

    @Column(name = "total_seats", nullable = false)
    private short totalSeats;

    @Column(name = "available_seats", nullable = false)
    private short availableSeats;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "vehicle_info", nullable = false)
    private String vehicleInfo;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contact_info", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> contactInfo;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        status = RideStatus.ACTIVE;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}