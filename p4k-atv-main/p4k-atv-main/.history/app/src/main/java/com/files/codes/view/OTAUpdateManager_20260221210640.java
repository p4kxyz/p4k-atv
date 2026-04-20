package com.files.codes.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.files.codes.R;
import com.files.codes.service.OTADownloadManager;
import com.files.codes.service.OTAUpdateService;
import com.files.codes.utils.PreferenceUtils;
import com.files.codes.utils.ToastMsg;

/**
 * OTA Update Manager
 * Handles the complete OTA update flow with UI
 */
public class OTAUpdateManager implements OTAUpdateService.OTAUpdateListener, OTADownloadManager.OTADownloadListener {
    private static final String TAG = "OTAUpdateManager";
    private static final int INSTALL_PERMISSION_REQUEST = 1001;
    
    private Context context;
    private OTAUpdateService updateService;
    private OTADownloadManager downloadManager;
    private AlertDialog updateDialog;
    private AlertDialog downloadDialog;
    private ProgressBar downloadProgress;
    private TextView downloadStatus;
    private boolean silentCheck = false; // true = startup background check, no toast for "no update"
    
    private static OTAUpdateManager instance;
    
    private OTAUpdateManager() {
        // Private constructor for singleton
    }
    
    public static OTAUpdateManager getInstance(Context context) {
        if (instance == null) {
            instance = new OTAUpdateManager();
        }
        instance.init(context);
        return instance;
    }
    
    private void init(Context context) {
        // Always refresh context so dialogs use the current Activity window
        this.context = context;
        if (this.updateService == null) {
            Log.e(TAG, "🔧 Creating OTAUpdateService...");
            this.updateService = new OTAUpdateService(context);
            this.updateService.setUpdateListener(this);
            Log.e(TAG, "✅ OTAUpdateService created and Firebase initialized");
        } else {
            // Pass fresh context to updateService too
            this.updateService.setContext(context);
        }
        if (this.downloadManager == null) {
            Log.e(TAG, "🔧 Creating OTADownloadManager...");
            this.downloadManager = new OTADownloadManager(context);
            this.downloadManager.setDownloadListener(this);
            Log.e(TAG, "✅ OTADownloadManager created");
        }
    }
    
    /**
     * Start checking for updates (shows toast if no update)
     */
    public void checkForUpdates() {
        Log.e(TAG, "Starting OTA update check (manual)...");
        silentCheck = false;
        updateService.checkForUpdates();
    }
    
    /**
     * Check for updates silently - no toast if no update (for startup auto-check)
     */
    public void checkForUpdatesSilently() {
        Log.e(TAG, "Starting OTA update check (silent)...");
        silentCheck = true;
        updateService.checkForUpdates();
    }
    
    // OTAUpdateService.OTAUpdateListener implementation
    @Override
    public void onUpdateAvailable(OTAUpdateService.UpdateInfo updateInfo) {
        Log.e(TAG, "Update available: " + updateInfo.version);
        showUpdateDialog(updateInfo);
    }
    
    @Override
    public void onNoUpdateAvailable() {
        Log.e(TAG, "No update available");
        if (!silentCheck) {
            new ToastMsg(context).toastIconSuccess("Bạn đang sử dụng phiên bản mới nhất!");
        }
    }
    
    @Override
    public void onUpdateCheckFailed(String error) {
        Log.e(TAG, "Update check failed: " + error);
        if (!silentCheck) {
            new ToastMsg(context).toastIconError("Không thể kiểm tra cập nhật: " + error);
        }
    }
    
    // OTADownloadManager.OTADownloadListener implementation
    @Override
    public void onDownloadStarted() {
        Log.d(TAG, "Download started");
        if (downloadProgress != null) {
            downloadProgress.setIndeterminate(true);
        }
        if (downloadStatus != null) {
            downloadStatus.setText("Đang tải xuống...");
        }
    }
    
    @Override
    public void onDownloadProgress(int progress) {
        Log.d(TAG, "Download progress: " + progress + "%");
        if (downloadProgress != null) {
            downloadProgress.setIndeterminate(false);
            downloadProgress.setProgress(progress);
        }
        if (downloadStatus != null) {
            downloadStatus.setText("Đang tải xuống... " + progress + "%");
        }
    }
    
