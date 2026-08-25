# Logout Functionality

## Overview

Add logout endpoints to terminate authenticated sessions for all three actor types: users (customers), chefs, and delivery agents.

The backend logout functionality allows an authenticated actor to invalidate their current session so that:

- The currently held access token (HS256 JWT, 3h lifetime) can no longer be used to authorize subsequent API calls.
- The refresh token (7d lifetime) can no longer be used to mint a new token pair via `POST /refresh`.
- A repeated logout call with the same already-invalidated token is treated as a successful no-op (idempotent logout).
- An already-expired or malformed token returns the existing standard 401 error rather than a server error.

The implementation integrates with the application's existing architecture: interceptor-based auth (`UserAuthInterceptor`/`ChefAuthInterceptor`/`DeliveryAgentAuthInterceptor`), `JwtUtility` (HS256, secret-key from `security.jwt.secret-key`), `ApiResponse`/`ErrorResponse` envelopes, `GlobalExceptionHandler`, and Redis (already configured via `RedisConfig`).

No new infrastructure is introduced; new code extends existing patterns.

## Scope

The implementation includes:

- Three symmetric logout endpoints — one per actor type — placed under each role's existing controller path
- Access-token invalidation via a Redis denylist keyed by a stable per-token identifier (since the current `JwtUtility` does not mint a `jti`, the denylist key is derived from a SHA-256 hash of the access token)
- Refresh-token revocation via the same Redis denylist (the refresh token is hashed and added to the denylist for its 7-day TTL)
- Idempotent logout behavior (calling logout twice with the same token does not error)
- Standard `ApiResponse` success envelope and `ErrorResponse` error envelope
- Standard error handling via `GlobalExceptionHandler` and existing exceptions (`UnauthorizedException`, `BadRequestException`)

No new infrastructure; extends existing patterns.

## Functional Requirements

### 1. Logout API

Three new endpoints are introduced, one per actor type, mirroring the existing controller namespaces and auth interceptors:

- `POST /api/users/logout` — protected by `UserAuthInterceptor`
- `POST /api/chefs/logout` — protected by `ChefAuthInterceptor`
- `POST /api/delivery-agents/logout` — protected by `DeliveryAgentAuthInterceptor`

- HTTP method: `POST` (logout mutates server state — the denylist — so POST is correct; GET would be unsafe and would be reachable by prefetch caches)
- Auth: `Authorization: Bearer <accessToken>` validated by the role-specific `*AuthInterceptor`. The token is read from the `Authorization` header by the interceptor (the controller never parses the token directly).
- No request body is required. An empty body is accepted.
- Successful response is wrapped in `ApiResponse` with `success: true`, an informational `message`, and `data: null` (or omitted under `JsonInclude.NON_NULL`).
- The endpoints are placed under the existing role-scoped `addPathPatterns("/api/{role}/**")` registrations in `WebConfig` so that the interceptor already authenticates the caller before the controller method is invoked.

The endpoints are NOT added to any `excludePathPatterns` — logout requires a valid, currently-authenticated access token.

### 2. Access Token Invalidation Strategy

The implementation uses a **Redis denylist** to invalidate access tokens.

- **Key derivation:** Because the existing `JwtUtility` (`utilities/JwtUtility.java`) does not place a `jti` claim in minted tokens, the denylist key is derived as `SHA-256(accessToken)` encoded as a hex string. This guarantees a stable, unique per-token identifier without requiring a JWT schema change.
- **Redis key format:** `auth:denylist:access:{sha256Hex}` — String value `"1"`.
- **TTL:** the remaining lifetime of the access token, computed as `exp - now` (in seconds) using the `exp` claim read from the JWT. This guarantees the denylist entry expires at the same time the token would have expired naturally, preventing unbounded growth.
- **Check:** every role-specific `*AuthInterceptor` is extended to look up `auth:denylist:access:{sha256Hex}` after token signature validation and before loading the entity; if present, the interceptor returns `401 Unauthorized` with the existing "Invalid or expired token" JSON shape (matching current behavior for invalid tokens).

The denylist lives in the already-configured Redis instance (`spring.data.redis.host`/`port`/`password`) and uses the existing `StringRedisTemplate` bean exposed by `RedisConfig`. No new Redis client is required.

### 3. Refresh Token Invalidation Strategy

