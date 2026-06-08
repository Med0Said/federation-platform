-- ============================================================
-- V12__seed_data.sql
--
-- Realistic seed data for local development and testing.
-- Covers all modules in dependency order.
--
-- Admin user was already seeded in V2. This migration adds:
--   - 4 staff/manager users
--   - 3 clubs
--   - 8 athletes
--   - 2 competitions with events
--   - registrations
--   - results & rankings
--   - news articles & tags
-- ============================================================

-- ============================================================
-- SECTION 1: USERS
-- Passwords: all set to  Test@1234
-- BCrypt hash below is for "Test@1234" with strength 12
-- ============================================================
INSERT INTO users (id, email, username, password, first_name, last_name, role, status) VALUES

-- Federation staff
('a1000000-0000-0000-0000-000000000001',
 'staff@federation.local', 'staff_manager',
 '$2a$12$hRv7f6T8kDq9lMoYPxGsOe3L8TqzYJmwY2OA6eLBxk7yrGnX1x4Vu',
 'Sonia', 'Ben Salah', 'ROLE_FEDERATION_STAFF', 'ACTIVE'),

-- Club managers
('a1000000-0000-0000-0000-000000000002',
 'manager.esperance@federation.local', 'manager_est',
 '$2a$12$hRv7f6T8kDq9lMoYPxGsOe3L8TqzYJmwY2OA6eLBxk7yrGnX1x4Vu',
 'Karim', 'Gharbi', 'ROLE_CLUB_MANAGER', 'ACTIVE'),

('a1000000-0000-0000-0000-000000000003',
 'manager.sfax@federation.local', 'manager_css',
 '$2a$12$hRv7f6T8kDq9lMoYPxGsOe3L8TqzYJmwY2OA6eLBxk7yrGnX1x4Vu',
 'Leila', 'Trabelsi', 'ROLE_CLUB_MANAGER', 'ACTIVE'),

('a1000000-0000-0000-0000-000000000004',
 'manager.sousse@federation.local', 'manager_usm',
 '$2a$12$hRv7f6T8kDq9lMoYPxGsOe3L8TqzYJmwY2OA6eLBxk7yrGnX1x4Vu',
 'Nabil', 'Jebali', 'ROLE_CLUB_MANAGER', 'ACTIVE'),

-- Athlete accounts
('a1000000-0000-0000-0000-000000000005',
 'athlete.ferjani@federation.local', 'ferjani_m',
 '$2a$12$hRv7f6T8kDq9lMoYPxGsOe3L8TqzYJmwY2OA6eLBxk7yrGnX1x4Vu',
 'Mohamed', 'Ferjani', 'ROLE_ATHLETE', 'ACTIVE'),

('a1000000-0000-0000-0000-000000000006',
 'athlete.ayari@federation.local', 'ayari_h',
 '$2a$12$hRv7f6T8kDq9lMoYPxGsOe3L8TqzYJmwY2OA6eLBxk7yrGnX1x4Vu',
 'Habiba', 'Ayari', 'ROLE_ATHLETE', 'ACTIVE'),

('a1000000-0000-0000-0000-000000000007',
 'athlete.dridi@federation.local', 'dridi_a',
 '$2a$12$hRv7f6T8kDq9lMoYPxGsOe3L8TqzYJmwY2OA6eLBxk7yrGnX1x4Vu',
 'Amine', 'Dridi', 'ROLE_ATHLETE', 'ACTIVE'),

('a1000000-0000-0000-0000-000000000008',
 'athlete.mansour@federation.local', 'mansour_r',
 '$2a$12$hRv7f6T8kDq9lMoYPxGsOe3L8TqzYJmwY2OA6eLBxk7yrGnX1x4Vu',
 'Rim', 'Mansour', 'ROLE_ATHLETE', 'ACTIVE');


-- ============================================================
-- SECTION 2: CLUBS
-- ============================================================
INSERT INTO clubs (id, name, short_name, slug, license_number, city, region, country,
                   founded_year, description, email, phone, manager_id, status) VALUES

