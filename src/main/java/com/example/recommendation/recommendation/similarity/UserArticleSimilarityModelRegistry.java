package com.example.recommendation.recommendation.similarity;

import com.example.recommendation.config.RecommendationProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class UserArticleSimilarityModelRegistry {

    private final Map<String, UserArticleSimilarityModel> modelsByKey;
    private final RecommendationProperties recommendationProperties;

    public UserArticleSimilarityModelRegistry(
            List<UserArticleSimilarityModel> models,
            RecommendationProperties recommendationProperties
    ) {
        this.recommendationProperties = recommendationProperties;
        this.modelsByKey = models.stream()
                .collect(LinkedHashMap::new,
                        (map, model) -> map.put(model.key(), model),
                        Map::putAll);

        if (!modelsByKey.containsKey(recommendationProperties.getSimilarityModel())) {
            throw new IllegalStateException("Unknown recommendation similarity model: %s".formatted(
                    recommendationProperties.getSimilarityModel()
            ));
        }
    }

    public UserArticleSimilarityModel getActiveModel() {
        return modelsByKey.get(recommendationProperties.getSimilarityModel());
    }

    public Map<String, UserArticleSimilarityModel> getModelsByKey() {
        return Map.copyOf(modelsByKey);
    }
}
