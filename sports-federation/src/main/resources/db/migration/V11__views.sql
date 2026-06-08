-- ============================================================
-- V11__views.sql
--
-- Read-optimised views for common application query patterns.
-- These are plain SQL views (not materialised) so data is always fresh.
-- Add MATERIALIZED + REFRESH CONCURRENTLY when needed at scale.
-- ============================================================

-- ============================================================
-- VIEW: v_athlete_profiles
-- Full athlete profile with club name and user email.
-- Used by: athlete listing pages, search, admin dashboard.
-- ============================================================
CREATE VIEW v_athlete_profiles AS
SELECT
    a.id,
    a.license_number,
    a.license_expiry,
    a.first_name,
    a.last_name,
    a.first_name || ' ' || a.last_name  AS full_name,
    a.date_of_birth,
    EXTRACT(YEAR FROM AGE(a.date_of_birth))::INT  AS age,
    a.gender,
    a.nationality,
    a.country_code,
    a.category,
    a.status,
    a.weight_kg,
    a.height_cm,
    a.photo_url,
    -- Club snapshot
    a.club_id,
    c.name                              AS club_name,
    c.short_name                        AS club_short_name,
    c.city                              AS club_city,
    -- Linked user account
    a.user_id,
    u.email                             AS user_email,
    -- Timestamps
    a.created_at,
    a.updated_at
FROM athletes a
LEFT JOIN clubs  c ON c.id = a.club_id
LEFT JOIN users  u ON u.id = a.user_id;


-- ============================================================
-- VIEW: v_competition_summary
-- Competition with organiser name and registration stats.
-- Used by: competition listing, calendar, home page.
-- ============================================================
CREATE VIEW v_competition_summary AS
SELECT
    co.id,
    co.name,
    co.slug,
    co.edition,
    co.sport,
    co.level,
    co.format,
    co.status,
    co.start_date,
    co.end_date,
    co.registration_deadline,
    co.venue_name,
    co.venue_city,
    co.venue_country,
    co.max_participants,
    co.entry_fee,
    co.currency,
    co.poster_url,
    -- Host club
    co.host_club_id,
    cl.name                                         AS host_club_name,
    -- Organiser
    co.organizer_id,
    u.first_name || ' ' || u.last_name              AS organizer_name,
    -- Live counts
    (SELECT COUNT(*) FROM competition_events  ce WHERE ce.competition_id = co.id)               AS event_count,
    (SELECT COUNT(*) FROM competition_registrations cr
         WHERE cr.competition_id = co.id AND cr.status = 'CONFIRMED')                           AS confirmed_registrations,
    (SELECT COUNT(*) FROM competition_registrations cr
         WHERE cr.competition_id = co.id AND cr.status IN ('PENDING','CONFIRMED','WAITLISTED')) AS total_registrations,
    co.created_at,
    co.updated_at
FROM competitions co
LEFT JOIN clubs cl ON cl.id = co.host_club_id
LEFT JOIN users u  ON u.id  = co.organizer_id;


-- ============================================================
-- VIEW: v_event_results
-- Official results with athlete and club names.
-- Used by: results pages, medal tables, records.
-- ============================================================
CREATE VIEW v_event_results AS
SELECT
    r.id                                            AS result_id,
    r.competition_id,
    co.name                                         AS competition_name,
    co.start_date                                   AS competition_date,
    r.event_id,
    ce.name                                         AS event_name,
    ce.discipline,
    r.round,
    r.heat_number,
    r.lane_number,
    -- Athlete
    r.athlete_id,
    a.license_number,
    a.first_name || ' ' || a.last_name              AS athlete_name,
    a.gender,
    a.category,
    a.country_code,
    a.nationality,
    -- Club
    a.club_id,
    cl.name                                         AS club_name,
    -- Performance
    r.performance_value,
    r.performance_unit,
    r.performance_text,
    r.wind_speed,
    r.points,
    r.status,
    -- Rankings
    rk.rank_position,
    rk.medal,
    -- Record flags
    r.is_personal_best,
    r.is_season_best,
    r.is_competition_record,
    r.is_national_record,
    r.created_at
