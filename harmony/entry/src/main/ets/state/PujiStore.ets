import { common } from '@kit.AbilityKit';
import { Cart } from '../domain/Cart';
import { LatestRequestGate } from '../domain/LatestRequestGate';
import { customDateRange, monthRange, quarterRange, shiftPeriodAnchor, yearRange } from '../domain/Periods';
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
  DateRange,
  Product,
  StockDocument
} from '../model/Models';

export enum ScanOutcome {
  IGNORED = 'IGNORED',
  KNOWN = 'KNOWN',
  UNKNOWN = 'UNKNOWN'
}

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
  todaySalesCents: number = 0;
  todayItemCount: number = 0;
  productCount: number = 0;
  lowStockCount: number = 0;
  inventoryValueCents: number = 0;
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
  private readonly productsRequestGate: LatestRequestGate = new LatestRequestGate();
  private readonly dashboardRequestGate: LatestRequestGate = new LatestRequestGate();
  private readonly ledgerRequestGate: LatestRequestGate = new LatestRequestGate();
  private readonly summaryRequestGate: LatestRequestGate = new LatestRequestGate();
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
    if (this.busy) {
      this.message = '正在处理，请稍候';
      return;
    }
    this.selectedTab = index;
    if (index === 1) {
      this.reloadProducts();
    } else if (index === 3) {
      this.reloadSummary();
    }
  }

  setDocumentType(type: DocumentType): void {
    if (this.busy) {
      return;
    }
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
    const requestVersion = this.productsRequestGate.begin();
    try {
      const products = await this.productRepository.list('', '');
      const categories = await this.productRepository.categoryCounts();
      if (this.productsRequestGate.isLatest(requestVersion)) {
        this.products = products;
        this.categories = categories;
      }
    } catch (error) {
      if (this.productsRequestGate.isLatest(requestVersion)) {
        this.message = `商品加载失败：${error}`;
      }
    }
  }

  async reloadDashboard(): Promise<void> {
    if (this.reportingRepository === null) {
      return;
    }
    const requestVersion = this.dashboardRequestGate.begin();
    try {
      const dashboard = await this.reportingRepository.dashboard();
      if (this.dashboardRequestGate.isLatest(requestVersion)) {
        this.todaySalesCents = dashboard.todaySalesCents;
        this.todayItemCount = dashboard.todayItemCount;
        this.productCount = dashboard.productCount;
        this.lowStockCount = dashboard.lowStockCount;
        this.inventoryValueCents = dashboard.inventoryValueCents;
      }
    } catch (error) {
      if (this.dashboardRequestGate.isLatest(requestVersion)) {
        this.message = `概况加载失败：${error}`;
      }
    }
  }

  async reloadSummary(): Promise<void> {
    if (this.reportingRepository === null) {
      return;
    }
    const requestVersion = this.summaryRequestGate.begin();
    const range = this.currentPeriodRange();
    const kind = this.periodKind;
    try {
      const summary = await this.reportingRepository.summary(range, kind);
      if (this.summaryRequestGate.isLatest(requestVersion)) {
        this.summary = summary;
      }
    } catch (error) {
      if (this.summaryRequestGate.isLatest(requestVersion)) {
        this.message = `汇总加载失败：${error}`;
      }
    }
  }

  async loadLedger(type: DocumentType | null = null): Promise<void> {
    if (this.inventoryRepository === null) {
      return;
    }
    const requestVersion = this.ledgerRequestGate.begin();
    const range = monthRange(this.ledgerAnchorMs);
    try {
      const documents = await this.inventoryRepository.documents(type, range);
      if (this.ledgerRequestGate.isLatest(requestVersion)) {
        this.ledgerDocuments = documents;
      }
    } catch (error) {
      if (this.ledgerRequestGate.isLatest(requestVersion)) {
        this.message = `流水加载失败：${error}`;
      }
    }
  }

  async moveLedgerMonth(offset: number, type: DocumentType | null = null): Promise<void> {
    const shifted = shiftPeriodAnchor(this.ledgerAnchorMs, PeriodKind.MONTH, offset);
    if (shifted <= Date.now()) {
      this.ledgerAnchorMs = shifted;
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
    if (this.busy) {
      return ScanOutcome.IGNORED;
    }
    const barcode = this.scanGate.accept(raw);
    if (barcode === null || this.productRepository === null) {
      return ScanOutcome.IGNORED;
    }
    if (!this.tryBeginExclusiveOperation()) {
      this.scanGate.cancel();
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
    } finally {
      this.busy = false;
    }
  }

  async saveUnknownInbound(product: Product, quantity: number): Promise<void> {
    if (this.productRepository === null || this.inventoryRepository === null) {
      throw new Error('应用尚未初始化');
    }
    if (!this.tryBeginExclusiveOperation()) {
      throw new Error('正在处理，请稍候');
    }
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
      await this.inventoryRepository.createProductAndCommitInbound(zeroStockProduct, quantity);
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
    if (!this.tryBeginExclusiveOperation()) {
      return null;
    }
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
    if (!this.tryBeginExclusiveOperation()) {
      return;
    }
    this.invalidatePendingReads();
    try {
      await this.resetRepository.clearAll();
      this.products = [];
      this.categories = [];
      this.todaySalesCents = 0;
      this.todayItemCount = 0;
      this.productCount = 0;
      this.lowStockCount = 0;
      this.inventoryValueCents = 0;
      this.summary = EMPTY_SUMMARY;
      this.ledgerDocuments = [];
      this.cart = Cart.empty(DocumentType.INBOUND);
      this.documentType = DocumentType.INBOUND;
      this.selectedTab = 0;
      this.pendingBarcode = '';
      this.scanGate.cancel();
      this.message = '全部记录已清除';
    } catch (error) {
      this.message = `清除失败：${error}`;
      throw new Error(`${error}`);
    } finally {
      this.invalidatePendingReads();
      this.busy = false;
    }
  }

  private tryBeginExclusiveOperation(): boolean {
    if (this.busy) {
      this.message = '正在处理，请稍候';
      return false;
    }
    this.busy = true;
    return true;
  }

  private invalidatePendingReads(): void {
    this.productsRequestGate.invalidate();
    this.dashboardRequestGate.invalidate();
    this.ledgerRequestGate.invalidate();
    this.summaryRequestGate.invalidate();
  }

  setPeriod(kind: PeriodKind): void {
    this.periodKind = kind;
    this.periodAnchorMs = Date.now();
    this.reloadSummary();
  }

  movePeriod(offset: number): void {
    const shifted = shiftPeriodAnchor(this.periodAnchorMs, this.periodKind, offset);
    if (shifted <= Date.now()) {
      this.periodAnchorMs = shifted;
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
