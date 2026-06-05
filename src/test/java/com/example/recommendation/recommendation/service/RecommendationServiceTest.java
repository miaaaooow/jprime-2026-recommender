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
import com.example.recommendation.subscription.service.UserSubscriptionService;
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

    @Mock
    private UserSubscriptionService userSubscriptionService;

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
                similarityModelRegistry,
                recommendationProperties,
                userSubscriptionService
        );
    }

    @Test
    void recommendForUserRanksFamiliarArticlesAheadOfSerendipity() {
        UserProfileDocument profile = new UserProfileDocument();
        profile.setUserId(3L);
        profile.setSimilarityModelKey("fixed-weight");
        profile.setReadArticleIds(List.of("read-1"));
        profile.setLikedArticleIds(List.of("liked-1"));
        profile.setSharedArticleIds(List.of("shared-1"));
        profile.setTopicWeights(Map.of("ai", 0.7, "search", 0.3));
        profile.setTagWeights(Map.of("ml", 0.6, "ranking", 0.4));

        when(userProfileService.getProfile(3L)).thenReturn(profile);
        when(userSubscriptionService.subscribedPublisherIdsForUser(3L)).thenReturn(java.util.Set.of());
        when(articleRepository.findAll()).thenReturn(List.of(
                new ArticleDocument("read-1", "Seen article", "...", List.of("ai"), List.of("ml"), Instant.now()),
                new ArticleDocument("liked-1", "Seen article", "...", List.of("ai"), List.of("ml"), Instant.now()),
                new ArticleDocument("shared-1", "Seen article", "...", List.of("ai"), List.of("ml"), Instant.now()),
                new ArticleDocument("candidate-1", "AI ranking systems", "...", List.of("ai", "search"), List.of("ranking", "ml"), Instant.now()),
                new ArticleDocument("candidate-2", "Gardening tips", "...", List.of("gardening"), List.of("plants"), Instant.now())
        ));

        List<RecommendationResponse> recommendations = recommendationService.recommendForUser(3L, 5);

        assertThat(recommendations).hasSize(2);
        assertThat(recommendations.get(0).articleId()).isEqualTo("candidate-1");
        assertThat(recommendations.get(0).matchedTopics()).containsExactly("ai", "search");
        assertThat(recommendations.get(0).matchedTags()).containsExactly("ml", "ranking");
        assertThat(recommendations.get(0).score()).isGreaterThan(0.0);
        assertThat(recommendations.get(1).articleId()).isEqualTo("candidate-2");
        assertThat(recommendations.get(1).matchedTopics()).isEmpty();
        assertThat(recommendations.get(1).matchedTags()).isEmpty();
        assertThat(recommendations.get(1).score()).isEqualTo(0.0);
    }

    @Test
    void recommendForUserDoesNotInjectSerendipityWhenLimitIsOne() {
        UserProfileDocument profile = new UserProfileDocument();
        profile.setUserId(3L);
        profile.setSimilarityModelKey("fixed-weight");
        profile.setLikedArticleIds(List.of("liked-1"));
        profile.setTopicWeights(Map.of("ai", 1.0));
        profile.setTagWeights(Map.of("ml", 1.0));

        when(userProfileService.getProfile(3L)).thenReturn(profile);
        when(userSubscriptionService.subscribedPublisherIdsForUser(3L)).thenReturn(java.util.Set.of());
        when(articleRepository.findAll()).thenReturn(List.of(
                new ArticleDocument("liked-1", "Seen article", "...", List.of("ai"), List.of("ml"), Instant.now()),
                new ArticleDocument("candidate-1", "AI ranking systems", "...", List.of("ai"), List.of("ml"), Instant.now()),
                new ArticleDocument("candidate-2", "Gardening tips", "...", List.of("gardening"), List.of("plants"), Instant.now())
        ));

        List<RecommendationResponse> recommendations = recommendationService.recommendForUser(3L, 1);

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).articleId()).isEqualTo("candidate-1");
    }

    @Test
    void recommendForUserSkipsSubscriptionOnlyArticlesWhenUserIsNotSubscribed() {
        UserProfileDocument profile = new UserProfileDocument();
        profile.setUserId(5L);
        profile.setSimilarityModelKey("fixed-weight");
        profile.setTopicWeights(Map.of("fashion", 1.0));
        profile.setTagWeights(Map.of("luxury", 1.0));

        ArticleDocument publicArticle = new ArticleDocument(
                "public-1",
                "Public fashion brief",
                "...",
                List.of("fashion"),
                List.of("luxury"),
                Instant.now()
        );
        ArticleDocument premiumArticle = new ArticleDocument(
                "premium-1",
                "Premium fashion brief",
                "...",
                List.of("fashion"),
                List.of("luxury"),
                11L,
                "Leonie Weber",
                7L,
                "Cedar Review",
                com.example.recommendation.article.model.ArticleAccessLevel.SUBSCRIBERS_ONLY,
                Instant.now()
        );

        when(userProfileService.getProfile(5L)).thenReturn(profile);
        when(articleRepository.findAll()).thenReturn(List.of(publicArticle, premiumArticle));
        when(userSubscriptionService.subscribedPublisherIdsForUser(5L)).thenReturn(java.util.Set.of());

        List<RecommendationResponse> recommendations = recommendationService.recommendForUser(5L, 5);

        assertThat(recommendations).extracting(RecommendationResponse::articleId).containsExactly("public-1");
    }
}
