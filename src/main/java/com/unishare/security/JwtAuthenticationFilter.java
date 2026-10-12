
package com.unishare.security;

import com.unishare.service.auth.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

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
                    Claims claims = jwtService.extractAllClaims(token);

                    Number userIdClaim =
                            claims.get("userId", Number.class);
                    String role = claims.get("role", String.class);

                    if (userIdClaim != null
                            && role != null
                            && !role.isBlank()) {

                        Long userId = userIdClaim.longValue();

                        String authority = role.startsWith("ROLE_")
                                ? role
                                : "ROLE_" + role;

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userId.toString(),
                                        null,
                                        List.of(
                                                new SimpleGrantedAuthority(
                                                        authority
                                                )
                                        )
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource()
                                        .buildDetails(request)
                        );

                        SecurityContextHolder.getContext()
                                .setAuthentication(authentication);

                        log.debug(
                                "[AUTH] JWT authenticated user ID: {}",
                                userId
                        );
                    } else {
                        log.debug("[AUTH] JWT missing userId or role claim");
                    }
                } else if (token != null) {
                    log.debug("[AUTH] JWT is invalid or expired");
                }
            }
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            log.debug("[AUTH] JWT validation failed");
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null
                && authorizationHeader.startsWith("Bearer ")) {

            String bearerToken =
                    authorizationHeader.substring(7).trim();

            if (!bearerToken.isEmpty()) {
                return bearerToken;
            }
        }

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())
                        && cookie.getValue() != null
                        && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
