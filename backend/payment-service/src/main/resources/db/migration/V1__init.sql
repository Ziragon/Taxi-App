CREATE SEQUENCE IF NOT EXISTS driver_payout_accounts_id_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS payment_methods_id_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS transactions_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE driver_payout_accounts
(
    id                BIGINT       NOT NULL,
    driver_id         BIGINT       NOT NULL,
    stripe_account_id VARCHAR(255) NOT NULL,
    last_four         VARCHAR(4),
    is_verified       BOOLEAN      NOT NULL,
    is_default        BOOLEAN      NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_driver_payout_accounts PRIMARY KEY (id)
);

CREATE TABLE payment_methods
(
    id                       BIGINT       NOT NULL,
    passenger_id             BIGINT       NOT NULL,
    stripe_customer_id       VARCHAR(255) NOT NULL,
    stripe_payment_method_id VARCHAR(255) NOT NULL,
    card_brand               VARCHAR(255),
    last_four                VARCHAR(4),
    is_default               BOOLEAN      NOT NULL,
    is_active                BOOLEAN      NOT NULL,
    created_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_payment_methods PRIMARY KEY (id)
);

CREATE TABLE transactions
(
    id                       BIGINT         NOT NULL,
    trip_id                  BIGINT         NOT NULL,
    passenger_id             BIGINT         NOT NULL,
    driver_id                BIGINT         NOT NULL,
    type                     VARCHAR(255)   NOT NULL,
    amount                   DECIMAL(10, 2) NOT NULL,
    currency                 VARCHAR(3)     NOT NULL,
    status                   VARCHAR(255)   NOT NULL,
    stripe_payment_intent_id VARCHAR(255),
    payment_method_id        BIGINT,
    created_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_transactions PRIMARY KEY (id)
);

ALTER TABLE driver_payout_accounts
    ADD CONSTRAINT uc_driver_payout_accounts_stripe_account UNIQUE (stripe_account_id);

ALTER TABLE payment_methods
    ADD CONSTRAINT uc_payment_methods_stripe_payment_method UNIQUE (stripe_payment_method_id);

ALTER TABLE transactions
    ADD CONSTRAINT uc_transactions_stripe_payment_intent UNIQUE (stripe_payment_intent_id);

CREATE INDEX idx_driver_payout_accounts_driver_id ON driver_payout_accounts (driver_id);

CREATE INDEX idx_driver_payout_accounts_driver_id_is_default ON driver_payout_accounts (driver_id, is_default);

CREATE INDEX idx_driver_payout_accounts_driver_id_is_verified ON driver_payout_accounts (driver_id, is_verified);

CREATE UNIQUE INDEX idx_driver_payout_accounts_stripe_account_id ON driver_payout_accounts (stripe_account_id);

CREATE INDEX idx_payment_methods_passenger_id ON payment_methods (passenger_id);

CREATE INDEX idx_payment_methods_passenger_id_is_active ON payment_methods (passenger_id, is_active);

CREATE INDEX idx_payment_methods_passenger_id_is_default ON payment_methods (passenger_id, is_default);

CREATE UNIQUE INDEX idx_payment_methods_stripe_payment_method_id ON payment_methods (stripe_payment_method_id);

CREATE INDEX idx_transactions_driver_id ON transactions (driver_id);

CREATE INDEX idx_transactions_driver_id_status ON transactions (driver_id, status);

CREATE INDEX idx_transactions_passenger_id ON transactions (passenger_id);

CREATE INDEX idx_transactions_passenger_id_status ON transactions (passenger_id, status);

CREATE INDEX idx_transactions_status ON transactions (status);

CREATE UNIQUE INDEX idx_transactions_stripe_payment_intent_id ON transactions (stripe_payment_intent_id);

CREATE INDEX idx_transactions_trip_id ON transactions (trip_id);

ALTER TABLE transactions
    ADD CONSTRAINT FK_TRANSACTIONS_ON_PAYMENT_METHOD FOREIGN KEY (payment_method_id) REFERENCES payment_methods (id);