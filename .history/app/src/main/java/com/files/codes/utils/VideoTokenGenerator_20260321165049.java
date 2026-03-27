package com.files.codes.utils;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class VideoTokenGenerator {
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
        return nativeGenVideoUrl(filename.trim());
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
