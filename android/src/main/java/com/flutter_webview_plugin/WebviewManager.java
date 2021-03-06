package com.flutter_webview_plugin;

import android.util.Log;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.*;
import android.widget.FrameLayout;

import com.flutter_webview_plugin.BrowserChromeClient;
import com.flutter_webview_plugin.BrowserClient;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lejard_h on 20/12/2017.
 */

class WebviewManager {

    boolean closed = false;
    WebView webView;
    String token;

    WebviewManager(Activity activity, List<String> interceptUrls) {
        this.webView = new WebView(activity);
        WebViewClient webViewClient = new BrowserClient(interceptUrls);
        WebChromeClient webChromeClient = new BrowserChromeClient(activity);
        webView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_BACK:
                            if (webView.canGoBack()) {
                                webView.goBack();
                                webGoBack();
                            } else {
                                close();
                            }
                            return true;
                    }
                }

                return false;
            }
        });
        webView.setWebViewClient(webViewClient);
        webView.setWebChromeClient(webChromeClient);
    }

    private void clearCookies() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().removeAllCookies(new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean aBoolean) {

                }
            });
        } else {
            CookieManager.getInstance().removeAllCookie();
        }
    }

    private void clearCache() {
        webView.clearCache(true);
        webView.clearFormData();
    }

    void openUrl(boolean withJavascript, boolean clearCache, boolean hidden, boolean clearCookies, String userAgent, String url, boolean withZoom, boolean withLocalStorage, Map<String, String> additionalHttpHeaders) {
        webView.getSettings().setJavaScriptEnabled(withJavascript);
        webView.getSettings().setBuiltInZoomControls(withZoom);
        webView.getSettings().setSupportZoom(withZoom);
        webView.getSettings().setDomStorageEnabled(withLocalStorage);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        token = additionalHttpHeaders.get("hexindai-token");
        webView.addJavascriptInterface(new JavaScriptinterface(token), "App");
        if (clearCache) {
            clearCache();
        }

        if (hidden) {
            webView.setVisibility(View.INVISIBLE);
        }

        if (clearCookies) {
            clearCookies();
        }

        if (userAgent != null) {
            webView.getSettings().setUserAgentString(userAgent);
        }
        webView.loadUrl(url, additionalHttpHeaders);
    }

    void close(boolean goBack, MethodChannel.Result result) {
        if (goBack && webView.canGoBack()) {
            webView.goBack();
            webGoBack();
        } else {
            if (webView != null) {
                ViewGroup vg = (ViewGroup) (webView.getParent());
                vg.removeView(webView);
            }
            webView = null;
            if (result != null) {
                result.success(null);
            }
            closed = true;
            FlutterWebviewPlugin.channel.invokeMethod("onDestroy", null);
        }
    }

    void close() {
        close(false, null);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    void eval(MethodCall call, final MethodChannel.Result result) {
        String code = call.argument("code");

        webView.evaluateJavascript(code, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                result.success(value);
            }
        });
    }

    void resize(FrameLayout.LayoutParams params) {
        webView.setLayoutParams(params);
    }

    void webGoBack() {
        FlutterWebviewPlugin.channel.invokeMethod("onWebGoBack", null);
    }

    public class JavaScriptinterface {
        String token;

        private JavaScriptinterface(String token) {
            this.token = token;
        }

        @JavascriptInterface
        public String getToken() {
            return token;
        }
    }

}
