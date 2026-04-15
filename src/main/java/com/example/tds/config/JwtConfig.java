package com.example.tds.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtConfig {
    @Getter
    @Value("${security.jwt.secret-key}")
    private String secret;

    @Getter
    @Value("${security.jwt.expiration-time}")
    private long expiration;
}
