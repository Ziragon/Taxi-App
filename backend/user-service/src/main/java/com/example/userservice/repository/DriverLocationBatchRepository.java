package com.example.userservice.repository;

import com.example.userservice.dto.data.DriverLocationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DriverLocationBatchRepository {

    private final JdbcTemplate jdbcTemplate;

    public void batchUpsert(List<DriverLocationDto> locations) {
        String sql = """
            INSERT INTO driver_locations (driver_id, longitude, latitude, updated_at)
            VALUES (?, ?, ?, NOW())
            ON CONFLICT (driver_id)
            DO UPDATE SET
                longitude = EXCLUDED.longitude,
                latitude  = EXCLUDED.latitude,
                updated_at = EXCLUDED.updated_at
            """;

        jdbcTemplate.batchUpdate(sql, locations, locations.size(),
                (ps, dto) -> {
                    ps.setLong(1, dto.driverId());
                    ps.setBigDecimal(2, dto.location().longitude());
                    ps.setBigDecimal(3, dto.location().latitude());
                });
    }
}