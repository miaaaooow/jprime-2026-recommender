package com.example.recommendation.author.model;

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
        name = "authors",
        uniqueConstraints = @UniqueConstraint(columnNames = {"publisher_id", "name"})
)
public class AuthorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "publisher_id", nullable = false)
    private Long publisherId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double internalRating;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected AuthorEntity() {
    }

    public AuthorEntity(Long publisherId, String name, double internalRating) {
        this.publisherId = publisherId;
        this.name = name;
        this.internalRating = internalRating;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPublisherId() {
        return publisherId;
    }

    public String getName() {
        return name;
    }

    public double getInternalRating() {
        return internalRating;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
