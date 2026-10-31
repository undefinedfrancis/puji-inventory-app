---
name: "铺记 Android"
description: "米白、轻字重、清楚克制的离线小店经营界面"
colors:
  primary: "#365D6C"
  on-primary: "#FFFFFF"
  primary-container: "#E4ECEE"
  on-primary-container: "#365D6C"
  secondary: "#8A6736"
  on-secondary: "#FFFFFF"
  secondary-container: "#F0E5CF"
  on-secondary-container: "#6A502B"
  background: "#F6F2E9"
  on-background: "#292C2A"
  surface: "#FFFCF5"
  on-surface: "#292C2A"
  surface-container-lowest: "#FFFCF5"
  surface-container-low: "#FFFCF5"
  surface-container: "#F6F2E9"
  surface-container-high: "#FFFCF5"
  surface-container-highest: "#F6F2E9"
  surface-bright: "#FFFCF5"
  surface-dim: "#E8E2D8"
  surface-tint: "#365D6C"
  surface-variant: "#EDE8DE"
  on-surface-variant: "#6D6E69"
  outline: "#8B8C84"
  outline-variant: "#E3DED4"
  error: "#AA3D36"
  error-container: "#F6E7E2"
  on-error-container: "#8A2C27"
  dark-primary: "#9DC6D5"
  dark-on-primary: "#183C49"
  dark-primary-container: "#294B58"
  dark-on-primary-container: "#D2E8F0"
  dark-secondary: "#E2C48F"
  dark-on-secondary: "#3E2E15"
  dark-secondary-container: "#51432D"
  dark-on-secondary-container: "#F3DEB8"
  dark-background: "#1D1F1D"
  dark-on-background: "#F2EDE3"
  dark-surface: "#272A27"
  dark-on-surface: "#F2EDE3"
  dark-surface-container-lowest: "#181B18"
  dark-surface-container-low: "#222522"
  dark-surface-container: "#272A27"
  dark-surface-container-high: "#30332F"
  dark-surface-container-highest: "#383C36"
  dark-surface-bright: "#383C36"
  dark-surface-dim: "#181B18"
  dark-surface-tint: "#9DC6D5"
  dark-surface-variant: "#333630"
  dark-on-surface-variant: "#BCBDB4"
  dark-outline: "#8D9086"
  dark-outline-variant: "#42453E"
  dark-error: "#FFB4A9"
  dark-error-container: "#5E2D29"
  dark-on-error-container: "#FFDAD4"
  icon-apricot: "#EFDCC7"
  icon-paper: "#FFF8E9"
  icon-terracotta: "#B96449"
  icon-olive: "#626B4D"
  icon-window: "#CEA56B"
typography:
  display:
    fontFamily: "sans-serif"
    fontSize: "26sp"
    fontWeight: 500
    lineHeight: "32sp"
    fontFeature: "tnum"
  headline:
    fontFamily: "sans-serif"
    fontSize: "22sp"
    fontWeight: 600
    lineHeight: "28sp"
    fontFeature: "tnum"
  headline-small:
    fontFamily: "sans-serif"
    fontSize: "22sp"
    fontWeight: 500
    lineHeight: "28sp"
    fontFeature: "tnum"
  title:
    fontFamily: "sans-serif"
    fontSize: "15sp"
    fontWeight: 500
    lineHeight: "22sp"
    fontFeature: "tnum"
  body:
    fontFamily: "sans-serif"
    fontSize: "15sp"
    fontWeight: 400
    lineHeight: "22sp"
    fontFeature: "tnum"
  body-small:
    fontFamily: "sans-serif"
    fontSize: "13sp"
    fontWeight: 400
    lineHeight: "18sp"
    fontFeature: "tnum"
  label-large:
    fontFamily: "sans-serif"
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: "20sp"
    fontFeature: "tnum"
  label-medium:
    fontFamily: "sans-serif"
    fontSize: "12sp"
    fontWeight: 400
    lineHeight: "18sp"
    fontFeature: "tnum"
  label-small:
    fontFamily: "sans-serif"
    fontSize: "11sp"
    fontWeight: 500
    lineHeight: "16sp"
    fontFeature: "tnum"
