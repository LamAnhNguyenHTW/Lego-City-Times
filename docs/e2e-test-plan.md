# End-to-End-Testplan — Lego City Times

Dieser Plan prüft die gesamte Infrastruktur **gegen den laufenden Stack** und ist
1:1 an der [bewertungskriterien.md](../bewertungskriterien.md) ausgerichtet.
Jeder Punkt hat eine Checkbox und einen konkreten Befehl (PowerShell, Windows).

> **Konventionen**
> - nginx terminiert TLS mit selbst-signiertem Zertifikat → `curl` immer mit `-k`.
> - Aufrufe über `http://localhost/` werden auf `https://localhost/` umgeleitet.
> - Einzelne Basisdienste heißen `legocitytimes-<service>`. Replizierte Services
>   (`content-service`, `search-service`, `frontend`) heißen `lego-city-times-<service>-<n>`.

---

## Phase 0 — Stack starten

```powershell
docker compose down -v          # sauberer Ausgangszustand
.\scripts\up.ps1 -Build        # rendert Alertmanager + startet Compose
```

- [ ] Nach ca. 60–90 s sind alle Container `healthy` / `running`:
  ```powershell
  docker compose ps
  ```
- [ ] Keine Container im Status `restarting` oder `exited`.

---

## Phase 1 — Compose & Initialisierung (4 P)

| ✓ | Kriterium | Befehl / Prüfung | Erwartung |
|---|---|---|---|
| [ ] | Init-Schritte laufen automatisch | `docker compose logs content-service | Select-String "ddl"` | DB-Tabellen via JPA angelegt, keine manuellen Schritte |
| [ ] | ES-Index automatisch erzeugt | `docker compose logs search-service` | Index-Mapping ohne Fehler |
| [ ] | Webanwendung erreichbar | Browser: `https://localhost/` | Frontend lädt (Zertifikatswarnung bestätigen) |
| [ ] | Daten-Endpunkt vorhanden | siehe Phase 3 / Smoke-Test unten | HTTP 200 |
| [ ] | Health-URL existiert | `curl -k https://localhost/actuator/health` | `{"status":"UP"}` |

**Smoke-Test Daten-Endpunkt:**
```powershell
curl -k -X POST https://localhost/internal/search/articles/index `
  -H "Content-Type: application/json" `
  -d '{"id":"smoke-1","title":"Smoke Test","slug":"smoke","author":"E2E","status":"PUBLISHED","publishedAt":"2026-05-22T00:00:00Z"}'
```
- [ ] Antwort HTTP 200.
- [ ] Treffer auffindbar: `curl -k "https://localhost/api/v1/search/articles?q=smoke"`

---

## Phase 2 — Persistenz / Volumes (1 P)

- [ ] Testartikel anlegen (siehe Smoke-Test oben).
- [ ] Bild hochladen (optional, falls Admin-Token vorhanden).
- [ ] Infrastruktur **ohne** `-v` neu starten:
  ```powershell
  docker compose down
  docker compose up -d
  ```
- [ ] Artikel weiterhin auffindbar: `curl -k "https://localhost/api/v1/search/articles?q=smoke"`
- [ ] Volumes vorhanden:
  ```powershell
  docker volume ls | Select-String legocitytimes
  ```

---

## Phase 3 — Lasttests (12 P)

Voraussetzung: 5-MB-Payload erzeugen (einmalig):
```powershell
powershell -ExecutionPolicy Bypass -File load-tests/k6/generate-payloads.ps1
```

Ausführung jeweils mit (Windows/Docker Desktop):
```powershell
docker run --rm -v "${PWD}:/work" -w /work `
  -e K6_INSECURE_SKIP_TLS_VERIFY=true `
  -e WEB_BASE_URL=https://host.docker.internal `
  -e DATA_BASE_URL=https://host.docker.internal `
  grafana/k6 run load-tests/k6/<SKRIPT>.js
```

### Web-Endpunkt (Webseite anzeigen)
| ✓ | Szenario | Skript |
|---|---|---|
| [ ] | 10 parallel, 0 s Ramp-up | `web_page_parallel_10_0s.js` |
| [ ] | 100 parallel, 1 s Ramp-up | `web_page_parallel_100_1s.js` |
| [ ] | 1000 parallel, 5 s Ramp-up | `web_page_parallel_1000_5s.js` |
| [ ] | 1000 parallel, 1 s Ramp-up | `web_page_parallel_1000_1s.js` |
| [ ] | 1000 req/min über 10 min | `web_page_rate_1000rpm_10m.js` |

### Daten-Endpunkt (Indexing)
| ✓ | Szenario | Skript |
|---|---|---|
| [ ] | 10 parallel, 0 s, normal | `data_index_parallel_10_0s_small.js` |
| [ ] | 100 parallel, 1 s, normal | `data_index_parallel_100_1s_small.js` |
| [ ] | 1000 parallel, 5 s, normal | `data_index_parallel_1000_5s_small.js` |
| [ ] | 10 parallel, 0 s, 5 MB Body | `data_index_parallel_10_0s_5mb.js` |
| [ ] | 100 parallel, 1 s, 5 MB Body | `data_index_parallel_100_1s_5mb.js` |
| [ ] | 1000 parallel, 5 s, 5 MB (429 erlaubt) | `data_index_parallel_1000_5s_5mb_allow429.js` |
| [ ] | 1000 parallel, 5 s, 5 MB (alle 200) | `data_index_parallel_1000_5s_5mb_strict.js` |

**Pro Szenario dokumentieren** in `load-tests/results/<szenario>.md`:
- [ ] k6-Summary (req/s, p95, p99, Fehlerquote, `checks` = 100 %)
- [ ] Grafana-Screenshot (CPU/Memory/Latenz im Testzeitraum)
- [ ] Einordnung Pass/Fail + ggf. Bottleneck

---

## Phase 4 — Verfügbarkeit (6 P)

| ✓ | Kriterium | Befehl | Erwartung |
|---|---|---|---|
| [ ] | Ausfall eines Frontend-Webservers ohne Auswirkung | `docker kill lego-city-times-frontend-1` → `https://localhost/` neu laden | Seite weiter erreichbar (andere Replica) |
| [ ] | **nginx-DNS-Check** | mehrfach `curl -k https://localhost/` nach dem Kill | Antworten kommen weiter — falls nicht: nginx-`upstream` löst Replicas nicht dynamisch auf → Konfig anpassen |
| [ ] | Backend-Ausfall im Monitoring sichtbar | `docker compose stop content-service` → Prometheus `up{job="content-service"}` | Alert `ServiceDown` feuert |
| [ ] | Backend-Ausfall → Fehlermeldung im Frontend | Startseite laden | Fehler-Banner statt weiße Seite |
| [ ] | DB-Ausfall im Monitoring sichtbar | `docker compose stop postgres` → Grafana / Alerts | DB-/5xx-Alert feuert |
| [ ] | DB-Ausfall → Fehlermeldung im Frontend | Startseite laden | Fehler-Banner |
| [ ] | Scale-Out bei Überlastung | Lasttest starten, dann `docker compose up -d --scale frontend=6` | Latenz/Fehlerquote sinkt sichtbar in Grafana |