('b1000000-0000-0000-0000-000000000001',
 'Espérance Sportive de Tunis – Athlétisme',
 'EST',
 'esperance-sportive-tunis-athletisme',
 'CLB-2024-0001',
 'Tunis', 'Tunis', 'TN',
 1919,
 'Section athlétisme du club phare de Tunis, fondé en 1919.',
 'athletisme@esperance.tn', '+216 71 XXX 001',
 'a1000000-0000-0000-0000-000000000002',
 'ACTIVE'),

('b1000000-0000-0000-0000-000000000002',
 'Club Sportif Sfaxien – Athlétisme',
 'CSS',
 'club-sportif-sfaxien-athletisme',
 'CLB-2024-0002',
 'Sfax', 'Sfax', 'TN',
 1928,
 'Section athlétisme du CSS, club historique du sud tunisien.',
 'athletisme@css.tn', '+216 74 XXX 002',
 'a1000000-0000-0000-0000-000000000003',
 'ACTIVE'),

('b1000000-0000-0000-0000-000000000003',
 'Union Sportive Monastirienne – Athlétisme',
 'USM',
 'union-sportive-monastirienne-athletisme',
 'CLB-2024-0003',
 'Monastir', 'Monastir', 'TN',
 1923,
 'Section athlétisme de l''USM, basée à Monastir.',
 'athletisme@usm.tn', '+216 73 XXX 003',
 'a1000000-0000-0000-0000-000000000004',
 'ACTIVE');


-- ============================================================
-- SECTION 3: ATHLETES
-- ============================================================
INSERT INTO athletes (id, user_id, club_id, license_number, first_name, last_name,
                      date_of_birth, gender, nationality, country_code,
                      weight_kg, height_cm, status, email) VALUES

-- EST athletes
('c1000000-0000-0000-0000-000000000001',
 'a1000000-0000-0000-0000-000000000005',
 'b1000000-0000-0000-0000-000000000001',
 'ATH-2024-00001', 'Mohamed', 'Ferjani',
 '1998-03-15', 'MALE', 'Tunisian', 'TUN',
 72.5, 180.0, 'ACTIVE', 'ferjani@est.tn'),

('c1000000-0000-0000-0000-000000000002',
 NULL,
 'b1000000-0000-0000-0000-000000000001',
 'ATH-2024-00002', 'Youssef', 'Chaabane',
 '2001-07-22', 'MALE', 'Tunisian', 'TUN',
 68.0, 175.0, 'ACTIVE', 'chaabane@est.tn'),

-- CSS athletes
('c1000000-0000-0000-0000-000000000003',
 'a1000000-0000-0000-0000-000000000006',
 'b1000000-0000-0000-0000-000000000002',
 'ATH-2024-00003', 'Habiba', 'Ayari',
 '1999-11-05', 'FEMALE', 'Tunisian', 'TUN',
 55.0, 165.0, 'ACTIVE', 'ayari@css.tn'),

('c1000000-0000-0000-0000-000000000004',
 NULL,
 'b1000000-0000-0000-0000-000000000002',
 'ATH-2024-00004', 'Fatma', 'Kchich',
 '2003-02-18', 'FEMALE', 'Tunisian', 'TUN',
 52.0, 162.0, 'ACTIVE', 'kchich@css.tn'),

-- USM athletes
('c1000000-0000-0000-0000-000000000005',
 'a1000000-0000-0000-0000-000000000007',
 'b1000000-0000-0000-0000-000000000003',
 'ATH-2024-00005', 'Amine', 'Dridi',
 '1995-06-30', 'MALE', 'Tunisian', 'TUN',
 80.0, 185.0, 'ACTIVE', 'dridi@usm.tn'),

