package com.example.recommendation.api.controller;

import com.example.recommendation.api.dto.RecommendationResponse;
import com.example.recommendation.recommendation.service.RecommendationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/users/{userId}/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public List<RecommendationResponse> recommend(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit
    ) {
        return recommendationService.recommendForUser(userId, limit);
    }
}
