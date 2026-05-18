# Phase 4 — Search Service mit Elasticsearch

## Ziel

Einführung eines eigenständigen `search-service` als zweiter Microservice. Der Service kapselt Elasticsearch vollständig — der `content-service` (und später Clients) sprechen nie direkt mit Elasticsearch.

**Abgeschlossen, wenn:**
- `search-service` läuft als eigener Container neben `content-service` + `postgres`.
- Elasticsearch läuft im Compose-Stack.
- Such-Endpunkte liefern Treffer aus Elasticsearch (Filter, Pagination, Sortierung).
- Index-Endpunkte (`POST /internal/search/articles/index`, `DELETE /internal/search/articles/{id}`) funktionieren.
- Manuelle Verifikation per curl: indexieren → suchen → löschen → nicht mehr findbar.

### In Scope
- Elasticsearch in Docker Compose
- `search-service` als neues Spring-Boot-Modul
- `ArticleDocument`-Mapping
- Interner Index-Endpunkt
- Interner Delete-Endpunkt
- Öffentlicher Search-Endpunkt
- Pagination, Filter, Sortierung
- Healthcheck, Swagger, Tests
- Manuelle Verifikation per curl

### Out of Scope (kommt später)
- Sync vom `content-service` → Phase 5
- Echter Reindex aus PostgreSQL → Phase 5
- Kafka / Outbox Pattern
- Auth / JWT
- Autocomplete
- Facets / Aggregationen
- Highlighting

---

## Architektur-Constraints (aus Architectural Rules)

- PostgreSQL = Source of Truth, Elasticsearch = nur Suche.
- Nur **veröffentlichte** (`PUBLISHED`) Artikel werden indexiert.
- Search Service schreibt **nie** nach Postgres.
- **Der `search-service` bekommt in Phase 4 keine PostgreSQL-Dependency** — kein JDBC-Treiber, kein Spring Data JPA im `pom.xml`.
- Der `search-service` kennt **keine** JPA-Entities aus dem `content-service` — Daten kommen ausschließlich über `ArticleIndexRequest` rein.
- Search Service exponiert **keine** vollständigen Artikel-Daten — nur das, was für Suche/Listing nötig ist.

---

## Arbeitspakete

### 4.1 Elasticsearch in Docker Compose

- [ ] Neuen Service in [docker-compose.yml](../docker-compose.yml) ergänzen: `elasticsearch:8.x` (single-node, `xpack.security.enabled=false` für lokale Entwicklung).
- [ ] Volume `esdata` für Persistenz.
- [ ] Healthcheck: `curl -fs "http://localhost:9200/_cluster/health?wait_for_status=yellow&timeout=5s"` — Single-Node-ES bleibt `yellow`, das ist OK.
- [ ] Port `9200` nur intern (kein Host-Mapping nötig, optional `9200:9200` für lokales Debugging via `curl`/Kibana).

### 4.2 Maven-Modul `search-service` anlegen

- [ ] Verzeichnis `search-service/` parallel zu `content-service/`.
- [ ] `pom.xml` analog zu `content-service`:
  - Spring Boot 4.0.6, Java 21
  - `spring-boot-starter-webmvc`
  - `spring-boot-starter-data-elasticsearch`
  - `spring-boot-starter-actuator`
  - `spring-boot-starter-validation`
  - `springdoc-openapi-starter-webmvc-ui`
  - Lombok
- [ ] Multi-Stage `Dockerfile` (Kopie von `content-service/Dockerfile`, nur Artefakt-Name anpassen).
- [ ] Compose-Service `search-service` ergänzen, `depends_on: elasticsearch (healthy)`, Port `8081:8080`.

### 4.3 Projektstruktur `search-service`

Package-Root: `LegoCity.search_service`

```
search-service/src/main/java/LegoCity/search_service/
├── SearchServiceApplication.java
├── config/
│   ├── OpenApiConfig.java
│   └── ElasticsearchConfig.java         ← Index-Setup (Mapping, Settings)
├── document/
│   └── ArticleDocument.java             ← @Document(indexName = "articles")
├── dto/
│   ├── ArticleIndexRequest.java         ← Input für /internal/index
│   ├── ArticleSearchResponse.java       ← Output für /search
│   └── PageResponse.java
├── repository/
│   └── ArticleSearchRepository.java     ← extends ElasticsearchRepository
├── service/
│   └── ArticleSearchService.java
├── controller/
│   ├── ArticleSearchController.java     ← public: /api/v1/search
│   └── InternalIndexController.java     ← intern: /internal/search/articles
└── exception/
    ├── GlobalExceptionHandler.java
    └── ErrorResponse.java
```

### 4.4 `ArticleDocument` (Index-Mapping)

Felder, die indexiert werden (nur publish-relevant):

| Feld | ES-Typ | Hinweis |
|---|---|---|
| `id` | keyword | == Postgres-ID als String |
| `title` | text | analyzer `standard` |
| `subtitle` | text | analyzer `standard` |
| `content` | text | analyzer `standard` |
| `author` | keyword | für Aggregationen/Filter |
| `slug` | keyword | exakter Match |
| `categoryId` | keyword | Filter |
| `categoryName` | keyword | Filter/Facet |
| `tagIds` | keyword (array) | Filter |
| `tagNames` | keyword (array) | Facet |
| `publishedAt` | date | Sortierung |
| `coverImageUrl` | keyword (index=false) | nur Display |

Index-Name: `articles`. Bei Schema-Änderungen später per Alias + Reindex.

### 4.5 Endpunkte

**Öffentlich** — `/api/v1/search`

