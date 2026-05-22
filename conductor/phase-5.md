# Phase 5 — README, Frontend-Fehlerhandling & Lasttests

## Ziel

Drei der offenen Bewertungskriterien abdecken:

1. **README.md** als vollständige Kurzdokumentation (1 P)
2. **Frontend-Fehlermeldungen** bei Ausfall von Backend / Datenbank (Teil von "Verfügbarkeit", 2 P)
3. **Lasttests** für Web- und Daten-Endpunkt mit dokumentierten Ergebnissen (12 P)

**Abgeschlossen, wenn:**
- `README.md` beschreibt Setup, Start, URLs, API-Endpunkte und Health-URL.
- Frontend zeigt klare Fehler-UI wenn `content-service` oder `search-service` nicht antworten.
- Lasttest-Skripte (k6 oder JMeter) liegen im Repo, lassen sich reproduzierbar ausführen.
- Lasttest-Ergebnisse sind als Markdown/Screenshots in `loadtests/results/` dokumentiert.

### Out of Scope (kommt von Kollegen)
- SSL/TLS, Docker Secrets, Network-Isolation, WAF, Scan, Backup
- Horizontale Skalierung (Replicas)
- Alertmanager-Empfänger (Slack/Mail)

---

## Aufgabe 1 — README.md

### Pflichtinhalte (aus bewertungskriterien.md)
- **Bedienung** der Anwendung
- **URL** zum Aufruf der Webanwendung (`http://localhost/`)
- **Endpunkt** zum Senden von Daten (z.B. `POST /api/v1/articles`)
- **Health-URL** (`http://localhost/actuator/health`, ggf. Search-Service-Health)

### Struktur
1. Projektbeschreibung (1 Absatz)
2. Architektur-Übersicht (kurze Liste der Services + Ports)
3. Voraussetzungen (Docker, Docker Compose)
4. Start: `docker compose up -d --build`
5. URLs:
   - Frontend: `http://localhost/`
   - Content-API: `http://localhost/api/v1/articles`
   - Search-API: `http://localhost/api/v1/search`
   - Health: `http://localhost/actuator/health`
   - Grafana: `http://localhost:3000` (admin/admin)
   - Prometheus: `http://localhost:9090`
6. Initialisierung (läuft automatisch via JPA `ddl-auto` + ES-Index-Mapping)
7. Beispiel-Requests (curl) für Anlegen / Suchen / Abrufen
8. Volumes & Persistenz
9. Stop / Cleanup

---

## Aufgabe 2 — Frontend-Fehlerhandling

### Status quo
- Frontend ruft direkt `fetch()` in [page.tsx](../frontend/src/app/page.tsx), [admin/page.tsx](../frontend/src/app/admin/page.tsx), [article/[slug]/page.tsx](../frontend/src/app/article/[slug]/page.tsx)
- Keine sichtbare Fehler-UI bei Backend-Ausfall (vermutlich Default-Next.js-Error oder weiße Seite)

### Anforderungen aus bewertungskriterien.md
- "Bei Ausfall des Backend zeigt das Frontend eine passende Fehlermeldung."
- "Bei Ausfall der Datenbank zeigt das Frontend eine passende Fehlermeldung."

### Umsetzung
1. **Zentrale Fetch-Wrapper-Funktion** `src/lib/api.ts`
   - Behandelt: Netzwerk-Fehler, HTTP 5xx, Timeout (z.B. 5s `AbortController`)
   - Wirft strukturierte `ApiError` mit `kind: "network" | "server" | "timeout" | "notfound"`
2. **Error-UI-Komponente** `src/components/ErrorBanner.tsx`
   - Zeigt freundlichen Text: "Die Nachrichten können gerade nicht geladen werden. Bitte später erneut versuchen."
   - Optional: Retry-Button
3. **Next.js `error.tsx`** in `app/` und `app/article/[slug]/`
   - Catch-all für Server-Component-Errors
4. **Loading-States** mit `loading.tsx` für sauberes UX
5. **Manuelle Verifikation:**
   - `docker compose stop content-service` → Startseite zeigt Banner
   - `docker compose stop postgres` → Backend liefert 5xx → Frontend zeigt Banner
   - `docker compose stop search-service` → Suche zeigt Banner, Rest funktioniert

