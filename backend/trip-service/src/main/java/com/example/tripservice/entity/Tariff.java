package com.example.tripservice.entity;

import com.example.shared.dto.enums.VehicleClass;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tariffs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tariff {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tariffs_seq")
    @SequenceGenerator(name = "tariffs_seq", sequenceName = "tariffs_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_class")
    private VehicleClass tripClass;

    @Column(name = "base_fare", nullable = false, precision = 8, scale = 2)
    private BigDecimal baseFare;

    @Column(name = "price_per_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal pricePerKm;

    @Column(name = "price_per_min", nullable = false, precision = 8, scale = 2)
    private BigDecimal pricePerMin;

    @Column(name = "min_fare", nullable = false, precision = 8, scale = 2)
    private BigDecimal minFare;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
