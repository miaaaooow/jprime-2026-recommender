package com.example.recommendation.recommendation.service;

import com.example.recommendation.api.dto.RecommendationResponse;
import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.article.repository.ArticleRepository;
import com.example.recommendation.config.RecommendationProperties;
import com.example.recommendation.profile.model.UserProfileDocument;
import com.example.recommendation.profile.service.UserProfileService;
import com.example.recommendation.recommendation.similarity.ArticleSimilarity;
import com.example.recommendation.recommendation.similarity.UserArticleSimilarityModel;
import com.example.recommendation.recommendation.similarity.UserArticleSimilarityModelRegistry;
import com.example.recommendation.subscription.service.UserSubscriptionService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    private final ArticleRepository articleRepository;
    private final UserProfileService userProfileService;
    private final UserArticleSimilarityModelRegistry similarityModelRegistry;
    private final RecommendationProperties recommendationProperties;
    private final UserSubscriptionService userSubscriptionService;

    public RecommendationService(
            ArticleRepository articleRepository,
            UserProfileService userProfileService,
            UserArticleSimilarityModelRegistry similarityModelRegistry,
            RecommendationProperties recommendationProperties,
            UserSubscriptionService userSubscriptionService
    ) {
        this.articleRepository = articleRepository;
        this.userProfileService = userProfileService;
        this.similarityModelRegistry = similarityModelRegistry;
        this.recommendationProperties = recommendationProperties;
        this.userSubscriptionService = userSubscriptionService;
    }

    public List<RecommendationResponse> recommendForUser(Long userId, int limit) {
        UserProfileDocument profile = userProfileService.getProfile(userId);
        UserArticleSimilarityModel similarityModel = similarityModelRegistry.getActiveModel();
        Set<String> interactedArticleIds = Set.copyOf(loadInteractedArticleIds(profile));
        Set<Long> subscribedPublisherIds = userSubscriptionService.subscribedPublisherIdsForUser(userId);

        if (profile.getTopicWeights().isEmpty() && profile.getTagWeights().isEmpty()) {
            return List.of();
        }

        List<ScoredArticle> scoredArticles = StreamSupport.stream(articleRepository.findAll().spliterator(), false)
                .filter(article -> !interactedArticleIds.contains(article.getId()))
                .filter(article -> canAccessArticle(article, subscribedPublisherIds))
                .map(article -> scoreArticle(similarityModel, profile, article))
                .toList();

        List<ScoredArticle> familiarArticles = scoredArticles.stream()
                .filter(article -> article.similarity().score() > 0.0)
                .sorted(Comparator
                        .comparing((ScoredArticle article) -> article.similarity().score())
                        .reversed()
                        .thenComparing(article -> article.article().getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<ScoredArticle> serendipityArticles = scoredArticles.stream()
                .filter(article -> introducesNovelLabels(profile, article))
                .sorted(Comparator
                        .comparing((ScoredArticle article) -> isPureSerendipity(article))
                        .reversed()
                        .thenComparing(article -> unseenLabelCount(profile, article), Comparator.reverseOrder())
                        .thenComparing(article -> article.article().getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(article -> article.similarity().score(), Comparator.reverseOrder()))
                .toList();

        return mergeRecommendations(familiarArticles, serendipityArticles, limit).stream()
                .map(this::toResponse)
                .toList();
    }

    private Set<String> loadInteractedArticleIds(UserProfileDocument profile) {
        LinkedHashSet<String> interactedArticleIds = new LinkedHashSet<>();
        interactedArticleIds.addAll(profile.getReadArticleIds());
        interactedArticleIds.addAll(profile.getLikedArticleIds());
        interactedArticleIds.addAll(profile.getSharedArticleIds());
        return interactedArticleIds;
    }

    private boolean canAccessArticle(ArticleDocument article, Set<Long> subscribedPublisherIds) {
        if (article.getAccessLevel() != com.example.recommendation.article.model.ArticleAccessLevel.SUBSCRIBERS_ONLY) {
            return true;
        }

        Long publisherId = article.getPublisherId();
        return publisherId != null && subscribedPublisherIds.contains(publisherId);
    }

    private List<ScoredArticle> mergeRecommendations(
            List<ScoredArticle> familiarArticles,
            List<ScoredArticle> serendipityArticles,
            int limit
    ) {
        if (limit <= 0) {
            return List.of();
        }

        LinkedHashMap<String, ScoredArticle> merged = new LinkedHashMap<>();
        int serendipitySlots = desiredSerendipitySlots(limit);

        addArticles(merged, familiarArticles, Math.max(0, limit - serendipitySlots));
        addArticles(merged, serendipityArticles, limit);
        addArticles(merged, familiarArticles, limit);
        addArticles(merged, serendipityArticles, limit);

        return new ArrayList<>(merged.values()).stream()
                .limit(limit)
                .toList();
    }

    private void addArticles(
            Map<String, ScoredArticle> target,
            List<ScoredArticle> source,
            int maxSize
    ) {
        for (ScoredArticle article : source) {
            if (target.size() >= maxSize) {
                return;
            }
            target.putIfAbsent(article.article().getId(), article);
        }
    }

    private int desiredSerendipitySlots(int limit) {
        double quota = recommendationProperties.getSerendipity().getQuota();
        if (quota <= 0.0 || limit <= 1) {
            return 0;
        }

        return Math.min(limit - 1, Math.max(1, (int) Math.ceil(limit * quota)));
    }

    private boolean introducesNovelLabels(UserProfileDocument profile, ScoredArticle article) {
        return hasNovelLabels(profile.getTopicWeights(), article.article().getTopics())
                || hasNovelLabels(profile.getTagWeights(), article.article().getTags());
    }

    private boolean hasNovelLabels(Map<String, Double> knownWeights, List<String> articleLabels) {
        return articleLabels.stream().anyMatch(label -> !knownWeights.containsKey(label));
    }

    private boolean isPureSerendipity(ScoredArticle article) {
        return article.similarity().matchedTopics().isEmpty() && article.similarity().matchedTags().isEmpty();
    }

    private long unseenLabelCount(UserProfileDocument profile, ScoredArticle article) {
        return unseenLabels(profile.getTopicWeights(), article.article().getTopics()).size()
                + unseenLabels(profile.getTagWeights(), article.article().getTags()).size();
    }

    private Set<String> unseenLabels(Map<String, Double> knownWeights, List<String> articleLabels) {
        return articleLabels.stream()
                .filter(label -> !knownWeights.containsKey(label))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
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
                scoredArticle.article().getAccessLevel(),
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
