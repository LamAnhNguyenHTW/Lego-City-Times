import http from 'k6/http';
import { check } from 'k6';
import { envUrl } from './env.js';

export function postIndex(payloadJson, { allow429 = false } = {}) {
  const baseUrl = envUrl('DATA_BASE_URL', 'https://localhost');
  const res = http.post(`${baseUrl}/internal/search/articles/index`, payloadJson, {
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    timeout: '120s',
    tags: {
      endpoint: 'data-index',
    },
  });

  check(res, {
    'data: status is 200 (or 429 allowed)': (r) => (allow429 ? r.status === 200 || r.status === 429 : r.status === 200),
  });

  return res;
}
