package com.unishare.unishare.service.ride;

import com.unishare.unishare.enums.ride.RideStatus;
import com.unishare.unishare.repository.ride.RideRepository;
import com.unishare.unishare.repository.ride.RideRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RideCleanUpService {

    private final RideRepository rideRepository;
    private final RideRequestRepository rideRequestRepository;

    // Runs every hour to clean rides
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void deleteExpiredCancelledRides() {

        LocalDateTime threshold = LocalDateTime.now().minusHours(48);

        rideRepository.deleteByStatusAndCancelledAtBefore(
                RideStatus.CANCELLED,
                threshold
        );
    }

    // Runs every hour but 10 minutes later
    @Scheduled(cron = "0 10 * * * *")
    @Transactional
    public void deleteExpiredCancelledRequests() {

        LocalDateTime threshold = LocalDateTime.now().minusHours(24);

        rideRequestRepository.deleteExpiredCancelledRequests(threshold);
    }
}
