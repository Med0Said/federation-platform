-- ============================================================
-- V8__competition_registrations_schema.sql
--
-- Module: competition_registrations
--
-- Tables:
--   competition_registrations  – athlete sign-ups per event
--   registration_payments      – fee payment tracking per registration
--
-- Relationships:
--   competition_registrations.event_id    → competition_events.id
--   competition_registrations.athlete_id  → athletes.id
--   competition_registrations.club_id     → clubs.id
--   competition_registrations.registered_by → users.id
--   registration_payments.registration_id → competition_registrations.id
-- ============================================================

-- ============================================================
-- TABLE: competition_registrations
-- ============================================================
CREATE TABLE competition_registrations (
    id              UUID                PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Core relationships
    competition_id  UUID                NOT NULL REFERENCES competitions(id) ON DELETE RESTRICT,
    event_id        UUID                NOT NULL REFERENCES competition_events(id) ON DELETE RESTRICT,
    athlete_id      UUID                NOT NULL REFERENCES athletes(id)    ON DELETE RESTRICT,
    club_id         UUID                REFERENCES clubs(id) ON DELETE SET NULL,    -- snapshot at registration time

    -- Registration metadata
    registered_by   UUID                REFERENCES users(id) ON DELETE SET NULL,    -- staff or athlete self
    registration_number VARCHAR(30)     NOT NULL UNIQUE,                            -- e.g. 'REG-2024-00042'
    bib_number      INT,                                                             -- assigned at closing
    seed_value      NUMERIC(10, 4),                                                 -- declared entry mark
    seed_unit       VARCHAR(30),                                                    -- 'seconds', 'meters', etc.

    -- Status & workflow
    status          registration_status NOT NULL DEFAULT 'PENDING',
    waitlist_position INT,                                                           -- set when WAITLISTED
    confirmed_at    TIMESTAMPTZ,
    cancelled_at    TIMESTAMPTZ,
    cancellation_reason TEXT,

    -- Payment
    fee_amount      NUMERIC(10, 2)      CHECK (fee_amount IS NULL OR fee_amount >= 0),
    fee_currency    VARCHAR(3)          NOT NULL DEFAULT 'TND',
    fee_paid        BOOLEAN             NOT NULL DEFAULT FALSE,

    -- Notes
    medical_waiver  BOOLEAN             NOT NULL DEFAULT FALSE,
    notes           TEXT,

    -- Timestamps
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),

    -- Business constraints
    CONSTRAINT uq_registration_athlete_event UNIQUE (athlete_id, event_id),        -- one entry per athlete per event
    CONSTRAINT chk_reg_bib_positive          CHECK (bib_number IS NULL OR bib_number > 0),
    CONSTRAINT chk_reg_waitlist              CHECK (
        (status = 'WAITLISTED' AND waitlist_position IS NOT NULL) OR
        (status != 'WAITLISTED')
    )
);

-- Indexes
CREATE INDEX idx_reg_competition    ON competition_registrations(competition_id);
CREATE INDEX idx_reg_event          ON competition_registrations(event_id);
CREATE INDEX idx_reg_athlete        ON competition_registrations(athlete_id);
CREATE INDEX idx_reg_club           ON competition_registrations(club_id);
CREATE INDEX idx_reg_status         ON competition_registrations(status);
CREATE INDEX idx_reg_bib            ON competition_registrations(bib_number) WHERE bib_number IS NOT NULL;
CREATE INDEX idx_reg_number         ON competition_registrations(registration_number);
-- Composite: admin dashboard — all registrations for a competition by status
CREATE INDEX idx_reg_comp_status    ON competition_registrations(competition_id, status);

CREATE TRIGGER trg_competition_registrations_updated_at
    BEFORE UPDATE ON competition_registrations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


-- ============================================================
-- TABLE: registration_payments
-- Tracks each payment attempt/confirmation against a registration.
-- ============================================================
CREATE TABLE registration_payments (
    id                  UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    registration_id     UUID        NOT NULL REFERENCES competition_registrations(id) ON DELETE CASCADE,

    amount              NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
    currency            VARCHAR(3)  NOT NULL DEFAULT 'TND',
    payment_method      VARCHAR(50),                    -- 'CASH', 'BANK_TRANSFER', 'ONLINE'
    transaction_ref     VARCHAR(200) UNIQUE,             -- external payment gateway reference
    paid_at             TIMESTAMPTZ,
    confirmed_by        UUID        REFERENCES users(id) ON DELETE SET NULL,
    notes               TEXT,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_registration ON registration_payments(registration_id);
CREATE INDEX idx_payments_transaction  ON registration_payments(transaction_ref) WHERE transaction_ref IS NOT NULL;


-- ============================================================
-- FUNCTION: auto-assign registration number
-- Format: REG-<YEAR>-<5-digit sequence>
-- ============================================================
CREATE SEQUENCE IF NOT EXISTS registration_number_seq START 1;

CREATE OR REPLACE FUNCTION generate_registration_number()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.registration_number IS NULL OR NEW.registration_number = '' THEN
        NEW.registration_number := 'REG-' || TO_CHAR(NOW(), 'YYYY') || '-' ||
                                   LPAD(NEXTVAL('registration_number_seq')::TEXT, 5, '0');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_registration_number
    BEFORE INSERT ON competition_registrations
    FOR EACH ROW EXECUTE FUNCTION generate_registration_number();
