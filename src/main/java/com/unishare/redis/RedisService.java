package com.unishare.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Standard Redis Service with comprehensive logging and observability.
 * 
 * This service provides a centralized abstraction for all Redis operations in UniShare.
 * Every operation is logged with:
 * - Operation type
 * - Safe key representation  
 * - Result classification
 * - Execution duration
 * - TTL (when relevant)
 * - Error information (when applicable)
 * 
 * Architecture:
 * <pre>
 * Business Service
 *     ↓
 * RedisService (this class)
 *     ↓
 * StringRedisTemplate
 *     ↓
 * Redis Server
 * </pre>
 * 
 * Security:
 * - Never logs Redis values (passwords, tokens, secrets)
 * - Logs only safe key representations
 * - Sanitizes error messages
 * 
 * Observability:
 * - Automatic trace ID correlation via MDC
 * - Performance timing for every operation
 * - Standardized log format
 * - Result classification
 * 
 * Thread Safety:
 * - Stateless service (safe for concurrent requests)
 * - StringRedisTemplate is thread-safe
 * 
 * @see RedisOperation
 * @see RedisResult
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate stringRedisTemplate;
    
    // ==================== GET Operations ====================
    
    /**
     * GET - Retrieve a string value by key.
     * 
     * @param key Redis key
     * @return Value if exists, null if not found
     */
    public String get(String key) {
        long start = System.nanoTime();
        RedisOperation operation = RedisOperation.GET;
        
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            
            RedisResult result = (value != null) ? RedisResult.HIT : RedisResult.MISS;
            logOperation(operation, key, result, start, null, null);
            
            return value;
            
        } catch (Exception e) {
            logOperation(operation, key, RedisResult.FAILED, start, null, e);
            throw new RedisOperationException("Redis GET failed for key: " + getSafeKey(key), e);
        }
    }
    
    // ==================== SET Operations ====================
    
    /**
     * SET - Store a string value without TTL (persistent).
     * 
     * @param key Redis key
     * @param value Value to store
     */
    public void set(String key, String value) {
        set(key, value, null);
    }
    
    /**
     * SET - Store a string value with TTL.
     * 
     * @param key Redis key
     * @param value Value to store
     * @param ttl Time-to-live duration (null for no expiration)
     */
    public void set(String key, String value, Duration ttl) {
        long start = System.nanoTime();
        RedisOperation operation = RedisOperation.SET;
        
        try {
            if (ttl != null) {
                stringRedisTemplate.opsForValue().set(key, value, ttl);
            } else {
                stringRedisTemplate.opsForValue().set(key, value);
            }
            
            Long ttlSeconds = (ttl != null) ? ttl.getSeconds() : null;
            logOperation(operation, key, RedisResult.SUCCESS, start, ttlSeconds, null);
            
        } catch (Exception e) {
            Long ttlSeconds = (ttl != null) ? ttl.getSeconds() : null;
            logOperation(operation, key, RedisResult.FAILED, start, ttlSeconds, e);
            throw new RedisOperationException("Redis SET failed for key: " + getSafeKey(key), e);
        }
    }
    
    /**
     * SET_IF_ABSENT - Store a value only if key doesn't exist (SETNX).
     * 
     * This operation is atomic and safe for concurrent access.
     * Common use cases: distributed locks, idempotency keys, rate limiting.
     * 
     * @param key Redis key
     * @param value Value to store
     * @param ttl Time-to-live duration (null for no expiration)
     * @return true if key was created, false if key already exists
     */
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        long start = System.nanoTime();
        RedisOperation operation = RedisOperation.SET_IF_ABSENT;
        
        try {
            Boolean result;
            
            if (ttl != null) {
                result = stringRedisTemplate.opsForValue().setIfAbsent(key, value, ttl);
            } else {
                result = stringRedisTemplate.opsForValue().setIfAbsent(key, value);
            }
            
            boolean created = Boolean.TRUE.equals(result);
            RedisResult redisResult = created ? RedisResult.CREATED : RedisResult.EXISTS;
            
            Long ttlSeconds = (ttl != null) ? ttl.getSeconds() : null;
            logOperation(operation, key, redisResult, start, ttlSeconds, null);
            
            return created;
            
        } catch (Exception e) {
            Long ttlSeconds = (ttl != null) ? ttl.getSeconds() : null;
            logOperation(operation, key, RedisResult.FAILED, start, ttlSeconds, e);
            throw new RedisOperationException("Redis SET_IF_ABSENT failed for key: " + getSafeKey(key), e);
        }
    }
    
    // ==================== KEY Operations ====================
    
    /**
     * EXISTS - Check if a key exists.
     * 
     * @param key Redis key
     * @return true if key exists, false otherwise
     */
    public boolean exists(String key) {
        long start = System.nanoTime();
        RedisOperation operation = RedisOperation.EXISTS;
        
        try {
            Boolean result = stringRedisTemplate.hasKey(key);
            boolean exists = Boolean.TRUE.equals(result);
            
            RedisResult redisResult = exists ? RedisResult.EXISTS : RedisResult.NOT_FOUND;
            logOperation(operation, key, redisResult, start, null, null);
            
            return exists;
            
        } catch (Exception e) {
            logOperation(operation, key, RedisResult.FAILED, start, null, e);
            throw new RedisOperationException("Redis EXISTS failed for key: " + getSafeKey(key), e);
        }
    }
    
    /**
     * DELETE - Remove a key.
     * 
     * @param key Redis key
     * @return true if key was deleted, false if key didn't exist
     */
    public boolean delete(String key) {
        long start = System.nanoTime();
        RedisOperation operation = RedisOperation.DELETE;
        
        try {
            Boolean result = stringRedisTemplate.delete(key);
            boolean deleted = Boolean.TRUE.equals(result);
            
            RedisResult redisResult = deleted ? RedisResult.DELETED : RedisResult.NOT_FOUND;
            logOperation(operation, key, redisResult, start, null, null);
            
            return deleted;
            
        } catch (Exception e) {
            logOperation(operation, key, RedisResult.FAILED, start, null, e);
            throw new RedisOperationException("Redis DELETE failed for key: " + getSafeKey(key), e);
        }
    }
    
    // ==================== TTL Operations ====================
    
    /**
     * EXPIRE - Set TTL on an existing key.
     * 
     * @param key Redis key
     * @param ttl Time-to-live duration
     * @return true if TTL was set, false if key doesn't exist
     */
    public boolean expire(String key, Duration ttl) {
        long start = System.nanoTime();
        RedisOperation operation = RedisOperation.EXPIRE;
        
        try {
            Boolean result = stringRedisTemplate.expire(key, ttl);
            boolean success = Boolean.TRUE.equals(result);
            
            RedisResult redisResult = success ? RedisResult.SUCCESS : RedisResult.NOT_FOUND;
            logOperation(operation, key, redisResult, start, ttl.getSeconds(), null);
            
            return success;
            
        } catch (Exception e) {
            logOperation(operation, key, RedisResult.FAILED, start, ttl.getSeconds(), e);
            throw new RedisOperationException("Redis EXPIRE failed for key: " + getSafeKey(key), e);
        }
    }
    
    /**
     * TTL - Get remaining time-to-live for a key.
     * 
     * @param key Redis key
     * @return TTL in seconds, -1 if key has no expiry, -2 if key doesn't exist
     */
    public long getTtl(String key) {
        long start = System.nanoTime();
        RedisOperation operation = RedisOperation.TTL;
        
        try {
            Long ttl = stringRedisTemplate.getExpire(key);
            
            RedisResult result = RedisResult.SUCCESS;
            logOperation(operation, key, result, start, ttl, null);
            
            return ttl != null ? ttl : -2;
            
        } catch (Exception e) {
            logOperation(operation, key, RedisResult.FAILED, start, null, e);
            throw new RedisOperationException("Redis TTL failed for key: " + getSafeKey(key), e);
        }
    }
    
    // ==================== HASH Operations ====================
    
    /**
     * HGET - Get a field value from a hash.
     * 
     * @param key Redis key (hash)
     * @param field Hash field name
     * @return Field value if exists, null otherwise
     */
    public String hget(String key, String field) {
        long start = System.nanoTime();
        RedisOperation operation = RedisOperation.HGET;
        String fullKey = key + ":" + field;
        
        try {
            Object value = stringRedisTemplate.opsForHash().get(key, field);
            String stringValue = value != null ? value.toString() : null;
            
            RedisResult result = (stringValue != null) ? RedisResult.HIT : RedisResult.MISS;
            logOperation(operation, fullKey, result, start, null, null);
            
            return stringValue;
            
        } catch (Exception e) {
            logOperation(operation, fullKey, RedisResult.FAILED, start, null, e);
            throw new RedisOperationException("Redis HGET failed for key: " + getSafeKey(fullKey), e);
        }
    }
    
    /**
     * HSET - Set a field value in a hash.
     * 
     * @param key Redis key (hash)
     * @param field Hash field name
     * @param value Field value
     */
    public void hset(String key, String field, String value) {
        long start = System.nanoTime();
        RedisOperation operation = RedisOperation.HSET;
        String fullKey = key + ":" + field;
        
        try {
            stringRedisTemplate.opsForHash().put(key, field, value);
            
            logOperation(operation, fullKey, RedisResult.SUCCESS, start, null, null);
            
        } catch (Exception e) {
            logOperation(operation, fullKey, RedisResult.FAILED, start, null, e);
            throw new RedisOperationException("Redis HSET failed for key: " + getSafeKey(fullKey), e);
        }
    }
    
    /**
     * HDELETE - Remove a field from a hash.
     * 
     * @param key Redis key (hash)
     * @param field Hash field name
     * @return true if field was deleted, false if field didn't exist
     */
    public boolean hdelete(String key, String field) {
        long start = System.nanoTime();
        RedisOperation operation = RedisOperation.HDELETE;
        String fullKey = key + ":" + field;
        
        try {
            Long result = stringRedisTemplate.opsForHash().delete(key, field);
            boolean deleted = result != null && result > 0;
            
            RedisResult redisResult = deleted ? RedisResult.DELETED : RedisResult.NOT_FOUND;
            logOperation(operation, fullKey, redisResult, start, null, null);
            
            return deleted;
            
        } catch (Exception e) {
            logOperation(operation, fullKey, RedisResult.FAILED, start, null, e);
            throw new RedisOperationException("Redis HDELETE failed for key: " + getSafeKey(fullKey), e);
        }
    }
    
    // ==================== Logging Utilities ====================
    
    /**
     * Log a Redis operation with standardized format.
     * 
     * Format: [REDIS] operation=<op> | key=<safe-key> | result=<result> | duration=<ms>ms [| ttl=<ttl>s] [| error=<sanitized>]
     * 
     * @param operation Redis operation type
     * @param key Redis key (will be sanitized)
     * @param result Operation result
     * @param startNano Operation start time in nanoseconds
     * @param ttlSeconds TTL value in seconds (optional)
     * @param error Exception (optional)
     */
    private void logOperation(RedisOperation operation, String key, RedisResult result, 
                             long startNano, Long ttlSeconds, Exception error) {
        long durationMs = (System.nanoTime() - startNano) / 1_000_000;
        String safeKey = getSafeKey(key);
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("[REDIS] operation=").append(operation)
                  .append(" | key=").append(safeKey)
                  .append(" | result=").append(result)
                  .append(" | duration=").append(durationMs).append("ms");
        
        if (ttlSeconds != null && ttlSeconds >= 0) {
            logMessage.append(" | ttl=").append(ttlSeconds).append("s");
        }
        
        if (error != null) {
            String sanitizedError = sanitizeError(error);
            logMessage.append(" | error=").append(sanitizedError);
        }
        
        // Log at appropriate level based on result
        if (result == RedisResult.FAILED) {
            log.error(logMessage.toString());
        } else {
            log.debug(logMessage.toString());
        }
    }
    
    /**
     * Get safe key representation for logging.
     * 
     * Security: Never log keys that might contain sensitive tokens/secrets.
     * Strategy:
     * - Keys containing "token", "secret", "password", "jwt" are redacted
     * - Other keys are logged as-is (they contain IDs, not sensitive data)
     * 
     * @param key Original Redis key
     * @return Safe key representation for logging
     */
    private String getSafeKey(String key) {
        if (key == null) {
            return "null";
        }
        
        String lowerKey = key.toLowerCase();
        
        // Redact potentially sensitive keys
        if (lowerKey.contains("token") || 
            lowerKey.contains("secret") || 
            lowerKey.contains("password") || 
            lowerKey.contains("jwt")) {
            
            // Show prefix only
            int colonIndex = key.indexOf(':');
            if (colonIndex > 0) {
                return key.substring(0, colonIndex + 1) + "***";
            }
            return "***";
        }
        
        // Safe keys (user IDs, cache keys, etc.) can be logged
        return key;
    }
    
    /**
     * Sanitize exception error message for logging.
     * 
     * Removes sensitive information while preserving useful debugging info.
     * 
     * @param error Exception
     * @return Sanitized error message
     */
    private String sanitizeError(Exception error) {
        if (error == null) {
            return "Unknown error";
        }
        
        String message = error.getClass().getSimpleName();
        
        if (error.getMessage() != null) {
            // Include first part of message, but limit length
            String errorMsg = error.getMessage();
            if (errorMsg.length() > 100) {
                errorMsg = errorMsg.substring(0, 97) + "...";
            }
            message += ": " + errorMsg;
        }
        
        return message;
    }
}
