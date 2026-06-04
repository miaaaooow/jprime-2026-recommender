package com.example.recommendation.profile.service;

import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.article.repository.ArticleRepository;
import com.example.recommendation.common.exception.ResourceNotFoundException;
import com.example.recommendation.profile.model.UserProfileDocument;
import com.example.recommendation.profile.repository.UserProfileRepository;
import com.example.recommendation.recommendation.similarity.UserArticleInteraction;
import com.example.recommendation.recommendation.similarity.UserArticleSimilarityModel;
import com.example.recommendation.recommendation.similarity.UserArticleSimilarityModelRegistry;
import com.example.recommendation.user.model.UserArticleInteractionEntity;
import com.example.recommendation.user.model.UserArticleInteractionType;
import com.example.recommendation.user.model.UserArticleLikeEntity;
import com.example.recommendation.user.repository.UserArticleInteractionRepository;
import com.example.recommendation.user.repository.UserArticleLikeRepository;
import com.example.recommendation.user.service.UserService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserService userService;
    private final ArticleRepository articleRepository;
    private final UserArticleLikeRepository userArticleLikeRepository;
    private final UserArticleInteractionRepository userArticleInteractionRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserArticleSimilarityModelRegistry similarityModelRegistry;

    public UserProfileService(
            UserService userService,
            ArticleRepository articleRepository,
            UserArticleLikeRepository userArticleLikeRepository,
            UserArticleInteractionRepository userArticleInteractionRepository,
            UserProfileRepository userProfileRepository,
            UserArticleSimilarityModelRegistry similarityModelRegistry
    ) {
        this.userService = userService;
        this.articleRepository = articleRepository;
        this.userArticleLikeRepository = userArticleLikeRepository;
        this.userArticleInteractionRepository = userArticleInteractionRepository;
        this.userProfileRepository = userProfileRepository;
        this.similarityModelRegistry = similarityModelRegistry;
    }

    public UserProfileDocument getProfile(Long userId) {
        userService.getUser(userId);
        return userProfileRepository.findById(userId)
                .filter(this::isCurrentProfile)
                .orElseGet(() -> rebuildProfile(userId));
    }

    @Transactional
    public UserProfileDocument recordInteraction(Long userId, String articleId, UserArticleInteractionType interactionType) {
        userService.getUser(userId);
        articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article %s was not found.".formatted(articleId)));

        if (!interactionExists(userId, articleId, interactionType)) {
            userArticleInteractionRepository.save(new UserArticleInteractionEntity(userId, articleId, interactionType));
        }

        return rebuildProfile(userId);
    }

    public UserProfileDocument rebuildProfile(Long userId) {
        userService.getUser(userId);

        UserProfileDocument profile = activeModel().buildProfile(userId, loadInteractions(userId));
        profile.setUpdatedAt(Instant.now());
        return userProfileRepository.save(profile);
    }

    private boolean isCurrentProfile(UserProfileDocument profile) {
        return activeModel().key().equals(profile.getSimilarityModelKey());
    }

    private boolean interactionExists(Long userId, String articleId, UserArticleInteractionType interactionType) {
        if (interactionType == UserArticleInteractionType.LIKE
                && userArticleLikeRepository.existsByUserIdAndArticleId(userId, articleId)) {
            return true;
        }

        return userArticleInteractionRepository.existsByUserIdAndArticleIdAndInteractionType(
                userId,
                articleId,
                interactionType
        );
    }

    private List<UserArticleInteraction> loadInteractions(Long userId) {
        List<RawUserArticleInteraction> rawInteractions = new ArrayList<>();

        for (UserArticleLikeEntity like : userArticleLikeRepository.findByUserId(userId)) {
            rawInteractions.add(new RawUserArticleInteraction(like.getArticleId(), UserArticleInteractionType.LIKE));
        }

        for (UserArticleInteractionEntity interaction : userArticleInteractionRepository.findByUserId(userId)) {
            rawInteractions.add(new RawUserArticleInteraction(
                    interaction.getArticleId(),
                    interaction.getInteractionType()
            ));
        }

        Map<String, RawUserArticleInteraction> deduplicatedInteractions = new LinkedHashMap<>();
        for (RawUserArticleInteraction interaction : rawInteractions) {
            deduplicatedInteractions.putIfAbsent(interaction.key(), interaction);
        }

        List<String> articleIds = deduplicatedInteractions.values().stream()
                .map(RawUserArticleInteraction::articleId)
                .distinct()
                .toList();

        if (articleIds.isEmpty()) {
            return List.of();
        }

        Map<String, ArticleDocument> articlesById = StreamSupport.stream(
                        articleRepository.findAllById(articleIds).spliterator(),
                        false
                )
                .collect(LinkedHashMap::new,
                        (map, article) -> map.put(article.getId(), article),
                        LinkedHashMap::putAll);

        List<UserArticleInteraction> resolvedInteractions = new ArrayList<>();
        for (RawUserArticleInteraction interaction : deduplicatedInteractions.values()) {
            ArticleDocument article = articlesById.get(interaction.articleId());
            if (article != null) {
                resolvedInteractions.add(new UserArticleInteraction(interaction.interactionType(), article));
            }
        }

        return resolvedInteractions;
    }

    private UserArticleSimilarityModel activeModel() {
        return similarityModelRegistry.getActiveModel();
    }

    private record RawUserArticleInteraction(
            String articleId,
            UserArticleInteractionType interactionType
    ) {
        private String key() {
            return interactionType + ":" + articleId;
        }
    }
}