rounded:
  extra-small: "6dp"
  small: "8dp"
  medium: "12dp"
  large: "16dp"
  extra-large: "20dp"
spacing:
  micro: "4dp"
  tight: "6dp"
  inline: "8dp"
  control-gap: "12dp"
  card-inset: "16dp"
  page-inset: "20dp"
  section: "24dp"
  empty-vertical: "36dp"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.medium}"
    height: "48dp"
  button-outlined:
    textColor: "{colors.primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.medium}"
    height: "56dp"
  choice-selected:
    backgroundColor: "{colors.primary-container}"
    textColor: "{colors.on-primary-container}"
    typography: "{typography.label-large}"
    rounded: "{rounded.medium}"
    height: "48dp"
  choice-unselected:
    textColor: "{colors.on-surface-variant}"
    typography: "{typography.label-large}"
    rounded: "{rounded.medium}"
    height: "48dp"
  receipt-card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.large}"
    padding: "16dp"
  status-pill:
    backgroundColor: "{colors.primary-container}"
    textColor: "{colors.on-primary-container}"
    typography: "{typography.label-medium}"
    rounded: "{rounded.small}"
    padding: "4dp 8dp"
  status-warning:
    backgroundColor: "{colors.error-container}"
    textColor: "{colors.on-error-container}"
    typography: "{typography.label-medium}"
    rounded: "{rounded.small}"
    padding: "4dp 8dp"
  input-search:
    textColor: "{colors.on-surface}"
    typography: "{typography.body}"
    rounded: "{rounded.medium}"
  navigation:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface-variant}"
    typography: "{typography.label-small}"
---

# Design System: 铺记 Android

## Overview

**Creative North Star: "米白小店账页"**

用户已确认米白背景、小字号、轻字重和简洁直接的方向。界面像一页清楚整理的小店账目，以商品、数量、金额和操作为中心；不把纸张隐喻做成做旧纹理或装饰。此命名仅概括已确认的方向，不引入新的品牌选择。

本记录从 Android Compose 源码提取，适用于铺记的公共主题和组件。设计参考为 `docs/design/2026-10-31-cream/ui-cream-typography-v2.png`；它不是设备截图。记录日期为 2026-10-31。已在本任务独立 Android 16（API 36.1） 模拟器检查真实运行的浅/深色、375dp窄屏、1.3倍字体、长名称和大金额，截图位于 `docs/screenshots/2.0/`；底部软键盘展开时已验证表单可滚动至保存和取消，实体手机相机尚未实测。

**Key Characteristics:**
- 米白底与暖白面板，低饱和蓝灰只强调操作。
- 正文克制，主要金额与页标题建立清晰层级。
- 使用原生 Material 3 结构与交互，品牌通过主题表达。
- 小店图标使用独立的杏色、陶土和橄榄色片拼接。

源码权威：`app/src/main/java/cn/puji/app/ui/theme/PujiTheme.kt`、`ui/components/`、`ui/home/HomeScreen.kt`、`ui/inventory/InventoryScreen.kt`、`ui/scanner/ScannerScreen.kt`、`ui/records/RecordsScreen.kt`、`ui/manage/ManagementScreens.kt` 和 `ui/PujiApp.kt`。启动图标来自 `app/src/main/res/drawable/ic_launcher_foreground.xml`、`ic_launcher_monochrome.xml` 与 `res/values/colors.xml`。原生单位保留为 dp / sp；sidecar 文档面板用等数值 CSS px 示意，不是物理单位等价声明。

## Colors

主题是温暖纸色与清楚墨色，操作蓝灰保持低饱和。前置 token 的值是规范；下文只说明用途。

