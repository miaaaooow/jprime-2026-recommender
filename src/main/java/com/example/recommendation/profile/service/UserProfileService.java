package com.example.recommendation.profile.service;

import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.article.repository.ArticleRepository;
import com.example.recommendation.common.exception.ResourceNotFoundException;
import com.example.recommendation.profile.model.UserProfileDocument;
import com.example.recommendation.profile.repository.UserProfileRepository;
import com.example.recommendation.user.model.UserArticleLikeEntity;
import com.example.recommendation.user.repository.UserArticleLikeRepository;
import com.example.recommendation.user.service.UserService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserService userService;
    private final ArticleRepository articleRepository;
    private final UserArticleLikeRepository userArticleLikeRepository;
    private final UserProfileRepository userProfileRepository;

    public UserProfileService(
            UserService userService,
            ArticleRepository articleRepository,
            UserArticleLikeRepository userArticleLikeRepository,
            UserProfileRepository userProfileRepository
    ) {
        this.userService = userService;
        this.articleRepository = articleRepository;
        this.userArticleLikeRepository = userArticleLikeRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public UserProfileDocument likeArticle(Long userId, String articleId) {
        userService.getUser(userId);
        articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article %s was not found.".formatted(articleId)));

        if (!userArticleLikeRepository.existsByUserIdAndArticleId(userId, articleId)) {
            userArticleLikeRepository.save(new UserArticleLikeEntity(userId, articleId));
        }

        return rebuildProfile(userId);
    }

    public UserProfileDocument getProfile(Long userId) {
        userService.getUser(userId);
        return userProfileRepository.findById(userId)
                .orElseGet(() -> rebuildProfile(userId));
    }

    public UserProfileDocument rebuildProfile(Long userId) {
        userService.getUser(userId);

        List<String> likedArticleIds = userArticleLikeRepository.findByUserId(userId)
                .stream()
                .map(UserArticleLikeEntity::getArticleId)
                .distinct()
                .toList();

        List<ArticleDocument> likedArticles = likedArticleIds.isEmpty()
                ? List.of()
                : StreamSupport.stream(articleRepository.findAllById(likedArticleIds).spliterator(), false).toList();

        UserProfileDocument profile = new UserProfileDocument();
        profile.setUserId(userId);
        profile.setLikedArticleIds(likedArticleIds);
        profile.setLikedArticleCount(likedArticleIds.size());
        profile.setTopicWeights(normalizeWeights(flattenLabels(likedArticles, LabelType.TOPIC)));
        profile.setTagWeights(normalizeWeights(flattenLabels(likedArticles, LabelType.TAG)));
        profile.setUpdatedAt(Instant.now());

        return userProfileRepository.save(profile);
    }

    private List<String> flattenLabels(List<ArticleDocument> articles, LabelType labelType) {
        List<String> labels = new ArrayList<>();
        for (ArticleDocument article : articles) {
            if (labelType == LabelType.TOPIC) {
                labels.addAll(article.getTopics());
            } else {
                labels.addAll(article.getTags());
            }
        }
        return labels;
    }

    private Map<String, Double> normalizeWeights(Collection<String> labels) {
        if (labels.isEmpty()) {
            return Map.of();
        }

        Map<String, Double> counts = new LinkedHashMap<>();
        for (String label : labels) {
            counts.merge(label, 1.0, Double::sum);
        }

        double total = counts.values().stream().mapToDouble(Double::doubleValue).sum();

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue() / total),
                        LinkedHashMap::putAll);
    }

    private enum LabelType {
        TOPIC,
        TAG
    }
}
