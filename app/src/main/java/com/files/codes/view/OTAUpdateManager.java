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
    
    private static final long OTA_CHECK_COOLDOWN_MS = 60 * 60 * 1000L; // 1 giờ
    private static final String PREF_OTA_LAST_CHECK = "ota_last_check_time";

    private Context context;
    private OTAUpdateService updateService;
    private OTADownloadManager downloadManager;
    private android.app.Dialog updateDialog;
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
            this.updateService = new OTAUpdateService(context);
            this.updateService.setUpdateListener(this);
        } else {
            // Pass fresh context to updateService too
            this.updateService.setContext(context);
        }
        if (this.downloadManager == null) {
            this.downloadManager = new OTADownloadManager(context);
            this.downloadManager.setDownloadListener(this);
        }
    }
    
    /**
     * Start checking for updates (shows toast if no update)
     */
    public void checkForUpdates() {
        silentCheck = false;
        updateService.checkForUpdates();
    }
    
    /**
     * Check for updates silently - no toast if no update (for startup auto-check)
     * Skips if checked within the last hour.
     */
    public void checkForUpdatesSilently() {
        android.content.SharedPreferences prefs = context.getSharedPreferences("ota_prefs", Context.MODE_PRIVATE);
        long lastCheck = prefs.getLong(PREF_OTA_LAST_CHECK, 0);
        long elapsed = System.currentTimeMillis() - lastCheck;
        if (elapsed < OTA_CHECK_COOLDOWN_MS) {
            return;
        }
        silentCheck = true;
        prefs.edit().putLong(PREF_OTA_LAST_CHECK, System.currentTimeMillis()).apply();
        updateService.checkForUpdates();
    }
    
    // OTAUpdateService.OTAUpdateListener implementation
    @Override
    public void onUpdateAvailable(OTAUpdateService.UpdateInfo updateInfo) {
        showUpdateDialog(updateInfo);
    }

    @Override
    public void onNoUpdateAvailable() {
        if (!silentCheck) {
            new ToastMsg(context).toastIconSuccess("Bạn đang sử dụng phiên bản mới nhất!");
        }
    }

    @Override
    public void onUpdateCheckFailed(String error) {
    }
    
    // OTADownloadManager.OTADownloadListener implementation
    @Override
    public void onDownloadStarted() {
        if (downloadProgress != null) {
            downloadProgress.setIndeterminate(true);
        }
        if (downloadStatus != null) {
            downloadStatus.setText("Đang tải xuống...");
        }
    }
    
    @Override
    public void onDownloadProgress(int progress) {
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
        if (downloadDialog != null) {
            downloadDialog.dismiss();
        }
        
        showInstallDialog(apkPath);
    }
    
    @Override
    public void onDownloadFailed(String error) {
        if (downloadDialog != null) {
            downloadDialog.dismiss();
        }
        new ToastMsg(context).toastIconError("Tải xuống thất bại: " + error);
    }
    
    private int dp(int dpNum) {
        return (int) (dpNum * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    /**
     * Show update available dialog
     */
    private void showUpdateDialog(OTAUpdateService.UpdateInfo updateInfo) {
        try {
            android.app.Dialog dialog = new android.app.Dialog(context);
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
            dialog.setCancelable(!updateInfo.forceUpdate);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }

            // Root container
            android.widget.LinearLayout root = new android.widget.LinearLayout(context);
            root.setOrientation(android.widget.LinearLayout.VERTICAL);
            root.setPadding(dp(24), dp(24), dp(24), dp(24));
            
            // Background with rounded corners
            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            shape.setColor(0xFF1E1E2E);
            shape.setCornerRadius(dp(12));
            root.setBackground(shape);

            root.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                    dp(460), android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

            // Title
            android.widget.TextView titleView = new android.widget.TextView(context);
            titleView.setText(updateInfo.forceUpdate ? "Cập nhật bắt buộc" : "Cập nhật có sẵn");
            titleView.setTextColor(0xFFFFFFFF);
            titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20);
            titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            titleView.setGravity(android.view.Gravity.CENTER);
            titleView.setPadding(0, 0, 0, dp(16));
            root.addView(titleView);

            // Thin divider
            android.view.View divider = new android.view.View(context);
            divider.setBackgroundColor(0x33FFFFFF);
            android.widget.LinearLayout.LayoutParams divLp = 
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
            divLp.bottomMargin = dp(16);
            root.addView(divider, divLp);

            // Version info
            android.widget.TextView versionText = new android.widget.TextView(context);
            versionText.setText("Phiên bản: " + updateInfo.version);
            versionText.setTextColor(0xFFFFFFFF);
            versionText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
            versionText.setGravity(android.view.Gravity.CENTER);
            versionText.setPadding(0, 0, 0, dp(6));
            root.addView(versionText);

            // Size info
            android.widget.TextView sizeText = new android.widget.TextView(context);
            sizeText.setText("Kích thước: " + updateInfo.getFormattedFileSize());
            sizeText.setTextColor(0xFFAAAAAA);
            sizeText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
            sizeText.setGravity(android.view.Gravity.CENTER);
            sizeText.setPadding(0, 0, 0, dp(16));
            root.addView(sizeText);

            // Changelog Box
            android.widget.ScrollView sv = new android.widget.ScrollView(context);
            sv.setVerticalScrollBarEnabled(false);
            
            android.graphics.drawable.GradientDrawable scrollShape = new android.graphics.drawable.GradientDrawable();
            scrollShape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            scrollShape.setColor(0xFF2A2A3E); // lighter inner background
            scrollShape.setCornerRadius(dp(8));
            sv.setBackground(scrollShape);
            
            sv.setPadding(dp(16), dp(16), dp(16), dp(16));

            android.widget.TextView changelogText = new android.widget.TextView(context);
            String changelogStr = updateInfo.changelog != null && !updateInfo.changelog.isEmpty()
                    ? updateInfo.changelog.replace("\\n", "\n")
                    : "• Cải thiện hiệu suất\n• Sửa lỗi";
            changelogText.setText(changelogStr);
            changelogText.setTextColor(0xFFFFFFFF);
            changelogText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
            changelogText.setLineSpacing(dp(4), 1.0f);
            
            sv.addView(changelogText);

            android.widget.LinearLayout.LayoutParams svLp = 
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(140));
            svLp.bottomMargin = dp(24);
            root.addView(sv, svLp);

            // Buttons Layout
            android.widget.LinearLayout buttonLayout = new android.widget.LinearLayout(context);
            buttonLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);

            // Later Button
            android.widget.TextView laterButton = new android.widget.TextView(context);
            laterButton.setText("Để sau");
            laterButton.setTextColor(0xFFDDDDDD);
            laterButton.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
            laterButton.setGravity(android.view.Gravity.CENTER);
            laterButton.setPadding(0, dp(14), 0, dp(14));
            laterButton.setFocusable(true);
            laterButton.setClickable(true);
            
            android.graphics.drawable.GradientDrawable laterShape = new android.graphics.drawable.GradientDrawable();
            laterShape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            laterShape.setColor(0xFF333344);
            laterShape.setCornerRadius(dp(6));
            laterButton.setBackground(laterShape);

            android.widget.LinearLayout.LayoutParams laterLp = new android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            laterLp.rightMargin = dp(8);

            // Update Button
            android.widget.TextView updateButton = new android.widget.TextView(context);
            updateButton.setText("✓ Cập nhật");
            updateButton.setTextColor(0xFFFFFFFF);
            updateButton.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
            updateButton.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            updateButton.setGravity(android.view.Gravity.CENTER);
            updateButton.setPadding(0, dp(14), 0, dp(14));
            updateButton.setFocusable(true);
            updateButton.setClickable(true);
            
            android.graphics.drawable.GradientDrawable updateShape = new android.graphics.drawable.GradientDrawable();
            updateShape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            updateShape.setColor(0xFF007ACC); 
            updateShape.setCornerRadius(dp(6));
            updateButton.setBackground(updateShape);

            android.widget.LinearLayout.LayoutParams updateLp = new android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            updateLp.leftMargin = dp(8);

            // Focus states
            View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
                if (hasFocus) {
                    if (v == updateButton) {
                        updateShape.setColor(0xFF64B5F6); // Lighter blue focus
                        updateButton.setBackground(updateShape);
                    } else {
                        laterShape.setColor(0xFF555566); // Lighter grey focus
                        laterButton.setBackground(laterShape);
                    }
                } else {
                    if (v == updateButton) {
                        updateShape.setColor(0xFF007ACC);
                        updateButton.setBackground(updateShape);
                    } else {
                        laterShape.setColor(0xFF333344);
                        laterButton.setBackground(laterShape);
                    }
                }
            };
            laterButton.setOnFocusChangeListener(focusListener);
            updateButton.setOnFocusChangeListener(focusListener);

            laterButton.setOnClickListener(v -> dialog.dismiss());
            updateButton.setOnClickListener(v -> {
                dialog.dismiss();
                startDownload(updateInfo);
            });

            if (!updateInfo.forceUpdate) {
                buttonLayout.addView(laterButton, laterLp);
            } else {
                updateLp.leftMargin = 0;
            }
            buttonLayout.addView(updateButton, updateLp);

            root.addView(buttonLayout);

            dialog.setContentView(root);
            this.updateDialog = dialog;

            if (updateDialog.getWindow() != null) {
                updateDialog.getWindow().clearFlags(
                    android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            }

            updateDialog.setOnShowListener(d -> {
                updateButton.requestFocus();
            });

            updateDialog.show();

        } catch (android.view.WindowManager.BadTokenException e) {
            PreferenceUtils.saveString(context, "ota_pending_version", updateInfo.version);
            PreferenceUtils.saveString(context, "ota_pending_download_url", updateInfo.downloadUrl);
        } catch (Exception e) {
            e.printStackTrace();
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
        android.content.Context themedCtx = new androidx.appcompat.view.ContextThemeWrapper(
                context, androidx.appcompat.R.style.Theme_AppCompat_Dialog);
        AlertDialog.Builder builder = new AlertDialog.Builder(themedCtx);
        View dialogView = LayoutInflater.from(themedCtx).inflate(R.layout.dialog_ota_download, null);
        
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
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Cài đặt cập nhật")
               .setMessage("Tải xuống hoàn tất! Bạn có muốn cài đặt cập nhật ngay bây giờ?")
               .setPositiveButton("Cài đặt", (dialog, which) -> {
                   installUpdate(apkPath);
               })
               .setNegativeButton("Để sau", (dialog, which) -> {
                   // Save APK path for later installation
                   PreferenceUtils.saveString(context, "ota_pending_apk", apkPath);
                   new ToastMsg(context).toastIconSuccess("Cập nhật đã sẵn sàng cài đặt");
               })
               .setCancelable(false);
        
        try {
            AlertDialog dialog = builder.show();
            
            // Auto-click install after 3 seconds for Android TV
            new android.os.Handler().postDelayed(() -> {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                    installUpdate(apkPath);
                }
            }, 3000);
            
        } catch (Exception e) {
            // Fallback: direct install
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
            }
            
            if (updateDialog != null && updateDialog.isShowing()) {
                updateDialog.dismiss();
                updateDialog = null;
            }
            
            if (downloadDialog != null && downloadDialog.isShowing()) {
                downloadDialog.dismiss();
                downloadDialog = null;
            }
            
        } catch (Exception e) {
        }
    }
}