    @Override
    public void onDownloadCompleted(String apkPath) {
        Log.d(TAG, "Download completed: " + apkPath);
        if (downloadDialog != null) {
            downloadDialog.dismiss();
        }
        
        showInstallDialog(apkPath);
    }
    
    @Override
    public void onDownloadFailed(String error) {
        Log.e(TAG, "Download failed: " + error);
        if (downloadDialog != null) {
            downloadDialog.dismiss();
        }
        new ToastMsg(context).toastIconError("Tải xuống thất bại: " + error);
    }
    
    /**
     * Show update available dialog
     */
    private void showUpdateDialog(OTAUpdateService.UpdateInfo updateInfo) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_ota_update, null);

            TextView titleText = dialogView.findViewById(R.id.update_title);
            TextView versionText = dialogView.findViewById(R.id.update_version);
            TextView sizeText = dialogView.findViewById(R.id.update_size);
            TextView changelogText = dialogView.findViewById(R.id.update_changelog);
            Button updateButton = dialogView.findViewById(R.id.btn_update);
            Button laterButton = dialogView.findViewById(R.id.btn_later);

            titleText.setText(updateInfo.forceUpdate ? "Cập nhật bắt buộc" : "Cập nhật có sẵn");
            versionText.setText("Phiên bản: " + updateInfo.version);
            sizeText.setText("Kích thước: " + updateInfo.getFormattedFileSize());
            changelogText.setText(updateInfo.changelog != null ? updateInfo.changelog : "Cải thiện hiệu suất và sửa lỗi");

            if (updateInfo.forceUpdate) {
                laterButton.setVisibility(View.GONE);
            }

            updateButton.setOnClickListener(v -> {
                updateDialog.dismiss();
                startDownload(updateInfo);
            });

            laterButton.setOnClickListener(v -> updateDialog.dismiss());

            builder.setView(dialogView);
            builder.setCancelable(!updateInfo.forceUpdate);

            updateDialog = builder.create();

            if (updateDialog.getWindow() != null) {
                updateDialog.getWindow().clearFlags(
                    android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            }

            updateDialog.show();
            updateButton.requestFocus();

        } catch (android.view.WindowManager.BadTokenException e) {
            // Context (Activity) was destroyed before Firebase responded — store flag and show on next launch
            Log.w(TAG, "⚠️ BadTokenException: Activity destroyed before dialog could show. Will retry next launch.");
            // Store updateInfo so it can be shown on next launch
            PreferenceUtils.saveString(context, "ota_pending_version", updateInfo.version);
            PreferenceUtils.saveString(context, "ota_pending_download_url", updateInfo.downloadUrl);
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to show update dialog: " + e.getMessage());
        }
    }
    
    /**
     * Start download process
     */
    private void startDownload(OTAUpdateService.UpdateInfo updateInfo) {
        // Check write permission
        if (!hasWritePermission()) {
            requestWritePermission();
            return;
        }
        
        showDownloadDialog();
        downloadManager.downloadUpdate(updateInfo.downloadUrl, updateInfo.version);
    }
    
    /**
     * Show download progress dialog
     */
    private void showDownloadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_ota_download, null);
        
        downloadProgress = dialogView.findViewById(R.id.download_progress);
        downloadStatus = dialogView.findViewById(R.id.download_status);
        Button cancelButton = dialogView.findViewById(R.id.btn_cancel_download);
        
        cancelButton.setOnClickListener(v -> {
            downloadManager.cancelDownload();
            downloadDialog.dismiss();
        });
        
        builder.setView(dialogView);
        builder.setCancelable(false);

        downloadDialog = builder.create();

        // Ensure dialog is focusable on TV
        if (downloadDialog.getWindow() != null) {
            downloadDialog.getWindow().clearFlags(
                android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }

        downloadDialog.show();

        // Focus on cancel button
        cancelButton.requestFocus();
    }
    
    /**
     * Show install confirmation dialog
     */
    private void showInstallDialog(String apkPath) {
        Log.d(TAG, "Showing install dialog for APK: " + apkPath);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Cài đặt cập nhật")
               .setMessage("Tải xuống hoàn tất! Bạn có muốn cài đặt cập nhật ngay bây giờ?")
               .setPositiveButton("Cài đặt", (dialog, which) -> {
                   Log.d(TAG, "👆 User clicked Install button");
                   installUpdate(apkPath);
               })
               .setNegativeButton("Để sau", (dialog, which) -> {
                   // Save APK path for later installation
                   Log.d(TAG, "👆 User clicked Later button");
                   PreferenceUtils.saveString(context, "ota_pending_apk", apkPath);
                   new ToastMsg(context).toastIconSuccess("Cập nhật đã sẵn sàng cài đặt");
               })
               .setCancelable(false);
        
        try {
            AlertDialog dialog = builder.show();
            Log.d(TAG, "✅ Install dialog shown successfully");
            
            // Auto-click install after 3 seconds for Android TV
            new android.os.Handler().postDelayed(() -> {
                if (dialog.isShowing()) {
                    Log.d(TAG, "🤖 Auto-installing after 3 seconds for Android TV");
                    dialog.dismiss();
                    installUpdate(apkPath);
                }
            }, 3000);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to show install dialog: " + e.getMessage());
            // Fallback: direct install
            Log.d(TAG, "🔄 Fallback to direct install");
            installUpdate(apkPath);
        }
    }
    
    /**
     * Install the downloaded APK
     */
    private void installUpdate(String apkPath) {
        // Check install permission for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.getPackageManager().canRequestPackageInstalls()) {
                requestInstallPermission();
                PreferenceUtils.saveString(context, "ota_pending_apk", apkPath);
                return;
            }
        }
        
        downloadManager.installApk(apkPath);
    }
    
    /**
     * Check if app has write external storage permission
     */
    private boolean hasWritePermission() {
        return ContextCompat.checkSelfPermission(context, 
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Request write external storage permission
     */
    private void requestWritePermission() {
        new AlertDialog.Builder(context)
            .setTitle("Quyền cần thiết")
            .setMessage("Ứng dụng cần quyền truy cập bộ nhớ để tải xuống cập nhật.")
            .setPositiveButton("Cấp quyền", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
            })
            .setNegativeButton("Hủy", null)
            .show();
    }
    
    /**
     * Request install unknown apps permission (Android 8.0+)
     */
    private void requestInstallPermission() {
        new AlertDialog.Builder(context)
            .setTitle("Quyền cần thiết")
            .setMessage("Để cài đặt cập nhật, ứng dụng cần quyền cài đặt từ nguồn không xác định.")
            .setPositiveButton("Cấp quyền", (dialog, which) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                    context.startActivity(intent);
                }
            })
            .setNegativeButton("Hủy", null)
            .show();
    }
    
    /**
     * Check and install pending update
     */
    public void checkPendingUpdate() {
        String pendingApk = PreferenceUtils.getString(context, "ota_pending_apk", "");
        if (!pendingApk.isEmpty()) {
            showInstallDialog(pendingApk);
            PreferenceUtils.saveString(context, "ota_pending_apk", "");
        }
    }
    
    /**
     * MEMORY LEAK FIX: Cleanup resources
     */
    public void cleanup() {
        try {
            if (downloadManager != null) {
                downloadManager.cleanup();
                downloadManager = null;
                Log.d(TAG, "🧹 Download manager cleaned up");
            }
            
            if (updateDialog != null && updateDialog.isShowing()) {
                updateDialog.dismiss();
                updateDialog = null;
                Log.d(TAG, "🧹 Update dialog dismissed");
            }
            
            if (downloadDialog != null && downloadDialog.isShowing()) {
                downloadDialog.dismiss();
                downloadDialog = null;
                Log.d(TAG, "🧹 Download dialog dismissed");
            }
            
            Log.d(TAG, "✅ OTA Update Manager cleanup completed");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error during cleanup: " + e.getMessage());
        }
    }
}