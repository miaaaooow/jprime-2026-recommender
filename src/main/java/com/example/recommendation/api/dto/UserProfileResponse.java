package com.example.recommendation.api.dto;

import com.example.recommendation.profile.model.UserProfileDocument;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record UserProfileResponse(
        Long userId,
        int likedArticleCount,
        List<String> likedArticleIds,
        Map<String, Double> topicWeights,
        Map<String, Double> tagWeights,
        Instant updatedAt
) {
    public static UserProfileResponse from(UserProfileDocument profile) {
        return new UserProfileResponse(
                profile.getUserId(),
                profile.getLikedArticleCount(),
                List.copyOf(profile.getLikedArticleIds()),
                Map.copyOf(profile.getTopicWeights()),
                Map.copyOf(profile.getTagWeights()),
                profile.getUpdatedAt()
        );
    }
}