Nach den Tests Dienste wieder starten: `docker compose up -d`

---

## Phase 5 — Monitoring (6 P)

| ✓ | Kriterium | Befehl | Erwartung |
|---|---|---|---|
| [ ] | Services besitzen Health-Checks | `docker compose ps` | Spalte `STATUS` zeigt `healthy` für postgres, elasticsearch, content-service, search-service, nginx, frontend |
| [ ] | Geordnete Start-Reihenfolge | `docker compose logs --timestamps` | content/search starten nach DB-healthy, frontend/nginx danach |
| [ ] | Container-Absturz wird erkannt | `docker kill lego-city-times-content-service-1` | Alert `ServiceDown` / `RunningContainersBelowExpected` feuert |
| [ ] | Container wird automatisch neu gestartet | `docker compose ps` nach dem Kill | Container läuft wieder (`restart: unless-stopped`) |
| [ ] | Überwachungslimits konfiguriert | `docker inspect lego-city-times-content-service-1 --format '{{.HostConfig.Memory}} {{.HostConfig.NanoCpus}}'` | Werte ≠ 0 (Limits gesetzt) |
| [ ] | Limits im Monitoring sichtbar | Grafana-Dashboard | Memory-%-Panel zeigt Werte (cAdvisor) |
| [ ] | Benachrichtigung bei Limit-Überschreitung | Alert künstlich auslösen (z.B. Last erzeugen) | Webhook-Notification im Alertmanager; für Slack `alertmanager.yml.tmpl` rendern |

> Die Alert-Rules erfassen sowohl feste Containernamen (`legocitytimes-*`) als auch
> replizierte Compose-Container (`lego-city-times-*-<n>`).

---

## Phase 6 — Security (5 P)

| ✓ | Kriterium | Befehl | Erwartung |
|---|---|---|---|
| [ ] | Netze sinnvoll isoliert | `docker network inspect legocitytimes-db-net --format '{{.Internal}}'` | `true` (DB-Netz ohne Internet) |
| [ ] | SSL-Verschlüsselung | `curl -kv https://localhost/ 2>&1 | Select-String "SSL connection"` | TLS-Handshake erfolgreich |
| [ ] | HTTP → HTTPS Redirect | `curl -ki http://localhost/` | `301` nach `https://` |
| [ ] | Keine Passwörter in `docker inspect` | `docker inspect lego-city-times-content-service-1 --format '{{json .Config.Env}}'` | kein `SPRING_DATASOURCE_PASSWORD`/Klartext |
| [ ] | Secrets gemountet | `docker exec lego-city-times-content-service-1 ls /run/secrets` | `postgres_password` und `jwt_secret` vorhanden |
| [ ] | Keine unnötigen Ports | `docker compose ps` | nur 80, 443 (nginx), 3000 (grafana) gemappt |
| [ ] | Read-Only / Capabilities | `docker inspect legocitytimes-nginx --format '{{.HostConfig.ReadonlyRootfs}} {{.HostConfig.CapDrop}}'` | `true [ALL]` |

---

## Phase 7 — Sonstiges (4 P)

| ✓ | Kriterium | Befehl | Erwartung |
|---|---|---|---|
| [ ] | Automatisiertes DB-Backup | `docker exec legocitytimes-pg-backup /backup.sh` dann `docker exec legocitytimes-pg-backup ls -lh /backups/daily` | Backup-Datei vorhanden |
| [ ] | Backup-Restore funktioniert | Restore-Test laut [db-backup.md](db-backup.md) | Daten eingespielt |
| [ ] | Sicherheitsscan durchgeführt + dokumentiert | `.\scripts\security-scan.ps1` | Reports in `docs/security-scans/` |
| [ ] | Keine kritischen Schwachstellen | Scan-Ausgabe | `Critical = 0` für alle 3 Images |
| [ ] | **WAF** | — | ❌ noch nicht implementiert |

---

## Zusammenfassung offener Punkte

- [ ] **WAF** (Phase 7) — noch nicht umgesetzt.
- [ ] **Lasttest-Ergebnisse** (Phase 3) — Skripte vorhanden, Ausführung + Ergebnisablage stehen aus.
- [ ] **nginx-Upstream-DNS** (Phase 4) — praktisch prüfen, ob Replicas im laufenden Stack wirklich load-balanced werden.
