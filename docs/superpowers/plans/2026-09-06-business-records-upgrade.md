# 铺记经营记录与货架升级 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为铺记增加全部数据安全重置、货架类别筛选、按月流水、月/季/年经营总结，以及月度和自定义日期 CSV 导出。

**Architecture:** 以 `DateRange` 和成交成本快照统一所有统计口径；先实现纯 Kotlin 日期、汇总、分类和确认逻辑，再升级 SQLite 与仓库，最后接入 Compose 页面。经营概览和流水明细共用范围查询，重置使用独立仓库和单个数据库事务。

**Tech Stack:** Kotlin、Jetpack Compose Material 3、Android SQLiteOpenHelper、JUnit 4、AndroidX Instrumentation、Java 21。

---

## 文件职责

- `domain/Periods.kt`：自然月、自然季度、自然年和自定义日期范围。
- `domain/BusinessSummary.kt`：经营指标、毛利率和趋势计算。
- `domain/ProductCategories.kt`：类别归一化、排序和分组。
- `domain/HoldConfirmationGate.kt`：两秒按住确认状态。
- `data/PujiDatabase.kt`：数据库版本 2、成本快照迁移和日期索引。
- `data/InventoryRepository.kt`：保存成本快照并按范围读取流水。
- `data/BusinessSummaryRepository.kt`：按范围组装经营总结。
- `data/AppResetRepository.kt`：原子清空全部业务数据。
- `ui/records/*`：经营概览、月份流水和自定义导出。
- `ui/home/*`：数据管理入口和两秒确认弹窗。
- `ui/inventory/InventoryScreen.kt`：类别按钮和分组货架。

### Task 1: 日期周期模型

**Files:**
- Create: `app/src/main/java/cn/puji/app/domain/Periods.kt`
- Create: `app/src/test/java/cn/puji/app/domain/PeriodCalculatorTest.kt`

- [ ] **Step 1: 写失败测试**

```kotlin
class PeriodCalculatorTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test fun month_startsAtFirstDayAndEndsAtNextMonth() {
        val range = PeriodCalculator.range(PeriodKind.MONTH, LocalDate.of(2026, 9, 6), zone)
        assertEquals("2026 年 9 月", range.label)
        assertEquals(LocalDate.of(2026, 9, 1), localDate(range.startInclusive))
        assertEquals(LocalDate.of(2026, 10, 1), localDate(range.endExclusive))
    }

    @Test fun custom_includesWholeEndDateAndRejectsReverse() {
        val range = PeriodCalculator.custom(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 6), zone).getOrThrow()
        assertEquals(LocalDate.of(2026, 9, 7), localDate(range.endExclusive))
        assertTrue(PeriodCalculator.custom(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 6), zone).isFailure)
    }

    private fun localDate(value: Long) = Instant.ofEpochMilli(value).atZone(zone).toLocalDate()
}
```

- [ ] **Step 2: 确认测试因新 API 不存在而失败**

```bash
./gradlew -Dorg.gradle.java.home='/Applications/Android Studio.app/Contents/jbr/Contents/Home' testDebugUnitTest --tests cn.puji.app.domain.PeriodCalculatorTest
```

Expected: unresolved references to `PeriodCalculator`, `PeriodKind`, and `DateRange`.

- [ ] **Step 3: 实现最小日期 API**

```kotlin
enum class PeriodKind { MONTH, QUARTER, YEAR }

data class DateRange(val startInclusive: Long, val endExclusive: Long, val label: String) {
    init { require(startInclusive < endExclusive) }
    fun contains(timestamp: Long) = timestamp in startInclusive until endExclusive
}

object PeriodCalculator {
    fun range(kind: PeriodKind, anchor: LocalDate, zone: ZoneId = ZoneId.systemDefault()): DateRange
    fun custom(start: LocalDate, endInclusive: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Result<DateRange>
    fun previous(kind: PeriodKind, anchor: LocalDate): LocalDate
    fun next(kind: PeriodKind, anchor: LocalDate): LocalDate
    fun isCurrent(kind: PeriodKind, anchor: LocalDate, today: LocalDate = LocalDate.now()): Boolean
}
```

季度首月使用 `((anchor.monthValue - 1) / 3) * 3 + 1`；所有范围转换为左闭右开的毫秒区间。

- [ ] **Step 4: 运行测试并提交**

