# 铺记 HarmonyOS 原生版实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有仓库中增加一套可运行于 HarmonyOS 5.0.0 及以上设备的原生“铺记”应用，并产出经过测试的 Debug HAP。

**Architecture:** Android 工程保持不变，在 `harmony/` 中创建独立 Stage 模型 ArkTS 工程。ArkUI 页面通过应用状态服务调用领域逻辑和 ArkData `relationalStore` 仓库；扫码、数据库、导出和系统分享各自封装，避免平台 API 进入纯业务逻辑。

**Tech Stack:** DevEco Studio 26.0.0、HarmonyOS SDK 26.0.0、ArkTS、ArkUI、ArkData relationalStore、Scan Kit、Share Kit、Hvigor、ohpm、DECC-ArkTSUnit/Hypium。

---

## 文件结构

```text
harmony/
├── AppScope/
│   ├── app.json5
│   └── resources/base/{element,media}/
├── entry/
│   ├── build-profile.json5
│   ├── oh-package.json5
│   ├── src/main/
│   │   ├── module.json5
│   │   ├── ets/
│   │   │   ├── entryability/EntryAbility.ets
│   │   │   ├── model/Models.ets
│   │   │   ├── domain/{Money,Periods,Cart,ScanGate,BusinessSummary}.ets
│   │   │   ├── data/{PujiDatabase,ProductRepository,InventoryRepository,ReportingRepository,ResetRepository}.ets
│   │   │   ├── scanner/BarcodeScanner.ets
│   │   │   ├── export/CsvExporter.ets
│   │   │   ├── state/PujiStore.ets
│   │   │   ├── theme/PujiTheme.ets
│   │   │   ├── components/{CommonComponents,ProductEditor,ResetDialog}.ets
│   │   │   └── pages/{Index,HomePage,InventoryPage,ScannerPage,RecordsPage,LedgerPage}.ets
│   │   └── resources/base/{element,media,profile}/
│   ├── src/test/List.test.ets
│   ├── src/test/{Money,Periods,Cart,ScanGate,BusinessSummary,CsvExporter}.test.ets
│   └── src/ohosTest/ets/test/{List.test,Database,InventoryFlow}.test.ets
├── build-profile.json5
├── code-linter.json5
├── hvigor/hvigor-config.json5
├── hvigorfile.ts
├── oh-package.json5
└── README.md
```

每个领域文件只处理一种规则；每个仓库文件只管理一组数据职责；页面不直接执行 SQL。

### Task 1：安装环境并生成可编译空工程

**Files:**
- Create: `harmony/`（由 DevEco Studio 26.0.0 的 Empty Ability 模板生成）
- Create: `harmony/README.md`
- Modify: `.gitignore`

- [ ] **Step 1: 安装 DevEco Studio 26.0.0 正式版**

从华为开发者官方下载 macOS ARM 版本，将 `DevEco-Studio.app` 安装到 `/Applications`。首次启动时安装 HarmonyOS 26.0.0 SDK、Phone Emulator、Hvigor、ohpm 和内置 Node.js。

验证：

```bash
find '/Applications/DevEco-Studio.app' -maxdepth 1 -type d
```

Expected: 输出 `/Applications/DevEco-Studio.app`。

- [ ] **Step 2: 用 Empty Ability 模板生成工程**

创建 Stage 模型 ArkTS 工程，位置为 `harmony/`，应用名“铺记”，bundleName `cn.puji.app`，模块名 `entry`。根 `build-profile.json5` 明确包含：

```json5
{
  "app": {
    "signingConfigs": [],
    "products": [{
      "name": "default",
      "compatibleSdkVersion": "5.0.0(12)",
      "targetSdkVersion": "26.0.0",
      "runtimeOS": "HarmonyOS"
    }],
    "buildModeSet": [{ "name": "debug" }, { "name": "release" }]
  },
  "modules": [{
    "name": "entry",
    "srcPath": "./entry",
    "targets": [{ "name": "default", "applyToProducts": ["default"] }]
  }]
}
```

- [ ] **Step 3: 构建空工程**

Run:

```bash
cd harmony
./hvigorw assembleHap --mode module -p product=default -p module=entry@default -p buildMode=debug --no-daemon
```

Expected: `BUILD SUCCESSFUL`。随后使用 `find entry/build -name '*.hap' -type f` 解析模板生成的 Debug HAP 精确路径，并把该路径记录到 `harmony/README.md`。

