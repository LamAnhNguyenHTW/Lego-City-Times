# Lego City Times

Eine Microservice-basierte Demo-Anwendung für ein Online-Nachrichtenportal. Die Infrastruktur ist vollständig containerisiert und enthält Anwendung, Datenbank, Suche, Reverse Proxy und Monitoring.

## Architektur

| Service | Beschreibung | Interner Port |
|---|---|---|
| `frontend` | Next.js Web-UI | 3000 |
| `content-service` | Spring Boot REST API (Artikel, Bilder, Auth) | 8080 |
| `search-service` | Spring Boot REST API (Volltextsuche via Elasticsearch) | 8080 |
| `postgres` | Persistenz für Artikel/User | 5432 |
| `elasticsearch` | Suchindex | 9200 |
| `nginx` | Reverse Proxy / Single Entry Point | 80 |
| `prometheus` | Metriken-Server | 9090 |
| `alertmanager` | Alarmierung | 9093 |
| `grafana` | Dashboards | 3000 |
| `cadvisor` | Container-Metriken | — |
| `postgres-exporter` | DB-Metriken für Prometheus | — |
| `nginx-exporter` | Nginx-Metriken für Prometheus | — |

## Voraussetzungen

- Docker Desktop (oder Docker Engine + Docker Compose Plugin)
- Mind. 4 GB freier RAM (Elasticsearch + Postgres + Java-Services)
- Freie Host-Ports: `80`, `443` (Nginx) und `3000` (Grafana). Alle übrigen Dienste (Prometheus, Alertmanager, Elasticsearch, …) sind nur intern erreichbar.

## Start

```powershell
Copy-Item .env.example .env
# Danach SLACK_API_URL in .env mit der echten Slack Webhook URL setzen.
.\scripts\up.ps1 -Build
```

Beim ersten Start werden die Images gebaut, Volumes angelegt und Datenbank-Tabellen sowie Elasticsearch-Index **automatisch** erzeugt (JPA `ddl-auto` + Spring Data Elasticsearch Mapping). Keine manuellen Initialisierungs-Schritte nötig.

Alertmanager wird vor dem Start aus `monitoring/alertmanager/alertmanager.yml.tmpl` nach `monitoring/alertmanager/alertmanager.generated.yml` gerendert. Die Slack Webhook URL kommt aus `SLACK_API_URL` in `.env`; die generierte Datei bleibt lokal und wird nicht eingecheckt.

Status prüfen:

```bash
docker compose ps
```

Alle Services sollten nach ca. 30–60 s `healthy` melden.

## URLs

> nginx terminiert TLS mit einem selbst-signierten Zertifikat. Aufrufe über `http://localhost/`
> werden automatisch auf `https://localhost/` umgeleitet; der Browser zeigt eine Zertifikatswarnung.
> Für `curl` die Option `-k` verwenden.

| Zweck | URL |
|---|---|
| **Webanwendung (Frontend)** | https://localhost/ |
| **Daten verarbeitender Web-Endpunkt** | `POST https://localhost/internal/search/articles/index` |
| **Health-Check (Content-Service)** | https://localhost/actuator/health |
| **Health-Check (Search-Service)** | intern via Docker-Healthcheck + Prometheus-Scrape (kein Host-Port) |
| Such-API | `GET https://localhost/api/v1/search/articles?q=...` |
| Artikel-API (Anlegen, **ADMIN-Auth nötig**) | `POST https://localhost/api/v1/articles` |
| Swagger UI (Content) | https://localhost/swagger-ui/index.html |
| Statische Uploads | https://localhost/uploads/... |
| Grafana | http://localhost:3000 (`admin` / Passwort aus `secrets/grafana_password.txt`) |
| Prometheus | nur intern (`monitoring-net`) — Visualisierung über Grafana |
| Alertmanager | nur intern (`monitoring-net`) |

## API-Endpunkte (Auswahl)

### Artikel
- `GET    /api/v1/articles` — Liste
- `GET    /api/v1/articles/{id}` — Detail
- `GET    /api/v1/articles/slug/{slug}` — Detail per Slug
- `POST   /api/v1/articles` — Anlegen
- `PUT    /api/v1/articles/{id}` — Ändern
- `DELETE /api/v1/articles/{id}` — Löschen

### Suche
- `GET /api/v1/search/articles?q=lego&page=0&size=10`

### Weitere
- `/api/v1/categories`, `/api/v1/tags`, `/api/v1/images`
- `/api/v1/auth/register`, `/api/v1/auth/login`

## Beispiel-Requests

Daten senden (Such-Index-Endpunkt — keine Authentifizierung nötig):

```bash
curl -k -X POST https://localhost/internal/search/articles/index \
  -H "Content-Type: application/json" \
  -d '{
    "id": "demo-1",
    "title": "Neues Stadion in Lego City",
    "slug": "neues-stadion",
    "content": "In Lego City wurde heute...",
    "author": "Redaktion",
    "status": "PUBLISHED",
    "publishedAt": "2026-05-22T00:00:00Z"
  }'
```

Artikel suchen:

```bash
curl -k "https://localhost/api/v1/search/articles?q=stadion"
```

Health-Status:

```bash
curl -k https://localhost/actuator/health
```

## Persistenz (Volumes)

Folgende Daten überleben einen `docker compose down`:

| Volume | Inhalt |
|---|---|
| `legocitytimes-pgdata` | PostgreSQL-Daten |
| `legocitytimes-esdata` | Elasticsearch-Index |
| `legocitytimes-uploads` | Hochgeladene Bilder |
| `legocitytimes-promdata` | Prometheus-Metriken |
| `legocitytimes-alertdata` | Alertmanager-State |
| `legocitytimes-grafanadata` | Grafana-Dashboards/User |

Volumes prüfen:

```bash
docker volume ls | grep legocitytimes
```

## Stoppen / Cleanup

```bash
# Stoppen, Volumes bleiben erhalten
docker compose down

# Komplett entfernen (inkl. persistenter Daten)
docker compose down -v
```

## Monitoring

- **Grafana** zeigt das vorkonfigurierte Dashboard "Lego City Times" mit Container-CPU/Memory, HTTP-Raten, Latenz und DB-Verbindungen.
- **Prometheus** scrapt `content-service`, `search-service`, `nginx-exporter`, `postgres-exporter` und `cadvisor`.
- **Alert-Rules** in [monitoring/prometheus/alert_rules.yml](monitoring/prometheus/alert_rules.yml) decken CPU, Memory, HTTP 5xx, p95-Latenz, DB-Verbindungen und Service-Ausfälle ab.

## Lasttests

Lasttest-Skripte (k6) und dokumentierte Ergebnisse: siehe [load-tests/k6/README.md](load-tests/k6/README.md).
