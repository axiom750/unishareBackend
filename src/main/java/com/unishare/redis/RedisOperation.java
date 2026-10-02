package com.unishare.redis;

/**
 * Standardized Redis operation types for logging and monitoring.
 * 
 * These operation names appear in all Redis logs to provide consistent
 * observability across the UniShare application.
 * 
 * @see RedisService
 */
public enum RedisOperation {
    /**
     * GET - Retrieve a value by key
     */
    GET,
    
    /**
     * SET - Store a value with optional TTL
     */
    SET,
    
    /**
     * SET_IF_ABSENT - Store a value only if key doesn't exist (SETNX)
     */
    SET_IF_ABSENT,
    
    /**
     * EXISTS - Check if a key exists
     */
    EXISTS,
    
    /**
     * DELETE - Remove a key
     */
    DELETE,
    
    /**
     * EXPIRE - Set TTL on an existing key
     */
    EXPIRE,
    
    /**
     * TTL - Get remaining time-to-live for a key
     */
    TTL,
    
    /**
     * HGET - Get a field value from a hash
     */
    HGET,
    
    /**
     * HSET - Set a field value in a hash
     */
    HSET,
    
    /**
     * HDELETE - Remove a field from a hash
     */
    HDELETE
}
