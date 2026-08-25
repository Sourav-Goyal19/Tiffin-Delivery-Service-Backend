package com.example.tds.interceptors;

import com.example.tds.entity.ChefEntity;
import com.example.tds.exception.ResourceNotFoundException;
import com.example.tds.repository.ChefRepository;
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
 * Denylist-gate tests for {@link ChefAuthInterceptor}.
 */
@ExtendWith(MockitoExtension.class)
class ChefAuthInterceptorLogoutTest {

    @Mock
    private JwtUtility jwtUtil;

    @Mock
    private ChefRepository chefRepository;

    @Mock
    private LogoutService logoutService;

    private ChefAuthInterceptor interceptor;

    private static final String RAW_ACCESS_TOKEN = "chef-access-token";
    private static final UUID CHEF_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        interceptor = new ChefAuthInterceptor();
        org.springframework.test.util.ReflectionTestUtils.setField(interceptor, "jwtUtil", jwtUtil);
        org.springframework.test.util.ReflectionTestUtils.setField(interceptor, "chefRepository", chefRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(interceptor, "logoutService", logoutService);
    }

    private MockHttpServletRequest requestWithBearer() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + RAW_ACCESS_TOKEN);
        return req;
    }

    @Test
    void denylistHit_returns401_andDoesNotLookUpChef() throws Exception {
        when(jwtUtil.validateToken(RAW_ACCESS_TOKEN)).thenReturn(true);
        when(logoutService.isAccessTokenRevoked(RAW_ACCESS_TOKEN)).thenReturn(true);

        MockHttpServletRequest req = requestWithBearer();
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(req, res, new Object());

        assertFalse(proceed);
        assertEquals(401, res.getStatus());
        verify(chefRepository, never()).findByMobileNo(anyString());
    }

    @Test
    void tokenNotOnDenylist_proceedsToChefLookup_andAttachesChefAttribute() throws Exception {
        when(jwtUtil.validateToken(RAW_ACCESS_TOKEN)).thenReturn(true);
        when(logoutService.isAccessTokenRevoked(RAW_ACCESS_TOKEN)).thenReturn(false);

        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(claims.get("mobileNo", String.class)).thenReturn("8888888888");
        when(jwtUtil.extractAllClaims(RAW_ACCESS_TOKEN)).thenReturn(claims);

        ChefEntity chef = new ChefEntity();
        chef.setChefId(CHEF_ID);
        chef.setMobileNo("8888888888");
        when(chefRepository.findByMobileNo("8888888888")).thenReturn(Optional.of(chef));

        MockHttpServletRequest req = requestWithBearer();
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(req, res, new Object());

        assertTrue(proceed);
        assertEquals(200, res.getStatus());
        assertEquals(chef, req.getAttribute("chef"));
        verify(logoutService, times(1)).isAccessTokenRevoked(RAW_ACCESS_TOKEN);
    }

    @Test
    void missingAuthorizationHeader_denylistNeverConsulted() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(req, res, new Object());

        assertFalse(proceed);
        assertEquals(401, res.getStatus());
        verify(logoutService, never()).isAccessTokenRevoked(anyString());
    }

    @Test
    void invalidSignatureToken_denylistNeverConsulted() throws Exception {
        when(jwtUtil.validateToken(RAW_ACCESS_TOKEN)).thenReturn(false);

        boolean proceed = interceptor.preHandle(requestWithBearer(), new MockHttpServletResponse(), new Object());

        assertFalse(proceed);
        verify(logoutService, never()).isAccessTokenRevoked(anyString());
    }
}