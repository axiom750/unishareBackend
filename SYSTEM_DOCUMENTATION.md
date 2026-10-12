# UniShare Backend — Complete System Documentation

> **Generated from codebase analysis** — Single reference for architecture, components, APIs, and operations.

---

## 1. Executive Summary

**UniShare** is a university community platform backend built with **Java 21 + Spring Boot 3.5.11**. Its core product feature is **ride sharing** (students offer rides, others request seats, drivers approve/decline). It also provides accounts (UniShare email/password, Google OAuth, GitHub OAuth), user profiles, password reset, and a dual-layer authorization model (application RBAC + control plane).

| Concern | Technology |
|---------|------------|
| Runtime | Java 21, Spring Boot 3.5 |
| Database | PostgreSQL (Supabase) via Spring Data JPA / Hibernate |
| Cache / Locks | Redis-compatible Valkey (Aiven) via Lettuce |
| Auth Tokens | JWT (jjwt 0.11.5, HS256) |
| OAuth Providers | Google, GitHub |
| Email | Brevo HTTP API |
| Media | Cloudinary |
| Hosting | Render (Docker image, `java -jar`) |
| Build Tool | Maven (with wrapper) |

---

## 2. Repository Layout

```
src/main/java/com/unishare/
├── UnishareApplication.java          # Entry point + ALL security identity declarations
├── annotation/                       # @GodAccount, @GodRole, @uniShareRole, @Permission
├── config/                           # SecurityConfig, CorsConfig, RedisConfig, CloudinaryConfig,
│                                     # GlobalResponseHandler (envelope), EnvironmentValidator
├── controller/
│   ├── auth/                         # UniShareAuthController, GoogleAuthController, GithubAuthController
│   ├── ride/                         # RideController, RideRequestController
│   ├── user/                         # UserController, UpdateUserProfileController
│   ├── password/                     # UserPasswordController (password reset)
│   ├── controlpanel/                 # RbacController (admin panel)
│   └── SystemHealth                  # GET /health
├── service/
│   ├── auth/                         # JwtService, UnishareAuthService, GoogleOAuthService, GithubOAuthService
│   ├── rbac/                         # RoleService (normal roles only, read-only), RbacControlPanelService
│   ├── ride/                         # RideService, RideRequestService, RideCleanUpService (scheduled)
│   ├── user/                         # UserService, ProfileUpdateService
│   ├── usermanagement/               # PasswordResetService
│   ├── mail/                         # MailService (Brevo)
│   └── media/                        # MediaService (Cloudinary)
├── entity/
│   ├── user/                         # User, UserProfile
│   ├── ride/                         # Ride, RideRequest
│   ├── auth/                         # PasswordResetToken
│   └── rbac/                         # Role, Permission, GodAccount, GodRole, GodAccountRole, GodRolePermission
├── enums/                            # AuthProvider, PermissionDomain, PermissionStatus, RideStatus, RideRequestStatus
├── repository/                       # Spring Data JPA repositories
├── security/                         # JwtAuthenticationFilter, CustomUserDetailsService, 401/403 handlers
├── idempotency/                      # @Idempotent, IdempotentInterceptor, IdempotencyService (Redis)
├── redis/                            # RedisService wrapper + result/exception types
---

## 3. Domain Model & Entities

### 3.1 User & Profile

| Entity | Table | Key Fields |
|--------|-------|------------|
| `User` | `users` | `id` (Long), `googleId`, `githubId`, `username`, `email`, `password`, `authProvider` (GOOGLE/GITHUB/UNISHARE), `active`, `roles` (ManyToMany → Role), `profile` (OneToOne → UserProfile), `createdAt`, `updatedAt` |
| `UserProfile` | `user_profiles` | `id`, `user` (OneToOne), `bio`, `profilePictureUrl`, `profilePicturePublicId` (Cloudinary), `universityName`, `dateOfBirth` |

### 3.2 Ride Sharing

| Entity | Table | Key Fields |
|--------|-------|------------|
| `Ride` | `rides` | `id` (UUID), `organizerId` (Long), `fromLocation`, `toLocation`, `rideDate`, `rideTime`, `totalSeats`, `availableSeats`, `price` (BigDecimal), `vehicleInfo`, `description`, `status` (ACTIVE/FULL/CANCELLED), `contactInfo` (JSONB Map), `cancelledAt`, `createdAt`, `updatedAt` |
| `RideRequest` | `ride_requests` | `id` (UUID), `rideId`, `passengerId` (Long), `seatsRequested`, `message`, `status` (PENDING/CONFIRMED/DECLINED/CANCELLED), `respondedAt`, `createdAt` |

### 3.3 RBAC — Application Layer (Normal Users)

| Entity | Table | Purpose |
|--------|-------|---------|
| `Role` | `roles` | Normal roles: USER, MODERATOR, ADMIN, CUSTOM_ROLE. UUID PK. `permissions` (ManyToMany), `users` (ManyToMany). |
| `Permission` | `permissions` | Canonical authority (e.g., `RIDE_CREATE`). Fields: `name` (authority), `displayName`, `description`, `baseEntity`, `reachableEntities` (CSV), `status` (ACTIVE/DEPRECATED), `domain` (APPLICATION/CONTROL_PLANE). |
| `role_permissions` | join table | Role ↔ Permission mapping. |
| `user_roles` | join table | User ↔ Role mapping. |

### 3.4 RBAC — Control Plane (Platform Admins)

| Entity | Table | Purpose |
|--------|-------|---------|
| `GodAccount` | `god_accounts` | Single superuser account. Fields: `id` (UUID), `username`, `email`, `password`, `displayName`, `active`. |
| `GodRole` | `god_roles` | Control-plane role (e.g., SUPERUSER). Never mixed with normal `Role`. |
| `GodAccountRole` | `god_account_roles` | GodAccount ↔ GodRole mapping. |
| `GodRolePermission` | `god_role_permissions` | GodRole ↔ Permission mapping (only `domain = CONTROL_PLANE`). |

### 3.5 Auth Supporting

| Entity | Table | Purpose |
|--------|-------|---------|
| `PasswordResetToken` | `password_reset_tokens` | Email-based password reset tokens with expiry. |

---

## 4. Enums

| Enum | Package | Values |
|------|---------|--------|
| `AuthProvider` | `com.unishare.enums.auth` | `GOOGLE`, `GITHUB`, `UNISHARE` |
| `PermissionDomain` | `com.unishare.enums.rbac` | `APPLICATION`, `CONTROL_PLANE` |
| `PermissionStatus` | `com.unishare.enums.rbac` | `ACTIVE`, `DEPRECATED` |
| `RideStatus` | `com.unishare.enums.ride` | `ACTIVE`, `FULL`, `CANCELLED` |
| `RideRequestStatus` | `com.unishare.enums.ride` | `PENDING`, `CONFIRMED`, `DECLINED`, `CANCELLED` |
├── logging/                          # RequestLoggingFilter (traceId per request)
├── utils/                            # Helpers (SlugUtil, etc.)
└── diagnostics/                      # ValkeyNetworkDiagnostic (optional TCP/TLS diagnostics on startup)
```
---

