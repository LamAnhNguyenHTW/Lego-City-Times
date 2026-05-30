# Web Application Firewall (WAF)

Die WAF schützt die Anwendung vor typischen Web-Angriffen (SQL-Injection, XSS,
Path Traversal, RCE, Scanner/Bots …). Sie ist direkt im Reverse Proxy `nginx`
integriert — also genau dort, wo TLS terminiert wird und der Klartext-Request
zum ersten Mal sichtbar ist.

## 1. Aufbau

| Komponente | Umsetzung |
|---|---|
| Engine | **ModSecurity v3** (nginx-Connector) |
| Regelwerk | **OWASP Core Rule Set (CRS)** |
| Image | `owasp/modsecurity-crs:nginx-alpine` (ersetzt `nginx:alpine`) |
| Platzierung | im bestehenden nginx, **einziger Eingang** (80/443) |
| Modus (Start) | `DetectionOnly` — nur loggen, **nicht** blockieren |

Das Image bringt Engine, Connector-Modul und das komplette CRS mit. Aktiviert
wird die WAF über drei Zeilen in `monitoring/nginx/nginx.conf`:

```nginx
load_module modules/ngx_http_modsecurity_module.so;   # vor events {}
# ... im http {} Block:
modsecurity on;
modsecurity_rules_file /etc/modsecurity.d/setup.conf;  # bindet Recommended-Config + CRS ein
```

Die restliche nginx-Config (Upstreams, Rate-Limiting, TLS) bleibt unverändert.

## 2. Konfiguration (Environment in `docker-compose.yml`)

| Variable | Wert | Zweck |
|---|---|---|
| `MODSEC_RULE_ENGINE` | `DetectionOnly` | Modus: nur erkennen/loggen (Start) |
| `PARANOIA` | `1` | CRS-Strenge (1 = Standard, weniger Fehlalarme) |
| `ANOMALY_INBOUND` | `5` | Schwellwert eingehende Anomalie-Score |
| `ANOMALY_OUTBOUND` | `4` | Schwellwert ausgehende Anomalie-Score |
| `MODSEC_REQ_BODY_LIMIT` | `20971520` | Body-Limit (20 MB) = `client_max_body_size` |
| `MODSEC_REQ_BODY_NOFILES_LIMIT` | `20971520` | Limit für Nicht-Upload-Bodies (JSON!) |
| `MODSEC_AUDIT_ENGINE` | `RelevantOnly` | Audit-Log nur bei Regeltreffern |
| `MODSEC_AUDIT_LOG` | `/var/log/modsec/audit.log` | Audit-Log-Pfad (Volume `modseclogs`) |

> **Wichtig — Lasttests:** Das CRS limitiert per Default Nicht-Upload-Bodies auf
> 128 KB. Da `/internal/search` JSON-Payloads bis **5 MB** verarbeitet, wurde
> `MODSEC_REQ_BODY_NOFILES_LIMIT` auf 20 MB angehoben — sonst würde die WAF die
> k6-Lasttests abweisen.

## 3. Vom Erkennen zum Blockieren

Empfohlener Ablauf, um Fehlalarme (False Positives) zu vermeiden:

1. **Start in `DetectionOnly`** (aktuell gesetzt) → Stack laufen lassen, normale
   Nutzung + Lasttests durchspielen.
2. **Audit-Log prüfen** (siehe unten): Treffer, die *legitimen* Traffic betreffen,
   sind False Positives.
3. Bei Bedarf einzelne Regeln per Ausnahme entschärfen (CRS-Exclusion-Rules).
4. **Auf blockierend umstellen:** in `docker-compose.yml`
   `MODSEC_RULE_ENGINE: On` setzen und nginx neu starten.

```powershell
# nach Änderung des Modus:
docker compose up -d nginx
```

## 4. Nachvollziehen / Testen

```powershell
# 1) Modul geladen & Config gültig?
docker exec legocitytimes-nginx nginx -t

# 2) Harmloser Request -> 200/301, kein WAF-Treffer
curl.exe -k -o NUL -w "%{http_code}`n" https://localhost/

# 3) Angriffs-Request (SQL-Injection in Query) auslösen:
curl.exe -k -o NUL -w "%{http_code}`n" "https://localhost/api/v1/search/articles?q=1%27%20OR%20%271%27=%271"

# 4) WAF-Treffer im Audit-Log ansehen:
docker exec legocitytimes-nginx tail -n 20 /var/log/modsec/audit.log
# bzw. Live:
docker exec legocitytimes-nginx tail -f /var/log/modsec/audit.log
```

- Im **`DetectionOnly`-Modus** liefert der Angriffs-Request weiterhin eine normale
  Antwort, **aber** das Audit-Log enthält den Regeltreffer (z. B. CRS-Regel-ID
  942xxx „SQL Injection").
- Nach Umstellung auf `On` antwortet derselbe Request mit **`403 Forbidden`**.

## 5. Logs

| Log | Ort |
|---|---|
| WAF-Audit-Log | Container `/var/log/modsec/audit.log` → Volume `legocitytimes-modseclogs` |
| ModSecurity-Meldungen | nginx-`error_log` (Container) |
| nginx-Access-Log | `/var/log/nginx/access.log` (Format `main`, unverändert) |

```powershell
docker volume ls | Select-String modseclogs
docker exec legocitytimes-nginx ls -lh /var/log/modsec
```

## 6. Hinweis zum Hardening

Das CRS-Image rendert beim Start seine Konfiguration (envsubst) nach
`/etc/modsecurity.d` und `/etc/nginx/conf.d` und braucht dafür ein beschreibbares
Dateisystem. Deshalb wurde `read_only: true` beim nginx-Service entfernt. Alle
übrigen Hardening-Maßnahmen (`cap_drop: ALL` + gezielte `cap_add`,
`no-new-privileges`, `tmpfs`) bleiben aktiv.