- [ ] **Step 4: 增加忽略规则和环境说明**

`.gitignore` 追加：

```gitignore
harmony/.idea/
harmony/.hvigor/
harmony/**/build/
harmony/local.properties
harmony/.test/
dist/*.hap
```

`harmony/README.md` 记录 DevEco/SDK 版本、构建命令和签名限制。

- [ ] **Step 5: Commit**

```bash
git add .gitignore harmony
git commit -m "build: scaffold HarmonyOS app"
```

### Task 2：建立领域模型、金额和日期范围

**Files:**
- Create: `harmony/entry/src/main/ets/model/Models.ets`
- Create: `harmony/entry/src/main/ets/domain/Money.ets`
- Create: `harmony/entry/src/main/ets/domain/Periods.ets`
- Create: `harmony/entry/src/test/Money.test.ets`
- Create: `harmony/entry/src/test/Periods.test.ets`
- Modify: `harmony/entry/src/test/List.test.ets`

- [ ] **Step 1: 写失败的金额测试**

```ts
import { describe, expect, it } from '@ohos/hypium';
import { formatMoney, parseMoneyToCents } from '../main/ets/domain/Money';

export default function moneyTest(): void {
  describe('Money', () => {
    it('formatsIntegerCents', 0, () => expect(formatMoney(12345)).assertEqual('¥123.45'));
    it('parsesTwoDecimals', 0, () => expect(parseMoneyToCents('12.30')).assertEqual(1230));
    it('rejectsInvalidMoney', 0, () => expect(parseMoneyToCents('12.345')).assertNull());
  });
}
```

- [ ] **Step 2: 运行并确认失败**

Run: `cd harmony && ./hvigorw test -p module=entry -p coverage=false -p scope=Money`

Expected: FAIL，提示 `Money` 模块不存在。

- [ ] **Step 3: 实现金额和共享模型**

`Models.ets` 定义 `Product`、`CategoryCount`、`DocumentType`、`StockDocument`、`DocumentLine`、`DashboardStats`、`PeriodKind`、`DateRange`、`BusinessSummary` 和 `TrendPoint`。所有金额字段使用 `number` 表示整数分，所有时间字段使用 Unix 毫秒数。

`Money.ets` 提供完整签名：

```ts
export function formatMoney(cents: number): string;
export function parseMoneyToCents(raw: string): number | null;
export function lineTotal(unitPriceCents: number, quantity: number): number;
```

- [ ] **Step 4: 写日期失败测试并实现**

覆盖 2026 年 1 月、2026 年第二季度、闰年 2028 和跨年边界。实现：

```ts
export function monthRange(anchorMs: number): DateRange;
export function quarterRange(anchorMs: number): DateRange;
export function yearRange(anchorMs: number): DateRange;
export function customDateRange(startMs: number, endMs: number): DateRange | null;
```

所有范围使用设备本地时区的 `[startInclusive, endExclusive)`。

- [ ] **Step 5: 运行测试**

Run: `cd harmony && ./hvigorw test -p module=entry -p coverage=false`

Expected: Money 和 Periods 全部 PASS。

- [ ] **Step 6: Commit**

```bash
git add harmony/entry/src/main/ets/model harmony/entry/src/main/ets/domain harmony/entry/src/test
git commit -m "feat: add HarmonyOS business primitives"
```

### Task 3：实现购物车、库存规则和扫码门闩

**Files:**
- Create: `harmony/entry/src/main/ets/domain/Cart.ets`
- Create: `harmony/entry/src/main/ets/domain/ScanGate.ets`
- Create: `harmony/entry/src/test/Cart.test.ets`
- Create: `harmony/entry/src/test/ScanGate.test.ets`
- Modify: `harmony/entry/src/test/List.test.ets`

- [ ] **Step 1: 写扫码和数量回归测试**

测试必须明确覆盖：同一条码在锁定时被忽略；确认后恢复；新商品入货数量 5 最终购物车数量是 5 而不是 6。

```ts
it('addsExactScannedInboundQuantity', 0, () => {
  const cart = Cart.empty(DocumentType.INBOUND).add(product, 5);
  expect(cart.lines[0].quantity).assertEqual(5);
});

it('rejectsDuplicateCallbackWhileLocked', 0, () => {
  const gate = new ScanGate();
  expect(gate.accept('6901')).assertEqual('6901');
  expect(gate.accept('6901')).assertNull();
});
```

