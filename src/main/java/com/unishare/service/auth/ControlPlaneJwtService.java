
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
import java.util.UUID;

@Service
public class ControlPlaneJwtService {

    private static final String TOKEN_TYPE = "CONTROL_PLANE";

    @Value("${control-plane.jwt.secret}")
    private String jwtSecret;

    @Value("${control-plane.jwt.expiry:900000}")
    private long jwtExpiry;

    private SecretKey signingKey;

    @PostConstruct
    void initialize() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "Control-plane JWT secret is required"
            );
        }

        if (jwtExpiry <= 0) {
            throw new IllegalStateException(
                    "Control-plane JWT expiry must be positive"
            );
        }

        try {
            signingKey = Keys.hmacShaKeyFor(
                    jwtSecret.getBytes(StandardCharsets.UTF_8)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Control-plane JWT secret must contain "
                            + "at least 32 bytes for HS256",
                    exception
            );
        }
    }

    public String generateToken(UUID accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException(
                    "GodAccount ID is required"
            );
        }

        long now = System.currentTimeMillis();
        long expiresAt = Math.addExact(now, jwtExpiry);

        return Jwts.builder()
                .setSubject(accountId.toString())
                .claim("tokenType", TOKEN_TYPE)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(expiresAt))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public UUID extractAccountId(String token) {
        Claims claims = parseClaims(token);

        if (!TOKEN_TYPE.equals(claims.get(
                "tokenType", String.class))) {
            throw new JwtException(
                    "Invalid control-plane token type"
            );
        }

        try {
            return UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new JwtException(
                    "Invalid control-plane account ID",
                    exception
            );
        }
    }

    public Claims parseClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtException("Token is missing");
        }

        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public long getExpiryMillis() {
        return jwtExpiry;
    }
}
