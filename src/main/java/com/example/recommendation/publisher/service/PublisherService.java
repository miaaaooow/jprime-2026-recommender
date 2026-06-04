package com.example.recommendation.publisher.service;

import com.example.recommendation.api.dto.ArticleCatalogItemResponse;
import com.example.recommendation.api.dto.CreatePublisherRequest;
import com.example.recommendation.api.dto.PublisherAuthorResponse;
import com.example.recommendation.api.dto.PublisherResponse;
import com.example.recommendation.article.repository.ArticleRepository;
import com.example.recommendation.author.repository.AuthorRepository;
import com.example.recommendation.common.exception.ResourceNotFoundException;
import com.example.recommendation.publisher.model.PublisherEntity;
import com.example.recommendation.publisher.repository.PublisherRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PublisherService {

    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;
    private final ArticleRepository articleRepository;

    public PublisherService(
            PublisherRepository publisherRepository,
            AuthorRepository authorRepository,
            ArticleRepository articleRepository
    ) {
        this.publisherRepository = publisherRepository;
        this.authorRepository = authorRepository;
        this.articleRepository = articleRepository;
    }

    public PublisherResponse createPublisher(CreatePublisherRequest request) {
        PublisherEntity publisher = publisherRepository.save(new PublisherEntity(request.name().trim()));
        return toResponse(publisher);
    }

    public PublisherEntity getPublisherEntity(Long publisherId) {
        return publisherRepository.findById(publisherId)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher %d was not found.".formatted(publisherId)));
    }

    public PublisherResponse getPublisher(Long publisherId) {
        return toResponse(getPublisherEntity(publisherId));
    }

    public List<PublisherResponse> getAllPublishers() {
        return publisherRepository.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    private PublisherResponse toResponse(PublisherEntity publisher) {
        return new PublisherResponse(
                publisher.getId(),
                publisher.getName(),
                authorRepository.findByPublisherIdOrderByNameAsc(publisher.getId()).stream()
                        .map(PublisherAuthorResponse::from)
                        .toList(),
                articleRepository.findByPublisherId(publisher.getId()).stream()
                        .sorted(Comparator.comparing(
                                com.example.recommendation.article.model.ArticleDocument::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ))
                        .map(ArticleCatalogItemResponse::from)
                        .toList(),
                publisher.getCreatedAt()
        );
    }
}
