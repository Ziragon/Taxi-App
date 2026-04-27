ALTER TABLE trips
    ADD duration_min DECIMAL(6, 2);

ALTER TABLE trips
    ALTER COLUMN duration_min SET NOT NULL;

ALTER TABLE trips
    DROP COLUMN duration_sec;