FROM results r
JOIN competitions    co  ON co.id  = r.competition_id
JOIN competition_events ce ON ce.id = r.event_id
JOIN athletes        a   ON a.id   = r.athlete_id
LEFT JOIN clubs      cl  ON cl.id  = a.club_id
LEFT JOIN rankings   rk  ON rk.result_id = r.id AND rk.round = r.round;


-- ============================================================
-- VIEW: v_club_roster
-- Active athletes per club with summary stats.
-- Used by: club detail page, club manager dashboard.
-- ============================================================
CREATE VIEW v_club_roster AS
SELECT
    c.id                                            AS club_id,
    c.name                                          AS club_name,
    c.short_name,
    c.city,
    c.status                                        AS club_status,
    a.id                                            AS athlete_id,
    a.license_number,
    a.first_name,
    a.last_name,
    a.first_name || ' ' || a.last_name              AS full_name,
    a.date_of_birth,
    EXTRACT(YEAR FROM AGE(a.date_of_birth))::INT    AS age,
    a.gender,
    a.category,
    a.status                                        AS athlete_status,
    a.photo_url
FROM clubs c
JOIN athletes a ON a.club_id = c.id;


-- ============================================================
-- VIEW: v_registration_dashboard
-- Registration status board for competition organisers.
-- ============================================================
CREATE VIEW v_registration_dashboard AS
SELECT
    cr.id                                           AS registration_id,
    cr.registration_number,
    cr.bib_number,
    cr.status,
    cr.fee_paid,
    cr.seed_value,
    cr.seed_unit,
    cr.created_at                                   AS registered_at,
    -- Competition
    cr.competition_id,
    co.name                                         AS competition_name,
    co.start_date                                   AS competition_start,
    -- Event
    cr.event_id,
    ce.name                                         AS event_name,
    ce.discipline,
    -- Athlete
    cr.athlete_id,
    a.license_number,
    a.first_name || ' ' || a.last_name              AS athlete_name,
    a.gender,
    a.category,
    a.country_code,
    -- Club
    cr.club_id,
    cl.name                                         AS club_name
FROM competition_registrations cr
JOIN competitions       co  ON co.id  = cr.competition_id
JOIN competition_events ce  ON ce.id  = cr.event_id
JOIN athletes           a   ON a.id   = cr.athlete_id
LEFT JOIN clubs         cl  ON cl.id  = cr.club_id;


-- ============================================================
-- VIEW: v_news_public
-- Published articles with author name and tags.
-- Used by: public news feed, SEO pages.
-- ============================================================
CREATE VIEW v_news_public AS
SELECT
    n.id,
    n.title,
    n.slug,
    n.excerpt,
    n.category,
    n.language,
    n.cover_url,
    n.cover_alt_text,
    n.is_featured,
    n.is_pinned,
    n.view_count,
    n.published_at,
    n.meta_title,
    n.meta_description,
    -- Author
    n.author_id,
    u.first_name || ' ' || u.last_name              AS author_name,
    u.avatar_url                                    AS author_avatar,
    -- Related entities
    n.related_competition_id,
    co.name                                         AS related_competition_name,
    n.related_athlete_id,
    a.first_name || ' ' || a.last_name              AS related_athlete_name,
    n.related_club_id,
    cl.name                                         AS related_club_name,
    -- Tags (aggregated as JSON array)
    COALESCE(
        (SELECT JSON_AGG(JSON_BUILD_OBJECT('id', t.id, 'name', t.name, 'slug', t.slug, 'color', t.color))
         FROM news_tags nt JOIN tags t ON t.id = nt.tag_id
         WHERE nt.news_id = n.id),
        '[]'::JSON
    )                                               AS tags
FROM news n
LEFT JOIN users        u   ON u.id  = n.author_id
LEFT JOIN competitions co  ON co.id = n.related_competition_id
LEFT JOIN athletes     a   ON a.id  = n.related_athlete_id
LEFT JOIN clubs        cl  ON cl.id = n.related_club_id
WHERE n.status = 'PUBLISHED';
