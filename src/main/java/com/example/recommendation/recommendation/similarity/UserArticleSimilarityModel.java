package com.example.recommendation.recommendation.similarity;

import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.profile.model.UserProfileDocument;
import java.util.List;

public interface UserArticleSimilarityModel {

    String key();

    UserProfileDocument buildProfile(Long userId, List<UserArticleInteraction> interactions);

    ArticleSimilarity score(UserProfileDocument profile, ArticleDocument article);
}
