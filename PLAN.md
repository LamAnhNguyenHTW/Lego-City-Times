# Lego City Times — Projektplan für Agenten

## Projektübersicht

**Lego City Times** ist ein Nachrichtenportal-Backend (REST API) für das fiktive Lego City Universum.
Der Dienst verwaltet Nachrichtenartikel inklusive Kategorien, Tags und Bilder.

Monorepo-Struktur — aktuell ein Microservice (`content-service`), geplante Erweiterung um einen separaten **search-service**. Der `content-service` bleibt unter diesem Namen bestehen.

---

## Architectural Rules

Verbindliche Regeln für die Zielarchitektur mit getrenntem Article- und Search-Service:

- **PostgreSQL bleibt die Source of Truth** für alle Artikeldaten.
- **Elasticsearch speichert nur durchsuchbare Projektionen** veröffentlichter Artikel.
- **Der Search Service schreibt nie nach PostgreSQL.**
- **Der Content Service exponiert Elasticsearch nie direkt.**
- **Indexierung erfolgt nach Publish-/Update-/Archive-Events** im Content Service.
- **Archivierte oder nicht veröffentlichte Artikel** müssen aus dem Elasticsearch-Index entfernt werden.
- **Suchendpunkte lesen ausschließlich aus Elasticsearch.**

---

## Aktuelle Architektur

```
Lego-City-Times/
├── content-service/          ← Content-Service (Source of Truth)
│   ├── src/main/java/LegoCity/content_service/
│   │   ├── model/            ← JPA-Entitäten
│   │   ├── dto/              ← Request/Response-Objekte
│   │   ├── repository/       ← Spring Data JPA
│   │   ├── service/          ← Business-Logik (@Transactional)
│   │   ├── controller/       ← REST-Controller
│   │   ├── exception/        ← Fehlerbehandlung
│   │   ├── config/           ← OpenAPI, WebMVC
│   │   └── DataInitializer   ← Beispieldaten beim Start
│   ├── src/test/             ← Tests laufen gegen H2
│   ├── Dockerfile            ← Multi-Stage Build (JDK 21 → JRE Alpine)
│   └── pom.xml
├── search-service/           ← Elasticsearch-basierter Suchservice
├── docker-compose.yml        ← PostgreSQL + Elasticsearch + content-service + search-service
└── PLAN.md                   ← diese Datei
```

---

## Tech Stack

| Bereich | Technologie |
|---------|-------------|
| Sprache | Java 21 |
| Framework | Spring Boot 4.0.6 (Spring Framework 7, Jakarta EE 10) |
| Web | spring-boot-starter-webmvc |
| Datenbank (Prod) | PostgreSQL 17 |
| Datenbank (Test) | H2 (in-memory) |
| Suche | Elasticsearch 9.2.8 |
| ORM | Hibernate 7 / Spring Data JPA |
| Validierung | jakarta.validation (Bean Validation) |
| Boilerplate | Lombok |
| API-Docs | springdoc-openapi 3.0.2 → Swagger UI |
| Monitoring | Spring Boot Actuator |
| Container | Docker (eclipse-temurin:21-jre-alpine) |
| Orchestrierung | Docker Compose |

---

## Datenmodell

```
Category (1) ──────< Article (N)
                        │
                        ├──>< Tag (M:N, join-table: article_tags)
                        │
                        └──>  ArticleImage (1:N, cascade ALL)

Article.status: DRAFT | PUBLISHED | ARCHIVED
```

### Wichtige Entity-Details

- `Article.slug` — URL-sicherer Bezeichner, wird automatisch aus dem Titel generiert (unique, Duplikate bekommen Suffix `-1`, `-2` …)
- `Article.viewCount` — wird bei jedem GET per ID/Slug inkrementiert (JPQL UPDATE, `@Modifying`)
- `ArticleImage.url` — relativer Pfad `/uploads/images/{uuid}.ext`, statisch serviert via `WebConfig`
- Bilder werden im Dateisystem gespeichert (`app.image.upload-dir`), Metadaten in der DB

---

## Implementierte API-Endpunkte

