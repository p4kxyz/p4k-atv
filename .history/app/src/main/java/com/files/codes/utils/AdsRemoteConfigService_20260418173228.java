package com.files.codes.utils;

import android.util.Base64;
import android.util.Log;

import com.files.codes.AppConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class AdsRemoteConfigService {
    private static final String TAG = "AdsRemoteConfigService";
    private static final String ADS_URL = "https://ads.phim4k.workers.dev/api/ads";
    private static final String ADS_SECRET = "9f3c2a7e6b1d4c8fa2e97d5b0c3a41f8";

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

    private static void fetchAndApply() throws Exception {
        String body = httpGet(ADS_URL);
        JSONObject encrypted = new JSONObject(body);
        String plaintext = decryptPayload(encrypted, ADS_SECRET);
        JSONObject plainJson = new JSONObject(plaintext);

        JSONObject data = plainJson.optJSONObject("data");
        if (data == null) {
            return;
        }

        String apiUrl = normalizeApiUrl(data.optString("api", ""));
        String token = extractToken(data.opt("tokens"));
        if (!apiUrl.isEmpty() || !token.isEmpty()) {
            AppConfig.updateRuntimeApiConfig(apiUrl, token);
            Log.i(TAG, "Applied remote api/token config from workers");
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
