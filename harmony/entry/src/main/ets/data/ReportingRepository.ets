import { calculateBusinessSummary } from '../domain/BusinessSummary';
import { DocumentType, PeriodKind } from '../model/Models';
import type { BusinessSummary, DashboardStats, DateRange } from '../model/Models';
import { InventoryRepository } from './InventoryRepository';
import { PujiDatabase } from './PujiDatabase';

export class ReportingRepository {
  private readonly database: PujiDatabase;
  private readonly inventoryRepository: InventoryRepository;

  constructor(database: PujiDatabase, inventoryRepository: InventoryRepository) {
    this.database = database;
    this.inventoryRepository = inventoryRepository;
  }

  async dashboard(): Promise<DashboardStats> {
    const store = await this.database.getStore();
    const start = new Date();
    start.setHours(0, 0, 0, 0);
    const end = new Date(start.getFullYear(), start.getMonth(), start.getDate() + 1);
    const resultSet = await store.querySql(
      `SELECT
        COALESCE((SELECT SUM(total_cents) FROM documents WHERE type = 'OUTBOUND' AND created_at >= ? AND created_at < ?), 0) AS sales,
        COALESCE((SELECT SUM(dl.quantity) FROM document_lines dl JOIN documents d ON d.id = dl.document_id WHERE d.type = 'OUTBOUND' AND d.created_at >= ? AND d.created_at < ?), 0) AS item_count,
        COALESCE((SELECT COUNT(*) FROM products WHERE active = 1), 0) AS product_count,
        COALESCE((SELECT COUNT(*) FROM products WHERE active = 1 AND stock_quantity <= low_stock_threshold), 0) AS low_stock_count,
        COALESCE((SELECT SUM(cost_price_cents * stock_quantity) FROM products WHERE active = 1), 0) AS inventory_value`,
      [start.getTime(), end.getTime(), start.getTime(), end.getTime()]
    );
    try {
      resultSet.goToFirstRow();
      return {
        todaySalesCents: resultSet.getLong(resultSet.getColumnIndex('sales')),
        todayItemCount: resultSet.getLong(resultSet.getColumnIndex('item_count')),
        productCount: resultSet.getLong(resultSet.getColumnIndex('product_count')),
        lowStockCount: resultSet.getLong(resultSet.getColumnIndex('low_stock_count')),
        inventoryValueCents: resultSet.getLong(resultSet.getColumnIndex('inventory_value'))
      };
    } finally {
      resultSet.close();
    }
  }

  async summary(range: DateRange, kind: PeriodKind): Promise<BusinessSummary> {
    const documents = await this.inventoryRepository.documents(null, range);
    return calculateBusinessSummary(documents, kind);
  }

  async todayOutbound(range: DateRange): Promise<number> {
    const documents = await this.inventoryRepository.documents(DocumentType.OUTBOUND, range);
    return documents.reduce((total: number, document) => total + document.totalCents, 0);
  }
}
