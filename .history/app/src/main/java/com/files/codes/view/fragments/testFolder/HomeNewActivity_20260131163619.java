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

import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;

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
        setupSearchOrb(); // Initialize Search Orb
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
    
    private void setupSearchOrb() {
        androidx.leanback.widget.SearchOrbView searchOrb = findViewById(R.id.custom_search_orb);
        if (searchOrb != null) {
             searchOrb.setOrbColor(android.graphics.Color.parseColor("#E50914")); // Netflix Red or Brand Color
             searchOrb.setOnClickListener(new View.OnClickListener() {
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
             
             // Add Scale Animation
             searchOrb.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        v.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).start();
                    } else {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
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
                        if (rowsContainer != null && isChildOf(focused, rowsContainer)) {
                            Log.d("HomeNewActivity", "FocusSearch: UP from content -> Search Orb");
                            return searchOrb; // Prioritize Search on UP
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
        final String[] mainItems = {"Cài đặt Trình phát", "Giải mã Âm thanh", "Cài đặt hiển thị phụ đề", "Đóng"};
        
        new android.app.AlertDialog.Builder(this)
            .setTitle("Cài đặt nhanh")
            .setItems(mainItems, new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                   if (which == 0) showPlayerSelectionDialog();
                   else if (which == 1) showAudioSelectionDialog();
                   else if (which == 2) openSubtitleSettingsDialog();
                   else dialog.dismiss();
                }
            })
            .show();
    }

    private void saveSubtitleSetting(String key, int value) {
        android.content.SharedPreferences.Editor editor = getSharedPreferences("subtitle_settings", MODE_PRIVATE).edit();
        editor.putInt(key, value);
        editor.apply();
    }

    private void saveSubtitleSetting(String key, boolean value) {
        android.content.SharedPreferences.Editor editor = getSharedPreferences("subtitle_settings", MODE_PRIVATE).edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void saveSubtitleSetting(String key, float value) {
        android.content.SharedPreferences.Editor editor = getSharedPreferences("subtitle_settings", MODE_PRIVATE).edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    private void openSubtitleSettingsDialog() {
        // Subtitle settings dialog with all customization options
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Cài đặt phụ đề");
        
        // Create a ScrollView to handle long content
        ScrollView scrollView = new ScrollView(this);
        
        // Create a vertical layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        
        android.content.SharedPreferences prefs = getSharedPreferences("subtitle_settings", MODE_PRIVATE);
        
        // Font size controls
        TextView fontSizeLabel = new TextView(this);
        fontSizeLabel.setText("Cỡ chữ:");
        fontSizeLabel.setTextSize(16);
        fontSizeLabel.setPadding(0, 10, 0, 10);
        
        LinearLayout fontSizeLayout = new LinearLayout(this);
        fontSizeLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button fontSizeMinusBtn = new Button(this);
        fontSizeMinusBtn.setText("-");
        Button fontSizePlusBtn = new Button(this);
        fontSizePlusBtn.setText("+");
        TextView fontSizeTV = new TextView(this);
        fontSizeTV.setText(prefs.getInt("font_size", 20) + "sp");
        fontSizeTV.setGravity(android.view.Gravity.CENTER);
        fontSizeTV.setPadding(20, 0, 20, 0);
        
        fontSizeLayout.addView(fontSizeMinusBtn);
        fontSizeLayout.addView(fontSizeTV);
        fontSizeLayout.addView(fontSizePlusBtn);
        
        // Font type controls
        TextView fontTypeLabel = new TextView(this);
        fontTypeLabel.setText("Kiểu chữ:");
        fontTypeLabel.setTextSize(16);
        fontTypeLabel.setPadding(0, 20, 0, 10);
        
        LinearLayout fontTypeLayout = new LinearLayout(this);
        fontTypeLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button fontTypePrevBtn = new Button(this);
        fontTypePrevBtn.setText("◀");
        Button fontTypeNextBtn = new Button(this);
        fontTypeNextBtn.setText("▶");
        TextView fontTypeTV = new TextView(this);
        
        String[] fontNames = {"Mặc định", "Sans Serif", "Serif", "Monospace", "Tiếng Việt"};
        int currentFontType = prefs.getInt("font_type", 4); // Default to Vietnamese
        // Bounds checking to prevent crash
        if (currentFontType >= fontNames.length) {
            currentFontType = 4; // Default to Vietnamese
            prefs.edit().putInt("font_type", currentFontType).apply();
        }
        fontTypeTV.setText(fontNames[currentFontType]);
        fontTypeTV.setGravity(android.view.Gravity.CENTER);
        fontTypeTV.setPadding(20, 0, 20, 0);
        
        fontTypeLayout.addView(fontTypePrevBtn);
        fontTypeLayout.addView(fontTypeTV);
        fontTypeLayout.addView(fontTypeNextBtn);
        
        // Position controls
        TextView positionLabel = new TextView(this);
        positionLabel.setText("Vị trí:");
        positionLabel.setTextSize(16);
        positionLabel.setPadding(0, 20, 0, 10);
        
        LinearLayout positionLayout = new LinearLayout(this);
        positionLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button positionUpBtn = new Button(this);
        positionUpBtn.setText("Lên");
        Button positionDownBtn = new Button(this);
        positionDownBtn.setText("Xuống");
        TextView positionTV = new TextView(this);
        
        // Position logic - offset from default position (negative = closer to bottom, positive = further up)
        int currentOffset = prefs.getInt("vertical_offset", 0);
        positionTV.setText("Dịch chuyển (" + (currentOffset > 0 ? "+" : "") + currentOffset + "%)");
        positionTV.setGravity(android.view.Gravity.CENTER);
        positionTV.setPadding(20, 0, 20, 0);
        
        positionLayout.addView(positionUpBtn);
        positionLayout.addView(positionTV);
        positionLayout.addView(positionDownBtn);
        
        // Background switch
        LinearLayout backgroundLayout = new LinearLayout(this);
        backgroundLayout.setOrientation(LinearLayout.HORIZONTAL);
        backgroundLayout.setPadding(0, 20, 0, 10);
        TextView backgroundLabel = new TextView(this);
        backgroundLabel.setText("Nền: ");
        backgroundLabel.setTextSize(16);
        Switch backgroundSwitch = new Switch(this);
        backgroundSwitch.setChecked(prefs.getBoolean("background", false));
        
        backgroundLayout.addView(backgroundLabel);
        backgroundLayout.addView(backgroundSwitch);
        
        // Text Color controls
        TextView textColorLabel = new TextView(this);
        textColorLabel.setText("Màu chữ:");
        textColorLabel.setTextSize(16);
        textColorLabel.setPadding(0, 20, 0, 10);
        
        LinearLayout textColorLayout = new LinearLayout(this);
        textColorLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button textColorPrevBtn = new Button(this);
        textColorPrevBtn.setText("◀");
        Button textColorNextBtn = new Button(this);
        textColorNextBtn.setText("▶");
        TextView textColorTV = new TextView(this);
        
        String[] colorNames = {"Trắng", "Vàng", "Đỏ", "Xanh lá", "Xanh dương", "Cam", "Hồng", "Xanh lơ"};
        int[] colorValues = {Color.WHITE, Color.YELLOW, Color.RED, Color.GREEN, 
                            Color.BLUE, 0xFFFF8C00, 0xFFFFC0CB, Color.CYAN}; // Orange, Pink
        int currentTextColor = prefs.getInt("text_color", 0);
        // Bounds checking to prevent crash
        if (currentTextColor >= colorNames.length) {
            currentTextColor = 0; // Default to White
            prefs.edit().putInt("text_color", currentTextColor).apply();
        }
        textColorTV.setText(colorNames[currentTextColor]);
        textColorTV.setTextColor(colorValues[currentTextColor]);
        textColorTV.setGravity(android.view.Gravity.CENTER);
        textColorTV.setPadding(20, 0, 20, 0);
        
        textColorLayout.addView(textColorPrevBtn);
        textColorLayout.addView(textColorTV);
        textColorLayout.addView(textColorNextBtn);
        
        // Outline Color controls
        TextView outlineColorLabel = new TextView(this);
        outlineColorLabel.setText("Màu viền:");
        outlineColorLabel.setTextSize(16);
        outlineColorLabel.setPadding(0, 20, 0, 10);
        
        LinearLayout outlineColorLayout = new LinearLayout(this);
        outlineColorLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button outlineColorPrevBtn = new Button(this);
        outlineColorPrevBtn.setText("◀");
        Button outlineColorNextBtn = new Button(this);
        outlineColorNextBtn.setText("▶");
        TextView outlineColorTV = new TextView(this);
        
        String[] outlineColorNames = {"Trong suốt", "Đen", "Trắng", "Đỏ", "Xanh dương", "Vàng"};
        int[] outlineColorValues = {Color.TRANSPARENT, Color.BLACK, Color.WHITE, Color.RED, Color.BLUE, Color.YELLOW};
        int currentOutlineColor = prefs.getInt("outline_color", 1);
        // Bounds checking to prevent crash
        if (currentOutlineColor >= outlineColorNames.length) {
            currentOutlineColor = 1; // Default to Black
            prefs.edit().putInt("outline_color", currentOutlineColor).apply();
        }
        outlineColorTV.setText(outlineColorNames[currentOutlineColor]);
        // Show the actual color like text color does
        if (currentOutlineColor == 0) {
            outlineColorTV.setTextColor(Color.GRAY); // Show gray for "None"
        } else {
            outlineColorTV.setTextColor(outlineColorValues[currentOutlineColor]);
        }
        outlineColorTV.setGravity(android.view.Gravity.CENTER);
        outlineColorTV.setPadding(20, 0, 20, 0);
        outlineColorTV.setGravity(android.view.Gravity.CENTER);
        
        outlineColorLayout.addView(outlineColorPrevBtn);
        outlineColorLayout.addView(outlineColorTV);
        outlineColorLayout.addView(outlineColorNextBtn);
        
        // Playback Speed controls
        TextView playbackSpeedLabel = new TextView(this);
        playbackSpeedLabel.setText("Tốc độ phát mặc định:");
        playbackSpeedLabel.setTextSize(16);
        playbackSpeedLabel.setPadding(0, 20, 0, 10);
        
        LinearLayout playbackSpeedLayout = new LinearLayout(this);
        playbackSpeedLayout.setOrientation(LinearLayout.HORIZONTAL);
        playbackSpeedLayout.setGravity(android.view.Gravity.CENTER);
        
        float currentSpeed = prefs.getFloat("playback_speed", 1.0f);
        
        Button speed05 = new Button(this);
        speed05.setText("0.5x");
        speed05.setTextSize(12);
        if (currentSpeed == 0.5f) speed05.setBackgroundColor(Color.GREEN);
        
        Button speed075 = new Button(this);
        speed075.setText("0.75x");
        speed075.setTextSize(12);
        if (currentSpeed == 0.75f) speed075.setBackgroundColor(Color.GREEN);
        
        Button speed10 = new Button(this);
        speed10.setText("1.0x");
        speed10.setTextSize(12);
        if (currentSpeed == 1.0f) speed10.setBackgroundColor(Color.GREEN);
        
        Button speed15 = new Button(this);
        speed15.setText("1.5x");
        speed15.setTextSize(12);
        if (currentSpeed == 1.5f) speed15.setBackgroundColor(Color.GREEN);
        
        Button speed20 = new Button(this);
        speed20.setText("2.0x");
        speed20.setTextSize(12);
        if (currentSpeed == 2.0f) speed20.setBackgroundColor(Color.GREEN);
        
        Button speed30 = new Button(this);
        speed30.setText("3.0x");
        speed30.setTextSize(12);
        if (currentSpeed == 3.0f) speed30.setBackgroundColor(Color.GREEN);
        
        playbackSpeedLayout.addView(speed05);
        playbackSpeedLayout.addView(speed075);
        playbackSpeedLayout.addView(speed10);
        playbackSpeedLayout.addView(speed15);
        playbackSpeedLayout.addView(speed20);
        playbackSpeedLayout.addView(speed30);
        
        // Reset button
        Button resetBtn = new Button(this);
        resetBtn.setText("Khôi phục mặc định");
        resetBtn.setPadding(0, 30, 0, 0);
        
        // Add all views to layout
        layout.addView(fontSizeLabel);
        layout.addView(fontSizeLayout);
        layout.addView(fontTypeLabel);
        layout.addView(fontTypeLayout);
        layout.addView(positionLabel);
        layout.addView(positionLayout);
        layout.addView(backgroundLayout);
        layout.addView(textColorLabel);
        layout.addView(textColorLayout);
        layout.addView(outlineColorLabel);
        layout.addView(outlineColorLayout);
        layout.addView(playbackSpeedLabel);
        layout.addView(playbackSpeedLayout);
        layout.addView(resetBtn);
        
        // Set up button listeners
        fontSizeMinusBtn.setOnClickListener(v -> {
            int currentSize = Integer.parseInt(fontSizeTV.getText().toString().replace("sp", ""));
            if (currentSize > 12) {
                currentSize -= 2;
                fontSizeTV.setText(currentSize + "sp");
                saveSubtitleSetting("font_size", currentSize);
            }
        });
        
        fontSizePlusBtn.setOnClickListener(v -> {
            int currentSize = Integer.parseInt(fontSizeTV.getText().toString().replace("sp", ""));
            if (currentSize < 40) {
                currentSize += 2;
                fontSizeTV.setText(currentSize + "sp");
                saveSubtitleSetting("font_size", currentSize);
            }
        });
        
        fontTypePrevBtn.setOnClickListener(v -> {
            int currentType = prefs.getInt("font_type", 0);
            currentType = (currentType - 1 + fontNames.length) % fontNames.length;
            fontTypeTV.setText(fontNames[currentType]);
            saveSubtitleSetting("font_type", currentType);
        });
        
        fontTypeNextBtn.setOnClickListener(v -> {
            int currentType = prefs.getInt("font_type", 0);
            currentType = (currentType + 1) % fontNames.length;
            fontTypeTV.setText(fontNames[currentType]);
            saveSubtitleSetting("font_type", currentType);
        });
        
        positionUpBtn.setOnClickListener(v -> {
            int offset = prefs.getInt("vertical_offset", 0);
            offset += 5; // Move up (increase offset from bottom - higher value = further from bottom)
            offset = Math.min(offset, 80); // Max 80% from bottom
            String newPositionText = offset == 0 ? "Giữa" : "Lên +" + offset + "%";
            positionTV.setText(newPositionText);
            saveSubtitleSetting("vertical_offset", offset);
        });
        
        positionDownBtn.setOnClickListener(v -> {
            int offset = prefs.getInt("vertical_offset", 0);
            offset -= 5; // Move down (decrease offset - negative values = closer to bottom edge)
            offset = Math.max(offset, -10); // Min -10% (very close to bottom edge)
            String newPositionText = offset == 0 ? "Giữa" : "Xuống " + offset + "%";
            positionTV.setText(newPositionText);
            saveSubtitleSetting("vertical_offset", offset);
        });
        
        backgroundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSubtitleSetting("background", isChecked);
        });
        
        // Text color controls
        textColorPrevBtn.setOnClickListener(v -> {
            int index = (prefs.getInt("text_color", 0) - 1 + colorNames.length) % colorNames.length;
            saveSubtitleSetting("text_color", index);
            textColorTV.setText(colorNames[index]);
            textColorTV.setTextColor(colorValues[index]);
        });
        
        textColorNextBtn.setOnClickListener(v -> {
            int index = (prefs.getInt("text_color", 0) + 1) % colorNames.length;
            saveSubtitleSetting("text_color", index);
            textColorTV.setText(colorNames[index]);
            textColorTV.setTextColor(colorValues[index]);
        });
        
        // Outline color controls
        outlineColorPrevBtn.setOnClickListener(v -> {
            int index = (prefs.getInt("outline_color", 1) - 1 + outlineColorNames.length) % outlineColorNames.length;
            saveSubtitleSetting("outline_color", index);
            outlineColorTV.setText(outlineColorNames[index]);
            // Show the actual color like text color does
            if (index == 0) {
                outlineColorTV.setTextColor(Color.GRAY); // Show gray for "None"
            } else {
                outlineColorTV.setTextColor(outlineColorValues[index]);
            }
        });
        
        outlineColorNextBtn.setOnClickListener(v -> {
            int index = (prefs.getInt("outline_color", 1) + 1) % outlineColorNames.length;
            saveSubtitleSetting("outline_color", index);
            outlineColorTV.setText(outlineColorNames[index]);
            // Show the actual color like text color does
            if (index == 0) {
                outlineColorTV.setTextColor(Color.GRAY); // Show gray for "None"
            } else {
                outlineColorTV.setTextColor(outlineColorValues[index]);
            }
        });
        
        // Playback speed controls
        View.OnClickListener speedClickListener = v -> {
            float speed = 1.0f;
            if (v == speed05) speed = 0.5f;
            else if (v == speed075) speed = 0.75f;
            else if (v == speed10) speed = 1.0f;
            else if (v == speed15) speed = 1.5f;
            else if (v == speed20) speed = 2.0f;
            else if (v == speed30) speed = 3.0f;
            
            // Reset all button backgrounds
            speed05.setBackgroundColor(Color.TRANSPARENT);
            speed075.setBackgroundColor(Color.TRANSPARENT);
            speed10.setBackgroundColor(Color.TRANSPARENT);
            speed15.setBackgroundColor(Color.TRANSPARENT);
            speed20.setBackgroundColor(Color.TRANSPARENT);
            speed30.setBackgroundColor(Color.TRANSPARENT);
            
            // Highlight selected button
            ((Button)v).setBackgroundColor(Color.GREEN);
            
            // Save speed
            saveSubtitleSetting("playback_speed", speed);
        };
        
        speed05.setOnClickListener(speedClickListener);
        speed075.setOnClickListener(speedClickListener);
        speed10.setOnClickListener(speedClickListener);
        speed15.setOnClickListener(speedClickListener);
        speed20.setOnClickListener(speedClickListener);
        speed30.setOnClickListener(speedClickListener);
        
        resetBtn.setOnClickListener(v -> {
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("font_size", 20);
            editor.putInt("font_type", 4); // Default to Vietnamese
            editor.putInt("vertical_offset", 0);
            editor.putBoolean("background", false);
            editor.putInt("text_color", 0); // White
            editor.putInt("outline_color", 1); // Black
            editor.putFloat("playback_speed", 1.0f); // Normal speed
            editor.apply();
            
            fontSizeTV.setText("20sp");
            fontTypeTV.setText(fontNames[4]); // Default to Vietnamese
            positionTV.setText("Giữa");
            backgroundSwitch.setChecked(false);
            textColorTV.setText(colorNames[0]);
            textColorTV.setTextColor(colorValues[0]);
            outlineColorTV.setText(outlineColorNames[1]);
            outlineColorTV.setTextColor(outlineColorValues[1]); // Show black color for reset
            
            Toast.makeText(HomeNewActivity.this, "Đã khôi phục cài đặt gốc", Toast.LENGTH_SHORT).show();
        });
        
        // Add layout to ScrollView and ScrollView to dialog
        scrollView.addView(layout);
        builder.setView(scrollView);
        builder.setPositiveButton("Đóng", (dialog, which) -> {
            dialog.dismiss();
            showQuickSettingsDialog(); // Re-open quick settings
        });
        builder.show();
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
}