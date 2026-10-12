
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
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource)
                )
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint)
                )
                .authorizeHttpRequests(auth -> auth

                        // This endpoint requires a valid authentication.
                        .requestMatchers(
                                "/api/auth/unishare/me"
                        ).authenticated()

                        // Other authentication endpoints remain public.
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        // Health endpoints.
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/health",
                                "/error"
                        ).permitAll()

                        // Password reset endpoints.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/user-management/password-update",
                                "/api/user-management/password-update/confirm"
                        ).permitAll()

                        // User-scoped ride endpoints.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/rides/my-rides",
                                "/api/rides/my-requests",
                                "/api/rides/*/requests",
                                "/api/rides/*/passengers"
                        ).authenticated()

                        // Public ride reads.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/rides/**"
                        ).permitAll()

                        // Ride modifications require authentication.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/rides/**"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/rides/**"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/rides/**"
                        ).authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
