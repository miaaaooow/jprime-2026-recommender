package com.example.recommendation.api.dto;

import java.time.Instant;
import java.util.List;

public record AuthorResponse(
        Long id,
        String name,
        double internalRating,
        Long publisherId,
        String publisherName,
        List<ArticleCatalogItemResponse> articles,
        Instant createdAt
) {
}
