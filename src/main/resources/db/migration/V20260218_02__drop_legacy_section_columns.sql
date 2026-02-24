-- Drop legacy Core-6 per-section JSONB columns from project_wiki_snapshots.
-- All section data now lives in the dynamic `sections` JSONB array column.
ALTER TABLE project_wiki_snapshots
    DROP COLUMN IF EXISTS what_section,
    DROP COLUMN IF EXISTS how_section,
    DROP COLUMN IF EXISTS architecture_section,
    DROP COLUMN IF EXISTS activity_section,
    DROP COLUMN IF EXISTS releases_section,
    DROP COLUMN IF EXISTS chat_section;