- [ ] **Step 2: 运行并确认失败**

Run: `cd harmony && ./hvigorw test -p module=entry -p coverage=false -p scope=Cart,ScanGate`

Expected: FAIL，提示类型尚未定义。

- [ ] **Step 3: 实现最小领域对象**

`Cart.ets` 暴露：

```ts
export class Cart {
  static empty(type: DocumentType): Cart;
  add(product: Product, quantity?: number): Cart;
  increment(productId: number): Cart;
  decrement(productId: number): Cart;
  updateUnitPrice(productId: number, cents: number): Cart;
  totalCents(): number;
}
```

`ScanGate.ets` 暴露 `accept(raw)`、`complete()`、`cancel()`、`isLocked()`；`accept` 先规范化条码，再原子锁定。

- [ ] **Step 4: 运行测试**

Run: `cd harmony && ./hvigorw test -p module=entry -p coverage=false`

Expected: 全部 PASS。

- [ ] **Step 5: Commit**

```bash
git add harmony/entry/src/main/ets/domain harmony/entry/src/test
git commit -m "feat: add HarmonyOS cart and scan gate"
```

### Task 4：创建 ArkData 数据库和商品仓库

**Files:**
- Create: `harmony/entry/src/main/ets/data/PujiDatabase.ets`
- Create: `harmony/entry/src/main/ets/data/ProductRepository.ets`
- Create: `harmony/entry/src/ohosTest/ets/test/Database.test.ets`
- Modify: `harmony/entry/src/ohosTest/ets/test/List.test.ets`

- [ ] **Step 1: 写失败的设备数据库测试**

测试初始化临时数据库，断言存在四张表，插入两个相同非空条码时第二次失败，并验证空条码允许多个商品。

- [ ] **Step 2: 运行并确认失败**

Run: 在 DevEco 模拟器执行 `Database.test.ets`。

Expected: FAIL，提示 `PujiDatabase` 不存在。

- [ ] **Step 3: 建立数据库**

`PujiDatabase.ets` 使用 `relationalStore.getRdbStore` 创建 `puji.db`，schemaVersion 为 1，依次创建：

```sql
products(id, name, barcode, category, cost_price_cents, sale_price_cents,
stock_quantity, low_stock_threshold, note, active, created_at, updated_at)
documents(id, type, total_cents, note, created_at)
document_lines(id, document_id, product_id, product_name_snapshot,
barcode_snapshot, unit_price_cents, cost_price_snapshot_cents, quantity, line_total_cents)
stock_movements(id, product_id, document_id, type, quantity_delta, stock_after, created_at)
```

创建条码唯一索引、商品名索引、单据时间索引和明细外键索引。SQL 写入全部使用参数绑定。

- [ ] **Step 4: 实现商品仓库**

```ts
export class ProductRepository {
  list(query?: string, category?: string, includeInactive?: boolean): Promise<Product[]>;
  findByBarcode(barcode: string): Promise<Product | null>;
  save(product: Product): Promise<Product>;
  categoryCounts(): Promise<CategoryCount[]>;
}
```

首次建库插入与 Android 版相同的三条演示商品。

- [ ] **Step 5: 运行设备测试并提交**

Expected: 建库、唯一条码、查询和更新测试全部 PASS。

```bash
git add harmony/entry/src/main/ets/data harmony/entry/src/ohosTest
git commit -m "feat: add HarmonyOS product database"
```

### Task 5：实现入出货事务、统计和安全重置

**Files:**
- Create: `harmony/entry/src/main/ets/data/InventoryRepository.ets`
- Create: `harmony/entry/src/main/ets/data/ReportingRepository.ets`
- Create: `harmony/entry/src/main/ets/data/ResetRepository.ets`
- Create: `harmony/entry/src/main/ets/domain/BusinessSummary.ets`
- Create: `harmony/entry/src/test/BusinessSummary.test.ets`
- Create: `harmony/entry/src/ohosTest/ets/test/InventoryFlow.test.ets`

- [ ] **Step 1: 写失败的交易测试**

覆盖：入货增加准确数量；出货扣减；库存不足整笔回滚；单据价格和成本快照不随商品修改；新商品扫码入货 5 件最终库存为 5。

- [ ] **Step 2: 运行并确认失败**

Expected: FAIL，提示 `InventoryRepository` 不存在。

