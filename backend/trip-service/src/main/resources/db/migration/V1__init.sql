CREATE SEQUENCE IF NOT EXISTS ratings_id_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS tariffs_id_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS trip_status_id_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS trips_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE ratings
(
    id         BIGINT                      NOT NULL,
    trip_id    BIGINT,
    rated_by   VARCHAR(255)                NOT NULL,
    rater_id   BIGINT                      NOT NULL,
    ratee_id   BIGINT                      NOT NULL,
    score      INTEGER                     NOT NULL,
    comment    TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_ratings PRIMARY KEY (id)
);

CREATE TABLE tariffs
(
    id            BIGINT        NOT NULL,
    trip_class    VARCHAR(255),
    base_fare     DECIMAL(8, 2) NOT NULL,
    price_per_km  DECIMAL(8, 2) NOT NULL,
    price_per_min DECIMAL(8, 2) NOT NULL,
    is_active     BOOLEAN       NOT NULL,
    CONSTRAINT pk_tariffs PRIMARY KEY (id)
);

CREATE TABLE trip_status_history
(
    id              BIGINT                      NOT NULL,
    trip_id         BIGINT                      NOT NULL,
    previous_status VARCHAR(255)                NOT NULL,
    new_status      VARCHAR(255)                NOT NULL,
    changed_by      VARCHAR(255)                NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_trip_status_history PRIMARY KEY (id)
);

CREATE TABLE trips
(
    id                  BIGINT                      NOT NULL,
    passenger_id        BIGINT                      NOT NULL,
    driver_id           BIGINT,
    status              VARCHAR(255)                NOT NULL,
    trip_class          VARCHAR(255),
    origin_address      VARCHAR(255)                NOT NULL,
    origin_lat          DECIMAL(10, 7)              NOT NULL,
    origin_lng          DECIMAL(10, 7)              NOT NULL,
    destination_address VARCHAR(255)                NOT NULL,
    destination_lat     DECIMAL(10, 7)              NOT NULL,
    destination_lng     DECIMAL(10, 7)              NOT NULL,
    distance_km         DECIMAL(6, 2)               NOT NULL,
    duration_sec        INTEGER                     NOT NULL,
    weather_coefficient DECIMAL(4, 2)               NOT NULL,
    surge_coefficient   DECIMAL(4, 2)               NOT NULL,
    details             JSONB,
    price               DECIMAL(10, 2),
    payment_id          BIGINT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_trips PRIMARY KEY (id)
);

ALTER TABLE ratings
    ADD CONSTRAINT ek_trip_raters UNIQUE (trip_id, rater_id, ratee_id);

CREATE INDEX idx_ratings_ratee_id ON ratings (ratee_id);

CREATE INDEX idx_trips_driver_id ON trips (driver_id);

CREATE INDEX idx_trips_passenger_id ON trips (passenger_id);

CREATE INDEX idx_trips_status ON trips (status);

ALTER TABLE ratings
    ADD CONSTRAINT FK_RATINGS_ON_TRIP FOREIGN KEY (trip_id) REFERENCES trips (id);

CREATE INDEX idx_ratings_trip_id ON ratings (trip_id);

ALTER TABLE trip_status_history
    ADD CONSTRAINT FK_TRIP_STATUS_HISTORY_ON_TRIP FOREIGN KEY (trip_id) REFERENCES trips (id);

CREATE INDEX idx_status_history_trip_id ON trip_status_history (trip_id);