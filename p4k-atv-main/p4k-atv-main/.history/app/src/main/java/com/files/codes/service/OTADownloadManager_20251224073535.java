package com.files.codes.service;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import androidx.core.content.FileProvider;
import com.files.codes.utils.PreferenceUtils;
import java.io.File;

/**
 * OTA Update Download Manager
 * Handles APK download and installation
 */
public class OTADownloadManager {
    private static final String TAG = "OTADownloadManager";
    
    private Context context;
    private DownloadManager downloadManager;
    private long downloadId = -1;
    private OTADownloadListener listener;
    private BroadcastReceiver downloadReceiver;
    
    public interface OTADownloadListener {
        void onDownloadStarted();
        void onDownloadProgress(int progress);
        void onDownloadCompleted(String apkPath);
        void onDownloadFailed(String error);
    }
    
    public OTADownloadManager(Context context) {
        this.context = context;
        this.downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        registerDownloadReceiver();
    }
    
    public void downloadUpdate(String downloadUrl, String version) {
        try {
            Log.e(TAG, "🔽 Starting download...");
            Log.e(TAG, "Download URL: " + downloadUrl);
            Log.e(TAG, "Version: " + version);
            
            // Validate URL
            if (downloadUrl == null || downloadUrl.isEmpty()) {
                Log.e(TAG, "❌ Invalid download URL");
                if (listener != null) {
                    listener.onDownloadFailed("Invalid download URL");
                }
                return;
            }
            
            // Use the real Firebase download URL
            // Using Firebase download URL
            
            // Delete old APK if exists
            deleteOldApk();
            
            String fileName = "Phim4K_" + version + ".apk";
            Log.e(TAG, "APK filename: " + fileName);
            
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
            request.setTitle("Phim4K Update");
            request.setDescription("Downloading version " + version);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            // request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            
            downloadId = downloadManager.enqueue(request);
            Log.e(TAG, "Download ID: " + downloadId);
            
            // Save download info
            PreferenceUtils.saveString(context, "ota_download_id", String.valueOf(downloadId));
            PreferenceUtils.saveString(context, "ota_apk_filename", fileName);
            
            if (listener != null) {
                listener.onDownloadStarted();
            }
            
            Log.e(TAG, "✅ Started downloading update: " + version);
            
            // Start a fallback checker in case receiver fails
            startDownloadStatusChecker();
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting download", e);
            if (listener != null) {
                listener.onDownloadFailed("Failed to start download: " + e.getMessage());
            }
        }
    }
    
