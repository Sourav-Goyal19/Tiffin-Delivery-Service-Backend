# Logout Functionality — Implementation Plan

**Spec:** `.claude/specs/02-logout-functionality.md`

## Context

Today the API has no way to terminate a session — once an access token (3h) or refresh token (7d) is issued, the only way to "invalidate" it is to wait for natural expiry. Add three symmetric logout endpoints — one per actor type — that revoke the bearer token in a Redis denylist. The denylist is checked by the existing role-specific `*AuthInterceptor`s on every subsequent request, so revoked tokens stop working immediately without any per-user schema change.

**Endpoints:**
- `POST /api/users/logout`
- `POST /api/chefs/logout`
- `POST /api/delivery-agents/logout`

Each is auth-protected by its role's existing `*AuthInterceptor` (already covers `/api/{role}/**` per `WebConfig`).

**Denylist design:** key the entry by `SHA-256(rawToken)` hex (the current `JwtUtility` mints no `jti`, and adding one is out of scope). TTL equals the remaining token lifetime, so the denylist self-prunes — no background cleanup, no schema changes.

**Idempotency:** a second logout call with the same token is rejected by the interceptor's denylist check with the same 401 it returns for any other invalid token. This is the same outcome as a normal "already logged out" client experience — no special idempotency layer, no separate error.

---

## Architecture

```
UserController.logout / ChefController.logout / DeliveryAgentController.logout
        ↓
@RequestAttribute("user"|"chef"|"deliveryAgent")  (already loaded by interceptor)
@RequestBody LogoutRequest  (optional refreshToken)
        ↓
UserService.handleLogout / ChefService.handleLogout / DeliveryAgentService.handleLogout
        ↓
JwtUtility.getRemainingSeconds(rawAccessToken)    → TTL
TokenHasher.sha256Hex(rawToken)                   → key suffix
StringRedisTemplate.opsForValue().set(...)        → denylist write (TTL = exp - now)
StringRedisTemplate.opsForValue().set(...)        → denylist write (TTL = 7d) if refreshToken supplied
        ↓
ApiResponse { message: "Logged out successfully", success: true, data: null }

(separate path on /refresh:)
handleRefresh(incomingRefreshToken)
        ↓
TokenHasher.sha256Hex → check EXISTS auth:denylist:refresh:{hash} → 401 if present
```

Interceptors call into the same `TokenHasher` + Redis check before loading the entity. Fail-open on Redis outage with WARN log.

---

## Files to Create / Modify

### 1. NEW `src/main/java/com/example/tds/dto/requests/common/LogoutRequest.java`

```java
package com.example.tds.dto.requests.common;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {
    @Size(max = 2048, message = "refreshToken must not exceed 2048 characters")
    private String refreshToken;
}
```

**Why no `@NotBlank`:** the body is optional per spec §1/§7. A present-but-empty `refreshToken` is invalid (handled below), but an entirely absent body is fine. Validation stays on `@Size` only. Matches `RefreshTokenRequest` style (Lombok class, not record). Placed in `dto/requests/common/` next to `RefreshTokenRequest.java` to mirror its sibling.

### 2. NEW `src/main/java/com/example/tds/utilities/TokenHasher.java`

```java
package com.example.tds.utilities;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class TokenHasher {

    public String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required by the JRE; this branch is unreachable.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
```

**Why a separate util and not a method on `JwtUtility`:** `JwtUtility` is JWT-only (parse/issue). Hashing raw bytes has nothing to do with JWTs and would muddy that class. `TokenHasher` is small, single-purpose, and reusable if any future feature (e.g., refresh-token rotation tracking) needs to derive a stable key from an opaque token. `@Component` so it's injectable into both the service and the interceptors.

### 3. MODIFY `src/main/java/com/example/tds/utilities/JwtUtility.java`

Add one helper to centralize the access-token remaining-lifetime computation (used by `LogoutService` and any future denylist writer):

