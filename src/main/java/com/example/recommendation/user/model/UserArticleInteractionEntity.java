package com.example.recommendation.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "user_article_interactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "article_id", "interaction_type"})
)
public class UserArticleInteractionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "article_id", nullable = false)
    private String articleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false)
    private UserArticleInteractionType interactionType;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected UserArticleInteractionEntity() {
    }

    public UserArticleInteractionEntity(Long userId, String articleId, UserArticleInteractionType interactionType) {
        this.userId = userId;
        this.articleId = articleId;
        this.interactionType = interactionType;
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

    public String getArticleId() {
        return articleId;
    }

    public UserArticleInteractionType getInteractionType() {
        return interactionType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
