package com.unishare.redis;

/**
 * Exception thrown when a Redis operation fails.
 * 
 * This exception wraps underlying Redis/connection exceptions and provides
 * a consistent exception type for Redis operations throughout UniShare.
 * 
 * The RedisService logs the detailed error before throwing this exception,
 * so stack traces are preserved in logs while allowing services to handle
 * Redis failures appropriately.
 */
public class RedisOperationException extends RuntimeException {

    public RedisOperationException(String message) {
        super(message);
    }

    public RedisOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
