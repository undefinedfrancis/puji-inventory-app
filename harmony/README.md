# 铺记 HarmonyOS 版

这是“铺记”的原生 HarmonyOS Stage 模型工程。Android 版本仍保留在仓库根目录，两端的数据彼此独立。

## 开发环境

- DevEco Studio 26.0.0 Release（macOS ARM）
- HarmonyOS SDK 6.0.2（API 22）
- 最低兼容 HarmonyOS 5.0.0（API 12）
- ArkTS、ArkUI、ArkData、Hvigor、ohpm

## 在 DevEco Studio 中运行

1. 使用 DevEco Studio 打开本目录 `harmony/`。
2. 等待 SDK、ohpm 与 Hvigor 同步完成。
3. 选择 Phone 模拟器或已开启开发者模式的 HarmonyOS 设备。
4. 运行 `entry` 模块的 `default` 产品。

## 命令行构建

由于当前仓库路径包含中文，Hvigor 命令行会拒绝直接构建。请将 `harmony/` 同步到一个仅含英文字符的临时目录后再构建，或直接在 DevEco Studio 中打开工程运行。

开发机上的验证命令：

```bash
rsync -a --exclude build --exclude .hvigor harmony/ /private/tmp/puji-harmony-build/
devecocli build --product default --modules entry@default --build-mode debug
```

Debug HAP 位于临时工程的 `entry/build/default/outputs/default/entry-default-unsigned.hap`。

## 已实现功能

- 首页经营概况、低库存提醒、双重确认的一键清空
- 商品货架、类别筛选、名称/条码搜索、库存 CSV 导出
- Scan Kit 相机条码识别和手动条码输入
- 入货、出货购物车与库存校验
- 未知商品扫码后暂停，保存或取消后才允许下一次扫码
- 月度、季度、年度经营汇总
- 按月查看流水、筛选入货/出货、月度和自定义日期 CSV 导出

## 签名说明

当前命令行产物为未签名 HAP。安装到真实设备或运行模拟器前，需要登录华为开发者帐号并执行自动调试签名；发布版本还需要正式发布证书和 Profile。签名材料包含帐号及设备信息，不应提交到 Git。
