# Sports Federation Platform — Backend

A **production-grade modular monolith** built with Spring Boot 3, Java 21, PostgreSQL and JWT authentication.

---

## Architecture Overview

```
sports-federation/
├── src/main/java/com/federation/
│   ├── SportsFederationApplication.java       ← entry point
│   │
│   ├── common/                                ← shared kernel (no business logic)
│   │   ├── config/
│   │   │   ├── SecurityConfig.java            ← Spring Security filter chain
│   │   │   ├── JwtAuthenticationFilter.java   ← JWT extraction & validation per request
│   │   │   ├── JwtAuthEntryPoint.java         ← 401 JSON responses
│   │   │   ├── JwtAccessDeniedHandler.java    ← 403 JSON responses
│   │   │   ├── JwtProperties.java             ← app.jwt.* binding
│   │   │   ├── CorsProperties.java            ← app.cors.* binding
│   │   │   ├── JpaConfig.java                 ← auditing, AuditorAware
│   │   │   └── OpenApiConfig.java             ← Swagger / SpringDoc setup
│   │   ├── exception/
│   │   │   ├── FederationException.java       ← base exception
│   │   │   ├── BadRequestException.java
│   │   │   ├── ResourceNotFoundException.java
│   │   │   ├── ResourceAlreadyExistsException.java
│   │   │   ├── UnauthorizedException.java
│   │   │   ├── ForbiddenException.java
│   │   │   ├── BusinessRuleViolationException.java
│   │   │   └── GlobalExceptionHandler.java    ← @RestControllerAdvice
│   │   ├── response/
│   │   │   ├── ApiResponse.java               ← unified response envelope
│   │   │   ├── PagedResponse.java             ← pagination wrapper
│   │   │   └── ValidationErrorResponse.java   ← structured 400 errors
│   │   └── util/
│   │       ├── BaseEntity.java                ← UUID PK + audit timestamps
│   │       ├── JwtTokenUtil.java              ← token generation & validation
│   │       └── TokenCleanupTask.java          ← nightly token purge
│   │
│   ├── auth/                                  ← authentication module
│   │   ├── controller/AuthController.java
│   │   ├── service/AuthService.java
│   │   ├── service/AuthHelperService.java
│   │   ├── dto/AuthDtos.java
│   │   ├── entity/RefreshToken.java
│   │   └── repository/RefreshTokenRepository.java
│   │
│   ├── users/                                 ← user management module
│   │   ├── entity/User.java
│   │   ├── entity/UserRole.java
│   │   ├── entity/UserStatus.java
│   │   ├── repository/UserRepository.java
│   │   ├── dto/UserDtos.java
│   │   └── service/UserDetailsServiceImpl.java
│   │
│   ├── clubs/         ← stub (schema + entity ready)
│   ├── athletes/      ← stub (schema + entity ready)
│   ├── competitions/  ← stub (schema + entity ready)
│   └── news/          ← stub (schema + entity ready)
│
└── src/main/resources/
    ├── application.yml
    ├── application-test.yml
    └── db/migration/
        ├── V1__init_schema.sql     ← full schema (all tables, enums, indexes, triggers)
        └── V2__seed_admin_user.sql ← default admin account
```

---

## Prerequisites

| Tool          | Version   |
|---------------|-----------|
| Java (JDK)    | 21+       |
| Maven         | 3.9+      |
| Docker        | 24+       |
| Docker Compose| 2.x       |

---

## Local Development — Quick Start

### 1. Clone and enter the project

```bash
git clone <repo-url>
cd sports-federation
```

### 2. Start PostgreSQL with Docker Compose

```bash
docker compose up -d postgres
```

Verify it is healthy:

```bash
docker compose ps
# federation_postgres should show: healthy
```

### 3. (Optional) Start pgAdmin for DB exploration

```bash
docker compose --profile tools up -d pgadmin
# Open http://localhost:5050
# Login: admin@federation.local / admin
# Server: host=postgres, port=5432, db=federation_db, user=federation_user
```

### 4. Build the project

```bash
./mvnw clean package -DskipTests
```

### 5. Run the application

