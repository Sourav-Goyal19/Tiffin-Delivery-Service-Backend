# Search Functionality

## Overview

Add endpoint for searching chefs based on customer search queries.

The backend search functionality allows customers (users) to search for chefs using:
- Chef name
- Chef address

Search results are ranked with **chef name matches taking priority over chef address matches**.

The API returns a predictable, consistent response structure following existing conventions and handles empty queries, no-result cases, invalid requests, and server/database errors appropriately.

The implementation integrates with the application's existing API architecture, authentication, database models, validation, error handling, and response conventions.

## Scope

The implementation includes:

- A dedicated API endpoint for chef search under the user API namespace
- Search by chef name and chef address with ranking (name > address)
- Consistent ordering, pagination, input validation, empty/no-result handling
- Standard API error responses via `GlobalExceptionHandler`
- Efficient native SQL querying; reuses `ChefEntity`, `ChefRepository`, `ChefService`, `ChefResponse`, `ChefMapper`, `UserAuthInterceptor`, `ApiResponse`

No new infrastructure; extends existing patterns.

## Functional Requirements

### 1. Search API

Endpoint: `GET /api/users/{userId}/chefs/search?q=John&page=1&limit=20`

- Customer-facing under `/api/users/{userId}/...`
- Auth: `UserAuthInterceptor` (Bearer token)
- Query params: `q` (required), `page`, `limit`
- Returns `ChefResponse` DTOs in `ApiResponse` with ranking (name > address) and pagination

### 2. Search Input

Backend validates `q`: required, trimmed, non-empty, max 100 chars, case-insensitive. Rejects whitespace-only with 400. Parameterized queries prevent injection. Normalization done server-side.

### 3. Search Fields

- **Chef Name** (`chef.name`): highest ranking priority (e.g., `John Smith` > `John Street`)
- **Chef Address** (`chef.address`): lower priority, single text column on `ChefEntity`

### 4. Search Matching

The search supports practical partial matching using PostgreSQL's `ILIKE` operator.

For example, a query `Joh` matches `John Smith`.

