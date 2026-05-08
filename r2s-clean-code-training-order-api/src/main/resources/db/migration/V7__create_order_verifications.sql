CREATE TABLE order_verifications (
    id                     BIGSERIAL PRIMARY KEY,
    order_id               BIGINT        NOT NULL UNIQUE,
    verification_code_hash VARCHAR(64)   NOT NULL,
    expires_at             TIMESTAMP     NOT NULL,
    is_verified            BOOLEAN       NOT NULL DEFAULT FALSE,
    verified_at            TIMESTAMP,
    created_at             TIMESTAMP     NOT NULL,

    CONSTRAINT fk_order_verifications_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
);