- [ ] **Step 3: 实现事务提交**

```ts
export class InventoryRepository {
  commit(cart: Cart, note?: string): Promise<StockDocument>;
  documents(type: DocumentType | null, range: DateRange): Promise<StockDocument[]>;
}
```

`commit` 在同一事务内校验所有行、插入单据和明细、更新库存并插入库存变更。任一步失败调用 rollback。

- [ ] **Step 4: 写汇总失败测试并实现**

`calculateBusinessSummary(documents, periodKind)` 必须计算销售收入、进货支出、毛利润、现金结余、毛利率、交易笔数和趋势点，覆盖零销售与负毛利润。

- [ ] **Step 5: 实现统计仓库和重置仓库**

```ts
export class ReportingRepository {
  dashboard(): Promise<DashboardStats>;
  summary(range: DateRange, kind: PeriodKind): Promise<BusinessSummary>;
}

export class ResetRepository {
  clearAll(): Promise<void>;
}
```

`clearAll` 按依赖顺序删除四张业务表并重置自增序列，失败时回滚。

- [ ] **Step 6: 运行全部领域和设备测试**

Run: `cd harmony && ./hvigorw test -p module=entry -p coverage=false`

Expected: Local Tests 全部 PASS；模拟器中的 Database 与 InventoryFlow 全部 PASS。

- [ ] **Step 7: Commit**

```bash
git add harmony/entry/src/main/ets/data harmony/entry/src/main/ets/domain harmony/entry/src/test harmony/entry/src/ohosTest
git commit -m "feat: add HarmonyOS inventory transactions"
```

### Task 6：实现 CSV 导出和系统分享边界

**Files:**
- Create: `harmony/entry/src/main/ets/export/CsvExporter.ets`
- Create: `harmony/entry/src/test/CsvExporter.test.ets`
- Modify: `harmony/entry/src/test/List.test.ets`

- [ ] **Step 1: 写失败测试**

覆盖 UTF-8 BOM、中文、逗号、双引号、换行转义、库存列、流水成本列和入货毛利润留空。

- [ ] **Step 2: 实现纯 CSV 生成器**

```ts
export function exportProducts(products: Product[]): string;
export function exportDocuments(documents: StockDocument[]): string;
export function escapeCsv(value: string): string;
```

返回字符串首字符必须为 `\uFEFF`。

- [ ] **Step 3: 运行测试并提交**

Run: `cd harmony && ./hvigorw test -p module=entry -p coverage=false -p scope=CsvExporter`

Expected: PASS。

```bash
git add harmony/entry/src/main/ets/export harmony/entry/src/test
git commit -m "feat: add HarmonyOS CSV exports"
```

### Task 7：建立主题、应用状态和四栏导航

**Files:**
- Create: `harmony/entry/src/main/ets/theme/PujiTheme.ets`
- Create: `harmony/entry/src/main/ets/state/PujiStore.ets`
- Create: `harmony/entry/src/main/ets/components/CommonComponents.ets`
- Replace: `harmony/entry/src/main/ets/pages/Index.ets`
- Modify: `harmony/entry/src/main/ets/entryability/EntryAbility.ets`
- Modify: `harmony/entry/src/main/resources/base/element/{string,color}.json`
- Modify: `harmony/AppScope/resources/base/media/app_icon.*`

- [ ] **Step 1: 建立主题常量和复用组件**

迁移 Android 版的绿色、暖橙、背景色、卡片圆角和间距；实现 `PageHeader`、`MetricCard`、`SectionCard`、`CategoryChip`、`EmptyState` 和危险操作按钮。所有交互目标最小 48vp。

- [ ] **Step 2: 实现应用状态**

`PujiStore` 持有当前标签、商品、分类、购物车、经营周期、流水月份和消息状态，公开 `initialize()`、`reloadAll()`、`setTab()` 和各仓库调用方法。

- [ ] **Step 3: 实现底部四栏导航**

`Index.ets` 使用 `Tabs` 展示“首页、货架、扫码、记录”，扫码入口保持暖橙强调。切换到记录时刷新统计，切换到货架时刷新商品。

- [ ] **Step 4: 构建并预览**

Run: `cd harmony && ./hvigorw assembleHap --mode module -p product=default -p module=entry@default -p buildMode=debug --no-daemon`

Expected: BUILD SUCCESSFUL，无 ArkTS 严格模式错误。

