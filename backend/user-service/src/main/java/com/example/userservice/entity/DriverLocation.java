package com.example.userservice.entity;

import com.example.shared.dto.enums.VehicleClass;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "driver_locations", indexes = {
        @Index(name = "idx_driver_locations_driver_recorded", columnList = "driver_id, recorded_at"),
        @Index(name = "idx_driver_locations_recorded", columnList = "recorded_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "driver_location_seq")
    @SequenceGenerator(
            name = "driver_location_seq",
            sequenceName = "driver_locations_seq",
            allocationSize = 100
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private DriverProfile driver;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_class", nullable = false)
    private VehicleClass vehicleClass;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
}
