import { postIndex } from './lib/search-index.js';
import { sleepDistributedRampUp } from './lib/ramp.js';

const VUS = 10;
const RAMP_UP_SECONDS = 0;

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
      maxDuration: '180s',
    },
  },
};

export default function () {
  sleepDistributedRampUp(VUS, RAMP_UP_SECONDS);
  postIndex(payload);
}
