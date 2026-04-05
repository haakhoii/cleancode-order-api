CREATE TABLE customers (
                           id BIGSERIAL PRIMARY KEY,
                           email VARCHAR(255) UNIQUE NOT NULL,
                           full_name VARCHAR(255) NOT NULL
);

CREATE TABLE products (
                          id BIGSERIAL PRIMARY KEY,
                          sku VARCHAR(50) UNIQUE NOT NULL,
                          name VARCHAR(255) NOT NULL,
                          price_cents INTEGER NOT NULL
);

CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        customer_id BIGINT NOT NULL REFERENCES customers(id),
                        status VARCHAR(30) NOT NULL,
                        total_cents INTEGER NOT NULL,
                        discount_cents INTEGER NOT NULL,
                        created_at TIMESTAMP NOT NULL
);

CREATE TABLE order_items (
                             id BIGSERIAL PRIMARY KEY,
                             order_id BIGINT NOT NULL REFERENCES orders(id),
                             product_id BIGINT NOT NULL REFERENCES products(id),
                             quantity INTEGER NOT NULL,
                             unit_price_cents INTEGER NOT NULL,
                             line_total_cents INTEGER NOT NULL
);