package com.medsyncpro.service;

import com.medsyncpro.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.access-expiration:900000}")
    private long accessExpiration;
    
    /**
     * Generate access token with JTI, tokenVersion, and short expiry.
     */
    public String generateAccessToken(User user, String deviceInfo) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("userId", user.getId())
                .claim("tokenVersion", user.getTokenVersion())
                .claim("deviceInfo", deviceInfo)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * Parse and validate token. Throws on invalid/expired/tampered tokens.
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * Extract claims even from expired tokens (for blacklisting during logout).
     */
    public Claims extractClaimsAllowExpired(String token) {
        try {
            return extractClaims(token);
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
    
    public String extractJti(Claims claims) {
        return claims.getId();
    }
    
    public String extractEmail(Claims claims) {
        return claims.getSubject();
    }
    
    public Integer extractTokenVersion(Claims claims) {
        return claims.get("tokenVersion", Integer.class);
    }
    
    public String extractDeviceInfo(Claims claims) {
        return claims.get("deviceInfo", String.class);
    }
    
    public Date extractExpiration(Claims claims) {
        return claims.getExpiration();
    }
    
    public long getAccessExpiration() {
        return accessExpiration;
    }
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}
