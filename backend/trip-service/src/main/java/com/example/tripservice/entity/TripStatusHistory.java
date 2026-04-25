package com.example.tripservice.entity;

import com.example.tripservice.entity.enums.AccountType;
import com.example.tripservice.entity.enums.TripStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "trip_status_history",
        indexes = {
                @Index(name = "idx_status_history_trip_id", columnList = "trip_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trip_status_seq")
    @SequenceGenerator(name = "trip_status_seq", sequenceName = "trip_status_id_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false)
    private TripStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private TripStatus newStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "changed_by", nullable = false)
    private AccountType changedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
