package io.github.iammaplezeng.flowmeter;

import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** 一次查询得到的完整快照。 */
public class Snapshot {

    public String device = "";
    public String mobile = "";
    public String packageName = "";
    public String startDate = "";
    public String expireDate = "";
    public String endTime = "";

    public double todayMb = 0;
    public double monthGb = 0;
    public double totalGb = 0;
    public double usedGb = 0;
    public double remainGb = 0;

    public int daysLeft = 0;
    public int usedDays = 0;
    public int totalDays = 0;

    public long updatedAt = 0;

    public void computeDaysLeft() {
        String src = endTime != null && !endTime.isEmpty() ? endTime : expireDate;
        if (src == null || src.isEmpty()) {
            daysLeft = 0;
            return;
        }
        try {
            LocalDate end;
            if (src.length() >= 10) {
                end = LocalDate.parse(src.substring(0, 10));
            } else {
                return;
            }
            daysLeft = (int) ChronoUnit.DAYS.between(LocalDate.now(), end);
            if (daysLeft < 0) daysLeft = 0;
        } catch (Exception ignored) { }
    }

    public int percentUsed() {
        if (totalGb <= 0) return 0;
        int v = (int) Math.round(usedGb / totalGb * 100);
        return Math.max(0, Math.min(100, v));
    }

    /** MB 自适应显示。 */
    public static String fmtMb(double mb) {
        if (mb >= 1024) return String.format(Locale.US, "%.2f GB", mb / 1024d);
        return String.format(Locale.US, "%.2f MB", mb);
    }

    public static String fmtGb(double gb) {
        if (gb > 0 && gb < 0.01) return "0.00 GB";
        return String.format(Locale.US, "%.2f GB", gb);
    }

    public String todayText() {
        return fmtMb(todayMb);
    }

    public String updatedText() {
        if (updatedAt <= 0) return "尚未刷新";
        return "更新于 " + LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(updatedAt),
                java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
    }

    public String expireText() {
        String d = expireDate != null && !expireDate.isEmpty() ? expireDate : endTime;
        if (d == null || d.isEmpty()) return "—";
        return d.length() >= 10 ? d.substring(0, 10) : d;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("device", device);
            o.put("mobile", mobile);
            o.put("packageName", packageName);
            o.put("startDate", startDate);
            o.put("expireDate", expireDate);
            o.put("endTime", endTime);
            o.put("todayMb", todayMb);
            o.put("monthGb", monthGb);
            o.put("totalGb", totalGb);
            o.put("usedGb", usedGb);
            o.put("remainGb", remainGb);
            o.put("daysLeft", daysLeft);
            o.put("usedDays", usedDays);
            o.put("totalDays", totalDays);
            o.put("updatedAt", updatedAt);
        } catch (Exception ignored) { }
        return o;
    }

    public static Snapshot fromJson(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            JSONObject o = new JSONObject(json);
            Snapshot s = new Snapshot();
            s.device = o.optString("device");
            s.mobile = o.optString("mobile");
            s.packageName = o.optString("packageName");
            s.startDate = o.optString("startDate");
            s.expireDate = o.optString("expireDate");
            s.endTime = o.optString("endTime");
            s.todayMb = o.optDouble("todayMb", 0);
            s.monthGb = o.optDouble("monthGb", 0);
            s.totalGb = o.optDouble("totalGb", 0);
            s.usedGb = o.optDouble("usedGb", 0);
            s.remainGb = o.optDouble("remainGb", 0);
            s.daysLeft = o.optInt("daysLeft", 0);
            s.usedDays = o.optInt("usedDays", 0);
            s.totalDays = o.optInt("totalDays", 0);
            s.updatedAt = o.optLong("updatedAt", 0);
            return s;
        } catch (Exception e) {
            return null;
        }
    }
}
