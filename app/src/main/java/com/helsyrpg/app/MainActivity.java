package com.helsyrpg.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.chaquo.python.Python;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {
    private static final String APP_VERSION = "5.20";
    private static final String SERVER_URL = "http://127.0.0.1:8765/";
    private static final Set<String> PERSISTENT_ROOTS = new HashSet<>(Arrays.asList(
            "helsy.db", "config.json", "saves", "backups", "arts"
    ));

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showStatus("Подготовка HelsyRPG v5.20…");

        new Thread(() -> {
            try {
                File runtimeDir = prepareRuntime();
                startPython(runtimeDir);
                if (!waitForServer()) {
                    showFailure("Сервер HelsyRPG не запустился. Перезапусти приложение.");
                    return;
                }
                runOnUiThread(this::openGame);
            } catch (Throwable t) {
                showFailure("Ошибка запуска: " + t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage()));
            }
        }, "helsy-bootstrap").start();
    }

    private void showStatus(String text) {
        TextView status = new TextView(this);
        status.setText(text);
        status.setTextSize(18f);
        status.setPadding(32, 64, 32, 32);
        setContentView(status, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void showFailure(String message) {
        runOnUiThread(() -> showStatus(message));
    }

    private File prepareRuntime() throws IOException {
        File runtime = new File(getFilesDir(), "helsy_runtime");
        if (!runtime.exists() && !runtime.mkdirs()) {
            throw new IOException("Не удалось создать runtime directory");
        }

        File marker = new File(runtime, ".version");
        String installed = marker.isFile() ? readSmallFile(marker).trim() : "";
        if (!APP_VERSION.equals(installed)) {
            clearForUpgrade(runtime);
            extractRuntime(runtime);
            writeSmallFile(marker, APP_VERSION + "\n");
        }
        return runtime;
    }

    private void clearForUpgrade(File runtime) {
        File[] entries = runtime.listFiles();
        if (entries == null) return;
        for (File entry : entries) {
            if (PERSISTENT_ROOTS.contains(entry.getName())) continue;
            deleteRecursively(entry);
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursively(child);
            }
        }
        file.delete();
    }

    private void extractRuntime(File runtime) throws IOException {
        String runtimeCanonical = runtime.getCanonicalPath() + File.separator;
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(getAssets().open("helsy_runtime.zip")))) {
            ZipEntry entry;
            byte[] buffer = new byte[64 * 1024];
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.isEmpty()) continue;

                String root = name.contains("/") ? name.substring(0, name.indexOf('/')) : name;
                File out = new File(runtime, name);
                String outCanonical = out.getCanonicalPath();
                if (!(outCanonical + (entry.isDirectory() ? File.separator : "")).startsWith(runtimeCanonical)) {
                    throw new IOException("Недопустимый путь в runtime: " + name);
                }

                boolean preserveExisting = PERSISTENT_ROOTS.contains(root) && out.exists();
                if (preserveExisting) continue;

                if (entry.isDirectory()) {
                    if (!out.exists() && !out.mkdirs()) {
                        throw new IOException("Не удалось создать " + out);
                    }
                } else {
                    File parent = out.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        throw new IOException("Не удалось создать " + parent);
                    }
                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(out))) {
                        int n;
                        while ((n = zis.read(buffer)) > 0) bos.write(buffer, 0, n);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private String readSmallFile(File file) throws IOException {
        byte[] data = new byte[(int) Math.min(file.length(), 4096)];
        try (FileInputStream in = new FileInputStream(file)) {
            int n = in.read(data);
            return n <= 0 ? "" : new String(data, 0, n, StandardCharsets.UTF_8);
        }
    }

    private void writeSmallFile(File file, String text) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void startPython(File runtimeDir) {
        Thread serverThread = new Thread(() -> {
            Python py = Python.getInstance();
            py.getModule("launcher").callAttr("run_server", runtimeDir.getAbsolutePath());
        }, "helsy-python-server");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private boolean waitForServer() {
        for (int i = 0; i < 120; i++) {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(SERVER_URL + "health").openConnection();
                conn.setConnectTimeout(500);
                conn.setReadTimeout(1000);
                conn.setUseCaches(false);
                if (conn.getResponseCode() == 200) return true;
            } catch (IOException ignored) {
            } finally {
                if (conn != null) conn.disconnect();
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void openGame() {
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebViewClient(new WebViewClient());
        setContentView(webView);
        webView.loadUrl(SERVER_URL);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