('c1000000-0000-0000-0000-000000000006',
 NULL,
 'b1000000-0000-0000-0000-000000000003',
 'ATH-2024-00006', 'Khaled', 'Souissi',
 '2000-09-12', 'MALE', 'Tunisian', 'TUN',
 76.0, 182.0, 'ACTIVE', 'souissi@usm.tn'),

('c1000000-0000-0000-0000-000000000007',
 'a1000000-0000-0000-0000-000000000008',
 'b1000000-0000-0000-0000-000000000001',
 'ATH-2024-00007', 'Rim', 'Mansour',
 '2005-04-10', 'FEMALE', 'Tunisian', 'TUN',
 50.0, 158.0, 'ACTIVE', 'mansour@est.tn'),

('c1000000-0000-0000-0000-000000000008',
 NULL,
 'b1000000-0000-0000-0000-000000000002',
 'ATH-2024-00008', 'Sarra', 'Hamdi',
 '1997-12-01', 'FEMALE', 'Tunisian', 'TUN',
 57.0, 168.0, 'ACTIVE', 'hamdi@css.tn');


-- ============================================================
-- SECTION 4: ATHLETE CLUB HISTORY
-- ============================================================
INSERT INTO athlete_club_history (athlete_id, club_id, club_name, start_date) VALUES
('c1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001', 'Espérance Sportive de Tunis – Athlétisme', '2018-09-01'),
('c1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000001', 'Espérance Sportive de Tunis – Athlétisme', '2020-01-15'),
('c1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000002', 'Club Sportif Sfaxien – Athlétisme', '2019-03-01'),
('c1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000002', 'Club Sportif Sfaxien – Athlétisme', '2021-09-01'),
('c1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000003', 'Union Sportive Monastirienne – Athlétisme', '2017-06-01'),
('c1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000003', 'Union Sportive Monastirienne – Athlétisme', '2022-01-01'),
('c1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000001', 'Espérance Sportive de Tunis – Athlétisme', '2023-09-01'),
('c1000000-0000-0000-0000-000000000008', 'b1000000-0000-0000-0000-000000000002', 'Club Sportif Sfaxien – Athlétisme', '2019-07-01');


-- ============================================================
-- SECTION 5: COMPETITIONS
-- ============================================================
INSERT INTO competitions (id, name, slug, edition, description, sport, level, format,
                          venue_name, venue_city, start_date, end_date, registration_deadline,
                          max_participants, organizer_id, host_club_id,
                          entry_fee, status, published_at) VALUES

('d1000000-0000-0000-0000-000000000001',
 'Championnat National d''Athlétisme 2024',
 'championnat-national-athletisme-2024',
 '2024',
 'La compétition nationale annuelle de la Fédération Tunisienne d''Athlétisme.',
 'Athletics', 'NATIONAL', 'INDIVIDUAL',
 'Stade El Menzah', 'Tunis',
 '2024-06-15', '2024-06-16',
 '2024-06-01',
 200,
 'a1000000-0000-0000-0000-000000000001',
 'b1000000-0000-0000-0000-000000000001',
 50.00, 'COMPLETED', '2024-04-01 08:00:00+01'),

('d1000000-0000-0000-0000-000000000002',
 'Open International de Tunis 2025',
 'open-international-tunis-2025',
 '2025',
 'Compétition internationale ouverte aux athlètes tunisiens et étrangers.',
 'Athletics', 'INTERNATIONAL', 'INDIVIDUAL',
 'Stade de Radès', 'Tunis',
 '2025-03-20', '2025-03-22',
 '2025-03-05',
 300,
 'a1000000-0000-0000-0000-000000000001',
 'b1000000-0000-0000-0000-000000000001',
 75.00, 'REGISTRATION_OPEN', '2025-01-10 08:00:00+01');


-- ============================================================
-- SECTION 6: COMPETITION EVENTS
-- ============================================================
INSERT INTO competition_events (id, competition_id, name, code, discipline,
                                 gender_category, age_category, scoring_unit,
                                 lower_is_better, max_participants, status) VALUES

