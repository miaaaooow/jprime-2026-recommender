package com.example.recommendation.api.dto;

import java.time.Instant;
import java.util.List;

public record PublisherResponse(
        Long id,
        String name,
        List<PublisherAuthorResponse> authors,
        List<ArticleCatalogItemResponse> articles,
        Instant createdAt
) {
}
