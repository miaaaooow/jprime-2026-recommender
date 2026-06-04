package com.example.recommendation.article.extractor;

import com.example.recommendation.config.ArticleTaggingProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OpenAiSmartArticleTaggingClient implements SmartArticleTaggingClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiSmartArticleTaggingClient.class);

    private final ArticleTaggingProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiSmartArticleTaggingClient(ArticleTaggingProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder().build());
    }

    OpenAiSmartArticleTaggingClient(
            ArticleTaggingProperties properties,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<ArticleFeatureExtractor.ArticleFeatures> extract(
            String title,
            String content,
            List<String> candidateTopics,
            List<String> candidateTags
    ) {
        if (!isActive()) {
            return Optional.empty();
        }

        if (!"openai".equalsIgnoreCase(properties.getSmart().getProvider())) {
            log.warn("Unsupported smart tagging provider: {}", properties.getSmart().getProvider());
            return Optional.empty();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getSmart().getBaseUrl()))
                    .timeout(properties.getSmart().getTimeout())
                    .header("Authorization", "Bearer " + properties.getSmart().getApiKey().trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(
                            title,
                            content,
                            candidateTopics,
                            candidateTags
                    )))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("OpenAI smart tagging request failed with status {}", response.statusCode());
                return Optional.empty();
            }

            return parseResponse(response.body());
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("OpenAI smart tagging request failed: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private boolean isActive() {
        return properties.getSmart().isEnabled()
                && properties.getSmart().getApiKey() != null
                && !properties.getSmart().getApiKey().isBlank();
    }

    private String buildRequestBody(
            String title,
            String content,
            List<String> candidateTopics,
            List<String> candidateTags
    ) throws JsonProcessingException {
        List<InputItem> input = List.of(
                new InputItem("system", List.of(new InputText("""
                        You classify news-style articles into topics and tags.
                        Return only structured JSON with two arrays: topics and tags.
                        Keep labels lowercase, concise, and deduplicated.
                        Prefer the provided candidate labels when they fit the article.
                        """))),
                new InputItem("user", List.of(new InputText(buildUserPrompt(
                        title,
                        content,
                        candidateTopics,
                        candidateTags
                ))))
        );

        ResponseFormat format = new ResponseFormat(
                "json_schema",
                "article_features",
                articleFeaturesSchema(),
                "Structured article topics and tags.",
                true
        );

        OpenAiResponsesRequest request = new OpenAiResponsesRequest(
                properties.getSmart().getModel(),
                input,
                new TextConfig(format)
        );

        return objectMapper.writeValueAsString(request);
    }

    private String buildUserPrompt(
            String title,
            String content,
            List<String> candidateTopics,
            List<String> candidateTags
    ) {
        return """
                Title:
                %s

                Content:
                %s

                Candidate topics:
                %s

                Candidate tags:
                %s

                Select up to %d topics and up to %d tags.
                Prefer specific, newsroom-friendly labels.
                """.formatted(
                safeText(title),
                safeText(content),
                candidateTopics,
                candidateTags,
                properties.getSmart().getMaxTopics(),
                properties.getSmart().getMaxTags()
        );
    }

    private JsonNode articleFeaturesSchema() {
        return objectMapper.valueToTree(new JsonSchema(
                "object",
                new JsonSchemaProperties(
                        new ArrayProperty("array", new ItemProperty("string")),
                        new ArrayProperty("array", new ItemProperty("string"))
                ),
                List.of("topics", "tags"),
                false
        ));
    }

    private Optional<ArticleFeatureExtractor.ArticleFeatures> parseResponse(String body) throws JsonProcessingException {
        OpenAiResponsesResponse response = objectMapper.readValue(body, OpenAiResponsesResponse.class);
        if (response.output() == null) {
            return Optional.empty();
        }

        for (OutputItem item : response.output()) {
            if (!"message".equals(item.type()) || item.content() == null) {
                continue;
            }

            for (ContentItem contentItem : item.content()) {
                if ("output_text".equals(contentItem.type()) && contentItem.text() != null) {
                    SmartFeatures smartFeatures = objectMapper.readValue(contentItem.text(), SmartFeatures.class);
                    return Optional.of(new ArticleFeatureExtractor.ArticleFeatures(
                            normalize(smartFeatures.topics(), properties.getSmart().getMaxTopics()),
                            normalize(smartFeatures.tags(), properties.getSmart().getMaxTags())
                    ));
                }
            }
        }

        return Optional.empty();
    }

    private List<String> normalize(List<String> values, int maxSize) {
        if (values == null) {
            return List.of();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .limit(Math.max(0, maxSize))
                .toList();
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private record OpenAiResponsesRequest(
            String model,
            List<InputItem> input,
            TextConfig text
    ) {
    }

    private record InputItem(
            String role,
            List<InputText> content
    ) {
    }

    private record InputText(
            String type,
            String text
    ) {
        private InputText(String text) {
            this("input_text", text);
        }
    }

    private record TextConfig(
            ResponseFormat format
    ) {
    }

    private record ResponseFormat(
            String type,
            String name,
            JsonNode schema,
            String description,
            boolean strict
    ) {
    }

    private record JsonSchema(
            String type,
            JsonSchemaProperties properties,
            List<String> required,
            boolean additionalProperties
    ) {
    }

    private record JsonSchemaProperties(
            ArrayProperty topics,
            ArrayProperty tags
    ) {
    }

    private record ArrayProperty(
            String type,
            ItemProperty items
    ) {
    }

    private record ItemProperty(
            String type
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiResponsesResponse(
            List<OutputItem> output
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OutputItem(
            String type,
            List<ContentItem> content
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ContentItem(
            String type,
            String text
    ) {
    }

    private record SmartFeatures(
            List<String> topics,
            List<String> tags
    ) {
    }
}
