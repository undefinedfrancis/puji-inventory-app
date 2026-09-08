import { DocumentType, PeriodKind } from '../model/Models';
import type { BusinessSummary, StockDocument, TrendPoint } from '../model/Models';

function grossProfitCents(document: StockDocument): number {
  if (document.type !== DocumentType.OUTBOUND) {
    return 0;
  }
  return document.lines.reduce((total: number, line) => {
    const revenue = line.unitPriceCents * line.quantity;
    const cost = line.costPriceSnapshotCents * line.quantity;
    return total + revenue - cost;
  }, 0);
}

function trendKey(timestamp: number, kind: PeriodKind): string {
  const date = new Date(timestamp);
  if (kind === PeriodKind.MONTH) {
    return `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`;
  }
  return `${date.getFullYear()}-${date.getMonth()}`;
}

function trendLabel(timestamp: number, kind: PeriodKind): string {
  const date = new Date(timestamp);
  return kind === PeriodKind.MONTH ? `${date.getDate()}日` : `${date.getMonth() + 1}月`;
}

function buildTrend(documents: StockDocument[], kind: PeriodKind): TrendPoint[] {
  const sorted = [...documents].sort((left: StockDocument, right: StockDocument) => left.createdAt - right.createdAt);
  const keys: string[] = [];
  const points = new Map<string, TrendPoint>();

  sorted.forEach((document: StockDocument) => {
    const key = trendKey(document.createdAt, kind);
    let point = points.get(key);
    if (point === undefined) {
      point = {
        label: trendLabel(document.createdAt, kind),
        revenueCents: 0,
        expenseCents: 0
      };
      points.set(key, point);
      keys.push(key);
    }
    if (document.type === DocumentType.OUTBOUND) {
      point.revenueCents += document.totalCents;
    } else {
      point.expenseCents += document.totalCents;
    }
  });

  return keys.map((key: string) => points.get(key) as TrendPoint);
}

export function calculateBusinessSummary(
  documents: StockDocument[],
  periodKind: PeriodKind
): BusinessSummary {
  let salesRevenueCents = 0;
  let purchaseExpenseCents = 0;
  let grossProfit = 0;

  documents.forEach((document: StockDocument) => {
    if (document.type === DocumentType.OUTBOUND) {
      salesRevenueCents += document.totalCents;
      grossProfit += grossProfitCents(document);
    } else {
      purchaseExpenseCents += document.totalCents;
    }
  });

  return {
    salesRevenueCents,
    purchaseExpenseCents,
    grossProfitCents: grossProfit,
    cashBalanceCents: salesRevenueCents - purchaseExpenseCents,
    grossMarginBasisPoints: salesRevenueCents === 0
      ? 0
      : Math.trunc(grossProfit * 10000 / salesRevenueCents),
    transactionCount: documents.length,
    trend: buildTrend(documents, periodKind)
  };
}
