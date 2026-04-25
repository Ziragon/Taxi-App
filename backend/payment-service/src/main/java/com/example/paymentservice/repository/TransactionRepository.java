package com.example.paymentservice.repository;

import com.example.paymentservice.entity.Transaction;
import com.example.paymentservice.entity.enums.TransactionStatus;
import com.example.paymentservice.entity.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTripId(Long tripId);

    Optional<Transaction> findByStripePaymentIntentId(String stripePaymentIntentId);

    List<Transaction> findAllByPassengerId(Long passengerId);

    List<Transaction> findAllByDriverId(Long driverId);

    List<Transaction> findAllByStatus(TransactionStatus status);

    List<Transaction> findAllByType(TransactionType type);

    List<Transaction> findAllByPassengerIdAndStatus(Long passengerId, TransactionStatus status);

    List<Transaction> findAllByDriverIdAndStatus(Long driverId, TransactionStatus status);

    @Query("SELECT t FROM Transaction t WHERE t.passengerId = :passengerId ORDER BY t.createdAt DESC")
    List<Transaction> findRecentByPassengerId(@Param("passengerId") Long passengerId);

    @Query("SELECT t FROM Transaction t WHERE t.driverId = :driverId ORDER BY t.createdAt DESC")
    List<Transaction> findRecentByDriverId(@Param("driverId") Long driverId);
}
