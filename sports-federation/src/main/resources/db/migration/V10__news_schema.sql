-- ============================================================
-- V10__news_schema.sql
--
-- Module: news
--
-- Tables:
--   news          – articles and press releases
--   news_tags     – M:N tag taxonomy for articles
--   tags          – reusable tag dictionary
--   news_media    – attached images / videos per article
--
-- Relationships:
--   news.author_id        → users.id
--   news.reviewed_by      → users.id
--   news_tags.news_id     → news.id
--   news_tags.tag_id      → tags.id
--   news_media.news_id    → news.id
--   news_media.uploaded_by → users.id
-- ============================================================

-- ============================================================
-- TABLE: tags
-- Centralised tag dictionary shared across articles.
-- ============================================================
CREATE TABLE tags (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    slug        VARCHAR(110) NOT NULL UNIQUE,
    color       VARCHAR(7),              -- hex colour, e.g. '#FF5733'
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tags_slug ON tags(slug);


-- ============================================================
-- TABLE: news
-- ============================================================
CREATE TABLE news (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Identity
    title           VARCHAR(300)    NOT NULL,
    slug            VARCHAR(320)    NOT NULL UNIQUE,
    excerpt         TEXT,
    content         TEXT            NOT NULL,

    -- Classification
    category        news_category   NOT NULL DEFAULT 'GENERAL',
    status          news_status     NOT NULL DEFAULT 'DRAFT',
    language        VARCHAR(10)     NOT NULL DEFAULT 'fr',   -- BCP-47 language tag

    -- Related entities (optional contextual links)
    related_competition_id  UUID    REFERENCES competitions(id) ON DELETE SET NULL,
    related_athlete_id      UUID    REFERENCES athletes(id)     ON DELETE SET NULL,
    related_club_id         UUID    REFERENCES clubs(id)        ON DELETE SET NULL,

    -- Media
    cover_url       VARCHAR(500),
    cover_alt_text  VARCHAR(255),

    -- Authorship & review
    author_id       UUID            REFERENCES users(id) ON DELETE SET NULL,
    reviewed_by     UUID            REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at     TIMESTAMPTZ,

    -- Publishing
    published_at    TIMESTAMPTZ,
    archived_at     TIMESTAMPTZ,
    is_featured     BOOLEAN         NOT NULL DEFAULT FALSE,
    is_pinned       BOOLEAN         NOT NULL DEFAULT FALSE,
    view_count      INT             NOT NULL DEFAULT 0 CHECK (view_count >= 0),

    -- SEO
    meta_title      VARCHAR(300),
    meta_description VARCHAR(500),

    -- Timestamps
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_news_published_at CHECK (
        (status != 'PUBLISHED') OR (published_at IS NOT NULL)
    ),
    CONSTRAINT chk_news_reviewed CHECK (
        (reviewed_by IS NULL AND reviewed_at IS NULL) OR
        (reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL)
    )
);

-- Indexes
CREATE INDEX idx_news_slug         ON news(slug);
CREATE INDEX idx_news_status       ON news(status);
CREATE INDEX idx_news_category     ON news(category);
CREATE INDEX idx_news_author       ON news(author_id);
CREATE INDEX idx_news_published_at ON news(published_at DESC) WHERE status = 'PUBLISHED';
CREATE INDEX idx_news_featured     ON news(is_featured, published_at DESC) WHERE is_featured;
CREATE INDEX idx_news_competition  ON news(related_competition_id) WHERE related_competition_id IS NOT NULL;
CREATE INDEX idx_news_athlete      ON news(related_athlete_id)     WHERE related_athlete_id IS NOT NULL;
CREATE INDEX idx_news_club         ON news(related_club_id)        WHERE related_club_id IS NOT NULL;
-- Full-text search index
CREATE INDEX idx_news_fts          ON news USING gin(
    to_tsvector('french', COALESCE(title, '') || ' ' || COALESCE(excerpt, '') || ' ' || COALESCE(content, ''))
);

CREATE TRIGGER trg_news_updated_at
    BEFORE UPDATE ON news
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


-- ============================================================
-- TABLE: news_tags  (M:N junction)
-- ============================================================
CREATE TABLE news_tags (
    news_id     UUID NOT NULL REFERENCES news(id) ON DELETE CASCADE,
    tag_id      UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (news_id, tag_id)
);

CREATE INDEX idx_news_tags_tag ON news_tags(tag_id);


-- ============================================================
-- TABLE: news_media
-- ============================================================
CREATE TABLE news_media (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    news_id         UUID        NOT NULL REFERENCES news(id) ON DELETE CASCADE,
    media_type      VARCHAR(20) NOT NULL CHECK (media_type IN ('IMAGE', 'VIDEO', 'DOCUMENT')),
    file_url        VARCHAR(500) NOT NULL,
    thumbnail_url   VARCHAR(500),
    alt_text        VARCHAR(255),
    caption         TEXT,
    display_order   SMALLINT    NOT NULL DEFAULT 0,
    uploaded_by     UUID        REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_news_media_news ON news_media(news_id);


-- ============================================================
-- FUNCTION: increment view count atomically
-- Usage: SELECT increment_news_views('<news-uuid>');
-- ============================================================
CREATE OR REPLACE FUNCTION increment_news_views(p_news_id UUID)
RETURNS VOID AS $$
BEGIN
    UPDATE news SET view_count = view_count + 1 WHERE id = p_news_id;
END;
$$ LANGUAGE plpgsql;
