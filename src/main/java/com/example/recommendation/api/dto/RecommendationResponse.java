package com.example.recommendation.api.dto;

import com.example.recommendation.article.model.ArticleAccessLevel;
import java.util.List;

public record RecommendationResponse(
        String articleId,
        String title,
        ArticleAccessLevel accessLevel,
        double score,
        List<String> matchedTopics,
        List<String> matchedTags,
        List<String> topics,
        List<String> tags
) {
}
