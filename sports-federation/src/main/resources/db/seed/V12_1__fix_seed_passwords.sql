-- ============================================================
-- V12_1__fix_seed_passwords.sql   (demo data — db/seed, not applied in prod)
--
-- The BCrypt hash used in V12 does not match the documented
-- demo password, so none of the seeded accounts could log in.
-- Reset them to "Test@1234" (BCrypt, strength 12).
-- V12 itself is left untouched so existing databases keep a
-- valid Flyway checksum.
-- ============================================================

UPDATE users
SET    password = '$2a$12$iHAGk29sR8pTCK7ZeVGZc.LVFDCfkkP59I0gcMRYVOQY3u9frzoym'
WHERE  id IN (
    'a1000000-0000-0000-0000-000000000001',
    'a1000000-0000-0000-0000-000000000002',
    'a1000000-0000-0000-0000-000000000003',
    'a1000000-0000-0000-0000-000000000004',
    'a1000000-0000-0000-0000-000000000005',
    'a1000000-0000-0000-0000-000000000006',
    'a1000000-0000-0000-0000-000000000007',
    'a1000000-0000-0000-0000-000000000008'
);
