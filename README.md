# Lego City Times

Eine Microservice-basierte Demo-Anwendung für ein Online-Nachrichtenportal. Die Infrastruktur ist vollständig containerisiert und enthält Anwendung, Datenbank, Suche, Reverse Proxy und Monitoring.

Wenn das Projekt als ZIP Datei übergeben wurde, sind bereits alle .env und secrets im Projekt und es muss nur:
```
docker compose up
```
geschrieben werden. 

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

- **Docker Desktop** (Windows/Mac) oder **Docker Engine + Compose Plugin** (Linux)
- **Mind. 6 GB freier RAM** — Elasticsearch allein reserviert 2 GB Heap, dazu zwei Java-Services und Monitoring
- **Freie Ports:** `80` und `443` (nginx), `3000` (Grafana)
- Alle anderen Services (Prometheus, Alertmanager, Elasticsearch, …) sind nur intern erreichbar und benötigen keinen Host-Port

---

## Setup

### 1. Repository klonen

```bash
git clone <repo-url>
cd Lego-City-Times
```

### 2. Umgebungsvariablen konfigurieren

`.env.example` als Vorlage kopieren und anpassen:

```bash
# Windows (PowerShell)
Copy-Item .env.example .env

# Linux / Mac
cp .env.example .env
```

Danach `.env` öffnen und die Werte setzen:

```env
DB_PASSWORD=change-me           # Datenbankpasswort (beliebig wählen)
SLACK_API_URL=https://hooks.slack.com/services/XXX/YYY/ZZZ  # Slack Webhook für Alerts
```

### 3. Secrets anlegen

Die Anwendung liest sensible Werte aus Dateien im Ordner `secrets/` — diese werden als Docker Secrets in die Container gemountet und erscheinen nie als Klartext in der Compose-Konfiguration.

```bash
# Windows (PowerShell)
"dein-db-passwort"        | Out-File -Encoding ascii secrets/postgres_password.txt
"dein-jwt-secret-schluessel" | Out-File -Encoding ascii secrets/jwt_secret.txt
"dein-grafana-passwort"   | Out-File -Encoding ascii secrets/grafana_password.txt

# Linux / Mac
echo -n "dein-db-passwort"           > secrets/postgres_password.txt
echo -n "dein-jwt-secret-schluessel" > secrets/jwt_secret.txt
echo -n "dein-grafana-passwort"      > secrets/grafana_password.txt
```

> Die Dateien in `secrets/` sind in `.gitignore` eingetragen und werden nicht eingecheckt.

### 4. SSL-Zertifikat erstellen (selbst-signiert für lokale Entwicklung)

nginx erwartet ein Zertifikat unter `monitoring/nginx/certs/`:

```bash
# Linux / Mac / Git Bash
mkdir -p monitoring/nginx/certs
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout monitoring/nginx/certs/server.key \
  -out monitoring/nginx/certs/server.crt \
  -subj "/CN=localhost"

# Windows (PowerShell) — erfordert OpenSSL installiert
New-Item -ItemType Directory -Force monitoring/nginx/certs
openssl req -x509 -nodes -days 365 -newkey rsa:2048 `
  -keyout monitoring/nginx/certs/server.key `
  -out monitoring/nginx/certs/server.crt `
  -subj "/CN=localhost"
```

### 5. Projekt starten

**Erster Start** (baut alle Docker Images):

```powershell
# Windows
.\scripts\up.ps1 -Build

# Linux / Mac
./scripts/up.sh --build
```

**Folgestarts** (ohne Rebuild, wenn sich nichts geändert hat):

```powershell
# Windows
.\scripts\up.ps1

# Linux / Mac
./scripts/up.sh
```

Das Skript führt automatisch folgendes aus:
1. Alertmanager-Konfiguration aus `.env` generieren (`alertmanager.generated.yml`)
2. Alle Container starten

Beim ersten Start werden Datenbank-Tabellen und der Elasticsearch-Index **automatisch** erstellt — keine manuellen Schritte nötig. Die Datenbank wird mit Beispieldaten befüllt (3 Artikel, Kategorien, Tags, Admin-User).

### 6. Status prüfen

```bash
docker compose ps
```

Nach ca. **30–60 Sekunden** sollten alle Services `healthy` melden. Elasticsearch braucht am längsten (~30 s).

```bash
# Logs eines bestimmten Services anschauen
docker compose logs content-service -f
docker compose logs elasticsearch -f
```

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
# Stoppen — Volumes bleiben erhalten, Daten gehen nicht verloren
docker compose down

# Komplett entfernen inkl. aller persistenter Daten (Datenbank, Uploads, Metriken)
docker compose down -v

# Einzelnen Service neu starten
docker compose restart content-service

# Images neu bauen (nach Code-Änderungen)
.\scripts\up.ps1 -Build   # Windows
./scripts/up.sh --build   # Linux / Mac
```

## Troubleshooting

**Elasticsearch startet nicht / bleibt unhealthy**

Elasticsearch benötigt mind. 2 GB RAM. Unter Windows/Mac in Docker Desktop unter *Settings → Resources* den Arbeitsspeicher auf mind. 6 GB erhöhen.

```bash
# Logs prüfen
docker compose logs elasticsearch
```

**content-service startet nicht — Datenbankfehler**

PostgreSQL braucht beim ersten Start etwas länger. Der content-service startet automatisch neu sobald die DB healthy ist. Warten und prüfen:

```bash
docker compose ps postgres
docker compose logs content-service --tail 20
```

**Port 80 oder 443 bereits belegt**

Ein anderer Prozess belegt den Port. Entweder den Prozess stoppen oder in `docker-compose.yml` die Ports anpassen (z.B. `"8080:80"`).

**Browser zeigt Zertifikatswarnung**

Das ist normal bei selbst-signierten Zertifikaten. Im Browser "Trotzdem fortfahren" wählen. Für `curl` die Option `-k` verwenden:

```bash
curl -k https://localhost/actuator/health
```

## Monitoring

- **Grafana** zeigt das vorkonfigurierte Dashboard "Lego City Times" mit Container-CPU/Memory, HTTP-Raten, Latenz und DB-Verbindungen.
- **Prometheus** scrapt `content-service`, `search-service`, `nginx-exporter`, `postgres-exporter` und `cadvisor`.
- **Alert-Rules** in [monitoring/prometheus/alert_rules.yml](monitoring/prometheus/alert_rules.yml) decken CPU, Memory, HTTP 5xx, p95-Latenz, DB-Verbindungen und Service-Ausfälle ab.

## Lasttests

Lasttest-Skripte (k6) und dokumentierte Ergebnisse: siehe [load-tests/k6/README.md](load-tests/k6/README.md).
