package com.example.recommendation.recommendation.similarity;

import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.config.RecommendationProperties;
import com.example.recommendation.profile.model.UserProfileDocument;
import com.example.recommendation.user.model.UserArticleInteractionType;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

abstract class AbstractWeightedUserArticleSimilarityModel implements UserArticleSimilarityModel {

    private final RecommendationProperties recommendationProperties;

    protected AbstractWeightedUserArticleSimilarityModel(RecommendationProperties recommendationProperties) {
        this.recommendationProperties = recommendationProperties;
    }

    @Override
    public UserProfileDocument buildProfile(Long userId, List<UserArticleInteraction> interactions) {
        List<UserArticleInteraction> includedInteractions = interactions.stream()
                .filter(this::includeInteraction)
                .toList();

        UserProfileDocument profile = new UserProfileDocument();
        profile.setUserId(userId);
        profile.setSimilarityModelKey(key());
        profile.setReadArticleIds(articleIdsFor(includedInteractions, UserArticleInteractionType.READ));
        profile.setReadArticleCount(profile.getReadArticleIds().size());
        profile.setLikedArticleIds(articleIdsFor(includedInteractions, UserArticleInteractionType.LIKE));
        profile.setLikedArticleCount(profile.getLikedArticleIds().size());
        profile.setSharedArticleIds(articleIdsFor(includedInteractions, UserArticleInteractionType.SHARE));
        profile.setSharedArticleCount(profile.getSharedArticleIds().size());
        profile.setTopicWeights(normalizeWeights(weightedLabels(includedInteractions, ArticleDocument::getTopics)));
        profile.setTagWeights(normalizeWeights(weightedLabels(includedInteractions, ArticleDocument::getTags)));
        return profile;
    }

    protected boolean includeInteraction(UserArticleInteraction interaction) {
        return true;
    }

    protected double interactionWeight(UserArticleInteraction interaction) {
        return recommendationProperties.getInteractionWeights().weightFor(interaction.type());
    }

    protected double weightedOverlap(Map<String, Double> profileWeights, List<String> articleLabels) {
        if (profileWeights.isEmpty() || articleLabels.isEmpty()) {
            return 0.0;
        }

        return uniqueLabels(articleLabels).stream()
                .mapToDouble(label -> profileWeights.getOrDefault(label, 0.0))
                .sum();
    }

    protected double cosineSimilarity(Map<String, Double> profileWeights, List<String> articleLabels) {
        if (profileWeights.isEmpty() || articleLabels.isEmpty()) {
            return 0.0;
        }

        Set<String> uniqueArticleLabels = uniqueLabels(articleLabels);
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

    protected ArticleSimilarity articleSimilarityFromScores(
            UserProfileDocument profile,
            ArticleDocument article,
            double topicScore,
            double tagScore
    ) {
        return new ArticleSimilarity(
                combineScores(topicScore, tagScore),
                matchedLabels(profile.getTopicWeights(), article.getTopics()),
                matchedLabels(profile.getTagWeights(), article.getTags())
        );
    }

    private double combineScores(double topicScore, double tagScore) {
        return (topicScore * recommendationProperties.getLabelWeights().getTopic())
                + (tagScore * recommendationProperties.getLabelWeights().getTag());
    }

    private List<String> matchedLabels(Map<String, Double> profileWeights, List<String> articleLabels) {
        return uniqueLabels(articleLabels).stream()
                .filter(profileWeights::containsKey)
                .sorted()
                .toList();
    }

    private List<String> articleIdsFor(
            List<UserArticleInteraction> interactions,
            UserArticleInteractionType interactionType
    ) {
        return interactions.stream()
                .filter(interaction -> interaction.type() == interactionType)
                .map(UserArticleInteraction::articleId)
                .distinct()
                .toList();
    }

    private Map<String, Double> weightedLabels(
            List<UserArticleInteraction> interactions,
            Function<ArticleDocument, List<String>> labelsExtractor
    ) {
        Map<String, Double> counts = new LinkedHashMap<>();
        for (UserArticleInteraction interaction : interactions) {
            double weight = interactionWeight(interaction);
            for (String label : labelsExtractor.apply(interaction.article())) {
                counts.merge(label, weight, Double::sum);
            }
        }
        return counts;
    }

    private Map<String, Double> normalizeWeights(Map<String, Double> counts) {
        if (counts.isEmpty()) {
            return Map.of();
        }

        double total = counts.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(java.util.Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue() / total),
                        LinkedHashMap::putAll);
    }

    private Set<String> uniqueLabels(List<String> labels) {
        return new LinkedHashSet<>(labels);
    }
}
