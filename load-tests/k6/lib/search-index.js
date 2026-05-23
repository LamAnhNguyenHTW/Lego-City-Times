import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { envUrl } from './env.js';

const dataIndexResponses = new Counter('data_index_responses');

export function postIndex(payloadJson, { allow429 = false } = {}) {
  const baseUrl = envUrl('DATA_BASE_URL', 'https://localhost');
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  };
  if (allow429) {
    headers['X-Load-Test-Allow-429'] = 'true';
  }

  const res = http.post(`${baseUrl}/internal/search/articles/index`, payloadJson, {
    headers,
    timeout: '300s',
    tags: {
      endpoint: 'data-index',
    },
  });

  dataIndexResponses.add(1, { status: String(res.status) });

  const accepted = allow429 ? res.status === 200 || res.status === 429 : res.status === 200;
  if (!accepted) {
    console.warn(`data-index unexpected status=${res.status}`);
  }

  check(res, {
    [allow429 ? 'data: status is 200 or 429' : 'data: status is 200']: () => accepted,
  });

  return res;
}
