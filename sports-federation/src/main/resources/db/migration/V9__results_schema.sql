-- ============================================================
-- V9__results_schema.sql
--
-- Module: results
--
-- Tables:
--   results           – individual athlete performances per event
--   result_splits     – intermediate split times (e.g. lap times)
--   rankings          – computed standings per competition event
--   records           – all-time / competition records broken
--
-- Relationships:
--   results.registration_id → competition_registrations.id
--   results.event_id        → competition_events.id
--   results.athlete_id      → athletes.id
--   results.recorded_by     → users.id
--   result_splits.result_id → results.id
--   rankings.result_id      → results.id
--   rankings.event_id       → competition_events.id
--   records.result_id       → results.id
--   records.athlete_id      → athletes.id
-- ============================================================

-- ============================================================
-- TABLE: results
-- One row per athlete per event per heat/round.
-- ============================================================
CREATE TABLE results (
    id                  UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Core relationships
    registration_id     UUID            REFERENCES competition_registrations(id) ON DELETE SET NULL,
    competition_id      UUID            NOT NULL REFERENCES competitions(id)      ON DELETE RESTRICT,
    event_id            UUID            NOT NULL REFERENCES competition_events(id) ON DELETE RESTRICT,
    athlete_id          UUID            NOT NULL REFERENCES athletes(id)           ON DELETE RESTRICT,

    -- Round / heat (for multi-round formats)
    round               VARCHAR(50)     NOT NULL DEFAULT 'FINAL',    -- 'HEAT_1', 'SEMI_1', 'FINAL'
    heat_number         INTEGER        CHECK (heat_number IS NULL OR heat_number > 0),
    lane_number         INTEGER        CHECK (lane_number IS NULL OR lane_number > 0),

    -- Performance
    performance_value   NUMERIC(12, 4), -- raw result: seconds, meters, points, etc.
    performance_unit    VARCHAR(30),    -- 'seconds', 'meters', 'points'
    performance_text    VARCHAR(50),    -- human-readable: '9.58', '8.90m', 'DNS'
    wind_speed          NUMERIC(5, 2),  -- m/s (for sprint/jump events)
    points              INT,            -- for multi-event (decathlon, etc.)

    -- Status & validation
    status              result_status   NOT NULL DEFAULT 'UNOFFICIAL',
    disqualification_reason TEXT,
    protest_reason      TEXT,

    -- Record flags
    is_personal_best    BOOLEAN         NOT NULL DEFAULT FALSE,
    is_season_best      BOOLEAN         NOT NULL DEFAULT FALSE,
    is_competition_record BOOLEAN       NOT NULL DEFAULT FALSE,
    is_national_record  BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Admin
    recorded_by         UUID            REFERENCES users(id) ON DELETE SET NULL,
    verified_by         UUID            REFERENCES users(id) ON DELETE SET NULL,
    verified_at         TIMESTAMPTZ,
    notes               TEXT,

    -- Timestamps
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uq_result_athlete_event_round UNIQUE (athlete_id, event_id, round),
    CONSTRAINT chk_result_verified CHECK (
        (verified_by IS NULL AND verified_at IS NULL) OR
        (verified_by IS NOT NULL AND verified_at IS NOT NULL)
    )
);

-- Indexes
CREATE INDEX idx_results_competition ON results(competition_id);
CREATE INDEX idx_results_event       ON results(event_id);
CREATE INDEX idx_results_athlete     ON results(athlete_id);
CREATE INDEX idx_results_status      ON results(status);
CREATE INDEX idx_results_round       ON results(event_id, round);
-- Partial: only official results (most read queries)
CREATE INDEX idx_results_official    ON results(event_id, performance_value)
    WHERE status = 'OFFICIAL';
-- Partial: records
CREATE INDEX idx_results_records     ON results(athlete_id) WHERE is_personal_best OR is_national_record;

CREATE TRIGGER trg_results_updated_at
    BEFORE UPDATE ON results
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


