package com.example.recommendation.recommendation.similarity;

import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.config.RecommendationProperties;
import com.example.recommendation.profile.model.UserProfileDocument;
import org.springframework.stereotype.Component;

@Component
public class WeightedCosineUserArticleSimilarityModel extends AbstractWeightedUserArticleSimilarityModel {

    public WeightedCosineUserArticleSimilarityModel(RecommendationProperties recommendationProperties) {
        super(recommendationProperties);
    }

    @Override
    public String key() {
        return "weighted-cosine";
    }

    @Override
    public ArticleSimilarity score(UserProfileDocument profile, ArticleDocument article) {
        double topicScore = cosineSimilarity(profile.getTopicWeights(), article.getTopics());
        double tagScore = cosineSimilarity(profile.getTagWeights(), article.getTags());
        return articleSimilarityFromScores(profile, article, topicScore, tagScore);
    }
}
