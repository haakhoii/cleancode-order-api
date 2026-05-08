ALTER TABLE orders
  ADD COLUMN IF NOT EXISTS verification_code_hash   VARCHAR(64),
  ADD COLUMN IF NOT EXISTS verification_expires_at  TIMESTAMP,
  ADD COLUMN IF NOT EXISTS verification_attempts    INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS verified_at              TIMESTAMP;