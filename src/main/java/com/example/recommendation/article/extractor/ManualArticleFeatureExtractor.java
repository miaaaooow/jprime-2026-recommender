package com.example.recommendation.article.extractor;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ManualArticleFeatureExtractor implements ArticleFeatureExtractor {

    @Override
    public ArticleFeatures extract(String content, List<String> topics, List<String> tags) {
        return new ArticleFeatures(normalize(topics), normalize(tags));
    }

    private List<String> normalize(List<String> values) {
        if (values == null) {
            return List.of();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
