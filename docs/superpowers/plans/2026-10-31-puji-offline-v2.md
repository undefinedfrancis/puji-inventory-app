# 铺记 Android 离线升级 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现已确认的米白轻排版 Android 界面、新图标与离线经营基础能力，保留旧数据。
**Architecture:** 延续 Compose + SQLite；业务写入集中在仓库事务，UI 通过后台任务读取；独立实现页面、业务账本、文件备份三个边界，最后由顶层页面整合。只构建 Android。
**Tech Stack:** Kotlin、Compose Material3、SQLiteOpenHelper、CameraX、ML Kit。
**Spec:** docs/superpowers/specs/2026-10-31-puji-offline-v2-design.md；最终视觉为 docs/design/2026-10-31-cream/ui-cream-typography-v2.png。

## Global Constraints

- 米白 #F6F2E9、奶白 #FFFCF5、灰蓝 #365D6C；页面标题 22sp、正文 15sp、重点金额 26sp，少量字重，不用 Serif/Monospace/Black；触控最少 48dp。
- cn.puji.app、Android API 26 及以上、离线；不修改 harmony/，不改变其他设备或共享服务。
- 保留旧数据与快照，数据库迁移不清空数据（旧documents表因类型CHECK限制在迁移事务内保留主键重建）；库存不为负、金额为整数分；事务与请求 ID 避免重复。
- 原始单据不可直接修改删除；通过关联退货/冲正处理；备份可恢复，CSV不当作完整备份。
- 用户已指示按方案落地，不再请求重复批准；实现中的常规选择写入进度记录。

## Review Focus

- 升级 v1/v2 保留原有商品与历史快照；成本来源不明确时不冒称精确。
- 部分退货多次累计的数量/成本尾差、入货退货金额与当前成本差异。
- 恢复文件损坏、重复 ID/外键、失败回滚和草稿清理。
- 扫码同码停留不连加、异码连续加入、提交期间禁用、切换模式不丢清单。
- 375dp/大字体/长名称/大金额/键盘/深色下布局与48dp触控。

### Task 1: 可运行构建基线

Files: gradle.properties、local.properties（不提交）、scripts/本任务专用构建命令记录。
- [x] 确认独立工作区和本任务分支；发现 JDK/SDK 与可用 Gradle。
- [ ] 使用不影响其他任务的串行、低 worker 构建执行现有单测；记录结果及环境差异。
- [x] 不停止共享 daemon，不清理其他任务缓存，不使用已有模拟器。

### Task 2: 米白 UI 与统一排版

Files: ui/theme、ui/components、ui/home、ui/inventory、ui/scanner、ui/records、ui/product。
Interfaces: 保持现有 Screen 的参数签名；扩展能力使用有默认值的新回调/数据参数，统一图标为自绘 ImageVector。
- [x] 对照预览与现有 UI，重做主题/字体角色、列表/按钮/筛选/趋势图及空状态。
- [x] 去掉装饰说明、英文 kicker、粗黑字、等宽/衬线体、固定额外底部84dp空白。
- [x] 首页数据管理移入设置，商品列表轻量化，扫码支持选商品，金额可换布局。
- [x] 构建/静态检查；设备可用时截图核对浅深色和字号。

### Task 3: 可靠库存账本

Files: domain/Models.kt、Costing.kt、BusinessSummary.kt、data/PujiDatabase.kt、ProductRepository.kt、InventoryRepository.kt、DashboardRepository.kt；domain及数据库测试。
Interfaces: Product/DocumentLine 新字段使用默认值保持调用兼容；DocumentType 扩展期初/退货/盘盈/盘亏/报损/冲正；InventoryRepository 提供 count(productId, actualQuantity, expectedQuantity, unitCostCents, note)、writeOff(productId, quantity, note)、returnLines(documentId, quantities, requestId)、reverse(documentId, requestId) 返回 Result<Long>；commit(Cart, requestId) 兼容旧调用。
- [ ] 先写成本分摊、部分退货上限、库存冲突、金额边界、报表口径失败用例并确认失败。
- [x] 迁移旧表，新增成本总额/UUID/请求ID/关联字段与索引；首次空白不种演示数据。
- [x] 实现整数成本分摊、原子交易、盘点/报损/关联退货/冲正；建议进价编辑不覆盖库存。
- [x] 资料保存已有商品忽略库存字段；新增期初生成留痕。停用不让库存价值消失。
- [x] SQL聚合与分页/搜索接口，运行单测及数据库测试。

### Task 4: 本地备份恢复与商品导入

Files: data/BackupRepository.kt、importer/ProductCsvImporter.kt、测试。
Interfaces: BackupRepository(PujiDatabase).exportJson():String、restoreJson(String):Result<Unit>、inspect(String):BackupInfo；ProductCsvImporter.preview(String) 与 import(repository, rows)。不依赖新UI，不改其他任务文件。
- [ ] 先写坏文件/重复ID/引用缺失/旧商品库存覆盖/CSV转义测试，确认失败。
- [x] 版本化完整数据 JSON、校验、验证、事务替换、失败回滚；恢复前由调用方保存当前回退副本。
- [x] CSV解析及预览，已有商品只改资料，新商品库存通过期初路径；不接受流水表冒充商品表。
- [x] 运行相关测试，向顶层集成提供明确接口和错误文案。

### Task 5: 扫码、草稿与顶层集成

Files: ui/PujiApp.kt、scanner/BarcodeAnalyzer.kt、ScannerController.kt、ScanTransactionGate.kt、data/DraftRepository.kt、ui/settings及相关表单。
Interfaces: 整合Tasks2–4公开接口；顶层单独维护 loading/busy 与错误反馈，在 Dispatchers.IO 执行数据库/文件操作。
- [x] 扫码行为测试：同码停留、离开后重进、异码、多件合并、未知建档、重复提交。
- [x] 已知商品加入后继续；未知建档只加入待提交清单，最终事务建档入货；模式切换保护清单。
- [x] 本地草稿恢复、设置中文件选择保存恢复、CSV预览导入、商品详情盘点/报损、单据退货/冲正。
- [x] 前台恢复/跨日刷新、操作完成统一刷新；失败保留清单。
- [x] 运行全部业务/扫码测试与构建。

### Task 6: 图标、版本与设备验收

Files: res/drawable、mipmap、app/build.gradle.kts、README.md、docs/TESTING.md、dist/。
- [x] 按已选拼贴小店图标制作自适应前景/背景/单色资源，保留原签名策略。
- [x] 版本提升，运行单测、Lint、Release构建；核对签名，不能以卸载代替升级。
- [x] 仅用本任务独立设备进行迁移/备份恢复/多商品/退货盘点/大字体/深色截图；资源冲突优先避让。
- [x] 独立全分支代码审阅，修复重大问题后重新验证；记录环境限制，不宣称未做的验收。
- [x] 交付APK、截图、版本和验证说明，保留可复查源码。

实施记录：旧基线构建受环境下载限制未取得未修改版本测试结果；并非每条新测试都有单独RED日志，因此上述对应步骤保留未勾选。最终完整构建、70项单测与33项Android测试均通过，见docs/TESTING-V2.md。
