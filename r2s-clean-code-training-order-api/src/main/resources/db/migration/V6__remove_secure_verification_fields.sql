ALTER TABLE orders
    DROP COLUMN IF EXISTS verification_code_hash,
    DROP COLUMN IF EXISTS verification_expires_at,
    DROP COLUMN IF EXISTS verification_attempts,
    DROP COLUMN IF EXISTS verified_at;