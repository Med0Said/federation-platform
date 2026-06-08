-- ============================================================
-- V7__competitions_schema.sql
--
-- Module: competitions
--
-- Tables:
--   competitions          – top-level competition event
--   competition_events    – sub-events within a competition (e.g. 100m, 200m)
--   competition_officials – referees, judges, delegates per competition
--
-- Relationships:
--   competitions.organizer_id    → users.id
--   competitions.host_club_id    → clubs.id
--   competition_events.competition_id → competitions.id
--   competition_officials.competition_id → competitions.id
--   competition_officials.user_id        → users.id
-- ============================================================

-- ============================================================
-- TABLE: competitions
-- ============================================================
CREATE TABLE competitions (
    id                  UUID                PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Identity
    name                VARCHAR(200)        NOT NULL,
    slug                VARCHAR(220)        NOT NULL UNIQUE,
    edition             VARCHAR(20),                          -- e.g. '2024', '12th edition'
    description         TEXT,

    -- Classification
    sport               VARCHAR(100)        NOT NULL,         -- e.g. 'Athletics', 'Swimming'
    level               competition_level   NOT NULL DEFAULT 'NATIONAL',
    format              competition_format  NOT NULL DEFAULT 'INDIVIDUAL',

    -- Venue & schedule
    venue_name          VARCHAR(200),
    venue_city          VARCHAR(100),
    venue_country       VARCHAR(100)        NOT NULL DEFAULT 'TN',
    venue_address       TEXT,
    start_date          DATE                NOT NULL,
    end_date            DATE                NOT NULL,
    registration_deadline DATE,

    -- Capacity
    max_participants    INT                 CHECK (max_participants IS NULL OR max_participants > 0),
    max_per_club        INT                 CHECK (max_per_club IS NULL OR max_per_club > 0),

    -- Organisation
    organizer_id        UUID                REFERENCES users(id) ON DELETE SET NULL,
    host_club_id        UUID                REFERENCES clubs(id) ON DELETE SET NULL,

    -- Rules & details
    rules_url           VARCHAR(500),
    poster_url          VARCHAR(500),
    prize_info          TEXT,
    entry_fee           NUMERIC(10, 2)      CHECK (entry_fee IS NULL OR entry_fee >= 0),
    currency            VARCHAR(3)          NOT NULL DEFAULT 'TND',

    -- Status & lifecycle
    status              competition_status  NOT NULL DEFAULT 'DRAFT',
    published_at        TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,
    cancellation_reason TEXT,

    -- Timestamps
    created_at          TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ         NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_competitions_dates    CHECK (end_date >= start_date),
    CONSTRAINT chk_competitions_reg_dl   CHECK (registration_deadline IS NULL OR registration_deadline <= start_date),
    CONSTRAINT chk_competitions_fee      CHECK (entry_fee IS NULL OR entry_fee >= 0)
);

-- Indexes
CREATE INDEX idx_competitions_slug        ON competitions(slug);
CREATE INDEX idx_competitions_status      ON competitions(status);
CREATE INDEX idx_competitions_level       ON competitions(level);
CREATE INDEX idx_competitions_sport       ON competitions(sport);
CREATE INDEX idx_competitions_start_date  ON competitions(start_date);
CREATE INDEX idx_competitions_organizer   ON competitions(organizer_id);
CREATE INDEX idx_competitions_host_club   ON competitions(host_club_id);
-- Composite: typical calendar query
CREATE INDEX idx_competitions_calendar    ON competitions(start_date, end_date, status);

CREATE TRIGGER trg_competitions_updated_at
    BEFORE UPDATE ON competitions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


-- ============================================================
-- TABLE: competition_events
-- A competition can have multiple discipline events.
-- e.g. "National Athletics 2024" → 100m Men, 200m Women, Long Jump…
-- ============================================================
CREATE TABLE competition_events (
    id                  UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    competition_id      UUID        NOT NULL REFERENCES competitions(id) ON DELETE CASCADE,

    -- Identity
    name                VARCHAR(200) NOT NULL,          -- e.g. '100m Men Senior'
    code                VARCHAR(30),                    -- e.g. 'M-100-SR'
    discipline          VARCHAR(100) NOT NULL,          -- e.g. '100m Sprint'
    gender_category     gender,                         -- NULL = mixed/open
    age_category        athlete_category,               -- NULL = open/all categories

    -- Schedule
    scheduled_at        TIMESTAMPTZ,
    duration_minutes    INT         CHECK (duration_minutes IS NULL OR duration_minutes > 0),

    -- Capacity for this event specifically
    max_participants    INT         CHECK (max_participants IS NULL OR max_participants > 0),

    -- Scoring
    scoring_unit        VARCHAR(50),                    -- e.g. 'seconds', 'meters', 'points'
    lower_is_better     BOOLEAN     NOT NULL DEFAULT TRUE,  -- TRUE for time-based, FALSE for distance/points

    -- Status
    status              competition_status NOT NULL DEFAULT 'DRAFT',
    notes               TEXT,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_comp_event_code UNIQUE (competition_id, code)
);

CREATE INDEX idx_comp_events_competition ON competition_events(competition_id);
CREATE INDEX idx_comp_events_discipline  ON competition_events(discipline);
CREATE INDEX idx_comp_events_schedule    ON competition_events(scheduled_at);

CREATE TRIGGER trg_competition_events_updated_at
    BEFORE UPDATE ON competition_events
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


-- ============================================================
-- TABLE: competition_officials
-- Tracks judges, referees, delegates assigned to a competition.
-- ============================================================
CREATE TABLE competition_officials (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    competition_id  UUID        NOT NULL REFERENCES competitions(id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    role            VARCHAR(100) NOT NULL,    -- e.g. 'Chief Referee', 'Judge', 'Technical Delegate'
    event_id        UUID        REFERENCES competition_events(id) ON DELETE SET NULL,  -- specific event or whole competition
    confirmed       BOOLEAN     NOT NULL DEFAULT FALSE,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_comp_official UNIQUE (competition_id, user_id, role)
);

CREATE INDEX idx_comp_officials_competition ON competition_officials(competition_id);
CREATE INDEX idx_comp_officials_user        ON competition_officials(user_id);