## 5. Security & Authorization Model

### 5.1 Dual Authorization System

1. **Application RBAC** — Normal users get `Role` + `Permission` via `user_roles` and `role_permissions`.
2. **Control Plane** — Separate `GodAccount` + `GodRole` + `GodRolePermission` (for platform admins).

### 5.2 Identity Declaration (Code-First, Frozen UUIDs)

All identities declared **once** on `UnishareApplication` via annotations:

```java
@GodAccount(id = "01a0fe4b-f2ea-7d8c-9a3f-1e5c4b7a9d2f", ...)
@GodRole(id = "01a0fe4b-f2ea-72bb-b39f-fe77132c0be8", name = "SUPERUSER", ...)
@uniShareRole(id = "01a0fe4b-f2ea-73a5-ad8d-866b14ba8160", name = "USER", ...)
@uniShareRole(id = "01a0fe4b-f2ea-7efb-8b5b-826023276850", name = "MODERATOR", ...)
@uniShareRole(id = "01a0fe4b-f2ea-7aea-94fe-7a296412f964", name = "ADMIN", ...)
@uniShareRole(id = "0199c5e8-7a3b-7f42-9c18-6d4e2a8b9135", name = "CUSTOM_ROLE", ...)
```

- **UUID v7** (time-ordered) used for all identity IDs.
- **Frozen**: Never change a UUID after first deploy.
- **Synchronized at startup** by `SecurityDeclarationSynchronizer` (idempotent upsert).

