package com.example.recommendation.api.dto;

import com.example.recommendation.article.model.ArticleAccessLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateArticleRequest(
        @NotBlank(message = "must not be blank") String title,
        @NotBlank(message = "must not be blank") String content,
        @NotNull(message = "must not be null") Long authorId,
        @NotNull(message = "must not be null") Long publisherId,
        ArticleAccessLevel accessLevel,
        List<String> topics,
        List<String> tags
) {
}
