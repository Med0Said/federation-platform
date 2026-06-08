-- ============================================================
-- V1__init_schema.sql
-- Initial database schema for Sports Federation Platform
-- ============================================================

-- ===== EXTENSIONS =====
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- ===== ENUMS =====
CREATE TYPE user_role AS ENUM ('ROLE_ADMIN', 'ROLE_FEDERATION_STAFF', 'ROLE_CLUB_MANAGER', 'ROLE_ATHLETE', 'ROLE_PUBLIC');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION');
CREATE TYPE gender AS ENUM ('MALE', 'FEMALE', 'OTHER');

-- ===== USERS TABLE =====
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    username    VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    role        user_role    NOT NULL DEFAULT 'ROLE_PUBLIC',
    status      user_status  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    phone       VARCHAR(20),
    avatar_url  VARCHAR(500),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_login  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_users_email    ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role     ON users(role);
CREATE INDEX idx_users_status   ON users(status);

-- ===== REFRESH TOKENS TABLE =====
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token   ON refresh_tokens(token);

-- ===== CLUBS TABLE (placeholder for module) =====
CREATE TABLE clubs (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(200) NOT NULL UNIQUE,
    short_name  VARCHAR(10),
    city        VARCHAR(100),
    founded_year INTEGER,
    logo_url    VARCHAR(500),
    website     VARCHAR(300),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_clubs_name   ON clubs(name);
CREATE INDEX idx_clubs_active ON clubs(active);

-- ===== ATHLETES TABLE (placeholder for module) =====
CREATE TABLE athletes (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    club_id         UUID REFERENCES clubs(id) ON DELETE SET NULL,
    license_number  VARCHAR(50) NOT NULL UNIQUE,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    date_of_birth   DATE,
    gender          gender,
    nationality     VARCHAR(100),
    weight_kg       DECIMAL(5, 2),
    height_cm       DECIMAL(5, 2),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_athletes_license  ON athletes(license_number);
CREATE INDEX idx_athletes_club     ON athletes(club_id);
CREATE INDEX idx_athletes_user     ON athletes(user_id);

-- ===== COMPETITIONS TABLE (placeholder for module) =====
CREATE TABLE competitions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    location        VARCHAR(200),
    start_date      DATE,
    end_date        DATE,
    max_participants INTEGER,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_competitions_start_date ON competitions(start_date);
CREATE INDEX idx_competitions_active     ON competitions(active);

-- ===== NEWS TABLE (placeholder for module) =====
CREATE TABLE news (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title       VARCHAR(300) NOT NULL,
    slug        VARCHAR(300) NOT NULL UNIQUE,
    content     TEXT NOT NULL,
    excerpt     TEXT,
    author_id   UUID REFERENCES users(id) ON DELETE SET NULL,
    published   BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP WITH TIME ZONE,
    cover_url   VARCHAR(500),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_news_slug       ON news(slug);
CREATE INDEX idx_news_published  ON news(published);
CREATE INDEX idx_news_author     ON news(author_id);

-- ===== AUDIT TRIGGER FUNCTION =====
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to all tables with updated_at
CREATE TRIGGER trg_users_updated_at         BEFORE UPDATE ON users         FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_clubs_updated_at         BEFORE UPDATE ON clubs         FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_athletes_updated_at      BEFORE UPDATE ON athletes      FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_competitions_updated_at  BEFORE UPDATE ON competitions   FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_news_updated_at          BEFORE UPDATE ON news           FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