The implementation revokes the refresh token in the same denylist.

- The request body contains an optional `refreshToken` field. If present, the implementation hashes it (`SHA-256`) and adds it to the denylist under `auth:denylist:refresh:{sha256Hex}` with a TTL of 7 days (the refresh-token lifetime).
- If `refreshToken` is absent or blank, the refresh-token portion of logout is skipped (the access token is still invalidated, and the existing refresh token becomes effectively unusable after its natural 7-day expiry — but it is NOT actively revoked).
- The `/refresh` endpoint is extended so that, before minting a new pair, it computes `SHA-256(incomingRefreshToken)` and rejects with `401 Unauthorized` if the key is present in the denylist. This is the single point of enforcement for refresh-token revocation; the existing `*AuthInterceptor`s are not on the `/refresh` path because `/refresh` is excluded.

This strategy keeps refresh tokens stateless (no DB row per refresh token) while still allowing active revocation on logout.

### 4. Auth Entity Retrieval

The logout controllers follow the existing pattern of accepting the authenticated entity via `@RequestAttribute`:

- `UserController.logout` receives `@RequestAttribute("user") UserEntity user` (set by `UserAuthInterceptor`).
- `ChefController.logout` receives `@RequestAttribute("chef") ChefEntity chef` (set by `ChefAuthInterceptor`).
- `DeliveryAgentController.logout` receives `@RequestAttribute("deliveryAgent") DeliveryAgentEntity deliveryAgent` (set by `DeliveryAgentAuthInterceptor`).

The entity is loaded by the interceptor via `repository.findByMobileNo(mobileNoClaim)`, mirroring the existing flow. The controller does not re-validate the token, does not re-load the entity by id, and does not read any claim manually.

The `{userId}`, `{chefId}`, `{agentId}` path variables used elsewhere in the role APIs are NOT required here — logout is naturally scoped to "the currently authenticated actor". No path-variable-to-attribute comparison is needed (contrast with the chef-search spec, which validates `userId` against the authenticated user).

### 5. Idempotent Logout

Calling logout twice with the same access token does not produce an error.

- The first call adds the access token's hash to the denylist and returns 200.
- The second call, after the interceptor confirms the token is still signature-valid, hits the denylist check first and is rejected with `401 Unauthorized` (the standard invalid-token error). This is the same outcome as any other expired/revoked token and is **not** treated as a server error.
- Equivalently: the client should treat a 401 from a second logout call as a successful prior logout; the API contract treats it identically to any other invalid token. There is no special idempotency layer — the denylist check is the idempotency mechanism.

This is documented in the API contract and in the testing requirements so the client team is not surprised.

### 6. Already-Expired or Invalid Tokens

If the caller's `Authorization: Bearer` header is missing, malformed, expired, signature-invalid, or otherwise fails `JwtUtility.validateToken(...)`, the existing interceptor path produces `401 Unauthorized` with the existing JSON error shape (`{"error":"Invalid or expired token","success":"false"}` for the chef/delivery-agent interceptors; `{"error":"User not found","success":"false"}` when the mobile number on a valid token does not match any user for `UserAuthInterceptor`). The logout controller never executes in those cases, and the standard `ErrorResponse` path in `GlobalExceptionHandler` handles any thrown `UnauthorizedException`/`ResourceNotFoundException`.

There is no "logout an already-expired token" success path — that scenario cannot reach the controller method.

### 7. Validation

The `refreshToken` field, when supplied, is validated by the standard `MethodArgumentNotValidException` path in `GlobalExceptionHandler`:

- `@NotBlank` on `refreshToken` if the JSON object is present but the field is missing/empty (returns 400).
- Maximum length of the field: 2048 characters (the size of a typical HS256 JWT; defensive cap to prevent oversized payloads).

If the request body is entirely absent or empty JSON `{}`, the implementation accepts the request and only invalidates the access token. This is the common case for "logout from this device" where the client may not hold the refresh token.

### 8. Successful Response

```json
{
  "message": "Logged out successfully",
  "success": true,
  "data": null
}
```

`ApiResponse` uses `@JsonInclude(JsonInclude.Include.NON_NULL)`, so `data` is omitted from the JSON when `null`. The HTTP status code is `200 OK`.

The `message` text is identical across all three actor types (logout is the same operation, just scoped to a role).

