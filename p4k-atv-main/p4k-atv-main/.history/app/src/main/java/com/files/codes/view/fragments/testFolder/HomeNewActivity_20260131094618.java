package com.files.codes.view.fragments.testFolder;

import android.view.KeyEvent;
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
        setupFocusSearchListener();
        
        // CRITICAL: Add OTA update check to the correct home activity
        // Initialize app permissions and update check
        
        // Request permissions first, then check for updates
        requestAllPermissions();
    }
    
    private void setupSettingsOrb() {
        final ImageView settingsOrb = findViewById(R.id.settings_orb);
        if (settingsOrb != null) {
            settingsOrb.bringToFront();
            // Ensure focusable
            settingsOrb.setFocusable(true);
            settingsOrb.setFocusableInTouchMode(true);
            
            // Add Focus Animation (Scale Effect)
            settingsOrb.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        v.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).start();
                        // Highlight with a color tint
                        settingsOrb.setColorFilter(android.graphics.Color.parseColor("#FFC107")); // Amber/Gold color
                        v.setBackgroundColor(android.graphics.Color.parseColor("#33FFFFFF")); // Semi-transparent white bg
                    } else {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                        settingsOrb.clearColorFilter();
                        
                        // Reset background to selectable item
                        android.util.TypedValue outValue = new android.util.TypedValue();
                        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
                        v.setBackgroundResource(outValue.resourceId);
                    }
                }
            });

            settingsOrb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showQuickSettingsDialog();
                }
            });
        }
    }

    private void setupFocusSearchListener() {
        com.files.codes.view.CustomFrameLayout customFrameLayout = findViewById(R.id.custom_frame_layout);
        final View settingsOrb = findViewById(R.id.settings_orb);
        final View rowsContainer = findViewById(R.id.rows_container);
        
        if (customFrameLayout != null && settingsOrb != null) {
            customFrameLayout.setOnFocusSearchListener(new com.files.codes.view.CustomFrameLayout.OnFocusSearchListener() {
                @Override
                public View onFocusSearch(View focused, int direction) {
                    // Logic to bridge focus between Content (Rows) and Settings Orb
                    if (direction == View.FOCUS_UP) {
                        // If navigating UP from content, go to Settings
                        if (rowsContainer != null && isChildOf(focused, rowsContainer)) {
                            Log.d("HomeNewActivity", "FocusSearch: UP detected from Content -> Settings Orb");
                            return settingsOrb;
                        }
                    } else if (direction == View.FOCUS_DOWN) {
                        // If navigating DOWN from Settings, go to Content
                        if (focused == settingsOrb) {
                             Log.d("HomeNewActivity", "FocusSearch: DOWN detected from Settings Orb -> Content");
                             return rowsContainer;
                        }
                    }
                    return null; // Let system handle other cases
                }
            });
        }
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
            View focused = getCurrentFocus();
            View settingsOrb = findViewById(R.id.settings_orb);
            View rowsContainer = findViewById(R.id.rows_container);
            
            // Backup check: If we are in the rows container and press UP
            if (focused != null && settingsOrb != null && rowsContainer != null && isChildOf(focused, rowsContainer)) {
                 // Check vertical position to avoid jumping from middle of list
                 int[] location = new int[2];
                 focused.getLocationOnScreen(location);
                 // If the focused view is in the top 20% of the screen (heuristic for top row)
                 // Or we could check specific Y coordinate.
                 // Let's rely on FocusSearch first, logging will tell us if dispatchKeyEvent is needed.
                 // Log.d("HomeNewActivity", "DispatchKeyEvent: UP in content. Y=" + location[1]);
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private boolean isChildOf(View child, View parent) {
        if (child == parent) return true;
        if (child.getParent() instanceof View) {
            return isChildOf((View) child.getParent(), parent);
        }
        return false;
    }
    
    private void showQuickSettingsDialog() {
        final String[] mainItems = {"Cài đặt Trình phát", "Ưu tiên Âm thanh", "Đóng"};
        
        new android.app.AlertDialog.Builder(this)
            .setTitle("Cài đặt nhanh")
            .setItems(mainItems, new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                   if (which == 0) showPlayerSelectionDialog();
                   else if (which == 1) showAudioSelectionDialog();
                   else dialog.dismiss();
                }
            })
            .show();
    }

    private void showPlayerSelectionDialog() {
       final android.content.SharedPreferences pref = getSharedPreferences(com.files.codes.utils.Constants.USER_LOGIN_STATUS, MODE_PRIVATE);
       final boolean useExternal = pref.getBoolean("use_external_player", false);
       final String currentExternalPlayer = pref.getString("selected_player", "just");
       
       // Define list including ExoPlayer as first option
       final String[] displayNames = {
           "ExoPlayer (Mặc định)", // 0. Internal
           "Phim4K Player",        // 1. p4k
           "Kodi",                 // 2. kodi
           "Just Player",          // 3. just
           "MX Player",            // 4. mx
           "Vimu Player",          // 5. vimu
           "VLC Player",           // 6. vlc
           "nPlayer",              // 7. nplayer
           "Dune HD (Realtek)",    // 8. dune_realtek
           "Dune HD (Amlogic)",    // 9. dune_amlogic
           "Zidoo (Realtek)",      // 10. zidoo_realtek
           "Zidoo (Amlogic)"       // 11. zidoo_amlogic
       };
       
       final String[] valueDetails = {
           "internal",
           "p4k", "kodi", "just", "mx", "vimu", "vlc", "nplayer", 
           "dune_realtek", "dune_amlogic", "zidoo_realtek", "zidoo_amlogic"
       };
       
       // Determine initial selection
       int selectedIndex = 0; // Default to Internal
       if (useExternal) {
           for (int i = 1; i < valueDetails.length; i++) {
               if (valueDetails[i].equals(currentExternalPlayer)) {
                   selectedIndex = i;
                   break;
               }
           }
           // Fallback to "Just Player" (index 3) if external is on but value not matched
           if (selectedIndex == 0) selectedIndex = 3; 
       }
       
       new android.app.AlertDialog.Builder(this)
           .setTitle("Chọn Trình phát")
           .setSingleChoiceItems(displayNames, selectedIndex, new android.content.DialogInterface.OnClickListener() {
               @Override
               public void onClick(android.content.DialogInterface dialog, int which) {
                    android.content.SharedPreferences.Editor editor = pref.edit();
                    
                    if (which == 0) { // Internal ExoPlayer
                        editor.putBoolean("use_external_player", false);
                        // We don't change 'selected_player' so it remembers preference if toggled back
                    } else { // External Player
                        editor.putBoolean("use_external_player", true);
                        editor.putString("selected_player", valueDetails[which]);
                    }
                    editor.apply();
                    
                    new com.files.codes.utils.ToastMsg(HomeNewActivity.this).toastIconSuccess("Đã chọn: " + displayNames[which]);
                    dialog.dismiss();
                    showQuickSettingsDialog(); // Re-open main
               }
           })
           .show();
    }

    private void showAudioSelectionDialog() {
       final android.content.SharedPreferences pref = getSharedPreferences(com.files.codes.utils.Constants.USER_LOGIN_STATUS, MODE_PRIVATE);
       // Migration: if 'audio_priority_mode' missing, use 'audio_priority_sw'
       int currentMode = pref.getInt("audio_priority_mode", -1);
       if (currentMode == -1) {
           boolean oldSw = pref.getBoolean("audio_priority_sw", true);
           currentMode = oldSw ? 1 : 0; 
       }
       
       String[] items = {"Phần cứng (Hardware)", "Phần mềm (Software - Ffmpeg)", "Passthrough (Dành cho dàn âm thanh)"};
       
       new android.app.AlertDialog.Builder(this)
           .setTitle("Ưu tiên Âm thanh")
           .setSingleChoiceItems(items, currentMode, new android.content.DialogInterface.OnClickListener() {
               @Override
               public void onClick(android.content.DialogInterface dialog, int which) {
                    pref.edit().putInt("audio_priority_mode", which).apply();
                    // Update legacy boolean too for safety
                    pref.edit().putBoolean("audio_priority_sw", (which == 1)).apply(); 
                    
                    String label = "Hardware";
                    if (which == 1) label = "Software";
                    if (which == 2) label = "Passthrough";
                    
                    new com.files.codes.utils.ToastMsg(HomeNewActivity.this).toastIconSuccess("Đã chọn: " + label);
                    dialog.dismiss();
                    showQuickSettingsDialog();
               }
           })
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