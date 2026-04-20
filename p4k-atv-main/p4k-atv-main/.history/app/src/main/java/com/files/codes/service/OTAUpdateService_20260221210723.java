package com.files.codes.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.files.codes.utils.PreferenceUtils;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

/**
 * OTA Update Service
 * Checks for app updates using Firebase Realtime Database
 */
public class OTAUpdateService extends Service {
    private static final String TAG = "OTAUpdateService";
    private static final String FIREBASE_UPDATE_PATH = "app_updates/phim4k";
    
    private DatabaseReference updateRef;
    private OTAUpdateListener updateListener;
    private Context context;
    
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
        if (updateRef == null) {
            Log.e(TAG, "Firebase reference is null");
            if (updateListener != null) {
                updateListener.onUpdateCheckFailed("Firebase not initialized");
            }
            return;
        }
        
        Log.e(TAG, "🔄 Checking for updates with fresh data...");
        
        // Clear any cached OTA data
        PreferenceUtils.saveString(context, "ota_pending_apk", "");
        PreferenceUtils.saveString(context, "last_check_time", "");
        
        // Force fresh data from server (bypass cache)
        updateRef.keepSynced(false);
        updateRef.keepSynced(true);
        
        updateRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    Log.e(TAG, "🔥 Firebase snapshot received. Exists: " + snapshot.exists());
                    Log.e(TAG, "📊 Firebase Path: " + FIREBASE_UPDATE_PATH);
                    Log.e(TAG, "🗄️ Snapshot Key: " + snapshot.getKey());
                    Log.e(TAG, "👥 Children Count: " + snapshot.getChildrenCount());
                    
                    if (snapshot.exists()) {
                        UpdateInfo updateInfo = parseUpdateInfo(snapshot);
                        
                        if (isUpdateAvailable(updateInfo)) {
                            Log.e(TAG, "✅ Update available: " + updateInfo.version);
                            if (updateListener != null) {
                                updateListener.onUpdateAvailable(updateInfo);
                            }
                        } else {
                            Log.e(TAG, "❌ No update needed. Current >= Available");
                            if (updateListener != null) {
                                updateListener.onNoUpdateAvailable();
                            }
                        }
                    } else {
                        Log.e(TAG, "❌ No update data found in Firebase at path: " + FIREBASE_UPDATE_PATH);
                        Log.e(TAG, "🔍 DEBUG: Check if data exists at Firebase Console:");
                        Log.e(TAG, "🔗 URL: https://website-19a7d-default-rtdb.asia-southeast1.firebasedatabase.app/");
                        Log.e(TAG, "📍 Path: " + FIREBASE_UPDATE_PATH);
                        if (updateListener != null) {
                            updateListener.onNoUpdateAvailable();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing update data", e);
                    if (updateListener != null) {
                        updateListener.onUpdateCheckFailed("Error parsing update data: " + e.getMessage());
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase update check cancelled", error.toException());
                if (updateListener != null) {
                    updateListener.onUpdateCheckFailed("Update check failed: " + error.getMessage());
                }
            }
        });
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