Basis-URL: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`

### Artikel `/api/v1/articles`

| Methode | Pfad | Beschreibung |
|---------|------|--------------|
| GET | `/` | Liste (paginated) — Filter: `status`, `categoryId`, `tagId`, `search`, `sortBy`, `sortDir` |
| GET | `/{id}` | Vollständiger Artikel (inkl. viewCount++) |
| GET | `/slug/{slug}` | Artikel per URL-Slug |
| POST | `/` | Erstellen → Status DRAFT |
| PUT | `/{id}` | Vollständig aktualisieren |
| DELETE | `/{id}` | Löschen (löscht auch Bilddateien) |
| PATCH | `/{id}/publish` | Status → PUBLISHED, setzt publishedAt |
| PATCH | `/{id}/archive` | Status → ARCHIVED |

### Kategorien `/api/v1/categories`

| Methode | Pfad | Beschreibung |
|---------|------|--------------|
| GET | `/` | Alle Kategorien |
| GET | `/{id}` | Einzelne Kategorie |
| POST | `/` | Erstellen |
| PUT | `/{id}` | Aktualisieren |
| DELETE | `/{id}` | Löschen (schlägt fehl wenn Artikel zugeordnet) |

### Tags `/api/v1/tags`

| Methode | Pfad | Beschreibung |
|---------|------|--------------|
| GET | `/` | Alle Tags |
| GET | `/{id}` | Einzelner Tag |
| POST | `/` | Erstellen |
| PUT | `/{id}` | Aktualisieren |
| DELETE | `/{id}` | Löschen (schlägt fehl wenn Artikel zugeordnet) |

### Bilder `/api/v1/images`

| Methode | Pfad | Beschreibung |
|---------|------|--------------|
| POST | `/upload` | Multipart-Upload (param: `file`, opt. `articleId`, `altText`, `caption`) |
| GET | `/{id}` | Metadaten |
| GET | `/{id}/data` | Bilddatei (inline) |
| GET | `/article/{articleId}` | Alle Bilder eines Artikels |
| PATCH | `/{imageId}/attach/{articleId}` | Bild nachträglich einem Artikel zuordnen |
| DELETE | `/{id}` | Löscht DB-Eintrag + Datei |

Statisches Serving: `GET /uploads/images/{filename}` (kein DB-Lookup)

---

## Konfiguration

### `application.properties` (Prod — PostgreSQL)

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/legocitytimes
spring.datasource.username=legocity
spring.datasource.password=legocity
spring.jpa.hibernate.ddl-auto=update
app.image.upload-dir=uploads/images
app.image.max-size=10485760
app.image.allowed-types=image/jpeg,image/png,image/webp,image/gif
```

### `src/test/resources/application.properties` (Test — H2)

Tests laufen vollständig ohne laufendes PostgreSQL.

### Docker Compose Umgebungsvariablen (überschreiben application.properties)

```
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/legocitytimes
SPRING_DATASOURCE_USERNAME=legocity
SPRING_DATASOURCE_PASSWORD=legocity
APP_IMAGE_UPLOAD_DIR=/app/uploads/images
```

---

## Projekt starten

### Lokal mit Maven (PostgreSQL muss laufen)

```bash
# PostgreSQL starten
docker compose up postgres -d

# App starten
cd content-service
./mvnw spring-boot:run
```

### Vollständig mit Docker Compose

```bash
docker compose up --build
```

### Nur Tests

```bash
cd content-service
./mvnw test
```

---

## Coding-Konventionen

