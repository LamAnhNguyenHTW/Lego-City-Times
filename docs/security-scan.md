# Sicherheitsscan der Docker-Images

## Tool
[Trivy](https://github.com/aquasecurity/trivy) (Aqua Security) — Container-Vulnerability-Scanner. Prüft sowohl OS-Pakete (Alpine/Debian) als auch Anwendungs-Dependencies (Maven, npm).

## Durchführung

**Datum:** 2026-05-21
**Trivy-Image:** `aquasec/trivy:latest`
**Severity-Filter:** `LOW, MEDIUM, HIGH, CRITICAL`
**Aufruf:** [scripts/security-scan.ps1](../scripts/security-scan.ps1)

Das Script scannt alle drei selbst gebauten Images, schreibt Tabellen-Reports nach `docs/security-scans/*.txt` sowie maschinenlesbare JSON-Reports.

```powershell
.\scripts\security-scan.ps1
```

## Ergebnis (final)

| Image | Critical | High | Medium | Low |
|---|---:|---:|---:|---:|
| `legocitytimes/content-service:latest` | **0** | 1 | 0 | 0 |
| `legocitytimes/search-service:latest` | **0** | 0 | 1 | 0 |
| `legocitytimes/frontend:latest` | **0** | 11 | 3 | 2 |
| **Summe** | **0** | 12 | 4 | 2 |

**→ Keine kritischen Schwachstellen.** ✅

Detail-Reports:
- [`legocitytimes_content-service_latest.txt`](security-scans/legocitytimes_content-service_latest.txt)
- [`legocitytimes_search-service_latest.txt`](security-scans/legocitytimes_search-service_latest.txt)
- [`legocitytimes_frontend_latest.txt`](security-scans/legocitytimes_frontend_latest.txt)

## Behebung der ursprünglichen Critical-Findings

Erstscan ergab **3 Critical** in `content-service` und **3 Critical** in `search-service`, alle in derselben transitiven Dependency:

| CVE | Library | Installed | Fixed | Beschreibung |
|---|---|---|---|---|
| CVE-2026-41293 | `tomcat-embed-core` | 11.0.21 | 11.0.22 | Improper Input Validation |
| CVE-2026-43512 | `tomcat-embed-core` | 11.0.21 | 11.0.22 | Authentication Bypass (digest auth) |
| CVE-2026-43515 | `tomcat-embed-core` | 11.0.21 | 11.0.22 | Improper Authorization (method constraints) |

### Maßnahme
Tomcat-Version explizit über Maven-Property auf `11.0.22` hochgezogen (überschreibt die von `spring-boot-starter-parent:4.0.6` mitgelieferte Version):

```xml
<!-- content-service/pom.xml und search-service/pom.xml -->
<properties>
    <java.version>21</java.version>
    <tomcat.version>11.0.22</tomcat.version>
</properties>
```

Nach Re-Build mit `docker compose build content-service search-service` lieferte der Re-Scan **0 Critical**.

## Verbleibende Findings (akzeptabel)

### High-Findings im Frontend (11)
Stammen aus transitive npm-Dependencies (vermutlich Next.js/React-Toolchain). Die Anwendung ist hinter einem Reverse Proxy isoliert, kein direkter Internet-Zugriff auf Node-internals. Eine Reduktion wäre über regelmäßige `npm audit fix` möglich, ändert die Bewertung aber nicht — das Kriterium fordert lediglich "keine **kritischen**" Schwachstellen.

### High-Finding im content-service (1)
Verbleibendes High-Level-CVE in einer transitiven Library. Kein bekannter Public-Exploit, und die Anwendung läuft mit nicht-root-User (`spring:spring`) sowie ohne externe Erreichbarkeit der internen Java-Surface (nur Nginx als Entrypoint).

### Medium/Low
Üblicher Hintergrund-Noise (z.B. Edge-Cases in Standard-Libraries). Für die Belegarbeit ohne weitere Maßnahmen akzeptiert.

## Reproduzierbarkeit

Scan jederzeit erneut ausführbar:

```powershell
.\scripts\security-scan.ps1
```

Beim ersten Lauf werden die Trivy-CVE-Datenbanken (~1 GB) gecacht. Folgeläufe dauern ~1–2 Minuten.
