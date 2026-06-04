package com.example.recommendation.article.extractor;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.recommendation.config.ArticleTaggingProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class OpenAiSmartArticleTaggingClientContextTest {

    @Test
    void springCanInstantiateClientBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(ArticleTaggingProperties.class);
            context.registerBean(ObjectMapper.class);
            context.registerBean(OpenAiSmartArticleTaggingClient.class);

            context.refresh();

            assertThat(context.getBean(OpenAiSmartArticleTaggingClient.class)).isNotNull();
        }
    }
}
