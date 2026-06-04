package com.example.recommendation.article.bootstrap;

import com.example.recommendation.article.model.ArticleDocument;
import com.example.recommendation.article.repository.ArticleRepository;
import com.example.recommendation.author.model.AuthorEntity;
import com.example.recommendation.author.repository.AuthorRepository;
import com.example.recommendation.profile.service.UserProfileService;
import com.example.recommendation.publisher.model.PublisherEntity;
import com.example.recommendation.publisher.repository.PublisherRepository;
import com.example.recommendation.user.model.UserArticleInteractionEntity;
import com.example.recommendation.user.model.UserArticleInteractionType;
import com.example.recommendation.user.model.UserEntity;
import com.example.recommendation.user.repository.UserArticleInteractionRepository;
import com.example.recommendation.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.test-data.articles", name = "enabled", havingValue = "true")
public class ArticleTestDatasetLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ArticleTestDatasetLoader.class);

    private final ArticleRepository articleRepository;
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;
    private final UserRepository userRepository;
    private final UserArticleInteractionRepository userArticleInteractionRepository;
    private final UserProfileService userProfileService;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final String articleResourceLocation;
    private final String publisherResourceLocation;
    private final String authorResourceLocation;
    private final String userResourceLocation;
    private final String interactionResourceLocation;

    public ArticleTestDatasetLoader(
            ArticleRepository articleRepository,
            PublisherRepository publisherRepository,
            AuthorRepository authorRepository,
            UserRepository userRepository,
            UserArticleInteractionRepository userArticleInteractionRepository,
            UserProfileService userProfileService,
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            @Value("${app.test-data.articles.resource}") String articleResourceLocation,
            @Value("${app.test-data.articles.publishers-resource}") String publisherResourceLocation,
            @Value("${app.test-data.articles.authors-resource}") String authorResourceLocation,
            @Value("${app.test-data.articles.users-resource}") String userResourceLocation,
            @Value("${app.test-data.articles.interactions-resource}") String interactionResourceLocation
    ) {
        this.articleRepository = articleRepository;
        this.publisherRepository = publisherRepository;
        this.authorRepository = authorRepository;
        this.userRepository = userRepository;
        this.userArticleInteractionRepository = userArticleInteractionRepository;
        this.userProfileService = userProfileService;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.articleResourceLocation = articleResourceLocation;
        this.publisherResourceLocation = publisherResourceLocation;
        this.authorResourceLocation = authorResourceLocation;
        this.userResourceLocation = userResourceLocation;
        this.interactionResourceLocation = interactionResourceLocation;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Map<String, PublisherEntity> publishersByKey = seedPublishers(
                resourceLoader.getResource(publisherResourceLocation)
        );
        Map<AuthorLookupKey, AuthorEntity> authorsByKey = seedAuthors(
                resourceLoader.getResource(authorResourceLocation),
                publishersByKey
        );
        List<ArticleDocument> articles = loadArticles(
                resourceLoader.getResource(articleResourceLocation),
                publishersByKey,
                authorsByKey
        );
        Map<String, UserEntity> usersByKey = seedUsers(
                resourceLoader.getResource(userResourceLocation)
        );

        articleRepository.saveAll(articles);
        int interactionCount = seedInteractions(
                resourceLoader.getResource(interactionResourceLocation),
                usersByKey,
                articles
        );
        rebuildProfiles(usersByKey.values());
        log.info(
                "Loaded {} publishers, {} authors, {} test dataset articles, {} users, and {} interactions",
                publishersByKey.size(),
                authorsByKey.size(),
                articles.size(),
                usersByKey.size(),
                interactionCount
        );
    }

    Map<String, PublisherEntity> seedPublishers(Resource resource) throws IOException {
        List<DatasetPublisher> dataset = readResource(resource, new TypeReference<List<DatasetPublisher>>() {
        });
        Map<String, PublisherEntity> publishersByKey = new LinkedHashMap<>();

        for (DatasetPublisher publisher : dataset) {
            String normalizedName = normalizeName(publisher.name());
            PublisherEntity entity = publisherRepository.findByNameIgnoreCase(normalizedName)
                    .orElseGet(() -> publisherRepository.save(new PublisherEntity(normalizedName)));
            publishersByKey.put(normalizedName.toLowerCase(Locale.ROOT), entity);
        }

        return publishersByKey;
    }

    Map<AuthorLookupKey, AuthorEntity> seedAuthors(
            Resource resource,
            Map<String, PublisherEntity> publishersByKey
    ) throws IOException {
        List<DatasetAuthor> dataset = readResource(resource, new TypeReference<List<DatasetAuthor>>() {
        });
        Map<AuthorLookupKey, AuthorEntity> authorsByKey = new LinkedHashMap<>();

        for (DatasetAuthor author : dataset) {
            PublisherEntity publisher = requirePublisher(author.publisherName(), publishersByKey);
            String normalizedName = normalizeName(author.name());
            AuthorEntity entity = authorRepository.findByPublisherIdAndNameIgnoreCase(publisher.getId(), normalizedName)
                    .map(existingAuthor -> refreshAuthor(existingAuthor, author.internalRating()))
                    .orElseGet(() -> authorRepository.save(new AuthorEntity(
                            publisher.getId(),
                            normalizedName,
                            author.internalRating()
                    )));

            authorsByKey.put(new AuthorLookupKey(publisher.getId(), normalizedName.toLowerCase(Locale.ROOT)), entity);
        }

        return authorsByKey;
    }

    List<ArticleDocument> loadArticles(
            Resource resource,
            Map<String, PublisherEntity> publishersByKey,
            Map<AuthorLookupKey, AuthorEntity> authorsByKey
    ) throws IOException {
        List<DatasetArticle> dataset = readResource(resource, new TypeReference<List<DatasetArticle>>() {
        });

        return dataset.stream()
                .map(article -> toArticleDocument(article, publishersByKey, authorsByKey))
                .toList();
    }

    Map<String, UserEntity> seedUsers(Resource resource) throws IOException {
        List<DatasetUser> dataset = readResource(resource, new TypeReference<List<DatasetUser>>() {
        });
        Map<String, UserEntity> usersByKey = new LinkedHashMap<>();

        for (DatasetUser user : dataset) {
            String normalizedUsername = normalizeName(user.username());
            UserEntity entity = userRepository.findByUsernameIgnoreCase(normalizedUsername)
                    .map(existingUser -> refreshUser(existingUser, user.email()))
                    .orElseGet(() -> userRepository.save(new UserEntity(
                            normalizedUsername,
                            normalizeEmail(user.email())
                    )));

            usersByKey.put(normalizedUsername.toLowerCase(Locale.ROOT), entity);
        }

        return usersByKey;
    }

    int seedInteractions(
            Resource resource,
            Map<String, UserEntity> usersByKey,
            List<ArticleDocument> articles
    ) throws IOException {
        List<DatasetInteraction> dataset = readResource(resource, new TypeReference<List<DatasetInteraction>>() {
        });
        Set<String> articleIds = articles.stream()
                .map(ArticleDocument::getId)
                .collect(java.util.stream.Collectors.toSet());

        for (DatasetInteraction interaction : dataset) {
            UserEntity user = requireUser(interaction.username(), usersByKey);
            String articleId = requireArticleId(interaction.articleId(), articleIds);

            if (!userArticleInteractionRepository.existsByUserIdAndArticleIdAndInteractionType(
                    user.getId(),
                    articleId,
                    interaction.interactionType()
            )) {
                userArticleInteractionRepository.save(new UserArticleInteractionEntity(
                        user.getId(),
                        articleId,
                        interaction.interactionType()
                ));
            }
        }

        return dataset.size();
    }

    void rebuildProfiles(Iterable<UserEntity> users) {
        for (UserEntity user : users) {
            userProfileService.rebuildProfile(user.getId());
        }
    }

    private <T> T readResource(Resource resource, TypeReference<T> typeReference) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, typeReference);
        }
    }

    private ArticleDocument toArticleDocument(
            DatasetArticle article,
            Map<String, PublisherEntity> publishersByKey,
            Map<AuthorLookupKey, AuthorEntity> authorsByKey
    ) {
        PublisherEntity publisher = requirePublisher(article.publisherName(), publishersByKey);
        AuthorEntity author = requireAuthor(article.authorName(), publisher, authorsByKey);

        return new ArticleDocument(
                article.id().trim(),
                article.title().trim(),
                article.content().trim(),
                normalizeLabels(article.topics()),
                normalizeLabels(article.tags()),
                author.getId(),
                author.getName(),
                publisher.getId(),
                publisher.getName(),
                article.createdAt()
        );
    }

    private AuthorEntity refreshAuthor(AuthorEntity author, double internalRating) {
        if (Double.compare(author.getInternalRating(), internalRating) != 0) {
            author.setInternalRating(internalRating);
            return authorRepository.save(author);
        }

        return author;
    }

    private UserEntity refreshUser(UserEntity user, String email) {
        String normalizedEmail = normalizeEmail(email);
        if (!user.getEmail().equalsIgnoreCase(normalizedEmail)) {
            user.setEmail(normalizedEmail);
            return userRepository.save(user);
        }

        return user;
    }

    private PublisherEntity requirePublisher(String publisherName, Map<String, PublisherEntity> publishersByKey) {
        PublisherEntity publisher = publishersByKey.get(normalizeKey(publisherName));
        if (publisher == null) {
            throw new IllegalStateException("Unknown publisher in dataset: %s".formatted(publisherName));
        }
        return publisher;
    }

    private AuthorEntity requireAuthor(
            String authorName,
            PublisherEntity publisher,
            Map<AuthorLookupKey, AuthorEntity> authorsByKey
    ) {
        AuthorEntity author = authorsByKey.get(new AuthorLookupKey(
                publisher.getId(),
                normalizeKey(authorName)
        ));
        if (author == null) {
            throw new IllegalStateException("Unknown author %s for publisher %s in dataset.".formatted(
                    authorName,
                    publisher.getName()
            ));
        }
        return author;
    }

    private UserEntity requireUser(String username, Map<String, UserEntity> usersByKey) {
        UserEntity user = usersByKey.get(normalizeKey(username));
        if (user == null) {
            throw new IllegalStateException("Unknown user in dataset: %s".formatted(username));
        }
        return user;
    }

    private String requireArticleId(String articleId, Set<String> articleIds) {
        String normalizedArticleId = normalizeName(articleId);
        if (!articleIds.contains(normalizedArticleId)) {
            throw new IllegalStateException("Unknown article in interaction dataset: %s".formatted(articleId));
        }
        return normalizedArticleId;
    }

    private List<String> normalizeLabels(List<String> values) {
        return values == null
                ? List.of()
                : values.stream()
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .toList();
    }

    private String normalizeName(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalStateException("Dataset contains a blank name.");
        }
        return trimmed;
    }

    private String normalizeKey(String value) {
        return normalizeName(value).toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String value) {
        String trimmed = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            throw new IllegalStateException("Dataset contains a blank email.");
        }
        return trimmed;
    }

    private record DatasetPublisher(String name) {
    }

    private record DatasetAuthor(
            String name,
            String publisherName,
            double internalRating
    ) {
    }

    private record DatasetUser(
            String username,
            String email
    ) {
    }

    private record DatasetInteraction(
            String username,
            String articleId,
            UserArticleInteractionType interactionType
    ) {
    }

    private record DatasetArticle(
            String id,
            String title,
            String content,
            List<String> topics,
            List<String> tags,
            String authorName,
            String publisherName,
            Instant createdAt
    ) {
    }

    record AuthorLookupKey(Long publisherId, String normalizedAuthorName) {
    }
}