```java
public long getRemainingSeconds(String token) {
    Claims claims = extractAllClaims(token);
    long expMs = claims.getExpiration().getTime();
    long remainingMs = expMs - System.currentTimeMillis();
    return Math.max(0L, remainingMs / 1000L);
}
```

**Why here:** already has `extractAllClaims` and is the only thing in the codebase that knows about JWT claims. No new utility needed. Throws if the token is malformed — callers already gate this on a successful `validateToken(...)` check first.

### 4. NEW `src/main/java/com/example/tds/service/LogoutService.java`

Single service used by all three role controllers — the operation is symmetric and the only role-specific concern is the `actorId` (already on the request attribute) and the log prefix. Putting it in one class avoids triplicated denylist-writing logic.

```java
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

    static final String ACCESS_KEY_PREFIX  = "auth:denylist:access:";
    static final String REFRESH_KEY_PREFIX = "auth:denylist:refresh:";
    static final Duration REFRESH_TTL = Duration.ofDays(7);

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtility jwtUtility;
    private final TokenHasher tokenHasher;

    /**
     * Revoke an access token in the Redis denylist with TTL = remaining lifetime.
     */
    public void revokeAccessToken(String rawAccessToken, String role, UUID actorId) {
        long ttlSeconds = jwtUtility.getRemainingSeconds(rawAccessToken);
        if (ttlSeconds <= 0) {
            log.info("Logout skipped (already expired) role={} actorId={}", role, actorId);
            return;
        }
        String key = ACCESS_KEY_PREFIX + tokenHasher.sha256Hex(rawAccessToken);
        try {
            stringRedisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
            log.info("Logout successful role={} actorId={}", role, actorId);
        } catch (DataAccessException e) {
            log.error("Logout denylist write failed role={} actorId={} reason={}",
                    role, actorId, e.getMessage());
            throw e; // surfaces via Spring's default 500 (generic handler is commented out)
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
        String key = REFRESH_KEY_PREFIX + tokenHasher.sha256Hex(rawRefreshToken);
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
        String key = ACCESS_KEY_PREFIX + tokenHasher.sha256Hex(rawAccessToken);
        try {
            Boolean present = stringRedisTemplate.hasKey(key);
            return Boolean.TRUE.equals(present);
        } catch (DataAccessException e) {
            log.warn("Denylist read failed; failing open role=unknown reason={}", e.getMessage());
            return false;
        }
    }

    /** Read-path check used by /refresh. Fail-open on Redis outage. */
    public boolean isRefreshTokenRevoked(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return false;
        }
        String key = REFRESH_KEY_PREFIX + tokenHasher.sha256Hex(rawRefreshToken);
        try {
            Boolean present = stringRedisTemplate.hasKey(key);
            return Boolean.TRUE.equals(present);
        } catch (DataAccessException e) {
            log.warn("Refresh denylist read failed; failing open reason={}", e.getMessage());
            return false;
        }
    }
}
```

**Why one service, not three:** symmetric operation, single Redis key family, identical TTL math. The role-specific services (`UserService`, `ChefService`, `DeliveryAgentService`) remain responsible for issuing/refresh/logout-of-business-data; this service is purely the denylist write. Controllers inject `LogoutService` directly to avoid adding another method to each role service (which would touch more files than necessary).

**Why `hasKey` not `EXISTS`:** Spring's `StringRedisTemplate.hasKey(key)` issues an `EXISTS` against Redis — single round-trip, sub-ms on a local Redis, matches the spec's NFR §Performance.

**Why fail-open on read:** matches the existing interceptor style (interceptors do not currently fault on infrastructure failures). Documented trade-off per spec §Reliability.

### 5. MODIFY `src/main/java/com/example/tds/interceptors/UserAuthInterceptor.java`

Inject `LogoutService`. After `jwtUtil.validateToken(...)` succeeds and **before** `findByMobileNo`, check the denylist.

