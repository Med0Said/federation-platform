-- ============================================================
-- V5__clubs_schema.sql
--
-- Module: clubs
--
-- Tables:
--   clubs           – federation-registered clubs
--   club_officials  – club staff/manager linkage to users
--
-- Relationships:
--   clubs.manager_id  → users.id  (primary manager account)
--   club_officials.club_id → clubs.id
--   club_officials.user_id → users.id
-- ============================================================

-- ============================================================
-- TABLE: clubs
-- ============================================================
CREATE TABLE clubs (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Identity
    name            VARCHAR(200) NOT NULL,
    short_name      VARCHAR(20),
    slug            VARCHAR(220) NOT NULL UNIQUE,          -- URL-safe unique identifier
    license_number  VARCHAR(50)  NOT NULL UNIQUE,          -- federation-issued license

    -- Location
    city            VARCHAR(100),
    region          VARCHAR(100),
    country         VARCHAR(100) NOT NULL DEFAULT 'TN',    -- ISO 3166-1 alpha-2
    address         TEXT,
    postal_code     VARCHAR(20),

    -- Details
    founded_year    SMALLINT     CHECK (founded_year BETWEEN 1800 AND EXTRACT(YEAR FROM NOW())::INT + 1),
    description     TEXT,
    logo_url        VARCHAR(500),
    website         VARCHAR(300),
    email           VARCHAR(255),
    phone           VARCHAR(30),

    -- Management
    manager_id      UUID         REFERENCES users(id) ON DELETE SET NULL,

    -- Status & lifecycle
    status          club_status  NOT NULL DEFAULT 'ACTIVE',

    -- Timestamps
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_clubs_short_name_upper CHECK (short_name = UPPER(short_name))
);

-- Indexes
CREATE INDEX idx_clubs_slug           ON clubs(slug);
CREATE INDEX idx_clubs_license        ON clubs(license_number);
CREATE INDEX idx_clubs_status         ON clubs(status);
CREATE INDEX idx_clubs_manager        ON clubs(manager_id);
CREATE INDEX idx_clubs_country_region ON clubs(country, region);
-- CREATE INDEX idx_clubs_name_trgm      ON clubs USING gin(name gin_trgm_ops);    -- fuzzy search (requires pg_trgm)

-- Auto-update trigger
CREATE TRIGGER trg_clubs_updated_at
    BEFORE UPDATE ON clubs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


-- ============================================================
-- TABLE: club_officials
-- A user can hold multiple roles across clubs (or the same club).
-- ============================================================
CREATE TABLE club_officials (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    club_id     UUID        NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(100) NOT NULL,                   -- e.g. 'President', 'Coach', 'Secretary'
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    start_date  DATE,
    end_date    DATE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_club_officials_dates CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT uq_club_officials_active  UNIQUE NULLS NOT DISTINCT (club_id, user_id, title, active)
                                                       -- prevents duplicate active assignments
);

CREATE INDEX idx_club_officials_club   ON club_officials(club_id);
CREATE INDEX idx_club_officials_user   ON club_officials(user_id);
CREATE INDEX idx_club_officials_active ON club_officials(active);

CREATE TRIGGER trg_club_officials_updated_at
    BEFORE UPDATE ON club_officials
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
