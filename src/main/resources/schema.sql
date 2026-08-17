CREATE TABLE IF NOT EXISTS orders (
    order_id VARCHAR(40) PRIMARY KEY,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS products (
    product_id VARCHAR(40) PRIMARY KEY,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS customer_orders (
    order_id VARCHAR(40) PRIMARY KEY,
    customer_id VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    total_amount INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS inventory_items (
    item_id VARCHAR(40) PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL
);
