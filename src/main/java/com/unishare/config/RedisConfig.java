package com.unishare.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Redis Configuration with explicit Lettuce timeout settings.
 * 
 * This configuration ensures that both TCP connection timeout and Redis command timeout
 * are properly set for cloud Redis instances (Upstash) where network latency is higher.
 * 
 * Configuration:
 * - Socket connect timeout: 10 seconds (for TCP connection establishment)
 * - Command timeout: 10 seconds (for Redis operations)
 * - Auto-reconnect: enabled by default
 * 
 * Why explicit configuration is needed:
 * - Cloud Redis (Upstash) has higher latency than localhost (~200-500ms baseline)
 * - TLS handshake adds additional latency
 * - Default 2-second timeout is insufficient for remote connections
 * - Lettuce may perform internal retries, multiplying the total time
 * 
 * Architecture:
 * <pre>
 * StringRedisTemplate
 *     ↓
 * LettuceConnectionFactory (configured by Spring Boot auto-config)
 *     ↓
 * ClientOptions (this configuration)
 *     ↓
 * Upstash Redis
 * </pre>
 * 
 * @see org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
 */
@Configuration
public class RedisConfig {

    /**
     * Customize Lettuce client configuration with explicit timeouts.
     * 
     * This customizer is applied to the auto-configured LettuceConnectionFactory,
     * so we don't need to create a custom connection factory bean.
     * 
     * @return Lettuce client configuration customizer
     */
    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceClientConfigurationBuilderCustomizer() {
        return clientConfigurationBuilder -> {
            
            // Configure socket options for TCP connection
            SocketOptions socketOptions = SocketOptions.builder()
                    .connectTimeout(Duration.ofSeconds(10))  // TCP connection timeout
                    .build();
            
            // Configure client options
            ClientOptions clientOptions = ClientOptions.builder()
                    .socketOptions(socketOptions)
                    .build();
            
            // Apply configuration
            clientConfigurationBuilder
                    .commandTimeout(Duration.ofSeconds(10))  // Redis command timeout
                    .clientOptions(clientOptions);
        };
    }
}
