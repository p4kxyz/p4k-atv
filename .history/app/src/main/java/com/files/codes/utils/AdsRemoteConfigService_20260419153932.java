package com.files.codes.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.files.codes.AppConfig;
import com.files.codes.MyApplication;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class AdsRemoteConfigService {
    private static final String TAG = "AdsRemoteConfigService";
    private static final String ADS_URL = "https://l.dramahay.xyz/api/add";
    private static final String PREFS_NAME = "remote_api_config";
    private static final String KEY_API_URL = "api_url";
    private static final String KEY_API_TOKEN = "api_token";

    private AdsRemoteConfigService() {
    }

    public static void refreshInBackground() {
        new Thread(() -> {
            try {
                fetchAndApply();
            } catch (Exception e) {
                Log.w(TAG, "Failed to refresh remote api/token config", e);
            }
        }, "AdsRemoteConfig").start();
    }

    public static boolean refreshAndWait(long timeoutMs) {
        AtomicBoolean success = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        new Thread(() -> {
            try {
                fetchAndApply();
                success.set(true);
            } catch (Exception e) {
                Log.w(TAG, "Failed to refresh remote api/token config (blocking)", e);
            } finally {
                latch.countDown();
            }
        }, "AdsRemoteConfigBlocking").start();

        try {
            latch.await(Math.max(timeoutMs, 0L), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (AppConfig.getCurrentApiServerUrl().isEmpty() || AppConfig.getCurrentApiKey().isEmpty()) {
            restoreLastKnownConfig();
        }

        return success.get()
                && !AppConfig.getCurrentApiServerUrl().isEmpty()
                && !AppConfig.getCurrentApiKey().isEmpty();
    }

    public static void restoreLastKnownConfig() {
        Context context = MyApplication.getAppContext();
        if (context == null) {
            return;
        }
        if (!AppConfig.getCurrentApiServerUrl().isEmpty() && !AppConfig.getCurrentApiKey().isEmpty()) {
            // Keep native defaults unless values are missing.
            return;
        }
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String apiUrl = prefs.getString(KEY_API_URL, "");
            String token = prefs.getString(KEY_API_TOKEN, "");
            if (apiUrl != null && token != null && !apiUrl.trim().isEmpty() && !token.trim().isEmpty()) {
                AppConfig.updateRuntimeApiConfig(apiUrl.trim(), token.trim());
                Log.i(TAG, "Restored remote api/token config from local cache");
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to restore cached remote api/token config", e);
        }
    }

    private static void fetchAndApply() throws Exception {
        if (AppConfig.getCurrentApiServerUrl().isEmpty() || AppConfig.getCurrentApiKey().isEmpty()) {
            restoreLastKnownConfig();
        }

        if (AppConfig.getCurrentApiServerUrl().isEmpty() || AppConfig.getCurrentApiKey().isEmpty()) {
            return;
        }
        Log.i(TAG, "Ads remote worker config disabled; using native defaults/local cache");
    }
}
