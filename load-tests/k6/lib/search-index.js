import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { envUrl } from './env.js';

const dataIndexResponses = new Counter('data_index_responses');
const dataIndexConnErrors = new Counter('data_index_conn_errors');

// k6 v2.0.0 setzt exit code 1 wenn http_req_failed > 0% ist (auch ohne expliziten Threshold).
// Für allow429-Tests: alle HTTP-Fehlertypen sind akzeptabel — Threshold explizit auf 100% setzen.
export const allow429Thresholds = {
  checks: ['rate==1.0'],
  http_req_failed: ['rate<=1.0'],  // connection refused + 429 + 503 sind alle OK
};

// In allow429-Modus: 429 und 503 als "erwartet" markieren (zählen nicht als http_req_failed).
// Muss im Init-Kontext des Tests aufgerufen werden.
export function setupAllow429ResponseCallback() {
  http.setResponseCallback(http.expectedStatuses({ min: 200, max: 299 }, 429, 503));
}


export function postIndex(payloadJson, { allow429 = false } = {}) {
  const baseUrl = envUrl('DATA_BASE_URL', 'https://localhost');
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  };
  if (allow429) {
    headers['X-Load-Test-Allow-429'] = 'true';
  } else {
    headers['X-Load-Test-Allow-429'] = 'false';
  }

  const res = http.post(`${baseUrl}/internal/search/articles/index`, payloadJson, {
    headers,
    timeout: '300s',
    tags: {
      endpoint: 'data-index',
    },
  });

  // status=0 means connection-level error (refused, reset, timeout)
  if (res.status === 0) {
    dataIndexConnErrors.add(1, { allow429: String(allow429) });
    console.warn(`data-index connection error: ${res.error} (error_code=${res.error_code})`);
  } else {
    dataIndexResponses.add(1, { status: String(res.status) });
  }

  // allow429: accept 200 (ok), 429 (rate limited), 503 (ES overloaded),
  //           and 0 (connection refused = server protecting itself under extreme load)
  const accepted = allow429
    ? res.status === 200 || res.status === 429 || res.status === 503 || res.status === 0
    : res.status === 200;

  if (!accepted) {
    console.warn(`data-index unexpected status=${res.status} error=${res.error || ''}`);
  }

  const checkLabel = allow429
    ? 'data: status is 200, 429, 503 or conn-refused'
    : 'data: status is 200';
  check(res, {
    [checkLabel]: () => accepted,
  });

  return res;
}