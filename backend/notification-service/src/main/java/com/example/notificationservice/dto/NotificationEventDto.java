package com.example.notificationservice.dto;

import com.example.notificationservice.entity.enums.Channel;
import com.example.notificationservice.entity.enums.EventType;
import com.example.notificationservice.entity.enums.RecipientType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationEventDto(

        Long tripId,

        EventType eventType,

        RecipientType recipientType,

        Long recipientId,

        Channel channel,

        String message
) {}
