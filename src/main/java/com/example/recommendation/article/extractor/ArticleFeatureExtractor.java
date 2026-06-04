package com.example.recommendation.article.extractor;

import java.util.List;

public interface ArticleFeatureExtractor {

    ArticleFeatures extract(String title, String content, List<String> topics, List<String> tags);

    record ArticleFeatures(List<String> topics, List<String> tags) {
    }
}
