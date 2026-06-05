package com.example.recommendation;

import com.example.recommendation.author.repository.AuthorRepository;
import com.example.recommendation.article.repository.ArticleRepository;
import com.example.recommendation.profile.repository.UserProfileRepository;
import com.example.recommendation.publisher.repository.PublisherRepository;
import com.example.recommendation.subscription.repository.UserPublisherSubscriptionRepository;
import com.example.recommendation.user.repository.UserArticleInteractionRepository;
import com.example.recommendation.user.repository.UserArticleLikeRepository;
import com.example.recommendation.user.repository.UserRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackageClasses = {
        UserRepository.class,
        UserArticleLikeRepository.class,
        UserArticleInteractionRepository.class,
        PublisherRepository.class,
        AuthorRepository.class,
        UserPublisherSubscriptionRepository.class
})
@EnableElasticsearchRepositories(basePackageClasses = {
        ArticleRepository.class,
        UserProfileRepository.class
})
public class RecommendationApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendationApplication.class, args);
    }
}
