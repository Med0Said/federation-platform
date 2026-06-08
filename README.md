# 🏆 Sports Federation Platform

A full-stack national sports federation management system built with **Spring Boot 3** and **Angular 19**. Manages athletes, clubs, competitions, results, and news through a role-based web interface.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Local Setup](#local-setup)
- [Default Accounts](#default-accounts)
- [API Documentation](#api-documentation)
- [Environment Variables](#environment-variables)
- [Database Migrations](#database-migrations)
- [Role-Based Access Control](#role-based-access-control)
- [Screenshots](#screenshots)

---

## Overview

The Sports Federation Platform is a production-grade web application designed for national sports federations to manage their entire ecosystem — from athlete registration and club management to competition scheduling and official results publishing.

### Key capabilities

- **Athlete registry** — license management, categories, club affiliations, medical records
- **Club management** — registration, status tracking, roster management
- **Competition lifecycle** — from draft creation through registration, scheduling, and results
- **Official results** — performance recording, rankings, medals, national records
- **News publishing** — articles, press releases, announcements with tagging
- **Role-based portal** — different dashboards for admins, staff, club managers, and athletes
- **JWT authentication** — stateless auth with access/refresh token rotation

---

## Tech Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 3.2.5 | Application framework |
| Spring Security 6 | 6.2.4 | Authentication & authorization |
| Spring Data JPA | 3.2.5 | Data access layer |
| Hibernate | 6.4.4 | ORM |
| PostgreSQL | 16 | Primary database |
| Flyway | 9.22.3 | Database migrations |
| JJWT | 0.12.5 | JWT token library |
| MapStruct | 1.5.5 | DTO mapping |
| Lombok | 1.18.32 | Boilerplate reduction |
| SpringDoc OpenAPI | 2.5.0 | Swagger UI / API docs |
| Maven | 3.9+ | Build tool |

### Frontend
| Technology | Version | Purpose |
|---|---|---|
| Angular | 19 | SPA framework |
| TypeScript | 5.5 | Language |
| Angular Material | 19 | UI component library |
| Tailwind CSS | 3.4 | Utility-first styling |
| RxJS | 7.8 | Reactive state management |
| Angular Signals | 19 | Fine-grained reactivity |

### Infrastructure
| Technology | Purpose |
|---|---|
| Docker & Docker Compose | PostgreSQL container |
| pgAdmin 4 | Database browser (optional) |

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Angular 19 SPA                        │
│         (localhost:4200 → proxy → :8080/api)            │
│                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────┐  │
│  │  Auth    │  │  Admin   │  │  Portal  │  │ Public │  │
│  │  Module  │  │ Dashboard│  │ (Athlete)│  │ Pages  │  │
│  └──────────┘  └──────────┘  └──────────┘  └────────┘  │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP + JWT
┌────────────────────────▼────────────────────────────────┐
│               Spring Boot 3 REST API                     │
│                  (localhost:8080/api)                    │
│                                                          │
│  ┌──────┐ ┌───────┐ ┌──────────┐ ┌────────┐ ┌───────┐  │
│  │ Auth │ │Athlete│ │   Club   │ │Competi-│ │ News  │  │
│  │      │ │       │ │          │ │  tion  │ │       │  │
│  └──────┘ └───────┘ └──────────┘ └────────┘ └───────┘  │
│                                                          │
│  Spring Security Filter Chain → JWT Validation          │
│  Flyway Migrations → JPA/Hibernate → HikariCP           │
└────────────────────────┬────────────────────────────────┘
                         │ JDBC
┌────────────────────────▼────────────────────────────────┐
│           PostgreSQL 16 (Docker container)               │
│              federation_db  port:5432                    │
│                                                          │
│  users  clubs  athletes  competitions  results  news     │
└─────────────────────────────────────────────────────────┘
```

---

## Features

### Authentication
- JWT access + refresh token pair
- Automatic token refresh on expiry
- Secure logout with server-side token revocation
- BCrypt password hashing

### Modules
| Module | Endpoints | Description |
|---|---|---|
| Auth | `/api/auth/**` | Login, register, refresh, logout, profile |
| Athletes | `/api/athletes/**` | CRUD, filtering by status/gender/category/club |
| Clubs | `/api/clubs/**` | CRUD, search, slug-based lookup |
| Competitions | `/api/competitions/**` | Full lifecycle from draft to completed |
| Results | `/api/results/**` | Performance recording, rankings, medals |
| News | `/api/news/**` | Articles with tags, publishing workflow |
| Users | `/api/users/**` | Admin user management |

---

## Prerequisites

Install all of the following before starting:

| Tool | Minimum Version | Download |
|---|---|---|
| **Java JDK** | 21 | https://adoptium.net (Temurin 21 LTS) |
| **Apache Maven** | 3.9 | https://maven.apache.org/download.cgi |
| **Node.js** | 20 LTS | https://nodejs.org |
| **npm** | 10 | Bundled with Node.js |
| **Docker Desktop** | 4.x | https://www.docker.com/products/docker-desktop |
| **Git** | Any | https://git-scm.com |

### Verify your installation

```bash
java -version        # Should show 21.x.x
mvn -version         # Should show 3.9.x
node -v              # Should show 20.x.x or higher
npm -v               # Should show 10.x.x or higher
docker --version     # Should show 24.x.x or higher
```

---

## Project Structure

```
federation-platform/
│
├── sports-federation/              ← Spring Boot backend
│   ├── src/main/java/com/federation/
│   │   ├── auth/                   ← JWT auth, login, register, refresh
│   │   ├── athletes/               ← Athlete entity, CRUD, filtering
│   │   ├── clubs/                  ← Club entity, CRUD, slug management
│   │   ├── competitions/           ← Competition + events lifecycle
│   │   ├── results/                ← Result recording + rankings
│   │   ├── news/                   ← News articles + tags
│   │   ├── users/                  ← User management (admin)
│   │   └── common/                 ← Config, exceptions, response wrappers
│   ├── src/main/resources/
│   │   ├── application.yml         ← Main configuration
│   │   └── db/migration/           ← 12 Flyway SQL migrations
│   ├── docker-compose.yml          ← PostgreSQL + pgAdmin services
│   └── pom.xml
│
└── federation-frontend/            ← Angular 19 frontend
    ├── src/app/
    │   ├── core/                   ← Services, guards, interceptors, models
    │   ├── shared/                 ← Reusable components, pipes, directives
    │   ├── layouts/                ← Public and admin layout shells
    │   └── features/               ← Lazy-loaded feature pages
    │       ├── auth/               ← Login, register, forgot password
    │       ├── dashboard/          ← Admin dashboard
    │       ├── athletes/           ← Athlete list, detail, form
    │       ├── clubs/              ← Club list, detail, form
    │       ├── competitions/       ← Competition management
    │       ├── results/            ← Results and rankings
    │       ├── news/               ← News management
    │       ├── users/              ← User management
    │       └── portal/             ← Athlete self-service portal
    ├── proxy.conf.json             ← Dev proxy → :8080
    ├── tailwind.config.js
    └── package.json
```

---

## Local Setup

### Step 1 — Clone the repository

```bash
git clone https://github.com/your-username/federation-platform.git
cd federation-platform
```

### Step 2 — Fix CORS for Angular dev server

Open `sports-federation/src/main/resources/application.yml` and add port `4200`:

```yaml
app:
  cors:
    allowed-origins:
      - "http://localhost:3000"
      - "http://localhost:5173"
      - "http://localhost:4200"    # ← add this
```

### Step 3 — Start the database

```bash
cd sports-federation
docker compose up -d postgres
```

Wait ~10 seconds, then verify it's healthy:

```bash
docker compose ps
# Should show: federation_postgres   Up (healthy)
```

### Step 4 — Start the backend

```bash
# Still inside sports-federation/
mvn spring-boot:run -DskipTests
```

Watch for this line — it means everything is ready:

```
Started SportsFederationApplication in X.XXX seconds
Tomcat started on port 8080 with context path '/api'
Successfully applied 12 migrations to schema "public"
```

> **What happens on first run:** Flyway automatically creates all tables, enums, indexes and seeds test data including admin accounts. No manual SQL is needed.

### Step 5 — Install frontend dependencies

```bash
cd ../federation-frontend
npm install
```

### Step 6 — Start the frontend

```bash
npm start
```

Wait for:

```
** Angular Live Development Server is listening on localhost:4200 **
→ Local:   http://localhost:4200/
```

### Step 7 — Open the app

Navigate to **http://localhost:4200** and log in with the admin account below.

---

## Default Accounts

Seeded automatically on first startup by `V2` and `V12` Flyway migrations.

| Email | Password | Role | Access |
|---|---|---|---|
| `admin@federation.local` | `Admin@1234` | ROLE_ADMIN | Full platform access |
| `staff@federation.local` | `Test@1234` | ROLE_FEDERATION_STAFF | All except user management |
| `manager.esperance@federation.local` | `Test@1234` | ROLE_CLUB_MANAGER | Clubs & athletes only |
| `manager.sfax@federation.local` | `Test@1234` | ROLE_CLUB_MANAGER | Clubs & athletes only |
| `athlete.ferjani@federation.local` | `Test@1234` | ROLE_ATHLETE | Self-service portal |
| `athlete.ayari@federation.local` | `Test@1234` | ROLE_ATHLETE | Self-service portal |

---

## API Documentation

Once the backend is running, Swagger UI is available at:

```
http://localhost:8080/api/swagger-ui.html
```

### Quick test with curl

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin@federation.local","password":"Admin@1234"}'

# Use the returned accessToken for protected endpoints
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <your_access_token>"

# Get athletes
curl http://localhost:8080/api/athletes?page=0&size=10 \
  -H "Authorization: Bearer <your_access_token>"
```

### API Response format

All endpoints return a consistent envelope:

```json
{
  "success": true,
  "data": { ... },
  "message": "Operation successful.",
  "timestamp": "2026-06-07T12:00:00Z"
}
```

Paginated list responses:

```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 25,
    "totalElements": 120,
    "totalPages": 5,
    "first": true,
    "last": false
  }
}
```

---

## Environment Variables

All configuration is in `sports-federation/src/main/resources/application.yml`.

| Property | Default | Description |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/federation_db` | Database URL |
| `spring.datasource.username` | `federation_user` | DB username |
| `spring.datasource.password` | `federation_pass` | DB password |
| `app.jwt.secret` | *(see yml)* | JWT signing secret — **change in production** |
| `app.jwt.access-token-expiry` | `86400000` (24h) | Access token TTL in ms |
| `app.jwt.refresh-token-expiry` | `604800000` (7d) | Refresh token TTL in ms |
| `server.port` | `8080` | Backend port |

For production, override these via environment variables or a separate `application-prod.yml`.

---

## Database Migrations

Managed by Flyway. Files are in `src/main/resources/db/migration/` and run automatically on startup.

| Migration | Description |
|---|---|
| V1 | Core schema — `users`, `refresh_tokens`, PostgreSQL extensions |
| V2 | Seed admin user (`admin@federation.local`) |
| V3 | Drop placeholder stub tables |
| V4 | All custom PostgreSQL enum types (17 enums) |
| V5 | `clubs` table + indexes |
| V6 | `athletes` table + indexes |
| V7 | `competitions` + `competition_events` tables |
| V8 | `competition_registrations` table |
| V9 | `results` + `rankings` tables |
| V10 | `news` + `tags` + `news_tags` tables |
| V11 | 6 optimised read views |
| V12 | Full seed dataset — clubs, athletes, competitions, results, news |

### Reset the database

```bash
cd sports-federation
docker compose down -v        # Deletes all data
docker compose up -d postgres  # Fresh container
# Then restart the backend — Flyway re-runs all migrations
```

---

## Role-Based Access Control

| Role | Dashboard | Users | Clubs | Athletes | Competitions | Results | News | Portal |
|---|---|---|---|---|---|---|---|---|
| `ROLE_ADMIN` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — |
| `ROLE_FEDERATION_STAFF` | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ | — |
| `ROLE_CLUB_MANAGER` | ✅ | ❌ | Own club | Own athletes | View only | View only | ❌ | — |
| `ROLE_ATHLETE` | — | — | — | — | — | — | — | ✅ |
| Public | — | — | View | View | View | View | View | — |

---

## Service URLs

| Service | URL | Notes |
|---|---|---|
| Angular App | http://localhost:4200 | Main frontend |
| Spring Boot API | http://localhost:8080/api | REST backend |
| Swagger UI | http://localhost:8080/api/swagger-ui.html | Interactive API docs |
| Health Check | http://localhost:8080/api/actuator/health | Returns `{"status":"UP"}` |
| pgAdmin | http://localhost:5050 | DB browser (if enabled) |

### Enable pgAdmin

```bash
cd sports-federation
docker compose --profile tools up -d pgadmin
```

Login at http://localhost:5050 with `admin@federation.local` / `admin`, then connect to host `postgres`, port `5432`, database `federation_db`.

---

## Stopping Everything

```bash
# Ctrl+C in the Angular terminal
# Ctrl+C in the Spring Boot terminal

# Stop the database
cd sports-federation
docker compose down
```

---

## Common Issues

| Problem | Solution |
|---|---|
| `Port 5432 already in use` | Stop local PostgreSQL or change the port in `docker-compose.yml` |
| `Port 8080 already in use` | Change `server.port` in `application.yml` and update `proxy.conf.json` |
| Login returns `401` | Run `docker compose down -v && docker compose up -d postgres` to reset the database, then restart the backend |
| `npm install` fails with `ERESOLVE` | Run `npm install --legacy-peer-deps` |
| Frontend shows blank page | Check browser console for Angular compile errors — run `npm start` and check terminal output |
| `mvnw: Permission denied` (Linux/Mac) | Run `chmod +x ./mvnw` |

---

## License

This project is licensed under the MIT License.
