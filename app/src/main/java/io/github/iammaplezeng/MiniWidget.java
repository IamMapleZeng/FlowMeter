package io.github.iammaplezeng;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** 桌面迷你小组件（2×2）：今日用量大字 + 剩余流量 + 到期天数 + 更新时间。 */
public class MiniWidget extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "io.github.iammaplezeng.ACTION_REFRESH";

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
        }, "mini-widget-fetch").start();
    }

    static void render(Context ctx, AppWidgetManager mgr, int id) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_mini);
        Snapshot s = Prefs.loadSnapshot(ctx);

        if (s == null) {
            String dev = Prefs.device(ctx);
            rv.setTextViewText(R.id.mLabel, "今日用量");
            rv.setTextViewText(R.id.mToday, "—");
            rv.setTextViewText(R.id.mRemain, dev.isEmpty() ? "打开应用设置设备号" : "轻触刷新重试");
            rv.setTextViewText(R.id.mExpire, "");
            rv.setTextViewText(R.id.mUpdated, "点右上角刷新");
        } else {
            rv.setTextViewText(R.id.mLabel, "今日用量");
            rv.setTextViewText(R.id.mToday, s.todayText());
            rv.setTextViewText(R.id.mRemain, "剩余 " + Snapshot.fmtGb(s.remainGb));
            rv.setTextViewText(R.id.mExpire, s.daysLeft > 0 ? s.daysLeft + " 天后到期" : "已到期");
            rv.setTextViewText(R.id.mUpdated, s.updatedText());
        }

        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.mRoot, pi);

        Intent ref = new Intent(ctx, MiniWidget.class).setAction(ACTION_REFRESH);
        PendingIntent rpi = PendingIntent.getBroadcast(ctx, 2, ref,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.mRefresh, rpi);

        mgr.updateAppWidget(id, rv);
    }
}
