import assert from 'node:assert/strict';
import test from 'node:test';
import { PUJI_COLORS, PUJI_LAYOUT, PUJI_TABS } from '../entry/src/main/ets/theme/PujiTheme.ets';

test('defines the four approved business tabs in order', () => {
  assert.deepEqual(PUJI_TABS.map((tab) => tab.label), ['首页', '货架', '扫码', '记录']);
});

test('keeps scan as the warm accent action', () => {
  assert.equal(PUJI_TABS[2].accent, true);
  assert.equal(PUJI_COLORS.scanAccent, '#E8793E');
});

test('keeps every interaction target at least 48vp', () => {
  assert.equal(PUJI_LAYOUT.minimumTarget >= 48, true);
});
