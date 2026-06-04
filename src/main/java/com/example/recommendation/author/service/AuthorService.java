package com.example.recommendation.author.service;

import com.example.recommendation.api.dto.ArticleCatalogItemResponse;
import com.example.recommendation.api.dto.AuthorResponse;
import com.example.recommendation.api.dto.CreateAuthorRequest;
import com.example.recommendation.article.repository.ArticleRepository;
import com.example.recommendation.author.model.AuthorEntity;
import com.example.recommendation.author.repository.AuthorRepository;
import com.example.recommendation.common.exception.ResourceNotFoundException;
import com.example.recommendation.publisher.model.PublisherEntity;
import com.example.recommendation.publisher.repository.PublisherRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final ArticleRepository articleRepository;

    public AuthorService(
            AuthorRepository authorRepository,
            PublisherRepository publisherRepository,
            ArticleRepository articleRepository
    ) {
        this.authorRepository = authorRepository;
        this.publisherRepository = publisherRepository;
        this.articleRepository = articleRepository;
    }

    public AuthorResponse createAuthor(CreateAuthorRequest request) {
        PublisherEntity publisher = getPublisherEntity(request.publisherId());
        AuthorEntity author = authorRepository.save(new AuthorEntity(
                publisher.getId(),
                request.name().trim(),
                request.internalRating()
        ));
        return toResponse(author, publisher);
    }

    public AuthorEntity getAuthorEntity(Long authorId) {
        return authorRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author %d was not found.".formatted(authorId)));
    }

    public AuthorResponse getAuthor(Long authorId) {
        AuthorEntity author = getAuthorEntity(authorId);
        return toResponse(author, getPublisherEntity(author.getPublisherId()));
    }

    public List<AuthorResponse> getAllAuthors() {
        return authorRepository.findAllByOrderByNameAsc().stream()
                .map(author -> toResponse(author, getPublisherEntity(author.getPublisherId())))
                .toList();
    }

    private PublisherEntity getPublisherEntity(Long publisherId) {
        return publisherRepository.findById(publisherId)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher %d was not found.".formatted(publisherId)));
    }

    private AuthorResponse toResponse(AuthorEntity author, PublisherEntity publisher) {
        return new AuthorResponse(
                author.getId(),
                author.getName(),
                author.getInternalRating(),
                publisher.getId(),
                publisher.getName(),
                articleRepository.findByAuthorId(author.getId()).stream()
                        .sorted(Comparator.comparing(
                                com.example.recommendation.article.model.ArticleDocument::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ))
                        .map(ArticleCatalogItemResponse::from)
                        .toList(),
                author.getCreatedAt()
        );
    }
}
