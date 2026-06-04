package com.example.recommendation.recommendation.similarity;

import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.config.RecommendationProperties;
import com.example.recommendation.profile.model.UserProfileDocument;
import org.springframework.stereotype.Component;

@Component
public class FixedWeightUserArticleSimilarityModel extends AbstractWeightedUserArticleSimilarityModel {

    public FixedWeightUserArticleSimilarityModel(RecommendationProperties recommendationProperties) {
        super(recommendationProperties);
    }

    @Override
    public String key() {
        return "fixed-weight";
    }

    @Override
    public ArticleSimilarity score(UserProfileDocument profile, ArticleDocument article) {
        double topicScore = weightedOverlap(profile.getTopicWeights(), article.getTopics());
        double tagScore = weightedOverlap(profile.getTagWeights(), article.getTags());
        return articleSimilarityFromScores(profile, article, topicScore, tagScore);
    }
}
