CREATE TABLE IF NOT EXISTS wiki_drafts (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    sections JSONB,
    current_counters JSONB,
    hidden_sections JSONB,
    source_published_version_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_wiki_drafts_project_id ON wiki_drafts (project_id);
CREATE INDEX IF NOT EXISTS idx_wiki_drafts_updated_at ON wiki_drafts (updated_at DESC);

CREATE TABLE IF NOT EXISTS wiki_published_versions (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    version_number INTEGER NOT NULL,
    sections JSONB,
    current_counters JSONB,
    hidden_sections JSONB,
    published_from_draft_id BIGINT,
    rolled_back_from_version_id BIGINT,
    published_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_wiki_published_versions_project_version UNIQUE (project_id, version_number)
);

CREATE INDEX IF NOT EXISTS idx_wiki_published_versions_project_id ON wiki_published_versions (project_id);
CREATE INDEX IF NOT EXISTS idx_wiki_published_versions_project_version_desc
    ON wiki_published_versions (project_id, version_number DESC);