- [ ] **Step 5: Commit**

```bash
git add harmony/AppScope harmony/entry/src/main
git commit -m "feat: add HarmonyOS app shell"
```

### Task 8：实现首页、货架和商品编辑

**Files:**
- Create: `harmony/entry/src/main/ets/pages/HomePage.ets`
- Create: `harmony/entry/src/main/ets/pages/InventoryPage.ets`
- Create: `harmony/entry/src/main/ets/components/ProductEditor.ets`
- Create: `harmony/entry/src/main/ets/components/ResetDialog.ets`

- [ ] **Step 1: 实现首页**

展示五项指标、出入货快捷按钮、低库存列表、关于信息和底部数据管理区。空数据时金额与数量显示零值。

- [ ] **Step 2: 实现货架**

实现搜索输入、横向类别筛选、全部模式分类分组、低库存标记、添加/编辑入口和库存导出入口。

- [ ] **Step 3: 实现商品编辑验证**

名称必填；条码去空格；金额最多两位小数；库存和阈值为非负整数；扫码入货模式“本次入货数量”至少为 1。保存期间禁用按钮。

- [ ] **Step 4: 实现按住 2 秒重置**

按下开始进度，提前松开归零，达到 2 秒只调用一次 `clearAll()`；成功后清空状态并回到首页，失败显示错误且不关闭弹窗。

- [ ] **Step 5: 模拟器验收并提交**

验收搜索、类别、编辑、低库存与重置保护，无内容溢出。

```bash
git add harmony/entry/src/main/ets/pages harmony/entry/src/main/ets/components
git commit -m "feat: add HarmonyOS home and inventory"
```

### Task 9：接入 Scan Kit 并完成扫码交易闭环

**Files:**
- Create: `harmony/entry/src/main/ets/scanner/BarcodeScanner.ets`
- Create: `harmony/entry/src/main/ets/pages/ScannerPage.ets`
- Modify: `harmony/entry/src/main/module.json5`
- Modify: `harmony/entry/src/main/ets/components/ProductEditor.ets`
- Modify: `harmony/entry/src/main/ets/state/PujiStore.ets`

- [ ] **Step 1: 配置相机权限和隐私说明**

在 `module.json5` 声明 `ohos.permission.CAMERA`，reason 指向“用于扫描商品条码”，usedScene 仅包含 `EntryAbility` 和前台使用。

- [ ] **Step 2: 封装 Scan Kit**

`BarcodeScanner` 提供：

```ts
export class BarcodeScanner {
  initialize(context: UIAbilityContext, surfaceId: string): Promise<void>;
  start(onResult: (barcode: string) => void): Promise<void>;
  pause(): Promise<void>;
  resume(): Promise<void>;
  release(): Promise<void>;
}
```

回调先交给 `ScanGate.accept`；只有返回非空条码时暂停扫描并触发业务层。

- [ ] **Step 3: 实现扫码页面**

提供入货/出货切换、相机预览、扫描框、手动条码、当前清单、数量增减、价格编辑、合计、确认和取消。

- [ ] **Step 4: 实现未知商品一次确认入货**

扫描未知条码后打开 `ProductEditor`。入货模式保存时：

1. 新商品以库存 0 保存；
2. 创建数量等于“本次入货数量”的临时购物车；
3. 事务提交入货单并增加库存；
4. 成功后关闭弹窗、清空临时清单、调用 `ScanGate.complete()` 并恢复相机；
5. 失败时保留准确数量供重试，不自动额外增加 1。

- [ ] **Step 5: 验证回归场景**

使用模拟器虚拟相机或手动条码：未知商品输入数量 5，只点击一次确认，货架库存必须为 5；弹窗期间重复识别不产生第二个弹窗；保存后可扫描下一件。

- [ ] **Step 6: Commit**

```bash
git add harmony/entry/src/main/module.json5 harmony/entry/src/main/ets/scanner harmony/entry/src/main/ets/pages/ScannerPage.ets harmony/entry/src/main/ets/components/ProductEditor.ets harmony/entry/src/main/ets/state/PujiStore.ets
git commit -m "feat: add HarmonyOS barcode transactions"
```

### Task 10：实现经营总结、流水筛选和分享

