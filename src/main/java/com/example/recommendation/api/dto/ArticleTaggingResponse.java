package com.example.recommendation.api.dto;

import com.example.recommendation.article.extractor.ArticleFeatureExtractor;
import java.util.List;

public record ArticleTaggingResponse(
        List<String> topics,
        List<String> tags
) {
    public static ArticleTaggingResponse from(ArticleFeatureExtractor.ArticleFeatures features) {
        return new ArticleTaggingResponse(List.copyOf(features.topics()), List.copyOf(features.tags()));
    }
}
