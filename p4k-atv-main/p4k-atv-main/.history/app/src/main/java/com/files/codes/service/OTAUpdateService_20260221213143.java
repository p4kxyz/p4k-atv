package com.files.codes.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.os.Handler;
import android.os.Looper;

import com.files.codes.utils.PreferenceUtils;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OTA Update Service
 * Checks for app updates using Firebase Realtime Database
 */
public class OTAUpdateService extends Service {
    private static final String TAG = "OTAUpdateService";
    private static final String FIREBASE_UPDATE_PATH = "app_updates/phim4k";
    private static final String FIREBASE_REST_URL =
            "https://website-19a7d-default-rtdb.asia-southeast1.firebasedatabase.app/app_updates/phim4k.json";
    
    private DatabaseReference updateRef;
    private OTAUpdateListener updateListener;
    private Context context;
    private boolean isChecking = false;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    
    public interface OTAUpdateListener {
        void onUpdateAvailable(UpdateInfo updateInfo);
        void onNoUpdateAvailable();
        void onUpdateCheckFailed(String error);
    }
    
    public static class UpdateInfo {
        public String version;
        public int versionCode;
        public String downloadUrl;
        public String changelog;
        public boolean forceUpdate;
        public long fileSize;
        public String checksum;
        
        public UpdateInfo() {}
        
        public UpdateInfo(String version, int versionCode, String downloadUrl, 
                         String changelog, boolean forceUpdate, long fileSize, String checksum) {
            this.version = version;
            this.versionCode = versionCode;
            this.downloadUrl = downloadUrl;
            this.changelog = changelog;
            this.forceUpdate = forceUpdate;
            this.fileSize = fileSize;
            this.checksum = checksum;
        }
        
        public String getFormattedFileSize() {
            if (fileSize < 1024) return fileSize + " B";
            if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }
    
    // Constructor for direct instantiation with context
    public OTAUpdateService(Context context) {
        this.context = context;
        Log.d(TAG, "OTA Update Service created with context");
        initializeFirebase();
    }
    
    // Default constructor for Android Service lifecycle
    public OTAUpdateService() {
        Log.d(TAG, "OTA Update Service default constructor");
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        this.context = this;
        Log.d(TAG, "OTA Update Service onCreate()");
        initializeFirebase();
    }
    
