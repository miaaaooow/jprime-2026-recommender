package com.example.recommendation.article.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.article.repository.ArticleRepository;
import com.example.recommendation.author.model.AuthorEntity;
import com.example.recommendation.author.repository.AuthorRepository;
import com.example.recommendation.publisher.model.PublisherEntity;
import com.example.recommendation.publisher.repository.PublisherRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.DefaultResourceLoader;

class ArticleTestDatasetLoaderTest {

    @Test
    void runSeedsPublishersAuthorsAndArticlesAsOneCatalog() throws Exception {
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        PublisherRepository publisherRepository = mock(PublisherRepository.class);
        AuthorRepository authorRepository = mock(AuthorRepository.class);
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

        Map<String, PublisherEntity> publishersByKey = new LinkedHashMap<>();
        Map<String, AuthorEntity> authorsByKey = new LinkedHashMap<>();
        AtomicLong publisherIds = new AtomicLong(1);
        AtomicLong authorIds = new AtomicLong(1);
        List<ArticleDocument> savedArticles = new ArrayList<>();

        when(publisherRepository.findByNameIgnoreCase(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(publishersByKey.get(normalizeKey(invocation.getArgument(0)))));
        when(publisherRepository.save(any(PublisherEntity.class))).thenAnswer(invocation -> {
            PublisherEntity publisher = invocation.getArgument(0);
            setId(publisher, publisherIds.getAndIncrement());
            publishersByKey.put(normalizeKey(publisher.getName()), publisher);
            return publisher;
        });

        when(authorRepository.findByPublisherIdAndNameIgnoreCase(anyLong(), anyString())).thenAnswer(invocation ->
                Optional.ofNullable(authorsByKey.get(authorKey(invocation.getArgument(0), invocation.getArgument(1)))));
        when(authorRepository.save(any(AuthorEntity.class))).thenAnswer(invocation -> {
            AuthorEntity author = invocation.getArgument(0);
            if (author.getId() == null) {
                setId(author, authorIds.getAndIncrement());
            }
            authorsByKey.put(authorKey(author.getPublisherId(), author.getName()), author);
            return author;
        });

        when(articleRepository.saveAll(any())).thenAnswer(invocation -> {
            savedArticles.clear();
            Iterable<ArticleDocument> articles = invocation.getArgument(0);
            articles.forEach(savedArticles::add);
            return articles;
        });

        ArticleTestDatasetLoader loader = new ArticleTestDatasetLoader(
                articleRepository,
                publisherRepository,
                authorRepository,
                new ObjectMapper().findAndRegisterModules(),
                resourceLoader,
                "classpath:testdata/articles.json",
                "classpath:testdata/publishers.json",
                "classpath:testdata/authors.json"
        );

        loader.run(new DefaultApplicationArguments(new String[0]));

        assertThat(publishersByKey).hasSize(4);
        assertThat(authorsByKey).hasSize(8);
        assertThat(savedArticles).hasSize(11);

        ArticleDocument lisbon = savedArticles.stream()
                .filter(article -> article.getId().equals("tourism-lisbon-slow-weekend"))
                .findFirst()
                .orElseThrow();

        assertThat(lisbon.getPublisherName()).isEqualTo("North Star Dispatch");
        assertThat(lisbon.getAuthorName()).isEqualTo("Elena Rossi");
        assertThat(lisbon.getPublisherId()).isEqualTo(
                publishersByKey.get(normalizeKey("North Star Dispatch")).getId()
        );
        assertThat(lisbon.getAuthorId()).isEqualTo(
                authorsByKey.get(authorKey(lisbon.getPublisherId(), "Elena Rossi")).getId()
        );
    }

    private static String normalizeKey(String value) {
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String authorKey(Long publisherId, String authorName) {
        return publisherId + ":" + normalizeKey(authorName);
    }

    private static void setId(Object target, Long id) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }
}
