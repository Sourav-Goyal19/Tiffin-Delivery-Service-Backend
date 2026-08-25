package com.example.tds.interceptors;

import com.example.tds.entity.UserEntity;
import com.example.tds.repository.UserRepository;
import com.example.tds.service.LogoutService;
import com.example.tds.utilities.JwtUtility;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the denylist-check branch in {@link UserAuthInterceptor}.
 *
 * We exercise only the denylist gate (lines that involve {@link LogoutService}).
 * Other interceptor branches (signature validation, missing header, user lookup)
 * are exercised in the existing pre-logout code paths and are not the focus here.
 */
@ExtendWith(MockitoExtension.class)
class UserAuthInterceptorLogoutTest {

    @Mock
    private JwtUtility jwtUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LogoutService logoutService;

    private UserAuthInterceptor interceptor;

    private static final String RAW_ACCESS_TOKEN = "test-access-token";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        interceptor = new UserAuthInterceptor();
        org.springframework.test.util.ReflectionTestUtils.setField(interceptor, "jwtUtil", jwtUtil);
        org.springframework.test.util.ReflectionTestUtils.setField(interceptor, "userRepository", userRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(interceptor, "logoutService", logoutService);
    }

    private MockHttpServletRequest requestWithBearer() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + RAW_ACCESS_TOKEN);
        return req;
    }

    /**
     * Past validateToken, before user lookup — interceptor has decided the token is
     * signature-valid and is now about to consult the denylist.
     */
    private void primeInterceptorToDenylistGate() {
        when(jwtUtil.validateToken(RAW_ACCESS_TOKEN)).thenReturn(true);
    }

    @Test
    void denylistHit_returns401_andDoesNotLookUpUser() throws Exception {
        primeInterceptorToDenylistGate();
        when(logoutService.isAccessTokenRevoked(RAW_ACCESS_TOKEN)).thenReturn(true);

        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(requestWithBearer(), res, new Object());

        assertFalse(proceed, "Interceptor must reject revoked tokens with 401");
        assertEquals(401, res.getStatus());
        verify(userRepository, never()).findByMobileNo(anyString());
    }

    @Test
    void denylistHit_writesInvalidOrExpiredTokenJson() throws Exception {
        primeInterceptorToDenylistGate();
        when(logoutService.isAccessTokenRevoked(RAW_ACCESS_TOKEN)).thenReturn(true);

        MockHttpServletResponse res = new MockHttpServletResponse();

        interceptor.preHandle(requestWithBearer(), res, new Object());

        String body = res.getContentAsString();
        assertTrue(body.contains("Invalid or expired token"),
                "Body should contain 'Invalid or expired token'. Got: " + body);
    }

    @Test
    void tokenNotOnDenylist_proceedsToUserLookup_andAttachesUserAttribute() throws Exception {
        primeInterceptorToDenylistGate();
        when(logoutService.isAccessTokenRevoked(RAW_ACCESS_TOKEN)).thenReturn(false);

        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(claims.get("mobileNo", String.class)).thenReturn("9999999999");
        when(jwtUtil.extractAllClaims(RAW_ACCESS_TOKEN)).thenReturn(claims);

        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        user.setMobileNo("9999999999");
        when(userRepository.findByMobileNo("9999999999")).thenReturn(Optional.of(user));

        MockHttpServletRequest req = requestWithBearer();
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(req, res, new Object());

        assertTrue(proceed, "Interceptor must let a non-revoked, valid token through");
        assertEquals(200, res.getStatus());
        assertEquals(user, req.getAttribute("user"));
        verify(logoutService, times(1)).isAccessTokenRevoked(RAW_ACCESS_TOKEN);
    }

    @Test
    void missingAuthorizationHeader_denylistNeverConsulted() throws Exception {
        // The interceptor short-circuits on missing header BEFORE the denylist check.
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(new MockHttpServletRequest(), res, new Object());

        assertFalse(proceed);
        assertEquals(401, res.getStatus());
        verify(logoutService, never()).isAccessTokenRevoked(anyString());
    }

    @Test
    void invalidSignatureToken_denylistNeverConsulted() throws Exception {
        when(jwtUtil.validateToken(RAW_ACCESS_TOKEN)).thenReturn(false);

        MockHttpServletResponse res = new MockHttpServletResponse();
        boolean proceed = interceptor.preHandle(requestWithBearer(), res, new Object());

        assertFalse(proceed);
        assertEquals(401, res.getStatus());
        verify(logoutService, never()).isAccessTokenRevoked(anyString());
    }
}