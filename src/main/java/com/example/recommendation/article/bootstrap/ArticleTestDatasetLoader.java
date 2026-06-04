package com.example.recommendation.article.bootstrap;

import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.article.repository.ArticleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.test-data.articles", name = "enabled", havingValue = "true")
public class ArticleTestDatasetLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ArticleTestDatasetLoader.class);

    private final ArticleRepository articleRepository;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final String resourceLocation;

    public ArticleTestDatasetLoader(
            ArticleRepository articleRepository,
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            @Value("${app.test-data.articles.resource}") String resourceLocation
    ) {
        this.articleRepository = articleRepository;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.resourceLocation = resourceLocation;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Resource resource = resourceLoader.getResource(resourceLocation);
        List<ArticleDocument> articles = loadArticles(resource);
        articleRepository.saveAll(articles);
        log.info("Loaded {} test dataset articles from {}", articles.size(), resourceLocation);
    }

    List<ArticleDocument> loadArticles(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            List<DatasetArticle> dataset = objectMapper.readValue(inputStream, new TypeReference<List<DatasetArticle>>() {
            });

            return dataset.stream()
                    .map(this::toArticleDocument)
                    .toList();
        }
    }

    private ArticleDocument toArticleDocument(DatasetArticle article) {
        return new ArticleDocument(
                article.id().trim(),
                article.title().trim(),
                article.content().trim(),
                normalize(article.topics()),
                normalize(article.tags()),
                article.createdAt()
        );
    }

    private List<String> normalize(List<String> values) {
        return values == null
                ? List.of()
                : values.stream()
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .toList();
    }

    private record DatasetArticle(
            String id,
            String title,
            String content,
            List<String> topics,
            List<String> tags,
            Instant createdAt
    ) {
    }
}
