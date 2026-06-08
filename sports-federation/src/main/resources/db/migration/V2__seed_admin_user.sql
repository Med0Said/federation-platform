-- ============================================================
-- V2__seed_admin_user.sql
-- Seed initial admin user (password: Admin@1234)
-- BCrypt hash of "Admin@1234"
-- ============================================================

INSERT INTO users (id, email, username, password, first_name, last_name, role, status)
VALUES (
    uuid_generate_v4(),
    'admin@federation.local',
    'admin',
    '$2a$12$83/P5BBra1QcfTNFstIsXeFfHU5TggXtWKQlT68nlfpjgPYc4HXjy',  -- Admin@1234
    'System',
    'Administrator',
    'ROLE_ADMIN',
    'ACTIVE'
);
