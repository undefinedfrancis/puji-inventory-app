import { DocumentType } from '../model/Models';
import type { Product, StockDocument } from '../model/Models';

const BOM = '\uFEFF';

function decimalMoney(cents: number): string {
  const sign = cents < 0 ? '-' : '';
  const absolute = Math.abs(Math.trunc(cents));
  return `${sign}${Math.floor(absolute / 100)}.${(absolute % 100).toString().padStart(2, '0')}`;
}

function twoDigits(value: number): string {
  return value.toString().padStart(2, '0');
}

function formatLocalTime(timestamp: number, date: Date): string {
  date.setTime(timestamp);
  return `${date.getFullYear()}-${twoDigits(date.getMonth() + 1)}-${twoDigits(date.getDate())} ` +
    `${twoDigits(date.getHours())}:${twoDigits(date.getMinutes())}`;
}

export function escapeCsv(value: string): string {
  const safeValue = /^[\u0000-\u0020]*[=+\-@]/.test(value) ? `'${value}` : value;
  if (!/[",\r\n]/.test(safeValue)) {
    return safeValue;
  }
  return `"${safeValue.replace(/"/g, '""')}"`;
}

export function exportProducts(products: Product[]): string {
  const rows: string[] = [
    '商品名称,条码,类别,进货价,销售价,库存,低库存阈值,备注'
  ];
  products.forEach((product: Product) => {
    rows.push([
      escapeCsv(product.name),
      escapeCsv(product.barcode),
      escapeCsv(product.category),
      decimalMoney(product.costPriceCents),
      decimalMoney(product.salePriceCents),
      product.stockQuantity.toString(),
      product.lowStockThreshold.toString(),
      escapeCsv(product.note)
    ].join(','));
  });
  return BOM + rows.join('\r\n');
}

export function exportDocuments(documents: StockDocument[]): string {
  const rows: string[] = [
    '单据号,类型,时间,商品名称,条码,数量,单价,金额,成本金额,毛利润,备注'
  ];
  const reusableDate = new Date(0);
  documents.forEach((document: StockDocument) => {
    document.lines.forEach((line) => {
      const lineTotal = line.unitPriceCents * line.quantity;
      const costTotal = line.costPriceSnapshotCents * line.quantity;
      const grossProfit = document.type === DocumentType.OUTBOUND
        ? decimalMoney(lineTotal - costTotal)
        : '';
      rows.push([
        document.id.toString(),
        document.type === DocumentType.INBOUND ? '入货' : '出货',
        formatLocalTime(document.createdAt, reusableDate),
        escapeCsv(line.productName),
        escapeCsv(line.barcode),
        line.quantity.toString(),
        decimalMoney(line.unitPriceCents),
        decimalMoney(lineTotal),
        decimalMoney(costTotal),
        grossProfit,
        escapeCsv(document.note)
      ].join(','));
    });
  });
  return BOM + rows.join('\r\n');
}