-- Championnat 2024 events
('e1000000-0000-0000-0000-000000000001',
 'd1000000-0000-0000-0000-000000000001',
 '100m Hommes Senior', 'M-100-SR', '100m Sprint',
 'MALE', 'SENIOR', 'seconds', TRUE, 32, 'COMPLETED'),

('e1000000-0000-0000-0000-000000000002',
 'd1000000-0000-0000-0000-000000000001',
 '100m Femmes Senior', 'F-100-SR', '100m Sprint',
 'FEMALE', 'SENIOR', 'seconds', TRUE, 32, 'COMPLETED'),

('e1000000-0000-0000-0000-000000000003',
 'd1000000-0000-0000-0000-000000000001',
 '400m Hommes Senior', 'M-400-SR', '400m',
 'MALE', 'SENIOR', 'seconds', TRUE, 24, 'COMPLETED'),

('e1000000-0000-0000-0000-000000000004',
 'd1000000-0000-0000-0000-000000000001',
 'Saut en longueur Femmes', 'F-LJ-SR', 'Long Jump',
 'FEMALE', 'SENIOR', 'meters', FALSE, 20, 'COMPLETED'),

-- Open International 2025 events
('e1000000-0000-0000-0000-000000000005',
 'd1000000-0000-0000-0000-000000000002',
 '100m Hommes Open', 'M-100-OPEN', '100m Sprint',
 'MALE', NULL, 'seconds', TRUE, 48, 'REGISTRATION_OPEN'),

('e1000000-0000-0000-0000-000000000006',
 'd1000000-0000-0000-0000-000000000002',
 '100m Femmes Open', 'F-100-OPEN', '100m Sprint',
 'FEMALE', NULL, 'seconds', TRUE, 48, 'REGISTRATION_OPEN');


-- ============================================================
-- SECTION 7: COMPETITION REGISTRATIONS (2024 Championship)
-- ============================================================
INSERT INTO competition_registrations
    (id, competition_id, event_id, athlete_id, club_id, registered_by,
     registration_number, bib_number, seed_value, seed_unit, status, fee_paid,
     confirmed_at, medical_waiver) VALUES

-- 100m Men
('f1000000-0000-0000-0000-000000000001',
 'd1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001',
 'a1000000-0000-0000-0000-000000000002',
 'REG-2024-00001', 101, 10.45, 'seconds', 'CONFIRMED', TRUE, '2024-05-25 10:00:00+01', TRUE),

('f1000000-0000-0000-0000-000000000002',
 'd1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000001',
 'a1000000-0000-0000-0000-000000000002',
 'REG-2024-00002', 102, 10.72, 'seconds', 'CONFIRMED', TRUE, '2024-05-25 10:05:00+01', TRUE),

('f1000000-0000-0000-0000-000000000003',
 'd1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000003',
 'a1000000-0000-0000-0000-000000000004',
 'REG-2024-00003', 103, 10.58, 'seconds', 'CONFIRMED', TRUE, '2024-05-26 09:00:00+01', TRUE),

('f1000000-0000-0000-0000-000000000004',
 'd1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000003',
 'a1000000-0000-0000-0000-000000000004',
 'REG-2024-00004', 104, 10.80, 'seconds', 'CONFIRMED', TRUE, '2024-05-26 09:05:00+01', TRUE),

-- 100m Women
('f1000000-0000-0000-0000-000000000005',
 'd1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000002',
 'c1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000002',
 'a1000000-0000-0000-0000-000000000003',
 'REG-2024-00005', 201, 11.80, 'seconds', 'CONFIRMED', TRUE, '2024-05-27 10:00:00+01', TRUE),

('f1000000-0000-0000-0000-000000000006',
 'd1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000002',
 'c1000000-0000-0000-0000-000000000008', 'b1000000-0000-0000-0000-000000000002',
 'a1000000-0000-0000-0000-000000000003',
 'REG-2024-00006', 202, 11.95, 'seconds', 'CONFIRMED', TRUE, '2024-05-27 10:05:00+01', TRUE),

