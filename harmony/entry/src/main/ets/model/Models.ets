export interface Product {
  id: number;
  name: string;
  barcode: string;
  category: string;
  costPriceCents: number;
  salePriceCents: number;
  stockQuantity: number;
  lowStockThreshold: number;
  note: string;
  active: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface CategoryCount {
  category: string;
  count: number;
}

export enum DocumentType {
  INBOUND = 'INBOUND',
  OUTBOUND = 'OUTBOUND'
}

export interface DocumentLine {
  id: number;
  documentId: number;
  productId: number;
  productName: string;
  barcode: string;
  unitPriceCents: number;
  costPriceSnapshotCents: number;
  quantity: number;
  lineTotalCents: number;
}

export interface StockDocument {
  id: number;
  type: DocumentType;
  totalCents: number;
  note: string;
  createdAt: number;
  lines: DocumentLine[];
}

export interface DashboardStats {
  todaySalesCents: number;
  todayItemCount: number;
  productCount: number;
  lowStockCount: number;
  inventoryValueCents: number;
}

export enum PeriodKind {
  MONTH = 'MONTH',
  QUARTER = 'QUARTER',
  YEAR = 'YEAR'
}

export interface DateRange {
  startInclusive: number;
  endExclusive: number;
}

export interface TrendPoint {
  label: string;
  revenueCents: number;
  expenseCents: number;
}

export interface BusinessSummary {
  salesRevenueCents: number;
  purchaseExpenseCents: number;
  grossProfitCents: number;
  cashBalanceCents: number;
  grossMarginBasisPoints: number;
  transactionCount: number;
  trend: TrendPoint[];
}
