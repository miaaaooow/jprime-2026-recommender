package com.example.recommendation.recommendation.similarity;

import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.user.model.UserArticleInteractionType;

public record UserArticleInteraction(
        UserArticleInteractionType type,
        ArticleDocument article
) {
    public String articleId() {
        return article.getId();
    }
}
