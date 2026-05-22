import { postIndex } from './lib/search-index.js';
import { sleepDistributedRampUp } from './lib/ramp.js';

const VUS = 100;
const RAMP_UP_SECONDS = 1;

const payload = JSON.stringify({
  id: 'loadtest-article',
  title: 'Load Test Article',
  subtitle: 'k6 small',
  content: 'Hello from k6',
  author: 'LoadTest',
  slug: 'loadtest-article',
  status: 'PUBLISHED',
  publishedAt: '2026-05-21T00:00:00Z',
});

export const options = {
  discardResponseBodies: true,
  thresholds: {
    checks: ['rate==1.0'],
  },
  scenarios: {
    main: {
      executor: 'per-vu-iterations',
      vus: VUS,
      iterations: 1,
      maxDuration: '60s',
    },
  },
};

export default function () {
  sleepDistributedRampUp(VUS, RAMP_UP_SECONDS);
  postIndex(payload);
}
