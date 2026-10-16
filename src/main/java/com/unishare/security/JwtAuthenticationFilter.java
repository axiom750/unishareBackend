
package com.unishare.security;

import com.unishare.service.auth.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String TOKEN_COOKIE = "token";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            if (SecurityContextHolder.getContext()
                    .getAuthentication() == null) {

                String token = extractToken(request);

                if (token != null && jwtService.isTokenValid(token)) {
                    authenticateRequest(token, request);
                }
            }
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();

            log.debug(
                    "[SECURITY] Invalid JWT for request {}",
                    request.getRequestURI()
            );
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateRequest(
            String token,
            HttpServletRequest request
    ) {
        Long userId = jwtService.extractUserId(token);
        String email = jwtService.extractEmail(token);

        // Require the role claim for compatibility with issued tokens.
        // Actual authorities are loaded from the database below.
        jwtService.extractRole(token);

        UserDetails userDetails;

        try {
            userDetails = userDetailsService
                    .loadUserByUsername(email);
        } catch (UsernameNotFoundException exception) {
            log.debug(
                    "[SECURITY] JWT user no longer exists"
            );
            return;
        }

        if (userDetails == null) {
            return;
        }

        if (!userDetails.isEnabled()
                || !userDetails.isAccountNonExpired()
                || !userDetails.isAccountNonLocked()) {

            log.debug(
                    "[SECURITY] User account is not eligible for authentication"
            );
            return;
        }

        // CustomUserDetailsService returns the numeric user ID
        // as the UserDetails username.
        if (!String.valueOf(userId)
                .equals(userDetails.getUsername())) {

            log.warn(
                    "[SECURITY] JWT identity mismatch; authentication rejected"
            );
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails.getUsername(),
                        null,
                        userDetails.getAuthorities()
                );

        authentication.setDetails(
                new WebAuthenticationDetailsSource()
                        .buildDetails(request)
        );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        log.debug(
                "[SECURITY] JWT authentication established for user ID {}",
                userId
        );
    }

    private String extractToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(
                HttpHeaders.AUTHORIZATION
        );

        if (authorizationHeader != null
                && authorizationHeader.startsWith(BEARER_PREFIX)) {

            String token = authorizationHeader
                    .substring(BEARER_PREFIX.length())
                    .trim();

            return token.isEmpty() ? null : token;
        }

        return extractTokenFromCookie(request);
    }

    private String extractTokenFromCookie(
            HttpServletRequest request
    ) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (TOKEN_COOKIE.equals(cookie.getName())
                    && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {

                return cookie.getValue();
            }
        }

        return null;
    }
}
