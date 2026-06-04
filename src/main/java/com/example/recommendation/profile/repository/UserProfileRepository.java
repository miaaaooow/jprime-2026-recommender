package com.example.recommendation.profile.repository;

import com.example.recommendation.profile.model.UserProfileDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface UserProfileRepository extends ElasticsearchRepository<UserProfileDocument, Long> {
}
