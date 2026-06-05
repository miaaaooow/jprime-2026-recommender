package com.example.recommendation.api.controller;

import com.example.recommendation.api.dto.ArticleResponse;
import com.example.recommendation.api.dto.CreateArticleRequest;
import com.example.recommendation.article.service.ArticleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleResponse createArticle(@Valid @RequestBody CreateArticleRequest request) {
        return ArticleResponse.from(articleService.createArticle(request));
    }

    @GetMapping("/{articleId}")
    public ArticleResponse getArticle(
            @PathVariable String articleId,
            @RequestParam(required = false) Long userId
    ) {
        return ArticleResponse.from(articleService.getArticle(articleId, userId));
    }

    @GetMapping
    public List<ArticleResponse> getArticles(@RequestParam(required = false) Long userId) {
        return articleService.getAllArticles(userId).stream().map(ArticleResponse::from).toList();
    }
}
