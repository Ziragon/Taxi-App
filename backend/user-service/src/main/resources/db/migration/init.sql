CREATE TYPE account_role  AS ENUM ('PASSENGER', 'DRIVER', 'ADMIN');
CREATE TYPE driver_status AS ENUM ('ONLINE', 'OFFLINE', 'BUSY');
CREATE TYPE vehicle_class AS ENUM ('ECONOMY', 'COMFORT', 'BUSINESS');

CREATE SEQUENCE accounts_id_seq       INCREMENT BY 50 START WITH 1;
CREATE SEQUENCE refresh_tokens_id_seq INCREMENT BY 50 START WITH 1;
CREATE SEQUENCE vehicles_id_seq       INCREMENT BY 50 START WITH 1;

CREATE TABLE accounts
(
    id            BIGINT        PRIMARY KEY DEFAULT nextval('accounts_id_seq'),
    role          account_role  NOT NULL,
    email         VARCHAR(255)  NOT NULL UNIQUE,
    phone         VARCHAR(50)   NOT NULL UNIQUE,
    password_hash VARCHAR(255)  NOT NULL,
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_email ON accounts (email);
CREATE INDEX idx_accounts_phone ON accounts (phone);

CREATE TABLE refresh_tokens
(
    id         BIGINT       PRIMARY KEY DEFAULT nextval('refresh_tokens_id_seq'),
    account_id BIGINT       NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_refresh_tokens_account_id ON refresh_tokens (account_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);

CREATE TABLE passenger_profiles
(
    account_id     BIGINT         PRIMARY KEY REFERENCES accounts (id) ON DELETE CASCADE,
    first_name     VARCHAR(100)   NOT NULL,
    last_name      VARCHAR(100)   NOT NULL,
    photo_url      VARCHAR(500),
    average_rating DECIMAL(3, 2)  NOT NULL DEFAULT 0.00,
    total_trips    INTEGER        NOT NULL DEFAULT 0
);

CREATE TABLE driver_profiles
(
    account_id     BIGINT         PRIMARY KEY REFERENCES accounts (id) ON DELETE CASCADE,
    first_name     VARCHAR(100)   NOT NULL,
    last_name      VARCHAR(100)   NOT NULL,
    photo_url      VARCHAR(500),
    license_number VARCHAR(50)    NOT NULL UNIQUE,
    status         driver_status  NOT NULL DEFAULT 'OFFLINE',
    average_rating DECIMAL(3, 2)  NOT NULL DEFAULT 0.00,
    total_trips    INTEGER        NOT NULL DEFAULT 0,
    is_verified    BOOLEAN        NOT NULL DEFAULT FALSE
);

CREATE TABLE vehicles
(
    id            BIGINT        PRIMARY KEY DEFAULT nextval('vehicles_id_seq'),
    driver_id     BIGINT        NOT NULL REFERENCES driver_profiles (account_id) ON DELETE CASCADE,
    brand         VARCHAR(100)  NOT NULL,
    model         VARCHAR(100)  NOT NULL,
    year          SMALLINT      NOT NULL,
    color         VARCHAR(50)   NOT NULL,
    license_plate VARCHAR(20)   NOT NULL UNIQUE,
    vehicle_class vehicle_class NOT NULL,
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_vehicles_driver_id ON vehicles (driver_id);

CREATE TABLE driver_locations
(
    driver_id  BIGINT         PRIMARY KEY REFERENCES driver_profiles (account_id) ON DELETE CASCADE,
    latitude   DECIMAL(10, 7) NOT NULL,
    longitude  DECIMAL(10, 7) NOT NULL,
    updated_at TIMESTAMP      NOT NULL DEFAULT now()
);