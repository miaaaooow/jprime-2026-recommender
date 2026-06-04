package com.example.recommendation.article.extractor;

import com.example.recommendation.config.ArticleTaggingProperties;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class SmartKeywordArticleFeatureExtractor implements ArticleFeatureExtractor {

    private final KeywordArticleFeatureExtractor keywordArticleFeatureExtractor;
    private final SmartArticleTaggingClient smartArticleTaggingClient;
    private final ArticleTaggingProperties properties;

    public SmartKeywordArticleFeatureExtractor(
            KeywordArticleFeatureExtractor keywordArticleFeatureExtractor,
            SmartArticleTaggingClient smartArticleTaggingClient,
            ArticleTaggingProperties properties
    ) {
        this.keywordArticleFeatureExtractor = keywordArticleFeatureExtractor;
        this.smartArticleTaggingClient = smartArticleTaggingClient;
        this.properties = properties;
    }

    @Override
    public ArticleFeatures extract(String title, String content, List<String> topics, List<String> tags) {
        ArticleFeatures keywordFeatures = keywordArticleFeatureExtractor.extract(title, content, topics, tags);

        if (!"smart-keyword".equalsIgnoreCase(properties.getExtractor())) {
            return keywordFeatures;
        }

        return smartArticleTaggingClient.extract(
                        title,
                        content,
                        keywordFeatures.topics(),
                        keywordFeatures.tags()
                )
                .map(smartFeatures -> new ArticleFeatures(
                        merge(keywordFeatures.topics(), smartFeatures.topics()),
                        merge(keywordFeatures.tags(), smartFeatures.tags())
                ))
                .orElse(keywordFeatures);
    }

    private List<String> merge(List<String> left, List<String> right) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.addAll(normalize(left));
        merged.addAll(normalize(right));
        return merged.stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private List<String> normalize(List<String> values) {
        if (values == null) {
            return List.of();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }
}
