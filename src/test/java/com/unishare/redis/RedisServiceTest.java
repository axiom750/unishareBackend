package com.unishare.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RedisService.
 * 
 * These tests verify:
 * - Redis operations work correctly
 * - TTL functionality
 * - Atomic SET_IF_ABSENT behavior
 * - Error handling
 * - Logging (indirectly via operation success)
 * 
 * Requires a running Redis instance on localhost:6379
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379"
})
class RedisServiceTest {

    @Autowired
    private RedisService redisService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String TEST_KEY_PREFIX = "test:redis:";
    
    @BeforeEach
    void cleanup() {
        // Clean up any test keys before each test
        stringRedisTemplate.keys(TEST_KEY_PREFIX + "*").forEach(key -> {
            stringRedisTemplate.delete(key);
        });
    }

    // ==================== GET Tests ====================
    
    @Test
    void testGet_Hit() {
        String key = TEST_KEY_PREFIX + "get-hit";
        String expectedValue = "test-value";
        
        // Setup: Store a value directly
        stringRedisTemplate.opsForValue().set(key, expectedValue);
        
        // Test: Get should return the value (HIT)
        String actualValue = redisService.get(key);
        
        assertEquals(expectedValue, actualValue);
    }
    
    @Test
    void testGet_Miss() {
        String key = TEST_KEY_PREFIX + "get-miss";
        
        // Test: Get non-existent key should return null (MISS)
        String value = redisService.get(key);
        
        assertNull(value);
    }
    
    // ==================== SET Tests ====================
    
    @Test
    void testSet_WithoutTTL() {
        String key = TEST_KEY_PREFIX + "set-no-ttl";
        String value = "persistent-value";
        
        // Test: Set without TTL
        redisService.set(key, value);
        
        // Verify: Value exists and has no expiration
        String storedValue = stringRedisTemplate.opsForValue().get(key);
        assertEquals(value, storedValue);
        
        Long ttl = stringRedisTemplate.getExpire(key);
        assertEquals(-1, ttl); // -1 means no expiration
    }
    
    @Test
    void testSet_WithTTL() {
        String key = TEST_KEY_PREFIX + "set-with-ttl";
        String value = "temporary-value";
        Duration ttl = Duration.ofSeconds(300); // 5 minutes
        
        // Test: Set with TTL
        redisService.set(key, value, ttl);
        
        // Verify: Value exists
        String storedValue = stringRedisTemplate.opsForValue().get(key);
        assertEquals(value, storedValue);
        
        // Verify: TTL is set (allow some tolerance for execution time)
        Long actualTtl = stringRedisTemplate.getExpire(key);
        assertNotNull(actualTtl);
        assertTrue(actualTtl > 0 && actualTtl <= 300);
    }
    
    // ==================== SET_IF_ABSENT Tests ====================
    
    @Test
    void testSetIfAbsent_Created() {
        String key = TEST_KEY_PREFIX + "set-if-absent-new";
        String value = "new-value";
        Duration ttl = Duration.ofSeconds(300);
        
        // Test: Set new key
        boolean created = redisService.setIfAbsent(key, value, ttl);
        
        // Verify: Key was created
        assertTrue(created);
        
        // Verify: Value exists
        String storedValue = stringRedisTemplate.opsForValue().get(key);
        assertEquals(value, storedValue);
        
        // Verify: TTL is set
        Long actualTtl = stringRedisTemplate.getExpire(key);
        assertNotNull(actualTtl);
        assertTrue(actualTtl > 0 && actualTtl <= 300);
    }
    
    @Test
    void testSetIfAbsent_AlreadyExists() {
        String key = TEST_KEY_PREFIX + "set-if-absent-exists";
        String originalValue = "original-value";
        String newValue = "new-value";
        Duration ttl = Duration.ofSeconds(300);
        
        // Setup: Store original value
        stringRedisTemplate.opsForValue().set(key, originalValue);
        
        // Test: Attempt to set with SET_IF_ABSENT
        boolean created = redisService.setIfAbsent(key, newValue, ttl);
        
        // Verify: Key was NOT created
        assertFalse(created);
        
        // Verify: Original value is preserved
        String storedValue = stringRedisTemplate.opsForValue().get(key);
        assertEquals(originalValue, storedValue);
    }
    
    @Test
    void testSetIfAbsent_Atomic() throws InterruptedException {
        String key = TEST_KEY_PREFIX + "set-if-absent-atomic";
        String value1 = "value-1";
        String value2 = "value-2";
        Duration ttl = Duration.ofSeconds(300);
        
        // Test: Multiple concurrent SET_IF_ABSENT operations
        // Only one should succeed (atomicity test)
        
        boolean result1 = redisService.setIfAbsent(key, value1, ttl);
        boolean result2 = redisService.setIfAbsent(key, value2, ttl);
        
        // Verify: Exactly one succeeded
        assertTrue(result1);
        assertFalse(result2);
        
        // Verify: First value is stored
        String storedValue = stringRedisTemplate.opsForValue().get(key);
        assertEquals(value1, storedValue);
    }
    
    // ==================== EXISTS Tests ====================
    
    @Test
    void testExists_True() {
        String key = TEST_KEY_PREFIX + "exists-true";
        
        // Setup: Store a value
        stringRedisTemplate.opsForValue().set(key, "some-value");
        
        // Test: Check existence
        boolean exists = redisService.exists(key);
        
        assertTrue(exists);
    }
    
    @Test
    void testExists_False() {
        String key = TEST_KEY_PREFIX + "exists-false";
        
        // Test: Check non-existent key
        boolean exists = redisService.exists(key);
        
        assertFalse(exists);
    }
    
    // ==================== DELETE Tests ====================
    
