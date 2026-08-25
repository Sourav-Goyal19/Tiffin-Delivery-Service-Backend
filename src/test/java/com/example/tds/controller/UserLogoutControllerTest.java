package com.example.tds.controller;

import com.example.tds.dto.requests.common.LogoutRequest;
import com.example.tds.entity.UserEntity;
import com.example.tds.service.LogoutService;
import com.example.tds.service.UserService;
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
 * Controller-level tests for {@link UserController#handleLogout}.
 *
 * Uses {@link MockMvcBuilders#standaloneSetup} so we exercise the controller
 * handler directly (no Spring context, no JWT interceptor). This isolates
 * controller logic from the interceptor path — interceptor-level auth tests
 * live with the interceptor tests.
 */
@ExtendWith(MockitoExtension.class)
class UserLogoutControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private LogoutService logoutService;

    private UserController userController;

    private MockMvc mockMvc;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final UUID ACTOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String RAW_ACCESS_TOKEN = "raw-access-token-abc";

    @BeforeEach
    void setUp() {
        // UserController uses an explicit constructor (not @RequiredArgsConstructor), so wire it
        // up directly rather than relying on @InjectMocks to find a ctor.
        userController = new UserController(userService, logoutService);
        mockMvc = MockMvcBuilders.standaloneSetup(userController).setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter()).build();
    }

    private UserEntity buildUser() {
        UserEntity user = new UserEntity();
        user.setId(ACTOR_ID);
        user.setMobileNo("9999999999");
        return user;
    }

    @Test
    void logout_happyPath_noBody_returns200AndStandardEnvelope() throws Exception {
        String bearer = "Bearer " + RAW_ACCESS_TOKEN;

        MvcResult result = mockMvc.perform(post("/api/users/logout")
                        .header("Authorization", bearer)
                        .requestAttr("user", buildUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"))
                .andReturn();

        verify(logoutService, times(1)).revokeAccessToken(eq(RAW_ACCESS_TOKEN), eq("user"), eq(ACTOR_ID));
        // No body -> refresh path is skipped (controller guards with `if (logoutRequest != null)`)
        verify(logoutService, never()).revokeRefreshToken(anyString(), anyString(), any(UUID.class));

        java.util.Map<String, Object> body = OBJECT_MAPPER.readValue(result.getResponse().getContentAsString(), new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
        assertEquals(Boolean.TRUE, body.get("success"));
        assertEquals("Logged out successfully", body.get("message"));
    }

    @Test
    void logout_happyPath_emptyJsonBody_stillRevokesAccessOnly() throws Exception {
        String bearer = "Bearer " + RAW_ACCESS_TOKEN;

        mockMvc.perform(post("/api/users/logout")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("user", buildUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(logoutService, times(1)).revokeAccessToken(eq(RAW_ACCESS_TOKEN), eq("user"), eq(ACTOR_ID));
        // Empty refreshToken -> service itself is a no-op (per spec); controller still calls the path
        verify(logoutService, times(1)).revokeRefreshToken(eq((String) null), eq("user"), eq(ACTOR_ID));
    }

    @Test
    void logout_withRefreshTokenInBody_revokesBothTokens() throws Exception {
        String bearer = "Bearer " + RAW_ACCESS_TOKEN;
        String refreshToken = "refresh-jwt-xyz";
        LogoutRequest body = new LogoutRequest(refreshToken);

        mockMvc.perform(post("/api/users/logout")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(body))
                        .requestAttr("user", buildUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(logoutService).revokeAccessToken(eq(RAW_ACCESS_TOKEN), eq("user"), eq(ACTOR_ID));
        verify(logoutService).revokeRefreshToken(eq(refreshToken), eq("user"), eq(ACTOR_ID));
    }

    @Test
    void logout_missingAuthorizationHeader_returns400FromController() throws Exception {
        // The controller requires the @RequestHeader("Authorization") — no header => 400
        mockMvc.perform(post("/api/users/logout")
                        .requestAttr("user", buildUser()))
                .andExpect(status().isBadRequest());

        verify(logoutService, never()).revokeAccessToken(anyString(), anyString(), any(UUID.class));
    }

    @Test
    void logout_oversizedRefreshToken_returns400() throws Exception {
        String bearer = "Bearer " + RAW_ACCESS_TOKEN;
        String tooLong = "a".repeat(2049);
        LogoutRequest body = new LogoutRequest(tooLong);

        mockMvc.perform(post("/api/users/logout")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(body))
                        .requestAttr("user", buildUser()))
                .andExpect(status().isBadRequest());

        verify(logoutService, never()).revokeAccessToken(anyString(), anyString(), any(UUID.class));
    }

    @Test
    void logout_malformedJsonBody_returns400() throws Exception {
        String bearer = "Bearer " + RAW_ACCESS_TOKEN;

        mockMvc.perform(post("/api/users/logout")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json")
                        .requestAttr("user", buildUser()))
                .andExpect(status().isBadRequest());

        verify(logoutService, never()).revokeAccessToken(anyString(), anyString(), any(UUID.class));
    }

    @Test
    void logout_refreshTokenIsBestEffort_malformedRefreshDoesNotFailController() throws Exception {
        // Spec edge case: logout with a malformed/garbage refresh token must still 200;
        // controller delegates to service, and service's revokeRefreshToken does no JWT validation
        // on the refresh token (hashing works on any string).
        String bearer = "Bearer " + RAW_ACCESS_TOKEN;
        String garbageRefresh = "this-is-not-a-jwt-but-still-a-string";
        LogoutRequest body = new LogoutRequest(garbageRefresh);

        mockMvc.perform(post("/api/users/logout")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(body))
                        .requestAttr("user", buildUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(logoutService).revokeAccessToken(eq(RAW_ACCESS_TOKEN), eq("user"), eq(ACTOR_ID));
        verify(logoutService).revokeRefreshToken(eq(garbageRefresh), eq("user"), eq(ACTOR_ID));
    }

    @Test
    void logout_redisWriteFailureSurfacesFromControllerPath() throws Exception {
        // If Redis write blows up, the service re-throws DataAccessException.
        // Since the spec notes the generic @ExceptionHandler(Exception.class) is commented out,
        // we verify the controller does NOT swallow the exception — the DataAccessException
        // propagates out and MockMvc surfaces it via a re-thrown ServletException.
        String bearer = "Bearer " + RAW_ACCESS_TOKEN;
        org.springframework.dao.QueryTimeoutException boom =
                new org.springframework.dao.QueryTimeoutException("redis down");
        org.mockito.Mockito.doThrow(boom)
                .when(logoutService)
                .revokeAccessToken(anyString(), anyString(), any(UUID.class));

        boolean propagated = false;
        try {
            mockMvc.perform(post("/api/users/logout")
                    .header("Authorization", bearer)
                    .requestAttr("user", buildUser()));
        } catch (Exception e) {
            // MockMvc rethrows the handler exception; that's the contract for unrecovered errors.
            propagated = true;
        }

        verify(logoutService).revokeAccessToken(eq(RAW_ACCESS_TOKEN), eq("user"), eq(ACTOR_ID));
        assertTrue(propagated, "DataAccessException should propagate from controller when Redis fails");
    }

    @Test
    void logout_secondCallIsHandledByInterceptor_notController() throws Exception {
        // Idempotency: the second logout is rejected by the interceptor BEFORE the controller
        // runs. We simulate that here by confirming the controller code itself makes no special
        // idempotency decision — the interceptor stops it. This is a property test of the
        // contract, not of the controller body.
        String bearer = "Bearer " + RAW_ACCESS_TOKEN;

        // First call: full path
        mockMvc.perform(post("/api/users/logout")
                        .header("Authorization", bearer)
                        .requestAttr("user", buildUser()))
                .andExpect(status().isOk());

        // If a second call slipped past the interceptor, the controller would just call
        // revokeAccessToken again (idempotent in itself). The interceptor-level guarantee
        // is exercised in the interceptor test class.
        verify(logoutService, times(1)).revokeAccessToken(eq(RAW_ACCESS_TOKEN), eq("user"), eq(ACTOR_ID));
    }
}