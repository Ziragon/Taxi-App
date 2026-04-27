package com.example.tripservice.repository;

import com.example.tripservice.entity.TripStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripStatusRepository extends JpaRepository<TripStatusHistory, Long> {
}
