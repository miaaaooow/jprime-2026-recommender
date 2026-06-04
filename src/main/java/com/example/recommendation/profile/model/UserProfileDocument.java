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

    @Field(type = FieldType.Keyword)
    private String similarityModelKey;

    @Field(type = FieldType.Object)
    private Map<String, Double> topicWeights = new LinkedHashMap<>();

    @Field(type = FieldType.Object)
    private Map<String, Double> tagWeights = new LinkedHashMap<>();

    @Field(type = FieldType.Keyword)
    private List<String> readArticleIds = new ArrayList<>();

    @Field(type = FieldType.Integer)
    private int readArticleCount;

    @Field(type = FieldType.Keyword)
    private List<String> likedArticleIds = new ArrayList<>();

    @Field(type = FieldType.Integer)
    private int likedArticleCount;

    @Field(type = FieldType.Keyword)
    private List<String> sharedArticleIds = new ArrayList<>();

    @Field(type = FieldType.Integer)
    private int sharedArticleCount;

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

    public String getSimilarityModelKey() {
        return similarityModelKey;
    }

    public void setSimilarityModelKey(String similarityModelKey) {
        this.similarityModelKey = similarityModelKey;
    }

    public Map<String, Double> getTopicWeights() {
        if (topicWeights == null) {
            topicWeights = new LinkedHashMap<>();
        }
        return topicWeights;
    }

    public void setTopicWeights(Map<String, Double> topicWeights) {
        this.topicWeights = topicWeights == null ? new LinkedHashMap<>() : new LinkedHashMap<>(topicWeights);
    }

    public Map<String, Double> getTagWeights() {
        if (tagWeights == null) {
            tagWeights = new LinkedHashMap<>();
        }
        return tagWeights;
    }

    public void setTagWeights(Map<String, Double> tagWeights) {
        this.tagWeights = tagWeights == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tagWeights);
    }

    public List<String> getReadArticleIds() {
        if (readArticleIds == null) {
            readArticleIds = new ArrayList<>();
        }
        return readArticleIds;
    }

    public void setReadArticleIds(List<String> readArticleIds) {
        this.readArticleIds = readArticleIds == null ? new ArrayList<>() : new ArrayList<>(readArticleIds);
    }

    public int getReadArticleCount() {
        return readArticleCount;
    }

    public void setReadArticleCount(int readArticleCount) {
        this.readArticleCount = readArticleCount;
    }

    public List<String> getLikedArticleIds() {
        if (likedArticleIds == null) {
            likedArticleIds = new ArrayList<>();
        }
        return likedArticleIds;
    }

    public void setLikedArticleIds(List<String> likedArticleIds) {
        this.likedArticleIds = likedArticleIds == null ? new ArrayList<>() : new ArrayList<>(likedArticleIds);
    }

    public int getLikedArticleCount() {
        return likedArticleCount;
    }

    public void setLikedArticleCount(int likedArticleCount) {
        this.likedArticleCount = likedArticleCount;
    }

    public List<String> getSharedArticleIds() {
        if (sharedArticleIds == null) {
            sharedArticleIds = new ArrayList<>();
        }
        return sharedArticleIds;
    }

    public void setSharedArticleIds(List<String> sharedArticleIds) {
        this.sharedArticleIds = sharedArticleIds == null ? new ArrayList<>() : new ArrayList<>(sharedArticleIds);
    }

    public int getSharedArticleCount() {
        return sharedArticleCount;
    }

    public void setSharedArticleCount(int sharedArticleCount) {
        this.sharedArticleCount = sharedArticleCount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
