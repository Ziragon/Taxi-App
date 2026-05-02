ALTER TABLE driver_locations
    ADD vehicle_class VARCHAR(255);

ALTER TABLE driver_locations
    ALTER COLUMN vehicle_class SET NOT NULL;