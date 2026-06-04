package com.example.recommendation.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.recommendation.api.dto.RecommendationResponse;
import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.article.repository.ArticleRepository;
import com.example.recommendation.config.RecommendationProperties;
import com.example.recommendation.profile.model.UserProfileDocument;
import com.example.recommendation.profile.service.UserProfileService;
import com.example.recommendation.recommendation.similarity.FixedWeightUserArticleSimilarityModel;
import com.example.recommendation.recommendation.similarity.UserArticleSimilarityModelRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private UserProfileService userProfileService;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        RecommendationProperties recommendationProperties = new RecommendationProperties();
        UserArticleSimilarityModelRegistry similarityModelRegistry = new UserArticleSimilarityModelRegistry(
                List.of(new FixedWeightUserArticleSimilarityModel(recommendationProperties)),
                recommendationProperties
        );
        recommendationService = new RecommendationService(
                articleRepository,
                userProfileService,
                similarityModelRegistry
        );
    }

    @Test
    void recommendForUserRanksArticlesByProfileOverlap() {
        UserProfileDocument profile = new UserProfileDocument();
        profile.setUserId(3L);
        profile.setSimilarityModelKey("fixed-weight");
        profile.setReadArticleIds(List.of("read-1"));
        profile.setLikedArticleIds(List.of("liked-1"));
        profile.setSharedArticleIds(List.of("shared-1"));
        profile.setTopicWeights(Map.of("ai", 0.7, "search", 0.3));
        profile.setTagWeights(Map.of("ml", 0.6, "ranking", 0.4));

        when(userProfileService.getProfile(3L)).thenReturn(profile);
        when(articleRepository.findAll()).thenReturn(List.of(
                new ArticleDocument("read-1", "Seen article", "...", List.of("ai"), List.of("ml"), Instant.now()),
                new ArticleDocument("liked-1", "Seen article", "...", List.of("ai"), List.of("ml"), Instant.now()),
                new ArticleDocument("shared-1", "Seen article", "...", List.of("ai"), List.of("ml"), Instant.now()),
                new ArticleDocument("candidate-1", "AI ranking systems", "...", List.of("ai", "search"), List.of("ranking", "ml"), Instant.now()),
                new ArticleDocument("candidate-2", "Gardening tips", "...", List.of("gardening"), List.of("plants"), Instant.now())
        ));

        List<RecommendationResponse> recommendations = recommendationService.recommendForUser(3L, 5);

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).articleId()).isEqualTo("candidate-1");
        assertThat(recommendations.get(0).matchedTopics()).containsExactly("ai", "search");
        assertThat(recommendations.get(0).matchedTags()).containsExactly("ml", "ranking");
        assertThat(recommendations.get(0).score()).isGreaterThan(0.0);
    }
}
