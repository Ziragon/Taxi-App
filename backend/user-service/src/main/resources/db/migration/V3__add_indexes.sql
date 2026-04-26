ALTER TABLE driver_profiles ADD COLUMN version BIGINT DEFAULT 0;

CREATE INDEX idx_driver_profiles_status ON driver_profiles (status);
CREATE UNIQUE INDEX idx_driver_profiles_license_number ON driver_profiles (license_number);
CREATE UNIQUE INDEX idx_vehicles_license_plate ON vehicles (license_plate);
CREATE INDEX idx_driver_locations_lat_lng ON driver_locations (latitude, longitude);