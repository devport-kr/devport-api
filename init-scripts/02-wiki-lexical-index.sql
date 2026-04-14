CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_wiki_section_chunks_content_trgm
    ON wiki_section_chunks USING gin (content gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_wiki_section_chunks_title_trgm
    ON wiki_section_chunks USING gin ((metadata->>'titleKo') gin_trgm_ops);
