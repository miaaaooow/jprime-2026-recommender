package com.example.recommendation.api.dto;

import com.example.recommendation.article.model.ArticleDocument;
import java.time.Instant;

public record ArticleCatalogItemResponse(
        String id,
        String title,
        Long authorId,
        String authorName,
        Instant createdAt
) {
    public static ArticleCatalogItemResponse from(ArticleDocument article) {
        return new ArticleCatalogItemResponse(
                article.getId(),
                article.getTitle(),
                article.getAuthorId(),
                article.getAuthorName(),
                article.getCreatedAt()
        );
    }
}
