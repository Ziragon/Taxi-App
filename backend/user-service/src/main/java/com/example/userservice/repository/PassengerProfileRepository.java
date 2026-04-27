package com.example.userservice.repository;

import com.example.userservice.entity.PassengerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PassengerProfileRepository extends JpaRepository<PassengerProfile, Long> {

    @Modifying
    @Query("""
            UPDATE PassengerProfile p
            SET p.averageRating = (p.averageRating * p.totalTrips + :newRating) / (p.totalTrips + 1),
                p.totalTrips = p.totalTrips + 1
            WHERE p.accountId = :accountId
            """)
    void updateRating(@Param("accountId") Long accountId, @Param("newRating") BigDecimal newRating);

    @Query("SELECT pp FROM PassengerProfile pp JOIN FETCH pp.account")
    List<PassengerProfile> findAllWithAccount();
}
