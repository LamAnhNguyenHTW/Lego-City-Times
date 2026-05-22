# data_index_parallel_10_0s_small

- Zeitpunkt: 2026-05-22
- Skript: `load-tests/k6/data_index_parallel_10_0s_small.js`
- Ziel: `https://host.docker.internal/internal/search/articles/index`
- Ergebnis: Pass

## k6 Summary

- Checks: 100.00% (10/10)
- HTTP Requests: 10
- HTTP Fehler: 0.00% (0/10)
- Request-Dauer: avg 2.08 s, p95 2.09 s, max 2.09 s
- Durchsatz: 4.71 req/s

## Einordnung

Das 10-parallele Daten-Szenario lief erfolgreich gegen den gestarteten Compose-Stack.
