import { postIndex } from './lib/search-index.js';
import { sleepDistributedRampUp } from './lib/ramp.js';

const VUS = 1000;
const RAMP_UP_SECONDS = 5;

const payload = open('./payloads/article-index-5mb.json');

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
      maxDuration: '600s',
    },
  },
};

export default function () {
  sleepDistributedRampUp(VUS, RAMP_UP_SECONDS);
  postIndex(payload, { allow429: false });
}
