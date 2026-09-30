-- Autocomplete (title-only ILIKE '%q%') needs a trigram GIN index on articles.summary_ko_title.
-- Apply manually to existing databases (init-scripts only run on an empty volume, before Hibernate
-- creates the table). CONCURRENTLY avoids blocking writes; it cannot run inside a transaction:
--   psql -U <user> -d <db> -f sql/manual/001-article-title-trgm-index.sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_articles_summary_ko_title_trgm
    ON articles USING gin (summary_ko_title gin_trgm_ops);