    private void registerDownloadReceiver() {
        Log.e(TAG, "📡 Registering download receiver...");
        
        // Unregister old receiver if exists
        if (downloadReceiver != null) {
            try {
                context.unregisterReceiver(downloadReceiver);
                Log.e(TAG, "📡 Old receiver unregistered");
            } catch (Exception e) {
                Log.w(TAG, "No old receiver to unregister");
            }
        }
        
        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.e(TAG, "📡 Broadcast received!");
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                Log.e(TAG, "📡 Download ID from broadcast: " + id + " (expected: " + downloadId + ")");
                
                if (id == downloadId) {
                    Log.e(TAG, "📡 ✅ Download ID matches! Handling completion...");
                    handleDownloadComplete();
                } else {
                    Log.e(TAG, "📡 ❌ Download ID mismatch, ignoring...");
                }
            }
        };
        
        context.registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        Log.e(TAG, "📡 ✅ Download receiver registered successfully!");
    }
    
    private void handleDownloadComplete() {
        Log.e(TAG, "📥 Download completed callback received");
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        
        android.database.Cursor cursor = downloadManager.query(query);
        if (cursor.moveToFirst()) {
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            Log.e(TAG, "Download status: " + status);
            
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                Log.e(TAG, "✅ Download successful!");
                String apkPath = getDownloadedApkPath();
                Log.e(TAG, "APK path: " + apkPath);
                if (listener != null) {
                    listener.onDownloadCompleted(apkPath);
                }
            } else if (status == DownloadManager.STATUS_FAILED) {
                Log.e(TAG, "❌ Download failed!");
                int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
                Log.e(TAG, "Failure reason: " + reason);
                if (listener != null) {
                    listener.onDownloadFailed("Download failed with reason: " + reason);
                }
            } else {
                Log.d(TAG, "Download status unknown: " + status);
                if (listener != null) {
                    listener.onDownloadFailed("Download failed with unknown status: " + status);
                }
            }
        }
        cursor.close();
    }
    
    private String getDownloadedApkPath() {
        String fileName = PreferenceUtils.getString(context, "ota_apk_filename", "");
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
        return file.getAbsolutePath();
    }
    
    private void deleteOldApk() {
        try {
            String oldFileName = PreferenceUtils.getString(context, "ota_apk_filename", "");
            if (!oldFileName.isEmpty()) {
                File oldFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), oldFileName);
                if (oldFile.exists()) {
                    oldFile.delete();
                    Log.d(TAG, "Deleted old APK: " + oldFileName);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error deleting old APK", e);
        }
    }
    
    public void installApk(String apkPath) {
        try {
            Log.e(TAG, "Installing APK: " + apkPath);
            
            File apkFile = new File(apkPath);
            if (!apkFile.exists()) {
                Log.e(TAG, "❌ APK file not found: " + apkPath);
                if (listener != null) {
                    listener.onDownloadFailed("APK file not found");
                }
                return;
            }
            
            // Validate APK file
            long fileSize = apkFile.length();
            Log.e(TAG, "APK file size: " + fileSize + " bytes");
            
            // Check if file is too small (likely corrupt or incomplete)
            if (fileSize < 1024 * 1024) { // Less than 1MB
                Log.e(TAG, "❌ APK file too small, likely corrupt: " + fileSize);
                if (listener != null) {
                    listener.onDownloadFailed("Downloaded file is corrupt or incomplete");
                }
                return;
            }
            
            // Check if file is readable
            if (!apkFile.canRead()) {
                Log.e(TAG, "❌ APK file not readable");
                if (listener != null) {
                    listener.onDownloadFailed("APK file is not readable");
                }
                return;
            }
            
            Log.e(TAG, "✅ APK validation passed, proceeding with installation");
            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            
            // For Android 7.0 and above, use FileProvider
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                Uri apkUri = FileProvider.getUriForFile(context, 
                    context.getPackageName() + ".fileprovider", apkFile);
                
                Log.e(TAG, "Generated FileProvider URI: " + apkUri.toString());
                
                installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                installIntent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            }
            
            installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            Log.e(TAG, "🚀 Starting install activity...");
            context.startActivity(installIntent);
            
            Log.e(TAG, "✅ APK installation intent started successfully!");
            Log.e(TAG, "📱 ===== APK INSTALLATION COMPLETED =====");

            // Do NOT kill the process immediately. This can cause "Parse Error" because
            // the URI permission grant might be revoked when the app dies.
            // Let the Android Installer handle the process.
            /*
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "👋 Exiting app to ensure smooth installation...");
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                }
            }, 1000);
            */
            
        } catch (Exception e) {
            Log.e(TAG, "Error installing APK", e);
            if (listener != null) {
                listener.onDownloadFailed("Failed to install APK: " + e.getMessage());
            }
        }
    }
    
    /**
     * Fallback checker in case receiver fails
     */
    private void startDownloadStatusChecker() {
        Log.d(TAG, "🕐 Starting fallback download status checker...");
        
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (downloadId != -1) {
                    Log.d(TAG, "🕐 Checking download status manually...");
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(downloadId);
                    
                    android.database.Cursor cursor = downloadManager.query(query);
                    if (cursor.moveToFirst()) {
                        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        Log.d(TAG, "🕐 Manual check - Download status: " + status);
                        
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            Log.d(TAG, "🕐 ✅ Manual check detected successful download!");
                            handleDownloadComplete();
                        } else if (status == DownloadManager.STATUS_RUNNING) {
                            Log.d(TAG, "🕐 Still downloading, will check again...");
                            // Check again in 5 seconds
                            startDownloadStatusChecker();
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            Log.e(TAG, "🕐 ❌ Manual check detected failed download!");
                            handleDownloadComplete();
                        }
                    }
                    cursor.close();
                }
            }
        }, 5000); // Check every 5 seconds
    }
    
    /**
     * Cleanup receiver
     */
    public void cleanup() {
        if (downloadReceiver != null) {
            try {
                context.unregisterReceiver(downloadReceiver);
                Log.d(TAG, "📡 Download receiver cleaned up");
            } catch (Exception e) {
                Log.w(TAG, "Error cleaning up receiver: " + e.getMessage());
            }
            downloadReceiver = null;
        }
    }
    
    public void setDownloadListener(OTADownloadListener listener) {
        this.listener = listener;
    }
    
    public void cancelDownload() {
        if (downloadId != -1) {
            downloadManager.remove(downloadId);
            downloadId = -1;
        }
    }
}