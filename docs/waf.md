# Web Application Firewall (WAF)

Die WAF schützt die Anwendung vor typischen Web-Angriffen (SQL-Injection, XSS,
Path Traversal, RCE, Scanner/Bots …). Sie ist direkt im Reverse Proxy `nginx`
integriert — also genau dort, wo TLS terminiert wird und der Klartext-Request
zum ersten Mal sichtbar ist.

> **Aktueller Modus: `On` (blockierend).** Die WAF erkennt Angriffe, schreibt sie
> ins Audit-Log und blockiert mit **`403`**. Der Modus wird ueber
> `MODSEC_RULE_ENGINE: On` gesetzt (siehe Abschnitt 2).

## 1. Aufbau

| Komponente | Umsetzung |
|---|---|
| Engine | **ModSecurity v3** (nginx-Connector) |
| Regelwerk | **OWASP Core Rule Set (CRS)** |
| Image | `owasp/modsecurity-crs:nginx` (ersetzt `nginx:alpine`) |
| Platzierung | im bestehenden nginx, **einziger Eingang** (80/443) |
| Modus (Start) | `On` — blockiert mit **403** |

Das Image bringt Engine, Connector-Modul und das komplette CRS mit. Aktiviert
wird die WAF über drei Zeilen in `monitoring/nginx/nginx.conf`:

```nginx
load_module modules/ngx_http_modsecurity_module.so;   # vor events {}
# ... im http {} Block:
modsecurity on;
modsecurity_rules_file /etc/modsecurity.d/setup.conf;  # bindet Recommended-Config + CRS ein
```

Die restliche nginx-Config (Upstreams, Rate-Limiting, TLS) bleibt unverändert.

> **Mount-Hinweis:** Das CRS-Image generiert seine `/etc/nginx/nginx.conf` beim
> Start selbst aus einem Template (envsubst). Unsere `nginx.conf` wird deshalb
> **nicht** nach `/etc/nginx/nginx.conf`, sondern als Template nach
> `/etc/nginx/templates/nginx.conf.template` gemountet — sonst scheitert der Start
> mit „can't create /etc/nginx/nginx.conf: Read-only file system". Zusätzlich
> schreibt nginx die PID nach `/tmp/nginx.pid` (`pid /tmp/nginx.pid;`), damit der
> Pfad auf dem nicht-root-Container beschreibbar ist.

## 2. Konfiguration (Environment in `docker-compose.yml`)

| Variable | Wert | Zweck |
|---|---|---|
| `MODSEC_RULE_ENGINE` | `On` | Modus: blockierend (403) |
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

## 3. Blockiermodus und Tuning

Die WAF laeuft im blockierenden Modus. Falls es zu Fehlalarmen kommt, kannst du
die Regeln gezielt entschärfen (CRS-Exclusion-Rules) oder temporaer auf
`DetectionOnly` umstellen.

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

- Im **`On`-Modus** antwortet der Angriffs-Request mit **`403 Forbidden`** und das
  Audit-Log enthaelt den Regeltreffer (z. B. CRS-Regel-ID 942xxx „SQL Injection").

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

Das CRS-Image rendert beim Start seine Konfiguration (envsubst) und legt Cache-,
Log- und Temp-Verzeichnisse an. Da nginx im Container als **nicht-root** läuft,
müssen diese Pfade beschreibbar sein. Statt `read_only: true` werden sie als
`tmpfs` mit `mode=1777` eingebunden:

```yaml
tmpfs:
  - /var/cache/nginx:rw,mode=1777
  - /var/log/nginx:rw,mode=1777
  - /var/log/modsecurity:rw,mode=1777
  - /var/run:rw,mode=1777
  - /tmp
```

> Ohne `mode=1777` schlägt der Start mit
> `mkdir() "/var/cache/nginx/client_temp" failed (13: Permission denied)` fehl,
> weil der nicht-root-Prozess in den root-eigenen tmpfs-Mounts nichts anlegen darf.

Alle übrigen Hardening-Maßnahmen bleiben aktiv: `cap_drop: ALL` + gezielte
`cap_add` (`CHOWN`, `SETUID`, `SETGID`, `NET_BIND_SERVICE`, `DAC_OVERRIDE`) und
`no-new-privileges`.