-- ============================================================
-- TABLE: result_splits
-- Intermediate times within a performance (e.g. 50m split in 100m).
-- ============================================================
CREATE TABLE result_splits (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    result_id   UUID        NOT NULL REFERENCES results(id) ON DELETE CASCADE,
    split_label VARCHAR(50) NOT NULL,               -- e.g. '50m', 'Lap 1', 'First jump'
    split_order SMALLINT    NOT NULL,
    value       NUMERIC(12, 4) NOT NULL,
    unit        VARCHAR(30)    NOT NULL DEFAULT 'seconds',
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_result_split UNIQUE (result_id, split_order)
);

CREATE INDEX idx_splits_result ON result_splits(result_id);


-- ============================================================
-- TABLE: rankings
-- Computed final standings per event/round.
-- Materialized here so we can query quickly without sorting.
-- ============================================================
CREATE TABLE rankings (
    id                  UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    competition_id      UUID        NOT NULL REFERENCES competitions(id)      ON DELETE CASCADE,
    event_id            UUID        NOT NULL REFERENCES competition_events(id) ON DELETE CASCADE,
    result_id           UUID        NOT NULL REFERENCES results(id)            ON DELETE CASCADE,
    athlete_id          UUID        NOT NULL REFERENCES athletes(id)           ON DELETE RESTRICT,
    club_id             UUID        REFERENCES clubs(id) ON DELETE SET NULL,

    -- Ranking data
    rank_position       INT         NOT NULL CHECK (rank_position > 0),
    round               VARCHAR(50) NOT NULL DEFAULT 'FINAL',
    medal               medal_type,                                 -- set for top 3

    -- Snapshot of result (denormalised for quick reads)
    performance_value   NUMERIC(12, 4),
    performance_text    VARCHAR(50),
    points              INT,

    is_official         BOOLEAN     NOT NULL DEFAULT FALSE,
    published_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_ranking_athlete_event_round UNIQUE (athlete_id, event_id, round),
    CONSTRAINT uq_ranking_position_event      UNIQUE (event_id, round, rank_position)
);

CREATE INDEX idx_rankings_competition ON rankings(competition_id);
CREATE INDEX idx_rankings_event       ON rankings(event_id);
CREATE INDEX idx_rankings_athlete     ON rankings(athlete_id);
CREATE INDEX idx_rankings_medal       ON rankings(medal) WHERE medal IS NOT NULL;
CREATE INDEX idx_rankings_official    ON rankings(event_id, rank_position) WHERE is_official;

CREATE TRIGGER trg_rankings_updated_at
    BEFORE UPDATE ON rankings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


-- ============================================================
-- TABLE: records
-- Tracks broken records (personal best, national, competition).
-- Append-only. Never delete.
-- ============================================================
CREATE TABLE records (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    result_id       UUID        NOT NULL REFERENCES results(id) ON DELETE RESTRICT,
    competition_id  UUID        NOT NULL REFERENCES competitions(id) ON DELETE RESTRICT,
    event_id        UUID        NOT NULL REFERENCES competition_events(id) ON DELETE RESTRICT,
    athlete_id      UUID        NOT NULL REFERENCES athletes(id) ON DELETE RESTRICT,

    record_type     VARCHAR(50) NOT NULL,    -- 'PERSONAL_BEST', 'NATIONAL_RECORD', 'COMPETITION_RECORD', 'AGE_GROUP_RECORD'
    discipline      VARCHAR(100) NOT NULL,
    gender_category gender,
    age_category    athlete_category,
    performance_value NUMERIC(12, 4) NOT NULL,
    performance_unit  VARCHAR(30),
    performance_text  VARCHAR(50),
    previous_value  NUMERIC(12, 4),          -- previous record being broken
    previous_holder_athlete_id UUID REFERENCES athletes(id) ON DELETE SET NULL,

    set_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ratified        BOOLEAN     NOT NULL DEFAULT FALSE,
    ratified_by     UUID        REFERENCES users(id) ON DELETE SET NULL,
    ratified_at     TIMESTAMPTZ,
    notes           TEXT,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_records_athlete     ON records(athlete_id);
CREATE INDEX idx_records_competition ON records(competition_id);
CREATE INDEX idx_records_type        ON records(record_type);
CREATE INDEX idx_records_discipline  ON records(discipline, gender_category, age_category);
CREATE INDEX idx_records_ratified    ON records(ratified, discipline) WHERE ratified;