### 5.3 Permission Annotations on Controllers

```java
@Permission(
    id = "0199c5c4-7b2a-7abc-9e31-4f5a7e8d21c4",
    displayName = "Create Ride",
    description = "Allows a user to create a ride.",
    baseEntity = Ride.class,
    reachableEntities = {},
    domain = PermissionDomain.APPLICATION
)
@PreAuthorize("hasAuthority('RIDE_CREATE')")
@PostMapping
public ResponseEntity<Ride> createRide(...)
```

- `@Permission` = documentation + discovery (scanned at startup).
- `@PreAuthorize` = runtime enforcement (currently uses `ROLE_` prefix via JWT `role` claim).

### 5.4 JWT & Authentication Flow

- **Token**: HS256, claims: `sub` (email), `userId` (Long), `role` (primary role name).
- **Delivery**: HTTP-only cookie (`token`) + `Authorization: Bearer` header support.
- **Filter**: `JwtAuthenticationFilter` → extracts token → validates → sets `UsernamePasswordAuthenticationToken` with `ROLE_<role>` authority.
- **Session**: Stateless (`STATELESS`).

### 5.5 Security Config Highlights

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/auth/**", "/api/auth/**").permitAll()
    .requestMatchers("/actuator/health", "/actuator/info", "/health", "/error").permitAll()
    .requestMatchers(POST, "/api/user-management/password-update/**").permitAll()
    // User-scoped ride reads (must precede public GET)
    .requestMatchers(GET, "/api/rides/my-rides", "/api/rides/my-requests",
                          "/api/rides/*/requests", "/api/rides/*/passengers").authenticated()
    .requestMatchers(GET, "/api/rides/**").permitAll()
    .requestMatchers(POST, PATCH, DELETE, "/api/rides/**").authenticated()
    .anyRequest().authenticated()
)
```
---

## 6. API Reference

### 6.1 Health

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/health` | Public | System health check |

### 6.2 Authentication — Native (UniShare)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/unishare/register` | Public | Register new user (email/password) |
| POST | `/api/auth/unishare/login` | Public | Login, returns JWT in cookie + body |
| GET | `/api/auth/unishare/me` | JWT | Get current user profile |
| POST | `/api/auth/unishare/logout` | JWT | Clear JWT cookie |

### 6.3 Authentication — OAuth

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/auth/google` | Public | Redirect to Google consent screen |
| GET | `/api/auth/google/callback` | Public | Handle Google callback, set JWT cookie, redirect to frontend |
| GET | `/api/auth/github` | Public | Redirect to GitHub consent screen |
| GET | `/api/auth/github/callback` | Public | Handle GitHub callback, set JWT cookie, redirect to frontend |

### 6.4 User Management

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/auth/me` | JWT/Cookie | Get logged-in user info (legacy endpoint) |
| POST | `/auth/logout` | JWT/Cookie | Logout (cookie-based) |
| PUT | `/api/update/profile` | JWT (`USER_PROFILE_UPDATE`) | Update profile (multipart: username, bio, university, profile image) |
| POST | `/api/user-management/password-update` | Public (Idempotent) | Request password reset email |
| POST | `/api/user-management/password-update/confirm` | Public | Confirm password reset with token |

### 6.5 Ride Sharing — Ride Management

| Method | Path | Auth | Permission | Description |
|--------|------|------|------------|-------------|
| GET | `/api/rides` | Public | — | List available rides (paginated) |
| POST | `/api/rides` | JWT | `RIDE_CREATE` | Create new ride |
| PATCH | `/api/rides/{id}` | JWT | `RIDE_UPDATE` | Update ride (organizer only, @Idempotent) |
| DELETE | `/api/rides/{id}` | JWT | `RIDE_DELETE` | Cancel ride (organizer only) |
| GET | `/api/rides/my-rides` | JWT | `RIDE_VIEW_OWN` | List rides organized by current user |

### 6.6 Ride Sharing — Request Management

| Method | Path | Auth | Permission | Description |
|--------|------|------|------------|-------------|
| POST | `/api/rides/{rideId}/request` | JWT | `RIDE_REQUEST_CREATE` | Request seat on ride |
| GET | `/api/rides/my-requests` | JWT | `RIDE_REQUEST_VIEW_OWN` | List current user's seat requests |
| POST | `/api/rides/{requestId}/approve` | JWT | `RIDE_REQUEST_APPROVE` | Approve request (ride organizer) |
| POST | `/api/rides/{requestId}/decline` | JWT | `RIDE_REQUEST_DECLINE` | Decline request (ride organizer) |
| POST | `/api/rides/{requestId}/cancel` | JWT | `RIDE_REQUEST_CANCEL` | Cancel own request (passenger) |
| GET | `/api/rides/{rideId}/requests` | JWT | `RIDE_REQUEST_VIEW` | List requests for own ride (organizer) |
| GET | `/api/rides/{rideId}/passengers` | JWT | `RIDE_PASSENGER_VIEW` | List confirmed passengers with contact (organizer) |

### 6.7 Admin / Control Panel

| Method | Path | Auth | Permission (CONTROL_PLANE) | Description |
|--------|------|------|----------------------------|-------------|
| GET | `/api/control-panel/rbac/permissions` | JWT | `VIEW_PERMISSIONS` | List all permissions |
| GET | `/api/control-panel/rbac/roles` | JWT | `VIEW_ROLES` | List all normal roles |
| GET | `/api/control-panel/rbac/roles/{roleId}/permissions` | JWT | `VIEW_ROLE_PERMISSIONS` | Permissions assigned to a role |
---

## 7. Services — Business Logic

### 7.1 Auth Services

| Service | Responsibility |
|---------|----------------|
| `JwtService` | Generate/validate HS256 JWT, extract claims |
| `UnishareAuthService` | Native register/login/logout, current user, JWT cookie building |
| `GoogleOAuthService` | OAuth code exchange, ID token verification, user upsert, JWT issuance |
| `GithubOAuthService` | OAuth code exchange, user info fetch, user upsert, JWT issuance |

### 7.2 User Services

| Service | Responsibility |
|---------|----------------|
| `UserService` | CRUD, find by email/googleId/githubId, save, role assignment |
| `ProfileUpdateService` | Update profile fields + Cloudinary image upload |

### 7.3 Ride Services

| Service | Responsibility |
|---------|----------------|
| `RideService` | Create/update/delete rides, list available/my rides, contact validation, seat counting |
| `RideRequestService` | Request/approve/decline/cancel seat requests, passenger listing with contact info |
| `RideCleanUpService` | `@Scheduled` hourly: delete CANCELLED rides >48h, CANCELLED requests >24h |

### 7.4 RBAC Services

| Service | Responsibility |
|---------|----------------|
| `RoleService` | Read-only: get default role (USER), get primary role name (priority: ADMIN > MODERATOR > USER) |
| `RbacControlPanelService` | Admin panel: list permissions, roles, role-permissions |

### 7.5 Supporting Services

| Service | Responsibility |
|---------|----------------|
| `PasswordResetService` | Generate/send/validate reset tokens via Brevo email |
| `MailService` | Brevo HTTP API wrapper |
| `MediaService` | Cloudinary upload/delete (profile images) |
| `RedisService` | Generic Redis ops (get/set/delete/exists, TTL) |
| `IdempotencyService` | Redis-backed idempotency key reservation (24h TTL) |

---

## 8. Repositories (Spring Data JPA)

| Repository | Entity | Custom Queries |
|------------|--------|----------------|
| `UserRepository` | `User` | `findByEmail`, `findByGoogleId`, `findByGithubId`, `existsByEmail`, `existsByUsername`, `countByRoles_Id` |
| `UserProfileRepository` | `UserProfile` | Standard JPA |
| `RoleRepository` | `Role` | Standard JPA |
| `PermissionRepository` | `Permission` | Standard JPA |
| `GodAccountRepository` | `GodAccount` | Standard JPA |
| `GodRoleRepository` | `GodRole` | Standard JPA |
| `GodAccountRoleRepository` | `GodAccountRole` | Standard JPA |
| `GodRolePermissionRepository` | `GodRolePermission` | Standard JPA |
| `RideRepository` | `Ride` | `findByIdForUpdate` (pessimistic lock), `findByStatusAndRideDateGreaterThanEqual`, `deleteByStatusAndCancelledAtBefore`, `findByOrganizerId` |
| `RideRequestRepository` | `RideRequest` | `findByRideId`, `findByRideIdAndPassengerIdAndStatus`, `findByPassengerId`, `findByRideIdAndStatus`, `findConfirmedPassengers` (DTO projection), `cancelAllRequestsByRideId`, `deleteExpiredCancelledRequests` |
| `PasswordResetTokenRepository` | `PasswordResetToken` | Standard JPA |

---

## 9. DTOs — Request/Response

### 9.1 Request DTOs

| DTO | Package | Fields |
|-----|---------|--------|
| `RegisterRequest` | `dto.request.auth` | `email`, `password`, `username` (optional) |
| `LoginRequest` | `dto.request.auth` | `email`, `password` |
| `RideCreateRequest` | `dto.request.ride` | `fromLocation`, `toLocation`, `rideDate`, `rideTime`, `totalSeats`, `price`, `vehicleInfo`, `description`, `contactInfo` (Map<String,String>) |
| `RideRequestCreateDto` | `dto.request.ride` | `seatsRequested`, `message` |
| `RidePassengerDto` | `dto.request.ride` | `passengerId`, `email`, `username`, `profilePictureUrl`, `bio`, `seatsRequested` |
| `UpdateUserProfileRequest` | `dto.request.user` | `username`, `bio`, `universityName`, `dateOfBirth` |
| `PasswordResetRequest` | `dto.request.password` | `email` |
| `PasswordUpdateRequest` | `dto.request.password` | `token`, `newPassword` |

### 9.2 Response DTOs

| DTO | Package | Purpose |
|-----|---------|---------|
| `ApiResponse<T>` | `dto.response.api` | Standard envelope: `success`, `message`, `data` |
| `UniEnvelope<T>` | `dto.response.api` | Global response wrapper (traceId, timestamp, data, error) |
| `PageResponse<T>` | `dto.response.api` | Paginated: `content`, `page`, `size`, `totalElements`, `totalPages` |
| `AuthResponseDTO` | `dto.response.auth` | Login/register response: `success`, `message`, `user` (UserDTO) |
| `UserDTO` | `dto.response.user` | User + profile + roles for auth responses |
| `UserResponse` | `dto.response.user` | User + profile + roles for `/auth/me` |
| `UserProfileResponse` | `dto.response.user` | Profile fields only |
| `PermissionResponse` | `dto.response.rbac` | Permission metadata for admin panel |
| `RoleResponse` | `dto.response.rbac` | Role metadata for admin panel |
| `PasswordResetResponse` | `dto.response.password` | Reset flow messages |
| `MessageResponse` | `dto.response` | Simple message wrapper |
---

## 10. Configuration

### 10.1 Required Environment Variables (application.yaml)

```yaml
spring:
  application:
    name: unishare
  datasource:
    url: jdbc:postgresql://host:5432/db
    username: user
    password: pass
    driver-class-name: org.postgresql.Driver
    hikari:
      auto-commit: false
      maximum-pool-size: 5
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
    show-sql: true
    open-in-view: false
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB

frontend:
  url: http://localhost:3000

google:
  clientId: ${GOOGLE_CLIENT_ID}
  clientSecret: ${GOOGLE_CLIENT_SECRET}
  redirectURL: http://localhost:8080/api/auth/google/callback
  auth:
    endPoint: https://oauth2.googleapis.com/token

github:
  clientId: ${GITHUB_CLIENT_ID}
  clientSecret: ${GITHUB_CLIENT_SECRET}
  redirectUrl: http://localhost:8080/api/auth/github/callback
  tokenUrl: https://github.com/login/oauth/access_token
  userUrl: https://api.github.com/user

jwt:
  secret: ${JWT_SECRET}
  expiry: 86400000

production:
  status: false

cloudinary:
  cloud-name: ${CLOUDINARY_CLOUD_NAME}
  api-key: ${CLOUDINARY_API_KEY}
  api-secret: ${CLOUDINARY_API_SECRET}

unishare:
  security:
    scanning:
      enabled: true
      god-account: true
      god-role: true
      roles: true
      permissions: true
    seeding:
      enabled: true
      god-account: true
      god-role: true
      roles: true
      permissions: true
      god-account-role: true
    god-account:
      initial-password: ${GOD_ACCOUNT_INITIAL_PASSWORD}
```

### 10.2 Docker Deployment

**Dockerfile**: Multi-stage (Maven build → Eclipse Temurin 21 JRE alpine), non-root user, port 7500, healthcheck on `/actuator/health`.

**docker-compose.yml**:
- `postgres:17-alpine` (port 5432, healthcheck)
- `backend` (build from Dockerfile, port 7500, depends on postgres healthy, env vars from `.env`)

---

## 11. Background Jobs

| Service | Schedule | Action |
|---------|----------|--------|
| `RideCleanUpService` | Hourly at `:00` | Delete rides with `status=CANCELLED` and `cancelledAt > 48h ago` |
| `RideCleanUpService` | Hourly at `:10` | Delete ride requests with `status=CANCELLED` and `respondedAt > 24h ago` |

---

## 12. Cross-Cutting Concerns

| Concern | Implementation |
| **Global Response Envelope** | `GlobalResponseHandler` wraps all responses in `UniEnvelope` (traceId, timestamp, data, error) |
| **Request Tracing** | `RequestLoggingFilter` generates/propagates `traceId` (header `X-Trace-Id`) |
| **Idempotency** | `@Idempotent` annotation + `IdempotentInterceptor` reserves key in Redis (24h TTL) |
| **Redis Wrapper** | `RedisService` with `RedisResult`/`RedisException` for safe ops |
| **Valkey Diagnostics** | `ValkeyNetworkDiagnostic` optional TCP/TLS connectivity check on startup |
| **CORS** | `CorsConfig` allows frontend origin, credentials, all headers/methods |
| **Exception Handling** | `CustomAuthenticationEntryPoint` (401), `CustomAccessDeniedHandler` (403), global `@ControllerAdvice` |

---

## 13. Startup Initialization Sequence

1. `SecurityDeclarationScanner` — scans `@GodAccount`, `@GodRole`, `@uniShareRole`, `@Permission` from bytecode.
2. `PermissionScanner` — extracts permissions from `@Permission` annotations.
3. `SecuritySeedProperties` — binds `unishare.security.*` YAML flags.
4. `SecuritySeedInitializer` — gatekeeper: runs synchronizers only if flags enabled.
5. `SecurityDeclarationSynchronizer` — idempotent upsert of GodAccount, GodRole, Roles, GodAccountRole, GodRolePermissions.
6. `PermissionSynchronizer` — idempotent upsert of Permission rows; marks stale as DEPRECATED.
7. `RoleService` — caches default role (USER) and role priority for JWT `role` claim.

---

## 14. Testing

| Test Class | Coverage |
|------------|----------|
| `SecurityDeclarationScannerTest` | Frozen identity pin, declaration validation, collisions, UUID v7 |
| `SecuritySeedPropertiesTest` | YAML binding, flag dependency rules |
| `SecurityDeclarationSynchronizerTest` | GodAccount/GodRole/roles/GodAccountRole, legacy guard, GodRole permissions, domain changes, idempotency (in-memory tables) |
| `PermissionScannerTest` | Domain read from `@Permission` (default APPLICATION) |
| `PermissionSynchronizerTest` | Upsert, domain update, DEPRECATED lifecycle, idempotency |
| `RoleServiceTest` | Default role, primary-role priority |
| `SecuritySeedInitializerTest` | Startup order and flag gating |
| `ControlPlaneLifecycleOwnershipTest` | Bytecode-level mutation ownership + schema/entity isolation |
| `UnishareApplicationTests`, `RedisServiceTest` | Require local DB + Redis on localhost:6379; seeding disabled |

Run: `./mvnw test`

---

## 15. How To… (Common Operations)

### Add an Application Permission
1. Annotate controller method:
   ```java
   @Permission(
       id = "0199xxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", // NEW UUID v7
       displayName = "Action Name",
       description = "What this permission allows",
       baseEntity = Ride.class,
       reachableEntities = {RideRequest.class},
       domain = PermissionDomain.APPLICATION
   )
   @PreAuthorize("hasAuthority('NEW_PERMISSION_NAME')")
   ```
2. Restart app → row appears in `permissions`.
3. Grant to roles via `role_permissions` (SQL or future admin UI).

### Add a Control-Plane Permission
Same as above, but `domain = PermissionDomain.CONTROL_PLANE`. SUPERUSER gets it automatically on restart.

### Add a Normal Role
Add `@uniShareRole(id = <new UUID v7>, name, description)` on `UnishareApplication`.
- **Order matters**: first role = default for new users.
- Do not reorder unless intentionally changing default.

### Rename/Update Role Description
Edit the annotation; row updated by UUID (PK).

### ❌ Never Do
- Change a frozen UUID.
- Declare identities in YAML/SQL.
- Write to control-plane tables outside `SecurityDeclarationSynchronizer`.

---

## 16. Known Gaps & Next Steps

1. **Permissions not enforced per-request** — Method security not enabled; `@PreAuthorize("RIDE_CREATE")` is invalid SpEL (should be `hasAuthority('RIDE_CREATE')`). Today authorization is "authenticated or not" plus `ROLE_*` authorities.
2. **GodAccount has no login/MFA** — Control-plane permissions cannot be exercised yet; no CONTROL_PLANE `@Permission` exists because no control-plane endpoints exist yet.
3. **Internal IDs exposed** — JWT `userId`, `UserDTO.id` are database `Long` ids. Org standard requires an external `publicId` (UUID).
4. **No versioned migrations** — Relies on Hibernate `ddl-auto`. After any `create` run, switch back to `update`; adopt Flyway/Liquibase before production lock-down.
5. **`application-local.yaml` holds live credentials** (gitignored). Prefer environment variables, as in prod.
6. **No admin UI** — Control-panel endpoints exist but no frontend.

---

## 17. File Index (Key Files)

| File | Purpose |
|------|---------|
| `pom.xml` | Maven config, dependencies |
| `UnishareApplication.java` | Entry point + all security identity declarations |
| `SecurityConfig.java` | Spring Security filter chain |
| `JwtAuthenticationFilter.java` | JWT extraction (header + cookie) + auth context |
| `JwtService.java` | JWT generate/validate |
| `SecurityDeclarationSynchronizer.java` | Startup sync of GodAccount, GodRole, Roles, Permissions |
| `PermissionSynchronizer.java` | Startup sync of Permission table from `@Permission` |
| `RoleService.java` | Default role + primary role resolution |
| `RideService.java` | Ride CRUD + business rules |
| `RideRequestService.java` | Seat request lifecycle |
| `RideCleanUpService.java` | Scheduled cleanup |
| `GlobalResponseHandler.java` | Uniform response envelope |
| `RequestLoggingFilter.java` | TraceId per request |
| `IdempotentInterceptor.java` | Redis idempotency key reservation |
| `docker-compose.yml` | Local stack (Postgres + Backend) |
| `Dockerfile` | Production image |
| `.env.example` | Required env vars template |
| `docs/SYSTEM_OVERVIEW.md` | This document's source |

---

## 18. Quick Start

```bash
# 1. Configure
cp src/main/resources/application-example.yaml src/main/resources/application.yaml
# Edit with your credentials (or use environment variables)

# 2. Run locally
# Windows:
mvnw.cmd spring-boot:run
# Linux/Mac:
./mvnw spring-boot:run

# 3. Or with Docker
docker-compose up --build

# Server: http://localhost:8080 (local) or http://localhost:7500 (Docker)
# Health: GET /health
```

---

## 19. License & Author

**Author**: Pravin Choudhary  
**Repository**: https://github.com/PravinChoudhary11/unishareBackend

---

*Document generated from codebase analysis on 2026-10-10*