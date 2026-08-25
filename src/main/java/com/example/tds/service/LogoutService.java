package com.example.tds.service;

import com.example.tds.utilities.JwtUtility;
import com.example.tds.utilities.TokenHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutService {

    static final String ACCESS_KEY_PREFIX = "auth:denylist:access:";
    static final String REFRESH_KEY_PREFIX = "auth:denylist:refresh:";
    static final Duration REFRESH_TTL = Duration.ofDays(7);

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtility jwtUtility;

    /**
     * Revoke an access token in the Redis denylist with TTL = remaining lifetime.
     */
    public void revokeAccessToken(String rawAccessToken, String role, UUID actorId) {
        long ttlSeconds = jwtUtility.getRemainingSeconds(rawAccessToken);
        if (ttlSeconds <= 0) {
            log.info("Logout skipped (already expired) role={} actorId={}", role, actorId);
            return;
        }
        String key = ACCESS_KEY_PREFIX + TokenHasher.sha256(rawAccessToken);
        try {
            stringRedisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
            log.info("Logout successful role={} actorId={}", role, actorId);
        } catch (DataAccessException e) {
            log.error("Logout denylist write failed role={} actorId={} reason={}",
                    role, actorId, e.getMessage());
            throw e;
        }
    }

    /**
     * Revoke a refresh token in the Redis denylist with a fixed 7-day TTL.
     * No-op when refreshToken is null/blank.
     */
    public void revokeRefreshToken(String rawRefreshToken, String role, UUID actorId) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String key = REFRESH_KEY_PREFIX + TokenHasher.sha256(rawRefreshToken);
        try {
            stringRedisTemplate.opsForValue().set(key, "1", REFRESH_TTL);
            log.info("Refresh token denylisted role={} actorId={}", role, actorId);
        } catch (DataAccessException e) {
            log.error("Refresh denylist write failed role={} actorId={} reason={}",
                    role, actorId, e.getMessage());
            throw e;
        }
    }

    /** Read-path check used by the interceptors. Fail-open on Redis outage. */
    public boolean isAccessTokenRevoked(String rawAccessToken) {
        String key = ACCESS_KEY_PREFIX + TokenHasher.sha256(rawAccessToken);
        try {
            Boolean present = stringRedisTemplate.hasKey(key);
            return Boolean.TRUE.equals(present);
        } catch (DataAccessException e) {
            log.warn("Denylist read failed; failing open reason={}", e.getMessage());
            return false;
        }
    }

    /** Read-path check used by /refresh. Fail-open on Redis outage. */
    public boolean isRefreshTokenRevoked(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return false;
        }
        String key = REFRESH_KEY_PREFIX + TokenHasher.sha256(rawRefreshToken);
        try {
            Boolean present = stringRedisTemplate.hasKey(key);
            return Boolean.TRUE.equals(present);
        } catch (DataAccessException e) {
            log.warn("Refresh denylist read failed; failing open reason={}", e.getMessage());
            return false;
        }
    }
}