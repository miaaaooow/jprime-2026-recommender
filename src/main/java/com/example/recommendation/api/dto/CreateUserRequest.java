package com.example.recommendation.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank(message = "must not be blank") String username,
        @NotBlank(message = "must not be blank") @Email(message = "must be a valid email") String email
) {
}
