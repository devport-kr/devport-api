-- Runs once at Postgres container startup (as superuser), before the app connects and Hibernate
-- generates DDL. The wiki entities map a vector(1536) column and the search SQL uses pg_trgm.
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
