# 流量小助手 · FlowMeter

**中文** | [English](README.en.md)

一个轻量的 Android 物联网卡流量查询工具。输入设备号即可查看今日用量、当月用量、套餐剩余与到期时间，并支持桌面小组件常驻显示。

> 数据来自第三方流量平台 [c.js-iot.cn](http://c.js-iot.cn/c/)，本应用只是把它 H5 页面里展示的信息搬到手机上，不代理任何业务，也不改动账户数据。

---

## 功能

**首页**
- 今日用量大号气泡展示，顶部提示套餐剩余天数
- 四张数据卡：今日用量 / 当月用量 / 套餐剩余 / 套餐用量
- 套餐卡：套餐名、有效期、已用天数（如 9/30 天）、用量进度条
- 下拉即刷新，点气泡也能刷新

**设备号管理**
- 首次打开弹框输入，保存在本地（SharedPreferences），之后打开直接出数据
- 右上角铅笔图标可随时更换设备号，换完自动重新登录查询
- 设备号只存本机，不上传到任何服务器

**桌面组件**
- **4×2 标准组件**：设备号、今日用量大字、剩余流量、到期天数、用量进度条
- **2×2 迷你组件**：今日用量大字 + 剩余流量 + 到期天数 + 更新时间 + 刷新按钮
- 两个组件可同时使用，点右上角小箭头独立刷新（一次刷新两者同步）
- 系统每 30 分钟自动刷新；App 内刷新后组件同步更新
- 已适配小米 HyperOS（组件用纯图片绘制进度条，规避部分桌面渲染兼容问题）

**购买套餐**
- 首页一键进入内置 WebView 打开平台 H5
- URL 携带 `cIdentity` 参数，利用平台自带的「携参免登录」能力，**自动用当前设备号登录并跳转**，无需再输号码
- 页面内点微信/支付宝支付会正常拉起对应 App
- 支持分享链接到微信、复制链接、改用系统浏览器打开

---

## 下载安装

到 [Releases](https://github.com/IamMapleZeng/FlowMeter/releases) 下载最新 APK，传到手机安装即可（需允许「安装未知来源应用」）。

> APK 使用 debug 证书签名，仅适合自用/侧载，不适合上架应用商店。

---

## 构建

需要 JDK 17 与 Android SDK（Platform 37 / Build-Tools 36+）。

```bash
git clone https://github.com/IamMapleZeng/FlowMeter.git
cd FlowMeter
./gradlew assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`

Windows 下首次构建若提示找不到 SDK，在项目根目录新建 `local.properties`：

```properties
sdk.dir=C\:\\Users\\<你的用户名>\\AppData\\Local\\Android\\Sdk
```

---

## 技术栈

| 项 | 值 |
| --- | --- |
| 语言 | Java 17 |
| 包名 / applicationId | `io.github.iammaplezeng.flowmeter` |
| compileSdk / targetSdk | 37（Android 17） |
| minSdk | 26（Android 8.0） |
| Gradle / AGP | 8.13 / 8.13.2 |
| 第三方依赖 | 无（纯 Android Framework，未使用 AndroidX） |
| 体积 | 约 48 KB |

零依赖是刻意的：整个 App 只用了 `HttpURLConnection`、原生 View 与 `RemoteViews`，构建快、包体小、不会有依赖冲突。

---

## 接口说明

App 直接调用平台 H5 使用的接口（`https://cloud-api.aiotzy.com/api`，全部 POST）：

1. **取 token**：`/auth/oauth/token`
   form 表单：`grant_type=device&client_id=zmp-h5&client_secret=<H5 内置的 client_secret>&cIdentity=<设备号>`
2. **设备信息**：`/device/h5/index/getDeviceInfo` `{imei}`（注意只认 `imei`，传 `deviceId` 会返回 `507 手机号|imei必传其一`）
3. **今日用量 + 到期时间**：`/meter/h5/meterBaseMeter/getEndTimeAndTodayUsage` `{terminalId}`
4. **当月用量**：`/meter/h5/meterBaseMeter/monthlyUsageInfo` `{terminalId}`
5. **套餐总量/已用/剩余**：`/meter/h5/meterBaseMeter/usageGbInfo` `{terminalId, meterType:"2"}`
6. **套餐起止日期与天数**：`/meter/h5/meterBaseMeter/getUsagePackage` `{terminalId}`
7. **兜底用量**：`/meter/h5/meterBaseMeter/getCurrentMeter` `{terminalId}`

所有请求需带请求头 `Authorization: bearer <accessToken>`；网关偶发 502/503/504，`Api.java` 里做了 3 次退避重试。

**购买页免登录**：平台 H5 自身支持 `login?redirectUrl=/&cIdentity=<设备号>`，页面加载时会自动完成 OAuth 登录并跳转，App 的购买页正是利用这一点。

---

## 项目结构

```
app/src/main/java/io/github/iammaplezeng/flowmeter/
├── Api.java          接口封装（token、各用量接口、重试）
├── Snapshot.java     一次查询的完整数据 + 格式化
├── Prefs.java        本地存储（设备号、数据快照）
├── Repo.java         刷新逻辑 + 组件联动
├── MainActivity.java 首页
├── BubbleView.java   顶部用量气泡（自绘）
├── BuyActivity.java  购买页（WebView + 免登录）
├── UsageWidget.java  4×2 桌面组件
└── MiniWidget.java   2×2 迷你组件
```

---

## 已知限制

- 详情页那条「日/月使用量」折线图未实现：对应接口不在主 bundle 内，没能确认真实参数
- 若平台改版接口，App 会失效；届时改 `Api.java` 里的路径即可
- 若账户在平台侧开启了「H5 强制手机验证」，免登录会停在短信验证页（平台安全策略，无法绕过）

---

## 免责声明

本项目为个人自用工具，与流量平台方无任何关联。接口为公开 H5 前端所用接口，若平台方认为不妥，请联系删除。请勿用于批量抓取或商业用途。
