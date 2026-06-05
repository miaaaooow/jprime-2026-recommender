package com.example.recommendation.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.recommendation.article.model.ArticleAccessLevel;
import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.publisher.model.PublisherEntity;
import com.example.recommendation.publisher.repository.PublisherRepository;
import com.example.recommendation.subscription.model.UserPublisherSubscriptionEntity;
import com.example.recommendation.subscription.repository.UserPublisherSubscriptionRepository;
import com.example.recommendation.user.model.UserEntity;
import com.example.recommendation.user.service.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserSubscriptionServiceTest {

    @Mock
    private UserPublisherSubscriptionRepository subscriptionRepository;

    @Mock
    private PublisherRepository publisherRepository;

    @Mock
    private UserService userService;

    private UserSubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        subscriptionService = new UserSubscriptionService(subscriptionRepository, publisherRepository, userService);
    }

    @Test
    void subscribeCreatesSubscriptionOnlyOnce() {
        Long userId = 3L;
        Long publisherId = 5L;
        PublisherEntity publisher = new PublisherEntity("Public Ledger");
        setId(publisher, publisherId);

        when(userService.getUser(userId)).thenReturn(new UserEntity("alice", "alice@example.com"));
        when(publisherRepository.findById(publisherId)).thenReturn(Optional.of(publisher));
        when(subscriptionRepository.findByUserIdAndPublisherId(userId, publisherId)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(org.mockito.ArgumentMatchers.any(UserPublisherSubscriptionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(subscriptionService.subscribe(userId, publisherId).publisherName()).isEqualTo("Public Ledger");

        verify(subscriptionRepository).save(org.mockito.ArgumentMatchers.any(UserPublisherSubscriptionEntity.class));
    }

    @Test
    void listSubscriptionsReturnsPublisherNames() {
        Long userId = 7L;
        PublisherEntity publisher = new PublisherEntity("Cedar Review");
        setId(publisher, 11L);
        UserPublisherSubscriptionEntity subscription = new UserPublisherSubscriptionEntity(userId, 11L);

        when(userService.getUser(userId)).thenReturn(new UserEntity("mila", "mila@example.com"));
        when(subscriptionRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of(subscription));
        when(publisherRepository.findAllById(List.of(11L))).thenReturn(List.of(publisher));

        assertThat(subscriptionService.listSubscriptions(userId))
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.publisherId()).isEqualTo(11L);
                    assertThat(response.publisherName()).isEqualTo("Cedar Review");
                });
    }

    @Test
    void canAccessArticleRequiresSubscriptionForSubscribersOnlyContent() {
        ArticleDocument premiumArticle = new ArticleDocument(
                "premium-1",
                "Premium",
                "...",
                List.of("economy"),
                List.of("analysis"),
                2L,
                "Ana Petrova",
                9L,
                "Public Ledger",
                ArticleAccessLevel.SUBSCRIBERS_ONLY,
                Instant.now()
        );

        when(subscriptionRepository.existsByUserIdAndPublisherId(4L, 9L)).thenReturn(true);

        assertThat(subscriptionService.canAccessArticle(4L, premiumArticle)).isTrue();
        assertThat(subscriptionService.canAccessArticle(8L, premiumArticle)).isFalse();
    }

    @Test
    void unsubscribeDeletesExistingSubscriptionOnly() {
        Long userId = 2L;
        Long publisherId = 6L;
        PublisherEntity publisher = new PublisherEntity("Signal Weekly");
        setId(publisher, publisherId);
        UserPublisherSubscriptionEntity subscription = new UserPublisherSubscriptionEntity(userId, publisherId);

        when(userService.getUser(userId)).thenReturn(new UserEntity("ivo", "ivo@example.com"));
        when(publisherRepository.findById(publisherId)).thenReturn(Optional.of(publisher));
        when(subscriptionRepository.findByUserIdAndPublisherId(userId, publisherId)).thenReturn(Optional.of(subscription));

        subscriptionService.unsubscribe(userId, publisherId);

        verify(subscriptionRepository).delete(subscription);
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
