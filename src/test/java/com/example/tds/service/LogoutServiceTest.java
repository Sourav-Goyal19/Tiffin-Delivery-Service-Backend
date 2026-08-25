package com.example.tds.service;

import com.example.tds.utilities.JwtUtility;
import com.example.tds.utilities.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtUtility jwtUtility;

    @InjectMocks
    private LogoutService logoutService;

    private static final String ACCESS_PREFIX = "auth:denylist:access:";
    private static final String REFRESH_PREFIX = "auth:denylist:refresh:";
    private static final String ROLE_USER = "user";
    private static final String ROLE_CHEF = "chef";
    private static final String ROLE_AGENT = "deliveryAgent";
    private static final UUID ACTOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUpValueOps() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ---------- revokeAccessToken ----------

    @Test
    void revokeAccessToken_writesKeyWithCorrectPrefixAndHashAndTtl() {
        String rawToken = "access-token-abc";
        long ttlSeconds = 1800L;
        when(jwtUtility.getRemainingSeconds(rawToken)).thenReturn(ttlSeconds);

        logoutService.revokeAccessToken(rawToken, ROLE_USER, ACTOR_ID);

        String expectedKey = ACCESS_PREFIX + TokenHasher.sha256(rawToken);
        verify(valueOperations).set(eq(expectedKey), eq("1"), eq(Duration.ofSeconds(ttlSeconds)));
    }

    @Test
    void revokeAccessToken_doesNotWriteWhenRemainingSecondsIsZero() {
        String rawToken = "expired-token";
        when(jwtUtility.getRemainingSeconds(rawToken)).thenReturn(0L);

        logoutService.revokeAccessToken(rawToken, ROLE_USER, ACTOR_ID);

        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
        // StringRedisTemplate.hasKey should never be touched on the write-skip path
        verify(stringRedisTemplate, never()).hasKey(anyString());
    }

    @Test
    void revokeAccessToken_doesNotWriteWhenRemainingSecondsIsNegative() {
        String rawToken = "way-expired-token";
        when(jwtUtility.getRemainingSeconds(rawToken)).thenReturn(-42L);

        logoutService.revokeAccessToken(rawToken, ROLE_USER, ACTOR_ID);

        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void revokeAccessToken_propagatesDataAccessException() {
        String rawToken = "access-token-fail";
        when(jwtUtility.getRemainingSeconds(rawToken)).thenReturn(60L);
        DataAccessException boom = new QueryTimeoutException("redis down");
        org.mockito.Mockito.doThrow(boom)
                .when(valueOperations)
                .set(anyString(), anyString(), any(Duration.class));

        DataAccessException thrown = assertThrows(DataAccessException.class,
                () -> logoutService.revokeAccessToken(rawToken, ROLE_USER, ACTOR_ID));
        assertEquals(boom, thrown);
    }

    // ---------- revokeRefreshToken ----------

    @Test
    void revokeRefreshToken_writesKeyWithRefreshPrefixAndHashAndSevenDayTtl() {
        String rawRefreshToken = "refresh-token-xyz";

        logoutService.revokeRefreshToken(rawRefreshToken, ROLE_CHEF, ACTOR_ID);

        String expectedKey = REFRESH_PREFIX + TokenHasher.sha256(rawRefreshToken);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(eq(expectedKey), eq("1"), ttlCaptor.capture());
        assertEquals(Duration.ofDays(7), ttlCaptor.getValue());
    }

    @Test
    void revokeRefreshToken_isNoOpWhenTokenIsNull() {
        logoutService.revokeRefreshToken(null, ROLE_USER, ACTOR_ID);

        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void revokeRefreshToken_isNoOpWhenTokenIsBlank() {
        logoutService.revokeRefreshToken("   ", ROLE_USER, ACTOR_ID);

        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void revokeRefreshToken_isNoOpWhenTokenIsEmpty() {
        logoutService.revokeRefreshToken("", ROLE_USER, ACTOR_ID);

        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void revokeRefreshToken_propagatesDataAccessException() {
        String rawRefreshToken = "refresh-token-fail";
        DataAccessException boom = new QueryTimeoutException("redis down");
        org.mockito.Mockito.doThrow(boom)
                .when(valueOperations)
                .set(anyString(), anyString(), any(Duration.class));

        assertThrows(DataAccessException.class,
                () -> logoutService.revokeRefreshToken(rawRefreshToken, ROLE_USER, ACTOR_ID));
    }

    // ---------- isAccessTokenRevoked ----------

    @Test
    void isAccessTokenRevoked_returnsTrueWhenKeyExists() {
        String rawToken = "revoked-access";
        when(stringRedisTemplate.hasKey(ACCESS_PREFIX + TokenHasher.sha256(rawToken)))
                .thenReturn(true);

        assertTrue(logoutService.isAccessTokenRevoked(rawToken));
    }

    @Test
    void isAccessTokenRevoked_returnsFalseWhenKeyDoesNotExist() {
        String rawToken = "live-access";
        when(stringRedisTemplate.hasKey(ACCESS_PREFIX + TokenHasher.sha256(rawToken)))
                .thenReturn(false);

        assertFalse(logoutService.isAccessTokenRevoked(rawToken));
    }

    @Test
    void isAccessTokenRevoked_returnsFalseWhenKeyReturnsNull() {
        String rawToken = "weird-access";
        when(stringRedisTemplate.hasKey(ACCESS_PREFIX + TokenHasher.sha256(rawToken)))
                .thenReturn(null);

        assertFalse(logoutService.isAccessTokenRevoked(rawToken));
    }

    @Test
    void isAccessTokenRevoked_failsOpenWhenRedisReadThrows() {
        String rawToken = "redis-down-access";
        when(stringRedisTemplate.hasKey(anyString()))
                .thenThrow(new QueryTimeoutException("redis down"));

        // Fail-open: do NOT propagate, return false so request is allowed through
        assertFalse(logoutService.isAccessTokenRevoked(rawToken));
    }

    // ---------- isRefreshTokenRevoked ----------

    @Test
    void isRefreshTokenRevoked_returnsTrueWhenKeyExists() {
        String rawToken = "revoked-refresh";
        when(stringRedisTemplate.hasKey(REFRESH_PREFIX + TokenHasher.sha256(rawToken)))
                .thenReturn(true);

        assertTrue(logoutService.isRefreshTokenRevoked(rawToken));
    }

    @Test
    void isRefreshTokenRevoked_returnsFalseWhenKeyDoesNotExist() {
        String rawToken = "live-refresh";
        when(stringRedisTemplate.hasKey(REFRESH_PREFIX + TokenHasher.sha256(rawToken)))
                .thenReturn(false);

        assertFalse(logoutService.isRefreshTokenRevoked(rawToken));
    }

    @Test
    void isRefreshTokenRevoked_returnsFalseWhenTokenIsNull() {
        assertFalse(logoutService.isRefreshTokenRevoked(null));
        verify(stringRedisTemplate, never()).hasKey(anyString());
    }

    @Test
    void isRefreshTokenRevoked_returnsFalseWhenTokenIsBlank() {
        assertFalse(logoutService.isRefreshTokenRevoked(""));
        assertFalse(logoutService.isRefreshTokenRevoked("   "));
        verify(stringRedisTemplate, never()).hasKey(anyString());
    }

    @Test
    void isRefreshTokenRevoked_failsOpenWhenRedisReadThrows() {
        String rawToken = "redis-down-refresh";
        when(stringRedisTemplate.hasKey(anyString()))
                .thenThrow(new QueryTimeoutException("redis down"));

        assertFalse(logoutService.isRefreshTokenRevoked(rawToken));
    }

    // ---------- Cross-role independence ----------

    @Test
    void differentTokensProduceIndependentDenyListEntries() {
        String userToken = "user-access-token";
        String chefToken = "chef-access-token";
        long ttl = 60L;
        when(jwtUtility.getRemainingSeconds(userToken)).thenReturn(ttl);
        when(jwtUtility.getRemainingSeconds(chefToken)).thenReturn(ttl);

        logoutService.revokeAccessToken(userToken, ROLE_USER, ACTOR_ID);
        logoutService.revokeAccessToken(chefToken, ROLE_CHEF, ACTOR_ID);

        // Both keys were written and they are different
        verify(valueOperations, times(2)).set(anyString(), eq("1"), any(Duration.class));
        verify(valueOperations).set(eq(ACCESS_PREFIX + TokenHasher.sha256(userToken)),
                eq("1"), any(Duration.class));
        verify(valueOperations).set(eq(ACCESS_PREFIX + TokenHasher.sha256(chefToken)),
                eq("1"), any(Duration.class));
        // Hash sanity: keys must differ
        assertEquals(64, TokenHasher.sha256(userToken).length());
        assertEquals(64, TokenHasher.sha256(chefToken).length());
    }

    @Test
    void revokeAccessToken_ttlMirrorsJwtRemainingSeconds() {
        // The TTL set in Redis must be exactly the value reported by JwtUtility
        String rawToken = "ttl-mirror-token";
        when(jwtUtility.getRemainingSeconds(rawToken)).thenReturn(7777L);

        logoutService.revokeAccessToken(rawToken, ROLE_AGENT, ACTOR_ID);

        verify(valueOperations).set(anyString(), eq("1"), eq(Duration.ofSeconds(7777L)));
    }
}