-- Long Jump Women
('f1000000-0000-0000-0000-000000000007',
 'd1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000004',
 'c1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000002',
 'a1000000-0000-0000-0000-000000000003',
 'REG-2024-00007', 301, 5.85, 'meters', 'CONFIRMED', TRUE, '2024-05-28 11:00:00+01', TRUE),

('f1000000-0000-0000-0000-000000000008',
 'd1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000004',
 'c1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000001',
 'a1000000-0000-0000-0000-000000000002',
 'REG-2024-00008', 302, 5.60, 'meters', 'CONFIRMED', TRUE, '2024-05-28 11:05:00+01', TRUE);


-- ============================================================
-- SECTION 8: RESULTS (2024 Championship – 100m Men Final)
-- ============================================================
INSERT INTO results (id, registration_id, competition_id, event_id, athlete_id,
                     round, lane_number, performance_value, performance_unit,
                     performance_text, wind_speed, status,
                     is_personal_best, is_competition_record,
                     recorded_by, verified_by, verified_at) VALUES

('00700000-0000-0000-0000-000000000001',
 'f1000000-0000-0000-0000-000000000001',
 'd1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'FINAL', 4, 10.32, 'seconds', '10.32', 0.8, 'OFFICIAL',
 TRUE, TRUE,
 'a1000000-0000-0000-0000-000000000001',
 'a1000000-0000-0000-0000-000000000001', '2024-06-15 15:45:00+01'),

('00700000-0000-0000-0000-000000000002',
 'f1000000-0000-0000-0000-000000000003',
 'd1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000005',
 'FINAL', 5, 10.48, 'seconds', '10.48', 0.8, 'OFFICIAL',
 FALSE, FALSE,
 'a1000000-0000-0000-0000-000000000001',
 'a1000000-0000-0000-0000-000000000001', '2024-06-15 15:45:00+01'),

('00700000-0000-0000-0000-000000000003',
 'f1000000-0000-0000-0000-000000000002',
 'd1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000002',
 'FINAL', 6, 10.61, 'seconds', '10.61', 0.8, 'OFFICIAL',
 FALSE, FALSE,
 'a1000000-0000-0000-0000-000000000001',
 'a1000000-0000-0000-0000-000000000001', '2024-06-15 15:45:00+01'),

('00700000-0000-0000-0000-000000000004',
 'f1000000-0000-0000-0000-000000000004',
 'd1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000006',
 'FINAL', 7, 10.79, 'seconds', '10.79', 0.8, 'OFFICIAL',
 FALSE, FALSE,
 'a1000000-0000-0000-0000-000000000001',
 'a1000000-0000-0000-0000-000000000001', '2024-06-15 15:45:00+01'),

-- 100m Women Final
('00700000-0000-0000-0000-000000000005',
 'f1000000-0000-0000-0000-000000000005',
 'd1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000002',
 'c1000000-0000-0000-0000-000000000003',
 'FINAL', 4, 11.73, 'seconds', '11.73', 0.5, 'OFFICIAL',
 TRUE, TRUE,
 'a1000000-0000-0000-0000-000000000001',
 'a1000000-0000-0000-0000-000000000001', '2024-06-15 17:10:00+01'),

('00700000-0000-0000-0000-000000000006',
 'f1000000-0000-0000-0000-000000000006',
 'd1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000002',
 'c1000000-0000-0000-0000-000000000008',
 'FINAL', 5, 11.92, 'seconds', '11.92', 0.5, 'OFFICIAL',
 FALSE, FALSE,
 'a1000000-0000-0000-0000-000000000001',
 'a1000000-0000-0000-0000-000000000001', '2024-06-15 17:10:00+01');


-- ============================================================
-- SECTION 9: RANKINGS
-- ============================================================
INSERT INTO rankings (competition_id, event_id, result_id, athlete_id, club_id,
                      rank_position, round, medal, performance_value, performance_text,
                      is_official, published_at) VALUES

