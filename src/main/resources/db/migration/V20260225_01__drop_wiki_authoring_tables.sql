-- Remove unused tables. Wiki content is now served exclusively from
-- wiki_section_chunks written directly by the AI agent. Star history is
-- no longer tracked — only the current star count on the projects row is used.

DROP TABLE IF EXISTS wiki_published_versions;
DROP TABLE IF EXISTS wiki_drafts;
DROP TABLE IF EXISTS project_wiki_snapshots;
DROP TABLE IF EXISTS project_star_history;

ALTER TABLE projects DROP CONSTRAINT IF EXISTS fk_projects_port_id;
ALTER TABLE projects DROP COLUMN  IF EXISTS port_id;
DROP INDEX  IF EXISTS idx_projects_port_id;
DROP TABLE  IF EXISTS ports;
