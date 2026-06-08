-- ============================================================
-- V6__athletes_schema.sql
--
-- Module: athletes
--
-- Tables:
--   athletes              – registered federation athletes
--   athlete_club_history  – audit trail of club memberships over time
--   athlete_documents     – scanned docs (ID, medical clearance, etc.)
--
-- Relationships:
--   athletes.user_id  → users.id   (optional linked account)
--   athletes.club_id  → clubs.id   (current club)
--   athlete_club_history.athlete_id → athletes.id
--   athlete_club_history.club_id    → clubs.id
-- ============================================================

-- ============================================================
-- TABLE: athletes
-- ============================================================
CREATE TABLE athletes (
    id              UUID             PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Linked user account (optional — athletes may not have a portal login)
    user_id         UUID             UNIQUE REFERENCES users(id) ON DELETE SET NULL,

    -- Current club affiliation
    club_id         UUID             REFERENCES clubs(id) ON DELETE SET NULL,

    -- Federation identity
    license_number  VARCHAR(50)      NOT NULL UNIQUE,
    license_expiry  DATE,

    -- Personal info
    first_name      VARCHAR(100)     NOT NULL,
    last_name       VARCHAR(100)     NOT NULL,
    date_of_birth   DATE             NOT NULL,
    gender          gender           NOT NULL,
    nationality     VARCHAR(100)     NOT NULL DEFAULT 'Tunisian',
    country_code    VARCHAR(3)       NOT NULL DEFAULT 'TUN',   -- IOC 3-letter code

    -- Physical (optional, sport-specific)
    weight_kg       NUMERIC(5, 2)    CHECK (weight_kg > 0 AND weight_kg < 500),
    height_cm       NUMERIC(5, 2)    CHECK (height_cm > 0 AND height_cm < 300),

    -- Classification
   category        athlete_category,

    -- Contact
    email           VARCHAR(255),
    phone           VARCHAR(30),
    emergency_contact_name  VARCHAR(200),
    emergency_contact_phone VARCHAR(30),

    -- Medical
    medical_clearance_date  DATE,
    medical_notes           TEXT,

    -- Status & lifecycle
    status          athlete_status   NOT NULL DEFAULT 'ACTIVE',
    photo_url       VARCHAR(500),
    notes           TEXT,

    -- Timestamps
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_athletes_dob       CHECK (date_of_birth <= CURRENT_DATE - INTERVAL '5 years'),
    CONSTRAINT chk_athletes_license_expiry CHECK (license_expiry IS NULL OR license_expiry >= date_of_birth)
);

-- Indexes
CREATE INDEX idx_athletes_license       ON athletes(license_number);
CREATE INDEX idx_athletes_club          ON athletes(club_id);
CREATE INDEX idx_athletes_user          ON athletes(user_id);
CREATE INDEX idx_athletes_status        ON athletes(status);
CREATE INDEX idx_athletes_category      ON athletes(category);
CREATE INDEX idx_athletes_gender        ON athletes(gender);
CREATE INDEX idx_athletes_country       ON athletes(country_code);
CREATE INDEX idx_athletes_dob           ON athletes(date_of_birth);
CREATE INDEX idx_athletes_name          ON athletes(last_name, first_name);
-- Partial index for active athletes (most common query target)
CREATE INDEX idx_athletes_active        ON athletes(club_id, status) WHERE status = 'ACTIVE';

CREATE TRIGGER trg_athletes_updated_at
    BEFORE UPDATE ON athletes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


-- ============================================================
-- TABLE: athlete_club_history
-- Immutable audit log — never delete rows, only INSERT.
-- ============================================================
CREATE TABLE athlete_club_history (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    athlete_id  UUID        NOT NULL REFERENCES athletes(id) ON DELETE CASCADE,
    club_id     UUID        REFERENCES clubs(id) ON DELETE SET NULL,   -- NULL = no club (independent)
    club_name   VARCHAR(200),   -- denormalized snapshot in case club is later deleted
    start_date  DATE        NOT NULL,
    end_date    DATE,           -- NULL = current membership
    transfer_reason TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_club_history_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_club_history_athlete ON athlete_club_history(athlete_id);
CREATE INDEX idx_club_history_club    ON athlete_club_history(club_id);
CREATE INDEX idx_club_history_dates   ON athlete_club_history(start_date, end_date);
-- Only one open membership per athlete at a time
CREATE UNIQUE INDEX uq_club_history_open ON athlete_club_history(athlete_id) WHERE end_date IS NULL;


-- ============================================================
-- TABLE: athlete_documents
-- ============================================================
CREATE TABLE athlete_documents (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    athlete_id      UUID        NOT NULL REFERENCES athletes(id) ON DELETE CASCADE,
    document_type   VARCHAR(80) NOT NULL,     -- e.g. 'ID_CARD', 'MEDICAL_CLEARANCE', 'PHOTO'
    file_url        VARCHAR(500) NOT NULL,
    file_name       VARCHAR(255),
    mime_type       VARCHAR(100),
    expiry_date     DATE,
    uploaded_by     UUID        REFERENCES users(id) ON DELETE SET NULL,
    verified        BOOLEAN     NOT NULL DEFAULT FALSE,
    verified_by     UUID        REFERENCES users(id) ON DELETE SET NULL,
    verified_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_athlete_docs_athlete  ON athlete_documents(athlete_id);
CREATE INDEX idx_athlete_docs_type     ON athlete_documents(document_type);
CREATE INDEX idx_athlete_docs_expiry   ON athlete_documents(expiry_date) WHERE expiry_date IS NOT NULL;
