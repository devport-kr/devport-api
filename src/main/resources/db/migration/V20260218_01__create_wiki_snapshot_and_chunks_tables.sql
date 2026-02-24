-- Enable pgvector extension (idempotent)
CREATE EXTENSION IF NOT EXISTS vector;

-- project_wiki_snapshots: stores the assembled wiki for each project
CREATE TABLE IF NOT EXISTS project_wiki_snapshots (
    id                   BIGSERIAL    PRIMARY KEY,
    project_external_id  VARCHAR(255) NOT NULL UNIQUE,
    generated_at         TIMESTAMPTZ  NOT NULL,
    sections             JSONB,
    current_counters     JSONB,

    -- Legacy Core-6 section fields (fallback compatibility only)
    what_section         JSONB,
    how_section          JSONB,
    architecture_section JSONB,
    activity_section     JSONB,
    releases_section     JSONB,
    chat_section         JSONB,

    -- Readiness gates
    is_data_ready        BOOLEAN      NOT NULL DEFAULT false,
    hidden_sections      JSONB,
    readiness_metadata   JSONB,

    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- wiki_section_chunks: embedded wiki chunks for vector similarity retrieval
CREATE TABLE IF NOT EXISTS wiki_section_chunks (
    id                   BIGSERIAL    PRIMARY KEY,
    project_external_id  VARCHAR(255) NOT NULL,
    section_id           VARCHAR(100) NOT NULL,
    subsection_id        VARCHAR(100),
    chunk_type           VARCHAR(20)  NOT NULL,
    content              TEXT         NOT NULL,
    embedding            vector(1536) NOT NULL,
    token_count          INTEGER,
    metadata             JSONB,
    commit_sha           VARCHAR(40)  NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_wiki_section_chunks_project
    ON wiki_section_chunks (project_external_id);

CREATE INDEX IF NOT EXISTS idx_wiki_section_chunks_project_section
    ON wiki_section_chunks (project_external_id, section_id);

-- HNSW index for vector similarity search
CREATE INDEX IF NOT EXISTS idx_wiki_section_chunks_embedding
    ON wiki_section_chunks USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
