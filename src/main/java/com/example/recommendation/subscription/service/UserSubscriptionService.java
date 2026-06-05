package com.example.recommendation.subscription.service;

import com.example.recommendation.api.dto.UserSubscriptionResponse;
import com.example.recommendation.article.model.ArticleAccessLevel;
import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.common.exception.ResourceNotFoundException;
import com.example.recommendation.publisher.model.PublisherEntity;
import com.example.recommendation.publisher.repository.PublisherRepository;
import com.example.recommendation.subscription.model.UserPublisherSubscriptionEntity;
import com.example.recommendation.subscription.repository.UserPublisherSubscriptionRepository;
import com.example.recommendation.user.service.UserService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSubscriptionService {

    private final UserPublisherSubscriptionRepository subscriptionRepository;
    private final PublisherRepository publisherRepository;
    private final UserService userService;

    public UserSubscriptionService(
            UserPublisherSubscriptionRepository subscriptionRepository,
            PublisherRepository publisherRepository,
            UserService userService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.publisherRepository = publisherRepository;
        this.userService = userService;
    }

    @Transactional
    public UserSubscriptionResponse subscribe(Long userId, Long publisherId) {
        userService.getUser(userId);
        PublisherEntity publisher = getPublisherEntity(publisherId);

        UserPublisherSubscriptionEntity subscription = subscriptionRepository.findByUserIdAndPublisherId(userId, publisherId)
                .orElseGet(() -> subscriptionRepository.save(new UserPublisherSubscriptionEntity(userId, publisherId)));

        return UserSubscriptionResponse.from(subscription, publisher.getName());
    }

    @Transactional
    public void unsubscribe(Long userId, Long publisherId) {
        userService.getUser(userId);
        getPublisherEntity(publisherId);

        subscriptionRepository.findByUserIdAndPublisherId(userId, publisherId)
                .ifPresent(subscriptionRepository::delete);
    }

    public List<UserSubscriptionResponse> listSubscriptions(Long userId) {
        userService.getUser(userId);
        List<UserPublisherSubscriptionEntity> subscriptions = subscriptionRepository.findByUserIdOrderByCreatedAtAsc(userId);
        Map<Long, String> publisherNamesById = publisherRepository.findAllById(
                        subscriptions.stream()
                                .map(UserPublisherSubscriptionEntity::getPublisherId)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(
                        PublisherEntity::getId,
                        PublisherEntity::getName,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        return subscriptions.stream()
                .map(subscription -> UserSubscriptionResponse.from(
                        subscription,
                        publisherNamesById.getOrDefault(subscription.getPublisherId(), "Unknown publisher")
                ))
                .toList();
    }

    public Set<Long> subscribedPublisherIdsForUser(Long userId) {
        return subscriptionRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(UserPublisherSubscriptionEntity::getPublisherId)
                .collect(Collectors.toSet());
    }

    public boolean canAccessArticle(Long userId, ArticleDocument article) {
        if (article.getAccessLevel() != ArticleAccessLevel.SUBSCRIBERS_ONLY) {
            return true;
        }

        Long publisherId = article.getPublisherId();
        return publisherId != null && subscriptionRepository.existsByUserIdAndPublisherId(userId, publisherId);
    }

    private PublisherEntity getPublisherEntity(Long publisherId) {
        return publisherRepository.findById(publisherId)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher %d was not found.".formatted(publisherId)));
    }
}
