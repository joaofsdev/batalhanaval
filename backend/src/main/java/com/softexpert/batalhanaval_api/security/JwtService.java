package com.softexpert.batalhanaval_api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final String ISSUER = "batalhanaval-api";

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.expiration}") long expirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UUID userId, String username) {
        Date now = new Date();
        return Jwts.builder()
            .subject(userId.toString())
            .claim("username", username)
            .issuer(ISSUER)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expirationMs))
            .signWith(secretKey)
            .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractClaims(token).getSubject());
    }

    public String extractUsername(String token) {
        return extractClaims(token).get("username", String.class);
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .requireIssuer(ISSUER)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
