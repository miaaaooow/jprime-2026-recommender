package com.example.recommendation.user.model;

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
        name = "user_article_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "article_id"})
)
public class UserArticleLikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "article_id", nullable = false)
    private String articleId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected UserArticleLikeEntity() {
    }

    public UserArticleLikeEntity(Long userId, String articleId) {
        this.userId = userId;
        this.articleId = articleId;
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