| Methode | Pfad | Beschreibung |
|---|---|---|
| GET | `/articles` | Suche; Query-Params: `q`, `categoryId`, `tagId`, `author`, `page`, `size`, `sort` (`relevance` \| `publishedAt`) |

**Intern** — `/internal/search/articles` (später per Network-Policy / Auth abgesichert)

| Methode | Pfad | Beschreibung |
|---|---|---|
| POST | `/index` | Body = `ArticleIndexRequest`, upsert ins ES (Verhalten siehe 4.5b) |
| DELETE | `/{id}` | Entfernt Dokument (idempotent, 204 auch wenn nicht existent) |

> Hinweis: `POST /reindex` ist in Phase 4 **kein** Bestandteil — er würde eine Quelle voraussetzen, die erst in Phase 5 verfügbar wird.

### 4.5a Suchlogik (Definition für `GET /api/v1/search/articles`)

- `q` sucht in `title`, `subtitle`, `content` (Multi-Match).
- `title` bekommt höheren Boost als `subtitle`, `subtitle` höher als `content` (z. B. `title^3`, `subtitle^2`, `content^1`).
- Filter (kombinierbar, AND): `categoryId`, `tagId`, `author`.
- `sort=relevance` (Default, wenn `q` gesetzt) → Sortierung nach ES `_score` desc.
- `sort=publishedAt` → `publishedAt` desc.
- `page` (default 0), `size` (default 20, max 100).
- Response: `PageResponse<ArticleSearchResponse>` (analog zum `content-service`).

### 4.5b Index-Verhalten (`POST /internal/search/articles/index`)

- `ArticleIndexRequest` enthält das Feld **`status`** (`DRAFT | PUBLISHED | ARCHIVED`).
- Wenn `status == PUBLISHED` → Dokument wird upserted.
- Wenn `status == ARCHIVED` → Dokument wird per `deleteById` entfernt (Endpoint bleibt idempotent — auch wenn das Dokument nicht existiert, `200/204`).
- Wenn `status == DRAFT` → Dokument wird **nicht** indexiert; wenn es bereits existiert, wird es entfernt (gleiche Logik wie `ARCHIVED`).
- Diese Regel sichert die Architectural Rule „nur PUBLISHED wird indexiert" auf der **Empfänger-Seite** ab, unabhängig davon, was der `content-service` sendet.

### 4.6 Konfiguration

`search-service/src/main/resources/application.properties`:

```properties
spring.application.name=search-service
server.port=8080

spring.elasticsearch.uris=http://localhost:9200
spring.elasticsearch.connection-timeout=5s
spring.elasticsearch.socket-timeout=30s

springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

Compose-Override:
```
SPRING_ELASTICSEARCH_URIS=http://elasticsearch:9200
```

### 4.7 Fehlerbehandlung & Logging

- [ ] `GlobalExceptionHandler` analog zu `content-service` (gleiches `ErrorResponse`-Schema, damit Clients einheitlich reagieren).
- [ ] ES-spezifische Exceptions (`NoSuchIndexException`, `ElasticsearchException`) gezielt mappen → `503` wenn Cluster unreachable, `404` wenn Dokument fehlt.

### 4.8 Tests

- [ ] Smoke-Test analog zu `ContentServiceApplicationTests` — Context lädt.
- [ ] Slice-Test für `ArticleSearchController` mit gemocktem Service.
- [ ] Optional: Testcontainer mit Elasticsearch für Integrations-Test (nur wenn zeitlich machbar).

### 4.9 Doku & Verifikation

- [ ] Swagger UI des `search-service` erreichbar unter `http://localhost:8081/swagger-ui.html`.
- [ ] Manuelles Verifikations-Skript / curl-Snippets in `conductor/phase-4-verify.md`:
  1. `docker compose up` → alle 3 Services healthy
  2. Dokument per `POST /internal/search/articles/index` einfügen
  3. Über `GET /api/v1/search/articles?q=...` finden
  4. `DELETE /internal/search/articles/{id}` → nicht mehr findbar

### 4.10 PLAN.md aktualisieren

- [ ] In "Bereits umgesetzt" Phase-4-Block ergänzen, wenn fertig.
- [ ] Tech-Stack-Tabelle um Elasticsearch erweitern.
- [ ] Architektur-Diagramm um `search-service` + `elasticsearch` ergänzen.

---

## Reihenfolge (Abarbeitung)

1. **4.1** Elasticsearch in Compose hochziehen, `curl http://localhost:9200` muss antworten.
2. **4.2 + 4.3** Maven-Modul + Skelett anlegen, leerer Service startet gegen ES.
3. **4.4** `ArticleDocument` + Index-Bootstrap.
4. **4.5** Intern-Endpunkte zuerst (`index`, `delete`) — ohne sie kann nichts getestet werden.
5. **4.5** Öffentlichen Such-Endpunkt darüber bauen.
6. **4.6–4.7** Konfig, Fehlerbehandlung sauberziehen.
7. **4.8** Tests.
8. **4.9** Verifikation gegen laufenden Compose-Stack.
9. **4.10** PLAN.md-Updates.

---

## Risiken / offene Fragen

- **Elasticsearch-Version**: Spring Boot 4.0.6 → kompatibel mit ES 8.x. ES 9 evtl. noch zu neu — vor Modul-Setup kurz prüfen.
- **Speicher-Bedarf**: ES braucht im Container leicht ~1 GB RAM. Für Entwicklungs-Compose `ES_JAVA_OPTS=-Xms512m -Xmx512m` setzen.
- **Security**: `xpack.security.enabled=false` ist nur für lokale Dev OK — für Produktion separates Ticket.
