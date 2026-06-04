package com.example.recommendation.article.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.article.repository.ArticleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class ArticleTestDatasetLoaderTest {

    private final ArticleTestDatasetLoader loader = new ArticleTestDatasetLoader(
            org.mockito.Mockito.mock(ArticleRepository.class),
            new ObjectMapper().findAndRegisterModules(),
            new DefaultResourceLoader(),
            "classpath:testdata/articles.json"
    );

    @Test
    void loadArticlesParsesSampleDataset() throws IOException {
        List<ArticleDocument> articles = loader.loadArticles(new DefaultResourceLoader()
                .getResource("classpath:testdata/articles.json"));

        assertThat(articles).hasSize(11);
        assertThat(articles)
                .extracting(ArticleDocument::getId)
                .contains("tourism-lisbon-slow-weekend", "current-affairs-grocery-price-stickiness");
        assertThat(articles)
                .extracting(ArticleDocument::getTopics)
                .contains(List.of("tourism", "city-break", "europe-travel"));
        assertThat(articles)
                .extracting(ArticleDocument::getTags)
                .contains(List.of("resale", "repair", "consumer-trends", "luxury"));
    }
}
