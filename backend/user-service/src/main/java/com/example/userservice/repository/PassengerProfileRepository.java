package com.example.userservice.repository;

import com.example.userservice.entity.PassengerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PassengerProfileRepository extends JpaRepository<PassengerProfile, Long> {
}
