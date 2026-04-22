ALTER TABLE accounts ALTER COLUMN role DROP DEFAULT;
ALTER TABLE driver_profiles ALTER COLUMN status DROP DEFAULT;
ALTER TABLE vehicles ALTER COLUMN vehicle_class DROP DEFAULT;

ALTER TABLE accounts
    ALTER COLUMN role TYPE VARCHAR(255)
        USING role::text;

ALTER TABLE driver_profiles
    ALTER COLUMN status TYPE VARCHAR(255)
        USING status::text;

ALTER TABLE vehicles
    ALTER COLUMN vehicle_class TYPE VARCHAR(255)
        USING vehicle_class::text;

DROP TYPE account_role;
DROP TYPE driver_status;
DROP TYPE vehicle_class;