```java
@Autowired
private LogoutService logoutService;

// inside preHandle, after the validateToken block, before extractAllClaims / findByMobileNo:
if (logoutService.isAccessTokenRevoked(accessToken)) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.getWriter().write("{" +
            "\"error\": \"Invalid or expired token\",\n" +
            "\"success\": \"false\"" +
        "}");
    return false;
}
```

**Why reuse the existing JSON shape:** spec §Acceptance "all three `*AuthInterceptor`s reject access tokens whose hash is in the denylist with 401". Same wire format as the existing "Invalid or expired token" response — clients cannot tell denylist from signature failure (and shouldn't, per the security goal of treating revoked tokens as just another invalid token).

**Idempotency:** the second logout call is rejected here, before any controller runs. No special branch needed.

### 6. MODIFY `src/main/java/com/example/tds/interceptors/ChefAuthInterceptor.java`

Identical insertion as `UserAuthInterceptor` — inject `LogoutService` and add the same denylist check after `jwtUtil.validateToken`. Same JSON error shape.

### 7. MODIFY `src/main/java/com/example/tds/interceptors/DeliveryAgentAuthInterceptor.java`

Same change as the other two interceptors.

### 8. MODIFY `src/main/java/com/example/tds/controller/UserController.java`

```java
private final LogoutService logoutService;

public UserController(UserService userService, LogoutService logoutService){
    this.userService = userService;
    this.logoutService = logoutService;
}

@PostMapping("/logout")
public ResponseEntity<ApiResponse> handleLogout(
        @RequestAttribute("user") UserEntity currentUser,
        @RequestHeader("Authorization") String authorizationHeader,
        @RequestBody(required = false) @Valid LogoutRequest logoutRequest
) {
    String rawAccessToken = authorizationHeader.substring("Bearer ".length());
    UUID actorId = currentUser.getId();
    logoutService.revokeAccessToken(rawAccessToken, "user", actorId);
    if (logoutRequest != null) {
        logoutService.revokeRefreshToken(logoutRequest.getRefreshToken(), "user", actorId);
    }
    return ResponseEntity.status(HttpStatus.OK).body(
            ApiResponse.builder()
                    .message("Logged out successfully")
                    .success(true)
                    .build()
    );
}
```

**Why pass `Authorization` as `@RequestHeader` rather than re-deriving:** the interceptor has already validated the token. Reading the raw header keeps the controller trivially consistent with the interceptor (same source of truth). The controller does not re-validate; if the header is malformed, the interceptor already rejected the call.

**Why `@RequestBody(required = false)`:** spec §7 accepts both `{}` and no body at all. Without `required = false`, a missing body would 400 on `HttpMessageNotReadableException`.

### 9. MODIFY `src/main/java/com/example/tds/controller/ChefController.java`

Same shape as `UserController.handleLogout`, but `@RequestAttribute("chef") ChefEntity` and `"chef"` role tag. Add a `LogoutService logoutService` field (constructor injection). `@RequiredArgsConstructor` is already on this controller — add `private final LogoutService logoutService;` and Lombok generates the constructor.

### 10. MODIFY `src/main/java/com/example/tds/controller/DeliveryAgentController.java`

Same shape as above, `@RequestAttribute("deliveryAgent")` and `"deliveryAgent"` role tag. Same constructor pattern.

### 11. MODIFY `src/main/java/com/example/tds/service/UserService.java` (handleRefresh)

Inject `LogoutService` (Lombok constructor injection). At the top of `handleRefresh`, after the empty-token guard but before the existing `jwt.validateToken(refreshToken)` check, call the denylist check:

```java
if (logoutService.isRefreshTokenRevoked(refreshToken)) {
    throw new UnauthorizedException("Refresh token invalid or expired. Please login again.");
}
```

The existing `UnauthorizedException` message is reused so the 401 response shape stays identical to "expired refresh token" — clients can't distinguish revocation from natural expiry (and shouldn't, per the security goal). No DB or claim change.

