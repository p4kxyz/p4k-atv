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
        String body = httpGet(ADS_URL);
        JSONObject encrypted = new JSONObject(body);
        String adsSecret = AppConfig.getAdsWorkerSecret();
        if (adsSecret == null || adsSecret.trim().isEmpty()) {
            throw new IllegalStateException("Ads worker secret is empty from native config");
        }
        String plaintext = decryptPayload(encrypted, adsSecret.trim());
        JSONObject plainJson = new JSONObject(plaintext);

        JSONObject data = plainJson.optJSONObject("data");
        if (data == null) {
            return;
        }

        String apiUrl = normalizeApiUrl(data.optString("api", ""));
        String token = extractToken(data.opt("tokens"));
        if (!apiUrl.isEmpty() && !token.isEmpty()) {
            AppConfig.updateRuntimeApiConfig(apiUrl, token);
            persistLastKnownConfig(apiUrl, token);
            Log.i(TAG, "Applied remote api/token config from workers");
        }
    }

    private static void persistLastKnownConfig(String apiUrl, String token) {
        Context context = MyApplication.getAppContext();
        if (context == null) {
            return;
        }
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_API_URL, apiUrl)
                    .putString(KEY_API_TOKEN, token)
                    .apply();
        } catch (Exception e) {
            Log.w(TAG, "Failed to persist remote api/token config", e);
        }
    }

    private static String normalizeApiUrl(String apiUrl) {
        if (apiUrl == null) {
            return "";
        }
        String out = apiUrl.trim();
        if (out.isEmpty()) {
            return "";
        }
        if (!out.endsWith("/")) {
            out += "/";
        }
        return out;
    }

    private static String extractToken(Object tokensNode) {
        if (tokensNode == null) {
            return "";
        }
        if (tokensNode instanceof String) {
            return ((String) tokensNode).trim();
        }
        if (tokensNode instanceof JSONObject) {
            JSONObject obj = (JSONObject) tokensNode;
            String[] keys = new String[]{"api_key", "apiKey", "token", "key"};
            for (String k : keys) {
                String value = obj.optString(k, "").trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
            return "";
        }
        if (tokensNode instanceof JSONArray) {
            JSONArray arr = (JSONArray) tokensNode;
            for (int i = 0; i < arr.length(); i++) {
                String value = arr.optString(i, "").trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return "";
    }

    private static String httpGet(String endpoint) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        int code = conn.getResponseCode();
        BufferedReader reader;
        if (code >= 200 && code < 300) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        }

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();

        if (code < 200 || code >= 300) {
            throw new IllegalStateException("HTTP " + code + " from ads endpoint: " + sb);
        }

        return sb.toString();
    }

    private static String decryptPayload(JSONObject encrypted, String secret) throws Exception {
        String alg = encrypted.optString("alg", "");
        if (!"AES-GCM-256".equalsIgnoreCase(alg)) {
            throw new IllegalArgumentException("Unsupported alg: " + alg);
        }

        byte[] iv = base64UrlDecode(encrypted.getString("iv"));
        byte[] ciphertextAndTag = base64UrlDecode(encrypted.getString("ciphertext"));

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] key = digest.digest(secret.getBytes(StandardCharsets.UTF_8));

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcm = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcm);

        byte[] clear = cipher.doFinal(ciphertextAndTag);
        return new String(clear, StandardCharsets.UTF_8);
    }

    private static byte[] base64UrlDecode(String input) {
        String normalized = input.replace('-', '+').replace('_', '/');
        int pad = (4 - (normalized.length() % 4)) % 4;
        StringBuilder sb = new StringBuilder(normalized);
        for (int i = 0; i < pad; i++) {
            sb.append('=');
        }
        return Base64.decode(sb.toString(), Base64.DEFAULT);
    }
}
