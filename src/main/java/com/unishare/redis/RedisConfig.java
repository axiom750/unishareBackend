package com.unishare.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Configuration for UniShare Backend
 * 
 * Configures RedisTemplate and StringRedisTemplate beans with appropriate serializers.
 * These templates are used by RedisService for all Redis operations.
 * 
 * Serialization Strategy:
 * - Keys: String serialization (UTF-8)
 * - Values: JSON serialization (for flexibility with complex objects)
 * - Hash Keys: String serialization
 * - Hash Values: JSON serialization
 * 
 * @see RedisService
 */
@Configuration
public class RedisConfig {

    /**
     * Configure RedisTemplate with JSON serialization for values.
     * Used for storing complex objects.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serialization for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // Use JSON serialization for values
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Configure StringRedisTemplate for string-only operations.
     * This is the primary template used by RedisService for most operations.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
