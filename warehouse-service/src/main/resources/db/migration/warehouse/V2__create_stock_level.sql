CREATE TABLE warehouse.stock_level (
    company_id UUID NOT NULL,
    product_id UUID NOT NULL,
    available INT NOT NULL DEFAULT 0,
    reserved INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (company_id, product_id)
);