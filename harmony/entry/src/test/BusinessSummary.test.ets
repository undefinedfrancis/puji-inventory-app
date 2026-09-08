import { describe, expect, it } from '@ohos/hypium';
import { calculateBusinessSummary } from '../main/ets/domain/BusinessSummary';
import { DocumentType, PeriodKind, StockDocument } from '../main/ets/model/Models';

const documents: StockDocument[] = [{
  id: 1,
  type: DocumentType.INBOUND,
  totalCents: 1000,
  note: '',
  createdAt: new Date(2026, 8, 2).getTime(),
  lines: []
}, {
  id: 2,
  type: DocumentType.OUTBOUND,
  totalCents: 1600,
  note: '',
  createdAt: new Date(2026, 8, 3).getTime(),
  lines: [{
    id: 1,
    documentId: 2,
    productId: 1,
    productName: '测试商品',
    barcode: '6901',
    unitPriceCents: 800,
    costPriceSnapshotCents: 500,
    quantity: 2,
    lineTotalCents: 1600
  }]
}];

export default function businessSummaryTest(): void {
  describe('BusinessSummary', () => {
    it('calculatesTotals', 0, () => {
      const summary = calculateBusinessSummary(documents, PeriodKind.MONTH);
      expect(summary.salesRevenueCents).assertEqual(1600);
      expect(summary.purchaseExpenseCents).assertEqual(1000);
      expect(summary.grossProfitCents).assertEqual(600);
      expect(summary.cashBalanceCents).assertEqual(600);
      expect(summary.grossMarginBasisPoints).assertEqual(3750);
    });
  });
}
