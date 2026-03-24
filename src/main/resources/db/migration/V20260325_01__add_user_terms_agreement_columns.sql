ALTER TABLE users
    ADD COLUMN IF NOT EXISTS agreed_terms_version character varying(20),
    ADD COLUMN IF NOT EXISTS agreed_at timestamp(6) without time zone;
