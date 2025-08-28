CREATE TABLE categories
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE purchases
(
    id           UUID PRIMARY KEY,
    name_parsed  TEXT,
    name      TEXT,
    price        NUMERIC(12,2) CHECK (price >= 0),
    currency     CHAR(3),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    purchased_at TIMESTAMPTZ,
    category_id  UUID        REFERENCES categories (id) ON DELETE SET NULL,
    status       VARCHAR(255),
    check_id     UUID
);