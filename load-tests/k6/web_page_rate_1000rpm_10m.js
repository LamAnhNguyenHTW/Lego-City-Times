import { getHomePage } from './lib/web.js';

export const options = {
  discardResponseBodies: true,
  thresholds: {
    checks: ['rate==1.0'],
  },
  scenarios: {
    main: {
      executor: 'constant-arrival-rate',
      rate: 1000,
      timeUnit: '1m',
      duration: '10m',
      preAllocatedVUs: 100,
      maxVUs: 400,
    },
  },
};

export default function () {
  getHomePage();
}
