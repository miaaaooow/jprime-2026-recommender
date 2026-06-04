package com.example.recommendation.config;

import com.example.recommendation.user.model.UserArticleInteractionType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.recommendation")
public class RecommendationProperties {

    private String similarityModel = "fixed-weight";
    private final LabelWeights labelWeights = new LabelWeights();
    private final InteractionWeights interactionWeights = new InteractionWeights();
    private final Serendipity serendipity = new Serendipity();

    public String getSimilarityModel() {
        return similarityModel;
    }

    public void setSimilarityModel(String similarityModel) {
        this.similarityModel = similarityModel;
    }

    public LabelWeights getLabelWeights() {
        return labelWeights;
    }

    public InteractionWeights getInteractionWeights() {
        return interactionWeights;
    }

    public Serendipity getSerendipity() {
        return serendipity;
    }

    public static class LabelWeights {

        private double topic = 0.7;
        private double tag = 0.3;

        public double getTopic() {
            return topic;
        }

        public void setTopic(double topic) {
            this.topic = topic;
        }

        public double getTag() {
            return tag;
        }

        public void setTag(double tag) {
            this.tag = tag;
        }
    }

    public static class InteractionWeights {

        private double read = 1.0;
        private double like = 3.0;
        private double share = 5.0;

        public double getRead() {
            return read;
        }

        public void setRead(double read) {
            this.read = read;
        }

        public double getLike() {
            return like;
        }

        public void setLike(double like) {
            this.like = like;
        }

        public double getShare() {
            return share;
        }

        public void setShare(double share) {
            this.share = share;
        }

        public double weightFor(UserArticleInteractionType interactionType) {
            return switch (interactionType) {
                case READ -> read;
                case LIKE -> like;
                case SHARE -> share;
            };
        }
    }

    public static class Serendipity {

        private double quota = 0.1;

        public double getQuota() {
            return quota;
        }

        public void setQuota(double quota) {
            this.quota = quota;
        }
    }
}
