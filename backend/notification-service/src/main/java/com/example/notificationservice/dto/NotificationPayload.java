package com.example.notificationservice.dto;

import com.example.notificationservice.entity.enums.EventType;

import java.time.Instant;

public record NotificationPayload(

        Long notificationId,

        Long tripId,

        EventType eventType,

        String message,

        Instant createdAt,

        DriverProfileSnapshot driverProfile,

        PassengerProfileSnapshot passengerProfile
) {}