```bash
./gradlew -Dorg.gradle.java.home='/Applications/Android Studio.app/Contents/jbr/Contents/Home' testDebugUnitTest --tests cn.puji.app.domain.PeriodCalculatorTest
git add app/src/main/java/cn/puji/app/domain/Periods.kt app/src/test/java/cn/puji/app/domain/PeriodCalculatorTest.kt
git commit -m "feat: add reporting period ranges"
```

Expected: all period tests pass before commit.

### Task 2: 成本快照、数据库迁移和范围流水

**Files:**
- Modify: `app/src/main/java/cn/puji/app/domain/Models.kt`
- Modify: `app/src/main/java/cn/puji/app/data/PujiDatabase.kt`
- Modify: `app/src/main/java/cn/puji/app/data/InventoryRepository.kt`
- Modify: `app/src/androidTest/java/cn/puji/app/data/InventoryRepositoryTest.kt`
- Create: `app/src/androidTest/java/cn/puji/app/data/PujiDatabaseMigrationTest.kt`

- [ ] **Step 1: 写成本快照和边界失败测试**

```kotlin
@Test fun outbound_keepsCostSnapshotAfterProductPriceChanges() {
    val product = products.list().first()
    inventory.commit(Cart.empty(DocumentType.OUTBOUND).add(product)).getOrThrow()
    products.save(product.copy(costPriceCents = product.costPriceCents + 100)).getOrThrow()
    assertEquals(product.costPriceCents, inventory.documents().single().lines.single().costPriceSnapshotCents)
}
```

迁移测试手动创建版本 1 数据库及一笔入货、一笔出货，设置 `PRAGMA user_version=1`，再由 `PujiDatabase` 打开；断言入货快照等于行单价、出货快照等于商品进货价，而且原商品和流水仍存在。范围查询测试用可注入时钟创建起点、范围内和终点三笔单据，只期待前两笔。

- [ ] **Step 2: 运行 Android 测试并确认失败**

```bash
./gradlew -Dorg.gradle.java.home='/Applications/Android Studio.app/Contents/jbr/Contents/Home' connectedDebugAndroidTest
```

Expected: compilation fails because cost snapshot, injectable clock, migration, and range query are missing.

- [ ] **Step 3: 扩展模型和版本 2 数据库**

`DocumentLine` 增加：

```kotlin
val costPriceSnapshotCents: Long = 0
val costTotalCents: Long get() = costPriceSnapshotCents * quantity
val grossProfitCents: Long get() = lineTotalCents - costTotalCents
```

新建表包含：

```sql
cost_price_snapshot_cents INTEGER NOT NULL DEFAULT 0
```

`DATABASE_VERSION = 2`。`onUpgrade` 在事务中 `ALTER TABLE`，入货行用原行单价回填，出货行用关联商品当前进货价回填，并创建：

```sql
CREATE INDEX idx_documents_type_created ON documents(type, created_at DESC)
```

- [ ] **Step 4: 保存快照并实现范围查询**

```kotlin
class InventoryRepository(
    private val database: PujiDatabase,
    private val nowProvider: () -> Long = System::currentTimeMillis,
)

fun documents(type: DocumentType? = null, range: DateRange? = null): List<StockDocument>
```

入货快照取实际行单价，出货快照取商品当前进货价。查询组合 `type = ?`、`created_at >= ?`、`created_at < ?`，取消 200 条上限，并读取新字段。

- [ ] **Step 5: 运行测试并提交**

```bash
./gradlew -Dorg.gradle.java.home='/Applications/Android Studio.app/Contents/jbr/Contents/Home' connectedDebugAndroidTest
git add app/src/main/java/cn/puji/app/domain/Models.kt app/src/main/java/cn/puji/app/data/PujiDatabase.kt app/src/main/java/cn/puji/app/data/InventoryRepository.kt app/src/androidTest/java/cn/puji/app/data
git commit -m "feat: preserve transaction cost snapshots"
```

Expected: migration and repository tests pass.

### Task 3: 经营总结与趋势

**Files:**
- Create: `app/src/main/java/cn/puji/app/domain/BusinessSummary.kt`
- Create: `app/src/main/java/cn/puji/app/data/BusinessSummaryRepository.kt`
- Create: `app/src/test/java/cn/puji/app/domain/BusinessSummaryCalculatorTest.kt`

- [ ] **Step 1: 写指标失败测试**

