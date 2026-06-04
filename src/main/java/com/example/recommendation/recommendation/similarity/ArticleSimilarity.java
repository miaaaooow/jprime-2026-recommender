package com.example.recommendation.recommendation.similarity;

import java.util.List;

public record ArticleSimilarity(
        double score,
        List<String> matchedTopics,
        List<String> matchedTags
) {
}
