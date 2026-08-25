package com.example.tds.interceptors;

import com.example.tds.entity.DeliveryAgentEntity;
import com.example.tds.repository.DeliveryAgentRepository;
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
 * Denylist-gate tests for {@link DeliveryAgentAuthInterceptor}.
 */
@ExtendWith(MockitoExtension.class)
class DeliveryAgentAuthInterceptorLogoutTest {

    @Mock
    private JwtUtility jwtUtil;

    @Mock
    private DeliveryAgentRepository deliveryAgentRepository;

    @Mock
    private LogoutService logoutService;

    private DeliveryAgentAuthInterceptor interceptor;

    private static final String RAW_ACCESS_TOKEN = "agent-access-token";
    private static final UUID AGENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        interceptor = new DeliveryAgentAuthInterceptor();
        org.springframework.test.util.ReflectionTestUtils.setField(interceptor, "jwtUtil", jwtUtil);
        org.springframework.test.util.ReflectionTestUtils.setField(interceptor, "deliveryAgentRepository", deliveryAgentRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(interceptor, "logoutService", logoutService);
    }

    private MockHttpServletRequest requestWithBearer() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + RAW_ACCESS_TOKEN);
        return req;
    }

    @Test
    void denylistHit_returns401_andDoesNotLookUpAgent() throws Exception {
        when(jwtUtil.validateToken(RAW_ACCESS_TOKEN)).thenReturn(true);
        when(logoutService.isAccessTokenRevoked(RAW_ACCESS_TOKEN)).thenReturn(true);

        MockHttpServletResponse res = new MockHttpServletResponse();
        boolean proceed = interceptor.preHandle(requestWithBearer(), res, new Object());

        assertFalse(proceed);
        assertEquals(401, res.getStatus());
        verify(deliveryAgentRepository, never()).findByMobileNo(anyString());
    }

    @Test
    void tokenNotOnDenylist_proceedsToAgentLookup_andAttachesDeliveryAgentAttribute() throws Exception {
        when(jwtUtil.validateToken(RAW_ACCESS_TOKEN)).thenReturn(true);
        when(logoutService.isAccessTokenRevoked(RAW_ACCESS_TOKEN)).thenReturn(false);

        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(claims.get("mobileNo", String.class)).thenReturn("7777777777");
        when(jwtUtil.extractAllClaims(RAW_ACCESS_TOKEN)).thenReturn(claims);

        DeliveryAgentEntity agent = new DeliveryAgentEntity();
        agent.setDeliveryAgentId(AGENT_ID);
        agent.setMobileNo("7777777777");
        when(deliveryAgentRepository.findByMobileNo("7777777777")).thenReturn(Optional.of(agent));

        MockHttpServletRequest req = requestWithBearer();
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(req, res, new Object());

        assertTrue(proceed);
        assertEquals(200, res.getStatus());
        assertEquals(agent, req.getAttribute("deliveryAgent"));
        verify(logoutService, times(1)).isAccessTokenRevoked(RAW_ACCESS_TOKEN);
    }

    @Test
    void missingAuthorizationHeader_denylistNeverConsulted() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        boolean proceed = interceptor.preHandle(new MockHttpServletRequest(), res, new Object());

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