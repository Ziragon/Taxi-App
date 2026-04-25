package com.example.paymentservice.repository;

import com.example.paymentservice.entity.DriverPayoutAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverPayoutAccountRepository extends JpaRepository<DriverPayoutAccount, Long> {

    List<DriverPayoutAccount> findAllByDriverId(Long driverId);

    Optional<DriverPayoutAccount> findByDriverIdAndDefaultvalueTrue(Long driverId);

    Optional<DriverPayoutAccount> findByStripeAccountId(String stripeAccountId);

    List<DriverPayoutAccount> findAllByVerifiedTrue();

    boolean existsByDriverIdAndStripeAccountId(Long driverId, String stripeAccountId);

    @Modifying
    @Query("UPDATE DriverPayoutAccount dpa SET dpa.defaultvalue = false WHERE dpa.driverId = :driverId")
    int clearDefaultForDriver(@Param("driverId") Long driverId);

    @Modifying
    @Query("UPDATE DriverPayoutAccount dpa SET dpa.verified = :verified WHERE dpa.id = :id")
    int updateVerificationStatus(@Param("id") Long id, @Param("verified") boolean verified);
}
