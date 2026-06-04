package com.example.recommendation.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAuthorRequest(
        @NotBlank(message = "must not be blank") String name,
        @NotNull(message = "must not be null") Long publisherId,
        @NotNull(message = "must not be null")
        @DecimalMin(value = "0.0", message = "must be greater than or equal to 0.0")
        @DecimalMax(value = "100.0", message = "must be less than or equal to 100.0")
        Double internalRating
) {
}
