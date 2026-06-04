package com.example.recommendation.article.repository;

import com.example.recommendation.article.model.ArticleDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ArticleRepository extends ElasticsearchRepository<ArticleDocument, String> {

    java.util.List<ArticleDocument> findByAuthorId(Long authorId);

    java.util.List<ArticleDocument> findByPublisherId(Long publisherId);
}
