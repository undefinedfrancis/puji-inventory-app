# 铺记 Android 小卖部出货系统设计

## 目标

制作一个安装后即可离线使用的 Android App，覆盖小卖部最核心的商品登记、扫码入货、扫码出货、库存查询和出入库流水。首版不依赖服务器，不要求用户注册，也不把复杂 ERP 功能带入移动端。

## 与 Open Source POS 的关系

本项目以 Open Source POS 的核心业务模型为基础，将其 `items`、`item_quantities`、`inventory`、`sales`、`sales_items`、`receivings` 和 `receivings_items` 思路重组为适合单店、单手机使用的原生 Android 应用。由于原项目是 PHP Web 系统，APK 不直接嵌入其服务端代码；应用保留稳定的数据边界，后续可以增加与 Open Source POS 服务端同步的适配器。

## 首版范围

### 商品

- 新建、编辑、停用商品。
- 字段包括商品名称、条码、分类、进货价、售价、当前库存、低库存阈值和备注。
- 条码必须唯一；允许没有条码的临时商品。
- 支持按名称、条码和分类搜索。

### 入货

- 摄像头扫描条码或手动选择商品。
- 同一商品在单据内自动合并数量。
- 可修改数量和进货单价，填写供应商备注。
- 确认后原子性增加库存，并写入入货单和库存流水。

### 出货

- 摄像头连续扫码或手动选择商品。
- 扫到未知条码时进入快速建档。
- 展示售价、库存、数量和合计金额。
- 禁止库存不足的出货；确认后原子性扣减库存，并写入出货单和库存流水。

### 库存与记录

- 首页显示今日出货额、今日出货件数、商品数量和低库存数量。
- 库存页显示库存状态、价值和低库存提醒。
- 记录页按入货/出货筛选，展示单据明细和时间。
- 支持把商品和流水导出为 CSV，通过系统分享保存或发送。

## 导航与操作

底部导航包含“首页、库存、扫码、记录”四项。扫码按钮位于中央并使用高对比强调色。入货和出货使用同一个扫码工作台，通过顶部模式切换，减少重复页面和学习成本。

关键操作均可在单手范围内完成：确认按钮固定在底部；数量使用大号加减控件；金额与库存数字采用等宽数字样式。删除商品改为“停用”，避免历史单据失去关联。

## 视觉方向

名称为“铺记”。视觉采用“现代收银小票”方向：暖纸白背景、深墨黑文字、票据绿作为正常状态、朱砂橙作为扫码和警告强调色。卡片不使用大面积渐变，依靠细线、票据切口、紧凑数字排版和少量弹性动画形成辨识度。界面使用简体中文，优先保证户外和柜台环境下的可读性。

## 技术架构

- Kotlin、Jetpack Compose、Material 3。
- 单 Activity，Navigation Compose 管理页面。
- SQLiteOpenHelper 提供本地持久化，Repository 封装事务和查询；业务层不依赖 Android UI，便于单元测试。
- CameraX 提供相机预览，ML Kit Bundled Barcode Scanning 在设备端识别 EAN-8、EAN-13、UPC-A、UPC-E、Code 39、Code 93、Code 128、ITF 和 QR Code。
- ViewModel + StateFlow 管理界面状态。
- CSV 通过 FileProvider 和 Android 分享面板导出。

## 数据模型

### products

`id`、`name`、`barcode`、`category`、`cost_price_cents`、`sale_price_cents`、`stock_quantity`、`low_stock_threshold`、`note`、`active`、`created_at`、`updated_at`。

### documents

`id`、`type`（INBOUND/OUTBOUND）、`total_cents`、`note`、`created_at`。

### document_lines

`id`、`document_id`、`product_id`、`product_name_snapshot`、`barcode_snapshot`、`unit_price_cents`、`quantity`、`line_total_cents`。

### stock_movements

`id`、`product_id`、`document_id`、`type`、`quantity_delta`、`stock_after`、`created_at`。

金额全部使用整数“分”，避免浮点误差；时间保存为 UTC 毫秒；商品名称、条码和价格在单据行中保留快照，保证商品后来修改后历史单据仍可读。

## 业务约束与错误处理

- 条码去除首尾空白后比较，空条码不参与唯一约束。
- 商品名称不能为空，价格和数量不能为负数。
- 出货事务在数据库层再次校验库存，防止界面状态过期。
- 单据、明细、库存和流水必须在同一数据库事务中成功或一起回滚。
- 相机权限拒绝时仍允许手动输入条码和搜索商品。
- 数据库异常以不破坏当前购物车的方式显示，用户可以重试。

## 测试与验收

- 业务单元测试覆盖金额计算、条码规范化、购物车合并、库存增减、库存不足和低库存判断。
- 数据库仪器测试覆盖入货/出货事务、回滚、历史快照和搜索。
- Compose UI 测试覆盖首页关键指标、商品表单验证和出货确认流程。
- 最终执行单元测试、静态检查、Debug/Release 构建，并在 Android 模拟器或连接设备上做安装启动验证。

## 暂不包含

- 多门店、云同步、员工权限、会员、赊账、在线支付、打印机、复杂税率、促销组合、批次/保质期、Web 管理后台。
- 这些能力不会阻塞首版日常入货与出货，后续按真实使用反馈分阶段增加。

## 开源归属

应用“关于”页会注明业务设计参考 Open Source POS 并链接其仓库与许可证。首版不复制 Open Source POS 的 PHP 源码，因此不在 APK 内嵌其 Web 页脚或服务端资产。
