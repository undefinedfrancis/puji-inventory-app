# 铺记 Android MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个离线可用、支持商品管理、摄像头扫码、入货、出货、库存提醒、流水查询和 CSV 导出的可安装 Android APK。

**Architecture:** 单 Activity Jetpack Compose 应用；领域层使用纯 Kotlin 模型和用例；数据层使用 SQLiteOpenHelper 与事务化 Repository；扫码层使用 CameraX 与本地打包的 ML Kit；UI 通过 ViewModel 和 StateFlow 驱动。Open Source POS 的商品、当前库存、库存流水、销售和入货单据模型被映射到本地数据表，同时保持未来同步接口可替换。

**Tech Stack:** Kotlin 2.3、Android Gradle Plugin 8.13.2、Gradle 8.13、compileSdk 36.1、minSdk 26、Jetpack Compose BOM 2026.06.00、Material 3、CameraX 1.6.2、ML Kit Barcode Scanning 17.3.0、JUnit 4、AndroidX Test。

---

## 文件结构

```text
app/src/main/java/cn/puji/app/
  MainActivity.kt                 应用入口、权限与导航
  PujiApplication.kt             依赖装配
  domain/Models.kt               商品、单据、购物车等领域模型
  domain/Money.kt                金额解析与格式化
  domain/Cart.kt                 购物车合并、数量与合计
  domain/InventoryService.kt     入货、出货业务约束
  data/PujiDatabase.kt           表结构、升级和 SQLite 连接
  data/ProductRepository.kt      商品查询与维护
  data/InventoryRepository.kt    单据、库存与流水事务
  data/DashboardRepository.kt    首页统计和历史记录
  scanner/BarcodeAnalyzer.kt     ML Kit 图像分析器
  scanner/ScannerController.kt   扫码节流和结果分发
  export/CsvExporter.kt          CSV 生成与分享
  ui/PujiApp.kt                  导航框架和底栏
  ui/theme/Color.kt              票据配色
  ui/theme/Theme.kt              Material 主题
  ui/theme/Type.kt               中文与数字排版
  ui/components/*.kt             票据卡片、金额、状态标签等复用控件
  ui/home/HomeScreen.kt           今日概览
  ui/inventory/InventoryScreen.kt 库存列表
  ui/product/ProductEditor.kt     商品新增/编辑
  ui/scanner/ScannerScreen.kt     入货/出货工作台
  ui/records/RecordsScreen.kt     单据记录和详情
  ui/about/AboutScreen.kt         开源归属和版本
```

## Task 1: 项目骨架与可重复构建

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/cn/puji/app/MainActivity.kt`
- Create: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/cn/puji/app/SmokeTest.kt`

- [ ] **Step 1: 创建最小失败测试**

```kotlin
class SmokeTest {
    @Test fun applicationName_isPuji() {
        assertEquals("铺记", AppIdentity.displayName)
    }
}
```

- [ ] **Step 2: 运行 `./gradlew testDebugUnitTest`，确认因 `AppIdentity` 不存在而失败**

- [ ] **Step 3: 创建 Gradle/Android 骨架和最小实现**

```kotlin
object AppIdentity {
    const val displayName = "铺记"
    const val packageName = "cn.puji.app"
}
```

- [ ] **Step 4: 运行 `./gradlew testDebugUnitTest assembleDebug`，确认测试和空壳 APK 构建通过**

- [ ] **Step 5: 提交 `build: scaffold native Android application`**

## Task 2: 领域模型、金额和购物车

**Files:**
- Create: `app/src/main/java/cn/puji/app/domain/Models.kt`
- Create: `app/src/main/java/cn/puji/app/domain/Money.kt`
- Create: `app/src/main/java/cn/puji/app/domain/Cart.kt`
- Test: `app/src/test/java/cn/puji/app/domain/MoneyTest.kt`
- Test: `app/src/test/java/cn/puji/app/domain/CartTest.kt`

- [ ] **Step 1: 编写金额测试，覆盖 `3`、`3.5`、`3.50`、空值、负数和超过两位小数**

```kotlin
@Test fun parseYuan_usesIntegerCents() {
    assertEquals(350L, Money.parseYuan("3.50").getOrThrow())
}
@Test fun parseYuan_rejectsMoreThanTwoDecimals() {
    assertTrue(Money.parseYuan("3.005").isFailure)
}
```

- [ ] **Step 2: 运行金额测试并确认因 `Money` 不存在而失败**

- [ ] **Step 3: 使用 `BigDecimal` 解析后转为 Long 分，格式化固定两位小数**

