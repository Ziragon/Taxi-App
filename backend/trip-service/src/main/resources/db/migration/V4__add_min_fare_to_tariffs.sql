ALTER TABLE tariffs
    ADD min_fare DECIMAL(8, 2);

UPDATE tariffs SET min_fare = 200.0 WHERE id = 1;
UPDATE tariffs SET min_fare = 300.0 WHERE id = 2;
UPDATE tariffs SET min_fare = 450.0 WHERE id = 3;

ALTER TABLE tariffs
    ALTER COLUMN min_fare SET NOT NULL;