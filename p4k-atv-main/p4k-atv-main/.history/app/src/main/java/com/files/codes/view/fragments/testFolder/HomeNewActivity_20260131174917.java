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
import androidx.leanback.widget.SearchOrbView;
import androidx.leanback.widget.VerticalGridView;
import androidx.fragment.app.Fragment;
import java.lang.reflect.Method;

public class HomeNewActivity extends FragmentActivity {
    
    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int REQUEST_INSTALL_PERMISSION = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_new);
        
        // Settings Orb Logic
        setupSettingsOrb();
        setupSearchOrb(); // Initialize Search Orb
        setupFocusSearchListener();
        
        // CRITICAL: Add OTA update check to the correct home activity
        // Initialize app permissions and update check
        
        // Request permissions first, then check for updates
        requestAllPermissions();
    }
    
    public void setOrbsVisibility(boolean visible) {
        View searchOrb = findViewById(R.id.custom_search_orb);
        View settingsOrb = findViewById(R.id.settings_orb);
        
        int visibility = visible ? View.VISIBLE : View.GONE;
        
        if (searchOrb != null) {
            searchOrb.setVisibility(visibility);
        }
        if (settingsOrb != null) {
            settingsOrb.setVisibility(visibility);
        }
    }

    private void setupSettingsOrb() {
        SearchOrbView settingsOrb = findViewById(R.id.settings_orb);
        if (settingsOrb != null) {
            settingsOrb.bringToFront();
            
            // Fix: Force icon to be white to match Search button style (Red Orb, White Icon)
            android.graphics.drawable.Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_settings_white);
            if (icon != null) {
                // Wrap and mutate to ensure safe tinting
                icon = androidx.core.graphics.drawable.DrawableCompat.wrap(icon).mutate();
                androidx.core.graphics.drawable.DrawableCompat.setTint(icon, android.graphics.Color.WHITE);
                settingsOrb.setOrbIcon(icon);
            }
            
            settingsOrb.setOrbColor(getResources().getColor(R.color.colorAccent));
            settingsOrb.setOnOrbClickedListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showQuickSettingsDialog();
                }
            });
        }
    }
    
    private void setupSearchOrb() {
        SearchOrbView searchOrb = findViewById(R.id.custom_search_orb);
        if (searchOrb != null) {
             searchOrb.setOrbColor(getResources().getColor(R.color.colorAccent));
             searchOrb.bringToFront();
             searchOrb.setOnOrbClickedListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View view) {
                     // Launch Search Activity
                     try {
                         Intent intent = new Intent(HomeNewActivity.this, com.files.codes.view.SearchActivity.class);
                         startActivity(intent);
                     } catch (Exception e) {
                         Log.e("HomeNewActivity", "Error launching search", e);
                     }
                 }
             });
        }
    }

    private void setupFocusSearchListener() {
        com.files.codes.view.CustomFrameLayout customFrameLayout = findViewById(R.id.custom_frame_layout);
        final View settingsOrb = findViewById(R.id.settings_orb);
        final View searchOrb = findViewById(R.id.custom_search_orb);
        final View rowsContainer = findViewById(R.id.rows_container);
        
        if (customFrameLayout != null) {
            customFrameLayout.setOnFocusSearchListener(new com.files.codes.view.CustomFrameLayout.OnFocusSearchListener() {
                @Override
                public View onFocusSearch(View focused, int direction) {
                    // 1. Navigation from Content -> UP -> Search Orb
                    if (direction == View.FOCUS_UP) {
                        // Logic from HomeActivity backup: Always force Search Orb if visibility is visible and focused is not orb
                        if (focused != searchOrb && searchOrb.getVisibility() == View.VISIBLE) {
                             Log.d("HomeNewActivity", "FocusSearch: UP from content -> Search Orb");
                             return searchOrb; 
                        }
                    } 
                    
                    // 2. Navigation between Search Orb <-> Settings Orb (LEFT/RIGHT)
                    if (focused == searchOrb && direction == View.FOCUS_RIGHT) {
                         Log.d("HomeNewActivity", "FocusSearch: RIGHT from Search -> Settings");
                         return settingsOrb;
                    }
                    if (focused == settingsOrb && direction == View.FOCUS_LEFT) {
                         Log.d("HomeNewActivity", "FocusSearch: LEFT from Settings -> Search");
                         return searchOrb;
                    }
                    
                    // 3. Navigation DOWN from Top Bar -> Content
                    if ((focused == searchOrb || focused == settingsOrb) && direction == View.FOCUS_DOWN) {
                         Log.d("HomeNewActivity", "FocusSearch: DOWN from Top Bar -> Content");
                         
                         // Try to get VerticalGridView from the active fragment first (Best practice from backup)
                         androidx.fragment.app.Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.page_list_fragment);
                         if (fragment == null) {
                             // Fallback to container lookup if fragment was replaced
                             fragment = getSupportFragmentManager().findFragmentById(R.id.rows_container);
                         }

                         if (fragment != null) {
                             // Use helper method to find the specific GridView for Leanback fragments (Movies, TV Series, etc.)
                             View gridView = getVerticalGridView(fragment);
                             if (gridView != null) {
                                  return gridView;
                             }
                             
                             if (fragment.getView() != null) return fragment.getView();
                         }
                         
                         return rowsContainer;
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
        final String[] mainItems = {"Cài đặt Trình phát", "Giải mã Âm thanh", "Đóng"};
        
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
       
       String[] items = {
           "Phần cứng (Dành cho các thiết bị hỗ trợ đầy đủ codec)", 
           "Phần mềm (Khi bạn bị lỗi mất tiếng hãy dùng)", 
           "Passthrough (Dành cho dàn âm thanh ngoài)"
       };
       
       new android.app.AlertDialog.Builder(this)
           .setTitle("Giải mã Âm thanh")
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

    public androidx.leanback.widget.VerticalGridView getVerticalGridView(androidx.fragment.app.Fragment fragment) {
        try {
            if (fragment == null) return null;

            // Direct check for known fragments (from backup logic)
            String className = fragment.getClass().getSimpleName();
            // Assuming these extend BaseRowSupportFragment or similar
            if (className.equals("TvSeriesFragment") || className.equals("MoviesFragment") || 
                className.equals("HomeNewFragment") || className.equals("GenreFragment") || 
                className.equals("CountryFragment") || className.equals("FavouriteFragment") ||
                className.equals("MyAccountFragment")) {

                // Use reflection to find getVerticalGridView
                // Try BaseRowSupportFragment first as it contains the method in Leanback lib
                Class<?> clazz;
                try {
                    clazz = Class.forName("androidx.leanback.app.BaseRowSupportFragment");
                } catch (ClassNotFoundException e) {
                    // Fallback to BaseSupportFragment if needed (though method is usually in Row)
                    clazz = Class.forName("androidx.leanback.app.BaseSupportFragment");
                }

                java.lang.reflect.Method method = clazz.getDeclaredMethod("getVerticalGridView");
                method.setAccessible(true);
                return (androidx.leanback.widget.VerticalGridView) method.invoke(fragment);
            } else {
                 // Fallback for generic case
                 Class<?> clazz = Class.forName("androidx.leanback.app.BaseRowSupportFragment");
                 java.lang.reflect.Method method = clazz.getDeclaredMethod("getVerticalGridView");
                 method.setAccessible(true);
                 return (androidx.leanback.widget.VerticalGridView) method.invoke(fragment);
            }

        } catch (Exception e) {
            Log.e("HomeNewActivity", "Error getting VerticalGridView", e);
        }
        return null;
    }
}