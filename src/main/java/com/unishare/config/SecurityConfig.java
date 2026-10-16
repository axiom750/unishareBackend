
package com.unishare.config;

import com.unishare.security.ControlPlaneJwtAuthenticationFilter;
import com.unishare.security.CustomAccessDeniedHandler;
import com.unishare.security.CustomAuthenticationEntryPoint;
import com.unishare.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final ControlPlaneJwtAuthenticationFilter controlPlaneFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    /**
     * Dedicated security chain for GodAccount control-plane endpoints.
     * It must be evaluated before the normal application chain.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain controlPlaneSecurityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .securityMatcher("/api/controlpanel/**")
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource)
                )
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/controlpanel/login"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        controlPlaneFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * Normal UniShare application security chain.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource)
                )
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint)
                )
                .authorizeHttpRequests(auth -> auth

                        // Public native authentication endpoints.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/unishare/register",
                                "/api/auth/unishare/login"
                        ).permitAll()

                        // OAuth initiation and callback endpoints.
                        .requestMatchers("/api/auth/**").permitAll()

                        // Public health and error endpoints.
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/health",
                                "/error"
                        ).permitAll()

                        // Public password-reset endpoints.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/user-management/password-update",
                                "/api/user-management/password-update/confirm"
                        ).permitAll()

                        // Personal ride endpoints require authentication.
                        // Keep these before the public ride-read matcher.
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

                        // All other endpoints require authentication.
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * Prevent Spring Boot from registering the security filters
     * independently as servlet filters. Spring Security owns them.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter>
    disableNormalJwtServletRegistration() {

        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtFilter);

        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<ControlPlaneJwtAuthenticationFilter>
    disableControlPlaneJwtServletRegistration() {

        FilterRegistrationBean<ControlPlaneJwtAuthenticationFilter>
                registration =
                new FilterRegistrationBean<>(controlPlaneFilter);

        registration.setEnabled(false);
        return registration;
    }
}
