# Load Tests (k6)

## Targets

Alle Lasttests laufen über den **nginx-Reverse-Proxy** (Single Entrypoint, HTTPS auf Port 443):

- **Webseite anzeigen**: `GET /` über nginx → Next.js
  - Default: `WEB_BASE_URL=https://localhost`
- **Daten verarbeitender Web-Endpunkt**: `POST /internal/search/articles/index` über nginx → `search-service`
  - Default: `DATA_BASE_URL=https://localhost`

Warum der Index-Endpunkt?
- Er verarbeitet Request-Bodies (auch groß) und antwortet klein (kein 5MB-Response).
- Er ist idempotent (wir schreiben immer dieselbe `id`) und benötigt keine Authentifizierung.

> **TLS-Hinweis:** nginx nutzt ein selbst-signiertes Zertifikat. k6 muss die Zertifikatsprüfung
> überspringen — dazu die Umgebungsvariable `K6_INSECURE_SKIP_TLS_VERIFY=true` setzen
> (in allen Beispielen unten enthalten).

## Voraussetzungen

- Docker + Docker Compose
- Optional: lokales `k6` (sonst: k6 via Docker nutzen)

## Stack starten

```powershell
docker compose up --build
```

## 5MB Payload erzeugen (einmalig)

Für die 5MB-Szenarien wird eine große JSON-Datei generiert (nicht eingecheckt).

```powershell
powershell -ExecutionPolicy Bypass -File load-tests/k6/generate-payloads.ps1
```

## Tests ausführen (k6 via Docker, empfohlen)

Wichtig (Windows/Docker Desktop): In einem Container bedeutet `localhost` **der Container selbst**.
Damit k6 deinen lokalen Compose-Stack erreicht, nutze `host.docker.internal`.

In PowerShell im Repo-Root:

```powershell
docker run --rm -v "${PWD}:/work" -w /work `
  -e K6_INSECURE_SKIP_TLS_VERIFY=true `
  -e WEB_BASE_URL=https://host.docker.internal `
  -e DATA_BASE_URL=https://host.docker.internal `
  grafana/k6 run load-tests/k6/web_page_parallel_10_0s.js
```

### Web-Endpunkt: Webseite anzeigen

- 10 parallel (0s Ramp-up)
  - `docker run --rm -v "${PWD}:/work" -w /work -e K6_INSECURE_SKIP_TLS_VERIFY=true -e WEB_BASE_URL=https://host.docker.internal grafana/k6 run load-tests/k6/web_page_parallel_10_0s.js`
- 100 parallel (1s Ramp-up)
  - `docker run --rm -v "${PWD}:/work" -w /work -e K6_INSECURE_SKIP_TLS_VERIFY=true -e WEB_BASE_URL=https://host.docker.internal grafana/k6 run load-tests/k6/web_page_parallel_100_1s.js`
- 1000 parallel (5s Ramp-up)
  - `docker run --rm -v "${PWD}:/work" -w /work -e K6_INSECURE_SKIP_TLS_VERIFY=true -e WEB_BASE_URL=https://host.docker.internal grafana/k6 run load-tests/k6/web_page_parallel_1000_5s.js`
- 1000 parallel (1s Ramp-up)
  - `docker run --rm -v "${PWD}:/work" -w /work -e K6_INSECURE_SKIP_TLS_VERIFY=true -e WEB_BASE_URL=https://host.docker.internal grafana/k6 run load-tests/k6/web_page_parallel_1000_1s.js`
- 1000 Requests/Minute über 10 Minuten
  - `docker run --rm -v "${PWD}:/work" -w /work -e K6_INSECURE_SKIP_TLS_VERIFY=true -e WEB_BASE_URL=https://host.docker.internal grafana/k6 run load-tests/k6/web_page_rate_1000rpm_10m.js`

### Daten-Endpunkt: Indexing (über nginx → `search-service`)

- 10 parallel (0s Ramp-up, normale Requests)
  - `docker run --rm -v "${PWD}:/work" -w /work -e K6_INSECURE_SKIP_TLS_VERIFY=true -e DATA_BASE_URL=https://host.docker.internal grafana/k6 run load-tests/k6/data_index_parallel_10_0s_small.js`
- 100 parallel (1s Ramp-up, normale Requests)
  - `docker run --rm -v "${PWD}:/work" -w /work -e K6_INSECURE_SKIP_TLS_VERIFY=true -e DATA_BASE_URL=https://host.docker.internal grafana/k6 run load-tests/k6/data_index_parallel_100_1s_small.js`
- 1000 parallel (5s Ramp-up, normale Requests)
  - `docker run --rm -v "${PWD}:/work" -w /work -e K6_INSECURE_SKIP_TLS_VERIFY=true -e DATA_BASE_URL=https://host.docker.internal grafana/k6 run load-tests/k6/data_index_parallel_1000_5s_small.js`
- 10 parallel (0s Ramp-up, 5MB Request Body)
  - `docker run --rm -v "${PWD}:/work" -w /work -e K6_INSECURE_SKIP_TLS_VERIFY=true -e DATA_BASE_URL=https://host.docker.internal grafana/k6 run load-tests/k6/data_index_parallel_10_0s_5mb.js`
- 100 parallel (1s Ramp-up, 5MB Request Body)
  - `docker run --rm -v "${PWD}:/work" -w /work -e K6_INSECURE_SKIP_TLS_VERIFY=true -e DATA_BASE_URL=https://host.docker.internal grafana/k6 run load-tests/k6/data_index_parallel_100_1s_5mb.js`
- 1000 parallel (5s Ramp-up, 5MB, **429 erlaubt**)
  - `docker run --rm -v "${PWD}:/work" -w /work -e K6_INSECURE_SKIP_TLS_VERIFY=true -e DATA_BASE_URL=https://host.docker.internal grafana/k6 run load-tests/k6/data_index_parallel_1000_5s_5mb_allow429.js`
- 1000 parallel (5s Ramp-up, 5MB, **muss 200 sein**)
  - `docker run --rm -v "${PWD}:/work" -w /work -e K6_INSECURE_SKIP_TLS_VERIFY=true -e DATA_BASE_URL=https://host.docker.internal grafana/k6 run load-tests/k6/data_index_parallel_1000_5s_5mb_strict.js`

## URLs überschreiben

Die Defaults zeigen auf `https://localhost`. Über `WEB_BASE_URL` / `DATA_BASE_URL`
lässt sich ein anderer Host/Port setzen (z.B. für Tests gegen eine entfernte Umgebung).

## k6 lokal (ohne Docker)

Wenn du k6 lokal installiert hast, funktionieren die Defaults (`https://localhost`) ohne extra Env-Variablen
— nur die TLS-Prüfung muss deaktiviert werden:

```powershell
$env:K6_INSECURE_SKIP_TLS_VERIFY = "true"
k6 run load-tests/k6/web_page_parallel_10_0s.js
k6 run load-tests/k6/data_index_parallel_10_0s_small.js
```
