package com.example.recommendation.article.service;

import com.example.recommendation.article.extractor.ArticleFeatureExtractor;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ArticleTaggingService {

    private final ArticleFeatureExtractor articleFeatureExtractor;

    public ArticleTaggingService(ArticleFeatureExtractor articleFeatureExtractor) {
        this.articleFeatureExtractor = articleFeatureExtractor;
    }

    public ArticleFeatureExtractor.ArticleFeatures extractFeatures(
            String title,
            String content,
            List<String> topics,
            List<String> tags
    ) {
        return articleFeatureExtractor.extract(title, content, topics, tags);
    }
}