```kotlin
object Money {
    fun parseYuan(text: String): Result<Long> = runCatching {
        val value = text.trim().toBigDecimal().setScale(2, RoundingMode.UNNECESSARY)
        require(value.signum() >= 0)
        value.movePointRight(2).longValueExact()
    }
    fun format(cents: Long) = "¥%.2f".format(Locale.CHINA, cents / 100.0)
}
```

- [ ] **Step 4: 编写购物车测试，覆盖扫码合并、加减数量、移除、入货价/售价选择和总额**

```kotlin
@Test fun add_sameProduct_mergesQuantity() {
    val cart = Cart.empty(DocumentType.OUTBOUND).add(product).add(product)
    assertEquals(2, cart.lines.single().quantity)
    assertEquals(product.salePriceCents * 2, cart.totalCents)
}
```

- [ ] **Step 5: 运行购物车测试并确认因 `Cart` 不存在而失败**

- [ ] **Step 6: 实现不可变 `Cart`、`CartLine` 和纯函数数量操作，运行所有单元测试**

- [ ] **Step 7: 提交 `feat: add inventory domain models and cart rules`**

## Task 3: SQLite 数据库和商品仓库

**Files:**
- Create: `app/src/main/java/cn/puji/app/data/PujiDatabase.kt`
- Create: `app/src/main/java/cn/puji/app/data/SqlMappers.kt`
- Create: `app/src/main/java/cn/puji/app/data/ProductRepository.kt`
- Test: `app/src/androidTest/java/cn/puji/app/data/ProductRepositoryTest.kt`

- [ ] **Step 1: 编写仪器测试，验证新增商品、条码唯一、空条码、多条件搜索、编辑和停用**

```kotlin
@Test fun duplicateBarcode_isRejected() {
    repository.save(product.copy(barcode = "6901234567890"))
    val result = repository.save(other.copy(barcode = " 6901234567890 "))
    assertTrue(result.isFailure)
}
```

- [ ] **Step 2: 运行目标仪器测试，确认因数据库类不存在而失败**

- [ ] **Step 3: 创建四张表及索引**

```sql
CREATE TABLE products (... barcode TEXT, active INTEGER NOT NULL DEFAULT 1, ...);
CREATE UNIQUE INDEX idx_products_barcode ON products(barcode)
WHERE barcode IS NOT NULL AND barcode <> '';
CREATE TABLE documents (...);
CREATE TABLE document_lines (...);
CREATE TABLE stock_movements (...);
```

- [ ] **Step 4: 实现参数化查询和 Cursor 映射；所有写入通过 `Result` 返回可读错误**

- [ ] **Step 5: 运行商品仓库仪器测试，确认全部通过**

- [ ] **Step 6: 提交 `feat: persist products in local sqlite database`**

## Task 4: 入货、出货和库存事务

**Files:**
- Create: `app/src/main/java/cn/puji/app/data/InventoryRepository.kt`
- Create: `app/src/main/java/cn/puji/app/domain/InventoryService.kt`
- Test: `app/src/androidTest/java/cn/puji/app/data/InventoryRepositoryTest.kt`
- Test: `app/src/test/java/cn/puji/app/domain/InventoryServiceTest.kt`

- [ ] **Step 1: 编写测试：入货增加库存并写三类记录**

```kotlin
@Test fun inbound_createsDocumentLinesMovements_andAddsStock() {
    val id = repository.commit(inboundCart).getOrThrow()
    assertEquals(15, products.get(product.id)!!.stockQuantity)
    assertEquals(5, repository.movements(id).single().quantityDelta)
}
```

- [ ] **Step 2: 编写测试：出货扣减、库存不足拒绝、失败事务完全回滚、历史名称/价格快照不随商品修改**

- [ ] **Step 3: 运行测试并确认因 `commit` 不存在而失败**

- [ ] **Step 4: 在一个 `beginTransaction` 中依序创建单据、写明细、条件更新库存和写流水**

```sql
UPDATE products SET stock_quantity = stock_quantity - ?, updated_at = ?
WHERE id = ? AND stock_quantity >= ? AND active = 1;
```

- [ ] **Step 5: 当出货更新影响行数不是 1 时抛出 `InsufficientStock` 并回滚**

- [ ] **Step 6: 运行领域与仪器测试，确认库存约束和事务全部通过**

- [ ] **Step 7: 提交 `feat: add atomic inbound and outbound transactions`**

## Task 5: 主题、导航和首页

