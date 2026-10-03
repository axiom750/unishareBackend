package com.unishare.service.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiry}")
    private long jwtExpiry;

    private Key getSigningKey() {

        return Keys.hmacShaKeyFor(
                jwtSecret.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }

    public String generateToken(
            Long userId,
            String email,
            String role
    ) {

        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(
                                System.currentTimeMillis()
                                        + jwtExpiry
                        )
                )
                .signWith(
                        getSigningKey(),
                        SignatureAlgorithm.HS256
                )
                .compact();
    }

    public Claims extractAllClaims(
            String token
    ) {

        return Jwts.parserBuilder()
                .setSigningKey(
                        getSigningKey()
                )
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(
            String token
    ) {

        try {

            extractAllClaims(token);

            return true;

        } catch (
                JwtException |
                IllegalArgumentException exception
        ) {

            return false;
        }
    }
}