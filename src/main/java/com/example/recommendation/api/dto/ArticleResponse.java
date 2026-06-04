package com.example.recommendation.api.dto;

import com.example.recommendation.article.model.ArticleDocument;
import java.time.Instant;
import java.util.List;

public record ArticleResponse(
        String id,
        String title,
        String content,
        List<String> topics,
        List<String> tags,
        Instant createdAt
) {
    public static ArticleResponse from(ArticleDocument article) {
        return new ArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                List.copyOf(article.getTopics()),
                List.copyOf(article.getTags()),
                article.getCreatedAt()
        );
    }
}
