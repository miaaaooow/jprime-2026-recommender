# Botev - The Content Recommender App that will Save Democracy

Botev is a recommendation and ranking service for a higher-quality news aggregator.

A small Spring Boot service that keeps:

- users, publishers, authors, and interactions in a SQL database
- articles in Elasticsearch
- user recommendation profiles in Elasticsearch

The recommendation logic (so far): user interactions build a topic/tag profile, and recommendations rank unseen articles by comparing their labels against that profile with a selectable similarity model.

## Motivation

Botev is intended to grow into a news aggregator that serves readers better than social platforms such as Facebook and X.
The goal is to reward strong journalism with fairer access to advertising and reader revenue, while pushing the system toward quality instead of clickbait headlines and disinformation.

## Model

- `UserEntity`: SQL record for a user
- `UserArticleInteractionEntity`: SQL record for article reads, likes, and shares
- `UserArticleLikeEntity`: legacy SQL record for likes kept for backward compatibility
- `PublisherEntity`: SQL record for a publisher
- `AuthorEntity`: SQL record for an author, including an internal rating and publisher membership
- `ArticleDocument`: Elasticsearch document with `title`, `content`, `topics`, `tags`, `author`, and `publisher`
- `UserProfileDocument`: Elasticsearch document with topic/tag weights derived from article interactions

Topics and tags are still accepted directly, but new articles also go through an internal automatic tagging flow.
The `ArticleFeatureExtractor` abstraction is the seam for replacing the current heuristic tagger with NLP or LLM-based extraction later.

## Internal Tagging

`POST /api/internal/article-tagging` previews automatic tags for a draft article.
The current implementation uses keyword heuristics over the title and content, merges the detected labels with any manually supplied labels, and falls back to generic labels when no stronger match is found.

## Similarity Models

The active recommendation model is selected with `app.recommendation.similarity-model`.

- `fixed-weight`: default; builds a user profile from `READ`, `LIKE`, and `SHARE` interactions using fixed weights and scores articles by direct label overlap
- `weighted-cosine`: interaction-aware profile plus cosine similarity scoring
- `legacy-like-cosine`: preserves the old like-only cosine behavior as a third option

Default interaction weights:

- `READ`: `1.0`
- `LIKE`: `3.0`
- `SHARE`: `5.0`

## Serendipity

To reduce article cold start, recommendations reserve a small exploration slice for articles that introduce tags or topics the user has not interacted with before.
By default, `app.recommendation.serendipity.quota=0.1`, so recommendation lists try to allocate roughly 10% of their slots to these exploratory items while still keeping familiar matches first.

The current implementation prefers:

- articles with no topic or tag overlap at all
- articles that introduce more unseen labels
- newer articles when the novelty signal is otherwise similar

Prioritizing globally new tags can be added later without changing the external API.

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

A reusable demo dataset lives under `src/main/resources/testdata/`.
It includes:

- `publishers.json`
- `authors.json`
- `articles.json`

The startup loader seeds publishers and authors into SQL first, then writes the article dataset to Elasticsearch using the real generated author and publisher ids.
The sample content covers tourism, cooking, fashion, and current affairs, with a few cross-topic articles so recommendation overlap is easier to test.

Load it on startup only when you need it:

```bash
APP_TEST_DATA_ARTICLES_ENABLED=true mvn spring-boot:run
```

The loader upserts the same article ids on each run, so local testing stays deterministic.
Publisher and author records are also reused by name, and author ratings are refreshed from the dataset if they change.

## API

Create a user:

```bash
curl -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","email":"alice@example.com"}'
```

Create publishers and authors:

```bash
curl -X POST http://localhost:8080/api/publishers \
  -H 'Content-Type: application/json' \
  -d '{"name":"Botev Newsroom"}'

curl -X POST http://localhost:8080/api/authors \
  -H 'Content-Type: application/json' \
  -d '{"name":"Marta Ivanova","publisherId":1,"internalRating":88.0}'
```

Preview automatic tagging for a draft:

```bash
curl -X POST http://localhost:8080/api/internal/article-tagging \
  -H 'Content-Type: application/json' \
  -d '{
    "title":"Government reviews AI platform rules",
    "content":"The minister outlined new policy for artificial intelligence systems."
  }'
```

Create articles:

```bash
curl -X POST http://localhost:8080/api/articles \
  -H 'Content-Type: application/json' \
  -d '{
    "title":"Modern Search Ranking",
    "content":"How retrieval and ranking fit together.",
    "authorId":1,
    "publisherId":1,
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
- Article creation validates that the selected author belongs to the selected publisher.
