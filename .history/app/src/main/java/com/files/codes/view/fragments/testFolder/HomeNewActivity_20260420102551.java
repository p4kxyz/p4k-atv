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
import android.view.ViewGroup;

public class HomeNewActivity extends FragmentActivity {
    
    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int REQUEST_INSTALL_PERMISSION = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_new);

        // Ensure left sidebar is fully hidden on entry to avoid black strip artifacts.
        forceCloseLeftSidebarIfPresent();
        
        // Settings Orb Logic
        setupSettingsOrb();
        setupSearchOrb(); // Initialize Search Orb
        setupFocusSearchListener();
        
        // CRITICAL: Add OTA update check to the correct home activity
        // Initialize app permissions and update check
        
        // Request permissions first, then check for updates
        requestAllPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-apply after returning from child activities in case layout state was interrupted.
        forceCloseLeftSidebarIfPresent();

        maybeShowFirstLaunchLanguageSetup();
    }

    private void maybeShowFirstLaunchLanguageSetup() {
        if (languageSetupDialogShowing || isFinishing()) return;

        android.content.SharedPreferences prefs = getSharedPreferences(PLAYER_PREFS, MODE_PRIVATE);
        if (prefs.getBoolean(PREF_LANG_SETUP_DONE, false)) return;

        languageSetupDialogShowing = true;
        new Handler().postDelayed(this::showFirstLaunchAudioLangDialog, 500);
    }

    private void showFirstLaunchAudioLangDialog() {
        final String[] names  = {"Không đặt (tự động)", "Tiếng Việt (vi)", "Tiếng Anh (en)",
                "Tiếng Nhật (ja)", "Tiếng Trung (zh)", "Tiếng Hàn (ko)",
                "Tiếng Thái (th)", "Tiếng Indonesia (id)", "Tiếng Tây Ban Nha (es)",
                "Tiếng Pháp (fr)", "Tiếng Đức (de)", "Tiếng Ý (it)",
                "Tiếng Bồ Đào Nha (pt)", "Tiếng Nga (ru)", "Tiếng Ả Rập (ar)",
                "Tiếng Hindi (hi)"};
        final String[] values = {"", "vi", "en", "ja", "zh", "ko", "th", "id", "es", "fr",
                "de", "it", "pt", "ru", "ar", "hi"};

        android.content.SharedPreferences prefs = getSharedPreferences(PLAYER_PREFS, MODE_PRIVATE);
        String cur = prefs.getString(PREF_AUDIO_LANG, "");
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (cur.equals(values[i])) {
                checked = i;
                break;
            }
        }
        final int finalChecked = checked;

        Runnable[] actions = new Runnable[names.length];
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            actions[i] = () -> {
                prefs.edit().putString(PREF_AUDIO_LANG, values[idx]).apply();
                showFirstLaunchSubtitleLangDialog();
            };
        }

        buildCustomListDialog(
                "🔊 Chọn ngôn ngữ âm thanh ưu tiên (Nếu xem thuyết minh để tiếng Việt)",
                names,
                finalChecked,
                actions,
                this::showFirstLaunchSubtitleLangDialog
        ).show();
    }

    private void showFirstLaunchSubtitleLangDialog() {
        final String[] names  = {"Không đặt (tự động)", "Tiếng Việt (vi)", "Tiếng Anh (en)",
                "Tiếng Nhật (ja)", "Tiếng Trung (zh)", "Tiếng Hàn (ko)",
                "Tiếng Thái (th)", "Tiếng Indonesia (id)", "Tiếng Tây Ban Nha (es)",
                "Tiếng Pháp (fr)", "Tiếng Đức (de)", "Tiếng Ý (it)",
                "Tiếng Bồ Đào Nha (pt)", "Tiếng Nga (ru)", "Tiếng Ả Rập (ar)",
                "Tiếng Hindi (hi)", "Tắt hoàn toàn"};
        final String[] values = {"", "vi", "en", "ja", "zh", "ko", "th", "id", "es", "fr",
                "de", "it", "pt", "ru", "ar", "hi", "off"};

        android.content.SharedPreferences prefs = getSharedPreferences(PLAYER_PREFS, MODE_PRIVATE);
        String cur = prefs.getString(PREF_SUBTITLE_LANG, "");
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (cur.equals(values[i])) {
                checked = i;
                break;
            }
        }
        final int finalChecked = checked;

        Runnable[] actions = new Runnable[names.length];
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            actions[i] = () -> {
                prefs.edit().putString(PREF_SUBTITLE_LANG, values[idx]).apply();
                finishFirstLaunchLanguageSetup();
            };
        }

        buildCustomListDialog(
                "📝 Thiết lập lần đầu: Chọn ngôn ngữ phụ đề ưu tiên",
                names,
                finalChecked,
                actions,
                this::finishFirstLaunchLanguageSetup
        ).show();
    }

    private void finishFirstLaunchLanguageSetup() {
        android.content.SharedPreferences prefs = getSharedPreferences(PLAYER_PREFS, MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_LANG_SETUP_DONE, true).apply();
        languageSetupDialogShowing = false;
        new com.files.codes.utils.ToastMsg(this).toastIconSuccess("✅ Đã lưu thiết lập ngôn ngữ ưu tiên");
    }

    private void forceCloseLeftSidebarIfPresent() {
        View headerContainer = findViewById(R.id.header_container);
        View rowsContainer = findViewById(R.id.rows_container);

        if (headerContainer != null) {
            ViewGroup.LayoutParams lp = headerContainer.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginLp = (ViewGroup.MarginLayoutParams) lp;
                int width = headerContainer.getWidth();
                if (width <= 0) {
                    width = Math.max(headerContainer.getMeasuredWidth(), dp(320));
                }
                marginLp.leftMargin = -width;
                headerContainer.setLayoutParams(marginLp);
            }
            headerContainer.clearAnimation();
            headerContainer.setVisibility(View.GONE);
        }

        if (rowsContainer != null) {
            ViewGroup.LayoutParams lp = rowsContainer.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginLp = (ViewGroup.MarginLayoutParams) lp;
                if (marginLp.leftMargin != 0) {
                    marginLp.leftMargin = 0;
                    rowsContainer.setLayoutParams(marginLp);
                }
            }
        }
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
                             return searchOrb; 
                        }
                    } 
                    
                    // 2. Navigation between Search Orb <-> Settings Orb (LEFT/RIGHT)
                    if (focused == searchOrb && direction == View.FOCUS_RIGHT) {
                         return settingsOrb;
                    }
                    if (focused == settingsOrb && direction == View.FOCUS_LEFT) {
                         return searchOrb;
                    }
                    
                    // 3. Navigation DOWN from Top Bar -> Content
                    if ((focused == searchOrb || focused == settingsOrb) && direction == View.FOCUS_DOWN) {
                         
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
    
    private static final String PLAYER_PREFS    = "player_settings";
    private static final String PREF_AUDIO_LANG    = "pref_audio_lang";
    private static final String PREF_SUBTITLE_LANG = "pref_subtitle_lang";
    private static final String PREF_LANG_SETUP_DONE = "pref_lang_setup_done";
    private boolean languageSetupDialogShowing = false;

    // -----------------------------------------------------------------------
    // Custom dialog builder – bypasses AlertDialog list rendering
    // (fixes blank list bug on Bonfire OS / JMGO and similar custom Android TV OS)
    // -----------------------------------------------------------------------
    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private android.app.Dialog buildCustomListDialog(
            String title, String[] items, int checkedIndex,
            Runnable[] actions, Runnable onCancel) {

        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        if (onCancel != null) {
            dialog.setOnCancelListener(d -> onCancel.run());
        }

        // Root container
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1E1E2E);
        root.setPadding(dp(20), dp(20), dp(20), dp(12));

        // Title
        android.widget.TextView titleView = new android.widget.TextView(this);
        titleView.setText(title);
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
        titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleView.setPadding(dp(4), 0, dp(4), dp(10));
        root.addView(titleView);

        // Thin divider
        android.view.View divider = new android.view.View(this);
        divider.setBackgroundColor(0x55FFFFFF);
        android.widget.LinearLayout.LayoutParams divLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divLp.bottomMargin = dp(6);
        root.addView(divider, divLp);

        // Scrollable item list
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.setVerticalScrollBarEnabled(true);
        sv.setScrollBarStyle(android.view.View.SCROLLBARS_INSIDE_INSET);
        sv.setSmoothScrollingEnabled(true);
        android.widget.LinearLayout ll = new android.widget.LinearLayout(this);
        ll.setOrientation(android.widget.LinearLayout.VERTICAL);

        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            boolean isSelected = (checkedIndex >= 0 && i == checkedIndex);

            android.widget.TextView tv = new android.widget.TextView(this);
            tv.setText(isSelected ? "✓  " + items[i] : "     " + items[i]);
            tv.setTextColor(isSelected ? 0xFF64B5F6 : 0xFFDDDDDD);
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
            tv.setPadding(dp(10), dp(13), dp(10), dp(13));
            tv.setFocusable(true);
            tv.setFocusableInTouchMode(false);
            tv.setClickable(true);
            tv.setBackground(null);

            // Focus highlight
            tv.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackgroundColor(0x664FC3F7);
                    ((android.widget.TextView) v).setTextColor(0xFFFFFFFF);
                    sv.post(() -> {
                        int targetY = Math.max(0, v.getTop() - dp(72));
                        sv.smoothScrollTo(0, targetY);
                    });
                } else {
                    v.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                    boolean sel = (checkedIndex >= 0 && idx == checkedIndex);
                    ((android.widget.TextView) v).setTextColor(sel ? 0xFF64B5F6 : 0xFFDDDDDD);
                }
            });

            // Click
            tv.setOnClickListener(v -> {
                dialog.dismiss();
                if (actions != null && idx < actions.length && actions[idx] != null) {
                    actions[idx].run();
                }
            });

            ll.addView(tv);

            // Separator
            android.view.View sep = new android.view.View(this);
            sep.setBackgroundColor(0x22FFFFFF);
            ll.addView(sep, new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1));
        }
        sv.addView(ll);

        android.widget.LinearLayout.LayoutParams svLp =
                new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(520));
        root.addView(sv, svLp);

        // Cancel button
        if (onCancel != null) {
            android.view.View btnDivider = new android.view.View(this);
            btnDivider.setBackgroundColor(0x33FFFFFF);
            android.widget.LinearLayout.LayoutParams bdLp =
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
            bdLp.topMargin = dp(6);
            root.addView(btnDivider, bdLp);

            android.widget.TextView cancelBtn = new android.widget.TextView(this);
            cancelBtn.setText("Hủy");
            cancelBtn.setTextColor(0xFF90CAF9);
            cancelBtn.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
            cancelBtn.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            cancelBtn.setPadding(dp(10), dp(12), dp(10), dp(4));
            cancelBtn.setFocusable(true);
            cancelBtn.setFocusableInTouchMode(false);
            cancelBtn.setClickable(true);
            cancelBtn.setBackground(null);
            cancelBtn.setGravity(android.view.Gravity.END);
            cancelBtn.setOnFocusChangeListener((v, hasFocus) ->
                    v.setBackgroundColor(hasFocus ? 0x33FFFFFF : android.graphics.Color.TRANSPARENT));
            cancelBtn.setOnClickListener(v -> { dialog.dismiss(); onCancel.run(); });
            android.widget.LinearLayout.LayoutParams cbLp =
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            root.addView(cancelBtn, cbLp);
        }

        dialog.setContentView(root);

        // Size the window
        android.view.Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout(dp(480), android.view.WindowManager.LayoutParams.WRAP_CONTENT);
            w.setGravity(android.view.Gravity.CENTER);
            w.setBackgroundDrawableResource(android.R.color.transparent);
        }

        return dialog;
    }

    // -----------------------------------------------------------------------
    // Quick-settings dialogs (rewritten to use buildCustomListDialog)
    // -----------------------------------------------------------------------

    private void showQuickSettingsDialog() {
        android.content.SharedPreferences playerPref = getSharedPreferences(PLAYER_PREFS, MODE_PRIVATE);
        String audioLang = playerPref.getString(PREF_AUDIO_LANG, "");
        String subLang   = playerPref.getString(PREF_SUBTITLE_LANG, "");
        boolean logoFixed = playerPref.getBoolean("logo_fixed", false);

        final String[] mainItems = {
            "🎞  Trình phát",
            "🔊 Giải mã âm thanh",
            "🔊 Ngôn ngữ âm thanh: " + getLangLabel(audioLang, false),
            "📝 Ngôn ngữ phụ đề: "   + getLangLabel(subLang, true),
            "🖼️ Logo player: " + (logoFixed ? "Cố định (góc trên phải)" : "Di chuyển (chống burn-in)"),
            "Đóng"
        };
        Runnable[] actions = {
            () -> showPlayerSelectionDialog(),
            () -> showAudioSelectionDialog(),
            () -> showAudioLangDialog(),
            () -> showSubtitleLangDialog(),
            () -> {
                boolean newVal = !logoFixed;
                playerPref.edit().putBoolean("logo_fixed", newVal).apply();
                new com.files.codes.utils.ToastMsg(this).toastIconSuccess(
                        newVal ? "Logo: Cố định góc trên phải" : "Logo: Di chuyển (chống burn-in)");
                showQuickSettingsDialog();
            },
            null   // Đóng – dialog đã dismiss sẵn
        };
        buildCustomListDialog("⚙️ Cài đặt nhanh", mainItems, -1, actions, null).show();
    }

    private String getLangLabel(String value, boolean isSub) {
        switch (value) {
            case "vi":  return "Tiếng Việt";
            case "en":  return "Tiếng Anh";
            case "ja":  return "Tiếng Nhật";
            case "zh":  return "Tiếng Trung";
            case "ko":  return "Tiếng Hàn";
            case "th":  return "Tiếng Thái";
            case "id":  return "Tiếng Indonesia";
            case "es":  return "Tiếng Tây Ban Nha";
            case "fr":  return "Tiếng Pháp";
            case "de":  return "Tiếng Đức";
            case "it":  return "Tiếng Ý";
            case "pt":  return "Tiếng Bồ Đào Nha";
            case "ru":  return "Tiếng Nga";
            case "ar":  return "Tiếng Ả Rập";
            case "hi":  return "Tiếng Hindi";
            case "off": return isSub ? "Tắt" : "Tự động";
            default:    return "Tự động";
        }
    }

    private void showAudioLangDialog() {
        final String[] names  = {"Không đặt (tự động)", "Tiếng Việt (vi)", "Tiếng Anh (en)",
                                  "Tiếng Nhật (ja)", "Tiếng Trung (zh)", "Tiếng Hàn (ko)",
                                  "Tiếng Thái (th)", "Tiếng Indonesia (id)", "Tiếng Tây Ban Nha (es)",
                                  "Tiếng Pháp (fr)", "Tiếng Đức (de)", "Tiếng Ý (it)",
                                  "Tiếng Bồ Đào Nha (pt)", "Tiếng Nga (ru)", "Tiếng Ả Rập (ar)",
                                  "Tiếng Hindi (hi)"};
        final String[] values = {"", "vi", "en", "ja", "zh", "ko", "th", "id", "es", "fr",
                                  "de", "it", "pt", "ru", "ar", "hi"};
        android.content.SharedPreferences prefs = getSharedPreferences(PLAYER_PREFS, MODE_PRIVATE);
        String cur = prefs.getString(PREF_AUDIO_LANG, "");
        int checked = 0;
        for (int i = 0; i < values.length; i++) if (cur.equals(values[i])) { checked = i; break; }
        final int finalChecked = checked;

        Runnable[] actions = new Runnable[names.length];
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            actions[i] = () -> {
                prefs.edit().putString(PREF_AUDIO_LANG, values[idx]).apply();
                new com.files.codes.utils.ToastMsg(this).toastIconSuccess("✅ Đã đặt: " + names[idx]);
                showQuickSettingsDialog();
            };
        }
        buildCustomListDialog("🔊 Ngôn ngữ âm thanh ưu tiên", names,
                finalChecked, actions, this::showQuickSettingsDialog).show();
    }

    private void showSubtitleLangDialog() {
        final String[] names  = {"Không đặt (tự động)", "Tiếng Việt (vi)", "Tiếng Anh (en)",
                                  "Tiếng Nhật (ja)", "Tiếng Trung (zh)", "Tiếng Hàn (ko)",
                                  "Tiếng Thái (th)", "Tiếng Indonesia (id)", "Tiếng Tây Ban Nha (es)",
                                  "Tiếng Pháp (fr)", "Tiếng Đức (de)", "Tiếng Ý (it)",
                                  "Tiếng Bồ Đào Nha (pt)", "Tiếng Nga (ru)", "Tiếng Ả Rập (ar)",
                                  "Tiếng Hindi (hi)", "Tắt hoàn toàn"};
        final String[] values = {"", "vi", "en", "ja", "zh", "ko", "th", "id", "es", "fr",
                                  "de", "it", "pt", "ru", "ar", "hi", "off"};
        android.content.SharedPreferences prefs = getSharedPreferences(PLAYER_PREFS, MODE_PRIVATE);
        String cur = prefs.getString(PREF_SUBTITLE_LANG, "");
        int checked = 0;
        for (int i = 0; i < values.length; i++) if (cur.equals(values[i])) { checked = i; break; }
        final int finalChecked = checked;

        Runnable[] actions = new Runnable[names.length];
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            actions[i] = () -> {
                prefs.edit().putString(PREF_SUBTITLE_LANG, values[idx]).apply();
                new com.files.codes.utils.ToastMsg(this).toastIconSuccess("✅ Đã đặt: " + names[idx]);
                showQuickSettingsDialog();
            };
        }
        buildCustomListDialog("📝 Ngôn ngữ phụ đề ưu tiên", names,
                finalChecked, actions, this::showQuickSettingsDialog).show();
    }

    private void showPlayerSelectionDialog() {
        final android.content.SharedPreferences pref =
                getSharedPreferences(com.files.codes.utils.Constants.USER_LOGIN_STATUS, MODE_PRIVATE);
        final boolean useExternal = pref.getBoolean("use_external_player", false);
        final String currentExternalPlayer = pref.getString("selected_player", "just");

        final String[] displayNames = {
            "ExoPlayer (Mặc định)",
            "Phim4K Player", "Kodi", "Just Player", "MX Player",
            "Vimu Player", "VLC Player", "nPlayer",
            "Dune HD (Realtek)", "Dune HD (Amlogic)",
            "Zidoo (Realtek)", "Zidoo (Amlogic)"
        };
        final String[] valueDetails = {
            "internal",
            "p4k", "kodi", "just", "mx", "vimu", "vlc", "nplayer",
            "dune_realtek", "dune_amlogic", "zidoo_realtek", "zidoo_amlogic"
        };

        int selectedIndex = 0;
        if (useExternal) {
            for (int i = 1; i < valueDetails.length; i++) {
                if (valueDetails[i].equals(currentExternalPlayer)) { selectedIndex = i; break; }
            }
            if (selectedIndex == 0) selectedIndex = 3;
        }
        final int finalSelected = selectedIndex;

        Runnable[] actions = new Runnable[displayNames.length];
        for (int i = 0; i < displayNames.length; i++) {
            final int idx = i;
            actions[i] = () -> {
                android.content.SharedPreferences.Editor editor = pref.edit();
                if (idx == 0) {
                    editor.putBoolean("use_external_player", false);
                } else {
                    editor.putBoolean("use_external_player", true);
                    editor.putString("selected_player", valueDetails[idx]);
                }
                editor.apply();
                new com.files.codes.utils.ToastMsg(this).toastIconSuccess("Đã chọn: " + displayNames[idx]);
                showQuickSettingsDialog();
            };
        }
        buildCustomListDialog("Chọn Trình phát", displayNames,
                finalSelected, actions, this::showQuickSettingsDialog).show();
    }

    private void showAudioSelectionDialog() {
        final android.content.SharedPreferences pref =
                getSharedPreferences(com.files.codes.utils.Constants.USER_LOGIN_STATUS, MODE_PRIVATE);
        int currentMode = pref.getInt("audio_priority_mode", -1);
        if (currentMode == -1) {
            currentMode = pref.getBoolean("audio_priority_sw", true) ? 1 : 0;
        }
        final int finalMode = currentMode;

        final String[] items = {
            "Phần cứng (Dành cho các thiết bị hỗ trợ đầy đủ codec)",
            "Phần mềm (Khi bạn bị lỗi mất tiếng hãy dùng)",
            "Passthrough (Dành cho dàn âm thanh ngoài)"
        };
        Runnable[] actions = new Runnable[items.length];
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            actions[i] = () -> {
                pref.edit().putInt("audio_priority_mode", idx)
                           .putBoolean("audio_priority_sw", idx == 1).apply();
                String label = idx == 0 ? "Hardware" : idx == 1 ? "Software" : "Passthrough";
                new com.files.codes.utils.ToastMsg(this).toastIconSuccess("Đã chọn: " + label);
                showQuickSettingsDialog();
            };
        }
        buildCustomListDialog("Giải mã Âm thanh", items,
                finalMode, actions, this::showQuickSettingsDialog).show();
    }
    
    /**
     * Request all necessary permissions for OTA updates
     */
    private void requestAllPermissions() {
        // Request all necessary permissions for OTA updates
        
        // Check storage permission first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
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
            startOTACheck();
            return;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                if (!getPackageManager().canRequestPackageInstalls()) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_INSTALL_PERMISSION);
                } else {
                    // All permissions granted, start OTA check
                    startOTACheck();
                }
            } catch (Exception e) {
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
        
        // Check for OTA updates after a short delay to let UI initialize
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkForUpdatesSilently();
            }
        }, 2000); // 2 second delay
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkInstallPermission();
            } else {
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
                } else {
                }
            }
            // Start OTA check regardless of permission result
            startOTACheck();
        }
    }
    
    /**
     * Check for OTA updates silently on startup (no toast if no update)
     */
    private void checkForUpdatesSilently() {
        try {
            OTAUpdateManager otaManager = OTAUpdateManager.getInstance(this);
            otaManager.checkForUpdatesSilently();
        } catch (Exception e) {
        }
    }

    /**
     * Check for OTA updates when app starts
     */
    private void checkForUpdates() {
        try {
            OTAUpdateManager otaManager = OTAUpdateManager.getInstance(this);
            
            otaManager.checkForUpdates();
        } catch (Exception e) {
            if (e.getCause() != null) {
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
            }
        } catch (Exception e) {
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
        }
        return null;
    }
}
