# Phase 4 Verification

## Start stack

```bash
docker compose up --build
```

Expected services:

- `legocitytimes-postgres`
- `legocitytimes-content-service`
- `legocitytimes-elasticsearch`
- `legocitytimes-search-service`

Swagger UI for the search service:

```bash
curl http://localhost:8081/swagger-ui.html
```

## Index a published article

```bash
curl -i -X POST http://localhost:8081/internal/search/articles/index \
  -H "Content-Type: application/json" \
  -d '{
    "id": "article-verify-1",
    "title": "Lego City Opens New Train Station",
    "subtitle": "The central station is ready for passengers",
    "content": "The new train station connects the harbor, downtown and airport districts.",
    "author": "Reporter",
    "slug": "lego-city-opens-new-train-station",
    "categoryId": "transport",
    "categoryName": "Transport",
    "tagIds": ["rail", "city"],
    "tagNames": ["Rail", "City"],
    "publishedAt": "2026-05-18T10:00:00Z",
    "coverImageUrl": "/uploads/images/train-station.webp",
    "status": "PUBLISHED"
  }'
```

Expected: `HTTP/1.1 200`.

## Search it

```bash
curl "http://localhost:8081/api/v1/search/articles?q=train&categoryId=transport&tagId=rail&sort=relevance"
```

Expected: response contains `article-verify-1`.

## Delete it

```bash
curl -i -X DELETE http://localhost:8081/internal/search/articles/article-verify-1
```

Expected: `HTTP/1.1 204`.

## Confirm it is gone

```bash
curl "http://localhost:8081/api/v1/search/articles?q=train&categoryId=transport&tagId=rail"
```

Expected: `totalElements` is `0` for the verification article.

## Verify DRAFT/ARCHIVED safety rule

```bash
curl -i -X POST http://localhost:8081/internal/search/articles/index \
  -H "Content-Type: application/json" \
  -d '{
    "id": "article-verify-1",
    "title": "Draft Article",
    "author": "Reporter",
    "slug": "draft-article",
    "status": "DRAFT"
  }'
```

Expected: `HTTP/1.1 200`, document is not searchable.
