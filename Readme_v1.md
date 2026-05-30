# Lego City Times - Projektstart

Diese Datei beschreibt die Voraussetzungen und die Schritte, um das gesamte Projekt lokal auszufuehren.

## Systemanforderungen

- 64-bit Betriebssystem (Windows 10/11, macOS, oder Linux)
- Mindestens 4 CPU-Kerne empfohlen
- Mindestens 8 GB RAM empfohlen (Elasticsearch + Java-Services)
- Mindestens 10 GB freier Speicher fuer Docker-Images und Volumes
- Freie Host-Ports: 80, 443, 3000

## Installationen

- Docker Desktop (Windows/macOS) oder Docker Engine mit Docker Compose Plugin (Linux)
  - Docker Compose v2 wird vorausgesetzt (Befehl: `docker compose`)
- Optional: Git (wenn das Repository erst geklont werden soll)

## Projektstruktur (relevant fuer den Start)

- Docker-Compose Setup: `docker-compose.yml`
- Startskripte:
  - Windows: `scripts/up.ps1`
  - Linux/macOS: `scripts/up.sh`
- Umgebungsdatei: `.env` (aus `.env.example`)
- Secrets (lokal, bereits im Repo): `secrets/`

## Vorbereitung

1) `.env` erzeugen

```powershell
Copy-Item .env.example .env
```

2) Slack Webhook eintragen (optional, fuer Alertmanager)

- In `.env` den Wert `SLACK_API_URL` setzen.
- Ohne Slack-Webhook laeuft das System weiterhin, allerdings ohne Slack-Benachrichtigungen.

## Start (Windows)

```powershell
# Images bauen und Container starten
.\scripts\up.ps1 -Build
```

## Start (Linux/macOS)

```bash
chmod +x ./scripts/up.sh
./scripts/up.sh --build
```

Beim ersten Start werden Images gebaut, Volumes angelegt und Datenbanktabellen sowie Elasticsearch-Index automatisch erzeugt. Es sind keine manuellen Initialisierungsschritte erforderlich.

## Status pruefen

```bash
docker compose ps
```

Alle Services sollten nach ca. 30 bis 60 Sekunden `healthy` melden.

## Wichtige URLs

Hinweis: Nginx verwendet ein selbstsigniertes Zertifikat. Browser zeigen eine Warnung, `curl` benoetigt ggf. `-k`.

- Webanwendung: https://localhost/
- Swagger UI (Content-Service): https://localhost/swagger-ui/index.html
- Such-API: https://localhost/api/v1/search/articles?q=...
- Health-Check Content-Service: https://localhost/actuator/health
- Grafana: http://localhost:3000 (User `admin`, Passwort aus `secrets/grafana_password.txt`)

## Stoppen und Cleanup

```bash
# Stoppen, Volumes bleiben erhalten
docker compose down

# Komplettes Cleanup (inklusive Daten)
docker compose down -v
```

## Fehlerbehebung (Kurz)

- Port-Konflikte: Stelle sicher, dass 80, 443 und 3000 frei sind.
- Docker-Ressourcen: Bei Startproblemen in Docker Desktop mehr RAM/CPU zuweisen.
- Zertifikatswarnung im Browser ist erwartetes Verhalten.
