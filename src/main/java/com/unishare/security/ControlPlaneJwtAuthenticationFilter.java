
package com.unishare.security;

import com.unishare.service.auth.ControlPlaneIdentityService;
import com.unishare.service.auth.ControlPlaneJwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ControlPlaneJwtAuthenticationFilter
        extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ControlPlaneJwtService jwtService;
    private final ControlPlaneIdentityService identityService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = extractBearerToken(request);

            if (token != null) {
                try {
                    UUID accountId = jwtService.extractAccountId(token);

                    var authorities =
                            identityService.loadAuthorities(accountId);

                    var authentication =
                            new UsernamePasswordAuthenticationToken(
                                    accountId.toString(),
                                    null,
                                    authorities
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);

                } catch (JwtException | IllegalArgumentException
                         | UsernameNotFoundException exception) {

                    SecurityContextHolder.clearContext();

                    log.debug(
                            "[CONTROL-PLANE] Authentication rejected for {}",
                            request.getRequestURI()
                    );
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();

        return token.isEmpty() ? null : token;
    }
}
