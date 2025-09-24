package com.backend.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMillis;
    private final long refreshTokenExpirationMillis;

    public JwtUtil(@Value("${jwt.secret}") String secret, 
                   @Value("${jwt.access_token_expiration_minutes}") long accessTokenExpirationMinutes,
                   @Value("${jwt.refresh_token_expiration_days}") long refreshTokenExpirationDays) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpirationMillis = accessTokenExpirationMinutes * 60 * 1000;
        this.refreshTokenExpirationMillis = refreshTokenExpirationDays * 24 * 60 * 60 * 1000;
    }

    public String generateAccessToken(String email) {
        return generateToken(email, accessTokenExpirationMillis);
    }

    public String generateRefreshToken(String email) {
        return generateToken(email, refreshTokenExpirationMillis);
    }

    private String generateToken(String email, long expirationMillis) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // MalformedJwtException, ExpiredJwtException, etc.
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public Long getRemainingExpirationMillis(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        Date expiration = claims.getExpiration();
        long now = new Date().getTime();
        return expiration.getTime() - now;
    }
}
