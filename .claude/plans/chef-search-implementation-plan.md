# Chef Search Functionality — Implementation Plan

## Context

Customers today can only browse chefs via `UserMenuService.handleGetMenusForCustomers` (`MenuRepository.findAllByDistance`), which returns menus with embedded chef info — there is **no chef-level search endpoint**. Adding search lets users type-ahead to find a specific chef by name or address.

Spec: `.claude/specs/01-chef-search-functionality.md`

**Endpoint:** `GET /api/users/{userId}/chefs/search?q=<query>&page=1&limit=20`

**Ranking:** exact name → prefix name → substring name → address-only (alphabetical ties within bucket).

**Visibility filters (per spec):**
- User has a location set
- Chef within 5km (`ST_DWithin(c.location, u.location, 5000)`)
- Chef has ≥1 active menu (`EXISTS menus WHERE is_active`)
- Chef has ≥1 active meal plan with capacity (`EXISTS meal_plans WHERE is_active AND remaining_capacity > 0`)

**Auth:** `UserAuthInterceptor` already covers `/api/users/**` (registered in `WebConfig`). The service must verify the `userId` path-var matches the authenticated user from `@RequestAttribute("user")` — the interceptor loads the entity but does not compare.

**No DB changes** (no `pg_trgm`/indexes — existing GIST on `location` + B-tree on `name`/`address` are sufficient; re-evaluate with `EXPLAIN ANALYZE` later if slow).

---

## Architecture

```
UserChefSearchController  (@RequestParam + @Validated)
        ↓
UserChefSearchService     (escape, userId check, manual projection → response)
        ↓
ChefRepository            (2 new native queries, joins + EXISTS subqueries)
        ↓
PostgreSQL/PostGIS        (single native query per call)
        ↓
ChefResponse (reused) + PaginationResponse (new)
        ↓
ApiResponse { data: Map.of("chefs", ..., "pagination", ...) }
```

---

## Files to Create / Modify

### 1. NEW `src/main/java/com/example/tds/dto/responses/PaginationResponse.java`

```java
package com.example.tds.dto.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginationResponse {
    private int page;
    private int limit;
    private long total;
    private int totalPages;
}
```

`long total` because the `COUNT(*)` returns `long`. `@JsonInclude(NON_NULL)` matches every other response DTO in this codebase.

### 2. NEW `src/main/java/com/example/tds/projection/ChefSearchProjection.java`

```java
package com.example.tds.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ChefSearchProjection {
    UUID getChefId();
    String getName();
    String getAddress();
    String getMobileNo();
    String getAvatarUrl();
    Double getRating();
    Double getLongitude();
    Double getLatitude();
    Double getDisInKm();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
    Integer getNameMatchPriority();
}
```

Interface-projection style matches `MenuWithDistanceProjection`, `OrderForChefProjection`, etc.

### 3. MODIFY `src/main/java/com/example/tds/repository/ChefRepository.java`

Add two native queries. **`EXISTS` subqueries** are used for the menu/meal_plan visibility gates because a chef with N active menus and M active meal plans would otherwise appear N×M times in a raw join. EXISTS keeps each chef row unique.

