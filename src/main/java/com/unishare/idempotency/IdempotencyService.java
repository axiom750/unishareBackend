package com.unishare.idempotency;

import com.unishare.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private final RedisService redisService;

    public boolean reserve(String key) {
        return redisService.setIfAbsent(
                key,
                "PROCESSING",
                DEFAULT_TTL
        );
    }
}