**Files:**
- Create: `harmony/entry/src/main/ets/pages/RecordsPage.ets`
- Create: `harmony/entry/src/main/ets/pages/LedgerPage.ets`
- Modify: `harmony/entry/src/main/ets/state/PujiStore.ets`
- Modify: `harmony/entry/src/main/ets/export/CsvExporter.ets`

- [ ] **Step 1: 实现经营总结页**

实现月度/季度/年度切换、前后周期、收入、支出、毛利润、现金结余、毛利率、笔数和趋势图；未来周期按钮禁用。

- [ ] **Step 2: 实现流水页**

实现月份前后切换、全部/入货/出货筛选、按时间倒序的单据卡片，以及返回经营总结。

- [ ] **Step 3: 实现导出范围和系统分享**

“导出当前月份”复用当前月份和类型筛选；“自定义时间导出”校验开始日不晚于结束日。将 CSV 写入缓存文件后通过系统分享能力打开目标选择面板，无数据时不写文件。

- [ ] **Step 4: 模拟器验收并提交**

验收自然月/季/年边界、空状态、负毛利润、月份筛选和分享面板。

```bash
git add harmony/entry/src/main/ets/pages harmony/entry/src/main/ets/state harmony/entry/src/main/ets/export
git commit -m "feat: add HarmonyOS business records"
```

### Task 11：完整测试、模拟器尺寸检查和问题修复

**Files:**
- Modify: `harmony/entry/src/test/*.test.ets`
- Modify: `harmony/entry/src/ohosTest/ets/test/*.test.ets`
- Modify: `harmony/entry/src/main/ets/domain/*.ets`
- Modify: `harmony/entry/src/main/ets/data/*.ets`
- Modify: `harmony/entry/src/main/ets/scanner/*.ets`
- Modify: `harmony/entry/src/main/ets/pages/*.ets`
- Modify: `harmony/entry/src/main/ets/components/*.ets`
- Create: `docs/HARMONYOS_TESTING.md`

- [ ] **Step 1: 运行全部本地单元测试**

Run: `cd harmony && ./hvigorw test -p module=entry -p coverage=false`

Expected: 0 failures。

- [ ] **Step 2: 运行数据库和交易设备测试**

在 HarmonyOS 模拟器运行全部 `ohosTest`，Expected: 0 failures。

- [ ] **Step 3: 运行代码检查**

使用 DevEco Studio Code Linter 检查整个 `harmony/entry`，修复所有 Error 和影响正确性的 Warning。

- [ ] **Step 4: 手动验收核心流程**

依次验收：新增商品、未知条码入货、已知条码出货、库存不足、类别筛选、月季年统计、月份流水、两类导出、按住重置。记录实际结果和测试环境到 `docs/HARMONYOS_TESTING.md`。

- [ ] **Step 5: Commit**

```bash
git add harmony docs/HARMONYOS_TESTING.md
git commit -m "test: verify HarmonyOS inventory app"
```

### Task 12：构建、复制 HAP、核对产物并推送

**Files:**
- Create: `dist/铺记-HarmonyOS-v1.0.0-debug.hap`（被 Git 忽略，仅本地交付）
- Modify: `README.md`
- Modify: `harmony/README.md`

- [ ] **Step 1: 更新安装与构建说明**

根 README 增加 Android 与 HarmonyOS 两套入口；`harmony/README.md` 说明 DevEco 打开、模拟器运行、Debug HAP 安装、真实设备签名和数据独立限制。

- [ ] **Step 2: 强制重新运行测试和构建**

```bash
cd harmony
./hvigorw test -p module=entry -p coverage=false
./hvigorw clean
./hvigorw assembleHap --mode module -p product=default -p module=entry@default -p buildMode=debug --no-daemon
```

Expected: 测试 0 failures，构建 `BUILD SUCCESSFUL`。

- [ ] **Step 3: 复制并校验 HAP**

将生成的已签名 Debug HAP 复制到 `dist/铺记-HarmonyOS-v1.0.0-debug.hap`，记录 SHA-256、包名、版本号和文件大小。

- [ ] **Step 4: 最终 Git 核对并提交文档**

```bash
git diff --check
git status --short
git add README.md harmony/README.md docs/HARMONYOS_TESTING.md
git commit -m "docs: add HarmonyOS build and install guide"
```

未跟踪的 `docs/icon-concepts/` 不加入提交。

- [ ] **Step 5: 推送私人仓库**

```bash
git push origin HEAD:main
```

Expected: 远程 `main` 与本地 HEAD 提交一致。
