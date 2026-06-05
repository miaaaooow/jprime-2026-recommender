package com.example.recommendation.api.dto;

import com.example.recommendation.subscription.model.UserPublisherSubscriptionEntity;
import java.time.Instant;

public record UserSubscriptionResponse(
        Long publisherId,
        String publisherName,
        Instant createdAt
) {
    public static UserSubscriptionResponse from(
            UserPublisherSubscriptionEntity subscription,
            String publisherName
    ) {
        return new UserSubscriptionResponse(
                subscription.getPublisherId(),
                publisherName,
                subscription.getCreatedAt()
        );
    }
}
