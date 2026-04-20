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
}
