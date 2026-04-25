package com.example.tripservice.entity;

import com.example.tripservice.dto.PriceBreakdown;
import com.example.tripservice.entity.enums.TripStatus;
import com.example.tripservice.entity.enums.VehicleClass;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trips",
        indexes = {
                @Index(name = "idx_trips_passenger_id", columnList = "passenger_id"),
                @Index(name = "idx_trips_driver_id", columnList = "driver_id"),
                @Index(name = "idx_trips_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trips_seq")
    @SequenceGenerator(name = "trips_seq", sequenceName = "trips_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "passenger_id", nullable = false)
    private Long passengerId;

    @Column(name = "driver_id")
    private Long driverId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TripStatus status = TripStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_class")
    private VehicleClass tripClass;

    @Column(name = "origin_address", nullable = false)
    private String originAddress;

    @Column(name = "origin_lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal originLat;

    @Column(name = "origin_lng", nullable = false, precision = 10, scale = 7)
    private BigDecimal originLng;

    @Column(name = "destination_address", nullable = false)
    private String destinationAddress;

    @Column(name = "destination_lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal destinationLat;

    @Column(name = "destination_lng", nullable = false, precision = 10, scale = 7)
    private BigDecimal destinationLng;

    @Column(name = "distance_km", nullable = false, precision = 6, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "duration_sec", nullable = false)
    private Integer durationSec;

    @Column(name = "weather_coefficient", nullable = false, precision = 4, scale = 2)
    private BigDecimal weatherCoef;

    @Column(name = "surge_coefficient", nullable = false, precision = 4, scale = 2)
    private BigDecimal surgeCoef;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private PriceBreakdown details;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "payment_id")
    private Long paymentId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL)
    private List<Rating> ratings = new ArrayList<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL)
    private List<TripStatusHistory> tripStatuses = new ArrayList<>();
}
