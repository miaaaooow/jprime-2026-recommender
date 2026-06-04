package com.example.recommendation.article.extractor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class KeywordArticleFeatureExtractorTest {

    private final KeywordArticleFeatureExtractor extractor = new KeywordArticleFeatureExtractor();

    @Test
    void extractMergesManualAndDetectedLabels() {
        ArticleFeatureExtractor.ArticleFeatures features = extractor.extract(
                "Government reviews AI platform rules",
                "The minister outlined new policy for artificial intelligence systems.",
                List.of("analysis"),
                List.of("editor-pick")
        );

        assertThat(features.topics()).contains("analysis", "politics", "technology");
        assertThat(features.tags()).contains("ai", "editor-pick", "policy");
    }

    @Test
    void extractFallsBackToGenericLabelsWhenNoKeywordsMatch() {
        ArticleFeatureExtractor.ArticleFeatures features = extractor.extract(
                "Quiet updates",
                "A short internal note without obvious category signals.",
                null,
                null
        );

        assertThat(features.topics()).containsExactly("current-affairs");
        assertThat(features.tags()).containsExactly("news");
    }
}
