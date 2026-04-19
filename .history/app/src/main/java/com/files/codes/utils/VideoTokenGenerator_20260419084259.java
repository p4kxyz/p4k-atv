package com.files.codes.utils;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class VideoTokenGenerator {
    static {
        System.loadLibrary("api_config");
    }

    private static final String TAG = "VideoTokenGenerator";
    private static final String VIDEO_BASE_URL = "https://dmp30.phim4k.lol";

    // Legacy secret (old worker secret). Keep empty if you want to force dynamic daily key.
    private static final String HMAC_SECRET = "";

    // Dynamic key seed is sourced from native C++ (XOR-obfuscated).
    private static volatile String cachedHmacSecret2;

    private static final int TOKEN_TTL_SECONDS = 300;

    private VideoTokenGenerator() {
    }

    // Keep declaration to match native RegisterNatives binding.
    private static native String nativeGenVideoUrl(String filename);

    private static native String nativeGetHmacSecret2();

    public static String extractFilenameFromCdnUrl(String cdnUrl) {
        if (cdnUrl == null) return "";
        String cleaned = cdnUrl.trim();
        Uri uri = Uri.parse(cleaned);
        String segment = uri.getLastPathSegment();
        return segment != null ? segment.trim() : "";
    }

    public static String genVideoUrl(String filename) throws Exception {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("filename is empty");
        }

        String secret = signingSecret(HMAC_SECRET, getHmacSecret2());
        if (secret.isEmpty()) {
            throw new IllegalStateException("hmac signing secret is empty");
        }

        long ts = System.currentTimeMillis() / 1000L;
        byte[] mask = otpMask(secret, "otp-ts-mask");
        String encTs = xorIntToHex8(ts, mask);
        String message = filename.trim() + ":" + ts;
        String tokenHex = toHex(hmacSha256(secret, message));

        return VIDEO_BASE_URL + "/" + filename.trim() + "?token=" + tokenHex + "&ts=" + encTs;
    }

    private static String getHmacSecret2() {
        String value = cachedHmacSecret2;
        if (value != null && !value.isEmpty()) {
            return value;
        }
        try {
            String fromNative = nativeGetHmacSecret2();
            if (fromNative != null) {
                fromNative = fromNative.trim();
            }
            cachedHmacSecret2 = fromNative == null ? "" : fromNative;
            return cachedHmacSecret2;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to load HMAC_SECRET_2 from native", t);
            cachedHmacSecret2 = "";
            return "";
        }
    }

    public interface StreamUrlCallback {
        void onSuccess(String streamUrl);
        void onError(Exception e);
    }

    public static void resolveStreamUrlAsync(String cdnUrl, StreamUrlCallback callback) {
        if (cdnUrl == null || !cdnUrl.contains("cdn.phim4k.lol")) {
            new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(cdnUrl));
            return;
        }

        new Thread(() -> {
            try {
                String filename = extractFilenameFromCdnUrl(cdnUrl);
                String signedUrl = genVideoUrl(filename);
                if (signedUrl == null || signedUrl.isEmpty()) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(cdnUrl));
                    return;
                }

                URL url = new URL(signedUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                String streamUrl = json.getString("url");

                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(streamUrl));
            } catch (Exception e) {
                Log.e(TAG, "Error resolving secure stream URL", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e));
            }
        }).start();
    }

    private static String signingSecret(String hmacSecret, String hmacSecret2) throws Exception {
        if (hmacSecret != null && !hmacSecret.isEmpty()) {
            return hmacSecret;
        }
        if (hmacSecret2 != null && !hmacSecret2.isEmpty()) {
            return deriveDailySecretV2(hmacSecret2, formatDateUtc(0));
        }
        return "";
    }

    private static String formatDateUtc(int offsetDays) {
        long now = System.currentTimeMillis();
        long shifted = now + (offsetDays * 24L * 60L * 60L * 1000L);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(shifted));
    }

    private static String deriveDailySecretV2(String baseSecret, String dateStr) throws Exception {
        byte[] keyRaw = sha256(baseSecret.getBytes(StandardCharsets.UTF_8));
        byte[] ivSeed = sha256(("iv:" + baseSecret).getBytes(StandardCharsets.UTF_8));
        byte[] iv = new byte[12];
        System.arraycopy(ivSeed, 0, iv, 0, 12);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(keyRaw, "AES");
        GCMParameterSpec gcm = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcm);

        byte[] plaintext = (baseSecret + ":" + dateStr).getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = cipher.doFinal(plaintext);
        return baseSecret + ":" + toHex(encrypted);
    }

    private static byte[] sha256(byte[] input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(input);
    }

    private static byte[] hmacSha256(String secret, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] otpMask(String secret, String purpose) throws Exception {
        byte[] h = hmacSha256(secret, purpose);
        byte[] out = new byte[4];
        System.arraycopy(h, 0, out, 0, 4);
        return out;
    }

    private static String xorIntToHex8(long value, byte[] mask4) {
        byte[] v = ByteBuffer.allocate(4).putInt((int) value).array();
        for (int i = 0; i < 4; i++) {
            v[i] = (byte) (v[i] ^ mask4[i]);
        }
        return toHex(v);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format(Locale.US, "%02x", b));
        }
        return sb.toString();
    }
}
