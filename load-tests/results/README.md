Lasttest-Ergebnisse
Ausgefuehrt gegen den Compose-Stack im internen Docker-Netzwerk
legocitytimes-app-net mit https://nginx als Ziel. Dadurch wird der
Docker-Desktop-Host-Port-Proxy nicht mitgemessen.

Die Data-Index-Ergebnisse enthalten bereits das nachtraegliche Tuning fuer
Elasticsearch, nginx und search-service sowie die asynchrone Annahme im
Index-Endpunkt.

Web-Frontend
| Szenario | Ergebnis | Erfolgreiche Checks | Fehlerquote | p95 |
| --- | --- | ---: | ---: | ---: |
| web_page_parallel_10_0s.js | PASS | 20/20 | 0.00% | 29.91 ms |
| web_page_parallel_100_1s.js | PASS | 200/200 | 0.00% | 55.37 ms |
| web_page_parallel_1000_5s.js | PASS | 2000/2000 | 0.00% | 4.32 ms |
| web_page_parallel_1000_1s.js via app-net | PASS | 2000/2000 | 0.00% | n/a |
| web_page_rate_1000rpm_10m.js via app-net | PASS | 20002/20002 | 0.00% | 4.02 ms |

Hinweis: Der erste Lauf von web_page_parallel_1000_1s.js gegen
host.docker.internal war frueher fehlerhaft. Der Lauf gegen https://nginx
im App-Netz war fehlerfrei; der Fehlschlag wird daher als lokaler
Docker-Desktop-Host-Port-Proxy-Effekt bewertet.

Data-Index
| Szenario | Ergebnis | Erfolgreiche Checks | Fehlerquote | p95 | Daten gesendet |
| --- | --- | ---: | ---: | ---: | ---: |
| data_index_parallel_10_0s_small.js | PASS | 10/10 | 0.00% | 2.59 s | 20 kB |
| data_index_parallel_100_1s_small.js | PASS | 100/100 | 0.00% | 3.71 s | 197 kB |
| data_index_parallel_1000_5s_small.js | PASS | 1000/1000 | 0.00% | n/a | n/a |
| data_index_parallel_10_0s_5mb.js | PASS | 10/10 | 0.00% | 981.48 ms | 53 MB |
| data_index_parallel_100_1s_5mb.js | PASS | 100/100 | 0.00% | 5.83 s | 525 MB |
| data_index_parallel_1000_5s_5mb_allow429.js | PASS | 1000/1000 | 0.00% | 9.8s | 600 MB |
| data_index_parallel_1000_5s_5mb_strict.js | OFFEN | n/a | n/a | n/a | n/a |

Rohdaten
Die vollstaendigen k6-Ausgaben liegen als .log-Dateien in diesem Verzeichnis.