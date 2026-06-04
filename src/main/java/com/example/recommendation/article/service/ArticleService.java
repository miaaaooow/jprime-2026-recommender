package com.example.recommendation.article.service;

import com.example.recommendation.api.dto.CreateArticleRequest;
import com.example.recommendation.article.extractor.ArticleFeatureExtractor;
import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.article.repository.ArticleRepository;
import com.example.recommendation.common.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Service;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleFeatureExtractor articleFeatureExtractor;

    public ArticleService(ArticleRepository articleRepository, ArticleFeatureExtractor articleFeatureExtractor) {
        this.articleRepository = articleRepository;
        this.articleFeatureExtractor = articleFeatureExtractor;
    }

    public ArticleDocument createArticle(CreateArticleRequest request) {
        ArticleFeatureExtractor.ArticleFeatures features = articleFeatureExtractor.extract(
                request.content(),
                request.topics(),
                request.tags()
        );

        ArticleDocument article = new ArticleDocument(
                UUID.randomUUID().toString(),
                request.title().trim(),
                request.content().trim(),
                features.topics(),
                features.tags(),
                Instant.now()
        );

        return articleRepository.save(article);
    }

    public ArticleDocument getArticle(String articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article %s was not found.".formatted(articleId)));
    }

    public List<ArticleDocument> getAllArticles() {
        return StreamSupport.stream(articleRepository.findAll().spliterator(), false).toList();
    }
}
