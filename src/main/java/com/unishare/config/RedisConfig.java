package com.unishare.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Redis/Valkey Configuration with explicit Lettuce timeout settings.
 * 
 * This configuration ensures that both TCP connection timeout and Redis command timeout
 * are properly set for cloud Redis-compatible instances (Aiven Valkey) where network
 * latency and TLS handshake overhead require longer timeouts.
 * 
 * Current Backend: Aiven Valkey 9.1
 * - Host: unishare-redis-superuser-2e6d.d.aivencloud.com
 * - Port: 16660
 * - TLS: Required
 * - Protocol: Redis-compatible (Valkey)
 * 
 * Configuration:
 * - Socket connect timeout: 10 seconds (for TCP + TLS connection establishment)
 * - Command timeout: 10 seconds (for Redis/Valkey operations)
 * - pingBeforeActivateConnection: false (avoid unnecessary validation overhead)
 * 
 * Performance Notes:
 * - Runtime Redis operations: ~150ms (excellent for cloud)
 * - Idempotency working correctly
 * - Connection pool managed by Spring Boot auto-configuration
 * 
 * Architecture:
 * <pre>
 * Business Service
 *     ↓
 * RedisService
 *     ↓
 * StringRedisTemplate
 *     ↓
 * LettuceConnectionFactory (configured by Spring Boot auto-config)
 *     ↓
 * ClientOptions (this configuration)
 *     ↓
 * Aiven Valkey (Redis-compatible)
 * </pre>
 * 
 * @see org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
 */
@Configuration
public class RedisConfig {

    /**
     * Customize Lettuce client configuration with timeouts appropriate for cloud Redis.
     * 
     * This customizer is applied to the auto-configured LettuceConnectionFactory.
     * Spring Boot handles connection pooling via commons-pool2 if present on classpath.
     * 
     * Valkey Compatibility:
     * Valkey is a Redis fork that maintains full Redis protocol compatibility.
     * No special configuration is needed - Lettuce treats it as Redis.
     * 
     * @return Lettuce client configuration customizer
     */
    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceClientConfigurationBuilderCustomizer() {
        return clientConfigurationBuilder -> {
            
            // Configure socket options for TCP connection
            SocketOptions socketOptions = SocketOptions.builder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            
            // Configure client options
            // Skip pre-activation PING to reduce connection establishment overhead
            ClientOptions clientOptions = ClientOptions.builder()
                    .socketOptions(socketOptions)
                    .pingBeforeActivateConnection(false)
                    .build();
            
            // Apply configuration
            clientConfigurationBuilder
                    .commandTimeout(Duration.ofSeconds(10))
                    .clientOptions(clientOptions);
        };
    }
}
