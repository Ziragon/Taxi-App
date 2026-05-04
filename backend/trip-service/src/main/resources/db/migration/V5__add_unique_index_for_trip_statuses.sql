CREATE UNIQUE INDEX idx_trips_active_passenger
    ON trips (passenger_id)
    WHERE status IN ('SEARCHING', 'DRIVER_ASSIGNED', 'IN_PROGRESS');

CREATE UNIQUE INDEX idx_trips_active_driver
    ON trips (driver_id)
    WHERE status IN ('SEARCHING', 'DRIVER_ASSIGNED', 'IN_PROGRESS');