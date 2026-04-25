package com.example.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "payment_methods",
        indexes = {
                @Index(name = "idx_payment_methods_passenger_id", columnList = "passenger_id"),
                @Index(name = "idx_payment_methods_stripe_payment_method_id", columnList = "stripe_payment_method_id", unique = true),
                @Index(name = "idx_payment_methods_passenger_id_is_default", columnList = "passenger_id, is_default"),
                @Index(name = "idx_payment_methods_passenger_id_is_active", columnList = "passenger_id, is_active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_methods_seq")
    @SequenceGenerator(name = "payment_methods_seq", sequenceName = "payment_methods_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "passenger_id", nullable = false)
    private Long passengerId;

    @Column(name = "stripe_customer_id", nullable = false)
    private String stripeCustomerId;

    @Column(name = "stripe_payment_method_id", nullable = false, unique = true)
    private String stripePaymentMethodId;

    @Column(name = "card_brand")
    private String cardBrand;

    @Column(name = "last_four", length = 4)
    private String lastFour;

    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private boolean defaultvalue = false;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "paymentMethod", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();
}
