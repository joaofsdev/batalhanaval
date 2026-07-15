# Restrict Swagger UI to Dev Profile Only

## Objective

Disable Swagger UI and OpenAPI docs in production while ensuring they remain fully accessible (without authentication) in the dev profile.

## Analysis Summary

The researcher confirmed that Swagger UI access **already works correctly in dev**:
- `/swagger-ui/**` and `/v3/api-docs/**` are already in `permitAll` in SecurityConfig.java.
- The JwtAuthenticationFilter passes through requests without a token (never blocks unauthenticated requests).
- No changes to SecurityConfig or JwtAuthenticationFilter are needed for Swagger access to work.

The **actual problem** is a security concern: Swagger is unconditionally exposed in production because:
1. The `springdoc-openapi-starter-webmvc-ui` dependency is always included.
2. The prod profile (`application-prod.yaml`) does not disable springdoc.
3. SecurityConfig permits Swagger paths regardless of active profile.

## Files to touch

- **modify** `backend/src/main/resources/application-prod.yaml` — disable springdoc in production
- **modify** `backend/src/main/java/com/softexpert/batalhanaval_api/config/SecurityConfig.java` — make Swagger permitAll conditional on dev profile (defense-in-depth)

## Steps

1. **Disable springdoc in production profile** (`application-prod.yaml`)
   - Add `springdoc.api-docs.enabled: false` and `springdoc.swagger-ui.enabled: false` to `application-prod.yaml`.
   - This tells springdoc to not register its endpoints at all when the prod profile is active.
   - Before: no springdoc configuration in prod YAML.
   - After: springdoc section with both api-docs and swagger-ui disabled.

2. **Make Swagger permitAll conditional on profile** (`SecurityConfig.java`) — defense-in-depth
   - Inject the active Spring profile (via `@Value("${spring.profiles.active}")` or `Environment`).
   - Only add `.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()` when the active profile is `dev`.
   - In production, these paths will fall through to `.anyRequest().authenticated()`, providing a second layer of protection even if someone accidentally re-enables springdoc.
   - Before: Swagger paths are unconditionally permitted.
   - After: Swagger paths are only permitted when `spring.profiles.active` equals `dev`.
   - **Important:** The `/h2-console/**` permitAll has the same pattern (H2 is only enabled in dev) — optionally make it conditional too for consistency, but this is not strictly required since H2 console won't respond in prod anyway.

3. **No changes needed to JwtAuthenticationFilter**
   - The filter already passes through unauthenticated requests gracefully.
   - Adding `shouldNotFilter()` for Swagger paths is unnecessary — it would only save a few nanoseconds of header-checking per request and adds maintenance burden.
   - No change.

4. **No changes needed to RateLimitFilter**
   - Already scoped to `/api/auth/` paths only — does not interfere with Swagger.
   - No change.

## Verification

1. **Dev profile (Swagger accessible):**
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
   Then verify:
   - `GET http://localhost:8080/swagger-ui/index.html` → HTTP 200 (Swagger UI loads)
   - `GET http://localhost:8080/v3/api-docs` → HTTP 200 (OpenAPI JSON returned)
   - `GET http://localhost:8080/api/games/fleet-config` → HTTP 200 (public endpoint still works)
   - `GET http://localhost:8080/api/admin/users` without token → HTTP 401 (protected endpoint still protected)

2. **Prod profile (Swagger blocked):**
   ```bash
   cd backend
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
   ```
   (Requires DB env vars — alternatively, verify by temporarily setting profile to prod with H2 URL)
   Then verify:
   - `GET http://localhost:8080/swagger-ui/index.html` → HTTP 401 or 404 (not accessible)
   - `GET http://localhost:8080/v3/api-docs` → HTTP 401 or 404 (not accessible)
   - All `/api/auth/**` endpoints still return 200 (no regression on public auth endpoints)

3. **Existing tests:**
   ```bash
   cd backend
   ./mvnw test
   ```
   All existing tests should pass (they run with dev profile by default).

## Rollback

```bash
git checkout -- backend/src/main/resources/application-prod.yaml
git checkout -- backend/src/main/java/com/softexpert/batalhanaval_api/config/SecurityConfig.java
```
