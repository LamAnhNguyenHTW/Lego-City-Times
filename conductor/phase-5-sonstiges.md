# Phase 5 — Sonstiges (4 Punkte)

## Ziel

Die vier Punkte aus der Sektion "Sonstiges" der [bewertungskriterien.md](../bewertungskriterien.md) abdecken:

1. **Automatisiertes DB-Backup** (1 P)
2. **Sicherheitsscan durchgeführt + dokumentiert** (1 P)
3. **Scan findet keine kritischen Schwachstellen** (1 P)
4. **Web Application Firewall (WAF)** (1 P)

**Abgeschlossen, wenn:**
- DB-Backup-Service läuft im Compose-Stack, schreibt regelmäßig in eigenes Volume, Restore wurde verifiziert.
- Trivy hat alle eigenen Images gescannt, Ergebnis in `docs/security-scan.md` dokumentiert.
- Keine "CRITICAL"-Findings mehr (ggf. Base-Images aktualisiert).
- WAF läuft als Reverse Proxy vor der App und blockt mind. einen offensichtlichen Angriff (z.B. SQLi).

### Out of Scope
- Vollständiges Disaster-Recovery-Konzept (nur Backup, kein Off-Site)
- WAF-Custom-Rules (nur OWASP CRS Default)
- Image-Signing / Supply-Chain-Security

---

## Aufgabe 1 — Automatisiertes DB-Backup

### Tool: `prodrigestivill/postgres-backup-local`
- Etabliert, läuft als Sidecar-Container
- Konfigurierbare Rotation (täglich / wöchentlich / monatlich)
- Schreibt `pg_dump`-Output komprimiert in ein Volume

### Compose-Snippet

```yaml
pg-backup:
  image: prodrigestivill/postgres-backup-local:17
  container_name: legocitytimes-pg-backup
  depends_on:
    postgres:
      condition: service_healthy
  environment:
    POSTGRES_HOST: postgres
    POSTGRES_DB: legocitytimes
    POSTGRES_USER: legocity
    POSTGRES_PASSWORD: legocity
    POSTGRES_EXTRA_OPTS: "-Z6 --schema=public --blobs"
    SCHEDULE: "@daily"            # Für Demo evtl. "*/5 * * * *"
    BACKUP_KEEP_DAYS: 7
    BACKUP_KEEP_WEEKS: 4
    BACKUP_KEEP_MONTHS: 6
    HEALTHCHECK_PORT: 8080
  volumes:
    - pgbackups:/backups
  restart: unless-stopped

# volumes:
#   pgbackups: { name: legocitytimes-pgbackups }
```

### Verifikation
1. `docker compose up -d pg-backup`
2. Für Demo: `SCHEDULE: "*/2 * * * *"` (alle 2 Min)
3. Nach 2 Min: `docker exec legocitytimes-pg-backup ls /backups/daily`
4. Restore-Test:
   ```bash
   docker exec -i legocitytimes-postgres psql -U legocity -d legocitytimes < <(docker exec legocitytimes-pg-backup zcat /backups/daily/legocitytimes-latest.sql.gz)
   ```
5. In `docs/db-backup.md` festhalten: Konfiguration, gefundene Dateien, Restore-Output.

---

## Aufgabe 2 — Sicherheitsscan mit Trivy

### Tool: `aquasec/trivy`
- Erkennt CVEs in OS-Paketen + Anwendungs-Dependencies (Maven/npm)
- Kostenlos, offline-fähig nach erstem Download

### Zu scannende Images
- `legocitytimes/content-service:latest`
- `legocitytimes/search-service:latest`
- `legocitytimes/frontend:latest`

### Ausführung

Script `scripts/security-scan.ps1`:

