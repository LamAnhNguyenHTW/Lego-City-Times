# Lego City Times — Projektplan für Agenten

## Projektübersicht

**Lego City Times** ist ein Nachrichtenportal-Backend (REST API) für das fiktive Lego City Universum.
Der Dienst verwaltet Nachrichtenartikel inklusive Kategorien, Tags und Bilder.

Monorepo-Struktur — aktuell ein Microservice, Erweiterung auf weitere Services geplant.

---

## Aktuelle Architektur

```
Lego-City-Times/
├── content-service/          ← einziger aktiver Service
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
├── docker-compose.yml        ← PostgreSQL + content-service
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

## Offene Aufgaben / mögliche Erweiterungen

### Kurzfristig
- [ ] **Authentifizierung** — Spring Security + JWT (Redakteure vs. Leser)
- [ ] **Kommentare** — `Comment`-Entity mit Artikel-Zuordnung, Moderation-Status
- [ ] **Volltext-Suche** — aktuell per LIKE-Query; Ersatz durch PostgreSQL `tsvector` oder Elasticsearch
- [ ] **Weitere Tests** — Controller-Tests (`@WebMvcTest`), Service-Unit-Tests

### Mittelfristig
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
