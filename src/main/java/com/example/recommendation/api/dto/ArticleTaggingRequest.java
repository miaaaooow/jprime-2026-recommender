package com.example.recommendation.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ArticleTaggingRequest(
        @NotBlank(message = "must not be blank") String title,
        @NotBlank(message = "must not be blank") String content,
        List<String> topics,
        List<String> tags
) {
}
