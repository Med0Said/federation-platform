# Federation Frontend — Angular 19

Production-grade Angular 19 SPA for the Sports Federation Platform.

---

## Quick Start

```bash
# 1. Install dependencies
npm install

# 2. Ensure Spring Boot backend is running on :8080
# (dev proxy forwards /api → http://localhost:8080/api)

# 3. Start dev server with hot reload
npm start
# → http://localhost:4200
```

---

## Tech Stack

| Layer            | Technology                      |
|------------------|---------------------------------|
| Framework        | Angular 19 (standalone, signals)|
| Styling          | Tailwind CSS 3.4 + SCSS         |
| Component lib    | Angular Material 19 (M2 theme)  |
| HTTP             | Angular HttpClient + functional interceptors |
| State            | Angular Signals (no NgRx needed at this scale) |
| Routing          | Angular Router with lazy loading + view transitions |
| Build            | `@angular-devkit/build-angular` (esbuild) |
| Type safety      | TypeScript 5.5 strict mode      |

---

## Project Structure

```
src/
├── app/
│   ├── core/                          # Singleton services, guards, interceptors
│   │   ├── models/           index.ts # All shared TypeScript interfaces & enums
│   │   ├── services/
│   │   │   ├── auth.service.ts        # Login, register, logout, token refresh
│   │   │   ├── token.service.ts       # JWT storage (memory + localStorage)
│   │   │   ├── api.service.ts         # Generic HTTP wrapper (auto-unwraps ApiResponse)
│   │   │   ├── notification.service.ts# Snackbar notifications
│   │   │   └── theme.service.ts       # Dark/light mode toggle
│   │   ├── guards/
│   │   │   ├── auth.guard.ts          # Blocks unauthenticated access → /auth/login
│   │   │   ├── role.guard.ts          # Blocks wrong-role access → /403
│   │   │   └── guest.guard.ts         # Redirects logged-in users away from login/register
│   │   └── interceptors/
│   │       ├── auth.interceptor.ts    # Attaches Bearer token; handles 401 + token refresh
│   │       └── error.interceptor.ts   # Maps HTTP errors to user-friendly snackbar messages
│   │
│   ├── shared/                        # Reusable UI building blocks
│   │   ├── components/
│   │   │   ├── loading-spinner/       # <app-loading-spinner> — inline or overlay
│   │   │   ├── confirm-dialog/        # <app-confirm-dialog> — Mat dialog with danger mode
│   │   │   ├── page-header/           # <app-page-header> — title + breadcrumbs + action slot
│   │   │   ├── status-chip/           # <app-status-chip> and <app-role-badge>
│   │   │   └── empty-state/           # <app-empty-state> — zero-data placeholder
│   │   ├── pipes/        index.ts     # relativeTime, truncate, roleLabel, initials, performance
│   │   └── directives/   index.ts     # *appHasRole structural directive, clickOutside
│   │
│   ├── layouts/
│   │   ├── public-layout/             # Navbar + footer shell (public pages)
│   │   ├── admin-layout/              # Sidebar + topbar shell (admin/portal pages)
│   │   └── components/
│   │       ├── navbar/                # Public top navigation
│   │       ├── sidebar/               # Admin collapsible sidebar with RBAC nav
│   │       ├── user-menu/             # Avatar dropdown with role badge
│   │       └── breadcrumb/            # Auto-generated breadcrumbs from route data
│   │
│   ├── features/                      # Lazy-loaded feature modules
│   │   ├── home/                      # Landing page
│   │   ├── auth/{login,register,forgot-password}/
│   │   ├── dashboard/                 # Admin dashboard with KPI cards
│   │   ├── profile/                   # User profile + change password
│   │   ├── competitions/              # Public + admin competition pages
│   │   ├── clubs/                     # Public + admin club pages
│   │   ├── athletes/                  # Public + admin athlete pages
│   │   ├── results/                   # Public + admin result pages
│   │   ├── news/                      # Public + admin news pages
│   │   ├── users/                     # Admin user management
│   │   ├── portal/                    # Athlete self-service portal
│   │   └── errors/{not-found,forbidden}/
│   │
│   ├── app.routes.ts                  # Root route tree (all lazy-loaded)
│   ├── app.config.ts                  # Application providers + APP_INITIALIZER
│   └── app.component.ts               # Root shell (just <router-outlet>)
│
├── environments/
│   ├── environment.ts                 # Dev (proxy to :8080)
│   └── environment.prod.ts            # Prod (same-origin /api)
│
└── styles/
    ├── styles.scss                    # Tailwind directives + component utilities
    └── material-theme.scss            # Angular Material M2 brand theme
```

---

## Routing Structure