```java
@Query(value = """
    SELECT
        c.chef_id                          AS "chefId",
        c.name                             AS "name",
        c.address                          AS "address",
        c.mobile_no                        AS "mobileNo",
        c.avatar_url                       AS "avatarUrl",
        c.rating                           AS "rating",
        ST_X(c.location::geometry)         AS "longitude",
        ST_Y(c.location::geometry)         AS "latitude",
        ROUND((ST_Distance(c.location::geography, u.location::geography) / 1000.0)::numeric, 2)
                                        AS "disInKm",
        c.created_at                       AS "createdAt",
        c.updated_at                       AS "updatedAt",
        CASE
            WHEN c.name = :trimmedQuery                         THEN 1
            WHEN c.name ILIKE :escapedPrefix  ESCAPE '\\'       THEN 2
            WHEN c.name ILIKE :escapedSubstring ESCAPE '\\'    THEN 3
            ELSE 4
        END AS "nameMatchPriority"
    FROM chefs c
    JOIN users u ON u.user_id = :userId
    WHERE u.location IS NOT NULL
      AND ST_DWithin(c.location, u.location, 5000)
      AND (
            c.name    ILIKE :escapedSubstring ESCAPE '\\'
         OR c.address ILIKE :escapedSubstring ESCAPE '\\'
      )
      AND EXISTS (
            SELECT 1 FROM menus m
            WHERE m.chef_id = c.chef_id AND m.is_active = TRUE
      )
      AND EXISTS (
            SELECT 1 FROM meal_plans mp
            WHERE mp.chef_id = c.chef_id
              AND mp.is_active = TRUE
              AND mp.remaining_capacity > 0
      )
    ORDER BY "nameMatchPriority" ASC, c.name ASC, c.address ASC
    LIMIT :limit OFFSET :offset
""", nativeQuery = true)
List<ChefSearchProjection> searchChefs(
    @Param("userId") UUID userId,
    @Param("trimmedQuery") String trimmedQuery,
    @Param("escapedPrefix") String escapedPrefix,
    @Param("escapedSubstring") String escapedSubstring,
    @Param("limit") int limit,
    @Param("offset") int offset
);

@Query(value = """
    SELECT COUNT(*)
    FROM chefs c
    JOIN users u ON u.user_id = :userId
    WHERE u.location IS NOT NULL
      AND ST_DWithin(c.location, u.location, 5000)
      AND (
            c.name    ILIKE :escapedSubstring ESCAPE '\\'
         OR c.address ILIKE :escapedSubstring ESCAPE '\\'
      )
      AND EXISTS (
            SELECT 1 FROM menus m
            WHERE m.chef_id = c.chef_id AND m.is_active = TRUE
      )
      AND EXISTS (
            SELECT 1 FROM meal_plans mp
            WHERE mp.chef_id = c.chef_id
              AND mp.is_active = TRUE
              AND mp.remaining_capacity > 0
      )
""", nativeQuery = true)
long countSearchChefs(
    @Param("userId") UUID userId,
    @Param("escapedSubstring") String escapedSubstring
);
```

**Conventions matched:** `@Param`-bound named parameters (no string interpolation); 5km radius hardcoded (matches `MenuRepository.findAllByDistance`); `disInKm` formula copied verbatim from that query.

### 4. NEW `src/main/java/com/example/tds/service/UserChefSearchService.java`

Manual projection→response mapping (mirrors `UserMenuService.handleGetMenusForCustomers` — not MapStruct, because `ChefMapper` only knows `ChefEntity` and the codebase convention is manual mapping for projection→response).

