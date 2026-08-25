package com.example.tds.utilities;

import com.example.tds.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtility {
    private static final Logger log = LoggerFactory.getLogger(JwtUtility.class);
    private final SecretKey secretKey;

    public JwtUtility(JwtConfig jwtConfig){
        this.secretKey = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());
    }

    public String generateToken(String subject, Map<String, Object> claims, long expiry){
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(secretKey)
                .compact();
    }

    public String generateToken(UUID id, Map<String, Object> claims, long expiry){
       return Jwts.builder()
               .claims(claims)
               .subject(id + "")
               .issuedAt(new Date())
               .expiration(new Date(System.currentTimeMillis() + expiry))
               .signWith(secretKey)
               .compact();
    }

    public Claims extractAllClaims(String token){
       return Jwts.parser()
               .verifyWith(secretKey)
               .build()
               .parseSignedClaims(token)
               .getPayload();
    }

    public boolean validateToken(String token){
        try{
            extractAllClaims(token);
            return true;
        }catch (Exception e){
            log.error(e.getMessage());
            return false;
        }
    }

    public UUID extractId(String token){
       return UUID.fromString(extractAllClaims(token).getSubject());
    }

    public long getRemainingSeconds(String token){
        Claims claims = extractAllClaims(token);
        long expMs = claims.getExpiration().getTime();
        long remainingMs = expMs - System.currentTimeMillis();
        return Math.max(0L, remainingMs / 1000L);
    }
}