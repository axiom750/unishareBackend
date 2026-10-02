package com.unishare.redis;

/**
 * Exception thrown when a Redis/Valkey operation fails.
 * 
 * This exception wraps underlying Redis client exceptions and provides
 * a consistent exception type for Redis/Valkey operation failures.
 */
public class RedisOperationException extends RuntimeException {
    
    public RedisOperationException(String message) {
        super(message);
    }
    
    public RedisOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