Matching behavior:
- **Case sensitivity**: Case-insensitive (`ILIKE`)
- **Partial vs exact**: Partial matching with wildcards (`%query%`)
- **Whitespace normalization**: Server-side trim; internal spaces preserved
- **Unicode handling**: Supported by PostgreSQL
- **Special-character handling**: Escaped for `LIKE` patterns (`%`, `_`, `\`)

The backend uses database-supported search operations (native SQL with `ILIKE`) rather than retrieving the entire chef dataset and filtering in application memory.

### 5. Search Result Ranking

Search results are ranked according to the following priority:

1. **Chef name matches** (higher priority)
2. **Chef address matches** (lower priority)

Chef name matches receive higher priority than address-only matches.

For example, for the query `John`:
1. Chefs where name ILIKE `%John%`
2. Chefs where address ILIKE `%John%` (and name does not match)

Within the same ranking category, results have deterministic ordering:
- Exact name match before partial name match (using `name = query` vs `name ILIKE %query%`)
- Name prefix match before name substring match (using `name ILIKE query%` vs `name ILIKE %query%`)
- Alphabetical chef name ordering (`name ASC`)
- Address ordering where relevant (`address ASC`)

The database/query layer performs ranking via `ORDER BY` with computed priority columns.

### 6. Search Results

The API response returns matching chefs using the existing `ChefResponse` DTO.

Response structure:
```json
{
  "message": "Chefs found successfully",
  "success": true,
  "data": {
    "chefs": [
      {
        "chefId": "uuid",
        "name": "John Smith",
        "address": "123 Main Street",
        "mobileNo": "9876543210",
        "avatarUrl": "https://...",
        "rating": 4.5,
        "longitude": 77.123,
        "latitude": 28.456,
        "disInKm": 2.5,
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-15T10:30:00"
      }
    ],
    "pagination": {
      "page": 1,
      "limit": 20,
      "total": 1,
      "totalPages": 1
    }
  }
}
```

The response follows existing API conventions:
- Wrapped in `ApiResponse`
- `data` contains `chefs` array and `pagination` object
- Uses existing `ChefResponse` DTO (includes `disInKm` for distance from user)

### 7. Empty Search Query

The backend defines explicit behavior for an empty query.

A request such as:
```http
GET /api/users/{userId}/chefs/search?q=
```
or a whitespace-only query returns a validation error (400 Bad Request):
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": { "q": "Search query is required" },
  "timestamp": "2024-01-15T10:30:00"
}
```

The implementation does not accidentally return the entire chef dataset when no search term is provided.

### 8. No Results

When no chefs match the search query, the API returns a successful response containing an empty result collection:

```json
{
  "message": "No chefs found",
  "success": true,
  "data": {
    "chefs": [],
    "pagination": {
      "page": 1,
      "limit": 20,
      "total": 0,
      "totalPages": 0
    }
  }
}
```

The API does not treat a valid search that produces zero results as an error.

The frontend can distinguish:
- Successful search with zero results (`success: true`, empty array)
- Successful search with results (`success: true`, non-empty array)
- Invalid search request (`success: false`, validation errors)
- Server/API failure (`success: false`, error message)

### 9. Pagination

The search API supports pagination following the existing application architecture.

Pagination parameters:
- `page` (default: 1, minimum: 1)
- `limit` (default: 20, minimum: 1, maximum: 50)

The backend enforces a maximum page size of 50 regardless of the value supplied by the client.

Consistent ordering across pages is guaranteed by the deterministic `ORDER BY` clause.

Example:
```http
GET /api/users/{userId}/chefs/search?q=John&page=2&limit=10
```

### 10. Filtering by Chef Availability/Status

Only chefs that are eligible to appear in the customer-facing chef listing are returned.

The search endpoint applies the same visibility rules as the existing customer-facing chef listing API (`UserMenuService.handleGetMenusForCustomers`):
- Chef must have at least one active menu (`m.is_active = true`)
- Chef must have at least one active meal plan with remaining capacity (`mp.is_active = true AND mp.remaining_capacity > 0`)
- Chef must be within 5km of the user (`ST_DWithin(c.location, u.location, 5000)`)
- User must have a location set

Search does not become a way for customers to discover chefs that are otherwise hidden from them.

### 11. Authorization

The search endpoint follows the application's existing authentication and authorization model.

Since chef listings are available only to authenticated customers:
- The search endpoint requires appropriate authentication via `UserAuthInterceptor`
- The endpoint enforces the same customer access rules as existing chef listing APIs
- The `userId` path variable must match the authenticated user (validated in service layer)

Authorization is applied independently of the search query.

### 12. Loading and Request Behavior

The backend is designed to support responsive frontend search behavior.

The API:
- Returns responses efficiently (database-level filtering and ranking)
- Avoids unnecessary database queries (single native query with joins)
- Avoids loading the complete chef dataset into application memory
- Uses appropriate database indexes (GIST on `location`, B-tree on `name`, `address`)
- Avoids repeated queries for the same request
- Respects request cancellation/timeouts according to existing infrastructure

The frontend may debounce requests, but the backend handles repeated search requests safely and efficiently.

### 13. Validation Errors

Invalid search requests return the application's standard validation/error response format (`ErrorResponse`).

Examples:
- Missing required query parameter `q` → 400 Bad Request
- Query exceeds maximum allowed length (100 chars) → 400 Bad Request
- Invalid pagination parameters (negative page, limit > 50) → 400 Bad Request
- User not found / unauthorized → 401 Unauthorized / 404 Not Found

The API returns appropriate HTTP status codes according to existing project conventions.

Validation errors are distinguishable from server errors.

### 14. Server and Database Errors

Unexpected failures are handled through the application's existing error-handling mechanism (`GlobalExceptionHandler`).

The API:
- Returns an appropriate server-error response (500 Internal Server Error, though generic handler is commented out)
- Avoids exposing database internals
- Avoids returning stack traces or sensitive implementation details
- Logs sufficient information for backend debugging
- Preserves the application's standard error response format

A database failure is never represented as an empty successful search result.

## API Contract

### Request

```http
GET /api/users/{userId}/chefs/search?q=<search-query>&page=1&limit=20
```

Path parameters:
- `userId` (UUID, required) - authenticated user's ID

Query parameters:
- `q` (String, required) - search query, 1-100 characters after trim
- `page` (Integer, optional, default: 1) - page number, minimum 1
- `limit` (Integer, optional, default: 20) - results per page, minimum 1, maximum 50

### Successful Response

For matching chefs:
```json
{
  "message": "Chefs found successfully",
  "success": true,
  "data": {
    "chefs": [
      {
        "chefId": "uuid",
        "name": "John Smith",
        "address": "123 Main Street",
        "mobileNo": "9876543210",
        "avatarUrl": "https://...",
        "rating": 4.5,
        "longitude": 77.123,
        "latitude": 28.456,
        "disInKm": 2.5,
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-15T10:30:00"
      }
    ],
    "pagination": {
      "page": 1,
      "limit": 20,
      "total": 1,
      "totalPages": 1
    }
  }
}
```

For no matches:
```json
{
  "message": "No chefs found",
  "success": true,
  "data": {
    "chefs": [],
    "pagination": {
      "page": 1,
      "limit": 20,
      "total": 0,
      "totalPages": 0
    }
  }
}
```

### Validation/Error Response

For an invalid request, use the existing API error format (`ErrorResponse`):

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": { "q": "Search query is required" },
  "timestamp": "2024-01-15T10:30:00"
}
```

The exact error structure and error codes follow the project's existing conventions.

## Database Requirements

### Query Efficiency

Search is executed at the database/query layer using a single native SQL query.

The implementation avoids:
1. Retrieving all chefs
2. Loading them into application memory
3. Filtering and ranking them in application code

The database performs the filtering, joining, and ordering.

The query joins `chefs` → `menus` → `meal_plans` → `users` (for distance) with the same visibility filters as `MenuRepository.findAllByDistance`.

### Indexing

Existing database indexes are reviewed for fields used by search:
- `chefs.name` - B-tree index recommended for `ILIKE` prefix patterns
- `chefs.address` - B-tree index recommended for `ILIKE` prefix patterns
- `chefs.location` - GIST index (already exists for PostGIS)
- `menus.is_active`, `meal_plans.is_active`, `meal_plans.remaining_capacity` - covered by existing query filters

If normal substring matching cannot use existing indexes efficiently, the implementation evaluates PostgreSQL's `pg_trgm` extension for trigram indexes rather than blindly adding indexes that do not improve the query.

Any new index is justified based on the expected query pattern and dataset size.

### Case Sensitivity

Search behavior is consistent regardless of capitalization.

For example:
```
john
John
JOHN
```
produce equivalent results.

The implementation uses PostgreSQL's `ILIKE` operator for case-insensitive search.

### Ranking Implementation

Ranking is performed as part of the database query.

Conceptually:
```
ORDER BY
  name_match_priority ASC,      -- 1 = exact, 2 = prefix, 3 = substring, 4 = address only
  secondary_name_order ASC,     -- name ASC
  secondary_address_order ASC   -- address ASC
