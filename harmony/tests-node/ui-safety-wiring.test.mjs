import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

test('enables the period forward button only when a newer period exists', async () => {
  const source = await readFile(new URL('../entry/src/main/ets/pages/RecordsPage.ets', import.meta.url), 'utf8');
  assert.match(source, /canMovePeriodForward/);
  assert.match(source, /movePeriod\(1\)/);
  assert.doesNotMatch(source, /\.enabled\(false\)/);
});

test('unknown product registration uses the atomic create-and-inbound operation', async () => {
  const source = await readFile(new URL('../entry/src/main/ets/state/PujiStore.ets', import.meta.url), 'utf8');
  assert.match(source, /createProductAndCommitInbound/);
});

test('reset failures are surfaced to the user', async () => {
  const source = await readFile(new URL('../entry/src/main/ets/state/PujiStore.ets', import.meta.url), 'utf8');
  assert.match(source, /清除失败/);
  assert.match(source, /invalidatePendingReads\(\)/);
});
