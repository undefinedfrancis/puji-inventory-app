import { common } from '@kit.AbilityKit';
import { Cart } from '../domain/Cart';
import { customDateRange, monthRange, quarterRange, yearRange } from '../domain/Periods';
import { ScanGate } from '../domain/ScanGate';
import { InventoryRepository } from '../data/InventoryRepository';
import { ProductRepository } from '../data/ProductRepository';
import { PujiDatabase } from '../data/PujiDatabase';
import { ReportingRepository } from '../data/ReportingRepository';
import { ResetRepository } from '../data/ResetRepository';
import { exportDocuments, exportProducts } from '../export/CsvExporter';
import { DocumentType, PeriodKind } from '../model/Models';
import type {
  BusinessSummary,
  CategoryCount,
  DashboardStats,
  DateRange,
  Product,
  StockDocument
} from '../model/Models';

export enum ScanOutcome {
  IGNORED = 'IGNORED',
  KNOWN = 'KNOWN',
  UNKNOWN = 'UNKNOWN'
}

const EMPTY_DASHBOARD: DashboardStats = {
  todaySalesCents: 0,
  todayItemCount: 0,
  productCount: 0,
  lowStockCount: 0,
  inventoryValueCents: 0
};

const EMPTY_SUMMARY: BusinessSummary = {
  salesRevenueCents: 0,
  purchaseExpenseCents: 0,
  grossProfitCents: 0,
  cashBalanceCents: 0,
  grossMarginBasisPoints: 0,
  transactionCount: 0,
  trend: []
};

@Observed
export class PujiStore {
  selectedTab: number = 0;
  products: Product[] = [];
  categories: CategoryCount[] = [];
  dashboard: DashboardStats = EMPTY_DASHBOARD;
  summary: BusinessSummary = EMPTY_SUMMARY;
  ledgerDocuments: StockDocument[] = [];
  ledgerAnchorMs: number = Date.now();
  cart: Cart = Cart.empty(DocumentType.INBOUND);
  documentType: DocumentType = DocumentType.INBOUND;
  periodKind: PeriodKind = PeriodKind.MONTH;
  periodAnchorMs: number = Date.now();
  pendingBarcode: string = '';
  busy: boolean = false;
  message: string = '';

  private readonly scanGate: ScanGate = new ScanGate();
  private productRepository: ProductRepository | null = null;
  private inventoryRepository: InventoryRepository | null = null;
  private reportingRepository: ReportingRepository | null = null;
  private resetRepository: ResetRepository | null = null;

  async initialize(context: common.Context): Promise<void> {
    if (this.productRepository !== null) {
      return;
    }
    const database = new PujiDatabase(context);
    this.productRepository = new ProductRepository(database);
    this.inventoryRepository = new InventoryRepository(database);
    this.reportingRepository = new ReportingRepository(database, this.inventoryRepository);
    this.resetRepository = new ResetRepository(database);
    await this.reloadAll();
  }

  setTab(index: number): void {
    this.selectedTab = index;
    if (index === 1) {
      this.reloadProducts();
    } else if (index === 3) {
      this.reloadSummary();
    }
  }

  setDocumentType(type: DocumentType): void {
    if (this.documentType === type) {
      return;
    }
    this.documentType = type;
    this.cart = Cart.empty(type);
    this.cancelPendingScan();
  }

  async reloadAll(): Promise<void> {
    await this.reloadProducts();
    await this.reloadDashboard();
    await this.reloadSummary();
  }

  async reloadProducts(): Promise<void> {
    if (this.productRepository === null) {
      return;
    }
    this.products = await this.productRepository.list('', '');
    this.categories = await this.productRepository.categoryCounts();
  }

  async reloadDashboard(): Promise<void> {
    if (this.reportingRepository === null) {
      return;
    }
    this.dashboard = await this.reportingRepository.dashboard();
  }

  async reloadSummary(): Promise<void> {
    if (this.reportingRepository === null) {
      return;
    }
    this.summary = await this.reportingRepository.summary(this.currentPeriodRange(), this.periodKind);
  }

  async loadLedger(type: DocumentType | null = null): Promise<void> {
    if (this.inventoryRepository === null) {
      return;
    }
    this.ledgerDocuments = await this.inventoryRepository.documents(type, monthRange(this.ledgerAnchorMs));
  }

  async moveLedgerMonth(offset: number, type: DocumentType | null = null): Promise<void> {
    const anchor = new Date(this.ledgerAnchorMs);
    anchor.setMonth(anchor.getMonth() + offset);
    if (anchor.getTime() <= Date.now()) {
      this.ledgerAnchorMs = anchor.getTime();
      await this.loadLedger(type);
    }
  }

  async inventoryCsv(): Promise<string> {
    if (this.productRepository === null) {
      return exportProducts([]);
    }
    return exportProducts(await this.productRepository.list('', ''));
  }

  async monthDocumentsCsv(): Promise<string> {
    if (this.inventoryRepository === null) {
      return exportDocuments([]);
    }
    return exportDocuments(await this.inventoryRepository.documents(null, monthRange(this.ledgerAnchorMs)));
  }

