import assert from 'node:assert/strict';
import test from 'node:test';
import { calculateBusinessSummary } from '../entry/src/main/ets/domain/BusinessSummary.ets';
import { DocumentType, PeriodKind } from '../entry/src/main/ets/model/Models.ets';

function line(unitPriceCents, costPriceCents, quantity) {
  return {
    id: 1,
    documentId: 1,
    productId: 1,
    productName: '测试商品',
    barcode: '6901',
    unitPriceCents,
    costPriceSnapshotCents: costPriceCents,
    quantity,
    lineTotalCents: unitPriceCents * quantity
  };
}

function document(id, type, totalCents, lines, day) {
  return {
    id,
    type,
    totalCents,
    note: '',
    createdAt: new Date(2026, 8, day, 10).getTime(),
    lines
  };
}

test('calculates revenue, expense, gross profit and cash balance', () => {
  const documents = [
    document(1, DocumentType.INBOUND, 1000, [line(500, 500, 2)], 2),
    document(2, DocumentType.OUTBOUND, 1600, [line(800, 500, 2)], 3)
  ];
  const summary = calculateBusinessSummary(documents, PeriodKind.MONTH);
  assert.equal(summary.salesRevenueCents, 1600);
  assert.equal(summary.purchaseExpenseCents, 1000);
  assert.equal(summary.grossProfitCents, 600);
  assert.equal(summary.cashBalanceCents, 600);
  assert.equal(summary.grossMarginBasisPoints, 3750);
  assert.equal(summary.transactionCount, 2);
});

test('returns a zero margin when there are no outbound sales', () => {
  const summary = calculateBusinessSummary([
    document(1, DocumentType.INBOUND, 1000, [line(500, 500, 2)], 2)
  ], PeriodKind.MONTH);
  assert.equal(summary.salesRevenueCents, 0);
  assert.equal(summary.grossMarginBasisPoints, 0);
});

test('preserves negative gross profit and margin', () => {
  const summary = calculateBusinessSummary([
    document(1, DocumentType.OUTBOUND, 600, [line(300, 500, 2)], 2)
  ], PeriodKind.MONTH);
  assert.equal(summary.grossProfitCents, -400);
  assert.equal(summary.grossMarginBasisPoints, -6666);
});

test('groups monthly summaries into daily trend points', () => {
  const summary = calculateBusinessSummary([
    document(1, DocumentType.OUTBOUND, 300, [line(300, 200, 1)], 2),
    document(2, DocumentType.OUTBOUND, 400, [line(400, 200, 1)], 2),
    document(3, DocumentType.INBOUND, 250, [line(250, 250, 1)], 3)
  ], PeriodKind.MONTH);
  assert.deepEqual(summary.trend, [
    { label: '2日', revenueCents: 700, expenseCents: 0 },
    { label: '3日', revenueCents: 0, expenseCents: 250 }
  ]);
});
