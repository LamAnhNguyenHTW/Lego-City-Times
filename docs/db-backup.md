# Automatisiertes Datenbank-Backup

## Tool

[`prodrigestivill/postgres-backup-local:17`](https://github.com/prodrigestivill/docker-postgres-backup-local) — Sidecar-Container mit eingebautem Cron, der periodisch `pg_dump` ausführt und die Ergebnisse komprimiert in ein Volume schreibt.

## Konfiguration

Definiert in [docker-compose.yml](../docker-compose.yml) als Service `pg-backup`:

| Variable | Wert | Bedeutung |
|---|---|---|
| `POSTGRES_HOST` | `postgres` | Ziel-DB im Compose-Netz |
| `POSTGRES_DB` | `legocitytimes` | Datenbank |
| `POSTGRES_USER/PASSWORD` | `legocity / legocity` | Auth |
| `POSTGRES_EXTRA_OPTS` | `-Z6 --blobs` | Kompressionslevel 6, BLOBs inkludieren |
| `SCHEDULE` | `@daily` | Cron-Ausdruck — täglich um Mitternacht |
| `BACKUP_KEEP_DAYS` | `7` | Tagesbackups: 7 Tage |
| `BACKUP_KEEP_WEEKS` | `4` | Wochenbackups: 4 Wochen |
| `BACKUP_KEEP_MONTHS` | `6` | Monatsbackups: 6 Monate |

Speicherort: Docker-Volume `legocitytimes-pgbackups` (gemountet auf `/backups`).

## Verzeichnisstruktur im Volume

```
/backups/
├── daily/
│   ├── legocitytimes-YYYY-MM-DD.sql.gz
│   └── legocitytimes-latest.sql.gz   → Symlink auf neuestes Tagesbackup
├── weekly/
│   └── legocitytimes-YYYY-Www.sql.gz
└── monthly/
    └── legocitytimes-YYYY-MM.sql.gz
```

## Verifikation

### Vorhandensein der Backups prüfen
```powershell
docker exec legocitytimes-pg-backup ls -lh /backups/daily
docker exec legocitytimes-pg-backup ls -lh /backups/last
```

### Manuelles Backup auslösen (ohne auf Cron zu warten)
```powershell
docker exec legocitytimes-pg-backup /backup.sh
```

### Restore-Test
```powershell
# Inhalt des letzten Backups in die DB einspielen
docker exec legocitytimes-pg-backup sh -c "zcat /backups/last/legocitytimes-latest.dump.sql.gz" | `
  docker exec -i legocitytimes-postgres psql -U legocity -d legocitytimes
```

### Volume-Persistenz
Backups überleben `docker compose down` (Volume bleibt erhalten) und werden erst durch `docker compose down -v` gelöscht.

## Demo-Hinweis

Für eine kurze Vorführung kann `SCHEDULE` auf `*/2 * * * *` (alle 2 Minuten) gesetzt werden:

```yaml
SCHEDULE: "*/2 * * * *"
```

Nach dem Anpassen `docker compose up -d pg-backup` ausführen.
