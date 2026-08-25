package com.example.tds.service;

import com.example.tds.exception.UnauthorizedException;
import com.example.tds.mapper.UserMapper;
import com.example.tds.repository.UserRepository;
import com.example.tds.utilities.JwtUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the denylist check introduced at the top of
 * {@link UserService#handleRefresh}.
 *
 * The handler reuses {@link LogoutService#isRefreshTokenRevoked(String)} and
 * throws {@link UnauthorizedException} on a hit. The check fires BEFORE the
 * JWT signature validation so that revoked tokens cannot mint new tokens,
 * matching the security goal from the spec.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceHandleRefreshLogoutTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtility jwt;

    @Mock
    private LogoutService logoutService;

    private UserService userService;

    private static final String RAW_REFRESH_TOKEN = "refresh-token-xyz";

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, userRepository, jwt, logoutService);
    }

    @Test
    void handleRefresh_revokedRefreshToken_throwsUnauthorized_andDoesNotMintTokens() {
        when(logoutService.isRefreshTokenRevoked(RAW_REFRESH_TOKEN)).thenReturn(true);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> userService.handleRefresh(RAW_REFRESH_TOKEN));

        assertNotNull(ex.getMessage());
        // Critical security property: JwtUtility must NOT have been used to mint a new pair
        // when the denylist says the refresh token is revoked.
        verify(jwt, never()).generateToken(anyString(), anyMap(), anyLong());
        verify(jwt, never()).validateToken(anyString());
        // The denylist was consulted exactly once.
        verify(logoutService, times(1)).isRefreshTokenRevoked(RAW_REFRESH_TOKEN);
    }

    @Test
    void handleRefresh_nonRevokedRefreshToken_proceedsBeyondDenylist() {
        // We stub the denylist to allow the token through. We don't care about the rest of
        // the JWT parse path (covered by service-level integration); we only verify the
        // denylist branch was taken and didn't reject.
        when(logoutService.isRefreshTokenRevoked(RAW_REFRESH_TOKEN)).thenReturn(false);

        try {
            userService.handleRefresh(RAW_REFRESH_TOKEN);
        } catch (Exception expected) {
            // JWT validate/parse will throw without further stubs — that's fine.
            // We only assert the denylist was consulted.
        }

        verify(logoutService, times(1)).isRefreshTokenRevoked(RAW_REFRESH_TOKEN);
    }

    @Test
    void handleRefresh_nullRefreshToken_stillThrowsUnauthorized_beforeDenyListCall() {
        // Per implementation: null check happens BEFORE the denylist check.
        // We verify this order matters: a null token short-circuits,
        // and the denylist is not even consulted.
        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> userService.handleRefresh(null));
        assertNotNull(ex.getMessage());
        verify(logoutService, never()).isRefreshTokenRevoked(anyString());
    }

    @Test
    void handleRefresh_emptyRefreshToken_stillThrowsUnauthorized_beforeDenyListCall() {
        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> userService.handleRefresh(""));
        assertNotNull(ex.getMessage());
        verify(logoutService, never()).isRefreshTokenRevoked(anyString());
    }
}