-- 100m Men Final
('d1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001',
 '00700000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001',
 'b1000000-0000-0000-0000-000000000001',
 1, 'FINAL', 'GOLD', 10.32, '10.32', TRUE, '2024-06-15 18:00:00+01'),

('d1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001',
 '00700000-0000-0000-0000-000000000002', 'c1000000-0000-0000-0000-000000000005',
 'b1000000-0000-0000-0000-000000000003',
 2, 'FINAL', 'SILVER', 10.48, '10.48', TRUE, '2024-06-15 18:00:00+01'),

('d1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001',
 '00700000-0000-0000-0000-000000000003', 'c1000000-0000-0000-0000-000000000002',
 'b1000000-0000-0000-0000-000000000001',
 3, 'FINAL', 'BRONZE', 10.61, '10.61', TRUE, '2024-06-15 18:00:00+01'),

('d1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001',
 '00700000-0000-0000-0000-000000000004', 'c1000000-0000-0000-0000-000000000006',
 'b1000000-0000-0000-0000-000000000003',
 4, 'FINAL', NULL, 10.79, '10.79', TRUE, '2024-06-15 18:00:00+01'),

-- 100m Women Final
('d1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000002',
 '00700000-0000-0000-0000-000000000005', 'c1000000-0000-0000-0000-000000000003',
 'b1000000-0000-0000-0000-000000000002',
 1, 'FINAL', 'GOLD', 11.73, '11.73', TRUE, '2024-06-15 18:30:00+01'),

('d1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000002',
 '00700000-0000-0000-0000-000000000006', 'c1000000-0000-0000-0000-000000000008',
 'b1000000-0000-0000-0000-000000000002',
 2, 'FINAL', 'SILVER', 11.92, '11.92', TRUE, '2024-06-15 18:30:00+01');


-- ============================================================
-- SECTION 10: RECORDS
-- ============================================================
INSERT INTO records (result_id, competition_id, event_id, athlete_id,
                     record_type, discipline, gender_category, age_category,
                     performance_value, performance_unit, performance_text,
                     set_at, ratified, ratified_by, ratified_at) VALUES

('00700000-0000-0000-0000-000000000001',
 'd1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'COMPETITION_RECORD', '100m Sprint', 'MALE', 'SENIOR',
 10.32, 'seconds', '10.32',
 '2024-06-15 15:45:00+01', TRUE,
 'a1000000-0000-0000-0000-000000000001', '2024-06-16 09:00:00+01'),

('00700000-0000-0000-0000-000000000005',
 'd1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000002',
 'c1000000-0000-0000-0000-000000000003',
 'COMPETITION_RECORD', '100m Sprint', 'FEMALE', 'SENIOR',
 11.73, 'seconds', '11.73',
 '2024-06-15 17:10:00+01', TRUE,
 'a1000000-0000-0000-0000-000000000001', '2024-06-16 09:05:00+01');


-- ============================================================
-- SECTION 11: TAGS
-- ============================================================
INSERT INTO tags (id, name, slug, color) VALUES
('00800000-0000-0000-0000-000000000001', 'Sprint',       'sprint',       '#E74C3C'),
('00800000-0000-0000-0000-000000000002', 'Résultats',    'resultats',    '#3498DB'),
('00800000-0000-0000-0000-000000000003', 'Championnat',  'championnat',  '#9B59B6'),
('00800000-0000-0000-0000-000000000004', 'Record',       'record',       '#F39C12'),
('00800000-0000-0000-0000-000000000005', 'Compétition',  'competition',  '#1ABC9C'),
('00800000-0000-0000-0000-000000000006', 'Athlète',      'athlete',      '#2ECC71'),
('00800000-0000-0000-0000-000000000007', 'International','international','#E67E22');


-- ============================================================
-- SECTION 12: NEWS
-- ============================================================
INSERT INTO news (id, title, slug, excerpt, content, category, status, language,
                  related_competition_id, related_athlete_id,
                  author_id, published_at, is_featured, cover_url) VALUES

