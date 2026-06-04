package com.example.recommendation.article.service;

import com.example.recommendation.api.dto.CreateArticleRequest;
import com.example.recommendation.article.extractor.ArticleFeatureExtractor;
import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.article.repository.ArticleRepository;
import com.example.recommendation.author.model.AuthorEntity;
import com.example.recommendation.author.service.AuthorService;
import com.example.recommendation.common.exception.BadRequestException;
import com.example.recommendation.common.exception.ResourceNotFoundException;
import com.example.recommendation.publisher.model.PublisherEntity;
import com.example.recommendation.publisher.service.PublisherService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Service;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final AuthorService authorService;
    private final PublisherService publisherService;
    private final ArticleTaggingService articleTaggingService;

    public ArticleService(
            ArticleRepository articleRepository,
            AuthorService authorService,
            PublisherService publisherService,
            ArticleTaggingService articleTaggingService
    ) {
        this.articleRepository = articleRepository;
        this.authorService = authorService;
        this.publisherService = publisherService;
        this.articleTaggingService = articleTaggingService;
    }

    public ArticleDocument createArticle(CreateArticleRequest request) {
        AuthorEntity author = authorService.getAuthorEntity(request.authorId());
        PublisherEntity publisher = publisherService.getPublisherEntity(request.publisherId());

        if (!publisher.getId().equals(author.getPublisherId())) {
            throw new BadRequestException("Author %d does not belong to publisher %d.".formatted(
                    author.getId(),
                    publisher.getId()
            ));
        }

        ArticleFeatureExtractor.ArticleFeatures features = articleTaggingService.extractFeatures(
                request.title(),
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
                author.getId(),
                author.getName(),
                publisher.getId(),
                publisher.getName(),
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