**Files:**
- Create: `app/src/main/java/cn/puji/app/ui/theme/Color.kt`
- Create: `app/src/main/java/cn/puji/app/ui/theme/Theme.kt`
- Create: `app/src/main/java/cn/puji/app/ui/theme/Type.kt`
- Create: `app/src/main/java/cn/puji/app/ui/PujiApp.kt`
- Create: `app/src/main/java/cn/puji/app/ui/components/ReceiptCard.kt`
- Create: `app/src/main/java/cn/puji/app/ui/components/MetricTile.kt`
- Create: `app/src/main/java/cn/puji/app/ui/home/HomeViewModel.kt`
- Create: `app/src/main/java/cn/puji/app/ui/home/HomeScreen.kt`
- Create: `app/src/main/java/cn/puji/app/data/DashboardRepository.kt`
- Test: `app/src/androidTest/java/cn/puji/app/ui/HomeScreenTest.kt`

- [ ] **Step 1: 编写 Compose 测试，要求显示“今日出货”“库存预警”“快捷出货”和四个底部导航项**

```kotlin
@Test fun home_showsCoreMetricsAndActions() {
    compose.onNodeWithText("今日出货").assertIsDisplayed()
    compose.onNodeWithText("库存预警").assertIsDisplayed()
    compose.onNodeWithContentDescription("扫码").assertIsDisplayed()
}
```

- [ ] **Step 2: 运行测试并确认因 `HomeScreen` 不存在而失败**

- [ ] **Step 3: 实现暖纸白/墨黑/票据绿/朱砂橙主题和小票卡片组件**

- [ ] **Step 4: 实现首页统计查询、ViewModel 和带中央扫码按钮的底栏**

- [ ] **Step 5: 运行 UI 测试并使用 Preview/截图检查 360dp 与 412dp 宽度**

- [ ] **Step 6: 提交 `feat: add receipt-inspired app shell and dashboard`**

## Task 6: 商品与库存界面

**Files:**
- Create: `app/src/main/java/cn/puji/app/ui/inventory/InventoryViewModel.kt`
- Create: `app/src/main/java/cn/puji/app/ui/inventory/InventoryScreen.kt`
- Create: `app/src/main/java/cn/puji/app/ui/product/ProductEditorViewModel.kt`
- Create: `app/src/main/java/cn/puji/app/ui/product/ProductEditor.kt`
- Test: `app/src/test/java/cn/puji/app/ui/product/ProductFormValidatorTest.kt`
- Test: `app/src/androidTest/java/cn/puji/app/ui/ProductEditorTest.kt`

- [ ] **Step 1: 编写验证测试：名称必填、价格非负、库存为整数、重复条码显示明确提示**

- [ ] **Step 2: 运行测试并确认验证器不存在而失败**

- [ ] **Step 3: 实现验证器、库存列表、搜索、低库存标签、空状态和商品编辑器**

- [ ] **Step 4: 实现“停用”确认，不提供物理删除**

- [ ] **Step 5: 运行单元与 UI 测试，确认新增、编辑、搜索和停用流程**

- [ ] **Step 6: 提交 `feat: add product editor and inventory browser`**

## Task 7: 摄像头扫码和入货/出货工作台

**Files:**
- Create: `app/src/main/java/cn/puji/app/scanner/BarcodeAnalyzer.kt`
- Create: `app/src/main/java/cn/puji/app/scanner/ScannerController.kt`
- Create: `app/src/main/java/cn/puji/app/ui/scanner/ScannerViewModel.kt`
- Create: `app/src/main/java/cn/puji/app/ui/scanner/CameraPreview.kt`
- Create: `app/src/main/java/cn/puji/app/ui/scanner/ScannerScreen.kt`
- Test: `app/src/test/java/cn/puji/app/scanner/ScannerControllerTest.kt`
- Test: `app/src/test/java/cn/puji/app/ui/scanner/ScannerViewModelTest.kt`
- Test: `app/src/androidTest/java/cn/puji/app/ui/ScannerScreenTest.kt`

- [ ] **Step 1: 编写扫码节流测试：同一码 1200ms 内只触发一次，不同码立即触发**

```kotlin
@Test fun repeatedBarcode_insideCooldown_isIgnored() {
    controller.accept("6901", now = 1000)
    controller.accept("6901", now = 1500)
    assertEquals(listOf("6901"), emitted)
}
```

- [ ] **Step 2: 运行测试并确认控制器不存在而失败**

- [ ] **Step 3: 实现 ML Kit 格式白名单、ImageProxy 安全关闭、生命周期解绑和扫码节流**