```java
package com.example.tds.service;

import com.example.tds.dto.responses.ChefResponse;
import com.example.tds.dto.responses.PaginationResponse;
import com.example.tds.entity.UserEntity;
import com.example.tds.exception.UnauthorizedException;
import com.example.tds.projection.ChefSearchProjection;
import com.example.tds.repository.ChefRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserChefSearchService {

    private static final int DEFAULT_PAGE  = 1;
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT     = 50;

    private final ChefRepository chefRepository;

    public Map<String, Object> handleSearchChefs(
            UUID userId, UserEntity currentUser, String q, Integer page, Integer limit
    ) {
        if (currentUser == null || !userId.equals(currentUser.getId())) {
            throw new UnauthorizedException("User not authorized");
        }

        int p = (page == null) ? DEFAULT_PAGE : Math.max(1, page);
        int l = (limit == null) ? DEFAULT_LIMIT
                                 : Math.min(MAX_LIMIT, Math.max(1, limit));
        int offset = (p - 1) * l;

        String trimmed = q.trim();
        String escapedSubstring = "%" + escapeLike(trimmed) + "%";
        String escapedPrefix    = escapeLike(trimmed) + "%";

        long total = chefRepository.countSearchChefs(userId, escapedSubstring);
        List<ChefSearchProjection> rows = chefRepository.searchChefs(
                userId, trimmed, escapedPrefix, escapedSubstring, l, offset
        );

        List<ChefResponse> chefs = rows.stream().map(this::toChefResponse).toList();

        int totalPages = (total == 0) ? 0 : (int) Math.ceil((double) total / l);
        PaginationResponse pagination = PaginationResponse.builder()
                .page(p).limit(l).total(total).totalPages(totalPages)
                .build();

        return Map.of("chefs", chefs, "pagination", pagination);
    }

    private ChefResponse toChefResponse(ChefSearchProjection p) {
        return ChefResponse.builder()
                .chefId(p.getChefId())
                .name(p.getName())
                .address(p.getAddress())
                .mobileNo(p.getMobileNo())
                .avatarUrl(p.getAvatarUrl())
                .rating(p.getRating())
                .longitude(p.getLongitude())
                .latitude(p.getLatitude())
                .disInKm(p.getDisInKm())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    /** Escape LIKE wildcards. Backslash must be escaped FIRST to avoid double-escaping. */
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
```

### 5. NEW `src/main/java/com/example/tds/controller/UserChefSearchController.java`

`@Validated` on the class is what makes method-level constraint annotations on `@RequestParam` actually fire (handled by `MethodValidationInterceptor`). The codebase has no `@ModelAttribute` precedent, so we extend the existing `@RequestParam` pattern instead.

```java
package com.example.tds.controller;

import com.example.tds.dto.responses.ApiResponse;
import com.example.tds.entity.UserEntity;
import com.example.tds.service.UserChefSearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/chefs")
@RequiredArgsConstructor
@Validated
public class UserChefSearchController {

    private final UserChefSearchService userChefSearchService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchChefs(
            @PathVariable("userId") UUID userId,
            @RequestAttribute("user") UserEntity currentUser,
            @RequestParam("q")
            @NotBlank(message = "Search query is required")
            @Size(max = 100, message = "Search query must not exceed 100 characters")
            String q,
            @RequestParam(value = "page", required = false, defaultValue = "1")
            @Min(value = 1, message = "page must be at least 1")
            Integer page,
            @RequestParam(value = "limit", required = false, defaultValue = "20")
            @Min(value = 1, message = "limit must be at least 1")
            @Max(value = 50, message = "limit must not exceed 50")
            Integer limit
    ) {
        Map<String, Object> data = userChefSearchService.handleSearchChefs(
                userId, currentUser, q, page, limit
        );

        boolean hasResults = !((List<?>) data.get("chefs")).isEmpty();
        String message = hasResults ? "Chefs found successfully" : "No chefs found";

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message(message)
                        .success(true)
                        .data(data)
                        .build()
        );
    }
}
```

### 6. MODIFY `src/main/java/com/example/tds/exception/GlobalExceptionHandler.java`

Add two new handlers. The existing handler covers `MethodArgumentNotValidException` (used for `@RequestBody`), but `@RequestParam + @Validated` throws **`ConstraintViolationException`** instead. Missing-param responses default to Spring's generic shape and need a custom handler to match the spec's `ErrorResponse` envelope.

```java
@ExceptionHandler(ConstraintViolationException.class)
public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
    Map<Object, Object> errors = new HashMap<>();
    ex.getConstraintViolations().forEach(cv -> {
        String path = cv.getPropertyPath().toString();   // "searchChefs.q"
        String field = path.contains(".")
                ? path.substring(path.lastIndexOf('.') + 1)
                : path;
        errors.put(field, cv.getMessage());
    });
    ErrorResponse body = ErrorResponse.builder()
            .success(false).message("Validation failed")
            .errors(errors).timestamp(LocalDateTime.now())
            .build();
    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
}

@ExceptionHandler(MissingServletRequestParameterException.class)
public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
    Map<Object, Object> errors = new HashMap<>();
    errors.put(ex.getParameterName(), "Search query is required");
    ErrorResponse body = ErrorResponse.builder()
            .success(false).message("Validation failed")
            .errors(errors).timestamp(LocalDateTime.now())
            .build();
    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
}
```

