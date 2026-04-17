ALTER TABLE orders
  ADD COLUMN verification_code       VARCHAR(20),
  ADD COLUMN verification_expired_at TIMESTAMP;