- [ ] **Step 4: 编写 ViewModel 测试，覆盖已知码加入购物车、未知码打开快速建档、库存不足、模式切换和确认后清空**

- [ ] **Step 5: 实现相机权限降级、手动输入、商品搜索、购物车、数量控件、金额合计和固定确认按钮**

- [ ] **Step 6: 运行全部扫码单元/UI 测试并在模拟器使用测试图片验证识别回调**

- [ ] **Step 7: 提交 `feat: add offline barcode inbound and outbound workspace`**

## Task 8: 记录、详情、导出和关于页

**Files:**
- Create: `app/src/main/java/cn/puji/app/ui/records/RecordsViewModel.kt`
- Create: `app/src/main/java/cn/puji/app/ui/records/RecordsScreen.kt`
- Create: `app/src/main/java/cn/puji/app/export/CsvExporter.kt`
- Create: `app/src/main/java/cn/puji/app/ui/about/AboutScreen.kt`
- Create: `app/src/main/res/xml/file_paths.xml`
- Test: `app/src/test/java/cn/puji/app/export/CsvExporterTest.kt`
- Test: `app/src/androidTest/java/cn/puji/app/ui/RecordsScreenTest.kt`

- [ ] **Step 1: 编写 CSV 测试，覆盖中文、逗号、引号、换行和 UTF-8 BOM**

```kotlin
@Test fun csv_escapesChineseCommaQuoteAndNewline() {
    assertEquals("\uFEFF名称,备注\r\n可乐,\"促销,\"\"红\"\"\"\r\n", exporter.products(rows))
}
```

- [ ] **Step 2: 运行测试并确认导出器不存在而失败**

- [ ] **Step 3: 实现记录筛选、单据详情、CSV 生成、FileProvider 分享和失败提示**

- [ ] **Step 4: 实现关于页，注明“业务设计参考 Open Source POS”、仓库链接、MIT 许可证链接和 App 版本**

- [ ] **Step 5: 运行导出和记录 UI 测试**

- [ ] **Step 6: 提交 `feat: add document history csv export and attribution`**

## Task 9: 示例数据、无障碍和视觉精修

**Files:**
- Modify: `app/src/main/java/cn/puji/app/data/PujiDatabase.kt`
- Modify: `app/src/main/java/cn/puji/app/ui/**/*.kt`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Test: `app/src/androidTest/java/cn/puji/app/ui/AccessibilitySmokeTest.kt`

- [ ] **Step 1: 编写测试，要求关键按钮有 contentDescription、触控区域至少 48dp、字体放大后核心操作仍存在**

- [ ] **Step 2: 运行测试并确认当前界面暴露问题**

- [ ] **Step 3: 增加首次启动示例商品，可在设置中一键清空；完善空状态、加载状态、动画和触觉反馈**

- [ ] **Step 4: 制作“铺”字票据印章启动图标和自适应图标**

- [ ] **Step 5: 在浅色、深色系统栏、360dp、412dp 和大字体场景做截图检查并修正遮挡**

- [ ] **Step 6: 提交 `style: polish Puji mobile experience`**

## Task 10: 最终验证和 APK 产出

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `README.md`
- Create: `docs/TESTING.md`
- Create: `docs/THIRD_PARTY_NOTICES.md`

- [ ] **Step 1: 运行 `./gradlew clean testDebugUnitTest connectedDebugAndroidTest lintDebug assembleDebug assembleRelease`**

Expected: 所有任务成功，单元测试和仪器测试零失败，lint 无 error，生成 Debug 与可安装 Release APK。

- [ ] **Step 2: 在模拟器或设备执行 `adb install -r app/build/outputs/apk/release/app-release.apk`**

- [ ] **Step 3: 手工验收完整闭环：新增商品 → 扫码入货 10 件 → 扫码出货 2 件 → 库存为 8 → 两张单据可查 → CSV 可分享**

- [ ] **Step 4: 检查 APK 签名、包名、版本、大小和 SHA-256**

- [ ] **Step 5: 将最终 APK 复制到 `dist/铺记-v1.0.0.apk`，并在 README 写清安装、权限、数据位置和限制**

- [ ] **Step 6: 提交 `release: build Puji Android v1.0.0`**

## 自检

- 设计中的商品、入货、出货、库存、记录、CSV、扫码降级和开源归属均有对应任务。
- 数据层、领域层、扫码层与 UI 边界明确；金额、库存和事务约束使用可测试接口。
- 计划不包含多门店、云同步、员工权限、支付和打印等首版外功能。
- 依赖版本全部固定，最终以完整测试、lint、安装启动和业务闭环为完成证据。
