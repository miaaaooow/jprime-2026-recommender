package com.example.recommendation.api.controller;

import com.example.recommendation.api.dto.ArticleTaggingRequest;
import com.example.recommendation.api.dto.ArticleTaggingResponse;
import com.example.recommendation.article.service.ArticleTaggingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/article-tagging")
public class InternalArticleTaggingController {

    private final ArticleTaggingService articleTaggingService;

    public InternalArticleTaggingController(ArticleTaggingService articleTaggingService) {
        this.articleTaggingService = articleTaggingService;
    }

    @PostMapping
    public ArticleTaggingResponse tagArticle(@Valid @RequestBody ArticleTaggingRequest request) {
        return ArticleTaggingResponse.from(articleTaggingService.extractFeatures(
                request.title(),
                request.content(),
                request.topics(),
                request.tags()
        ));
    }
}
