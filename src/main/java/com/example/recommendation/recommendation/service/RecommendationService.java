package com.example.recommendation.recommendation.service;

import com.example.recommendation.api.dto.RecommendationResponse;
import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.article.repository.ArticleRepository;
import com.example.recommendation.profile.model.UserProfileDocument;
import com.example.recommendation.profile.service.UserProfileService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    private static final double TOPIC_WEIGHT = 0.7;
    private static final double TAG_WEIGHT = 0.3;

    private final ArticleRepository articleRepository;
    private final UserProfileService userProfileService;

    public RecommendationService(ArticleRepository articleRepository, UserProfileService userProfileService) {
        this.articleRepository = articleRepository;
        this.userProfileService = userProfileService;
    }

    public List<RecommendationResponse> recommendForUser(Long userId, int limit) {
        UserProfileDocument profile = userProfileService.getProfile(userId);
        Set<String> likedArticleIds = Set.copyOf(profile.getLikedArticleIds());

        if (profile.getTopicWeights().isEmpty() && profile.getTagWeights().isEmpty()) {
            return List.of();
        }

        return StreamSupport.stream(articleRepository.findAll().spliterator(), false)
                .filter(article -> !likedArticleIds.contains(article.getId()))
                .map(article -> scoreArticle(profile, article))
                .filter(result -> result.score() > 0.0)
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .limit(limit)
                .map(this::toResponse)
                .toList();
    }

    private ScoredArticle scoreArticle(UserProfileDocument profile, ArticleDocument article) {
        List<String> matchedTopics = article.getTopics().stream()
                .filter(topic -> profile.getTopicWeights().containsKey(topic))
                .sorted()
                .toList();
        List<String> matchedTags = article.getTags().stream()
                .filter(tag -> profile.getTagWeights().containsKey(tag))
                .sorted()
                .toList();

        double topicScore = cosineSimilarity(profile.getTopicWeights(), article.getTopics());
        double tagScore = cosineSimilarity(profile.getTagWeights(), article.getTags());
        double combinedScore = (topicScore * TOPIC_WEIGHT) + (tagScore * TAG_WEIGHT);

        return new ScoredArticle(article, combinedScore, matchedTopics, matchedTags);
    }

    private double cosineSimilarity(Map<String, Double> profileWeights, List<String> articleLabels) {
        if (profileWeights.isEmpty() || articleLabels.isEmpty()) {
            return 0.0;
        }

        Set<String> uniqueArticleLabels = new LinkedHashSet<>(articleLabels);
        double numerator = uniqueArticleLabels.stream()
                .mapToDouble(label -> profileWeights.getOrDefault(label, 0.0))
                .sum();

        if (numerator == 0.0) {
            return 0.0;
        }

        double profileMagnitude = Math.sqrt(profileWeights.values().stream()
                .mapToDouble(weight -> weight * weight)
                .sum());
        double articleMagnitude = Math.sqrt(uniqueArticleLabels.size());

        return numerator / (profileMagnitude * articleMagnitude);
    }

    private RecommendationResponse toResponse(ScoredArticle scoredArticle) {
        return new RecommendationResponse(
                scoredArticle.article().getId(),
                scoredArticle.article().getTitle(),
                scoredArticle.score(),
                scoredArticle.matchedTopics(),
                scoredArticle.matchedTags(),
                List.copyOf(scoredArticle.article().getTopics()),
                List.copyOf(scoredArticle.article().getTags())
        );
    }

    private record ScoredArticle(
            ArticleDocument article,
            double score,
            List<String> matchedTopics,
            List<String> matchedTags
    ) {
    }
}