```kotlin
@Test fun calculate_reportsBothProfitDefinitions() {
    val summary = BusinessSummaryCalculator.calculate(documents)
    assertEquals(1_200, summary.salesRevenueCents)
    assertEquals(500, summary.purchaseExpenseCents)
    assertEquals(400, summary.grossProfitCents)
    assertEquals(700, summary.cashBalanceCents)
    assertEquals(3333, summary.grossMarginBasisPoints)
    assertEquals(3, summary.transactionCount)
}
```

另测零销售时毛利率为零、低价销售产生负毛利润，以及月度按日、季度和年度按月补齐空趋势桶。

- [ ] **Step 2: 运行测试并确认失败**

```bash
./gradlew -Dorg.gradle.java.home='/Applications/Android Studio.app/Contents/jbr/Contents/Home' testDebugUnitTest --tests cn.puji.app.domain.BusinessSummaryCalculatorTest
```

Expected: unresolved `BusinessSummaryCalculator`.

- [ ] **Step 3: 实现指标和仓库**

```kotlin
data class TrendPoint(val label: String, val revenueCents: Long, val expenseCents: Long)
data class BusinessSummary(
    val salesRevenueCents: Long = 0,
    val purchaseExpenseCents: Long = 0,
    val grossProfitCents: Long = 0,
    val cashBalanceCents: Long = 0,
    val grossMarginBasisPoints: Int = 0,
    val transactionCount: Int = 0,
    val trend: List<TrendPoint> = emptyList(),
)

object BusinessSummaryCalculator {
    fun calculate(documents: List<StockDocument>): BusinessSummary
    fun trend(documents: List<StockDocument>, kind: PeriodKind, range: DateRange, zone: ZoneId = ZoneId.systemDefault()): List<TrendPoint>
}
```

毛利率使用整数公式 `grossProfitCents * 10_000 / salesRevenueCents`。`BusinessSummaryRepository.summary(range, kind)` 读取范围内单据并合并指标和趋势。

- [ ] **Step 4: 运行全部单元测试并提交**

```bash
./gradlew -Dorg.gradle.java.home='/Applications/Android Studio.app/Contents/jbr/Contents/Home' testDebugUnitTest
git add app/src/main/java/cn/puji/app/domain/BusinessSummary.kt app/src/main/java/cn/puji/app/data/BusinessSummaryRepository.kt app/src/test/java/cn/puji/app/domain/BusinessSummaryCalculatorTest.kt
git commit -m "feat: calculate business summaries"
```

Expected: all unit tests pass.

### Task 4: 类别、重置与 CSV 核心逻辑

**Files:**
- Create: `app/src/main/java/cn/puji/app/domain/ProductCategories.kt`
- Create: `app/src/main/java/cn/puji/app/domain/HoldConfirmationGate.kt`
- Modify: `app/src/main/java/cn/puji/app/data/ProductRepository.kt`
- Create: `app/src/main/java/cn/puji/app/data/AppResetRepository.kt`
- Modify: `app/src/main/java/cn/puji/app/export/CsvExporter.kt`
- Create: `app/src/test/java/cn/puji/app/domain/ProductCategoriesTest.kt`
- Create: `app/src/test/java/cn/puji/app/domain/HoldConfirmationGateTest.kt`
- Modify: `app/src/test/java/cn/puji/app/export/CsvExporterTest.kt`
- Create: `app/src/androidTest/java/cn/puji/app/data/AppResetRepositoryTest.kt`

- [ ] **Step 1: 写类别、确认、重置和导出失败测试**

```kotlin
@Test fun categories_sortAndKeepUncategorizedLast() {
    val groups = ProductCategories.group(listOf(food, blankCategory, drink))
    assertEquals(listOf("食品", "饮料", "未分类"), groups.map { it.name })
}

@Test fun holdGate_completesOnlyOnceAfterTwoSeconds() {
    val gate = HoldConfirmationGate(2_000)
    gate.press(1_000)
    assertFalse(gate.tryComplete(2_999))
    assertTrue(gate.tryComplete(3_000))
    assertFalse(gate.tryComplete(3_500))
}
```

Android 重置测试先产生流水，执行重置后断言四张业务表为零；失败回滚测试创建阻止删除商品的临时触发器，断言调用失败且数据量不变。CSV 测试要求表头包含 `成本金额,毛利润`，出货写两列，入货毛利润为空。