('00900000-0000-0000-0000-000000000001',
 'Mohamed Ferjani remporte le 100m au Championnat National 2024',
 'ferjani-champion-100m-2024',
 'Mohamed Ferjani de l''EST établit un nouveau record de compétition avec 10.32 secondes.',
 'Dans une finale palpitante au Stade El Menzah, Mohamed Ferjani a dominé le 100m hommes senior en réalisant un temps remarquable de 10.32 secondes, établissant ainsi un nouveau record de la compétition. L''athlète de l''Espérance Sportive de Tunis a devancé Amine Dridi de l''USM (10.48) et son coéquipier Youssef Chaabane (10.61).',
 'COMPETITION', 'PUBLISHED', 'fr',
 'd1000000-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'a1000000-0000-0000-0000-000000000001',
 '2024-06-16 10:00:00+01', TRUE,
 'https://cdn.federation.tn/news/ferjani-100m-2024.jpg'),

('00900000-0000-0000-0000-000000000002',
 'Habiba Ayari : Nouvelle reine du 100m féminin',
 'ayari-championne-100m-feminin-2024',
 'La sprinteuse du CSS Habiba Ayari s''impose avec un chrono de 11.73 secondes.',
 'Habiba Ayari a confirmé son statut de meilleure sprinteuse tunisienne en remportant le 100m femmes senior du Championnat National 2024 avec un temps de 11.73 secondes, nouveau record de la compétition. Sa victoire conforte la domination du Club Sportif Sfaxien en sprint féminin.',
 'COMPETITION', 'PUBLISHED', 'fr',
 'd1000000-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000003',
 'a1000000-0000-0000-0000-000000000001',
 '2024-06-16 11:00:00+01', FALSE,
 'https://cdn.federation.tn/news/ayari-100m-2024.jpg'),

('00900000-0000-0000-0000-000000000003',
 'Ouverture des inscriptions : Open International de Tunis 2025',
 'ouverture-inscriptions-open-tunis-2025',
 'La Fédération annonce l''ouverture des inscriptions pour l''Open International de Tunis 2025.',
 'La Fédération Tunisienne d''Athlétisme est heureuse d''annoncer l''ouverture des inscriptions pour l''Open International de Tunis 2025, qui se tiendra du 20 au 22 mars 2025 au Stade de Radès. Cet événement international accueillera des athlètes de toutes catégories.',
 'ANNOUNCEMENT', 'PUBLISHED', 'fr',
 'd1000000-0000-0000-0000-000000000002',
 NULL,
 'a1000000-0000-0000-0000-000000000001',
 '2025-01-10 09:00:00+01', TRUE,
 'https://cdn.federation.tn/news/open-tunis-2025.jpg');


-- ============================================================
-- SECTION 13: NEWS TAGS
-- ============================================================
INSERT INTO news_tags (news_id, tag_id) VALUES
('00900000-0000-0000-0000-000000000001', '00800000-0000-0000-0000-000000000001'), -- Sprint
('00900000-0000-0000-0000-000000000001', '00800000-0000-0000-0000-000000000002'), -- Résultats
('00900000-0000-0000-0000-000000000001', '00800000-0000-0000-0000-000000000003'), -- Championnat
('00900000-0000-0000-0000-000000000001', '00800000-0000-0000-0000-000000000004'), -- Record
('00900000-0000-0000-0000-000000000002', '00800000-0000-0000-0000-000000000001'), -- Sprint
('00900000-0000-0000-0000-000000000002', '00800000-0000-0000-0000-000000000002'), -- Résultats
('00900000-0000-0000-0000-000000000002', '00800000-0000-0000-0000-000000000006'), -- Athlète
('00900000-0000-0000-0000-000000000003', '00800000-0000-0000-0000-000000000005'), -- Compétition
('00900000-0000-0000-0000-000000000003', '00800000-0000-0000-0000-000000000007'); -- International
