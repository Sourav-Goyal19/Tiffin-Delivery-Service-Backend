package com.example.tds.controller;

import com.example.tds.dto.requests.common.LogoutRequest;
import com.example.tds.entity.ChefEntity;
import com.example.tds.service.ChefService;
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
 * Controller-level tests for {@link ChefController#handleLogout}.
 *
 * Uses {@link MockMvcBuilders#standaloneSetup} so we exercise the controller
 * handler directly. Interceptor-level auth behavior is not asserted here.
 */
@ExtendWith(MockitoExtension.class)
class ChefLogoutControllerTest {

    @Mock
    private ChefService chefService;

    @Mock
    private LogoutService logoutService;

    private ChefController chefController;

    private MockMvc mockMvc;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final UUID CHEF_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String RAW_ACCESS_TOKEN = "chef-access-token-abc";

    @BeforeEach
    void setUp() {
        // ChefController uses Lombok @RequiredArgsConstructor which generates
        // a constructor with (ChefService, LogoutService).
        chefController = new ChefController(chefService, logoutService);
        mockMvc = MockMvcBuilders.standaloneSetup(chefController).setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter()).build();
    }

    private ChefEntity buildChef() {
        ChefEntity chef = new ChefEntity();
        chef.setChefId(CHEF_ID);
        chef.setMobileNo("8888888888");
        return chef;
    }

    @Test
    void logout_happyPath_noBody_returns200AndStandardEnvelope() throws Exception {
        String bearer = "Bearer " + RAW_ACCESS_TOKEN;

        MvcResult result = mockMvc.perform(post("/api/chefs/logout")
                        .header("Authorization", bearer)
                        .requestAttr("chef", buildChef()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"))
                .andReturn();

        verify(logoutService, times(1))
                .revokeAccessToken(eq(RAW_ACCESS_TOKEN), eq("chef"), eq(CHEF_ID));
        verify(logoutService, never()).revokeRefreshToken(anyString(), anyString(), any(UUID.class));

        java.util.Map<String, Object> body = OBJECT_MAPPER.readValue(result.getResponse().getContentAsString(), new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
        assertEquals(Boolean.TRUE, body.get("success"));
        assertEquals("Logged out successfully", body.get("message"));
    }

    @Test
    void logout_withRefreshTokenInBody_revokesBothTokens() throws Exception {
        String bearer = "Bearer " + RAW_ACCESS_TOKEN;
        String refreshToken = "chef-refresh-jwt";
        LogoutRequest body = new LogoutRequest(refreshToken);

        mockMvc.perform(post("/api/chefs/logout")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(body))
                        .requestAttr("chef", buildChef()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(logoutService).revokeAccessToken(eq(RAW_ACCESS_TOKEN), eq("chef"), eq(CHEF_ID));
        verify(logoutService).revokeRefreshToken(eq(refreshToken), eq("chef"), eq(CHEF_ID));
    }

    @Test
    void logout_missingAuthorizationHeader_returns400FromController() throws Exception {
        mockMvc.perform(post("/api/chefs/logout")
                        .requestAttr("chef", buildChef()))
                .andExpect(status().isBadRequest());

        verify(logoutService, never()).revokeAccessToken(anyString(), anyString(), any(UUID.class));
    }

    @Test
    void logout_oversizedRefreshToken_returns400() throws Exception {
        String bearer = "Bearer " + RAW_ACCESS_TOKEN;
        LogoutRequest body = new LogoutRequest("a".repeat(2049));

        mockMvc.perform(post("/api/chefs/logout")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(body))
                        .requestAttr("chef", buildChef()))
                .andExpect(status().isBadRequest());

        verify(logoutService, never()).revokeAccessToken(anyString(), anyString(), any(UUID.class));
    }

    @Test
    void logout_emptyJsonBody_skipsRefreshRevocation_callRecordsNullRefresh() throws Exception {
        String bearer = "Bearer " + RAW_ACCESS_TOKEN;

        mockMvc.perform(post("/api/chefs/logout")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("chef", buildChef()))
                .andExpect(status().isOk());

        verify(logoutService).revokeAccessToken(eq(RAW_ACCESS_TOKEN), eq("chef"), eq(CHEF_ID));
        verify(logoutService).revokeRefreshToken(eq((String) null), eq("chef"), eq(CHEF_ID));
    }

    @Test
    void logout_messageIsIdenticalAcrossRoles_userVsChef() throws Exception {
        // Cross-role invariant: same success message regardless of role.
        String bearer = "Bearer " + RAW_ACCESS_TOKEN;

        MvcResult result = mockMvc.perform(post("/api/chefs/logout")
                        .header("Authorization", bearer)
                        .requestAttr("chef", buildChef()))
                .andExpect(status().isOk())
                .andReturn();

        java.util.Map<String, Object> body = OBJECT_MAPPER.readValue(result.getResponse().getContentAsString(), new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
        assertEquals("Logged out successfully", body.get("message"));
    }
}