```bash
./mvnw spring-boot:run
```

Or with the packaged JAR:

```bash
java -jar target/sports-federation-1.0.0-SNAPSHOT.jar
```

The application starts on **http://localhost:8080/api**

Flyway automatically runs all migrations on startup. The schema and seed admin user are created on first run.

---

## Environment Configuration

All sensitive values should be overridden via environment variables in non-local environments:

| Variable                        | Default (local)                         | Description              |
|---------------------------------|-----------------------------------------|--------------------------|
| `SPRING_DATASOURCE_URL`         | `jdbc:postgresql://localhost:5432/federation_db` | JDBC URL    |
| `SPRING_DATASOURCE_USERNAME`    | `federation_user`                       | DB username              |
| `SPRING_DATASOURCE_PASSWORD`    | `federation_pass`                       | DB password              |
| `APP_JWT_SECRET`                | (hex string in yml)                     | **Must override in prod**|
| `APP_JWT_EXPIRATION_MS`         | `86400000` (24h)                        | Access token TTL         |
| `APP_JWT_REFRESH_EXPIRATION_MS` | `604800000` (7d)                        | Refresh token TTL        |
| `SERVER_PORT`                   | `8080`                                  | HTTP port                |

Example override:

```bash
APP_JWT_SECRET=your-256bit-base64-secret ./mvnw spring-boot:run
```

---

## API Documentation

Swagger UI is available at:

```
http://localhost:8080/api/swagger-ui.html
```

OpenAPI JSON spec:

```
http://localhost:8080/api/v3/api-docs
```

---

## Available Auth Endpoints

| Method | Path              | Auth required | Description                        |
|--------|-------------------|---------------|------------------------------------|
| POST   | /auth/register    | No            | Create a new account               |
| POST   | /auth/login       | No            | Obtain access + refresh tokens     |
| POST   | /auth/refresh     | No            | Exchange refresh token for new pair|
| POST   | /auth/logout      | Yes (Bearer)  | Revoke all tokens for current user |

### Example: Register

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email":     "jane@example.com",
    "username":  "jane_doe",
    "password":  "Secret@123",
    "firstName": "Jane",
    "lastName":  "Doe"
  }'
```

### Example: Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin@federation.local","password":"Admin@1234"}'
```

Copy the `accessToken` from the response, then use it as:

```bash
curl http://localhost:8080/api/users \
  -H "Authorization: Bearer <accessToken>"
```

---

## Running Tests

```bash
# All tests (uses H2 in-memory DB)
./mvnw test

# Specific test class
./mvnw test -Dtest=AuthControllerIntegrationTest
```

---

## Default Seeded Credentials

| Field    | Value                    |
|----------|--------------------------|
| Email    | admin@federation.local   |
| Password | Admin@1234               |
| Role     | ROLE_ADMIN               |

> Change this immediately after first login in any shared environment.

---

## Adding a New Module (e.g. clubs)

1. Create entities extending `BaseEntity` in `com.federation.clubs.entity`
2. Add a repository extending `JpaRepository` in `com.federation.clubs.repository`
3. Define DTOs (request/response) in `com.federation.clubs.dto`
4. Implement service logic in `com.federation.clubs.service`
5. Expose endpoints in `com.federation.clubs.controller`
6. Add a new Flyway migration if the schema changes: `V3__clubs_module.sql`
7. Secure endpoints with `@PreAuthorize("hasRole('ROLE_ADMIN')")` as needed

---

## Health Check

```bash
curl http://localhost:8080/api/actuator/health
```

---

## Tech Stack

| Layer          | Technology                    |
|----------------|-------------------------------|
| Runtime        | Java 21 (virtual threads ready)|
| Framework      | Spring Boot 3.2               |
| Security       | Spring Security 6 + JJWT 0.12 |
| Persistence    | Spring Data JPA + Hibernate 6 |
| Database       | PostgreSQL 16                 |
| Migrations     | Flyway                        |
| Build          | Maven 3.9                     |
| Mapping        | MapStruct                     |
| Docs           | SpringDoc OpenAPI 3           |
| Local DB       | Docker Compose                |
