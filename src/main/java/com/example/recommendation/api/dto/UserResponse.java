package com.example.recommendation.api.dto;

import com.example.recommendation.user.model.UserEntity;
import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String email,
        Instant createdAt
) {
    public static UserResponse from(UserEntity user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());
    }
}
