package com.example.recommendation.user.repository;

import com.example.recommendation.user.model.UserArticleLikeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserArticleLikeRepository extends JpaRepository<UserArticleLikeEntity, Long> {

    List<UserArticleLikeEntity> findByUserId(Long userId);

    boolean existsByUserIdAndArticleId(Long userId, String articleId);
}
