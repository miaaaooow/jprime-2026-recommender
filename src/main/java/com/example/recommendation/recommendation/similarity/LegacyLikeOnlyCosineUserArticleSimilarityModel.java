package com.example.recommendation.recommendation.similarity;

import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.config.RecommendationProperties;
import com.example.recommendation.profile.model.UserProfileDocument;
import com.example.recommendation.user.model.UserArticleInteractionType;
import org.springframework.stereotype.Component;

@Component
public class LegacyLikeOnlyCosineUserArticleSimilarityModel extends AbstractWeightedUserArticleSimilarityModel {

    public LegacyLikeOnlyCosineUserArticleSimilarityModel(RecommendationProperties recommendationProperties) {
        super(recommendationProperties);
    }

    @Override
    public String key() {
        return "legacy-like-cosine";
    }

    @Override
    protected boolean includeInteraction(UserArticleInteraction interaction) {
        return interaction.type() == UserArticleInteractionType.LIKE;
    }

    @Override
    protected double interactionWeight(UserArticleInteraction interaction) {
        return 1.0;
    }

    @Override
    public ArticleSimilarity score(UserProfileDocument profile, ArticleDocument article) {
        double topicScore = cosineSimilarity(profile.getTopicWeights(), article.getTopics());
        double tagScore = cosineSimilarity(profile.getTagWeights(), article.getTags());
        return articleSimilarityFromScores(profile, article, topicScore, tagScore);
    }
}
