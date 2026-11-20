package io.github.iammaplezeng.flowmeter;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 与 c.js-iot.cn 流量平台后台通信（与其 H5 前端完全一致的接口）。
 */
public final class Api {

    private static final String BASE = "https://cloud-api.aiotzy.com/api";
    private static final String UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/124.0.0.0 Mobile Safari/537.36";

    public static class ApiException extends Exception {
        public ApiException(String msg) {
            super(msg);
        }
    }

    private static String post(String url, String body, String contentType, String token) throws Exception {
        Exception last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return postOnce(url, body, contentType, token);
            } catch (ApiException e) {
                last = e;
                // 网关偶发 502/503/504，重试即可
                String m = e.getMessage() == null ? "" : e.getMessage();
                if (!(m.contains("502") || m.contains("503") || m.contains("504"))) throw e;
                Thread.sleep(500L * (attempt + 1));
            }
        }
        throw last == null ? new ApiException("网络异常") : last;
    }

    private static String postOnce(String url, String body, String contentType, String token) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", contentType);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("Origin", "http://c.js-iot.cn");
        c.setRequestProperty("Referer", "http://c.js-iot.cn/");
        c.setRequestProperty("User-Agent", UA);
        if (token != null) c.setRequestProperty("Authorization", "bearer " + token);
        try (OutputStream os = c.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        int code = c.getResponseCode();
        InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
        StringBuilder sb = new StringBuilder();
        if (is != null) {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
        }
        c.disconnect();
        if (code >= 400) throw new ApiException("服务器返回 " + code);
        return sb.toString();
    }

    /** 用设备号换 accessToken。 */
    private static String token(String device) throws Exception {
        String body = "grant_type=device&client_id=zmp-h5&client_secret=VIlA38m0G2YwGagS"
                + "&cIdentity=" + URLEncoder.encode(device, "UTF-8");
        String raw = post(BASE + "/auth/oauth/token", body,
                "application/x-www-form-urlencoded", null);
        JSONObject o = new JSONObject(raw);
        JSONObject data = o.optJSONObject("data");
        if (data == null || data.optString("accessToken").isEmpty()) {
            throw new ApiException("设备号不存在或已停用");
        }
        return data.optString("accessToken");
    }

    private static JSONObject call(String path, JSONObject payload, String token) throws Exception {
        String raw = post(BASE + path, payload.toString(), "application/json", token);
        JSONObject o = new JSONObject(raw);
        if (!"200".equals(o.optString("code"))) {
            throw new ApiException(o.optString("msg", "接口返回错误"));
        }
        return o;
    }

    /** 拉取设备的全部用量数据。 */
    public static Snapshot fetch(String device) throws Exception {
        Snapshot s = new Snapshot();
        s.device = device;
        s.updatedAt = System.currentTimeMillis();

        String tk = token(device);
        JSONObject p = new JSONObject().put("terminalId", device);

        JSONObject dev = call("/device/h5/index/getDeviceInfo",
                new JSONObject().put("imei", device), tk).optJSONObject("data");
        if (dev != null) {
            s.mobile = dev.optString("mobile", "");
            s.packageName = dev.optString("goodsBillingGroupName", "");
        }

        try {
            JSONObject d = call("/meter/h5/meterBaseMeter/getEndTimeAndTodayUsage", p, tk)
                    .optJSONObject("data");
            if (d != null) {
                s.todayMb = d.optDouble("todayUsage", 0);
                s.endTime = d.optString("endTime", "");
            }
        } catch (Exception ignored) { }

        try {
            JSONObject o = call("/meter/h5/meterBaseMeter/monthlyUsageInfo", p, tk);
            s.monthGb = o.optDouble("data", 0);
        } catch (Exception ignored) { }

        try {
            JSONObject o = call("/meter/h5/meterBaseMeter/usageGbInfo",
                    new JSONObject().put("terminalId", device).put("meterType", "2"), tk);
            JSONObject d = o.optJSONObject("data");
            if (d != null) {
                s.totalGb = d.optDouble("total", 0);
                s.usedGb = d.optDouble("usage", 0);
                s.remainGb = d.optDouble("availability", 0);
                if (!d.optString("packageName").isEmpty()) s.packageName = d.optString("packageName");
            }
        } catch (Exception ignored) { }

        if (s.totalGb <= 0) {
            // 兜底：用量以 MB 返回
            try {
                JSONObject d = call("/meter/h5/meterBaseMeter/getCurrentMeter", p, tk)
                        .optJSONObject("data");
                if (d != null) {
                    JSONObject first = null;
                    java.util.Iterator<String> it = d.keys();
                    if (it.hasNext()) first = d.optJSONObject(it.next());
                    if (first != null) {
                        double total = first.optDouble("total", 0);
                        double remain = first.optDouble("remain", 0);
                        s.totalGb = total / 1024d;
                        s.remainGb = remain / 1024d;
                        s.usedGb = (total - remain) / 1024d;
                        if (s.endTime.isEmpty()) s.endTime = first.optString("endTime", "");
                    }
                }
            } catch (Exception ignored) { }
        }

        try {
            JSONObject d = call("/meter/h5/meterBaseMeter/getUsagePackage", p, tk)
                    .optJSONObject("data");
            if (d != null) {
                s.startDate = d.optString("startTime", "");
                s.expireDate = d.optString("endTime", "");
                s.usedDays = (int) Double.parseDouble(d.optString("usageDay", "0").trim());
                s.totalDays = (int) Double.parseDouble(d.optString("totalDay", "0").trim());
                if (!d.optString("packageName").isEmpty()
                        && (s.packageName == null || s.packageName.isEmpty())) {
                    s.packageName = d.optString("packageName");
                }
            }
        } catch (Exception ignored) { }

        s.computeDaysLeft();
        return s;
    }
}
