package com.example.recommendation.recommendation.service;

import com.example.recommendation.api.dto.RecommendationResponse;
import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.article.repository.ArticleRepository;
import com.example.recommendation.profile.model.UserProfileDocument;
import com.example.recommendation.profile.service.UserProfileService;
import com.example.recommendation.recommendation.similarity.ArticleSimilarity;
import com.example.recommendation.recommendation.similarity.UserArticleSimilarityModel;
import com.example.recommendation.recommendation.similarity.UserArticleSimilarityModelRegistry;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    private final ArticleRepository articleRepository;
    private final UserProfileService userProfileService;
    private final UserArticleSimilarityModelRegistry similarityModelRegistry;

    public RecommendationService(
            ArticleRepository articleRepository,
            UserProfileService userProfileService,
            UserArticleSimilarityModelRegistry similarityModelRegistry
    ) {
        this.articleRepository = articleRepository;
        this.userProfileService = userProfileService;
        this.similarityModelRegistry = similarityModelRegistry;
    }

    public List<RecommendationResponse> recommendForUser(Long userId, int limit) {
        UserProfileDocument profile = userProfileService.getProfile(userId);
        UserArticleSimilarityModel similarityModel = similarityModelRegistry.getActiveModel();
        Set<String> interactedArticleIds = Set.copyOf(loadInteractedArticleIds(profile));

        if (profile.getTopicWeights().isEmpty() && profile.getTagWeights().isEmpty()) {
            return List.of();
        }

        return StreamSupport.stream(articleRepository.findAll().spliterator(), false)
                .filter(article -> !interactedArticleIds.contains(article.getId()))
                .map(article -> scoreArticle(similarityModel, profile, article))
                .filter(result -> result.similarity().score() > 0.0)
                .sorted((left, right) -> Double.compare(right.similarity().score(), left.similarity().score()))
                .limit(limit)
                .map(this::toResponse)
                .toList();
    }

    private Set<String> loadInteractedArticleIds(UserProfileDocument profile) {
        java.util.LinkedHashSet<String> interactedArticleIds = new java.util.LinkedHashSet<>();
        interactedArticleIds.addAll(profile.getReadArticleIds());
        interactedArticleIds.addAll(profile.getLikedArticleIds());
        interactedArticleIds.addAll(profile.getSharedArticleIds());
        return interactedArticleIds;
    }

    private ScoredArticle scoreArticle(
            UserArticleSimilarityModel similarityModel,
            UserProfileDocument profile,
            ArticleDocument article
    ) {
        return new ScoredArticle(article, similarityModel.score(profile, article));
    }

    private RecommendationResponse toResponse(ScoredArticle scoredArticle) {
        return new RecommendationResponse(
                scoredArticle.article().getId(),
                scoredArticle.article().getTitle(),
                scoredArticle.similarity().score(),
                scoredArticle.similarity().matchedTopics(),
                scoredArticle.similarity().matchedTags(),
                List.copyOf(scoredArticle.article().getTopics()),
                List.copyOf(scoredArticle.article().getTags())
        );
    }

    private record ScoredArticle(
            ArticleDocument article,
            ArticleSimilarity similarity
    ) {
    }
}