- [ ] **Step 2: 运行测试并确认失败**

```bash
./gradlew -Dorg.gradle.java.home='/Applications/Android Studio.app/Contents/jbr/Contents/Home' testDebugUnitTest
./gradlew -Dorg.gradle.java.home='/Applications/Android Studio.app/Contents/jbr/Contents/Home' connectedDebugAndroidTest
```

Expected: new domain classes and reset repository are unresolved; CSV assertions fail.

- [ ] **Step 3: 实现类别和仓库查询**

```kotlin
data class ProductCategoryGroup(val name: String, val products: List<Product>)
data class ProductCategoryCount(val name: String, val count: Int)

object ProductCategories {
    fun normalized(value: String): String
    fun group(products: List<Product>): List<ProductCategoryGroup>
    fun counts(products: List<Product>): List<ProductCategoryCount>
}
```

`ProductRepository.list(query, category, includeInactive)` 同时组合搜索和类别 SQL 条件；`categories()` 从在售商品生成计数，“未分类”最后。

- [ ] **Step 4: 实现两秒门和原子重置**

`HoldConfirmationGate` 提供 `press(now)`, `release()`, `progress(now)`, `tryComplete(now)`。`AppResetRepository.resetAll()` 在一个事务中依次删除 `stock_movements`、`document_lines`、`documents`、`products`，再删除相应 `sqlite_sequence` 行；异常时不调用 `setTransactionSuccessful()`。

- [ ] **Step 5: 扩展 CSV**

流水表头固定为：

```text
单号,类型,时间,商品,条码,单价,数量,小计,成本金额,毛利润,单据总额,备注
```

出货写 `costTotalCents` 和 `grossProfitCents`；入货成本金额等于行小计且毛利润为空。保留 UTF-8 BOM 和现有转义。

- [ ] **Step 6: 运行测试并提交**

```bash
./gradlew -Dorg.gradle.java.home='/Applications/Android Studio.app/Contents/jbr/Contents/Home' testDebugUnitTest connectedDebugAndroidTest
git add app/src/main/java/cn/puji/app/domain app/src/main/java/cn/puji/app/data/ProductRepository.kt app/src/main/java/cn/puji/app/data/AppResetRepository.kt app/src/main/java/cn/puji/app/export/CsvExporter.kt app/src/test app/src/androidTest/java/cn/puji/app/data/AppResetRepositoryTest.kt
git commit -m "feat: add category reset and report exports"
```

Expected: all unit and Android tests pass.

### Task 5: Compose 页面

**Files:**
- Modify: `app/src/main/java/cn/puji/app/ui/home/HomeScreen.kt`
- Create: `app/src/main/java/cn/puji/app/ui/home/ResetDataDialog.kt`
- Modify: `app/src/main/java/cn/puji/app/ui/inventory/InventoryScreen.kt`
- Modify: `app/src/main/java/cn/puji/app/ui/records/RecordsScreen.kt`
- Create: `app/src/main/java/cn/puji/app/ui/records/LedgerScreen.kt`
- Create: `app/src/main/java/cn/puji/app/ui/records/CustomExportDialog.kt`
- Modify: `app/src/main/java/cn/puji/app/ui/components/PujiComponents.kt`

- [ ] **Step 1: 先改变页面参数并确认编译失败**

```kotlin
fun HomeScreen(..., onResetAll: () -> Unit)
fun InventoryScreen(..., categoryCounts: List<ProductCategoryCount>, selectedCategory: String?, onCategoryChange: (String?) -> Unit)
fun RecordsScreen(summary: BusinessSummary, kind: PeriodKind, range: DateRange, onKindChange: (PeriodKind) -> Unit, onPrevious: () -> Unit, onNext: () -> Unit, canGoNext: Boolean, onOpenLedger: () -> Unit)
fun LedgerScreen(range: DateRange, documents: List<StockDocument>, filter: DocumentType?, onFilterChange: (DocumentType?) -> Unit, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit, canGoNext: Boolean, onExportMonth: () -> Unit, onCustomExport: () -> Unit, onBack: () -> Unit)
```

```bash
./gradlew -Dorg.gradle.java.home='/Applications/Android Studio.app/Contents/jbr/Contents/Home' compileDebugKotlin
```

Expected: existing `PujiApp` calls no longer match and new screens are not wired.

- [ ] **Step 2: 实现首页和重置弹窗**

