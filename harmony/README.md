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

DevEco Studio 完成首次 SDK 初始化后，在本目录执行：

```bash
./hvigorw assembleHap --mode module -p product=default -p module=entry@default -p buildMode=debug --no-daemon
```

Debug HAP 将位于 `entry/build/default/outputs/default/` 下。最终精确路径会在首次成功构建后补充。

## 签名说明

模拟器可直接运行调试版本。安装到真实设备时，需要在 DevEco Studio 中登录华为开发者帐号并配置调试签名；发布版本还需要正式发布证书和 Profile。
