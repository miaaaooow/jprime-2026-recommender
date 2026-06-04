package com.example.recommendation.profile.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "user_profiles")
public class UserProfileDocument {

    @Id
    private Long userId;

    @Field(type = FieldType.Object)
    private Map<String, Double> topicWeights = new LinkedHashMap<>();

    @Field(type = FieldType.Object)
    private Map<String, Double> tagWeights = new LinkedHashMap<>();

    @Field(type = FieldType.Keyword)
    private List<String> likedArticleIds = new ArrayList<>();

    @Field(type = FieldType.Integer)
    private int likedArticleCount;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant updatedAt;

    public UserProfileDocument() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Map<String, Double> getTopicWeights() {
        return topicWeights;
    }

    public void setTopicWeights(Map<String, Double> topicWeights) {
        this.topicWeights = new LinkedHashMap<>(topicWeights);
    }

    public Map<String, Double> getTagWeights() {
        return tagWeights;
    }

    public void setTagWeights(Map<String, Double> tagWeights) {
        this.tagWeights = new LinkedHashMap<>(tagWeights);
    }

    public List<String> getLikedArticleIds() {
        return likedArticleIds;
    }

    public void setLikedArticleIds(List<String> likedArticleIds) {
        this.likedArticleIds = new ArrayList<>(likedArticleIds);
    }

    public int getLikedArticleCount() {
        return likedArticleCount;
    }

    public void setLikedArticleCount(int likedArticleCount) {
        this.likedArticleCount = likedArticleCount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
