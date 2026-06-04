package com.example.recommendation.article.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "articles")
public class ArticleDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String content;

    @Field(type = FieldType.Keyword)
    private List<String> topics = new ArrayList<>();

    @Field(type = FieldType.Keyword)
    private List<String> tags = new ArrayList<>();

    @Field(type = FieldType.Long)
    private Long authorId;

    @Field(type = FieldType.Keyword)
    private String authorName;

    @Field(type = FieldType.Long)
    private Long publisherId;

    @Field(type = FieldType.Keyword)
    private String publisherName;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant createdAt;

    public ArticleDocument() {
    }

    public ArticleDocument(String id, String title, String content, List<String> topics, List<String> tags, Instant createdAt) {
        this(id, title, content, topics, tags, null, null, null, null, createdAt);
    }

    public ArticleDocument(
            String id,
            String title,
            String content,
            List<String> topics,
            List<String> tags,
            Long authorId,
            String authorName,
            Long publisherId,
            String publisherName,
            Instant createdAt
    ) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.topics = new ArrayList<>(topics);
        this.tags = new ArrayList<>(tags);
        this.authorId = authorId;
        this.authorName = authorName;
        this.publisherId = publisherId;
        this.publisherName = publisherName;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = new ArrayList<>(topics);
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = new ArrayList<>(tags);
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Long getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(Long publisherId) {
        this.publisherId = publisherId;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
