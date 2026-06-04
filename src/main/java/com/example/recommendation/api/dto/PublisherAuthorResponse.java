package com.example.recommendation.api.dto;

import com.example.recommendation.author.model.AuthorEntity;

public record PublisherAuthorResponse(
        Long id,
        String name,
        double internalRating
) {
    public static PublisherAuthorResponse from(AuthorEntity author) {
        return new PublisherAuthorResponse(author.getId(), author.getName(), author.getInternalRating());
    }
}
