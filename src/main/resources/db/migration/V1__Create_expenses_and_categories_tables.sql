CREATE TABLE categories
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE expenses
(
    id          UUID PRIMARY KEY,
    name        TEXT,
    description TEXT,
    price       NUMERIC(12, 2) CHECK (price >= 0),
    amount      INTEGER     NOT NULL DEFAULT 1 CHECK (amount > 0),
    currency    CHAR(3),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    category_id UUID        REFERENCES categories (id) ON DELETE SET NULL
);