package com.unishare.redis;

/**
 * Enum representing Redis/Valkey operation results for logging and observability.
 * 
 * Used by RedisService to classify and log operation outcomes.
 */
public enum RedisResult {
    SUCCESS,      // Operation completed successfully
    CREATED,      // Key was created (SET_IF_ABSENT)
    EXISTS,       // Key already exists
    HIT,          // Cache hit (GET returned value)
    MISS,         // Cache miss (GET returned null)
    DELETED,      // Key was deleted
    NOT_FOUND,    // Key does not exist
    FAILED        // Operation failed with exception
}
