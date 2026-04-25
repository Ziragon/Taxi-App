package com.example.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "driver_payout_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverPayoutAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "driver_payout_accounts_seq")
    @SequenceGenerator(name = "driver_payout_accounts_seq", sequenceName = "driver_payout_accounts_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "stripe_account_id", nullable = false)
    private String stripeAccountId;

    @Column(name = "last_four", length = 4)
    private String lastFour;

    @Builder.Default
    @Column(name = "is_verified", nullable = false)
    private boolean verified = false;

    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
