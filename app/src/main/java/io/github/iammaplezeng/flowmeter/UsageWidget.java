package io.github.iammaplezeng.flowmeter;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Locale;

/** 桌面小组件：今日用量 / 套餐剩余 / 到期天数。 */
public class UsageWidget extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "io.github.iammaplezeng.flowmeter.ACTION_REFRESH";

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] appWidgetIds) {
        for (int id : appWidgetIds) render(context, mgr, id);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        final String action = intent.getAction();
        boolean needFetch = ACTION_REFRESH.equals(action)
                || AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)
                || AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED.equals(action);
        if (!needFetch) return;

        final PendingResult pr = goAsync();
        new Thread(() -> {
            try {
                Repo.refresh(context);
            } catch (Exception ignored) {
            } finally {
                Repo.updateWidgets(context);
                pr.finish();
            }
        }, "widget-fetch").start();
    }

    static void render(Context ctx, AppWidgetManager mgr, int id) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_usage);
        Snapshot s = Prefs.loadSnapshot(ctx);
        String dev = Prefs.device(ctx);

        if (s == null) {
            rv.setTextViewText(R.id.wDev, dev.isEmpty() ? "未设置设备号" : "设备号 " + dev);
            rv.setTextViewText(R.id.wToday, "—");
            rv.setTextViewText(R.id.wLabel, "打开应用设置设备号");
            rv.setTextViewText(R.id.wRemain, "点击卡片开始查询");
            rv.setTextViewText(R.id.wDays, "");
            rv.setTextViewText(R.id.wUpdated, "点右上角刷新");
            rv.setInt(R.id.wBar, "setImageLevel", 0);
        } else {
            rv.setTextViewText(R.id.wDev, "设备号 " + s.device);
            if (s.todayMb >= 1024) {
                rv.setTextViewText(R.id.wToday,
                        String.format(Locale.US, "%.2f", s.todayMb / 1024d) + " GB");
            } else {
                rv.setTextViewText(R.id.wToday,
                        String.format(Locale.US, "%.2f", s.todayMb) + " MB");
            }
            rv.setTextViewText(R.id.wLabel, "今日用量");
            rv.setTextViewText(R.id.wRemain, "剩余 " + Snapshot.fmtGb(s.remainGb));
            rv.setTextViewText(R.id.wDays, s.daysLeft > 0 ? s.daysLeft + " 天后到期" : "已到期");
            rv.setTextViewText(R.id.wUpdated, s.updatedText() + " · 点右上角刷新");
            rv.setInt(R.id.wBar, "setImageLevel", s.percentUsed() * 100);
        }
        rv.setViewVisibility(R.id.wDays, View.VISIBLE);

        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.wRoot, pi);

        Intent ref = new Intent(ctx, UsageWidget.class).setAction(ACTION_REFRESH);
        PendingIntent rpi = PendingIntent.getBroadcast(ctx, 1, ref,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.wRefresh, rpi);

        mgr.updateAppWidget(id, rv);
    }
}