- **Packages**: `LegoCity.content_service.*` (entspricht Projekt-Namespace)
- **Namespaces**: Jakarta EE 10 (`jakarta.*`, nicht `javax.*`)
- **Transaktionen**: Service-Methoden sind `@Transactional` oder `@Transactional(readOnly = true)` — nie in Controllern
- **Fehler**: Alle Exceptions werden in `GlobalExceptionHandler` abgefangen → einheitliches `ErrorResponse`-Format
- **DTOs**: Entities werden nie direkt serialisiert — immer über DTOs mappen (kein Circular-Reference-Problem)
- **Slug-Generierung**: Automatisch aus Titel, Sonderzeichen entfernt, Leerzeichen → `-`, Duplikate → `-1`, `-2` …
- **Lombok**: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` auf Entities (kein `@Data` wegen JPA-Pitfalls); `@Data` nur auf DTOs
- **Tests**: Laufen gegen H2 (`src/test/resources/application.properties`)

---

## Bereits umgesetzt (Stand 2026-05-18)

Status-Snapshot des aktuellen `content-service` plus Phase-4-Erweiterung:

### Phase 4: Search Service mit Elasticsearch
- [x] `search-service` als eigenes Spring-Boot-Modul angelegt
- [x] Elasticsearch in `docker-compose.yml` eingebunden (`single-node`, lokaler Port `9200`, Volume `esdata`)
- [x] Compose-Service `search-service` mit Port `8081:8080` und ES-Dependency ergänzt
- [x] `ArticleDocument`-Mapping für den Index `articles`
- [x] Interne Endpunkte: `POST /internal/search/articles/index`, `DELETE /internal/search/articles/{id}`
- [x] Öffentlicher Suchendpunkt: `GET /api/v1/search/articles`
- [x] Suche mit Multi-Match (`title^3`, `subtitle^2`, `content`), Filtern, Pagination und Sortierung
- [x] Empfänger-seitige Regel: nur `PUBLISHED` wird indexiert, `DRAFT`/`ARCHIVED` löschen das Dokument
- [x] Swagger/OpenAPI und Actuator-Konfiguration
- [x] Fehlerbehandlung im Schema des `content-service`
- [x] Verifikationsanleitung: [phase-4-verify.md](conductor/phase-4-verify.md)

### Infrastruktur & Build
- [x] Maven-Projekt mit Spring Boot 4.0.6 (Java 21, Jakarta EE 10)
- [x] Multi-Stage `Dockerfile` (JDK 21 build → JRE Alpine runtime)
- [x] `docker-compose.yml` mit PostgreSQL 17 + content-service + Healthchecks
- [x] Persistente Volumes für `pgdata` und `uploads`
- [x] Actuator-Endpunkte (Healthcheck im Compose verdrahtet)
- [x] OpenAPI / Swagger UI unter `/swagger-ui.html`

### Datenmodell ([model/](content-service/src/main/java/LegoCity/content_service/model/))
- [x] `Article` mit Status `DRAFT | PUBLISHED | ARCHIVED`, Slug, viewCount, Timestamps
- [x] `Category` (1:N zu Article)
- [x] `Tag` (M:N zu Article via `article_tags`)
- [x] `ArticleImage` (1:N zu Article, cascade ALL)
- [x] `ArticleStatus`-Enum

### Repositories ([repository/](content-service/src/main/java/LegoCity/content_service/repository/))
- [x] `ArticleRepository` — Filter nach `status`, `categoryId`, `tagId`, `search`, Slug-Uniqueness, `incrementViewCount` per `@Modifying`
- [x] `CategoryRepository`, `TagRepository`, `ArticleImageRepository`

### Services ([service/](content-service/src/main/java/LegoCity/content_service/service/))
- [x] [ArticleService.java](content-service/src/main/java/LegoCity/content_service/service/ArticleService.java) — CRUD, Publish, Archive, Slug-Generierung mit Suffix-Logik, viewCount-Inkrement
- [x] `CategoryService`, `TagService` (mit Lösch-Schutz bei zugeordneten Artikeln)
- [x] `ImageService` — Multipart-Upload, MIME-Validierung, Dateisystem-Speicherung, Löschen inkl. Datei

### Controller ([controller/](content-service/src/main/java/LegoCity/content_service/controller/))
- [x] `ArticleController` — alle in der API-Tabelle gelisteten Endpunkte
- [x] `CategoryController`, `TagController`, `ImageController`

### DTOs ([dto/](content-service/src/main/java/LegoCity/content_service/dto/))
- [x] Request/Response-DTOs für Article (Voll- + Summary-Variante), Category, Tag, Image
- [x] Generisches `PageResponse<T>`

### Fehlerbehandlung ([exception/](content-service/src/main/java/LegoCity/content_service/exception/))
- [x] `GlobalExceptionHandler` mit einheitlichem `ErrorResponse`-Format
- [x] `ResourceNotFoundException`, `BadRequestException`

### Konfiguration ([config/](content-service/src/main/java/LegoCity/content_service/config/))
- [x] `OpenApiConfig` — Swagger-Setup
- [x] `WebConfig` — statisches Serving von `/uploads/images/**`
- [x] `application.properties` (Prod-Profil PostgreSQL) + Test-Properties (H2)
- [x] Image-Limits konfigurierbar (`app.image.max-size`, `app.image.allowed-types`)

### Daten & Tests
- [x] [DataInitializer.java](content-service/src/main/java/LegoCity/content_service/DataInitializer.java) — Beispieldaten beim Start (mit `count() > 0` Guard)
- [x] `ContentServiceApplicationTests` — Smoke-Test (Context lädt gegen H2)

### Noch offen aus Architectural Rules
- [ ] **Sync-Hooks** (publish/update/archive → Search Service) fehlen
- [ ] **Flyway** noch nicht eingebunden (`ddl-auto=update`)
- [ ] **Authentifizierung / JWT** fehlt komplett

---

## Geplante Services (MVP)

Für das MVP sind zwei Services vorgesehen: Der bestehende `content-service` bleibt unter diesem Namen und übernimmt die Rolle des Content Service (Source of Truth). Der `search-service` kommt neu hinzu.

### 1. Content Service (`content-service`)

Zuständig für die gesamte Artikelverwaltung (Source of Truth).

**Verantwortlich für:**
- Artikel erstellen
- Artikel bearbeiten
- Artikel veröffentlichen
- Artikel archivieren
- Artikel nach ID abrufen
- Artikel listen
- Drafts verwalten

**Tech:**
- Spring Boot
- PostgreSQL
- Flyway
- JPA / Hibernate
- Actuator Health

### 2. Search Service

Zuständig ausschließlich für die Suche über veröffentlichte Artikel.

**Verantwortlich für:**
- Artikel suchen
- Artikel indexieren
- Artikel aus Index entfernen
- Filter und Pagination
- später: Autocomplete

**Tech:**
- Spring Boot
- Elasticsearch
- Actuator Health

---

## Synchronisation Content Service → Search Service

**Publish:**
1. Content Service setzt `status = PUBLISHED` in PostgreSQL.
2. Search Service indexiert den Artikel in Elasticsearch.

**Archive:**
1. Content Service setzt `status = ARCHIVED` in PostgreSQL.
2. Search Service löscht den Artikel aus Elasticsearch.

**Update (eines veröffentlichten Artikels):**
1. Content Service aktualisiert den Artikel in PostgreSQL.
2. Search Service aktualisiert das entsprechende Elasticsearch-Dokument.

### Kommunikationsmodell

- **MVP:** Direkter interner REST-Call vom Content Service an den Search Service.
  - `POST /internal/search/articles/index`
  - `DELETE /internal/search/articles/{id}`
- **Später:** Outbox Pattern bzw. Kafka / RabbitMQ — `article_events`-Tabelle wird vom Search Service konsumiert, der dann Elasticsearch aktualisiert.

---

## Roadmap (Phasen)

| Phase | Inhalt |
|---|---|
| **Phase 1** | Foundation: Docker Compose, PostgreSQL, Monorepo-Setup |
| **Phase 2** | Content Service: CRUD, Draft, Publish, Archive |
| **Phase 3** | Public Article Endpoints (Listen, Detail, per Slug) |
| **Phase 4** | Search Service mit Elasticsearch (eigener Service, eigenes Image) |
| **Phase 5** | Synchronisation Content Service → Search Service (MVP: interner REST-Call) |
| **Phase 6** | Monitoring, Health Checks, Testing |

Elasticsearch wird **nicht** als spätere optionale Notiz behandelt, sondern als eigene Phase mit eigenem Service eingeführt — **nachdem** der Content Service stabiles Publish-/Archive-Verhalten zeigt.

---

## Offene Aufgaben / mögliche Erweiterungen

### Kurzfristig
- [ ] **Sync-Logik** im Content Service: REST-Calls an Search Service bei publish/update/archive
- [ ] **Authentifizierung** — Spring Security + JWT (Redakteure vs. Leser)
- [ ] **Kommentare** — `Comment`-Entity mit Artikel-Zuordnung, Moderation-Status
- [ ] **Weitere Tests** — Controller-Tests (`@WebMvcTest`), Service-Unit-Tests

### Mittelfristig
- [ ] **Outbox Pattern / Kafka / RabbitMQ** — Ersatz für direkten REST-Call zwischen Article- und Search-Service
- [ ] **Autocomplete** im Search Service
- [ ] **user-service** — Nutzerverwaltung als separater Microservice
- [ ] **notification-service** — Push/E-Mail bei Veröffentlichung
- [ ] **CDN/S3-Integration** — Bildablage in AWS S3 oder MinIO statt lokalem Dateisystem
- [ ] **Caching** — Redis-Cache für häufig abgerufene Artikel
- [ ] **Rate Limiting** — Schutz der Upload-Endpunkte

### Technisch
- [ ] **Flyway/Liquibase** — Datenbankmigrationen statt `ddl-auto=update`
- [ ] **Pagination-Links** — HATEOAS-Links in `PageResponse`
- [ ] **Logging** — strukturiertes JSON-Logging (Logback + Logstash-Encoder)
- [ ] **CI/CD** — GitHub Actions Pipeline (test → build → push image)

---

## Bekannte Entscheidungen & Begründungen

| Entscheidung | Begründung |
|---|---|
| H2 nur für Tests | Tests sind unabhängig von einer laufenden DB; Prod nutzt PostgreSQL |
| `ddl-auto=update` | Einfacher Start; für Produktion durch Flyway ersetzen |
| Bilder im Dateisystem | Einfachste funktionierende Lösung; später durch S3 ersetzbar |
| `@Transactional` in Services, nicht Controllern | Standard-Architektur; hält Controller schlank |
| `DataInitializer` mit `count() > 0` Guard | Verhindert Fehler beim Neustart wenn DB bereits Daten hat |
| Multi-Stage Docker Build | Image-Größe ~200 MB statt ~600 MB; Build-Tools nicht im Runtime-Image |
| PostgreSQL = Source of Truth, Elasticsearch = nur Suche | Saubere Trennung der Verantwortlichkeiten; Search-Index kann jederzeit neu aufgebaut werden |
| Content Service ↔ Search Service via internem REST-Call (MVP) | Einfachste funktionierende Lösung ohne Broker; später durch Outbox/Kafka ersetzbar |
| Eigener Search Service statt PostgreSQL `tsvector` | Realistische Microservice-Architektur; ermöglicht Skalierung und spätere Features (Autocomplete, Relevanz-Tuning) |
