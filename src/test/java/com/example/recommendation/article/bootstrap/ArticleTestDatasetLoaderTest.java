package com.example.recommendation.article.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.article.repository.ArticleRepository;
import com.example.recommendation.author.model.AuthorEntity;
import com.example.recommendation.author.repository.AuthorRepository;
import com.example.recommendation.profile.service.UserProfileService;
import com.example.recommendation.publisher.model.PublisherEntity;
import com.example.recommendation.publisher.repository.PublisherRepository;
import com.example.recommendation.subscription.model.UserPublisherSubscriptionEntity;
import com.example.recommendation.subscription.repository.UserPublisherSubscriptionRepository;
import com.example.recommendation.user.model.UserArticleInteractionEntity;
import com.example.recommendation.user.model.UserArticleInteractionType;
import com.example.recommendation.user.model.UserEntity;
import com.example.recommendation.user.repository.UserArticleInteractionRepository;
import com.example.recommendation.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.DefaultResourceLoader;

class ArticleTestDatasetLoaderTest {

    @Test
    void runSeedsPublishersAuthorsArticlesUsersAndInteractionsAsOneCatalog() throws Exception {
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        PublisherRepository publisherRepository = mock(PublisherRepository.class);
        AuthorRepository authorRepository = mock(AuthorRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserArticleInteractionRepository interactionRepository = mock(UserArticleInteractionRepository.class);
        UserPublisherSubscriptionRepository subscriptionRepository = mock(UserPublisherSubscriptionRepository.class);
        UserProfileService userProfileService = mock(UserProfileService.class);
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

        Map<String, PublisherEntity> publishersByKey = new LinkedHashMap<>();
        Map<String, AuthorEntity> authorsByKey = new LinkedHashMap<>();
        Map<String, UserEntity> usersByKey = new LinkedHashMap<>();
        AtomicLong publisherIds = new AtomicLong(1);
        AtomicLong authorIds = new AtomicLong(1);
        AtomicLong userIds = new AtomicLong(1);
        List<ArticleDocument> savedArticles = new ArrayList<>();
        List<UserArticleInteractionEntity> savedInteractions = new ArrayList<>();
        List<UserPublisherSubscriptionEntity> savedSubscriptions = new ArrayList<>();
        Set<String> interactionKeys = new HashSet<>();
        Set<String> subscriptionKeys = new HashSet<>();

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
        when(userRepository.findByUsernameIgnoreCase(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(usersByKey.get(normalizeKey(invocation.getArgument(0)))));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            if (user.getId() == null) {
                setId(user, userIds.getAndIncrement());
            }
            usersByKey.put(normalizeKey(user.getUsername()), user);
            return user;
        });
        when(interactionRepository.existsByUserIdAndArticleIdAndInteractionType(anyLong(), anyString(), any()))
                .thenAnswer(invocation -> interactionKeys.contains(interactionKey(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2)
                )));
        when(interactionRepository.save(any(UserArticleInteractionEntity.class))).thenAnswer(invocation -> {
            UserArticleInteractionEntity interaction = invocation.getArgument(0);
            savedInteractions.add(interaction);
            interactionKeys.add(interactionKey(
                    interaction.getUserId(),
                    interaction.getArticleId(),
                    interaction.getInteractionType()
            ));
            return interaction;
        });
        when(subscriptionRepository.existsByUserIdAndPublisherId(anyLong(), anyLong()))
                .thenAnswer(invocation -> subscriptionKeys.contains(subscriptionKey(
                        invocation.getArgument(0),
                        invocation.getArgument(1)
                )));
        when(subscriptionRepository.save(any(UserPublisherSubscriptionEntity.class))).thenAnswer(invocation -> {
            UserPublisherSubscriptionEntity subscription = invocation.getArgument(0);
            savedSubscriptions.add(subscription);
            subscriptionKeys.add(subscriptionKey(subscription.getUserId(), subscription.getPublisherId()));
            return subscription;
        });

        ArticleTestDatasetLoader loader = new ArticleTestDatasetLoader(
                articleRepository,
                publisherRepository,
                authorRepository,
                userRepository,
                interactionRepository,
                subscriptionRepository,
                userProfileService,
                new ObjectMapper().findAndRegisterModules(),
                resourceLoader,
                "classpath:testdata/articles.json",
                "classpath:testdata/publishers.json",
                "classpath:testdata/authors.json",
                "classpath:testdata/users.json",
                "classpath:testdata/interactions.json",
                "classpath:testdata/subscriptions.json"
        );

        loader.run(new DefaultApplicationArguments(new String[0]));

        assertThat(publishersByKey).hasSize(4);
        assertThat(authorsByKey).hasSize(8);
        assertThat(usersByKey).hasSize(4);
        assertThat(savedArticles).hasSize(11);
        assertThat(savedInteractions).hasSize(12);
        assertThat(savedSubscriptions).hasSize(7);

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
        assertThat(lisbon.getAccessLevel()).isEqualTo(com.example.recommendation.article.model.ArticleAccessLevel.PUBLIC);

        ArticleDocument cedarPremium = savedArticles.stream()
                .filter(article -> article.getId().equals("fashion-repair-economy"))
                .findFirst()
                .orElseThrow();
        assertThat(cedarPremium.getAccessLevel()).isEqualTo(
                com.example.recommendation.article.model.ArticleAccessLevel.SUBSCRIBERS_ONLY
        );

        UserEntity mila = usersByKey.get(normalizeKey("mila.travel"));
        assertThat(savedInteractions).anySatisfy(interaction -> {
            assertThat(interaction.getUserId()).isEqualTo(mila.getId());
            assertThat(interaction.getArticleId()).isEqualTo("tourism-lisbon-slow-weekend");
            assertThat(interaction.getInteractionType()).isEqualTo(UserArticleInteractionType.LIKE);
        });
        assertThat(savedSubscriptions).anySatisfy(subscription -> {
            assertThat(subscription.getUserId()).isEqualTo(mila.getId());
            assertThat(subscription.getPublisherId()).isEqualTo(
                    publishersByKey.get(normalizeKey("North Star Dispatch")).getId()
            );
        });

        ArgumentCaptor<Long> rebuiltUserIds = ArgumentCaptor.forClass(Long.class);
        verify(userProfileService, times(4)).rebuildProfile(rebuiltUserIds.capture());
        assertThat(rebuiltUserIds.getAllValues()).containsExactlyInAnyOrder(
                usersByKey.values().stream().map(UserEntity::getId).toArray(Long[]::new)
        );
    }

    private static String normalizeKey(String value) {
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String authorKey(Long publisherId, String authorName) {
        return publisherId + ":" + normalizeKey(authorName);
    }

    private static String interactionKey(Long userId, String articleId, UserArticleInteractionType interactionType) {
        return userId + ":" + articleId + ":" + interactionType.name();
    }

    private static String subscriptionKey(Long userId, Long publisherId) {
        return userId + ":" + publisherId;
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
