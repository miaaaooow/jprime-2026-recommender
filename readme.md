# Recommendation App

A small Spring Boot service that keeps:

- users and likes in a SQL database
- articles in Elasticsearch
- user recommendation profiles in Elasticsearch

The recommendation logic is intentionally simple: when a user likes articles, the service builds a profile from the frequency of article topics and tags. Recommendations are then ranked by cosine similarity between that profile and each unseen article.

## Model

- `UserEntity`: SQL record for a user
- `UserArticleLikeEntity`: SQL record for a user liking an article
- `ArticleDocument`: Elasticsearch document with `title`, `content`, `topics`, and `tags`
- `UserProfileDocument`: Elasticsearch document with normalized topic/tag weights derived from liked articles

Topics and tags are accepted directly today. The `ArticleFeatureExtractor` abstraction is the seam for replacing manual metadata with NLP or LLM-based extraction from article text later.

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

Like an article and update the profile:

```bash
curl -X POST http://localhost:8080/api/users/1/likes/<article-id>
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
- Because articles are stored in Elasticsearch, there is no SQL foreign key from likes to articles. The service validates article existence before saving a like.
