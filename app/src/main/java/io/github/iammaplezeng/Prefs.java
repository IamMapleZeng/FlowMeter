package io.github.iammaplezeng;

import android.content.Context;
import android.content.SharedPreferences;

/** 本地存储：设备号 + 最近一次数据快照。 */
public final class Prefs {

    private static final String NAME = "flowmeter";
    private static final String K_DEVICE = "device_no";
    private static final String K_SNAP = "snapshot";

    private Prefs() { }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static String device(Context c) {
        return sp(c).getString(K_DEVICE, "");
    }

    public static void setDevice(Context c, String device) {
        sp(c).edit().putString(K_DEVICE, device).apply();
    }

    public static void saveSnapshot(Context c, org.json.JSONObject json) {
        sp(c).edit().putString(K_SNAP, json.toString()).apply();
    }

    public static Snapshot loadSnapshot(Context c) {
        return Snapshot.fromJson(sp(c).getString(K_SNAP, ""));
    }
}
