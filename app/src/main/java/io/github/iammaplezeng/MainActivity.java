package io.github.iammaplezeng;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView tvDevice, tvMobile, tvBubbleHint, tvBubbleValue, tvBubbleUnit;
    private TextView tvToday, tvMonth, tvRemain, tvUsed;
    private TextView tvPkgName, tvPkgDate, tvPkgDays, tvUpdated;
    private ProgressBar pbPkg;

    private volatile boolean loading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applyWindowInsets();

        tvDevice = findViewById(R.id.tvDevice);
        tvMobile = findViewById(R.id.tvMobile);
        tvBubbleHint = findViewById(R.id.tvBubbleHint);
        tvBubbleValue = findViewById(R.id.tvBubbleValue);
        tvBubbleUnit = findViewById(R.id.tvBubbleUnit);
        tvToday = findViewById(R.id.tvToday);
        tvMonth = findViewById(R.id.tvMonth);
        tvRemain = findViewById(R.id.tvRemain);
        tvUsed = findViewById(R.id.tvUsed);
        tvPkgName = findViewById(R.id.tvPkgName);
        tvPkgDate = findViewById(R.id.tvPkgDate);
        tvPkgDays = findViewById(R.id.tvPkgDays);
        tvUpdated = findViewById(R.id.tvUpdated);
        pbPkg = findViewById(R.id.pbPkg);

        findViewById(R.id.btnRefresh).setOnClickListener(v -> refresh(true));
        findViewById(R.id.btnSettings).setOnClickListener(v -> askDevice(false));
        findViewById(R.id.btnAddWidget).setOnClickListener(v -> pinWidget());
        findViewById(R.id.btnBuy).setOnClickListener(v -> openBuyPage());
        findViewById(R.id.bubbleBox).setOnClickListener(v -> refresh(true));

        Snapshot cached = Prefs.loadSnapshot(this);
        if (cached != null) render(cached);

        if (Prefs.device(this).isEmpty()) {
            askDevice(true);
        } else {
            refresh(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Prefs.device(this).isEmpty() || loading) return;
        Snapshot s = Prefs.loadSnapshot(this);
        long age = s == null ? Long.MAX_VALUE : System.currentTimeMillis() - s.updatedAt;
        if (age > 3 * 60 * 1000L) refresh(false);
    }

    /** 打开套餐购买页：应用内打开，链接里带上当前设备号实现免登录。 */
    private void openBuyPage() {
        try {
            startActivity(new Intent(this, BuyActivity.class));
        } catch (Exception e) {
            toast("页面打开失败，请稍后重试");
        }
    }

    /** 一键请求添加到桌面（小米/红米等桌面支持时可直接弹确认框）。 */
    private void pinWidget() {
        String[] items = {"标准组件 4×2", "迷你组件 2×2"};
        new AlertDialog.Builder(this)
                .setTitle("添加桌面组件")
                .setItems(items, (d, which) ->
                        requestPin(which == 0 ? UsageWidget.class : MiniWidget.class))
                .setNegativeButton("取消", null)
                .show();
    }

    private void requestPin(Class<?> provider) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(this);
            ComponentName cn = new ComponentName(this, provider);
            if (m.isRequestPinAppWidgetSupported()) {
                m.requestPinAppWidget(cn, null, null);
                toast("请在弹窗中点击「添加」");
            } else {
                toast("请长按桌面空白处 → 添加小部件 → 全部应用 → 流量小助手");
            }
        } catch (Exception e) {
            toast("请长按桌面空白处 → 添加小部件 → 全部应用 → 流量小助手");
        }
    }

    /** 适配 Android 15+ 强制全面屏，让渐变延伸到状态栏后面。 */
    private void applyWindowInsets() {
        final View header = findViewById(R.id.header);
        final View footer = findViewById(R.id.footer);
        final View scroll = findViewById(R.id.scrollRoot);
        final int baseTop = header.getPaddingTop();
        final int baseBottom = footer.getPaddingBottom();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                c.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        }

        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets i = insets.getInsets(WindowInsets.Type.systemBars());
                top = i.top;
                bottom = i.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            header.setPadding(header.getPaddingLeft(), baseTop + top,
                    header.getPaddingRight(), header.getPaddingBottom());
            footer.setPadding(footer.getPaddingLeft(), footer.getPaddingTop(),
                    footer.getPaddingRight(), baseBottom + bottom);
            return insets;
        });
        scroll.requestApplyInsets();
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void askDevice(boolean first) {
        EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setText(Prefs.device(this));
        et.setSelectAllOnFocus(true);
        et.setSingleLine(true);

        FrameLayout box = new FrameLayout(this);
        int p = dp(20);
        box.setPadding(p, dp(8), p, 0);
        box.addView(et);

        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle("设备号")
                .setMessage(first ? "输入设备号即可查看流量用量，只需输入一次，之后可随时修改。"
                        : "修改设备号后会自动刷新数据。")
                .setView(box)
                .setCancelable(!first)
                .setPositiveButton("保存并查询", null)
                .setNegativeButton("取消", null)
                .create();

        d.setOnShowListener(dialog -> {
            d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String v2 = et.getText().toString().replaceAll("[^0-9A-Za-z]", "").trim();
                if (v2.isEmpty()) {
                    toast("请输入设备号");
                    return;
                }
                Prefs.setDevice(this, v2);
                d.dismiss();
                refresh(true);
            });
        });
        d.show();
        et.requestFocus();
        et.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
        }, 250);
    }

    private void refresh(boolean fromUser) {
        if (loading) return;
        final String dev = Prefs.device(this);
        if (dev.isEmpty()) {
            askDevice(true);
            return;
        }
        loading = true;
        if (fromUser) {
            View btn = findViewById(R.id.btnRefresh);
            btn.animate().rotationBy(360f).setDuration(700).start();
        }
        new Thread(() -> {
            try {
                Snapshot s = Api.fetch(dev);
                Prefs.saveSnapshot(MainActivity.this, s.toJson());
                runOnUiThread(() -> {
                    loading = false;
                    render(s);
                    Repo.updateWidgets(MainActivity.this);
                    if (fromUser) toast("已刷新");
                });
            } catch (final Exception e) {
                runOnUiThread(() -> {
                    loading = false;
                    String msg = e.getMessage();
                    if (msg == null || msg.isEmpty()) msg = "网络异常，请稍后重试";
                    toast(msg);
                    if (Prefs.loadSnapshot(MainActivity.this) == null) {
                        tvBubbleValue.setText("—");
                        tvBubbleHint.setText(msg);
                    }
                });
            }
        }, "flow-fetch").start();
    }

    private void render(Snapshot s) {
        tvDevice.setText("设备号 " + s.device);
        tvMobile.setText(s.mobile == null || s.mobile.isEmpty() ? "流量查询" : s.mobile);

        if (s.daysLeft > 0) {
            tvBubbleHint.setText("套餐 " + s.daysLeft + " 天后到期");
        } else {
            tvBubbleHint.setText("套餐已到期");
        }

        if (s.todayMb >= 1024) {
            tvBubbleValue.setText(String.format(java.util.Locale.US, "%.2f", s.todayMb / 1024d));
            tvBubbleUnit.setText("今日用量(GB)");
        } else {
            tvBubbleValue.setText(String.format(java.util.Locale.US, "%.2f", s.todayMb));
            tvBubbleUnit.setText("今日用量(MB)");
        }

        tvToday.setText(s.todayText());
        tvMonth.setText(Snapshot.fmtGb(s.monthGb));
        tvRemain.setText(Snapshot.fmtGb(s.remainGb));
        tvUsed.setText(Snapshot.fmtGb(s.usedGb));

        tvPkgName.setText(s.packageName == null || s.packageName.isEmpty() ? "当前套餐" : s.packageName);
        String date = s.expireDate.isEmpty() ? s.expireText() : s.expireDate;
        tvPkgDate.setText("有效期至 " + date
                + (s.totalDays > 0 ? "   ·   总量 " + Snapshot.fmtGb(s.totalGb) : ""));
        if (s.totalDays > 0) {
            tvPkgDays.setText("已用 " + s.usedDays + "/" + s.totalDays + " 天");
        } else {
            tvPkgDays.setText(s.daysLeft > 0 ? "剩余 " + s.daysLeft + " 天" : "");
        }
        pbPkg.setProgress(s.percentUsed());
        tvUpdated.setText(s.updatedText() + " · 点击气泡可刷新");
    }
}
