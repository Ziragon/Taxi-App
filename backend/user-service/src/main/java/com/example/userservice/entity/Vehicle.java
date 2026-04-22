package com.example.userservice.entity;

import com.example.userservice.entity.enums.VehicleClass;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vehicles_seq")
    @SequenceGenerator(name = "vehicles_seq", sequenceName = "vehicles_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private DriverProfile driver;

    @Column(name = "brand", nullable = false, length = 100)
    private String brand;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "year", nullable = false)
    private Short year;

    @Column(name = "color", nullable = false, length = 50)
    private String color;

    @Column(name = "license_plate", nullable = false, unique = true, length = 20)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_class", nullable = false)
    private VehicleClass vehicleClass;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
