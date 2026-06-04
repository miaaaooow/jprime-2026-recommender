# Botev - The Content Recommender App that will Save Democracy

Botev is a recommendation and ranking service for a higher-quality news aggregator.

A small Spring Boot service that keeps:

- users and likes in a SQL database
- articles in Elasticsearch
- user recommendation profiles in Elasticsearch

The recommendation logic (so far): user interactions build a topic/tag profile, and recommendations rank unseen articles by comparing their labels against that profile with a selectable similarity model.

## Motivation

Botev is intended to grow into a news aggregator that serves readers better than social platforms such as Facebook and X.
The goal is to reward strong journalism with fairer access to advertising and reader revenue, while pushing the system toward quality instead of clickbait headlines and disinformation.

## Model

- `UserEntity`: SQL record for a user
- `UserArticleLikeEntity`: SQL record for a user liking an article
- `ArticleDocument`: Elasticsearch document with `title`, `content`, `topics`, and `tags`
- `UserProfileDocument`: Elasticsearch document with topic/tag weights derived from article interactions

Topics and tags are accepted directly today. The `ArticleFeatureExtractor` abstraction is the seam for replacing manual metadata with NLP or LLM-based extraction from article text later.

## Similarity Models

The active recommendation model is selected with `app.recommendation.similarity-model`.

- `fixed-weight`: default; builds a user profile from `READ`, `LIKE`, and `SHARE` interactions using fixed weights and scores articles by direct label overlap
- `weighted-cosine`: interaction-aware profile plus cosine similarity scoring
- `legacy-like-cosine`: preserves the old like-only cosine behavior as a third option

Default interaction weights:

- `READ`: `1.0`
- `LIKE`: `3.0`
- `SHARE`: `5.0`

## Run

Prerequisites:

- Java 17+
- Maven 3.9+
- Elasticsearch running locally on `http://localhost:9200`

Start the app:

```bash
mvn spring-boot:run
```

Config:

- SQL DB: H2 file database at `./data/recommendation`
- Elasticsearch URL: `ELASTICSEARCH_URIS` env var, default `http://localhost:9200`
- H2 console: `http://localhost:8080/h2-console`

## Sample Test Dataset

A reusable article dataset lives at `src/main/resources/testdata/articles.json`.
It includes tourism, cooking, fashion, and current affairs samples, with a few cross-topic articles so recommendation overlap is easier to test.

Load it on startup only when you need it:

```bash
APP_TEST_DATA_ARTICLES_ENABLED=true mvn spring-boot:run
```

The loader upserts the same article ids on each run, so local testing stays deterministic.

## API

Create a user:

```bash
curl -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","email":"alice@example.com"}'
```

Create articles:

```bash
curl -X POST http://localhost:8080/api/articles \
  -H 'Content-Type: application/json' \
  -d '{
    "title":"Modern Search Ranking",
    "content":"How retrieval and ranking fit together.",
    "topics":["search","ai"],
    "tags":["ranking","ml"]
  }'
```

Record interactions and update the profile:

```bash
curl -X POST http://localhost:8080/api/users/1/reads/<article-id>

curl -X POST http://localhost:8080/api/users/1/likes/<article-id>

curl -X POST http://localhost:8080/api/users/1/shares/<article-id>
```

Inspect the stored profile:

```bash
curl http://localhost:8080/api/users/1/profile
```

Fetch recommendations:

```bash
curl 'http://localhost:8080/api/users/1/recommendations?limit=5'
```

## Notes

- This implementation scans all articles when ranking recommendations. That is fine for a small app and easy to reason about.
- For larger scale, push candidate generation into Elasticsearch and keep the Java service for profile updates, filtering, and reranking.
- Because articles are stored in Elasticsearch, there is no SQL foreign key from interactions to articles. The service validates article existence before saving an interaction.
