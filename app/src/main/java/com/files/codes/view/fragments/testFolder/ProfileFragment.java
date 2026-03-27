package com.files.codes.view.fragments.testFolder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import com.files.codes.R;
import com.files.codes.database.DatabaseHelper;
import com.files.codes.model.subscription.ActiveStatus;
import com.files.codes.utils.Constants;
import com.files.codes.utils.PreferenceUtils;
import com.files.codes.view.LoginChooserActivity;
import com.files.codes.view.OTAUpdateManager;

import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class ProfileFragment extends GuidedStepSupportFragment {
    private static final int ACTION_ID_SIGN_OUT = 1;
    private static final int ACTION_ID_EXTERNAL_PLAYER = 2;
    private static final int ACTION_ID_CHOOSE_PLAYER = 3;
    private static final int ACTION_ID_LOGIN = 4;
    private static final int ACTION_ID_CHECK_UPDATE = 5;
    private static final int ACTION_ID_AUDIO_LANG = 6;
    private static final int ACTION_ID_SUBTITLE_LANG = 7;
    private static final String PREF_USE_EXTERNAL_PLAYER = "use_external_player";
    private static final String PREF_SELECTED_PLAYER = "selected_player";
    private static final String PLAYER_PREFS = "player_settings";
    private static final String PREF_AUDIO_LANG = "pref_audio_lang";
    private static final String PREF_SUBTITLE_LANG = "pref_subtitle_lang";
    private DatabaseHelper db;

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        GuidanceStylist.Guidance guidance;
        db = new DatabaseHelper(getActivity());
        if (PreferenceUtils.isLoggedIn(getContext())) {
            ActiveStatus activeStatus = db.getActiveStatusData();
            String packageTitle = "Package: Free";
            String expireDate = "Valid till: No limit";
            
            if (activeStatus != null) {
                packageTitle = "Package: " + activeStatus.getPackageTitle();
                expireDate = "Valid till: " + activeStatus.getExpireDate();
            }
            
            String des = packageTitle + "\n" + expireDate;
            guidance = new GuidanceStylist.Guidance(db.getUserData().getName(), des, db.getUserData().getEmail(), null);
        } else {
            // Show account options even when not logged in
            guidance = new GuidanceStylist.Guidance("Tài khoản", "Cài đặt trình phát và tùy chọn khác", "Chưa đăng nhập", null);
        }

        return guidance;
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        SharedPreferences prefs = getContext().getSharedPreferences(Constants.USER_LOGIN_STATUS, MODE_PRIVATE);
        boolean useExternalPlayer = prefs.getBoolean(PREF_USE_EXTERNAL_PLAYER, false);
        String selectedPlayer = prefs.getString(PREF_SELECTED_PLAYER, "just"); // Default: just player
        
        // External Player Toggle
        GuidedAction externalPlayerAction = new GuidedAction.Builder(getActivity())
                .id(ACTION_ID_EXTERNAL_PLAYER)
                .title("⚙️ " + getResources().getString(R.string.use_external_player))
                .description(getResources().getString(R.string.external_player_description))
                .checkSetId(GuidedAction.DEFAULT_CHECK_SET_ID)
                .checked(useExternalPlayer)
                .build();
        actions.add(externalPlayerAction);
        
        // Choose Player Action (only show if external player is enabled)
        if (useExternalPlayer) {
            String playerName = getPlayerDisplayName(selectedPlayer);
            GuidedAction choosePlayerAction = new GuidedAction.Builder(getActivity())
                    .id(ACTION_ID_CHOOSE_PLAYER)
                    .title("📱 " + getResources().getString(R.string.choose_player))
                    .description("Hiện tại: " + playerName)
                    .build();
            actions.add(choosePlayerAction);
        }
        
        // Audio language preference
        String prefAudioLang = getContext().getSharedPreferences(PLAYER_PREFS, MODE_PRIVATE)
                .getString(PREF_AUDIO_LANG, "");
        GuidedAction audioLangAction = new GuidedAction.Builder(getActivity())
                .id(ACTION_ID_AUDIO_LANG)
                .title("🔊 Ngôn ngữ âm thanh ưu tiên")
                .description(getAudioLangDisplayName(prefAudioLang))
                .editable(false)
                .build();
        actions.add(audioLangAction);

        // Subtitle language preference
        String prefSubLang = getContext().getSharedPreferences(PLAYER_PREFS, MODE_PRIVATE)
                .getString(PREF_SUBTITLE_LANG, "");
        GuidedAction subLangAction = new GuidedAction.Builder(getActivity())
                .id(ACTION_ID_SUBTITLE_LANG)
                .title("📝 Ngôn ngữ phụ đề ưu tiên")
                .description(getSubtitleLangDisplayName(prefSubLang))
                .editable(false)
                .build();
        actions.add(subLangAction);

        // Check for Updates Action (always visible)
        String verName = "";
        try {
            verName = getContext().getPackageManager()
                    .getPackageInfo(getContext().getPackageName(), 0).versionName;
        } catch (Exception ignored) {}
        GuidedAction checkUpdateAction = new GuidedAction.Builder(getActivity())
                .id(ACTION_ID_CHECK_UPDATE)
                .title("🔄 Kiểm tra cập nhật")
                .description("Phiên bản hiện tại: v" + verName)
                .editable(false)
                .build();
        actions.add(checkUpdateAction);

        // Sign Out or Login Action
        if (PreferenceUtils.isLoggedIn(getContext())) {
            GuidedAction signOutAction = new GuidedAction.Builder(getActivity())
                    .id(ACTION_ID_SIGN_OUT)
                    .title(getResources().getString(R.string.signout))
                    .editable(false)
                    .build();
            actions.add(signOutAction);
        } else {
            // Add Login action when not logged in
            GuidedAction loginAction = new GuidedAction.Builder(getActivity())
                    .id(ACTION_ID_LOGIN)
                    .title("🔑 Đăng nhập")
                    .description("Đăng nhập để sử dụng đầy đủ tính năng")
                    .editable(false)
                    .build();
            actions.add(loginAction);
        }

    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == ACTION_ID_EXTERNAL_PLAYER) {
            // Get current state and toggle it
            SharedPreferences prefs = getContext().getSharedPreferences(Constants.USER_LOGIN_STATUS, MODE_PRIVATE);
            boolean currentValue = prefs.getBoolean(PREF_USE_EXTERNAL_PLAYER, false);
            boolean newValue = !currentValue;
            
            // Save the new value
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(PREF_USE_EXTERNAL_PLAYER, newValue);
            editor.apply();
            
            // Show toast message
            String message = newValue ? "✅ Đã bật trình phát ngoài" : "❌ Đã tắt trình phát ngoài";
            android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
            
            // Refresh actions to show/hide player choice immediately
            List<GuidedAction> newActions = new java.util.ArrayList<>();
            onCreateActions(newActions, null);
            setActions(newActions);
            
        } else if (action.getId() == ACTION_ID_CHOOSE_PLAYER) {
            // Show player selection dialog
            showPlayerSelectionDialog();
            
        } else if (action.getId() == ACTION_ID_AUDIO_LANG) {
            showAudioLangDialog();
        } else if (action.getId() == ACTION_ID_SUBTITLE_LANG) {
            showSubtitleLangDialog();
        } else if (action.getId() == ACTION_ID_CHECK_UPDATE) {
            android.widget.Toast.makeText(getContext(), "Đang kiểm tra cập nhật...", android.widget.Toast.LENGTH_SHORT).show();
            OTAUpdateManager.getInstance(getContext()).checkForUpdates();
        } else if (action.getId() == ACTION_ID_SIGN_OUT) {
            GuidedStepSupportFragment fragment = new SignOutFragment();
            add(getFragmentManager(), fragment);
        } else if (action.getId() == ACTION_ID_LOGIN) {
            // Launch login activity
            Intent intent = new Intent(getContext(), LoginChooserActivity.class);
            startActivity(intent);
        }
    }
    
    // -----------------------------------------------------------------------
    // Custom dialog builder – bypasses AlertDialog list rendering
    // (fixes blank list on Bonfire OS / JMGO)
    // -----------------------------------------------------------------------
    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private android.app.Dialog buildCustomListDialog(
            String title, String[] items, int checkedIndex,
            Runnable[] actions, Runnable onCancel) {

        android.app.Dialog dialog = new android.app.Dialog(getActivity());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        if (onCancel != null) dialog.setOnCancelListener(d -> onCancel.run());

        android.widget.LinearLayout root = new android.widget.LinearLayout(getActivity());
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1E1E2E);
        root.setPadding(dp(20), dp(20), dp(20), dp(12));

        android.widget.TextView titleView = new android.widget.TextView(getActivity());
        titleView.setText(title);
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
        titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleView.setPadding(dp(4), 0, dp(4), dp(10));
        root.addView(titleView);

        android.view.View divider = new android.view.View(getActivity());
        divider.setBackgroundColor(0x55FFFFFF);
        android.widget.LinearLayout.LayoutParams divLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divLp.bottomMargin = dp(6);
        root.addView(divider, divLp);

        android.widget.ScrollView sv = new android.widget.ScrollView(getActivity());
        sv.setVerticalScrollBarEnabled(false);
        android.widget.LinearLayout ll = new android.widget.LinearLayout(getActivity());
        ll.setOrientation(android.widget.LinearLayout.VERTICAL);

        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            boolean isSelected = (checkedIndex >= 0 && i == checkedIndex);

            android.widget.TextView tv = new android.widget.TextView(getActivity());
            tv.setText(isSelected ? "✓  " + items[i] : "     " + items[i]);
            tv.setTextColor(isSelected ? 0xFF64B5F6 : 0xFFDDDDDD);
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
            tv.setPadding(dp(10), dp(13), dp(10), dp(13));
            tv.setFocusable(true);
            tv.setFocusableInTouchMode(false);
            tv.setClickable(true);
            tv.setBackground(null);
            tv.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackgroundColor(0x664FC3F7);
                    ((android.widget.TextView) v).setTextColor(0xFFFFFFFF);
                } else {
                    v.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                    boolean sel = (checkedIndex >= 0 && idx == checkedIndex);
                    ((android.widget.TextView) v).setTextColor(sel ? 0xFF64B5F6 : 0xFFDDDDDD);
                }
            });
            tv.setOnClickListener(v -> {
                dialog.dismiss();
                if (actions != null && idx < actions.length && actions[idx] != null)
                    actions[idx].run();
            });
            ll.addView(tv);

            android.view.View sep = new android.view.View(getActivity());
            sep.setBackgroundColor(0x22FFFFFF);
            ll.addView(sep, new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1));
        }
        sv.addView(ll);
        root.addView(sv, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(360)));

        if (onCancel != null) {
            android.view.View btnDiv = new android.view.View(getActivity());
            btnDiv.setBackgroundColor(0x33FFFFFF);
            android.widget.LinearLayout.LayoutParams bdLp =
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
            bdLp.topMargin = dp(6);
            root.addView(btnDiv, bdLp);

            android.widget.TextView cancelBtn = new android.widget.TextView(getActivity());
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
            root.addView(cancelBtn, new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        dialog.setContentView(root);
        android.view.Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout(dp(480), android.view.WindowManager.LayoutParams.WRAP_CONTENT);
            w.setGravity(android.view.Gravity.CENTER);
            w.setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    private void refreshActions() {
        List<GuidedAction> newActions = new java.util.ArrayList<>();
        onCreateActions(newActions, null);
        setActions(newActions);
    }

    private void showAudioLangDialog() {
        final String[] displayNames = {
            "Không đặt (tự động)", "Tiếng Việt (vi)", "Tiếng Anh (en)",
            "Tiếng Nhật (ja)", "Tiếng Trung (zh)", "Tiếng Hàn (ko)"
        };
        final String[] values = {"", "vi", "en", "ja", "zh", "ko"};

        android.content.SharedPreferences prefs =
                getContext().getSharedPreferences(PLAYER_PREFS, MODE_PRIVATE);
        String current = prefs.getString(PREF_AUDIO_LANG, "");
        int checked = 0;
        for (int i = 0; i < values.length; i++) if (current.equals(values[i])) { checked = i; break; }

        Runnable[] actions = new Runnable[displayNames.length];
        for (int i = 0; i < displayNames.length; i++) {
            final int idx = i;
            actions[i] = () -> {
                prefs.edit().putString(PREF_AUDIO_LANG, values[idx]).apply();
                android.widget.Toast.makeText(getContext(),
                        "✅ Đã đặt: " + displayNames[idx], android.widget.Toast.LENGTH_SHORT).show();
                refreshActions();
            };
        }
        buildCustomListDialog("🔊 Ngôn ngữ âm thanh ưu tiên",
                displayNames, checked, actions, null).show();
    }

    private void showSubtitleLangDialog() {
        final String[] displayNames = {
            "Không đặt (tự động)", "Tiếng Việt (vi)", "Tiếng Anh (en)",
            "Tiếng Nhật (ja)", "Tiếng Trung (zh)", "Tiếng Hàn (ko)", "Tắt hoàn toàn"
        };
        final String[] values = {"", "vi", "en", "ja", "zh", "ko", "off"};

        android.content.SharedPreferences prefs =
                getContext().getSharedPreferences(PLAYER_PREFS, MODE_PRIVATE);
        String current = prefs.getString(PREF_SUBTITLE_LANG, "");
        int checked = 0;
        for (int i = 0; i < values.length; i++) if (current.equals(values[i])) { checked = i; break; }

        Runnable[] actions = new Runnable[displayNames.length];
        for (int i = 0; i < displayNames.length; i++) {
            final int idx = i;
            actions[i] = () -> {
                prefs.edit().putString(PREF_SUBTITLE_LANG, values[idx]).apply();
                android.widget.Toast.makeText(getContext(),
                        "✅ Đã đặt: " + displayNames[idx], android.widget.Toast.LENGTH_SHORT).show();
                refreshActions();
            };
        }
        buildCustomListDialog("📝 Ngôn ngữ phụ đề ưu tiên",
                displayNames, checked, actions, null).show();
    }

    private String getAudioLangDisplayName(String value) {
        switch (value) {
            case "vi": return "Tiếng Việt (vi)";
            case "en": return "Tiếng Anh (en)";
            case "ja": return "Tiếng Nhật (ja)";
            case "zh": return "Tiếng Trung (zh)";
            case "ko": return "Tiếng Hàn (ko)";
            default:   return "Không đặt (tự động)";
        }
    }

    private String getSubtitleLangDisplayName(String value) {
        switch (value) {
            case "vi":  return "Tiếng Việt (vi)";
            case "en":  return "Tiếng Anh (en)";
            case "ja":  return "Tiếng Nhật (ja)";
            case "zh":  return "Tiếng Trung (zh)";
            case "ko":  return "Tiếng Hàn (ko)";
            case "off": return "Tắt hoàn toàn";
            default:    return "Không đặt (tự động)";
        }
    }

    private void showPlayerSelectionDialog() {
        SharedPreferences prefs = getContext().getSharedPreferences(Constants.USER_LOGIN_STATUS, MODE_PRIVATE);
        String currentPlayer = prefs.getString(PREF_SELECTED_PLAYER, "just");

        final String[] players = {
            getString(R.string.p4k_player),
            getString(R.string.kodi_player),
            getString(R.string.just_player),
            getString(R.string.mx_player),
            "Vimu Player",
            getString(R.string.vlc_player),
            getString(R.string.nplayer),
            getString(R.string.dune_hd_realtek),
            getString(R.string.dune_hd_amlogic),
            getString(R.string.zidoo_realtek),
            getString(R.string.zidoo_amlogic)
        };
        final String[] playerValues = {
            "p4k", "kodi", "just", "mx", "vimu", "vlc", "nplayer",
            "dune_realtek", "dune_amlogic", "zidoo_realtek", "zidoo_amlogic"
        };

        int checked = 0;
        for (int i = 0; i < playerValues.length; i++) {
            if (currentPlayer.equals(playerValues[i])) { checked = i; break; }
        }

        Runnable[] actions = new Runnable[players.length];
        for (int i = 0; i < players.length; i++) {
            final int idx = i;
            actions[i] = () -> {
                prefs.edit().putString(PREF_SELECTED_PLAYER, playerValues[idx]).apply();
                android.widget.Toast.makeText(getContext(),
                        "✅ Đã chọn " + players[idx], android.widget.Toast.LENGTH_SHORT).show();
                refreshActions();
            };
        }
        buildCustomListDialog(getString(R.string.choose_player),
                players, checked, actions, null).show();
    }
    
    // Helper method to get player display name
    private String getPlayerDisplayName(String playerValue) {
        switch (playerValue) {
            case "p4k":
                return getString(R.string.p4k_player);
            case "kodi":
                return getString(R.string.kodi_player);
            case "just":
                return getString(R.string.just_player);
            case "mx":
                return getString(R.string.mx_player);
            case "vimu":
                return "Vimu Player";
            case "vlc":
                return getString(R.string.vlc_player);
            case "nplayer":
                return getString(R.string.nplayer);
            case "dune_realtek":
                return getString(R.string.dune_hd_realtek);
            case "dune_amlogic":
                return getString(R.string.dune_hd_amlogic);
            case "zidoo_realtek":
                return getString(R.string.zidoo_realtek);
            case "zidoo_amlogic":
                return getString(R.string.zidoo_amlogic);
            default:
                return getString(R.string.just_player);
        }
    }
    
    // Helper method for PlayerActivity to check preference
    public static boolean shouldUseExternalPlayer(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.USER_LOGIN_STATUS, MODE_PRIVATE);
        return prefs.getBoolean(PREF_USE_EXTERNAL_PLAYER, false);
    }


}