首页最后加入危险色描边“数据管理”卡。`ResetDataDialog` 使用 `AlertDialog`；按住按钮用 `pointerInput` 监听按下和松开，用 `Animatable.animateTo(1f, tween(2_000))` 显示进度，完成且仍按住时调用一次 `onConfirm`，提前松手归零。取消按钮始终独立可见。

- [ ] **Step 3: 实现类别货架**

搜索框下用 `LazyRow` 放置高度 48dp 的“全部 N”和类别计数按钮。列表调用 `ProductCategories.group(products)`，每组先显示类别标题和数量，再显示现有商品卡；搜索为空保留现有空状态。

- [ ] **Step 4: 实现经营概览和趋势**

三段按钮切换月/季/年；周期前后按钮至少 48dp。两列卡片显示收入、支出、毛利润、现金结余、毛利率和交易笔数。趋势图用 Compose `Canvas` 画收入与支出柱并提供文字图例；无数据显示说明。底部主按钮为“查阅流水与导出”。

- [ ] **Step 5: 实现流水与日期弹窗**

流水页显示月份前后按钮、全部/出货/入货筛选、倒序单据卡，以及两个导出按钮。`CustomExportDialog` 使用两个 Material 3 `DatePickerDialog` 选择首尾日期，并提供类型选择；日期倒置显示行内错误且禁用确认。

- [ ] **Step 6: 编译并提交 UI**

```bash
./gradlew -Dorg.gradle.java.home='/Applications/Android Studio.app/Contents/jbr/Contents/Home' compileDebugKotlin
git add app/src/main/java/cn/puji/app/ui
git commit -m "feat: redesign inventory and records screens"
```

Expected: Kotlin compilation succeeds.

### Task 6: 应用编排、版本与交付

**Files:**
- Modify: `app/src/main/java/cn/puji/app/ui/PujiApp.kt`
- Modify: `app/src/main/java/cn/puji/app/PujiApplication.kt`
- Modify: `app/src/main/java/cn/puji/app/MainActivity.kt`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: 注入仓库并接入页面状态**

`PujiApplication` 暴露 `BusinessSummaryRepository` 和 `AppResetRepository`。`PujiApp` 持有类别、经营周期与锚点、经营/流水内部页、流水月份、类型筛选、导出弹窗和重置弹窗状态。统一 `reload()` 刷新首页、类别、货架、经营总结和当前月份流水。`BackHandler` 从流水返回经营概览。

- [ ] **Step 2: 接入重置和导出**

重置成功后清空购物车、条码、搜索、类别和筛选状态，解除扫描锁，回到首页并刷新；失败只显示错误。月份导出使用当前月份和类型，日期导出使用 `PeriodCalculator.custom`；无数据不生成文件。分享失败捕获 `ActivityNotFoundException` 并显示可恢复提示。

- [ ] **Step 3: 升级版本并确认编译**

```kotlin
versionCode = 4
versionName = "1.1.0"
```

```bash
./gradlew -Dorg.gradle.java.home='/Applications/Android Studio.app/Contents/jbr/Contents/Home' compileDebugKotlin
```

Expected: compilation succeeds.

- [ ] **Step 4: 运行完整验证**

```bash
./gradlew -Dorg.gradle.java.home='/Applications/Android Studio.app/Contents/jbr/Contents/Home' test lint assembleRelease connectedDebugAndroidTest
```

Expected: unit tests、Android tests、lint 和 release build 成功；lint 不包含 error。

- [ ] **Step 5: 安装和设备验收**

```bash
/Users/francis/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/release/app-release.apk
/Users/francis/Library/Android/sdk/platform-tools/adb shell monkey -p cn.puji.app -c android.intent.category.LAUNCHER 1
```

确认旧数据迁移保留、类别筛选、月/季/年金额、月份流水、两种导出和两秒重置均符合规格；使用临时测试数据执行重置后首页为空。

- [ ] **Step 6: 复制 APK、校验并提交**

```bash
cp app/build/outputs/apk/release/app-release.apk dist/铺记-v1.1.0.apk
shasum -a 256 dist/铺记-v1.1.0.apk
git add app/src/main/java/cn/puji/app/PujiApplication.kt app/src/main/java/cn/puji/app/MainActivity.kt app/src/main/java/cn/puji/app/ui/PujiApp.kt app/build.gradle.kts
git commit -m "release: build Puji Android v1.1.0"
```
