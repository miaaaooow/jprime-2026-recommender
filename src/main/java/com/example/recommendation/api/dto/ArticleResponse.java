package com.example.recommendation.api.dto;

import com.example.recommendation.article.model.ArticleAccessLevel;
import com.example.recommendation.article.model.ArticleDocument;
import java.time.Instant;
import java.util.List;

public record ArticleResponse(
        String id,
        String title,
        String content,
        Long authorId,
        String authorName,
        Long publisherId,
        String publisherName,
        ArticleAccessLevel accessLevel,
        List<String> topics,
        List<String> tags,
        Instant createdAt
) {
    public static ArticleResponse from(ArticleDocument article) {
        return new ArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                article.getAuthorId(),
                article.getAuthorName(),
                article.getPublisherId(),
                article.getPublisherName(),
                article.getAccessLevel(),
                List.copyOf(article.getTopics()),
                List.copyOf(article.getTags()),
                article.getCreatedAt()
        );
    }
}
