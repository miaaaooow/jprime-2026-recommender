package com.example.recommendation.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.article.repository.ArticleRepository;
import com.example.recommendation.config.RecommendationProperties;
import com.example.recommendation.profile.model.UserProfileDocument;
import com.example.recommendation.profile.repository.UserProfileRepository;
import com.example.recommendation.recommendation.similarity.FixedWeightUserArticleSimilarityModel;
import com.example.recommendation.recommendation.similarity.UserArticleSimilarityModelRegistry;
import com.example.recommendation.user.model.UserArticleInteractionEntity;
import com.example.recommendation.user.model.UserArticleInteractionType;
import com.example.recommendation.user.model.UserEntity;
import com.example.recommendation.user.repository.UserArticleInteractionRepository;
import com.example.recommendation.user.repository.UserArticleLikeRepository;
import com.example.recommendation.user.service.UserService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private UserArticleLikeRepository userArticleLikeRepository;

    @Mock
    private UserArticleInteractionRepository userArticleInteractionRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        RecommendationProperties recommendationProperties = new RecommendationProperties();
        UserArticleSimilarityModelRegistry similarityModelRegistry = new UserArticleSimilarityModelRegistry(
                List.of(new FixedWeightUserArticleSimilarityModel(recommendationProperties)),
                recommendationProperties
        );
        userProfileService = new UserProfileService(
                userService,
                articleRepository,
                userArticleLikeRepository,
                userArticleInteractionRepository,
                userProfileRepository,
                similarityModelRegistry
        );
    }

    @Test
    void rebuildProfileAggregatesTopicAndTagWeights() {
        Long userId = 7L;
        when(userService.getUser(userId)).thenReturn(new UserEntity("alice", "alice@example.com"));
        when(userArticleLikeRepository.findByUserId(userId)).thenReturn(List.of(
                like(userId, "a-1"),
                like(userId, "a-2")
        ));
        when(userArticleInteractionRepository.findByUserId(userId)).thenReturn(List.of());
        when(articleRepository.findAllById(List.of("a-1", "a-2"))).thenReturn(List.of(
                new ArticleDocument("a-1", "Article 1", "...", List.of("ai", "search"), List.of("ml", "ranking"), Instant.now()),
                new ArticleDocument("a-2", "Article 2", "...", List.of("ai"), List.of("vector", "ml"), Instant.now())
        ));
        when(userProfileRepository.save(org.mockito.ArgumentMatchers.any(UserProfileDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileDocument profile = userProfileService.rebuildProfile(userId);

        assertThat(profile.getUserId()).isEqualTo(userId);
        assertThat(profile.getSimilarityModelKey()).isEqualTo("fixed-weight");
        assertThat(profile.getLikedArticleIds()).containsExactly("a-1", "a-2");
        assertThat(profile.getTopicWeights()).containsEntry("ai", 2.0 / 3.0);
        assertThat(profile.getTopicWeights()).containsEntry("search", 1.0 / 3.0);
        assertThat(profile.getTagWeights()).containsEntry("ml", 0.5);
        assertThat(profile.getTagWeights()).containsEntry("ranking", 0.25);
        assertThat(profile.getTagWeights()).containsEntry("vector", 0.25);

        ArgumentCaptor<UserProfileDocument> captor = ArgumentCaptor.forClass(UserProfileDocument.class);
        verify(userProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    void rebuildProfileWeightsReadLikeAndShareDifferently() {
        Long userId = 9L;
        when(userService.getUser(userId)).thenReturn(new UserEntity("bob", "bob@example.com"));
        when(userArticleLikeRepository.findByUserId(userId)).thenReturn(List.of());
        when(userArticleInteractionRepository.findByUserId(userId)).thenReturn(List.of(
                interaction(userId, "read-1", UserArticleInteractionType.READ),
                interaction(userId, "like-1", UserArticleInteractionType.LIKE),
                interaction(userId, "share-1", UserArticleInteractionType.SHARE)
        ));
        when(articleRepository.findAllById(List.of("read-1", "like-1", "share-1"))).thenReturn(List.of(
                new ArticleDocument("read-1", "Read", "...", List.of("travel"), List.of("guide"), Instant.now()),
                new ArticleDocument("like-1", "Liked", "...", List.of("travel"), List.of("guide"), Instant.now()),
                new ArticleDocument("share-1", "Shared", "...", List.of("travel"), List.of("trend"), Instant.now())
        ));
        when(userProfileRepository.save(org.mockito.ArgumentMatchers.any(UserProfileDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileDocument profile = userProfileService.rebuildProfile(userId);

        assertThat(profile.getReadArticleIds()).containsExactly("read-1");
        assertThat(profile.getLikedArticleIds()).containsExactly("like-1");
        assertThat(profile.getSharedArticleIds()).containsExactly("share-1");
        assertThat(profile.getTopicWeights()).containsEntry("travel", 1.0);
        assertThat(profile.getTagWeights()).containsEntry("guide", 4.0 / 9.0);
        assertThat(profile.getTagWeights()).containsEntry("trend", 5.0 / 9.0);
    }

    private static com.example.recommendation.user.model.UserArticleLikeEntity like(Long userId, String articleId) {
        return new com.example.recommendation.user.model.UserArticleLikeEntity(userId, articleId);
    }

    private static UserArticleInteractionEntity interaction(
            Long userId,
            String articleId,
            UserArticleInteractionType interactionType
    ) {
        return new UserArticleInteractionEntity(userId, articleId, interactionType);
    }
}
