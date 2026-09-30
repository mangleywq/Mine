# 项目维护指南

## 项目概况

- Mine 是 Java 编写的 Android 记账应用，当前版本号在根目录 `build.gradle` 中为 0.8.0，包名为 `com.coderpage.mine`。
- 源码仍使用 Android Support Library 27.1.1、compileSdk 27、minSdk 18、targetSdk 26。为适配当前 Android Studio，构建配置已调整为 Gradle Wrapper 7.3.3 和 Android Gradle Plugin 7.2.2；不要在普通功能修改中顺带进行全量 AndroidX 升级。
- `app/` 是 APK 主模块，`libbase/` 放通用 UI/工具代码，旧 `libupdate/` 模块和统计 JAR 已移除，`buildSrc/` 保留作者的旧上传插件源码但不参与本地构建，`source/` 存放更新日志和备份格式等参考文件。

## 代码入口与数据

- 应用入口：`app/src/main/AndroidManifest.xml`、`app/src/main/java/com/coderpage/mine/MineApp.java`、`app/src/main/java/com/coderpage/mine/app/tally/module/home/HomeActivity.java`。
- 记账功能主要在 `app/src/main/java/com/coderpage/mine/app/tally/`。`module/` 是页面，`data/` 和 `persistence/` 是业务数据及存储，界面资源在 `app/src/main/res/`。应用不再检查或下载安装更新。
- Room 数据库有 `app/tally/persistence/sql/TallyDatabase.java`（账目、分类，数据库名 `sql_tally`）及 `persistence/database/MineDatabase.java`（键值数据，数据库名 `sql_mine`）。修改实体或表结构时，检查版本号、迁移路径、旧数据保留以及备份/恢复兼容性；不要以清空用户数据代替迁移。
- 备份实现位于 `app/tally/module/backup/`，格式参考 `source/tally/backup_format.json`。改动记录或分类字段时同步检查导入、导出和统计相关代码。
- 删除分类通过 `category_hidden` 隐藏分类，不删除数据库行；旧记录仍需靠该行显示名称和图标。定期支出仍引用的分类，以及收入或支出的最后一个可用分类，不能隐藏。数据库从 62 到 63 的迁移和备份中的 `hidden` 字段应保持兼容；旧备份缺少该字段时按未隐藏处理。
- `largeexpense/` 是独立的大额支出功能，使用 `TallyDatabase` 中的 `large_expense` 表，入口在首页右下角菜单。此表不参与普通月支出、预算和图表；改动时保持这一隔离，并同步检查备份与恢复。
- `recurring/` 管理定期支出，规则存于 `recurring_expense` 表；新规则可选每月 1 日或 15 日，默认 1 日。新规则从下次到期日开始，应用回到首页时，`RecurringExpenseGenerator` 根据规则生成当天 12:00 的普通 `record` 支出。已有旧规则的日期需保留。生成的同步 ID 含规则 ID 和月份，用于防止重复；修改规则只影响未生成的月份。规则与已生成记录均应包含在备份中。
- 普通记录可选未来日期；“账单记录”默认查询范围应包含未来记录，图表可选月份应覆盖已有记录的最晚月份。首页“本月”按整个月统计，“今日”仍只按当天统计。

## 构建前提与命令

- 仓库附带 `gradlew.bat` 和 Gradle Wrapper JAR，但未附带 JDK、Android SDK 或依赖缓存。当前构建配置需要 JDK 11、Android SDK Platform 27 和 SDK Build Tools 30.0.3 或更新的兼容版本。旧依赖仓库包括 JCenter、JitPack 等，联网解析是否成功需实际验证。
- 根目录 `build.gradle` 无条件读取被 `.gitignore` 排除的 `local.properties`；Android Studio 会在其中写入 `sdk.dir`。普通 debug 构建使用默认调试签名，不需要作者的密钥。正式签名可选填 `keyAlias`、`keyPassword`、`keyStore`、`storePassword`；不要提交密钥或此配置文件。
- 在 Windows 上可用 `./gradlew.bat :app:assembleOfficialDebug` 构建单个渠道的 debug APK；测试可用 `./gradlew.bat :app:testOfficialDebugUnitTest`。debug 包开启代码及资源压缩以缩小 APK；若修改反射、数据绑定、Room 或备份序列化，需检查相应保留规则和实际运行效果。执行前确认本地 JDK、SDK 和依赖源可用。构建失败时区分环境/依赖问题与源码错误，并报告实际停在哪一步。
- `app/build.gradle` 的五个渠道为 `qh360`、`yingyongbao`、`xiaomi`、`huawei`、`official`。输出 APK 名称由该文件的 `applicationVariants` 逻辑生成。

## 修改约定

- 优先沿用现有 Java、Android Support、Room、Data Binding 与资源命名风格，尽量把修改限制在功能所在模块。
- 对记账金额、时间、分类、图表、搜索、备份和数据库迁移的改动，检查已有记录与旧备份的行为；能运行时优先执行相关单元测试及目标渠道构建。
- `MineApp` 不再触发版本检查或接入旧统计组件；`buildSrc` 留有连接旧上传服务的源码，但当前调试构建未启用该插件。不要把上传或发布任务当作普通构建验证，也不要在测试时提交真实令牌。
- 仓库下载副本目前没有 `.git` 元数据；若需要比较改动，应以当前文件为基线，不假定可用 Git 历史。