### 9. Error Responses

All error paths use the existing `ErrorResponse` envelope via `GlobalExceptionHandler`:

- 401 Unauthorized — `UnauthorizedException` thrown when the interceptor-detected token is missing/invalid/denylisted, or when `/refresh` receives a denylisted refresh token.
- 400 Bad Request — `MethodArgumentNotValidException` for malformed bodies (oversized `refreshToken`, blank when required), or `HttpMessageNotReadableException` for unparseable JSON.
- 500 Internal Server Error — unhandled failure in the denylist write. The generic `@ExceptionHandler(Exception.class)` is currently commented out in `GlobalExceptionHandler` (per the CLAUDE.md gotcha), so unexpected failures fall through to Spring's default handling. The implementation logs the failure via `@Slf4j` at ERROR level and re-throws as a `RuntimeException` so the existing default handling produces a 500 with the standard Spring Boot error shape. (See Open Questions.)

Example error responses:

```json
{
  "success": false,
  "message": "Unauthorized",
  "errors": "Invalid or expired token",
  "timestamp": "2026-08-25T10:30:00"
}
```

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": { "refreshToken": "must not be blank" },
  "timestamp": "2026-08-25T10:30:00"
}
```

## API Contract

### Request

```http
POST /api/users/logout
POST /api/chefs/logout
POST /api/delivery-agents/logout
Authorization: Bearer <accessToken>
Content-Type: application/json (optional)
```

Optional body:

```json
{
  "refreshToken": "<refresh-jwt>"
}
```

- `refreshToken` (String, optional, max 2048 chars) — when provided, it is also denylisted.

No path variables.

### Successful Response

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "message": "Logged out successfully",
  "success": true,
  "data": null
}
```

### Error Responses

Use the existing API error format (`ErrorResponse`):

- 401 — invalid/expired/denylisted access token, or denylisted refresh token at `/refresh`.
- 400 — malformed body, missing/blank required field, oversized `refreshToken`.
- 500 — unexpected server failure (see Open Questions).

## Database Requirements

No database schema changes are required for this feature.

- The denylist lives entirely in Redis.
- No new JPA entities, no migrations, no schema changes.
- No additions to existing entities (no `tokenVersion` column, no per-user refresh-token persistence).

The implementation reuses the existing `RedisConfig` (`StringRedisTemplate` bean) and the existing Redis instance (host/port from `spring.data.redis.*`).

## API Architecture

The implementation follows the application's existing layering:

```
Controller (UserController.logout / ChefController.logout / DeliveryAgentController.logout)
        ↓
Request Validation (Jakarta @Valid on LogoutRequest DTO)
        ↓
Service (UserService.handleLogout / ChefService.handleLogout / DeliveryAgentService.handleLogout)
        ↓
Redis (StringRedisTemplate.opsForValue().set(...) with TTL)
        ↓
Response (ApiResponse.success)
```

Existing functionality is reused:

- Three role-scoped `*AuthInterceptor`s (extended with a denylist check)
- `utilities/JwtUtility` (`extractAllClaims` for `exp`)
- `config/RedisConfig` (`StringRedisTemplate`)
- `dto/responses/ApiResponse` (success envelope)
- `dto/responses/ErrorResponse` (error envelope)
- `exception/GlobalExceptionHandler` (existing exception handlers)
- `exception/UnauthorizedException` (401 path)
- Lombok + MapStruct conventions, `@Slf4j` logging, `@Transactional` on the write flow in the service layer

## Non-Functional Requirements

### Performance

- Logout executes a fixed number of Redis operations: one `SET` (with TTL) per token being denylisted — at most 2 (access + optional refresh).
- The denylist check inside the interceptor is a single `EXISTS` Redis call (a single round-trip; sub-millisecond on a local Redis).
- Logout does not perform any database queries against Postgres.
- Logout does not load any entity beyond what the interceptor already loads.
- The Redis TTL matches the token's natural expiry, so the denylist self-prunes and does not grow unbounded.

### Scalability

- Redis can sustain millions of denylist keys per day; with TTL-based pruning, the steady-state set is bounded by the number of currently-outstanding but-revoked tokens.
- All three actor types share the same Redis namespace, so scale characteristics are uniform.
- The implementation does not introduce any synchronous Postgres writes or locks.

### Reliability

