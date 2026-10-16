
package com.unishare.service.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Token lifetime in milliseconds.
     */
    @Value("${jwt.expiry}")
    private long jwtExpiry;

    private SecretKey signingKey;

    @PostConstruct
    public void initialize() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret must not be empty"
            );
        }

        if (jwtExpiry <= 0) {
            throw new IllegalStateException(
                    "JWT expiry must be greater than zero"
            );
        }

        try {
            this.signingKey = Keys.hmacShaKeyFor(
                    jwtSecret.getBytes(StandardCharsets.UTF_8)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "JWT secret is too weak. HS256 requires at least "
                            + "32 bytes of key material.",
                    exception
            );
        }
    }

    public String generateToken(
            Long userId,
            String email,
            String role
    ) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                    "User ID must be a positive number"
            );
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email must not be empty"
            );
        }

        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException(
                    "Role must not be empty"
            );
        }

        long now = System.currentTimeMillis();
        long expiration;

        try {
            expiration = Math.addExact(now, jwtExpiry);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "JWT expiry value is too large",
                    exception
            );
        }

        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(expiration))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtException("JWT must not be empty");
        }

        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);

        Number userId = claims.get("userId", Number.class);

        if (userId == null || userId.longValue() <= 0) {
            throw new JwtException(
                    "JWT contains an invalid user ID"
            );
        }

        return userId.longValue();
    }

    public String extractEmail(String token) {
        String email = extractAllClaims(token).getSubject();

        if (email == null || email.isBlank()) {
            throw new JwtException(
                    "JWT subject is missing"
            );
        }

        return email;
    }

    public String extractRole(String token) {
        String role = extractAllClaims(token)
                .get("role", String.class);

        if (role == null || role.isBlank()) {
            throw new JwtException(
                    "JWT role claim is missing"
            );
        }

        return role;
    }

    public boolean isTokenValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            Claims claims = extractAllClaims(token);

            Number userId = claims.get("userId", Number.class);
            String role = claims.get("role", String.class);
            Date issuedAt = claims.getIssuedAt();
            Date expiration = claims.getExpiration();

            return claims.getSubject() != null
                    && !claims.getSubject().isBlank()
                    && userId != null
                    && userId.longValue() > 0
                    && role != null
                    && !role.isBlank()
                    && issuedAt != null
                    && expiration != null
                    && expiration.after(new Date())
                    && !issuedAt.after(new Date());

        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }
}