  async customDocumentsCsv(startMs: number, endMs: number): Promise<string | null> {
    if (this.inventoryRepository === null) {
      return exportDocuments([]);
    }
    const range = customDateRange(startMs, endMs);
    if (range === null) {
      return null;
    }
    return exportDocuments(await this.inventoryRepository.documents(null, range));
  }

  async scanBarcode(raw: string): Promise<ScanOutcome> {
    const barcode = this.scanGate.accept(raw);
    if (barcode === null || this.productRepository === null) {
      return ScanOutcome.IGNORED;
    }
    try {
      const product = await this.productRepository.findByBarcode(barcode);
      if (product === null) {
        if (this.documentType === DocumentType.OUTBOUND) {
          this.message = '未找到该商品，请先切换到入货登记';
          this.scanGate.complete();
          return ScanOutcome.UNKNOWN;
        }
        this.pendingBarcode = barcode;
        return ScanOutcome.UNKNOWN;
      }
      this.cart = this.cart.add(product, 1);
      this.scanGate.complete();
      return ScanOutcome.KNOWN;
    } catch (error) {
      this.message = `${error}`;
      this.scanGate.cancel();
      return ScanOutcome.IGNORED;
    }
  }

  async saveUnknownInbound(product: Product, quantity: number): Promise<void> {
    if (this.productRepository === null || this.inventoryRepository === null) {
      throw new Error('应用尚未初始化');
    }
    this.busy = true;
    try {
      const zeroStockProduct: Product = {
        id: product.id,
        name: product.name,
        barcode: this.pendingBarcode,
        category: product.category,
        costPriceCents: product.costPriceCents,
        salePriceCents: product.salePriceCents,
        stockQuantity: 0,
        lowStockThreshold: product.lowStockThreshold,
        note: product.note,
        active: true,
        createdAt: product.createdAt,
        updatedAt: product.updatedAt
      };
      const saved = await this.productRepository.save(zeroStockProduct);
      const exactCart = Cart.empty(DocumentType.INBOUND).add(saved, quantity);
      await this.inventoryRepository.commit(exactCart);
      this.pendingBarcode = '';
      this.cart = Cart.empty(this.documentType);
      this.scanGate.complete();
      this.message = `已入货 ${quantity} 件`;
      await this.reloadAll();
    } catch (error) {
      this.message = `${error}`;
      throw new Error(`${error}`);
    } finally {
      this.busy = false;
    }
  }

  cancelPendingScan(): void {
    this.pendingBarcode = '';
    this.scanGate.cancel();
  }

  increment(productId: number): void {
    this.cart = this.cart.increment(productId);
  }

  decrement(productId: number): void {
    this.cart = this.cart.decrement(productId);
  }

  async commitCart(): Promise<StockDocument | null> {
    if (this.inventoryRepository === null) {
      return null;
    }
    this.busy = true;
    try {
      const document = await this.inventoryRepository.commit(this.cart);
      this.cart = Cart.empty(this.documentType);
      this.message = this.documentType === DocumentType.INBOUND ? '入货登记成功' : '出货登记成功';
      await this.reloadAll();
      return document;
    } catch (error) {
      this.message = `${error}`;
      throw new Error(`${error}`);
    } finally {
      this.busy = false;
    }
  }

  async clearAll(): Promise<void> {
    if (this.resetRepository === null) {
      return;
    }
    this.busy = true;
    try {
      await this.resetRepository.clearAll();
      this.products = [];
      this.categories = [];
      this.dashboard = EMPTY_DASHBOARD;
      this.summary = EMPTY_SUMMARY;
      this.ledgerDocuments = [];
      this.cart = Cart.empty(DocumentType.INBOUND);
      this.documentType = DocumentType.INBOUND;
      this.selectedTab = 0;
      this.pendingBarcode = '';
      this.scanGate.cancel();
      this.message = '全部记录已清除';
    } finally {
      this.busy = false;
    }
  }

  setPeriod(kind: PeriodKind): void {
    this.periodKind = kind;
    this.periodAnchorMs = Date.now();
    this.reloadSummary();
  }

  movePeriod(offset: number): void {
    const anchor = new Date(this.periodAnchorMs);
    if (this.periodKind === PeriodKind.MONTH) {
      anchor.setMonth(anchor.getMonth() + offset);
    } else if (this.periodKind === PeriodKind.QUARTER) {
      anchor.setMonth(anchor.getMonth() + offset * 3);
    } else {
      anchor.setFullYear(anchor.getFullYear() + offset);
    }
    if (anchor.getTime() <= Date.now()) {
      this.periodAnchorMs = anchor.getTime();
      this.reloadSummary();
    }
  }

  currentPeriodRange(): DateRange {
    if (this.periodKind === PeriodKind.QUARTER) {
      return quarterRange(this.periodAnchorMs);
    }
    if (this.periodKind === PeriodKind.YEAR) {
      return yearRange(this.periodAnchorMs);
    }
    return monthRange(this.periodAnchorMs);
  }
}
