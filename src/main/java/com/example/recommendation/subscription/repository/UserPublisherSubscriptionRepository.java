package com.example.recommendation.subscription.repository;

import com.example.recommendation.subscription.model.UserPublisherSubscriptionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPublisherSubscriptionRepository extends JpaRepository<UserPublisherSubscriptionEntity, Long> {

    List<UserPublisherSubscriptionEntity> findByUserIdOrderByCreatedAtAsc(Long userId);

    boolean existsByUserIdAndPublisherId(Long userId, Long publisherId);

    Optional<UserPublisherSubscriptionEntity> findByUserIdAndPublisherId(Long userId, Long publisherId);
}
