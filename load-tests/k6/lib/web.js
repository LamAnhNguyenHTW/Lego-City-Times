import http from 'k6/http';
import { check } from 'k6';
import { envUrl } from './env.js';

export function getHomePage() {
  const baseUrl = envUrl('WEB_BASE_URL', 'https://localhost');
  const res = http.get(`${baseUrl}/`, {
    redirects: 0,
    headers: {
      Accept: 'text/html',
    },
    tags: {
      endpoint: 'web-home',
    },
  });

  check(res, {
    'web: status is 200': (r) => r.status === 200,
    'web: content-type is html': (r) => (r.headers['Content-Type'] || '').includes('text/html'),
  });

  return res;
}
