# GSD Summary

## 2026-07-14 — Restrict Swagger UI to Dev Profile Only

Implementer: modified 3 files (+20/-4 net lines)
- `backend/src/main/resources/application-prod.yaml` — added springdoc disable config (+5 lines)
- `backend/src/main/java/com/softexpert/batalhanaval_api/config/SecurityConfig.java` — made Swagger/H2 permitAll conditional on dev profile, added @Value for profile injection (+8/-4 lines)
- `backend/src/main/java/com/softexpert/batalhanaval_api/security/JwtAuthenticationFilter.java` — added shouldNotFilter() to skip Swagger paths entirely (+15/-2 lines)

Reviewer: pending
Commit: uncommitted