```

Exact implementation uses a `CASE` expression in `ORDER BY` to compute priority:
- Priority 1: `name = query` (exact match)
- Priority 2: `name ILIKE query%` (prefix match)
- Priority 3: `name ILIKE %query%` (substring match)
- Priority 4: `address ILIKE %query%` (address match only)

## API Architecture

The implementation follows the application's existing layering:

```
Controller (UserChefSearchController)
        ↓
Request Validation (Jakarta @Valid on SearchRequest DTO)
        ↓
Service (UserChefSearchService)
        ↓
Repository (ChefRepository with native query)
        ↓
Database (PostgreSQL with PostGIS)
        ↓
DTO/Mapper (ChefResponse via ChefMapper)
        ↓
API Response (ApiResponse)
```

Existing chef listing functionality is reused:
- `ChefRepository` extended with search method
- `ChefResponse` DTO reused
- `ChefMapper` reused
- `UserAuthInterceptor` for authentication
- `GlobalExceptionHandler` for errors
- Visibility logic mirrors `MenuRepository.findAllByDistance`

## Non-Functional Requirements

### Performance
- Search queries execute efficiently against the database (single query with joins)
- Avoids loading unnecessary chef records
- Avoids application-level filtering of the entire chef dataset
- Applies pagination to prevent unbounded responses
- Uses appropriate database indexes/search capabilities
- Avoids N+1 queries (all data fetched in single query)
- Avoids unnecessary joins (only joins required for visibility + distance)
- Search remains performant as the number of chefs grows

### Scalability
- The search implementation does not assume the number of chefs will remain small
- The API remains usable as chef count increases, search traffic increases, multiple customers perform searches concurrently
- Pagination and database-level filtering prevent unbounded responses and memory usage

### Reliability
- Handles malformed requests safely (validation)
- Handles empty result sets correctly (success with empty array)
- Handles database failures through standard error handling
- Avoids returning inconsistent result ordering (deterministic ORDER BY)
- Avoids exposing internal implementation details
- Respects existing API timeout and retry behavior

### Security
- Validates and sanitizes input according to the application's security practices
- Uses parameterized queries (native query with `:param` binding)
- Prevents SQL injection
- Respects existing authorization rules (UserAuthInterceptor)
- Avoids exposing private chef information (only returns ChefResponse fields)
- Avoids allowing search parameters to bypass visibility restrictions (same filters as menu listing)
- Applies existing rate limiting/API protection mechanisms where applicable

### Observability
- Uses the application's existing logging infrastructure (`@Slf4j`)
- Captures search endpoint failures, slow search queries, database/query performance issues, validation failures
- Avoids logging sensitive customer or chef information unnecessarily
- Search queries are not logged in plaintext if doing so would conflict with privacy requirements

### API Consistency
The search endpoint follows existing API conventions for:
- HTTP methods (GET)
- URL structure (`/api/users/{userId}/chefs/search`)
- Authentication (Bearer token via UserAuthInterceptor)
- Request validation (Jakarta validation on DTO)
- Response envelopes (ApiResponse)
- Pagination (page/limit with max limit enforcement)
- Error responses (ErrorResponse via GlobalExceptionHandler)
- Status codes (200, 400, 401, 404, 500)
- Serialization (Jackson with JsonInclude.NON_NULL)
- Versioning (none currently, follows existing pattern)

## Reusability

Prefer existing:
- Chef models (`ChefEntity`)
- Chef repositories (`ChefRepository`)
- Query builders (native SQL in repository)
- Search utilities (none existing, but pattern established)
- Pagination utilities (custom implementation following existing patterns)
- Authorization policies (UserAuthInterceptor)
- Visibility scopes (mirrors MenuRepository.findAllByDistance)
- DTOs/serializers (ChefResponse, ChefMapper)
- Error-handling middleware (GlobalExceptionHandler)
- Validation mechanisms (Jakarta validation)

Avoids duplicate implementations.

The existing chef listing query (`MenuRepository.findAllByDistance`) is extended to support search rather than creating an independent implementation with duplicated visibility and authorization logic.

## Testing Requirements

### Unit Tests
Add tests covering:
- Name matching (exact, prefix, substring)
- Address matching
- Case-insensitive matching
- Partial matching
- Name ranking over address ranking
- Deterministic ordering
- Empty query handling (validation error)
- Whitespace-only query handling (validation error)
- No-result searches
- Invalid query validation (too long, missing)
- Pagination behavior
- Visibility/status filtering (only chefs with active menus/meal plans within 5km)

### Integration/API Tests
Add API-level tests covering:
- Valid search requests
- Successful responses with data
- No-result responses
- Invalid requests (missing q, q too long, invalid pagination)
- Authentication/authorization behavior (unauthorized, user not found)
- Pagination (page, limit, max limit enforcement)
- Server/database error handling where practical

### Ranking Tests
Explicitly test cases such as:

Given:
```
Search query: John
```

And:
```
Chef A: John Smith (name exact)
Chef B: Johnny Brown (name prefix)
Chef C: Mary Johnson (name substring)
Chef D: Jane Doe — 123 John Street (address only)
```

The response must ensure that name matches are ranked before address-only matches:
1. Chef A (exact name match)
2. Chef B (prefix name match)
3. Chef C (substring name match)
4. Chef D (address match only)

The exact ordering within name matches follows the defined secondary ranking rules and remains deterministic.

### Security Tests
Verify that:
- Search parameters cannot inject database queries (parameterized queries)
- Unauthorized users cannot access protected chef data (UserAuthInterceptor)
- Hidden/inactive chefs are not returned when they should be excluded (visibility filters)
- Search cannot bypass existing chef visibility rules (same filters as menu listing)

## Out of Scope

The following are not part of this backend feature unless required by the existing project architecture:
- Advanced filters such as cuisine, price, rating, or availability
- Search history
- Recent searches
- Search suggestions/autocomplete
- Voice search
- Personalized ranking
- Search analytics
- Recommendation algorithms
- Full-text search infrastructure unrelated to the current requirement (no Elasticsearch, etc.)
- Changes to chef creation/editing workflows
- Changes to chef data models unless required to support the search
- Changes to the mobile UI

If any of these capabilities already exist in the backend, the implementation reuses them where appropriate rather than duplicating functionality.

## Acceptance Criteria

### API
- [ ] A search API endpoint is available at `/api/users/{userId}/chefs/search` for customer-facing chef search
- [ ] The endpoint follows the application's existing API conventions (ApiResponse, ErrorResponse, UserAuthInterceptor)
- [ ] Customers can search chefs by chef name
- [ ] Customers can search chefs by chef address
- [ ] The API returns the existing `ChefResponse` structure

### Search
- [ ] Search queries are validated (required, max 100 chars, non-whitespace)
- [ ] Leading and trailing whitespace is handled appropriately (trimmed)
- [ ] Case differences do not prevent expected matches (ILIKE)
- [ ] Partial matching works according to the defined search behavior (%query%)
- [ ] Empty queries do not trigger an unrestricted chef query (validation error)
- [ ] Whitespace-only queries are handled appropriately (validation error)

### Ranking
- [ ] Chef name matches are ranked before address-only matches
- [ ] Secondary ordering is deterministic (exact > prefix > substring > alphabetical)
- [ ] The database/query layer performs ranking where practical (ORDER BY with CASE)
- [ ] Ranking behavior is covered by automated tests

### Results
- [ ] Matching chefs are returned successfully
- [ ] No-result searches return a successful empty result collection
- [ ] No-result searches are not treated as server errors
- [ ] Only chefs visible to the customer are returned (active menu, active meal plan with capacity, within 5km)
- [ ] Private or unauthorized chef data is not exposed (only ChefResponse fields)

### Pagination
- [ ] Search results are paginated (page, limit parameters)
- [ ] A maximum page size of 50 is enforced
- [ ] Pagination ordering remains deterministic
- [ ] Pagination follows existing API conventions

### Validation and Errors
- [ ] Invalid search requests return the standard validation response (ErrorResponse, 400)
- [ ] Database/API failures return the standard server-error response
- [ ] Internal database or implementation details are not exposed
- [ ] Search failures are distinguishable from valid zero-result searches

### Performance
- [ ] Search is performed at the database/query layer where practical (single native query)
- [ ] The entire chef dataset is not loaded into application memory for filtering
- [ ] N+1 queries are avoided
- [ ] Appropriate database indexes/search capabilities have been evaluated
- [ ] Search performance is acceptable with a realistic dataset

### Security
- [ ] Search parameters are safely parameterized (native query with :params)
- [ ] SQL/query injection is prevented
- [ ] Existing authentication and authorization rules are enforced (UserAuthInterceptor)
- [ ] Chef visibility restrictions cannot be bypassed through search (same filters as menu listing)
- [ ] Existing rate-limiting/security mechanisms are respected

### Testing
- [ ] Unit tests cover the search and ranking logic
- [ ] API/integration tests cover successful searches
- [ ] API/integration tests cover no-result searches
- [ ] Validation and error cases are tested
- [ ] Authorization and visibility behavior is tested
- [ ] Pagination behavior is tested where applicable
- [ ] Security-related search behavior is tested