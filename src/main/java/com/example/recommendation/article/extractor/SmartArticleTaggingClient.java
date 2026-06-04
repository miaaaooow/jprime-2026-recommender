package com.example.recommendation.article.extractor;

import java.util.List;
import java.util.Optional;

public interface SmartArticleTaggingClient {

    Optional<ArticleFeatureExtractor.ArticleFeatures> extract(
            String title,
            String content,
            List<String> candidateTopics,
            List<String> candidateTags
    );
}
