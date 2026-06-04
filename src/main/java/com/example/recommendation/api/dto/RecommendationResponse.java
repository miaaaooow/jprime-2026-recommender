package com.example.recommendation.api.dto;

import java.util.List;

public record RecommendationResponse(
        String articleId,
        String title,
        double score,
        List<String> matchedTopics,
        List<String> matchedTags,
        List<String> topics,
        List<String> tags
) {
}
