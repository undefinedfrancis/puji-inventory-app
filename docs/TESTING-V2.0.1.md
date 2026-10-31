# 铺记 2.0.1 图标兼容修复

用户反馈：蒲公英上传2.0.0后显示平台默认图标。旧APK经Android SDK的aapt解析，application-icon各密度及application.icon均指向编译后的自适应XML（res/E4.xml），包内没有普通PNG启动图标。

## 修复

- 保持拼贴小店图案及原始sRGB颜色；补充48/72/96/144/192px的PNG资源和独立512px图标。
- 应用元信息的icon明确指向PNG资源，供分发平台直接提取。启动Activity继续指向自适应图标，保留桌面自适应和单色图标。
- 自适应XML放入mipmap-anydpi-v26。单独只加版本限定和PNG仍会在minSdk26的aapt解析结果中选中XML；最小打包验证已证实，因此应用与启动Activity分别声明图标。
- 版本2.0.1 (7)，包名、签名方式、数据库版本及业务代码保持。

## 验证

- 回归脚本 `scripts/check-apk-icon.py` 对旧2.0.0失败：Icon is not a directly extractable PNG: res/E4.xml。
- 修复后的最小APK在minSdk26下可直接提取5个密度的PNG，检查通过。
- 完整构建通过（87任务）；70项单元测试全部通过；Lint 0错误、16提示（其中新增一条为minSdk26下v26目录的冗余建议）。完整发布APK的5个密度图标均为可直接提取的PNG，检查通过；签名证书与2.0.0/1.1.1一致。
- [构建日志](verification/2.0.1/build.txt)、[实际APK图标提取](verification/2.0.1/icon-extraction.txt)、[摘要与校验](verification/2.0.1/summary.json)。本次未重跑Android数据库33项；上轮结果见2.0记录。
- 未直接登录或重新上传到用户蒲公英账户；平台最终显示需使用新包重新上传核对。

参考：[蒲公英上传时从包中解析图标](https://www.pgyer.com/doc/view/app_upload)、[Android自适应与传统图标资源](https://developer.android.com/codelabs/basic-android-kotlin-compose-training-change-app-icon)。

## 维护

运行 `swift scripts/generate-launcher-bitmaps.swift` 从现有矢量生成PNG；不单独改一套图案。

运行 `python3 scripts/check-apk-icon.py <APK> --aapt <Android SDK/build-tools/36.1.0/aapt>` 验证包元信息暴露的图标确为可提取PNG。

交付：dist/铺记-v2.0.1.apk，SHA-256 `a10b097c7607aa98d94cfa9ccdf60dcc2cf22eb7ebd32738309a2a373132fa04`。独立PNG图标为 docs/design/2026-10-31-cream/icon-distribution-512.png。
