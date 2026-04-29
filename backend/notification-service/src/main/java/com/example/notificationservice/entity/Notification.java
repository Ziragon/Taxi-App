package com.example.notificationservice.entity;

import com.example.notificationservice.entity.enums.Channel;
import com.example.notificationservice.entity.enums.EventType;
import com.example.notificationservice.entity.enums.NotificationStatus;
import com.example.notificationservice.entity.enums.RecipientType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_trip_id",
                        columnList = "trip_id"),
                @Index(name = "idx_notifications_recipient_id",
                        columnList = "recipient_id"),
                @Index(name = "idx_notifications_status",
                        columnList = "status"),
                @Index(name = "idx_notifications_recipient_id_status",
                        columnList = "recipient_id, status"),
                @Index(name = "idx_notifications_event_type",
                        columnList = "event_type")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notifications_seq")
    @SequenceGenerator(
            name = "notifications_seq",
            sequenceName = "notifications_id_seq",
            allocationSize = 50
    )
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "trip_id")
    private Long tripId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 16)
    private RecipientType recipientType;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private Channel channel;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private NotificationStatus status = NotificationStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
