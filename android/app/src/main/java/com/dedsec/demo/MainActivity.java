package com.dedsec.demo;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.File;
import java.util.Locale;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(9, 10, 10));
        window.setNavigationBarColor(Color.rgb(9, 10, 10));
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(9, 10, 10));
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(true);

        webView.addJavascriptInterface(new DeviceBridge(this), "DeviceBridge");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        setContentView(webView);
        webView.loadUrl("file:///android_asset/www/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    public static final class DeviceBridge {
        private final Context context;
        DeviceBridge(Context context) { this.context = context.getApplicationContext(); }

        @JavascriptInterface
        public String getDeviceInfoJson() {
            JSONObject out = new JSONObject();
            try {
                out.put("manufacturer", Build.MANUFACTURER == null ? "" : Build.MANUFACTURER);
                out.put("model", Build.MODEL == null ? "Android device" : Build.MODEL);
                out.put("androidVersion", Build.VERSION.RELEASE == null ? "" : Build.VERSION.RELEASE);
                out.put("sdkInt", Build.VERSION.SDK_INT);
                out.put("batteryPercent", batteryPercent());

                ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (manager != null) {
                    ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
                    manager.getMemoryInfo(memory);
                    out.put("ramTotalMb", Math.max(0, memory.totalMem / (1024L * 1024L)));
                    out.put("ramAvailableMb", Math.max(0, memory.availMem / (1024L * 1024L)));
                }

                File data = Environment.getDataDirectory();
                StatFs stat = new StatFs(data.getAbsolutePath());
                double totalGb = stat.getTotalBytes() / 1073741824.0;
                double freeGb = stat.getAvailableBytes() / 1073741824.0;
                out.put("storageTotalGb", roundOne(totalGb));
                out.put("storageFreeGb", roundOne(freeGb));

                String type = "OFFLINE";
                boolean connected = false;
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    Network active = cm.getActiveNetwork();
                    NetworkCapabilities caps = active == null ? null : cm.getNetworkCapabilities(active);
                    if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                        connected = true;
                        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) type = "Wi-Fi";
                        else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) type = "MOBILE DATA";
                        else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) type = "ETHERNET";
                        else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) type = "VPN";
                        else type = "CONNECTED";
                    }
                }
                out.put("networkType", type);
                out.put("connected", connected);
            } catch (Exception ignored) {
                try { out.put("connected", false); out.put("networkType", "UNAVAILABLE"); } catch (Exception ignoredAgain) {}
            }
            return out.toString();
        }

        private int batteryPercent() {
            try {
                BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
                if (bm != null) {
                    int value = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                    if (value >= 0 && value <= 100) return value;
                }
            } catch (Exception ignored) {}
            try {
                Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (battery != null) {
                    int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    if (level >= 0 && scale > 0) return Math.round(level * 100f / scale);
                }
            } catch (Exception ignored) {}
            return -1;
        }

        private double roundOne(double value) {
            return Math.round(value * 10.0) / 10.0;
        }
    }
}
