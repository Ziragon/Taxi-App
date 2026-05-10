package com.example.tripservice.repository;

import com.example.tripservice.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    boolean existsByTripIdAndRaterIdAndRateeId(Long tripId, Long raterId, Long rateeId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.rateeId = :rateeId")
    Optional<Double> findAverageScoreByRateeId(@Param("rateeId") Long rateeId);
}
