package com.unishare.redis;

/**
 * Standardized Redis operation result classifications for logging and monitoring.
 * 
 * These result types provide consistent observability for Redis operations
 * across the UniShare application.
 * 
 * @see RedisService
 */
public enum RedisResult {
    /**
     * Operation completed successfully (generic success)
     */
    SUCCESS,
    
    /**
     * GET operation found the key (cache hit)
     */
    HIT,
    
    /**
     * GET operation didn't find the key (cache miss)
     */
    MISS,
    
    /**
     * SET_IF_ABSENT created a new key (key didn't exist)
     */
    CREATED,
    
    /**
     * SET_IF_ABSENT didn't create key (key already exists)
     */
    EXISTS,
    
    /**
     * DELETE removed an existing key
     */
    DELETED,
    
    /**
     * DELETE didn't find the key to delete
     */
    NOT_FOUND,
    
    /**
     * Operation failed due to Redis exception or error
     */
    FAILED
}
