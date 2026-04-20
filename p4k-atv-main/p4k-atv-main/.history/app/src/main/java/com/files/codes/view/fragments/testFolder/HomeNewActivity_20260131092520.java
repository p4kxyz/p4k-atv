package com.files.codes.view.fragments.testFolder;

import androidx.fragment.app.FragmentActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import com.files.codes.R;
import com.files.codes.view.OTAUpdateManager;
import android.widget.ImageView;
import android.view.View;

public class HomeNewActivity extends FragmentActivity {
    
    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int REQUEST_INSTALL_PERMISSION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_new);
        
        // Settings Orb Logic
        setupSettingsOrb();

        // CRITICAL: Add OTA update check to the correct home activity
        // Initialize app permissions and update check
        
        // Request permissions first, then check for updates
        requestAllPermissions();
    }
    
    private void setupSettingsOrb() {
        ImageView settingsOrb = findViewById(R.id.settings_orb);
        if (settingsOrb != null) {
            settingsOrb.bringToFront();
            settingsOrb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showQuickSettingsDialog();
                }
            });
        }
    }
    
    private void showQuickSettingsDialog() {
        final android.content.SharedPreferences pref = getSharedPreferences(com.files.codes.utils.Constants.USER_LOGIN_STATUS, MODE_PRIVATE);
        final boolean useExternalPlayer = pref.getBoolean("use_external_player", false);
        final boolean useSoftwareAudio = pref.getBoolean("audio_priority_sw", true); // Default to True (SW)

        String[] items = new String[2];
        items[0] = "Trình phát: " + (useExternalPlayer ? "Bên ngoài (VLC/MX)" : "Mặc định (ExoPlayer)");
        items[1] = "Ưu tiên Audio: " + (useSoftwareAudio ? "Phần mềm (SW - Ổn định)" : "Phần cứng (HW - Mắt định)");

        new android.app.AlertDialog.Builder(this)
            .setTitle("Cài đặt nhanh")
            .setItems(items, new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    android.content.SharedPreferences.Editor editor = pref.edit();
                    if (which == 0) {
                        boolean newValue = !useExternalPlayer;
                        editor.putBoolean("use_external_player", newValue);
                        editor.apply();
                        new com.files.codes.utils.ToastMsg(HomeNewActivity.this).toastIconSuccess(newValue ? "Đã chọn: Trình phát ngoài" : "Đã chọn: ExoPlayer");
                    } else if (which == 1) {
                        boolean newValue = !useSoftwareAudio;
                        editor.putBoolean("audio_priority_sw", newValue);
                        editor.apply();
                        new com.files.codes.utils.ToastMsg(HomeNewActivity.this).toastIconSuccess(newValue ? "Đã chọn: Ưu tiên Audio Software" : "Đã chọn: Ưu tiên Audio Hardware");
                    }
                    dialog.dismiss();
                    // Re-show dialog to reflect changes
                    showQuickSettingsDialog();
                }
            })
            .setNegativeButton("Đóng", null)
            .show();
    }
    
    /**
     * Request all necessary permissions for OTA updates
     */
    private void requestAllPermissions() {
        // Request all necessary permissions for OTA updates
        
        // Check storage permission first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("HomeNewActivity", "📁 Requesting storage permission...");
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                REQUEST_STORAGE_PERMISSION);
        } else {
            // Storage permission granted, check install permission
            checkInstallPermission();
        }
    }
    
    /**
     * Check and request install permission for Android 8.0+
     * Skip on Android TV as it doesn't support MANAGE_UNKNOWN_APP_SOURCES
     */
    private void checkInstallPermission() {
        // Check if running on Android TV
        if (isAndroidTV()) {
            Log.d("HomeNewActivity", "📺 Android TV detected, skipping install permission check");
            startOTACheck();
            return;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                if (!getPackageManager().canRequestPackageInstalls()) {
                    Log.d("HomeNewActivity", "📱 Requesting install permission...");
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_INSTALL_PERMISSION);
                } else {
                    // All permissions granted, start OTA check
                    startOTACheck();
                }
            } catch (Exception e) {
                Log.e("HomeNewActivity", "❌ Error checking install permission: " + e.getMessage());
                // On error, just proceed with OTA check
                startOTACheck();
            }
        } else {
            // Android < 8.0, no install permission needed
            startOTACheck();
        }
    }
    
    /**
     * Check if running on Android TV
     */
    private boolean isAndroidTV() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_TYPE_MASK) 
                == Configuration.UI_MODE_TYPE_TELEVISION;
    }
    
    /**
     * Start OTA update check after permissions are granted
     */
    private void startOTACheck() {
        Log.d("HomeNewActivity", "✅ All permissions granted, starting OTA check...");
        
        // Check for OTA updates after a short delay to let UI initialize
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("HomeNewActivity", "🚀 Starting OTA update check...");
                checkForUpdates();
            }
        }, 2000); // 2 second delay
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("HomeNewActivity", "✅ Storage permission granted");
                checkInstallPermission();
            } else {
                Log.e("HomeNewActivity", "❌ Storage permission denied");
                // Still try to check install permission
                checkInstallPermission();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_INSTALL_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (getPackageManager().canRequestPackageInstalls()) {
                    Log.d("HomeNewActivity", "✅ Install permission granted");
                } else {
                    Log.e("HomeNewActivity", "❌ Install permission denied");
                }
            }
            // Start OTA check regardless of permission result
            startOTACheck();
        }
    }
    
    /**
     * Check for OTA updates when app starts
     */
    private void checkForUpdates() {
        try {
            Log.d("HomeNewActivity", "📱 Getting OTAUpdateManager instance...");
            OTAUpdateManager otaManager = OTAUpdateManager.getInstance(this);
            Log.d("HomeNewActivity", "✅ OTAUpdateManager instance created: " + (otaManager != null ? "SUCCESS" : "NULL"));
            
            Log.d("HomeNewActivity", "🔍 Calling checkForUpdates...");
            otaManager.checkForUpdates();
            Log.d("HomeNewActivity", "✅ checkForUpdates called successfully");
        } catch (Exception e) {
            Log.e("HomeNewActivity", "❌ Error checking for updates", e);
            Log.e("HomeNewActivity", "❌ Exception details: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            if (e.getCause() != null) {
                Log.e("HomeNewActivity", "❌ Cause: " + e.getCause().getMessage());
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // MEMORY LEAK FIX: Cleanup OTA download manager
        try {
            OTAUpdateManager otaManager = OTAUpdateManager.getInstance(this);
            if (otaManager != null) {
                otaManager.cleanup();
                Log.d("HomeNewActivity", "🧹 OTA Manager cleaned up successfully");
            }
        } catch (Exception e) {
            Log.e("HomeNewActivity", "❌ Error cleaning up OTA Manager: " + e.getMessage());
        }
    }
}