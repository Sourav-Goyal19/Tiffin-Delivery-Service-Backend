package com.example.tds.controller;

import com.example.tds.dto.requests.common.LogoutRequest;
import com.example.tds.entity.DeliveryAgentEntity;
import com.example.tds.service.DeliveryAgentService;
import com.example.tds.service.LogoutService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-level tests for {@link DeliveryAgentController#handleLogout}.
 */
@ExtendWith(MockitoExtension.class)
class DeliveryAgentLogoutControllerTest {

    @Mock
    private DeliveryAgentService deliveryAgentService;

    @Mock
    private LogoutService logoutService;

    private DeliveryAgentController deliveryAgentController;

    private MockMvc mockMvc;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final UUID AGENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String RAW_ACCESS_TOKEN = "agent-access-token-abc";

    @BeforeEach
    void setUp() {
        deliveryAgentController = new DeliveryAgentController(deliveryAgentService, logoutService);
        mockMvc = MockMvcBuilders.standaloneSetup(deliveryAgentController).setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter()).build();
    }

    private DeliveryAgentEntity buildAgent() {
        DeliveryAgentEntity agent = new DeliveryAgentEntity();
        agent.setDeliveryAgentId(AGENT_ID);
        agent.setMobileNo("7777777777");
        return agent;
    }

    @Test
    void logout_happyPath_noBody_returns200AndStandardEnvelope() throws Exception {
        String bearer = "Bearer " + RAW_ACCESS_TOKEN;

        MvcResult result = mockMvc.perform(post("/api/delivery-agents/logout")
                        .header("Authorization", bearer)
                        .requestAttr("deliveryAgent", buildAgent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"))
                .andReturn();

        verify(logoutService, times(1))
                .revokeAccessToken(eq(RAW_ACCESS_TOKEN), eq("deliveryAgent"), eq(AGENT_ID));
        verify(logoutService, never()).revokeRefreshToken(anyString(), anyString(), any(UUID.class));

        java.util.Map<String, Object> body = OBJECT_MAPPER.readValue(result.getResponse().getContentAsString(), new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
        assertEquals(Boolean.TRUE, body.get("success"));
        assertEquals("Logged out successfully", body.get("message"));
    }

    @Test
    void logout_withRefreshTokenInBody_revokesBothTokens() throws Exception {
        String bearer = "Bearer " + RAW_ACCESS_TOKEN;
        String refreshToken = "agent-refresh-jwt";
        LogoutRequest body = new LogoutRequest(refreshToken);

        mockMvc.perform(post("/api/delivery-agents/logout")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(body))
                        .requestAttr("deliveryAgent", buildAgent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(logoutService).revokeAccessToken(eq(RAW_ACCESS_TOKEN), eq("deliveryAgent"), eq(AGENT_ID));
        verify(logoutService).revokeRefreshToken(eq(refreshToken), eq("deliveryAgent"), eq(AGENT_ID));
    }

    @Test
    void logout_missingAuthorizationHeader_returns400FromController() throws Exception {
        mockMvc.perform(post("/api/delivery-agents/logout")
                        .requestAttr("deliveryAgent", buildAgent()))
                .andExpect(status().isBadRequest());

        verify(logoutService, never()).revokeAccessToken(anyString(), anyString(), any(UUID.class));
    }

    @Test
    void logout_oversizedRefreshToken_returns400() throws Exception {
        String bearer = "Bearer " + RAW_ACCESS_TOKEN;
        LogoutRequest body = new LogoutRequest("a".repeat(2049));

        mockMvc.perform(post("/api/delivery-agents/logout")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(body))
                        .requestAttr("deliveryAgent", buildAgent()))
                .andExpect(status().isBadRequest());

        verify(logoutService, never()).revokeAccessToken(anyString(), anyString(), any(UUID.class));
    }

    @Test
    void logout_malformedJsonBody_returns400() throws Exception {
        String bearer = "Bearer " + RAW_ACCESS_TOKEN;

        mockMvc.perform(post("/api/delivery-agents/logout")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-a-json-object")
                        .requestAttr("deliveryAgent", buildAgent()))
                .andExpect(status().isBadRequest());

        verify(logoutService, never()).revokeAccessToken(anyString(), anyString(), any(UUID.class));
    }

    @Test
    void logout_emptyJsonBody_skipsRefreshRevocation_callRecordsNullRefresh() throws Exception {
        String bearer = "Bearer " + RAW_ACCESS_TOKEN;

        mockMvc.perform(post("/api/delivery-agents/logout")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("deliveryAgent", buildAgent()))
                .andExpect(status().isOk());

        verify(logoutService).revokeAccessToken(eq(RAW_ACCESS_TOKEN), eq("deliveryAgent"), eq(AGENT_ID));
        verify(logoutService).revokeRefreshToken(eq((String) null), eq("deliveryAgent"), eq(AGENT_ID));
    }
}