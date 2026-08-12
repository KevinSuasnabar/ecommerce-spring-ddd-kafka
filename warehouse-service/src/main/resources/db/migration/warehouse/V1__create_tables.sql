CREATE SCHEMA IF NOT EXISTS warehouse;

CREATE TABLE warehouse.stock (
    company_id UUID NOT NULL,
    product_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (company_id, product_id)
);

CREATE TABLE warehouse.stock_movement (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    product_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (company_id, product_id) REFERENCES warehouse.stock(company_id, product_id)
);

CREATE TABLE warehouse.processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
