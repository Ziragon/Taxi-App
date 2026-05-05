package com.example.notificationservice.service;

import com.example.notificationservice.dto.DriverProfileSnapshot;
import com.example.notificationservice.dto.NotificationEventDto;
import com.example.notificationservice.dto.NotificationPayload;
import com.example.notificationservice.dto.PassengerProfileSnapshot;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.entity.enums.NotificationStatus;
import com.example.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String USER_QUEUE_DESTINATION = "/queue/notifications";

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Notification save(NotificationEventDto event) {
        return notificationRepository.save(
                Notification.builder()
                        .tripId(event.tripId())
                        .eventType(event.eventType())
                        .recipientType(event.recipientType())
                        .recipientId(event.recipientId())
                        .channel(event.channel())
                        .message(event.message())
                        .status(NotificationStatus.PENDING)
                        .build()
        );
    }

    @Transactional
    public void markSent(Long id) {
        notificationRepository.updateStatus(id, NotificationStatus.SENT);
    }

    @Transactional
    public void markFailed(Long id) {
        notificationRepository.updateStatus(id, NotificationStatus.FAILED);
    }

    public void sendToUser(
            Notification notification,
            DriverProfileSnapshot driverProfile,
            PassengerProfileSnapshot passengerProfile
    ) {
        NotificationPayload payload = new NotificationPayload(
                notification.getId(),
                notification.getTripId(),
                notification.getEventType(),
                notification.getMessage(),
                notification.getCreatedAt(),
                driverProfile,
                passengerProfile
        );

        messagingTemplate.convertAndSendToUser(
                notification.getRecipientId().toString(),
                USER_QUEUE_DESTINATION,
                payload
        );

        log.debug("WS sent: notificationId={}, recipientId={}, event={}",
                notification.getId(),
                notification.getRecipientId(),
                notification.getEventType()
        );
    }
}