### Primary

- **克制蓝灰**（primary / on-primary）：出货、确认、选中图标和销售趋势线。
- **淡蓝灰底**（primary-container / on-primary-container）：选择按钮与状态标签；并非大面积页面底色。

### Secondary

- **账目棕金**（secondary 与对应 container / on-*）：进货趋势系列，与销售蓝灰区分。

### Neutral

- **米白纸底**（background）、**暖白面板**（surface）：页面与卡片的相邻层次。
- **墨色**（on-background / on-surface）：主文字及金额。
- **辅助灰**（on-surface-variant）：日期、类别、库存说明；已在源码中轻微加深以改善对比度，已检查上述模拟器中的实际显示。
- **细分隔线**（outline-variant）与 **输入边界**（outline）：列表分组及 Material 字段边界。
- **深色语义组**（dark-*）：随系统深色模式切换，容器层级显式定义；不得直接反转浅色颜色。

### Status and identity

- **温红错误组**（error、error-container、on-error-container）：低库存、负金额、危险操作和异常状态。
- **图标独立色组**（icon-*）：杏色底、奶油店身、陶土雨棚、橄榄门、赭色窗；色片拼成小店。它不是 UI 的新主色盘。
- sidecar 中的八级 OKLCH tonalRamp 是文档面板合成的色带，不能作为新增生产色阶；实际浅深色主题以本文件前置 token 和 Kotlin 源码为准。
- 源码中 `TicketGreen` / `ScannerOrange` 是兼容旧调用的别名，均指向当前 primary；不把名字解释为绿色或橙色视觉角色。未复用的 `MutedGold` 不作为新界面规范。

**The 米白底色 Rule.** 浅色界面以 background 与 surface 为主体；primary 用于动作、选中态和数据系列，不铺成整屏钴蓝。

## Typography

统一使用 Android `FontFamily.SansSerif`，没有捆绑额外字体。中文实际字形由设备系统回退决定；不承诺某一款 iOS 或中文商用字体。所有角色启用 `tnum`，以便金额排列稳定，已检查上述模拟器的实际字形；其他品牌设备的字体回退仍由其系统决定。

### Hierarchy

- **主要金额**：displayLarge / Medium / Small 对应前置 display（中等字重）；`AmountPair(prominent = true)` 的首项使用它。
- **页面标题**：headlineLarge / Medium 对应 headline；headlineSmall 是较轻的 headline-small，用于指标和次级金额。
- **小标题**：titleLarge / Medium / Small 均对应 title，以字重而非大幅字号区分正文。
- **正文**：bodyLarge / Medium 对应 body；日期、类别和补充信息使用 bodySmall。
- **标签**：labelLarge 为按钮，labelMedium 为状态与辅助刻度，labelSmall 为底部导航。

**The 字号分工 Rule.** 正文使用 body，页面标题使用 headlineMedium，最重要金额才使用 displaySmall；不要把所有数字都放大或加粗。

## Layout

手机页面以纵向内容和底部四项导航组成；`Scaffold` 的 padding 传入内容。今日、商品、扫码和经营页面外侧主要使用 page-inset，分组留白主要使用 section；商品、扫码的密集分项使用 control-gap。正文行高见 typography，集中在（22sp），不是额外的布局间距。

卡片内部使用 card-inset 与 inline 节奏；今日交易、补货行最小高度（68dp），商品行与选货行最小高度（72dp）；管理菜单另有（52dp）行。公共交互至少（48dp），今日主按钮最小高度（56dp）。48dp 是点击区域，不要求状态标签本身具备按钮高度。

`AmountPair` 在可用宽度小于（300dp）、字体缩放大于（1.15）或任一金额字符串长度大于（11）时纵排；扫码合计也为大字体和长金额提供纵排。现有代码没有平板导航 rail 或完整自适应断点，不将手机布局宣称为已验证的平板规范。

## Elevation & Depth

