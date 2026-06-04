package com.example.recommendation.user.repository;

import com.example.recommendation.user.model.UserArticleInteractionEntity;
import com.example.recommendation.user.model.UserArticleInteractionType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserArticleInteractionRepository extends JpaRepository<UserArticleInteractionEntity, Long> {

    List<UserArticleInteractionEntity> findByUserId(Long userId);

    boolean existsByUserIdAndArticleIdAndInteractionType(
            Long userId,
            String articleId,
            UserArticleInteractionType interactionType
    );
}
