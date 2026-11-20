package io.github.iammaplezeng.flowmeter;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

/** 数据刷新 + 组件联动。 */
public final class Repo {

    private Repo() { }

    public static Snapshot refresh(Context ctx) throws Exception {
        String dev = Prefs.device(ctx);
        if (dev.isEmpty()) throw new Api.ApiException("请先设置设备号");
        Snapshot s = Api.fetch(dev);
        Prefs.saveSnapshot(ctx, s.toJson());
        return s;
    }

    public static void updateWidgets(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        for (int id : mgr.getAppWidgetIds(new ComponentName(ctx, UsageWidget.class))) {
            UsageWidget.render(ctx, mgr, id);
        }
        for (int id : mgr.getAppWidgetIds(new ComponentName(ctx, MiniWidget.class))) {
            MiniWidget.render(ctx, mgr, id);
        }
    }
}
