ALTER TABLE orders
  DROP COLUMN IF EXISTS verification_code,
  DROP COLUMN IF EXISTS verification_expired_at;