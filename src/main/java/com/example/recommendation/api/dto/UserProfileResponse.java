package com.example.recommendation.api.dto;

import com.example.recommendation.profile.model.UserProfileDocument;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record UserProfileResponse(
        Long userId,
        String similarityModel,
        int readArticleCount,
        List<String> readArticleIds,
        int likedArticleCount,
        List<String> likedArticleIds,
        int sharedArticleCount,
        List<String> sharedArticleIds,
        Map<String, Double> topicWeights,
        Map<String, Double> tagWeights,
        Instant updatedAt
) {
    public static UserProfileResponse from(UserProfileDocument profile) {
        return new UserProfileResponse(
                profile.getUserId(),
                profile.getSimilarityModelKey(),
                profile.getReadArticleCount(),
                List.copyOf(profile.getReadArticleIds()),
                profile.getLikedArticleCount(),
                List.copyOf(profile.getLikedArticleIds()),
                profile.getSharedArticleCount(),
                List.copyOf(profile.getSharedArticleIds()),
                Map.copyOf(profile.getTopicWeights()),
                Map.copyOf(profile.getTagWeights()),
                profile.getUpdatedAt()
        );
    }
}