### 12. MODIFY `src/main/java/com/example/tds/service/ChefService.java` (handleRefresh)

Identical insertion as `UserService.handleRefresh`.

### 13. MODIFY `src/main/java/com/example/tds/service/DeliveryAgentService.java` (handleRefresh)

Identical insertion.

---

## Reused Components

| Component | Path | Notes |
|---|---|---|
| `UserAuthInterceptor`, `ChefAuthInterceptor`, `DeliveryAgentAuthInterceptor` | `interceptors/` | Extended with denylist check after `validateToken` |
| `JwtUtility.extractAllClaims`, `validateToken` | `utilities/` | New `getRemainingSeconds` helper added |
| `StringRedisTemplate` | `config/RedisConfig` | Already a `@Bean` — `LogoutService` injects it directly |
| `ApiResponse` / `ErrorResponse` | `dto/responses/` | Existing envelopes; `ApiResponse` has `@JsonInclude(NON_NULL)` so `data` is omitted when null |
| `UnauthorizedException` | `exception/` | Reused for both interceptor-denied and refresh-denied paths |
| `GlobalExceptionHandler.handleAuthorization` | `exception/` | Already maps `UnauthorizedException` → 401 + `ErrorResponse` |
| `WebConfig.addPathPatterns` | `config/` | `/api/{role}/**` already covers `/logout` — no change needed |
| `UserService`/`ChefService`/`DeliveryAgentService` | `service/` | Existing `handleRefresh` is the only point where refresh-token revocation is enforced (the three `*AuthInterceptor`s do not run on `/refresh` because it's excluded) |
| Lombok + MapStruct conventions, `@Slf4j`, `@Transactional` | project-wide | `LogoutService` is `@Service @RequiredArgsConstructor @Slf4j`; no `@Transactional` because the write is to Redis, not Postgres |

---

## Out of Scope

- **`jti` claim in JWTs:** spec hashes the raw token because `JwtUtility` mints no `jti`. Adding `jti` is a larger change touching token issuance, parsing, and refresh flow — out of scope.
- **Per-user "log out everywhere":** the denylist is keyed by token, not user. A user with two devices must call logout twice (one per device). This matches the spec.
- **Persistent refresh-token storage:** remains stateless. Only the denylist knows about revoked tokens.
- **Cross-actor namespace separation:** spec §Open Questions suggests `auth:denylist:{role}:access:...` for defense-in-depth. **Decision: keep the flat namespace.** A collision requires two actors with identical JWT subject/issued-at/expiry/claims, which is astronomically unlikely in practice; adding a role suffix costs nothing in safety but complicates the hash contract for `/refresh`. If the team prefers defense-in-depth, change is local: one new prefix constant in `LogoutService`.
- **Tests:** codebase has only `contextLoads()`. Adding JUnit/Mockito fixtures is a separate effort. The current plan is verified by `./mvnw clean compile` + manual curl (see Verification).
- **Generic `@ExceptionHandler(Exception.class)` re-enable:** spec §Open Questions flags that an unhandled Redis outage during logout falls through to Spring's default 500 handler. **Decision: do not re-enable.** A targeted re-enable affects every exception path in the app, not just logout. If Redis-outage-500-becomes-ErrorResponse is desired, do it as its own spec.

---

## Open Decisions (made in this plan)

1. **Single `LogoutService` vs three role-specific methods.** Chose single — symmetric op, single Redis key family, no role-specific logic beyond the log tag.
2. **Hash strategy.** SHA-256 hex of the raw token string (the part after `Bearer `). Stored only as hex in Redis; raw token never persisted.
3. **TTL source for access-token denylist.** JWT `exp` claim, not `security.jwt.expiration-time` from `application.properties` (which is unused / wrong: 36000s = 10h vs. the actual 3h). Source of truth is the claim itself.
4. **Refresh-token revocation enforcement point.** Only inside the three `handleRefresh` methods, not via an interceptor (`/refresh` is excluded from auth interceptors per `WebConfig`). The interceptor path on subsequent access-token calls already kills the access side.
5. **Second logout call.** Returns the same 401 the interceptor returns for any invalid token. No new error path.
6. **`@RequestBody(required = false)`** to allow `Content-Length: 0` requests and absent bodies, matching spec §7.
7. **`/refresh` exclusion unchanged.** `WebConfig.excludePathPatterns` keeps `/api/{role}/refresh` out of the auth interceptor so the refresh flow remains reachable without an access token.

---

## Implementation Order

1. `LogoutRequest` DTO (pure DTO, no deps)
2. `TokenHasher` (pure util)
3. `JwtUtility.getRemainingSeconds` (extend existing)
4. `LogoutService` (depends on 2 + 3 + `StringRedisTemplate`)
5. `UserAuthInterceptor` / `ChefAuthInterceptor` / `DeliveryAgentAuthInterceptor` — add denylist check
6. `UserController` / `ChefController` / `DeliveryAgentController` — add `handleLogout`
7. `UserService` / `ChefService` / `DeliveryAgentService` — add denylist check at top of `handleRefresh`

---

## Verification

1. **Compile:** `./mvnw clean compile`
2. **Run:** `./mvnw spring-boot:run` (requires `.env`; Redis must be reachable for `SET` to succeed)
3. **Manual API tests** (Bearer token from `/otp/verify` flow):

   ```bash
   # Happy path — empty body, access denylisted, 200
   curl -X POST http://localhost:8000/api/users/logout \
        -H "Authorization: Bearer <access_token>"

   # With refresh token — both denylisted, 200
   curl -X POST http://localhost:8000/api/users/logout \
        -H "Authorization: Bearer <access_token>" \
        -H "Content-Type: application/json" \
        -d '{"refreshToken":"<refresh_token>"}'

   # Second logout with same access token — 401 (denylist hit, interceptor rejects)
   curl -X POST http://localhost:8000/api/users/logout \
        -H "Authorization: Bearer <access_token>"

   # Subsequent /api/users/me with same token — 401
   curl http://localhost:8000/api/users/me \
        -H "Authorization: Bearer <access_token>"

   # /refresh after logout (refreshToken denylisted) — 401
   curl -X POST http://localhost:8000/api/users/refresh \
        -H "Content-Type: application/json" \
        -d '{"refreshToken":"<refresh_token>"}'

   # Oversized refreshToken — 400 (Validation failed)
   curl -X POST http://localhost:8000/api/users/logout \
        -H "Authorization: Bearer <access_token>" \
        -H "Content-Type: application/json" \
        -d "$(python3 -c 'import json; print(json.dumps({"refreshToken":"a"*3000}))')"

   # No Authorization header — 401 (interceptor rejects before controller)
   curl -X POST http://localhost:8000/api/users/logout

   # Expired access token — 401 (interceptor rejects via validateToken)
   ```

4. **Chef and Delivery-Agent variants:** repeat the above against `/api/chefs/logout` and `/api/delivery-agents/logout`.

5. **Redis inspection** — confirm only hashes are stored, no raw tokens:

   ```bash
   redis-cli KEYS 'auth:denylist:*'
   # Expect entries like: auth:denylist:access:<64-hex-chars>
   #                      auth:denylist:refresh:<64-hex-chars>
   redis-cli TTL 'auth:denylist:access:<hash>'
   # Expect ~remaining-seconds, not -1
   ```

6. **Idempotency regression** — after a successful logout, any further call with the same access token to **any** role-scoped endpoint returns 401 (not 500). Subsequent `/refresh` with the supplied refresh token returns 401 (not a new token pair).

7. **Fail-open regression** — stop Redis, call `/api/users/me` with a valid token; expect 200 (interceptor logs WARN, lets the request through). Stop Redis, call logout; expect 500 via Spring default handling, logged at ERROR.