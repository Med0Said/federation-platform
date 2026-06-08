-- ============================================================
-- V4__enums.sql
--
-- All application-wide PostgreSQL enum types.
-- Grouped by module for readability.
-- ============================================================

-- ===== MODULE: users (already exist from V1 — guard with IF NOT EXISTS workaround) =====
-- user_role and user_status were created in V1; only add new ones here.

-- ===== MODULE: clubs =====
CREATE TYPE club_status AS ENUM (
    'ACTIVE',
    'SUSPENDED',
    'DISSOLVED'
);

-- ===== MODULE: athletes =====
-- gender already exists from V1
CREATE TYPE athlete_status AS ENUM (
    'ACTIVE',
    'INACTIVE',
    'INJURED',
    'RETIRED',
    'SUSPENDED'
);

CREATE TYPE athlete_category AS ENUM (
    'YOUTH',        -- under 18
    'JUNIOR',       -- 18–20
    'SENIOR',       -- 21–34
    'MASTERS',      -- 35–49
    'GRAND_MASTERS' -- 50+
);

-- ===== MODULE: competitions =====
CREATE TYPE competition_status AS ENUM (
    'DRAFT',
    'PUBLISHED',
    'REGISTRATION_OPEN',
    'REGISTRATION_CLOSED',
    'ONGOING',
    'COMPLETED',
    'CANCELLED'
);

CREATE TYPE competition_level AS ENUM (
    'LOCAL',
    'REGIONAL',
    'NATIONAL',
    'INTERNATIONAL'
);

CREATE TYPE competition_format AS ENUM (
    'INDIVIDUAL',
    'TEAM',
    'RELAY',
    'MIXED'
);

-- ===== MODULE: competition_registrations =====
CREATE TYPE registration_status AS ENUM (
    'PENDING',
    'CONFIRMED',
    'WAITLISTED',
    'CANCELLED',
    'DISQUALIFIED'
);

-- ===== MODULE: results =====
CREATE TYPE result_status AS ENUM (
    'UNOFFICIAL',
    'OFFICIAL',
    'PROTESTED',
    'DISQUALIFIED',
    'DNS',   -- Did Not Start
    'DNF',   -- Did Not Finish
    'DSQ'    -- Disqualified
);

CREATE TYPE medal_type AS ENUM (
    'GOLD',
    'SILVER',
    'BRONZE'
);

-- ===== MODULE: news =====
CREATE TYPE news_status AS ENUM (
    'DRAFT',
    'REVIEW',
    'PUBLISHED',
    'ARCHIVED'
);

CREATE TYPE news_category AS ENUM (
    'GENERAL',
    'COMPETITION',
    'ATHLETE',
    'CLUB',
    'ANNOUNCEMENT',
    'PRESS_RELEASE'
);