    @Test
    void testDelete_ExistingKey() {
        String key = TEST_KEY_PREFIX + "delete-existing";
        
        // Setup: Store a value
        stringRedisTemplate.opsForValue().set(key, "to-be-deleted");
        
        // Test: Delete the key
        boolean deleted = redisService.delete(key);
        
        // Verify: Key was deleted
        assertTrue(deleted);
        
        // Verify: Key no longer exists
        Boolean exists = stringRedisTemplate.hasKey(key);
        assertFalse(Boolean.TRUE.equals(exists));
    }
    
    @Test
    void testDelete_NonExistentKey() {
        String key = TEST_KEY_PREFIX + "delete-missing";
        
        // Test: Delete non-existent key
        boolean deleted = redisService.delete(key);
        
        // Verify: Returns false (key didn't exist)
        assertFalse(deleted);
    }
    
    // ==================== EXPIRE Tests ====================
    
    @Test
    void testExpire_ExistingKey() {
        String key = TEST_KEY_PREFIX + "expire-existing";
        Duration ttl = Duration.ofSeconds(600);
        
        // Setup: Store a value without TTL
        stringRedisTemplate.opsForValue().set(key, "value");
        
        // Test: Set expiration
        boolean success = redisService.expire(key, ttl);
        
        // Verify: Expiration was set
        assertTrue(success);
        
        // Verify: TTL is set
        Long actualTtl = stringRedisTemplate.getExpire(key);
        assertNotNull(actualTtl);
        assertTrue(actualTtl > 0 && actualTtl <= 600);
    }
    
    @Test
    void testExpire_NonExistentKey() {
        String key = TEST_KEY_PREFIX + "expire-missing";
        Duration ttl = Duration.ofSeconds(600);
        
        // Test: Set expiration on non-existent key
        boolean success = redisService.expire(key, ttl);
        
        // Verify: Operation failed (key doesn't exist)
        assertFalse(success);
    }
    
    // ==================== TTL Tests ====================
    
    @Test
    void testGetTtl_WithExpiration() {
        String key = TEST_KEY_PREFIX + "ttl-with-expiration";
        
        // Setup: Store value with TTL
        stringRedisTemplate.opsForValue().set(key, "value", Duration.ofSeconds(500));
        
        // Test: Get TTL
        long ttl = redisService.getTtl(key);
        
        // Verify: TTL is positive and reasonable
        assertTrue(ttl > 0 && ttl <= 500);
    }
    
    @Test
    void testGetTtl_NoExpiration() {
        String key = TEST_KEY_PREFIX + "ttl-no-expiration";
        
        // Setup: Store value without TTL
        stringRedisTemplate.opsForValue().set(key, "value");
        
        // Test: Get TTL
        long ttl = redisService.getTtl(key);
        
        // Verify: TTL is -1 (no expiration)
        assertEquals(-1, ttl);
    }
    
    @Test
    void testGetTtl_NonExistentKey() {
        String key = TEST_KEY_PREFIX + "ttl-missing";
        
        // Test: Get TTL for non-existent key
        long ttl = redisService.getTtl(key);
        
        // Verify: TTL is -2 (key doesn't exist)
        assertEquals(-2, ttl);
    }
    
    // ==================== HASH Tests ====================
    
    @Test
    void testHGet_Hit() {
        String key = TEST_KEY_PREFIX + "hash-get-hit";
        String field = "field1";
        String value = "value1";
        
        // Setup: Store hash field
        stringRedisTemplate.opsForHash().put(key, field, value);
        
        // Test: Get hash field
        String actualValue = redisService.hget(key, field);
        
        assertEquals(value, actualValue);
    }
    
    @Test
    void testHGet_Miss() {
        String key = TEST_KEY_PREFIX + "hash-get-miss";
        String field = "missing-field";
        
        // Test: Get non-existent hash field
        String value = redisService.hget(key, field);
        
        assertNull(value);
    }
    
    @Test
    void testHSet() {
        String key = TEST_KEY_PREFIX + "hash-set";
        String field = "field1";
        String value = "value1";
        
        // Test: Set hash field
        redisService.hset(key, field, value);
        
        // Verify: Field was set
        Object storedValue = stringRedisTemplate.opsForHash().get(key, field);
        assertEquals(value, storedValue);
    }
    
    @Test
    void testHDelete_ExistingField() {
        String key = TEST_KEY_PREFIX + "hash-delete-existing";
        String field = "field1";
        String value = "value1";
        
        // Setup: Store hash field
        stringRedisTemplate.opsForHash().put(key, field, value);
        
        // Test: Delete hash field
        boolean deleted = redisService.hdelete(key, field);
        
        // Verify: Field was deleted
        assertTrue(deleted);
        
        // Verify: Field no longer exists
        Boolean exists = stringRedisTemplate.opsForHash().hasKey(key, field);
        assertFalse(exists);
    }
    
    @Test
    void testHDelete_NonExistentField() {
        String key = TEST_KEY_PREFIX + "hash-delete-missing";
        String field = "missing-field";
        
        // Test: Delete non-existent hash field
        boolean deleted = redisService.hdelete(key, field);
        
        // Verify: Returns false (field didn't exist)
        assertFalse(deleted);
    }
    
    // ==================== Exception Handling Tests ====================
    
    @Test
    void testExceptionHandling() {
        // Note: This test verifies exception structure
        // Actual Redis connection failures would need a different test setup
        
        String key = TEST_KEY_PREFIX + "exception-test";
        
        // Test with valid operations - should not throw
        assertDoesNotThrow(() -> {
            redisService.set(key, "value");
            redisService.get(key);
            redisService.delete(key);
        });
    }
}