```powershell
$images = @(
  "legocitytimes/content-service:latest",
  "legocitytimes/search-service:latest",
  "legocitytimes/frontend:latest"
)

New-Item -ItemType Directory -Force -Path docs/security-scans | Out-Null

foreach ($img in $images) {
  $safeName = $img.Replace("/", "_").Replace(":", "_")
  Write-Host "Scanning $img ..."
  docker run --rm `
    -v /var/run/docker.sock:/var/run/docker.sock `
    -v ${PWD}/docs/security-scans:/out `
    aquasec/trivy:latest image `
    --severity HIGH,CRITICAL `
    --format table `
    --output /out/$safeName.txt `
    $img
}
```

### Dokumentation
`docs/security-scan.md` mit:
- Datum, Trivy-Version, Aufruf
- Tabelle pro Image: `Critical | High | Medium | Low`
- Links zu Detail-Reports (`docs/security-scans/*.txt`)
- Falls Findings: behandelte CVEs + Begründung wenn ignoriert (`.trivyignore`)

### Iteratives Fixing für "keine Critical"
1. Scan ausführen
2. Bei Critical-Findings:
   - Base-Image auf aktuelle LTS-Version updaten:
     - content/search: `eclipse-temurin:21-jre-alpine` (oder `21-jre`)
     - frontend: `node:22-alpine`
   - `mvn versions:use-latest-releases` ggf. nur für betroffene Libs
   - npm: `npm audit fix`
3. Re-Build + Re-Scan
4. Wiederholen bis Critical = 0
5. Verbleibende High-Findings dokumentieren (mit Begründung warum akzeptabel)

---

## Aufgabe 3 — Web Application Firewall (WAF)

### Tool: `owasp/modsecurity-crs:nginx-alpine`
- Drop-in-Ersatz für `nginx:alpine`
- OWASP ModSecurity v3 + Core Rule Set vorinstalliert
- Blockt SQLi, XSS, RCE, Path Traversal etc. out-of-the-box

### Compose-Änderung

Ersetzen:
```yaml
nginx:
  image: owasp/modsecurity-crs:nginx-alpine
  container_name: legocitytimes-nginx
  depends_on:
    - content-service
    - search-service
    - frontend
  ports:
    - "80:8080"
  environment:
    PARANOIA: 1
    ANOMALY_INBOUND: 5
    ANOMALY_OUTBOUND: 4
    MODSEC_RULE_ENGINE: "On"      # für Demo, sonst "DetectionOnly"
    BACKEND: http://frontend:3000  # nicht relevant — wir nutzen eigene Config
  volumes:
    - ./monitoring/nginx/nginx.conf:/etc/nginx/templates/conf.d/default.conf.template:ro
  restart: unless-stopped
```

⚠️ **Achtung:** Das CRS-Image nutzt ein anderes Config-Layout (`/etc/nginx/templates/...`) — die bestehende `nginx.conf` muss ggf. angepasst werden. Alternative: ModSecurity-Modul in bestehende Nginx-Config einbinden via `load_module modsecurity.so;`.

### Verifikation
Tests mit `curl`:

```bash
# Normale Anfrage → 200
curl -i http://localhost/

# SQL-Injection-Versuch → erwartet 403
curl -i "http://localhost/?id=1'%20OR%201=1--"

# XSS-Versuch → erwartet 403
curl -i "http://localhost/?q=<script>alert(1)</script>"

# Path Traversal → erwartet 403
curl -i "http://localhost/../../etc/passwd"
```

Logs prüfen:
```bash
docker logs legocitytimes-nginx 2>&1 | grep ModSecurity
```

### Dokumentation
`docs/waf.md` mit:
- Tool + Version
- Konfiguration (Paranoia-Level, Anomaly-Threshold)
- Test-Ergebnisse (curl-Ausgaben + Logs)
- Hinweis: Bei Lasttests mit Arman absprechen — eventuell User-Agent whitelisten oder auf "DetectionOnly" stellen.

---

## Reihenfolge / Vorgehen

1. **DB-Backup** (~30 min, am einfachsten)
2. **Security-Scan** durchführen, dokumentieren, Critical-Findings fixen (~1-2 h)
3. **WAF** einbauen, Verifikation, Doku (~1-2 h)
4. Mit Arman koordinieren: WAF muss vor Lasttests aktiv sein oder auf DetectionOnly
5. README-Update: Backup-Volume + Security-Scan-Skript erwähnen
