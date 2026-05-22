# Lasttest-Ergebnisse

Ausgefuehrt am 2026-05-22 gegen den Compose-Stack im internen Docker-Netzwerk
`legocitytimes-app-net` mit `https://nginx` als Ziel. Dadurch wird der
Docker-Desktop-Host-Port-Proxy nicht mitgemessen.

## Web-Frontend

| Szenario | Ergebnis | Fehlerquote | p95 |
| --- | --- | ---: | ---: |
| `web_page_parallel_10_0s.js` | PASS | 0.00% | 29.91 ms |
| `web_page_parallel_100_1s.js` | PASS | 0.00% | 55.37 ms |
| `web_page_parallel_1000_5s.js` | PASS | 0.00% | 280.05 ms |
| `web_page_parallel_1000_1s.js` via app-net | PASS | 0.00% | 97.38 ms |
| `web_page_rate_1000rpm_10m.js` via app-net | PASS | 0.00% | 4.02 ms |

Hinweis: Der erste Lauf von `web_page_parallel_1000_1s.js` gegen
`host.docker.internal` ist mit 35.09% Fehlerquote fehlgeschlagen. Der gleiche
Lauf gegen `https://nginx` im App-Netz war fehlerfrei; der Fehlschlag wird daher
als lokaler Docker-Desktop-Host-Port-Proxy-Effekt bewertet.

## Data-Index

| Szenario | Ergebnis | Erfolgreiche Checks | Fehlerquote | p95 |
| --- | --- | ---: | ---: | ---: |
| `data_index_parallel_10_0s_small.js` | PASS | 10/10 | 0.00% | 2.59 s |
| `data_index_parallel_100_1s_small.js` | PASS | 100/100 | 0.00% | 3.71 s |
| `data_index_parallel_1000_5s_small.js` | FAIL | 514/1000 | 48.60% | 11.88 s |
| `data_index_parallel_10_0s_5mb.js` | FAIL | 6/10 | 40.00% | 6.48 s |
| `data_index_parallel_100_1s_5mb.js` | FAIL | 47/100 | 53.00% | 39.51 s |
| `data_index_parallel_1000_5s_5mb_allow429.js` | ABGEBROCHEN | n/a | n/a | n/a |

Der 1000x5-MB-Lauf kam lokal nicht sauber aus der k6-Initialisierung heraus und
hat Docker Desktop sichtbar ueberlastet; die Docker API lieferte danach 500er.
Der Test wurde abgebrochen, bevor der Strict-Lauf erneut gestartet wurde.

## Rohdaten

Die vollstaendigen k6-Ausgaben liegen als `.log`-Dateien in diesem Verzeichnis.