- A Redis outage during logout produces a 500-class error response (logged at ERROR). The user's session is NOT considered logged out — they may still need to retry. Documented as a known operational dependency.
- The denylist check inside the interceptor is best-effort: if Redis is unreachable on the read path, the implementation falls back to the existing behavior (allow request through) and logs a WARN. This is consistent with the existing interceptor style (which does not fault on transient infrastructure failures). Documented trade-off.
- TTL on the denylist keys ensures eventual consistency even if explicit deletion is missed.

### Security

- The denylist key is `SHA-256(token)` hex-encoded; the raw token is never stored in Redis.
- Only the hash leaves the request handler — Redis cannot leak plaintext tokens even to operators with read access.
- The interceptor enforces authentication BEFORE the controller runs, so unauthenticated callers cannot enqueue arbitrary denylist entries.
- The `{role}/logout` path is added to the interceptor's `addPathPatterns` (i.e., it is NOT in `excludePathPatterns`). This prevents logout from being invoked by anonymous callers.
- The denylist entry is keyed by token, not by user — a single user can have multiple denylisted tokens (one per device) without any cross-device effect.
- The `/refresh` extension prevents token-lifetime extension after the refresh token has been denylisted.
- HTTPS for the logout endpoint is enforced at the deployment layer (the existing API is HTTPS-only in production via the reverse proxy).
- No sensitive actor data is logged at INFO/WARN; only the role type and token-hash prefix are logged at DEBUG.

### Observability

- `@Slf4j` is used for all log statements.
- Logout success logs at INFO: `Logout successful role=<user|chef|deliveryAgent> actorId=<uuid>`.
- Logout failures log at WARN/ERROR with the same prefix and the failure reason.
- The denylist hit path logs at INFO: `Denylist hit role=<role> actorId=<uuid>`.
- The `/refresh` denylist rejection logs at INFO: `Refresh token denylisted actorId=<uuid>`.
- Sensitive material (the raw token, the full hash) is never logged.

### API Consistency

The logout endpoints follow existing API conventions for:

