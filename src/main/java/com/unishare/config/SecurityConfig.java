package com.unishare.config;

import com.unishare.security.CustomAccessDeniedHandler;
import com.unishare.security.CustomAuthenticationEntryPoint;
import com.unishare.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll() // Allow frontend API calls
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll() // Health checks
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/user-management/password-update",
                                "/api/user-management/password-update/confirm"
                        ).permitAll() // Password reset endpoints
                        // User-scoped ride reads: must precede the public GET rule (first match wins)
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/rides/my-rides",
                                "/api/rides/my-requests",
                                "/api/rides/*/requests",
                                "/api/rides/*/passengers"
                        ).authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/rides/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/rides/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/rides/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/rides/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}