package com.example.recommendation.article.extractor;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class KeywordArticleFeatureExtractor implements ArticleFeatureExtractor {

    private static final List<KeywordRule> TOPIC_RULES = List.of(
            new KeywordRule("politics", List.of("election", "parliament", "congress", "senate", "government", "minister", "campaign", "coalition", "policy", "president")),
            new KeywordRule("economy", List.of("inflation", "gdp", "economy", "recession", "interest rate", "central bank", "unemployment", "wages", "consumer prices")),
            new KeywordRule("business", List.of("company", "startup", "earnings", "revenue", "profit", "merger", "acquisition", "shareholder")),
            new KeywordRule("technology", List.of("software", "platform", "cloud", "cyber", "artificial intelligence", "ai", "semiconductor", "chip", "robotics")),
            new KeywordRule("climate", List.of("climate", "carbon", "emissions", "heatwave", "drought", "wildfire", "flood", "renewable")),
            new KeywordRule("health", List.of("hospital", "vaccine", "outbreak", "public health", "medical", "healthcare", "disease")),
            new KeywordRule("sports", List.of("match", "tournament", "league", "season", "player", "coach", "goal", "championship")),
            new KeywordRule("culture", List.of("film", "music", "theatre", "museum", "festival", "novel", "exhibition")),
            new KeywordRule("tourism", List.of("travel", "flight", "hotel", "itinerary", "destination", "tourism", "visitor", "city break", "beach")),
            new KeywordRule("cooking", List.of("recipe", "kitchen", "dinner", "bake", "baking", "ingredients", "soup", "pasta", "sourdough")),
            new KeywordRule("fashion", List.of("fashion", "runway", "collection", "designer", "wardrobe", "styling", "tailoring", "textile", "luxury"))
    );

    private static final List<KeywordRule> TAG_RULES = List.of(
            new KeywordRule("elections", List.of("election", "campaign", "ballot")),
            new KeywordRule("policy", List.of("policy", "regulation", "bill", "reform")),
            new KeywordRule("inflation", List.of("inflation", "consumer prices")),
            new KeywordRule("markets", List.of("market", "stocks", "bonds", "investors")),
            new KeywordRule("ai", List.of("artificial intelligence", "ai", "machine learning")),
            new KeywordRule("cybersecurity", List.of("cyber", "ransomware", "data breach")),
            new KeywordRule("renewable-energy", List.of("renewable", "solar", "wind power")),
            new KeywordRule("public-health", List.of("public health", "vaccine", "outbreak")),
            new KeywordRule("football", List.of("goal", "football", "soccer", "striker")),
            new KeywordRule("film", List.of("film", "cinema", "director")),
            new KeywordRule("travel", List.of("travel", "hotel", "flight", "itinerary")),
            new KeywordRule("recipe", List.of("recipe", "ingredients", "cook", "cooking")),
            new KeywordRule("baking", List.of("bake", "baking", "sourdough")),
            new KeywordRule("style", List.of("style", "styling", "wardrobe", "tailoring")),
            new KeywordRule("luxury", List.of("luxury", "designer", "premium")),
            new KeywordRule("disinformation", List.of("misinformation", "disinformation", "false claims"))
    );

    @Override
    public ArticleFeatures extract(String title, String content, List<String> topics, List<String> tags) {
        String searchableText = ((title == null ? "" : title) + " " + (content == null ? "" : content))
                .toLowerCase(Locale.ROOT);

        List<String> mergedTopics = merge(normalize(topics), detect(searchableText, TOPIC_RULES));
        List<String> mergedTags = merge(normalize(tags), detect(searchableText, TAG_RULES));

        if (mergedTopics.isEmpty()) {
            mergedTopics = List.of("current-affairs");
        }

        if (mergedTags.isEmpty()) {
            mergedTags = List.of("news");
        }

        return new ArticleFeatures(mergedTopics, mergedTags);
    }

    private List<String> detect(String searchableText, List<KeywordRule> rules) {
        return rules.stream()
                .filter(rule -> rule.matches(searchableText))
                .map(KeywordRule::label)
                .sorted()
                .toList();
    }

    private List<String> merge(List<String> manualLabels, List<String> detectedLabels) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.addAll(manualLabels);
        merged.addAll(detectedLabels);

        return merged.stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private List<String> normalize(List<String> values) {
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
                .toList();
    }

    private record KeywordRule(String label, List<String> keywords) {

        private boolean matches(String searchableText) {
            return keywords.stream().anyMatch(keyword -> matchesKeyword(searchableText, keyword));
        }

        private boolean matchesKeyword(String searchableText, String keyword) {
            if (keyword.contains(" ")) {
                return searchableText.contains(keyword);
            }

            return Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b")
                    .matcher(searchableText)
                    .find();
        }
    }
}