主要通过相邻纸色、留白和分隔线区分内容；`ReceiptCard` 不添加自定义阴影，底部导航显式 `tonalElevation = 0dp`。按钮、对话框等保留 Material 3 自身状态与层级，本项目未定义独立阴影、缓动或动画时长 token，文档不杜撰其数值。

**The 语义深度 Rule.** 深色模式使用 DarkColors 中显式定义的 surfaceContainer 层级；组件从 MaterialTheme.colorScheme 取色。

## Shapes

主题使用温和圆角阶梯：extra-small、small、medium、large、extra-large。卡片使用 large，显式主按钮和选择按钮使用 medium，状态标签使用 small，自定义管理对话框使用 extra-large。不是所有控件都改成胶囊，也不把图标色片的轮廓复制到表单。

应用内图标是（24dp）Canvas 线性图形，圆端线宽为（1.6dp）；空态可放大至（32dp）。启动图标是独立的填充色片小店，不能混用为操作图标。

## Components

### Buttons

主要动作使用 Material 3 Button，今日出货为实心、入货为描边；使用 primary 与对应 on-primary。显式高度是最小高度，不锁死文字增大后的高度。选择按钮用淡蓝灰容器表达当前状态，未选中用辅助色文字；`enabled` 控制交互，busy 时显示进度。按压、聚焦和禁用沿用 Material 3；没有自定义网页 hover 契约。

### Chips

`StatusPill` 为展示标签，默认使用 primary-container；warning 切换 error-container 和 on-error-container。它不是可点击筛选按钮；交互筛选由 `ChoiceButton` 承担。

### Cards / Containers

`ReceiptCard` 使用 surface、large 圆角和 card-inset 内边距，子项间距 inline；不添加装饰性渐变或大投影。实际主界面分组和管理分组复用这一容器。`PujiDivider` 使用 outline-variant。

### Inputs / Fields

商品搜索为 Material 3 OutlinedTextField，明确使用 medium 圆角与搜索线性图标；其他字段以真实调用处和 Material 默认值为准，不把某个搜索框的圆角强加给所有原生对话框。聚焦、错误和禁用边界由 Material 色彩角色处理。sidecar 的 HTML 输入框只用于文档演示。详情字段用左右对齐行，标签与值按 1:2 分配可用宽度，长内容自然换行。

### Navigation

今日、商品、扫码、经营四项 Material NavigationBar，surface 背景；选中图标使用 primary 与 primary-container 指示底，未选中图标用 on-surface-variant，标签用 labelSmall。进入管理与详情后隐藏底栏，标题行提供返回动作，并由应用 `BackHandler` 处理返回。

### AmountPair and rows

双金额组件以标签在上、金额在下排列，必要时改为纵排。商品名占可伸缩列，金额右对齐，类别与库存降一级；不把所有行包成独立的装饰卡片。危险或低库存状态同时保留明确文字。

## Do's and Don'ts

### Do:

- **Do** 使用米白背景、暖白面板及语义主题色，新增界面沿用现有角色。
- **Do** 使用 sp 文字和 dp 布局，保持至少 48dp 的点击区域。
- **Do** 保留金额的等宽数字特性，并让长金额及大字体场景换行或纵向排列。
- **Do** 用文字、图形和可访问描述共同表达状态，错误不能只靠变红。
- **Do** 将图标的独立配色留在品牌资产中，并使用适配图标与单色图标资源。

### Don't:

- **Don't** 恢复被用户否决的钴蓝主界面、蓝白大字号重字重版本或深蓝扫描框启动图标。
- **Don't** 把用户图标参考中的爱心、星芒、翅膀或灰蓝配色照搬成新品牌。
- **Don't** 因为参考预览看起来完整，就宣称 Compose 页面通过了设备、深色或字体 QA。
- **Don't** 把文档面板的 HTML/CSS 映射当作可直接用于 Compose 的生产组件。
