package com.files.codes.utils;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class VideoTokenGenerator {
    private static final String TAG = "VideoTokenGenerator";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    // Keep empty to force dynamic signing with HMAC_SECRET_2.
    private static final String HMAC_SECRET = "";
    private static final byte HMAC_XOR_KEY = 0x5A;
    // XOR-obfuscated secret to avoid plain-text key in Java source.
    private static final byte[] HMAC_SECRET_2_XOR = new byte[]{
            111, 63, 98, 62, 107, 56, 110, 60, 99, 57, 104, 59, 108, 63, 109, 105,
            106, 56, 107, 60, 98, 62, 110, 59, 99, 104, 57, 111, 63, 105, 62, 107
    };
    private static final String PLAY_BASE_URL = "https://dmp30.phim4k.lol";

    static {
        System.loadLibrary("api_config");
    }

    private VideoTokenGenerator() {
    }

    private static native String nativeGenVideoUrl(String filename);

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
        String safeFilename = filename.trim();
        try {
            String signingSecret = signingSecret(HMAC_SECRET, null);
            if (signingSecret == null || signingSecret.isEmpty()) {
                // If key config is unavailable, fallback to native implementation.
                return nativeGenVideoUrl(safeFilename);
            }

            long nowEpochSec = System.currentTimeMillis() / 1000L;
            byte[] maskTs = otpMask(signingSecret, "otp-ts-mask");
            String encTs = xorIntToHex8(nowEpochSec, maskTs);

            String message = safeFilename + ":" + nowEpochSec;
            String tokenHex = toHex(hmacSha256(signingSecret, message));
            return PLAY_BASE_URL + "/" + safeFilename + "?token=" + tokenHex + "&ts=" + encTs;
        } catch (Exception e) {
            Log.w(TAG, "Dynamic signing failed, fallback to native", e);
            return nativeGenVideoUrl(safeFilename);
        }
    }

    private static String formatDateUtc(int offsetDays) {
        return LocalDate.now(ZoneOffset.UTC).plusDays(offsetDays).format(DATE_FMT);
    }

    private static String decodeXorString(byte[] data, byte key) {
        char[] out = new char[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = (char) ((data[i] & 0xFF) ^ key);
        }
        return new String(out);
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

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
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

    private static String signingSecret(String hmacSecret, String hmacSecret2) throws Exception {
        if (hmacSecret != null && !hmacSecret.isEmpty()) {
            return hmacSecret;
        }
        String effectiveHmacSecret2 = hmacSecret2;
        if (effectiveHmacSecret2 == null || effectiveHmacSecret2.isEmpty()) {
            effectiveHmacSecret2 = decodeXorString(HMAC_SECRET_2_XOR, HMAC_XOR_KEY);
        }
        if (effectiveHmacSecret2 != null && !effectiveHmacSecret2.isEmpty()) {
            return deriveDailySecretV2(effectiveHmacSecret2, formatDateUtc(0));
        }
        return "";
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
                Log.e("VideoTokenGenerator", "Error resolving secure stream URL", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e));
            }
        }).start();
    }
}
