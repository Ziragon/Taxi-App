package com.example.tripservice.entity;

import com.example.tripservice.entity.enums.AccountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "ratings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ek_trip_raters",
                        columnNames = {"trip_id", "rater_id", "ratee_id"}
                )},
        indexes = {
                @Index(name = "idx_ratings_ratee_id", columnList = "ratee_id"),
                @Index(name = "idx_ratings_trip_id", columnList = "trip_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ratings_seq")
    @SequenceGenerator(name = "ratings_seq", sequenceName = "ratings_id_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Enumerated(EnumType.STRING)
    @Column(name = "rated_by", nullable = false)
    private AccountType ratedBy;

    @Column(name = "rater_id", nullable = false)
    private Long raterId;

    @Column(name = "ratee_id", nullable = false)
    private Long rateeId;

    @Column(name = "score", nullable = false, check = @CheckConstraint(constraint = "score >= 1 AND score <= 5"))
    private Integer score;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
