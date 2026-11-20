# FlowMeter · 流量小助手

[中文](README.md) | **English**

A lightweight Android app for checking data usage on an IoT/M2M SIM card. Enter your device number once and see today's usage, this month's usage, remaining data and expiry date — plus two home-screen widgets that keep the numbers in sight.

> Data comes from the third-party platform [c.js-iot.cn](http://c.js-iot.cn/c/). This app only surfaces what its H5 page already shows; it does not resell anything or modify your account.

---

## Features

**Home screen**
- Today's usage in a large bubble, with remaining plan days shown above
- Four data cards: today / this month / remaining / used
- Plan card: plan name, validity, used days (e.g. 9/30) and a usage progress bar
- Pull or tap the bubble to refresh

**Device number**
- Asked once on first launch, stored locally (SharedPreferences); later launches load data straight away
- Any time changeable via the pencil icon — it re-authenticates and reloads automatically
- Stored only on your device, never uploaded anywhere

**Home screen widgets**
- **4×2 standard widget**: device number, today's usage, remaining data, days left, progress bar
- **2×2 mini widget**: large today's usage, remaining data, days left, last-updated time, refresh button
- Both can live on the home screen at once; each has its own refresh button, and one tap refreshes both
- Auto refresh every 30 minutes; a refresh inside the app updates the widgets too
- Xiaomi HyperOS friendly (the progress bar is drawn with plain images to avoid RemoteViews rendering issues)

**Buying / renewing a plan**
- One tap opens the platform's H5 page in an in-app WebView
- The URL carries `cIdentity`, exploiting the platform's own parameter-based login, so it **logs in with your current device number automatically** — no re-typing
- WeChat / Alipay payment links inside the page are handed off to their apps
- Share the link to WeChat, copy it, or open it in a system browser

---

## Download

Grab the latest APK from [Releases](https://github.com/IamMapleZeng/FlowMeter/releases) and sideload it (you must allow installing from unknown sources).

> The APK is signed with a debug certificate — fine for personal use and sideloading, not for store distribution.

---

## Build

Requires JDK 17 and the Android SDK (Platform 37 / Build-Tools 36+).

```bash
git clone https://github.com/IamMapleZeng/FlowMeter.git
cd FlowMeter
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

On Windows, if the SDK cannot be located, create `local.properties` in the project root:

```properties
sdk.dir=C\:\\Users\\<your-user>\\AppData\\Local\\Android\\Sdk
```

---

## Tech stack

| Item | Value |
| --- | --- |
| Language | Java 17 |
| Package / applicationId | `io.github.iammaplezeng.flowmeter` |
| compileSdk / targetSdk | 37 (Android 17) |
| minSdk | 26 (Android 8.0) |
| Gradle / AGP | 8.13 / 8.13.2 |
| Third-party deps | None (plain Android Framework, no AndroidX) |
| Size | ~48 KB |

Zero dependencies is deliberate: the whole app runs on `HttpURLConnection`, plain Views and `RemoteViews` — fast builds, tiny APK, no dependency conflicts.

---

## How the API works

The app calls the same endpoints the platform's H5 uses (`https://cloud-api.aiotzy.com/api`, all POST):

1. **Token**: `/auth/oauth/token`
   form body: `grant_type=device&client_id=zmp-h5&client_secret=<the secret bundled in the H5>&cIdentity=<device number>`
2. **Device info**: `/device/h5/index/getDeviceInfo` `{imei}` — note it only accepts `imei`; passing `deviceId` returns `507 手机号|imei必传其一`
3. **Today's usage + expiry**: `/meter/h5/meterBaseMeter/getEndTimeAndTodayUsage` `{terminalId}`
4. **This month's usage**: `/meter/h5/meterBaseMeter/monthlyUsageInfo` `{terminalId}`
5. **Plan total / used / remaining**: `/meter/h5/meterBaseMeter/usageGbInfo` `{terminalId, meterType:"2"}`
6. **Plan start/end dates and days**: `/meter/h5/meterBaseMeter/getUsagePackage` `{terminalId}`
7. **Fallback usage**: `/meter/h5/meterBaseMeter/getCurrentMeter` `{terminalId}`

Every request needs `Authorization: bearer <accessToken>`. The gateway occasionally returns 502/503/504, so `Api.java` retries up to 3 times with backoff.

**Passwordless purchase page**: the platform's H5 itself supports `login?redirectUrl=/&cIdentity=<device number>`, which triggers an automatic OAuth login on page load — that is exactly what the in-app purchase page uses.

---

## Project layout

```
app/src/main/java/io/github/iammaplezeng/flowmeter/
├── Api.java          HTTP layer (token, usage endpoints, retries)
├── Snapshot.java     Query result model + formatting
├── Prefs.java        Local storage (device number, cached snapshot)
├── Repo.java         Refresh logic + widget sync
├── MainActivity.java Home screen
├── BubbleView.java   Custom-drawn usage bubble
├── BuyActivity.java  Purchase page (WebView + auto login)
├── UsageWidget.java  4×2 widget
└── MiniWidget.java   2×2 mini widget
```

---

## Known limitations

- The day/month usage chart on the platform's detail page is not implemented — its endpoint is not in the main bundle and its parameters could not be confirmed
- If the platform changes its API the app breaks; fix the paths in `Api.java`
- If the account has "mandatory phone verification for H5" enabled, the automatic login stops at the SMS verification page (platform policy, cannot be bypassed)

---

## Disclaimer

A personal-use tool, not affiliated with the platform in any way. The endpoints used are the ones its public H5 front end already calls; if the operator objects, they will be removed on request. Please do not use this for bulk scraping or commercial purposes.