- HTTP methods (POST for state-changing operations, matching `/otp/verify` and `/refresh`)
- URL structure (`/api/{role}/logout`)
- Authentication (Bearer token via the role-specific `*AuthInterceptor`)
- Request validation (Jakarta validation on `LogoutRequest` DTO)
- Response envelopes (`ApiResponse` for success, `ErrorResponse` for errors)
- Status codes (200, 400, 401, 500)
- Serialization (Jackson with `JsonInclude.NON_NULL`)
- Lombok DTO style (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor`, not Java records, matching `OtpVerifyRequest`/`OtpGenerationRequest`)
- Service method naming (`handle<Action>` matching `handleOtpVerification`/`handleRefresh`)
- MapStruct-style separation: controller → service → repository/Redis (no business logic in the controller)

## Reusability

Prefer existing:

- Auth interceptors (`UserAuthInterceptor`, `ChefAuthInterceptor`, `DeliveryAgentInterceptor`) — extended with the denylist check
- `utilities/JwtUtility` (`extractAllClaims(token)` to read `exp`)
- `config/RedisConfig` (`StringRedisTemplate`)
- `dto/responses/ApiResponse`, `dto/responses/ErrorResponse`
- `exception/GlobalExceptionHandler`
- `exception/UnauthorizedException`, `exception/BadRequestException`
- `dto/requests/common/RefreshTokenRequest` style for the new `LogoutRequest` DTO
- Existing controller patterns (`@RequestAttribute` for the auth entity, `@Valid` on request DTOs)

Avoids duplicate implementations. No new utilities, no new exceptions, no new envelope types.

## Testing Requirements

### Unit Tests

Add tests covering:

- `SHA-256` hash derivation is deterministic for the same token
- Denylist key format is `auth:denylist:access:{sha256Hex}` for access tokens and `auth:denylist:refresh:{sha256Hex}` for refresh tokens
- TTL on the access denylist entry equals `exp - now` (in seconds)
- TTL on the refresh denylist entry equals the refresh-token lifetime (7 days)
- `handleLogout` is called with the auth entity from the request attribute
- `handleLogout` returns `ApiResponse` with `success=true` and the expected `message`
- Missing/blank `refreshToken` does NOT raise an exception — only the access token is denylisted
- A Redis write failure is logged at ERROR and re-thrown as `RuntimeException`

### Integration / API Tests

Add API-level tests covering:

- 200 OK on valid access token with empty body
- 200 OK on valid access token with a valid `refreshToken` (both tokens denylisted)
- 401 on missing `Authorization` header
- 401 on malformed bearer token (signature invalid, expired)
- 401 on a token whose hash is already in the denylist (idempotency behavior — second logout call)
- 401 on `/refresh` after the refresh token has been denylisted (no new tokens minted)
- 400 on JSON body that violates `LogoutRequest` validation (blank `refreshToken` if sent, oversized `refreshToken`)
- 400 on unparseable JSON body
- 200 on empty body `{}` (no refresh token supplied)
- 200 on no body at all

### Idempotency Tests

Verify that:

- Calling `/api/{role}/logout` twice with the same access token yields 200 then 401, NOT 200 then 500.
- After logout, subsequent calls to any role-scoped endpoint with the same access token yield 401.
- After logout with a refresh token supplied, calling `/refresh` with that refresh token yields 401.

### Security Tests

Verify that:

- Logout without a valid bearer token returns 401 (not 200) and adds nothing to the denylist.
- The raw access token never appears in Redis (only its SHA-256 hash).
- The denylist key is correctly namespaced per token type (access vs refresh).
- The denylist entry expires at the right time (TTL is within ±1 second of the token's natural expiry).

## Out of Scope

The following are NOT part of this backend feature unless required by the existing project architecture:

- Cross-device logout ("log out everywhere")
- Server-side persistence of refresh tokens (the implementation remains stateless apart from the denylist)
- Session-cookie auth (the API continues to use bearer tokens only)
- Logout via a "session" abstraction layered on top of JWT
- Login-throttling / brute-force protection (existing OTP flow is unchanged)
- A webhooks/push-notification to inform the client that a session was ended elsewhere
- Per-user token-version counters (the denylist alone is sufficient for the stated requirement)
- Schema changes to `UserEntity`, `ChefEntity`, `DeliveryAgentEntity`
- A separate "force-logout" admin endpoint
- Audit logging of logout events beyond the standard `@Slf4j` INFO/WARN entries
- A new exception type (the implementation reuses `UnauthorizedException`)

If any of these capabilities already exist in the backend, the implementation reuses them where appropriate rather than duplicating functionality.

## Acceptance Criteria

### API

- [ ] `POST /api/users/logout` exists and is protected by `UserAuthInterceptor`
- [ ] `POST /api/chefs/logout` exists and is protected by `ChefAuthInterceptor`
- [ ] `POST /api/delivery-agents/logout` exists and is protected by `DeliveryAgentAuthInterceptor`
- [ ] All three endpoints follow the existing API conventions (`ApiResponse`, `ErrorResponse`, Lombok DTOs)
- [ ] All three endpoints are wrapped in `@RequestAttribute`-style auth (the controller does not parse the token)
- [ ] No `/api/{role}/logout` path appears in any interceptor's `excludePathPatterns`

### Access-Token Invalidation

- [ ] The access token is denylisted in Redis on successful logout
- [ ] The denylist key is `SHA-256(token)` hex-encoded (raw token never stored)
- [ ] The TTL on the access denylist entry equals the remaining access-token lifetime
- [ ] All three `*AuthInterceptor`s reject access tokens whose hash is in the denylist with 401

### Refresh-Token Invalidation

- [ ] When `refreshToken` is supplied, it is denylisted in Redis with a 7-day TTL
- [ ] When `refreshToken` is absent or blank, no refresh-token denylist entry is written and no error is raised
- [ ] `POST /api/{role}/refresh` rejects a refresh token whose hash is in the denylist with 401

### Idempotency

- [ ] Calling logout with an already-denylisted token returns 401 (the standard invalid-token error), not 500
- [ ] Repeated logout calls do not produce new errors beyond the standard 401 on the second call

### Validation and Errors

- [ ] Missing/malformed `Authorization` header returns 401
- [ ] Invalid/expired token returns 401
- [ ] Malformed JSON body returns 400
- [ ] Blank or oversized `refreshToken` returns 400
- [ ] No internal implementation details are exposed in error responses
- [ ] Errors use the existing `ErrorResponse` shape

### Performance and Scalability

- [ ] Logout does not load or query Postgres
- [ ] Logout executes a constant number of Redis operations per call (max 2 SETs)
- [ ] Denylist entries self-expire via TTL (no background cleanup required)
- [ ] No new infrastructure dependencies beyond the existing Redis instance

### Security

- [ ] Logout requires a valid bearer token (anonymous callers cannot trigger denylist writes)
- [ ] The raw access token never leaves the request handler — only its SHA-256 hash is stored
- [ ] Denylist keys are namespaced by token type (`access` vs `refresh`)
- [ ] `/refresh` cannot be used to extend a denylisted refresh token's lifetime
- [ ] No sensitive material (raw token, full hash, actor secrets) is logged

### Testing

- [ ] Unit tests cover hash derivation, key format, TTL, idempotency, validation, and error paths
- [ ] API/integration tests cover all three endpoints
- [ ] Idempotency is verified end-to-end
- [ ] `/refresh` post-logout behavior is verified
- [ ] Security tests confirm raw tokens are never stored and anonymous callers cannot enqueue denylist entries

## Open Questions

- **Generic 500 handler:** `GlobalExceptionHandler` has the `@ExceptionHandler(Exception.class)` commented out (CLAUDE.md gotcha). Should an unhandled Redis outage during logout be wrapped in a new dedicated exception (e.g., `LogoutException`) and handled with 500 + `ErrorResponse`, or should it continue to surface via Spring's default 500 handler? Recommendation: add a dedicated exception handler for clarity and to match the rest of the `ErrorResponse`-based contract.
- **Read-path failure mode:** If the Redis `EXISTS` check inside the interceptor fails (e.g., Redis is down), should the request be allowed through (fail-open) or rejected (fail-closed)? Recommendation: fail-open with a WARN log, matching the existing interceptor style (which does not currently fault on infrastructure failures). Documented trade-off in Non-Functional Requirements.
- **Refresh-token optionality:** Is omitting the `refreshToken` field on logout acceptable? Some clients (single-device apps) may not retain the refresh token between sessions. Recommendation: yes, accept empty body; documented in the API contract.
- **Cross-actor denylist collision:** The denylist namespace (`auth:denylist:access:{sha256Hex}`) is shared across all three actor types. Two actors generating the same JWT (same claims, same secret) would produce the same hash and collide. In practice, the JWT subject is a per-actor UUID and the issued-at timestamp differs, so collisions are astronomically unlikely, but the spec notes this. Recommendation: namespace per role (`auth:denylist:user:access:...`, etc.) for defense-in-depth if the team prefers.
- **Existing `application.properties` mismatch:** `security.jwt.expiration-time=36000` (10h) in `application.properties` is unused by `UserService`/`ChefService`, which hard-code 3h. Should the denylist TTL for the access token use the JWT's actual `exp` claim (recommended), or read from `security.jwt.expiration-time`? Recommendation: read `exp` from the claim — that is the source of truth for the actual token being denylisted.
- **Existing unused signup paths:** `WebConfig.excludePathPatterns` lists `/api/users/signup` and `/api/chefs/signup` but no controllers handle those paths. This is a pre-existing gap, not in scope for logout.

## References

- `.claude/CLAUDE.md` — architecture overview, interceptor-based auth, response envelopes
- `.claude/specs/01-chef-search-functionality.md` — spec format and depth reference
- `utilities/JwtUtility.java` — current JWT generation/validation methods (no `jti`)
- `interceptors/UserAuthInterceptor.java`, `interceptors/ChefAuthInterceptor.java`, `interceptors/DeliveryAgentAuthInterceptor.java` — to be extended with denylist check
- `config/WebConfig.java` — interceptor registration; new logout paths must be added to `addPathPatterns` (already covered by `/api/{role}/**`)
- `config/RedisConfig.java` — `StringRedisTemplate` bean for denylist writes
- `controller/UserController.java`, `controller/ChefController.java`, `controller/DeliveryAgentController.java` — endpoints to extend with `logout`
- `dto/responses/ApiResponse.java`, `dto/responses/ErrorResponse.java` — response envelopes
- `exception/GlobalExceptionHandler.java` — error handling
- `exception/UnauthorizedException.java` — 401 exception
- `dto/requests/common/RefreshTokenRequest.java` — DTO style reference for the new `LogoutRequest`