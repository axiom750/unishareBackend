package com.unishare.redis;

/**
 * Enum representing Redis/Valkey operations for logging and observability.
 * 
 * Used by RedisService to classify and log different types of Redis operations.
 */
public enum RedisOperation {
    GET,
    SET,
    SET_IF_ABSENT,
    EXISTS,
    DELETE,
    EXPIRE,
    TTL,
    HGET,
    HSET,
    HDELETE,
    PING
}
