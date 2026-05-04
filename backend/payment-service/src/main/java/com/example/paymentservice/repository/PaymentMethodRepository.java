package com.example.paymentservice.repository;

import com.example.paymentservice.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findAllByPassengerId(Long passengerId);

    List<PaymentMethod> findAllByPassengerIdAndActiveTrue(Long passengerId);

    Optional<PaymentMethod> findByPassengerIdAndDefaultvalueTrue(Long passengerId);

    boolean existsByPassengerIdAndStripePaymentMethodId(Long passengerId, String stripePaymentMethodId);

    @Modifying
    @Query("UPDATE PaymentMethod pm SET pm.defaultvalue = false WHERE pm.passengerId = :passengerId")
    int clearDefaultForPassenger(@Param("passengerId") Long passengerId);

    @Modifying
    @Query("UPDATE PaymentMethod pm SET pm.active = false WHERE pm.id = :id")
    int deactivateById(@Param("id") Long id);

    @Modifying
    @Query("DELETE FROM PaymentMethod pm WHERE pm.id = :id")
    int deleteById_(@Param("id") Long id);
}