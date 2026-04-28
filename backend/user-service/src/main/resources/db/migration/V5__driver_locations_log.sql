DROP INDEX IF EXISTS idx_driver_locations_lat_lng;

ALTER TABLE driver_locations
    RENAME COLUMN updated_at TO recorded_at;

ALTER TABLE driver_locations DROP CONSTRAINT driver_locations_pkey;

CREATE SEQUENCE IF NOT EXISTS driver_locations_seq
    START WITH 1
    INCREMENT BY 100;

ALTER TABLE driver_locations
    ADD COLUMN id BIGINT NOT NULL DEFAULT nextval('driver_locations_seq');

ALTER TABLE driver_locations
    ADD CONSTRAINT driver_locations_pkey PRIMARY KEY (id);

ALTER TABLE driver_locations
    ADD CONSTRAINT fk_driver_locations_driver
        FOREIGN KEY (driver_id) REFERENCES driver_profiles(account_id);

CREATE INDEX idx_driver_locations_driver_recorded
    ON driver_locations (driver_id, recorded_at);

CREATE INDEX idx_driver_locations_recorded
    ON driver_locations (recorded_at);