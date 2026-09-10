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

test('atomic first inbound reuses the known zero stock without a transactional reread', async () => {
  const source = await readFile(new URL('../entry/src/main/ets/data/InventoryRepository.ets', import.meta.url), 'utf8');
  assert.match(source, /persistDocument\(store, cart, '', saved\.id\)/);
  assert.match(source, /line\.product\.id === knownZeroStockProductId\s*\? 0\s*:\s*await this\.currentStock/);
});

test('reset failures are surfaced to the user', async () => {
  const source = await readFile(new URL('../entry/src/main/ets/state/PujiStore.ets', import.meta.url), 'utf8');
  assert.match(source, /清除失败/);
  assert.match(source, /invalidatePendingReads\(\)/);
});

test('reset, scanning and stock writes share one exclusive operation guard', async () => {
  const storeSource = await readFile(new URL('../entry/src/main/ets/state/PujiStore.ets', import.meta.url), 'utf8');
  const homeSource = await readFile(new URL('../entry/src/main/ets/pages/HomePage.ets', import.meta.url), 'utf8');
  const guardCalls = storeSource.match(/tryBeginExclusiveOperation\(\)/g) ?? [];

  assert.ok(guardCalls.length >= 4);
  assert.match(storeSource, /finally \{\s+this\.invalidatePendingReads\(\);\s+this\.busy = false;/);
  assert.match(storeSource, /setTab\(index: number\): void \{\s+if \(this\.busy\)/);
  assert.match(homeSource, /\.enabled\(!this\.store\.busy\)/);
});

test('dashboard reads its fixed aggregate columns by position', async () => {
  const source = await readFile(new URL('../entry/src/main/ets/data/ReportingRepository.ets', import.meta.url), 'utf8');
  assert.match(source, /todaySalesCents: resultSet\.getLong\(0\)/);
  assert.match(source, /inventoryValueCents: resultSet\.getLong\(4\)/);
});

test('home inventory metrics derive from the same observable product collection as low-stock alerts', async () => {
  const homeSource = await readFile(new URL('../entry/src/main/ets/pages/HomePage.ets', import.meta.url), 'utf8');
  const storeSource = await readFile(new URL('../entry/src/main/ets/state/PujiStore.ets', import.meta.url), 'utf8');
  assert.match(homeSource, /this\.inventoryValueCents\(\)/);
  assert.match(homeSource, /this\.store\.products\.length/);
  assert.match(homeSource, /this\.lowStockProducts\(\)\.length/);
  assert.match(storeSource, /inventoryValueCents: number = 0/);
  assert.match(storeSource, /this\.inventoryValueCents = dashboard\.inventoryValueCents/);
});

test('reusable metric cards declare reactive one-way properties', async () => {
  const source = await readFile(new URL('../entry/src/main/ets/components/CommonComponents.ets', import.meta.url), 'utf8');
  assert.match(source, /export struct MetricCard \{\s+@Prop label:/);
  assert.match(source, /@Prop value:/);
  assert.match(source, /@Prop detail:/);
  assert.match(source, /@Prop emphasis:/);
});
