package com.example.tripservice.repository;

import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    Optional<Trip> findFirstByPassengerIdAndStatusOrderByCreatedAtDesc(Long userId, TripStatus tripStatus);

    boolean existsByPassengerIdAndStatusIn(Long userId, List<TripStatus> searching);

    Page<Trip> findByPassengerIdOrderByCreatedAtDesc(Long passengerId, Pageable pageable);

    Page<Trip> findByDriverIdOrderByCreatedAtDesc(Long driverId, Pageable pageable);
}
