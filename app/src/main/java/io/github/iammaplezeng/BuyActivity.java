package io.github.iammaplezeng;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * 套餐购买/续费页。
 *
 * 该 H5 自带「携参免登录」能力：login?redirectUrl=/&cIdentity=设备号
 * 打开即自动用设备号完成登录并跳转，用户无需再手动输入号码。
 */
public class BuyActivity extends Activity {

    private static final String BASE = "http://c.js-iot.cn/c/";

    private WebView web;
    private String buyUrl = BASE;

    /** 拼装免登录链接：带上当前设备号，页面会自动提交登录。 */
    static String buildUrl(String device) {
        StringBuilder sb = new StringBuilder(BASE)
                .append("#/login?redirectUrl=/&_t=")
                .append(System.currentTimeMillis());
        if (device != null && !device.isEmpty()) {
            sb.append("&cIdentity=").append(Uri.encode(device));
        }
        return sb.toString();
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_buy);

        String dev = Prefs.device(this);
        buyUrl = buildUrl(dev);
        ((TextView) findViewById(R.id.tvBuyTitle))
                .setText(dev.isEmpty() ? "购买套餐" : "购买套餐 · " + dev);

        findViewById(R.id.btnBuyClose).setOnClickListener(v -> finish());
        findViewById(R.id.btnBuyMore).setOnClickListener(v -> showMore());

        web = findViewById(R.id.webBuy);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                return handleUrl(req.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                return handleUrl(Uri.parse(url));
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                super.onPageFinished(v, url);
                // 免登链路万一没走通，兜底把设备号填进输入框（不自动点击，避免重复提交）
                if (url != null && url.contains("/login")) {
                    String dev = Prefs.device(BuyActivity.this);
                    if (!dev.isEmpty()) {
                        v.postDelayed(() -> {
                            try {
                                v.evaluateJavascript(prefillJs(dev), null);
                            } catch (Exception ignored) {
                            }
                        }, 2200);
                    }
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int p) {
                ProgressBar pb = findViewById(R.id.pbBuy);
                pb.setProgress(p);
                pb.setVisibility(p >= 100 ? View.GONE : View.VISIBLE);
            }
        });

        applyBarStyle();
        web.loadUrl(buyUrl);
    }

    /** 兜底脚本：把设备号写进登录框（兼容 React 受控输入）。 */
    private String prefillJs(String dev) {
        return "(function(){var d='" + dev + "';var n=0;var t=setInterval(function(){"
                + "n++;var el=document.querySelector('input');"
                + "if(el&&!el.value){try{var s=Object.getOwnPropertyDescriptor("
                + "window.HTMLInputElement.prototype,'value').set;s.call(el,d);"
                + "el.dispatchEvent(new Event('input',{bubbles:true}));}catch(e){}"
                + "clearInterval(t);}if(n>25)clearInterval(t);},300);})();";
    }

    /** 站点内链接留在 WebView，微信/支付宝等支付 scheme 交给系统处理。 */
    private boolean handleUrl(Uri uri) {
        if (uri == null) return false;
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.US);
        if (scheme.equals("http") || scheme.equals("https")
                || scheme.equals("about") || scheme.equals("javascript")
                || scheme.equals("data")) {
            return false;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception e) {
            toast("手机上没有可处理该链接的应用");
        }
        return true;
    }

    private void showMore() {
        final String[] items = {"在微信中打开", "复制链接", "用系统浏览器打开", "刷新页面"};
        new AlertDialog.Builder(this)
                .setTitle("更多操作")
                .setItems(items, (d, i) -> {
                    if (i == 0) openInWeChat();
                    else if (i == 1) copyLink();
                    else if (i == 2) openBrowser();
                    else web.reload();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 把带设备号的免登录链接发到微信，在微信里点开即用微信内置浏览器并自动登录。 */
    private void openInWeChat() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, buyUrl);
        send.putExtra(Intent.EXTRA_SUBJECT, "套餐购买");
        Intent wx = new Intent(send);
        wx.setPackage("com.tencent.mm");
        try {
            startActivity(wx);
            toast("发给好友或「文件传输助手」，在微信里点链接即可");
        } catch (Exception e) {
            try {
                startActivity(Intent.createChooser(send, "分享链接"));
            } catch (Exception e2) {
                toast("没有找到可分享的应用");
            }
        }
    }

    private void copyLink() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("购买套餐链接", buyUrl));
                toast("链接已复制，可粘贴到微信里打开");
            }
        } catch (Exception e) {
            toast("复制失败");
        }
    }

    private void openBrowser() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(buyUrl)));
        } catch (Exception e) {
            toast("没有找到可用的浏览器");
        }
    }

    private void applyBarStyle() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getWindow().setStatusBarColor(Color.WHITE);
                WindowInsetsController c = getWindow().getInsetsController();
                if (c != null) {
                    c.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().setStatusBarColor(Color.WHITE);
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        try {
            if (web != null) {
                web.setVisibility(View.GONE);
                web.destroy();
            }
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
