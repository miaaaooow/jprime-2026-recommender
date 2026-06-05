package com.example.recommendation.subscription.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "user_publisher_subscriptions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "publisher_id"})
)
public class UserPublisherSubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "publisher_id", nullable = false)
    private Long publisherId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected UserPublisherSubscriptionEntity() {
    }

    public UserPublisherSubscriptionEntity(Long userId, Long publisherId) {
        this.userId = userId;
        this.publisherId = publisherId;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getPublisherId() {
        return publisherId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
