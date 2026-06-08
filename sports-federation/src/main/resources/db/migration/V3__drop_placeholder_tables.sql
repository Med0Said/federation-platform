-- ============================================================
-- V3__drop_placeholder_tables.sql
--
-- Drops the stub tables created in V1 so V4+ can recreate
-- them with the full production schema.
-- Safe to run: CASCADE handles any FK dependencies in order.
-- ============================================================

-- Drop triggers first (they reference the tables)
DROP TRIGGER IF EXISTS trg_news_updated_at          ON news;
DROP TRIGGER IF EXISTS trg_competitions_updated_at  ON competitions;
DROP TRIGGER IF EXISTS trg_athletes_updated_at      ON athletes;
DROP TRIGGER IF EXISTS trg_clubs_updated_at         ON clubs;

-- Drop tables in reverse-dependency order
DROP TABLE IF EXISTS news         CASCADE;
DROP TABLE IF EXISTS competitions CASCADE;
DROP TABLE IF EXISTS athletes     CASCADE;
DROP TABLE IF EXISTS clubs        CASCADE;