    /**
     * Initialize Firebase Database - can be called directly
     */
    public void initializeFirebase() {
        if (updateRef == null) {
            Log.e(TAG, "🔥 Initializing Firebase Database...");
            try {
                // Firebase URL: https://website-19a7d-default-rtdb.asia-southeast1.firebasedatabase.app/
                FirebaseDatabase database = FirebaseDatabase.getInstance("https://website-19a7d-default-rtdb.asia-southeast1.firebasedatabase.app");
                updateRef = database.getReference(FIREBASE_UPDATE_PATH);
                Log.e(TAG, "✅ Firebase initialized successfully!");
                Log.e(TAG, "Firebase URL: https://website-19a7d-default-rtdb.asia-southeast1.firebasedatabase.app");
                Log.e(TAG, "Firebase path: " + FIREBASE_UPDATE_PATH);
            } catch (Exception e) {
                Log.e(TAG, "❌ Firebase initialization failed: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "Firebase already initialized");
        }
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkForUpdates();
        return START_NOT_STICKY;
    }
    
    public void checkForUpdates() {
        if (isChecking) {
            Log.e(TAG, "⚠️ Already checking for updates, skipping duplicate call");
            return;
        }
        isChecking = true;

        Log.e(TAG, "🔄 Checking for updates via REST API: " + FIREBASE_REST_URL);

        // Clear any cached OTA data
        PreferenceUtils.saveString(context, "ota_pending_apk", "");
        PreferenceUtils.saveString(context, "ota_skipped_version", "");

        executor.execute(() -> {
            try {
                URL url = new URL(FIREBASE_REST_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                Log.e(TAG, "🌐 HTTP response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    String json = sb.toString();
                    Log.e(TAG, "📥 REST response (first 200 chars): " +
                            (json.length() > 200 ? json.substring(0, 200) : json));

                    JSONObject obj = new JSONObject(json);
                    UpdateInfo updateInfo = parseUpdateInfoFromJson(obj);

                    mainHandler.post(() -> {
                        isChecking = false;
                        if (isUpdateAvailable(updateInfo)) {
                            Log.e(TAG, "✅ Update available: " + updateInfo.version
                                    + " (code " + updateInfo.versionCode + ")");
                            if (updateListener != null)
                                updateListener.onUpdateAvailable(updateInfo);
                        } else {
                            Log.e(TAG, "❌ No update needed. Firebase=" + updateInfo.versionCode);
                            if (updateListener != null)
                                updateListener.onNoUpdateAvailable();
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        isChecking = false;
                        Log.e(TAG, "❌ HTTP error: " + responseCode);
                        if (updateListener != null)
                            updateListener.onUpdateCheckFailed("HTTP error: " + responseCode);
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "❌ REST request failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                mainHandler.post(() -> {
                    isChecking = false;
                    if (updateListener != null)
                        updateListener.onUpdateCheckFailed("Network error: " + e.getMessage());
                });
            }
        });
    }

    private UpdateInfo parseUpdateInfoFromJson(JSONObject obj) throws Exception {
        UpdateInfo info = new UpdateInfo();
        info.version = obj.optString("version_name", "");
        info.versionCode = obj.optInt("version_code", 0);
        info.downloadUrl = obj.optString("download_url", "");
        info.changelog = obj.optString("changelog", "");
        info.forceUpdate = obj.optBoolean("force_update", false);
        info.fileSize = obj.optLong("file_size", 0);
        info.checksum = obj.optString("checksum", "");
        Log.e(TAG, "📝 Parsed: version=" + info.version + " code=" + info.versionCode
                + " url=" + info.downloadUrl);
        return info;
    }
    
    private UpdateInfo parseUpdateInfo(DataSnapshot snapshot) {
        UpdateInfo updateInfo = new UpdateInfo();
        
        try {
            Log.d(TAG, "🔍 Parsing Firebase update data...");
            
            // Parse basic fields
            updateInfo.version = snapshot.child("version_name").getValue(String.class);
            Integer versionCodeValue = snapshot.child("version_code").getValue(Integer.class);
            updateInfo.versionCode = versionCodeValue != null ? versionCodeValue : 0;
            updateInfo.downloadUrl = snapshot.child("download_url").getValue(String.class);
            Boolean forceUpdateValue = snapshot.child("force_update").getValue(Boolean.class);
            updateInfo.forceUpdate = forceUpdateValue != null ? forceUpdateValue : false;
            Long fileSizeValue = snapshot.child("file_size").getValue(Long.class);
            updateInfo.fileSize = fileSizeValue != null ? fileSizeValue : 0L;
            updateInfo.checksum = snapshot.child("checksum").getValue(String.class);
            
            // Handle changelog - it can be String or HashMap
            DataSnapshot changelogSnapshot = snapshot.child("changelog");
            if (changelogSnapshot.exists()) {
                Object changelogObj = changelogSnapshot.getValue();
                if (changelogObj instanceof String) {
                    updateInfo.changelog = (String) changelogObj;
                } else if (changelogObj instanceof java.util.HashMap) {
                    // Try Vietnamese first, then English
                    java.util.HashMap<String, String> changelogMap = (java.util.HashMap<String, String>) changelogObj;
                    updateInfo.changelog = changelogMap.get("vi");
                    if (updateInfo.changelog == null || updateInfo.changelog.isEmpty()) {
                        updateInfo.changelog = changelogMap.get("en");
                    }
                } else {
                    updateInfo.changelog = "Cập nhật phiên bản mới với nhiều cải tiến và sửa lỗi.";
                }
            } else {
                updateInfo.changelog = "Cập nhật phiên bản mới với nhiều cải tiến và sửa lỗi.";
            }
            
            // Fallback for missing fields
            if (updateInfo.version == null || updateInfo.version.isEmpty()) {
                updateInfo.version = "Unknown";
            }
            if (updateInfo.downloadUrl == null || updateInfo.downloadUrl.isEmpty()) {
                throw new Exception("Download URL is missing");
            }
            
            Log.d(TAG, "✅ Parsed update info:");
            Log.d(TAG, "Version: " + updateInfo.version);
            Log.d(TAG, "Version Code: " + updateInfo.versionCode);
            Log.d(TAG, "Download URL: " + updateInfo.downloadUrl);
            Log.d(TAG, "Force Update: " + updateInfo.forceUpdate);
            Log.d(TAG, "File Size: " + updateInfo.getFormattedFileSize());
            Log.d(TAG, "Changelog length: " + (updateInfo.changelog != null ? updateInfo.changelog.length() : 0));
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error parsing update info: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error parsing update data: " + e.getMessage());
        }
        
        return updateInfo;
    }
    
    private boolean isUpdateAvailable(UpdateInfo updateInfo) {
        if (updateInfo.versionCode <= 0) {
            Log.w(TAG, "Invalid version code: " + updateInfo.versionCode);
            return false;
        }
        
        // Get current app version code
        int currentVersionCode = getCurrentVersionCode();
        Log.d(TAG, "Version comparison - Current: " + currentVersionCode + ", Available: " + updateInfo.versionCode);
        
        // Check if remote version is newer
        boolean hasUpdate = updateInfo.versionCode > currentVersionCode;
        Log.d(TAG, "Has newer version: " + hasUpdate);
        
        Log.d(TAG, "Final update decision: " + hasUpdate);
        return hasUpdate;
    }
    
    private int getCurrentVersionCode() {
        try {
            Context ctx = context != null ? context : this;
            int versionCode = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionCode;
            Log.d(TAG, "Current app version code: " + versionCode);
            return versionCode;
        } catch (Exception e) {
            Log.e(TAG, "Error getting current version code", e);
            return 0;
        }
    }
    
    public void setUpdateListener(OTAUpdateListener listener) {
        this.updateListener = listener;
    }

    /** Update context reference when Activity is recreated */
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "OTA Update Service destroyed");
    }
    
    // Static method to start update check
    public static void checkForUpdates(Context context, OTAUpdateListener listener) {
        Intent intent = new Intent(context, OTAUpdateService.class);
        OTAUpdateService service = new OTAUpdateService();
        service.setUpdateListener(listener);
        context.startService(intent);
    }
}