package com.example.recommendation.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePublisherRequest(
        @NotBlank(message = "must not be blank") String name
) {
}
