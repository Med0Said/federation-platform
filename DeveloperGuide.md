# Developer Handoff Guide — Sports Federation Platform

> This document is for developers joining the project. It covers the full architecture, every module's current state, database schema, frontend structure, and a detailed list of what still needs to be built.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack](#2-tech-stack)
3. [Repository Structure](#3-repository-structure)
4. [Backend Architecture](#4-backend-architecture)
5. [Database Schema](#5-database-schema)
6. [Module Status — Backend](#6-module-status--backend)
7. [Frontend Architecture](#7-frontend-architecture)
8. [Module Status — Frontend](#8-module-status--frontend)
9. [Auth Flow](#9-auth-flow)
10. [Key Patterns and Conventions](#10-key-patterns-and-conventions)
11. [What Is Missing — To Do List](#11-what-is-missing--to-do-list)
12. [Known Issues and Technical Debt](#12-known-issues-and-technical-debt)
13. [Running the Project](#13-running-the-project)

---

## 1. Project Overview

A full-stack platform for a national sports federation. It manages the complete lifecycle of:

- **Athletes** — registration, license management, club affiliations, categories
- **Clubs** — federation-registered sports clubs with rosters and managers
- **Competitions** — creation, scheduling, event configuration, registration
- **Competition Registrations** — athlete sign-ups, bib assignment, waiting lists
- **Results** — performance recording, rankings, medals, national records
- **News** — articles, announcements, press releases with tagging

The system is multi-role: administrators, federation staff, club managers, registered athletes, and public visitors all have different levels of access.

---

## 2. Tech Stack

### Backend
| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security 6 + JJWT 0.12.5 |
| ORM | Hibernate 6.4.4 / Spring Data JPA 3.2.5 |
| Database | PostgreSQL 16 |
| Migrations | Flyway 9.22.3 |
| Mapping | MapStruct 1.5.5 |
| Boilerplate | Lombok 1.18.32 |
| API Docs | SpringDoc OpenAPI 2.5.0 (Swagger UI) |
| Build | Maven 3.9 |

### Frontend
| Layer | Technology |
|---|---|
| Framework | Angular 19 (standalone components) |
| Language | TypeScript 5.5 |
| UI Library | Angular Material 19 |
| Styling | Tailwind CSS 3.4 |
| State | Angular Signals |
| HTTP | Angular HttpClient + functional interceptors |
| Build | Angular CLI / esbuild |

### Infrastructure
| Component | Technology |
|---|---|
| Database host | Docker (docker compose) |
| Dev proxy | Angular proxy.conf.json → :8080 |

---

## 3. Repository Structure

```
federation-platform/
│
├── sports-federation/                      ← Spring Boot backend
│   ├── src/main/java/com/federation/
│   │   ├── SportsFederationApplication.java
│   │   ├── auth/                           ← JWT auth module (COMPLETE)
│   │   ├── athletes/                       ← Athletes module (COMPLETE)
│   │   ├── clubs/                          ← Clubs module (COMPLETE)
│   │   ├── competitions/                   ← Competitions module (COMPLETE)
│   │   ├── results/                        ← Results module (COMPLETE)
│   │   ├── news/                           ← News module (COMPLETE)
│   │   ├── users/                          ← User management (PARTIAL)
│   │   └── common/                         ← Shared infrastructure
│   │       ├── config/                     ← Security, JPA, CORS, OpenAPI
│   │       ├── exception/                  ← Exception hierarchy + GlobalExceptionHandler
│   │       ├── response/                   ← ApiResponse<T>, PagedResponse<T>
│   │       └── util/                       ← BaseEntity, JwtTokenUtil, Gender enum
│   ├── src/main/resources/
│   │   ├── application.yml                 ← All configuration
│   │   └── db/migration/                   ← 12 Flyway migrations (V1–V12)
│   ├── src/test/                           ← Integration + unit tests
│   ├── docker-compose.yml                  ← PostgreSQL + pgAdmin
│   └── pom.xml
│
└── federation-frontend/                    ← Angular 19 SPA
    ├── src/app/
    │   ├── core/                           ← Singleton layer
    │   │   ├── models/index.ts             ← All TypeScript interfaces and enums
    │   │   ├── services/                   ← auth, api, token, notification, theme
    │   │   ├── guards/                     ← auth, role, guest guards
    │   │   └── interceptors/              ← auth (JWT), error
    │   ├── shared/                         ← Reusable UI
    │   │   ├── components/                 ← loading-spinner, page-header, status-chip, etc.
    │   │   ├── pipes/                      ← relativeTime, initials, roleLabel, truncate
    │   │   └── directives/                 ← *appHasRole structural directive
    │   ├── layouts/                        ← Shell components
    │   │   ├── public-layout/              ← Navbar + footer (unauthenticated pages)
    │   │   └── admin-layout/               ← Sidebar + topbar (authenticated pages)
    │   └── features/                       ← Lazy-loaded pages (see section 8)
    ├── proxy.conf.json                     ← /api → http://localhost:8080
    ├── tailwind.config.js
    └── package.json
```

---

## 4. Backend Architecture

### Layer Pattern

Every feature module follows the same 6-layer pattern:

```
Controller  →  Service  →  Repository
    ↑              ↑
  DTOs          Entity
    ↑
  Mapper (MapStruct)
```

```
com.federation.{module}/
├── controller/     REST endpoint, @RequestMapping, input validation only
├── service/        Business logic, @Transactional, @PreAuthorize
├── repository/     extends JpaRepository<Entity, UUID>
├── entity/         @Entity, extends BaseEntity
├── dto/            Request/Response POJOs (Lombok @Builder)
└── mapper/         @Mapper(componentModel = "spring") — MapStruct interface
```

### BaseEntity

All entities extend `BaseEntity` which provides:

```java
@MappedSuperclass
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreatedDate
    private Instant createdAt;  // auto-set by JPA auditing

    @LastModifiedDate
    private Instant updatedAt;  // auto-updated on save
}
```

> **Important:** Because `id`, `createdAt`, `updatedAt` live in the parent class — NOT the entity's own class — MapStruct `@Builder` cannot see them. **Never add `@Mapping(target="id", ignore=true)` in mappers** — it will cause a compile error. Simply omit them; JPA sets them automatically.

### API Response Envelope

Every endpoint returns `ApiResponse<T>`:

```json
// Success
{ "success": true, "data": { ... }, "timestamp": "2026-06-07T..." }

// Paginated
{ "success": true, "data": { "content": [...], "page": 0, "size": 25,
  "totalElements": 120, "totalPages": 5, "first": true, "last": false } }

// Error
{ "success": false, "message": "Resource not found", "status": 404, "path": "/api/clubs/..." }
```

Use the static factories: `ApiResponse.ok(data)`, `ApiResponse.created(data)`, `ApiResponse.noContent()`.

### Security Configuration

Located in `common/config/SecurityConfig.java`:

- All `/api/auth/**` endpoints are public
- All other endpoints require a valid JWT
- Method-level security is enabled via `@EnableMethodSecurity`
- Role checks in services use `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`

### Exception Hierarchy

```
FederationException (base, unchecked)
├── ResourceNotFoundException   → 404
├── ResourceAlreadyExistsException → 409
├── BadRequestException         → 400
├── UnauthorizedException       → 401
├── ForbiddenException          → 403
└── BusinessRuleViolationException → 422
```

`GlobalExceptionHandler` catches all of these and returns a consistent `ApiResponse` error shape. Add new exception types here if needed.

### PostgreSQL Enum Type Mapping

This project uses **named PostgreSQL enum types** (e.g. `user_role`, `club_status`). When mapping Java enums to these columns you **must** use:

```java
@JdbcTypeCode(SqlTypes.NAMED_ENUM)
@Column(columnDefinition = "the_pg_enum_name")
private MyEnum field;
```

Do **not** use `@Enumerated(EnumType.STRING)` alone — PostgreSQL will reject the insert with a type mismatch error.

---

## 5. Database Schema

### Migration Files

| File | Description |
|---|---|
| `V1__init_schema.sql` | `users`, `refresh_tokens` tables. Extensions: `uuid-ossp`, `pgcrypto`, `pg_trgm` |
| `V2__seed_admin_user.sql` | Seeds `admin@federation.local` / `Admin@1234` (BCrypt strength 10) |
| `V3__drop_placeholder_tables.sql` | Removes temp tables from V1 |
| `V4__enums.sql` | All 17 PostgreSQL custom enum types |
| `V5__clubs_schema.sql` | `clubs` table + indexes |
| `V6__athletes_schema.sql` | `athletes`, `athlete_club_history`, `athlete_documents` |
| `V7__competitions_schema.sql` | `competitions`, `competition_events` |
| `V8__competition_registrations_schema.sql` | `competition_registrations`, `registration_payments` |
| `V9__results_schema.sql` | `results`, `rankings` |
| `V10__news_schema.sql` | `news`, `tags`, `news_tags` junction |
| `V11__views.sql` | 6 read-optimised SQL views |
| `V12__seed_data.sql` | Full test dataset — users, clubs, athletes, competitions, results, news |

### Entity Relationships

```
users ──────────────────────────────────────────┐
  │ (manager)                                   │
  ▼                                             │
clubs ──────────────────────────────────────┐   │
  │ (club_id)                               │   │
  ▼                                         │   │
athletes ────────────────────────────────┐  │   │
  │                                      │  │   │
  ▼ (athlete_id)                         │  │   │
competition_registrations                │  │   │
  │ (event_id)                           │  │   │
  ▼                                      │  │   │
competition_events                       │  │   │
  │ (competition_id)                     │  │   │
  ▼                                      │  │   │
competitions ◄───────────────────────────┘  │   │
  (host_club_id) ►────────────────────────►─┘   │
  (organizer_id) ►────────────────────────────►─┘
  │
  ▼ (competition_id + event_id + athlete_id)
results ──► rankings
```

### PostgreSQL Enum Types (V4)

```
user_role:          ROLE_ADMIN, ROLE_FEDERATION_STAFF, ROLE_CLUB_MANAGER, ROLE_ATHLETE, ROLE_PUBLIC
user_status:        ACTIVE, INACTIVE, SUSPENDED, BANNED
gender:             MALE, FEMALE, OTHER
club_status:        ACTIVE, SUSPENDED, DISSOLVED
athlete_status:     ACTIVE, INACTIVE, INJURED, RETIRED, SUSPENDED
athlete_category:   YOUTH, JUNIOR, SENIOR, MASTERS, GRAND_MASTERS
competition_status: DRAFT, PUBLISHED, REGISTRATION_OPEN, REGISTRATION_CLOSED, ONGOING, COMPLETED, CANCELLED
competition_level:  LOCAL, REGIONAL, NATIONAL, INTERNATIONAL
competition_format: INDIVIDUAL, TEAM, RELAY, MIXED
registration_status:PENDING, CONFIRMED, WAITLISTED, CANCELLED, DISQUALIFIED
result_status:      UNOFFICIAL, OFFICIAL, PROTESTED, DISQUALIFIED, DNS, DNF, DSQ
medal_type:         GOLD, SILVER, BRONZE
news_status:        DRAFT, REVIEW, PUBLISHED, ARCHIVED
news_category:      GENERAL, COMPETITION, ATHLETE, CLUB, ANNOUNCEMENT, PRESS_RELEASE
```

---

## 6. Module Status — Backend

### ✅ auth — COMPLETE

**Files:** `AuthController`, `AuthService`, `AuthMapper`, `RefreshTokenRepository`, `FederationUserDetails`, `JwtTokenUtil`, `JwtAuthenticationFilter`

**Endpoints:**
- `POST /auth/register` — create account (role: ROLE_PUBLIC)
- `POST /auth/login` — returns `{ accessToken, refreshToken, role, ... }`
- `POST /auth/refresh` — rotate token pair
- `POST /auth/logout` — revoke tokens server-side
- `GET  /auth/me` — authenticated user profile
- `PATCH /auth/change-password` — requires current password

**Notes:** Refresh tokens are stored in the `refresh_tokens` table. A scheduled task (`TokenCleanupTask`) deletes expired tokens. Tokens are rotated on every refresh (old token revoked, new pair issued).

---

### ✅ athletes — COMPLETE

**Files:** `AthleteController`, `AthleteService`, `AthleteRepository`, `Athlete`, `AthleteRequest/Response`, `AthleteMapper`

**Endpoints:**
- `GET    /athletes` — paginated list with search + filters (status, gender, category, clubId)
- `GET    /athletes/{id}` — detail
- `POST   /athletes` — create (ADMIN, STAFF, CLUB_MANAGER)
- `PUT    /athletes/{id}` — update (ADMIN, STAFF, CLUB_MANAGER)
- `DELETE /athletes/{id}` — delete (ADMIN, STAFF)

**Notes:** The `category` field (YOUTH/JUNIOR/SENIOR/MASTERS/GRAND_MASTERS) is **not** a generated DB column — it's computed from `dateOfBirth` in `AthleteService` before every save using `AthleteCategory.fromAge(int)`. The filtering uses in-memory Java streams (not JPQL) due to PostgreSQL enum type inference issues with parameterised queries.

---

### ✅ clubs — COMPLETE

**Files:** `ClubController`, `ClubService`, `ClubRepository`, `Club`, `ClubRequest/Response`, `ClubMapper`

**Endpoints:**
- `GET    /clubs` — paginated list with search + status filter
- `GET    /clubs/{id}` — by ID
- `GET    /clubs/slug/{slug}` — by slug
- `POST   /clubs` — create (ADMIN, STAFF)
- `PUT    /clubs/{id}` — update (ADMIN, STAFF, CLUB_MANAGER)
- `DELETE /clubs/{id}` — delete (ADMIN)

**Notes:** Slugs are auto-generated from the club name (URL-safe, unique). Filtering is in-memory Java streams.

---

### ✅ competitions — COMPLETE

**Files:** `CompetitionController`, `CompetitionService`, `CompetitionRepository`, `CompetitionEventRepository`, `Competition`, `CompetitionEvent`, `CompetitionRequest/Response`, `CompetitionMapper`

**Endpoints:**
- `GET    /competitions` — filtered list (search, status, from, to dates)
- `GET    /competitions/{id}` — detail with event count
- `GET    /competitions/slug/{slug}` — by slug
- `POST   /competitions` — create (ADMIN, STAFF)
- `PUT    /competitions/{id}` — update (ADMIN, STAFF)
- `DELETE /competitions/{id}` — delete (ADMIN)

**Missing:** No endpoints for `competition_events` (sub-events within a competition). The `CompetitionEvent` entity and `CompetitionEventRepository` exist but there is no `CompetitionEventController` or `CompetitionEventService`. This needs to be built.

---

### ✅ results — COMPLETE

**Files:** `ResultController`, `ResultService`, `ResultRepository`, `RankingRepository`, `Result`, `Ranking`, `ResultRequest/Response`, `ResultMapper`

**Endpoints:**
- `GET    /results` — filtered list (competitionId, eventId, athleteId, status)
- `GET    /results/{id}` — detail with rank/medal enrichment
- `GET    /results/event/{eventId}` — all results for an event, ordered by rank
- `POST   /results` — record result (ADMIN, STAFF)
- `PUT    /results/{id}` — update (ADMIN, STAFF)
- `DELETE /results/{id}` — delete (ADMIN, STAFF)

**Missing:** No endpoint to **create/update rankings**. Rankings are read during result queries but there is no service method to recalculate and write rankings after results are entered. This needs to be built.

---

### ✅ news — COMPLETE

The news module was complete in the original auth zip (691 lines). Full CRUD + publishing workflow + tag management.

---

### ⚠️ users — PARTIAL

**Files:** `UserRepository`, `UserDetailsServiceImpl`, `UserDtos`, `UsersController` (basic)

The users module has a list and detail stub on the frontend but the backend `UsersController` has minimal implementation. Full admin user management (list all users, change roles, ban/unban, reset passwords) is not implemented.

---

### ❌ competition_registrations — NOT IMPLEMENTED

The database table `competition_registrations` exists (V8 migration) with full schema including `registration_payments`. But there is **zero Java code** for this module — no entity, no repository, no service, no controller, no DTOs. This is the most important missing backend module.

---

## 7. Frontend Architecture

### Routing Structure

```
/                         → redirect to /home
/home                     → HomeComponent (public landing)
/competitions             → CompetitionsListComponent (public)
/clubs                    → ClubsListComponent (public)
/athletes                 → AthletesListComponent (public)
/results                  → ResultsListComponent (public)
/news                     → NewsListComponent (public)
/auth/login               → LoginComponent (guest only)
/auth/register            → RegisterComponent (guest only)
/auth/forgot-password     → ForgotPasswordComponent

/admin  (authGuard)
  /admin/dashboard        → DashboardComponent
  /admin/users            → UsersListComponent (ADMIN only)
  /admin/clubs/**         → adminClubsRoutes
  /admin/athletes/**      → adminAthletesRoutes
  /admin/competitions/**  → adminCompetitionsRoutes
  /admin/results/**       → adminResultsRoutes
  /admin/news/**          → adminNewsRoutes
  /admin/profile          → ProfileComponent

/portal  (authGuard + ROLE_ATHLETE)
  /portal/profile         → ProfileComponent
  /portal/registrations   → MyRegistrationsComponent
  /portal/results         → MyResultsComponent

/403                      → ForbiddenComponent
/404                      → NotFoundComponent
```

### Core Services

| Service | File | Responsibility |
|---|---|---|
| `AuthService` | `core/services/auth.service.ts` | Login, register, logout, token refresh, signals |
| `TokenService` | `core/services/token.service.ts` | Store/retrieve JWT tokens (memory + localStorage) |
| `ApiService` | `core/services/api.service.ts` | Generic HTTP wrapper, auto-unwraps `ApiResponse<T>` |
| `NotificationService` | `core/services/notification.service.ts` | Snackbar toasts |
| `ThemeService` | `core/services/theme.service.ts` | Dark/light mode toggle |

### `ApiService` — How to use it

`ApiService` automatically unwraps the `ApiResponse<T>` envelope so callers receive `T` directly:

```typescript
// Correct — ApiService unwraps for you
this.api.get<Club>('/clubs/some-id').subscribe(club => {
  console.log(club.name); // Club, not ApiResponse<Club>
});

// Paginated
this.api.getPaged<Athlete>('/athletes', { page: 0, size: 25 }).subscribe(page => {
  console.log(page.content);      // Athlete[]
  console.log(page.totalElements); // number
});
```

### Guards

| Guard | File | Behaviour |
|---|---|---|
| `authGuard` | `core/guards/auth.guard.ts` | Redirects to `/auth/login` if not authenticated |
| `roleGuard` | `core/guards/role.guard.ts` | Reads `data.roles` from route, redirects to `/403` |
| `guestGuard` | `core/guards/guest.guard.ts` | Redirects authenticated users away from login/register |

### Interceptors

| Interceptor | File | Behaviour |
|---|---|---|
| `authInterceptor` | `core/interceptors/auth.interceptor.ts` | Attaches `Authorization: Bearer <token>` to every request. On 401, queues concurrent requests, attempts token refresh, then replays them. On refresh failure, logs out. |
| `errorInterceptor` | `core/interceptors/error.interceptor.ts` | Maps HTTP errors to user-friendly snackbar messages |

### Shared Components

| Component | Selector | Usage |
|---|---|---|
| `LoadingSpinnerComponent` | `<app-loading-spinner>` | Inline or overlay spinner. Props: `size`, `message`, `overlay` |
| `PageHeaderComponent` | `<app-page-header>` | Title + breadcrumbs + action slot (`actions` content projection) |
| `StatusChipComponent` | `<app-status-chip>` | Coloured badge for any status enum string |
| `RoleBadgeComponent` | `<app-role-badge>` | Coloured badge specifically for user roles |
| `EmptyStateComponent` | `<app-empty-state>` | Zero-data placeholder with icon, message, optional action |
| `ConfirmDialogComponent` | (MatDialog) | Confirmation modal with danger mode. Open via `MatDialog.open()` |

### Pipes (in `shared/pipes/index.ts`)

| Pipe | Usage |
|---|---|
| `relativeTime` | `{{ date \| relativeTime }}` → "3 hours ago" |
| `initials` | `{{ fullName \| initials }}` → "AB" |
| `roleLabel` | `{{ role \| roleLabel }}` → "Club Manager" |
| `truncate` | `{{ text \| truncate:100 }}` |

---

## 8. Module Status — Frontend

### ✅ COMPLETE (production-ready)

| Feature | Components | Notes |
|---|---|---|
| **Auth** | Login, Register, ForgotPassword | Full forms, validation, error handling, password strength |
| **Dashboard** | DashboardComponent | KPI cards, quick actions, role-aware |
| **Profile** | ProfileComponent | View/edit profile, change password tabs |
| **Error pages** | NotFoundComponent, ForbiddenComponent | 404 and 403 with back/home buttons |
| **Public Home** | HomeComponent | Landing page with stats and feature cards |
| **Athletes List** | AthletesListComponent | Full paginated table, search, sort, gender/category/status filters, context menu, confirm-delete |
| **Athlete Detail** | AthleteDetailComponent | Profile card + tabbed history |
| **Athlete Form** | AthleteFormComponent | Full reactive form, create + edit mode |

### ⚠️ STUB (route works, shows placeholder UI, needs implementation)

These components exist and compile, but show a "coming in next phase" message. The data layer (API calls, forms, tables) needs to be written.

| Feature | Files to implement |
|---|---|
| **Clubs List** | `clubs-list.component.ts` — has card grid layout but no real API call |
| **Club Detail** | `club-detail.component.ts` — placeholder only |
| **Club Form** | `club-form.component.ts` — placeholder only |
| **Competitions List** | `competitions-list.component.ts` — placeholder |
| **Competition Detail** | `competition-detail.component.ts` — placeholder |
| **Competition Form** | `competition-form.component.ts` — placeholder |
| **Results List** | `results-list.component.ts` — placeholder |
| **Result Detail** | `result-detail.component.ts` — placeholder |
| **Result Form** | `result-form.component.ts` — placeholder |
| **News List** | `news-list.component.ts` — placeholder |
| **News Detail** | `news-detail.component.ts` — placeholder |
| **News Form** | `news-form.component.ts` — placeholder |
| **Users List** | `users-list.component.ts` — placeholder |
| **User Detail** | `user-detail.component.ts` — placeholder |
| **My Registrations** | `my-registrations.component.ts` — portal stub |
| **My Results** | `my-results.component.ts` — portal stub |

### ❌ NOT STARTED (no component exists at all)

| Feature | Notes |
|---|---|
| **Registration flow** | No component for athletes registering to competitions |
| **Competition events** | No UI to create/manage sub-events within a competition |
| **Rankings page** | No dedicated rankings/leaderboard view |
| **Public competition calendar** | No public-facing calendar view |
| **Notifications** | No real-time or polling notification system |

---

## 9. Auth Flow

```
Login form submit
│
▼
POST /api/auth/login
│
▼ (success)
Store accessToken in memory (TokenService.accessToken = ...)
Store refreshToken in localStorage
Load profile → GET /api/auth/me
Set AuthService.currentUser signal
Navigate to /admin/dashboard (or role-appropriate route)

─── On every API request ─────────────────────────────────
authInterceptor adds: Authorization: Bearer <accessToken>

─── On 401 response ──────────────────────────────────────
If not already refreshing:
  → POST /api/auth/refresh with refreshToken
  → On success: store new tokens, replay original request
  → On failure: clearAuthState(), navigate to /auth/login

Other in-flight requests: queue on refreshDone$ BehaviorSubject,
replay all when refresh completes

─── On logout ────────────────────────────────────────────
clearAuthState() → signals cleared, localStorage cleared
Navigate to /auth/login
Fire POST /api/auth/logout (best-effort, errors swallowed)
```

---

## 10. Key Patterns and Conventions

### Adding a new backend module

Follow this checklist for every new module:

1. Create package `com.federation.{module}/`
2. Create enum(s) if needed — add them to `V4__enums.sql` in a new migration
3. Create entity extending `BaseEntity`
4. On enum fields: use `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` + `@Column(columnDefinition = "pg_enum_name")`
5. Create `XxxRepository extends JpaRepository<Entity, UUID>`
6. Create `XxxRequest` and `XxxResponse` DTOs with Lombok `@Builder`
7. Create `XxxMapper` with MapStruct — **do not** add `@Mapping(target="id/createdAt/updatedAt", ignore=true)`
8. Create `XxxService` with `@PreAuthorize` on write methods
9. Create `XxxController` with `@RequestMapping("/xxx")` — return `ApiResponse<T>` or `ApiResponse<PagedResponse<T>>`
10. If filtering by enums in JPQL, use Java stream filtering instead to avoid PostgreSQL type inference issues

### Adding a new frontend feature page

1. Create directory `src/app/features/{module}/{component-name}/`
2. Create `{component-name}.component.ts` as standalone component
3. Add route in the feature's `*.routes.ts` file
4. Use `ApiService` for all HTTP calls — it unwraps `ApiResponse<T>` automatically
5. Use `PageHeaderComponent` for the page title/breadcrumbs
6. Use `EmptyStateComponent` for zero-data states
7. Use `LoadingSpinnerComponent` with `[overlay]="true"` during loading

### Naming conventions

| Layer | Convention | Example |
|---|---|---|
| Entity | PascalCase | `Athlete`, `CompetitionEvent` |
| Repository | `{Entity}Repository` | `AthleteRepository` |
| Service | `{Entity}Service` | `AthleteService` |
| Controller | `{Entity}Controller` | `AthleteController` |
| Request DTO | `{Entity}Request` | `AthleteRequest` |
| Response DTO | `{Entity}Response` | `AthleteResponse` |
| Mapper | `{Entity}Mapper` | `AthleteMapper` |
| Angular component | `{name}.component.ts` | `athletes-list.component.ts` |
| Angular service | `{name}.service.ts` | `auth.service.ts` |
| Angular route file | `{module}.routes.ts` | `athletes.routes.ts` |

---

## 11. What Is Missing — To Do List

### Backend — High Priority

#### 1. Competition Registrations module (biggest gap)

The DB table exists (`competition_registrations`) but there is zero Java code. Create the full module:

```
competitions_registrations/
├── entity/
│   ├── CompetitionRegistration.java    ← map to competition_registrations table
│   └── RegistrationStatus.java         ← enum (already in DB as registration_status)
├── repository/
│   └── CompetitionRegistrationRepository.java
├── dto/
│   ├── RegistrationRequest.java
│   └── RegistrationResponse.java
├── mapper/
│   └── RegistrationMapper.java
├── service/
│   └── RegistrationService.java        ← enforce max_participants, waitlist logic
└── controller/
    └── RegistrationController.java
```

Key endpoints needed:
- `POST /competitions/{id}/registrations` — athlete registers for an event
- `GET  /competitions/{id}/registrations` — list registrations (staff)
- `GET  /athletes/{id}/registrations` — athlete's own registrations
- `PATCH /registrations/{id}/status` — confirm, waitlist, cancel
- `GET  /registrations/{id}` — detail

#### 2. Competition Events endpoints

`CompetitionEvent` entity and repository exist. Needs a controller and service:
- `GET  /competitions/{id}/events` — list events for a competition
- `POST /competitions/{id}/events` — create event (ADMIN, STAFF)
- `PUT  /competitions/{id}/events/{eventId}` — update
- `DELETE /competitions/{id}/events/{eventId}` — delete

#### 3. Ranking recalculation

Results are stored but rankings are never automatically computed. After results are entered for an event, a method should sort by `performanceValue` (respecting `lowerIsBetter`), assign `rankPosition`, and write `Ranking` rows. Add this to `ResultService` or a dedicated `RankingService`.

#### 4. User management endpoints

`UsersController` needs:
- `GET    /users` — paginated user list (ADMIN)
- `GET    /users/{id}` — user detail
- `PATCH  /users/{id}/role` — change role (ADMIN)
- `PATCH  /users/{id}/status` — activate/ban (ADMIN)
- `POST   /users/{id}/reset-password` — admin password reset

#### 5. Forgot password endpoint

`ForgotPasswordComponent` exists in the frontend but `POST /auth/forgot-password` is not implemented. Needs email integration (SMTP config) or a token-based reset flow.

---

### Frontend — High Priority

#### 1. Clubs module (stub → real)

`clubs-list.component.ts` has the card-grid layout but no real API call. Implement:
- Call `GET /api/clubs` with pagination and search
- Render club cards with real data
- `club-detail.component.ts` — show full club info + athlete count
- `club-form.component.ts` — create/edit form with validation

#### 2. Competitions module (stub → real)

- `competitions-list.component.ts` — paginated table/card grid, filter by status and date range
- `competition-detail.component.ts` — show competition info, events list, registration count
- `competition-form.component.ts` — multi-step form: basic info → venue → events → settings
- Add event management sub-page

#### 3. Registration flow

New component needed — `competition-register.component.ts`:
- Athlete selects an event within a competition
- Submits seed value (entry mark)
- Shows confirmation / waitlist message

#### 4. Results and Rankings module (stub → real)

- `results-list.component.ts` — filterable table by competition/event
- `result-form.component.ts` — enter multiple results for an event at once
- New: `rankings.component.ts` — leaderboard view per event

#### 5. News module (stub → real)

- `news-list.component.ts` — card grid with search and category filter
- `news-detail.component.ts` — full article view with rich text
- `news-form.component.ts` — editor with tag management and publish workflow

#### 6. User management (stub → real)

- `users-list.component.ts` — paginated table with role/status filters
- `user-detail.component.ts` — full user profile with edit and role change actions

#### 7. Athlete portal

`MyRegistrationsComponent` and `MyResultsComponent` exist as stubs:
- Wire up `GET /athletes/{id}/registrations` for registrations list
- Wire up `GET /results?athleteId={id}` for results list
- Add personal bests summary

---

### Infrastructure — Lower Priority

| Item | Description |
|---|---|
| Email service | Needed for forgot-password reset and registration confirmation emails. Add JavaMail or SendGrid config to `application.yml`. |
| File uploads | `photo_url` on athletes/news stores URLs only. A file upload endpoint + S3/MinIO integration is not implemented. |
| Pagination in repositories | Current athlete and club filtering uses `findAll()` + Java streams, which loads everything into memory. For large datasets this needs to be replaced with Spring Data `Specification` or native queries with proper type casting. |
| Production config | No `application-prod.yml`. JWT secret, database credentials, CORS origins all need to be externalised via environment variables before any deployment. |
| Test coverage | Integration tests exist only for auth (`AuthIntegrationTest`). No tests for clubs, athletes, competitions, results, or news. |
| Pagination for competition registrations | The `V8` schema has `waitlist_position` — implement the waitlist logic in `RegistrationService`. |

---

## 12. Known Issues and Technical Debt

| Issue | Location | Impact | Fix |
|---|---|---|---|
| In-memory filtering | `AthleteService.findAll()`, `ClubService.findAll()` | Performance with large datasets | Replace with Spring Data `Specification` + proper JPQL type casting |
| MapStruct warnings | All 4 mappers | Build noise only | Add `@Mapping(target="postalCode/venueAddress/disqualificationReason", ignore=true)` to silence warnings |
| News mapper warning | `NewsMapper` | Build noise only | Same as above — add `ignore=true` for unmapped `id/createdAt/updatedAt` |
| `ClubDtos.java` | `clubs/dto/` | Placeholder file with a comment, not harmful | Delete the file (real DTOs are in separate files) |
| Duplicate junk directories | `auth/{...}` and `results/{...}` | Empty directories, not harmful | Delete them |
| Hibernate dialect warning | `application.yml` | Harmless deprecation | Remove `hibernate.dialect` from `application.yml` — Hibernate auto-detects it |

---

## 13. Running the Project

### Quick start

```bash
# 1. Start database
cd sports-federation
docker compose up -d postgres

# 2. Start backend
mvn spring-boot:run -DskipTests

# 3. Start frontend (new terminal)
cd ../federation-frontend
npm install   # first time only
npm start
```

Open **http://localhost:4200** — log in as `admin@federation.local` / `Admin@1234`.

### Service URLs

| Service | URL |
|---|---|
| Angular app | http://localhost:4200 |
| Spring Boot API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/api/swagger-ui.html |
| Health check | http://localhost:8080/api/actuator/health |

### Reset the database

```bash
docker compose down -v   # wipes all data
docker compose up -d postgres
# Restart Spring Boot — Flyway re-runs all 12 migrations
```