### Test-Matrix
| Szenario | erwartetes Frontend-Verhalten |
|---|---|
| `content-service` down | Startseite + Artikel-Detail: Fehler-Banner |
| `search-service` down | Suchergebnisse: Fehler-Banner, Rest okay |
| `postgres` down | 5xx von content-service → Fehler-Banner |
| `elasticsearch` down | Suche: Fehler-Banner |
| Alles ok | Normale UI |

---

## Aufgabe 3 — Lasttests

### Tool-Wahl: **k6**
- Gründe: Skripte sind JavaScript (lesbar), Docker-Image vorhanden, gute CSV/JSON-Reports, einfache Ramp-Up-Definition.
- Alternative wäre JMeter (XML-Pläne) — aufwendiger in Versionierung.

### Verzeichnis-Struktur
```
loadtests/
├── README.md              # Anleitung zum Ausführen
├── scripts/
│   ├── web-endpoint.js    # GET / (Frontend)
│   ├── api-normal.js      # POST /api/v1/articles (kleine Bodies)
│   └── api-bigbody.js     # POST /api/v1/articles (5 MB Body)
└── results/
    ├── web-10.md          # je ein Markdown mit Stats + Screenshot Grafana
    ├── web-100.md
    ├── web-1000-5s.md
    ├── web-1000-1s.md
    ├── web-sustained.md
    ├── api-normal-10.md
    ├── api-normal-100.md
    ├── api-normal-1000.md
    ├── api-big-10.md
    ├── api-big-100.md
    ├── api-big-1000-reject.md
    └── api-big-1000-ok.md
```

### Szenarien (aus bewertungskriterien.md)

**Web-Endpunkt (GET `http://localhost/`):**
| # | VUs / Rate | Ramp-up | Dauer |
|---|---|---|---|
| W1 | 10 parallel | 0 s | ~30 s |
| W2 | 100 parallel | 1 s | ~30 s |
| W3 | 1000 parallel | 5 s | ~30 s |
| W4 | 1000 parallel | 1 s | ~30 s |
| W5 | 1000 req/min | konstant | 10 min |

**Daten-Endpunkt (POST `http://localhost/api/v1/articles`):**
| # | VUs | Ramp-up | Body | Erwartung |
|---|---|---|---|---|
| A1 | 10 | 0 s | klein | 200/201 |
| A2 | 100 | 1 s | klein | 200/201 |
| A3 | 1000 | 5 s | klein | 200/201 |
| B1 | 10 | 0 s | 5 MB | 200/201 |
| B2 | 100 | 1 s | 5 MB | 200/201 |
| B3 | 1000 | 5 s | 5 MB | 200/201 oder 429 |
| B4 | 1000 | 5 s | 5 MB | alle erfolgreich |

### Ausführung
```bash
docker run --rm -i --network host -v ${PWD}/loadtests/scripts:/scripts \
  grafana/k6 run /scripts/web-endpoint.js -e SCENARIO=W1
```

Ergebnis-Dokumentation pro Szenario:
- k6-Summary (req/s, p95, p99, Fehlerquote)
- Grafana-Screenshot (CPU/Memory/Latency) im selben Zeitraum
- Kurze Einordnung (Pass/Fail, Bottleneck)

### Voraussetzung für B3/B4
- `client_max_body_size` in [nginx.conf](../monitoring/nginx/nginx.conf) ist aktuell `10m` — passt.
- Spring `spring.servlet.multipart.max-request-size` prüfen.
- Für B4 ("ordnungsgemäß beantwortet") muss ggf. horizontal skaliert werden — falls Single-Instance nicht reicht: dokumentieren als "erfordert Scale-Out (Kollegen-Task)".

---

## Reihenfolge / Vorgehen

1. README.md schreiben (schnell, ~30 min)
2. Frontend: API-Wrapper + ErrorBanner + error.tsx, dann manuell die 4 Stop-Szenarien testen
3. k6-Skripte schreiben (3 Dateien)
4. Tests ausführen, Ergebnisse je Szenario in `loadtests/results/*.md` festhalten
5. Loadtest-Übersicht in README verlinken
