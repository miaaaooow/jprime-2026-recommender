package com.example.recommendation.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.recommendation.api.dto.CreateArticleRequest;
import com.example.recommendation.article.model.ArticleAccessLevel;
import com.example.recommendation.article.extractor.ArticleFeatureExtractor;
import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.article.repository.ArticleRepository;
import com.example.recommendation.author.model.AuthorEntity;
import com.example.recommendation.author.service.AuthorService;
import com.example.recommendation.common.exception.BadRequestException;
import com.example.recommendation.common.exception.ForbiddenException;
import com.example.recommendation.publisher.model.PublisherEntity;
import com.example.recommendation.publisher.service.PublisherService;
import com.example.recommendation.subscription.service.UserSubscriptionService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private AuthorService authorService;

    @Mock
    private PublisherService publisherService;

    @Mock
    private ArticleTaggingService articleTaggingService;

    @Mock
    private UserSubscriptionService userSubscriptionService;

    private ArticleService articleService;

    @BeforeEach
    void setUp() {
        articleService = new ArticleService(
                articleRepository,
                authorService,
                publisherService,
                articleTaggingService,
                userSubscriptionService
        );
    }

    @Test
    void createArticleStoresAuthorPublisherAndAutoTags() {
        CreateArticleRequest request = new CreateArticleRequest(
                "AI policy briefing",
                "The government outlined new artificial intelligence rules.",
                7L,
                3L,
                ArticleAccessLevel.SUBSCRIBERS_ONLY,
                null,
                null
        );
        AuthorEntity author = new AuthorEntity(3L, "Marta Ivanova", 88.0);
        PublisherEntity publisher = new PublisherEntity("Botev Newsroom");
        setId(author, 7L);
        setId(publisher, 3L);

        when(authorService.getAuthorEntity(7L)).thenReturn(author);
        when(publisherService.getPublisherEntity(3L)).thenReturn(publisher);
        when(articleTaggingService.extractFeatures(request.title(), request.content(), request.topics(), request.tags()))
                .thenReturn(new ArticleFeatureExtractor.ArticleFeatures(
                        List.of("politics", "technology"),
                        List.of("ai", "policy")
                ));
        when(articleRepository.save(org.mockito.ArgumentMatchers.any(ArticleDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArticleDocument article = articleService.createArticle(request);

        assertThat(article.getAuthorId()).isEqualTo(7L);
        assertThat(article.getAuthorName()).isEqualTo("Marta Ivanova");
        assertThat(article.getPublisherId()).isEqualTo(3L);
        assertThat(article.getPublisherName()).isEqualTo("Botev Newsroom");
        assertThat(article.getAccessLevel()).isEqualTo(ArticleAccessLevel.SUBSCRIBERS_ONLY);
        assertThat(article.getTopics()).containsExactly("politics", "technology");
        assertThat(article.getTags()).containsExactly("ai", "policy");
    }

    @Test
    void createArticleRejectsAuthorFromDifferentPublisher() {
        CreateArticleRequest request = new CreateArticleRequest(
                "Mismatch",
                "Text",
                7L,
                3L,
                null,
                null,
                null
        );
        AuthorEntity author = new AuthorEntity(9L, "Marta Ivanova", 88.0);
        PublisherEntity publisher = new PublisherEntity("Botev Newsroom");
        setId(author, 7L);
        setId(publisher, 3L);

        when(authorService.getAuthorEntity(7L)).thenReturn(author);
        when(publisherService.getPublisherEntity(3L)).thenReturn(publisher);

        assertThatThrownBy(() -> articleService.createArticle(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Author 7 does not belong to publisher 3.");

        verifyNoInteractions(articleRepository, articleTaggingService);
    }

    @Test
    void getAllArticlesWithoutUserIdReturnsOnlyPublicArticles() {
        ArticleDocument publicArticle = new ArticleDocument(
                "public-1",
                "Public",
                "...",
                List.of("ai"),
                List.of("ml"),
                7L,
                "Marta Ivanova",
                3L,
                "Botev Newsroom",
                ArticleAccessLevel.PUBLIC,
                Instant.now()
        );
        ArticleDocument premiumArticle = new ArticleDocument(
                "premium-1",
                "Premium",
                "...",
                List.of("ai"),
                List.of("ml"),
                7L,
                "Marta Ivanova",
                3L,
                "Botev Newsroom",
                ArticleAccessLevel.SUBSCRIBERS_ONLY,
                Instant.now()
        );

        when(articleRepository.findAll()).thenReturn(List.of(publicArticle, premiumArticle));

        assertThat(articleService.getAllArticles()).extracting(ArticleDocument::getId).containsExactly("public-1");
    }

    @Test
    void getArticleRejectsSubscriptionOnlyArticleForUnsubscribedUser() {
        ArticleDocument premiumArticle = new ArticleDocument(
                "premium-1",
                "Premium",
                "...",
                List.of("ai"),
                List.of("ml"),
                7L,
                "Marta Ivanova",
                3L,
                "Botev Newsroom",
                ArticleAccessLevel.SUBSCRIBERS_ONLY,
                Instant.now()
        );

        when(articleRepository.findById("premium-1")).thenReturn(java.util.Optional.of(premiumArticle));
        when(userSubscriptionService.canAccessArticle(9L, premiumArticle)).thenReturn(false);

        assertThatThrownBy(() -> articleService.getArticle("premium-1", 9L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Article premium-1 requires a publisher subscription.");
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
