package com.example.recommendation.article.extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.recommendation.config.ArticleTaggingProperties;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SmartKeywordArticleFeatureExtractorTest {

    private final KeywordArticleFeatureExtractor keywordArticleFeatureExtractor = new KeywordArticleFeatureExtractor();
    private final SmartArticleTaggingClient smartArticleTaggingClient = mock(SmartArticleTaggingClient.class);
    private final ArticleTaggingProperties properties = new ArticleTaggingProperties();
    private final SmartKeywordArticleFeatureExtractor extractor = new SmartKeywordArticleFeatureExtractor(
            keywordArticleFeatureExtractor,
            smartArticleTaggingClient,
            properties
    );

    @Test
    void extractFallsBackToKeywordExtractorWhenSmartModeIsNotSelected() {
        ArticleFeatureExtractor.ArticleFeatures features = extractor.extract(
                "Government reviews AI platform rules",
                "The minister outlined new policy for artificial intelligence systems.",
                List.of("analysis"),
                List.of("editor-pick")
        );

        assertThat(features.topics()).contains("analysis", "politics", "technology");
        assertThat(features.tags()).contains("ai", "editor-pick", "policy");
        verifyNoInteractions(smartArticleTaggingClient);
    }

    @Test
    void extractMergesKeywordAndSmartLabelsWhenSmartModeIsSelected() {
        properties.setExtractor("smart-keyword");
        when(smartArticleTaggingClient.extract(
                "Government reviews AI platform rules",
                "The minister outlined new policy for artificial intelligence systems.",
                List.of("analysis", "politics", "technology"),
                List.of("ai", "editor-pick", "policy")
        )).thenReturn(Optional.of(new ArticleFeatureExtractor.ArticleFeatures(
                List.of("current-affairs", "technology"),
                List.of("ai", "regulation")
        )));

        ArticleFeatureExtractor.ArticleFeatures features = extractor.extract(
                "Government reviews AI platform rules",
                "The minister outlined new policy for artificial intelligence systems.",
                List.of("analysis"),
                List.of("editor-pick")
        );

        assertThat(features.topics()).containsExactly("analysis", "current-affairs", "politics", "technology");
        assertThat(features.tags()).containsExactly("ai", "editor-pick", "policy", "regulation");
    }
}
