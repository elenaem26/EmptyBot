CREATE TABLE categories
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, name)
);

CREATE TABLE expenses
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    name        TEXT,
    price       NUMERIC(12, 2) CHECK (price >= 0),
    currency    CHAR(3),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    category_id BIGINT      REFERENCES categories (id) ON DELETE SET NULL
);

CREATE TABLE category_rules
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT        NOT NULL,
    keyword    VARCHAR(1000) NOT NULL,
    category   TEXT          NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (user_id, keyword)
);