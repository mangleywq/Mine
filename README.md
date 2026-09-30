# Mine 我的记账本（社区续改版）

这是基于 [coderpage/Mine](https://github.com/coderpage/Mine) 继续维护的 Android 记账应用。原作者 **coderpage** 将它介绍为一个简单、干净的记账本；本仓库由 **mangleywq** 在原项目基础上进行个人需求定制，**不是原作者的官方更新**。应用包名仍为 `com.coderpage.mine`。

## 下载与安装

**[下载 Mine 0.8.0 APK](https://github.com/mangleywq/Mine/raw/refs/heads/master/releases/Mine-0.8.0.apk)**（约 2.6 MB）

APK SHA-256：`8613315319D540805D5A05217D676BAB5179777D3488E22C5585D0704D76538A`

这是使用 Android 调试签名构建的安装包。安装前建议先在旧版应用中导出备份文件并妥善保存。若手机提示“无法覆盖安装”或“签名不一致”，请先保留备份；卸载旧版会清除其本地数据，之后需要在新版中导入备份。此版本不再联网检查或下载安装更新，以后需手动下载新版 APK。

## 在原项目基础上增加和调整的功能

- **大额支出**：单独记录非经常性支出，不计入普通月支出、预算或图表；支持备份和恢复。
- **定期支出**：可设每月 1 日或 15 日自动记账，默认 1 日，账单时间为当天 12:00；保留已生成的普通账单。
- **分类删除**：从今后的分类选择中隐藏，保留旧账的分类名称和图标；定期支出正在使用的分类及每类最后一个分类不能删除。
- **日期与图表**：普通账单可选择未来日期；长按日柱状图约 1 秒可打开当天记录。
- **界面和体积**：统一新增页面的按键风格，移除旧自动更新和统计组件，并压缩 APK。

旧版备份可以导入；新版备份还包含大额支出、定期支出和已隐藏分类信息。恢复前建议另外保存一份原始备份文件。

## 构建

项目沿用 Java、Android Support Library 和 Room。需要 **JDK 11**、**Android SDK Platform 27**、兼容的 **SDK Build Tools 30.0.3 或更新版本**。Gradle Wrapper 为 7.3.3，Android Gradle Plugin 为 7.2.2。用 Android Studio 打开项目根目录，等待同步完成后可运行 `app`；Windows 命令行可执行：

```powershell
.\gradlew.bat :app:assembleOfficialDebug
```

APK 输出在 `app/build/outputs/apk/official/debug/`。`local.properties` 由 Android Studio 写入本机 SDK 路径，不应上传到仓库。正式发布时应配置自己的签名；本仓库提供的 APK 是调试签名版本。

## 原项目与素材说明

- 原项目：[coderpage/Mine](https://github.com/coderpage/Mine)，原作者 **coderpage**。本仓库保留其原有的记账功能、Java/Android 项目结构和相关素材，并在此基础上修改。
- 原作者在 README 中注明图标来自 [iconfont](https://www.iconfont.cn/) 和 [Material Design Icons](https://github.com/google/material-design-icons)。请分别遵循素材来源的使用条件。
- 原仓库未附带明确的 `LICENSE` 文件；本仓库不另行声明对原作者代码的许可或所有权。
- 原 README 曾提供应用宝下载地址；该地址指向原作者版本，不代表本仓库的 APK。本仓库的下载入口见上文。