**New imports:** `jakarta.validation.ConstraintViolationException`, `org.springframework.web.bind.MissingServletRequestParameterException`.

---

## Reused Components

| Component | Path | Notes |
|---|---|---|
| `UserAuthInterceptor` | `interceptors/` | Sets `request.attribute("user")`; `/api/users/**` registered in `WebConfig` |
| `ChefResponse` | `dto/responses/` | Already has every field needed (`chefId, name, address, mobileNo, avatarUrl, rating, longitude, latitude, disInKm, createdAt, updatedAt`) |
| `ApiResponse` / `ErrorResponse` | `dto/responses/` | Existing envelopes |
| `MenuRepository.findAllByDistance` SQL | `repository/MenuRepository.java` | Borrowed `ST_DWithin` + `disInKm` formula verbatim |
| `UserMenuService.handleGetMenusForCustomers` | `service/UserMenuService.java` | Reference for manual projection→response mapping pattern |
| `UnauthorizedException` | `exception/` | Mapped to 401 by existing handler |

---

## Out of Scope

- **Tests:** codebase only has `contextLoads()`; no test infra to extend. Adding JUnit fixtures is a separate effort.
- **DB indexes / `pg_trgm`:** no precedent in `sqlScripts/`. Re-evaluate with `EXPLAIN ANALYZE` once real data is loaded.
- **`ChefMapper` projection mapping:** not added — codebase convention is manual mapping for projections.
- **Spec items explicitly out of scope:** cuisine/price/rating filters, search history, autocomplete, recommendations.

---

## Implementation Order

1. `PaginationResponse` (pure DTO)
2. `ChefSearchProjection` (pure interface)
3. `ChefRepository.searchChefs` + `countSearchChefs` (compile-checked against projection + `long`)
4. `UserChefSearchService` (wires it all together)
5. `UserChefSearchController` (`@RequestParam` + `@Validated`)
6. `GlobalExceptionHandler` — add the two new `@ExceptionHandler` methods

---

## Verification

1. **Compile:** `./mvnw clean compile`
2. **Run:** `./mvnw spring-boot:run` (requires `.env` with DB credentials)
3. **Manual API tests** with a Bearer token from the OTP flow:

   ```bash
   # Valid search — 200, ranked results
   curl "http://localhost:8000/api/users/{userId}/chefs/search?q=John&page=1&limit=20" \
        -H "Authorization: Bearer <access_token>"

   # Empty q — 400, { errors: { q: "Search query is required" } }
   curl "http://localhost:8000/api/users/{userId}/chefs/search?q=" -H "Authorization: Bearer <token>"

   # Whitespace-only q — 400 (NotBlank catches this)

   # q missing entirely — 400 (MissingServletRequestParameterException handler)
   curl "http://localhost:8000/api/users/{userId}/chefs/search" -H "Authorization: Bearer <token>"

   # q > 100 chars — 400

   # limit=51 — 400 (over max)

   # userId mismatch (token for a different user) — 401 UnauthorizedException
   ```

4. **Ranking regression** — seed these chefs and verify order for `q=John`:
   - A: "John Smith" → priority 1 (exact)
   - B: "Johnny Brown" → priority 2 (prefix)
   - C: "Mary Johnson" → priority 3 (substring)
   - D: "Jane Doe" at "123 John Street" → priority 4 (address only)

   Expected response order: **A → B → C → D**.

5. **Visibility regression** — chefs with no active menu, no active meal plan, no remaining capacity, or >5km away must NOT appear, regardless of name match.