```
/                         → redirect to /home
/home                     → HomeComponent          (public)
/competitions/**          → competitionsRoutes     (public, lazy)
/clubs/**                 → clubsRoutes            (public, lazy)
/athletes/**              → athletesRoutes         (public, lazy)
/results/**               → resultsRoutes          (public, lazy)
/news/**                  → newsRoutes             (public, lazy)
/auth/login               → LoginComponent         (guest only)
/auth/register            → RegisterComponent      (guest only)
/auth/forgot-password     → ForgotPasswordComponent(guest only)
/admin                    → AdminLayoutComponent   (authGuard)
  /admin/dashboard        → DashboardComponent     (ADMIN | STAFF | CLUB_MANAGER)
  /admin/users/**         → usersRoutes            (ADMIN only)
  /admin/clubs/**         → adminClubsRoutes       (ADMIN | STAFF | CLUB_MANAGER)
  /admin/athletes/**      → adminAthletesRoutes    (ADMIN | STAFF | CLUB_MANAGER)
  /admin/competitions/**  → adminCompetitionsRoutes(ADMIN | STAFF)
  /admin/results/**       → adminResultsRoutes     (ADMIN | STAFF)
  /admin/news/**          → adminNewsRoutes        (ADMIN | STAFF)
  /admin/profile          → ProfileComponent       (any authenticated)
/portal                   → AdminLayoutComponent   (authGuard + roleGuard[ATHLETE])
  /portal/profile         → ProfileComponent
  /portal/registrations   → MyRegistrationsComponent
  /portal/results         → MyResultsComponent
/403                      → ForbiddenComponent
/404                      → NotFoundComponent
/**                       → redirect to /404
```

---

## Auth Flow

```
User submits login form
    │
    ▼
AuthService.login()
    │  POST /api/auth/login
    ▼
Backend returns { accessToken, refreshToken, ... }
    │
    ├─ accessToken  → stored in memory (TokenService.accessToken)
    └─ refreshToken → stored in localStorage
    │
    ▼
AuthService.loadProfile()
    │  GET /api/auth/me
    ▼
currentUser signal set → UI updates reactively

─────────────────────────────────────────
On every API request:
  authInterceptor adds Authorization: Bearer <accessToken>

On 401 response:
  ├─ Try POST /api/auth/refresh
  │   ├─ Success → store new tokens, replay original request
  │   └─ Failure → clearAuthState(), redirect to /auth/login
  └─ Queue concurrent requests until refresh completes

On logout:
  POST /api/auth/logout → revoke backend tokens
  clearAuthState() → signals cleared, localStorage cleared
  Router → /auth/login
```

---

## RBAC

Route-level: declare `data: { roles: [UserRole.ADMIN, ...] }` + add `roleGuard` to `canActivate`.

Template-level:
```html
<button *appHasRole="'ROLE_ADMIN'">Admin only</button>
<div *appHasRole="[UserRole.ADMIN, UserRole.FEDERATION_STAFF]">Staff tools</div>
```

Service-level:
```ts
if (this.auth.hasRole(UserRole.ADMIN)) { ... }
if (this.auth.hasAnyRole([UserRole.ADMIN, UserRole.FEDERATION_STAFF])) { ... }
```

---

## HTTP API Strategy

All HTTP calls go through `ApiService` which:
1. Prefixes every path with `environment.apiBaseUrl` (`/api`)
2. Auto-unwraps the `ApiResponse<T>` envelope — callers receive `T` directly
3. Provides typed helpers: `get<T>`, `post<T>`, `put<T>`, `patch<T>`, `delete<T>`, `getPaged<T>`

Example service:
```ts
@Injectable({ providedIn: 'root' })
export class ClubService {
  constructor(private api: ApiService) {}

  getAll(params?: { page?: number; size?: number }) {
    return this.api.getPaged<Club>('/clubs', params);
  }

  getById(id: string) {
    return this.api.get<Club>(`/clubs/${id}`);
  }

  create(data: CreateClubRequest) {
    return this.api.post<Club>('/clubs', data);
  }
}
```

---

## Theme Customisation

Edit `tailwind.config.js` → `theme.extend.colors.primary` to change the brand colour.
Edit `src/styles/material-theme.scss` to update Angular Material's palette.

Dark mode toggle: `ThemeService.toggle()` — adds/removes `.dark` on `<html>`.

---

## Local Development Credentials

| Email                              | Password    | Role                  |
|------------------------------------|-------------|-----------------------|
| admin@federation.local             | Admin@1234  | ROLE_ADMIN            |
| staff@federation.local             | Test@1234   | ROLE_FEDERATION_STAFF |
| manager.esperance@federation.local | Test@1234   | ROLE_CLUB_MANAGER     |
| athlete.ferjani@federation.local   | Test@1234   | ROLE_ATHLETE          |

---

## Build for Production

```bash
npm run build:prod
# Output: dist/federation-frontend/
# Serve with nginx — all routes rewrite to index.html
```

nginx snippet:
```nginx
location / {
  try_files $uri $uri/ /index.html;
}
location /api {
  proxy_pass http://localhost:8080;
}
```
