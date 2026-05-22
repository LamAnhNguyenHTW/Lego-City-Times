import { sleep } from 'k6';

export function sleepDistributedRampUp(totalVus, rampUpSeconds) {
  const seconds = Number(rampUpSeconds || 0);
  if (!seconds || seconds <= 0) return;
  if (!totalVus || totalVus <= 1) return;

  const vuIndex = Number(__VU || 1) - 1;
  const maxIndex = totalVus - 1;
  const offset = (vuIndex / maxIndex) * seconds;
  sleep(offset);
}
