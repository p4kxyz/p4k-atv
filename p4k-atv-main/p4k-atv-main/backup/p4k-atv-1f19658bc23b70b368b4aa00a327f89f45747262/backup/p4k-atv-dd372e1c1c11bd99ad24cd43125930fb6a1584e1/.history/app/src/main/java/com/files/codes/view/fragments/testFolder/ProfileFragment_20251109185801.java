package com.files.codes.view.fragments.testFolder;

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

import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class ProfileFragment extends GuidedStepSupportFragment {
    private static final int ACTION_ID_SIGN_OUT = 1;
    private static final int ACTION_ID_EXTERNAL_PLAYER = 2;
    private static final int ACTION_ID_CHOOSE_PLAYER = 3;
    private static final String PREF_USE_EXTERNAL_PLAYER = "use_external_player";
    private static final String PREF_SELECTED_PLAYER = "selected_player";
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
            guidance = new GuidanceStylist.Guidance(getResources().getString(R.string.something_went_wrong), "", "", null);
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
        
        // Sign Out Action
        GuidedAction signOutAction = new GuidedAction.Builder(getActivity())
                .id(ACTION_ID_SIGN_OUT)
                .title(getResources().getString(R.string.signout))
                .editable(false)
                .build();
        actions.add(signOutAction);

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
            
        } else if (action.getId() == ACTION_ID_SIGN_OUT) {
            GuidedStepSupportFragment fragment = new SignOutFragment();
            add(getFragmentManager(), fragment);
        }
    }
    
    private void showPlayerSelectionDialog() {
        SharedPreferences prefs = getContext().getSharedPreferences(Constants.USER_LOGIN_STATUS, MODE_PRIVATE);
        String currentPlayer = prefs.getString(PREF_SELECTED_PLAYER, "just");
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.choose_player);
        
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
        final String[] playerValues = {"p4k", "kodi", "just", "mx", "vimu", "vlc", "nplayer", "dune_realtek", "dune_amlogic", "zidoo_realtek", "zidoo_amlogic"};
        
        int checkedItem = 0;
        for (int i = 0; i < playerValues.length; i++) {
            if (currentPlayer.equals(playerValues[i])) {
                checkedItem = i;
                break;
            }
        }
        
        builder.setSingleChoiceItems(players, checkedItem, (dialog, which) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_SELECTED_PLAYER, playerValues[which]);
            editor.apply();
            
            String message = "✅ Đã chọn " + players[which];
            android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
            
            dialog.dismiss();
            
            // Refresh actions to update description immediately
            List<GuidedAction> newActions = new java.util.ArrayList<>();
            onCreateActions(newActions, null);
            setActions(newActions);
        });
        
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
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
        // Chỉ cho phép external player khi user đã đăng nhập
        if (!com.files.codes.utils.PreferenceUtils.isLoggedIn(context)) {
            return false;
        }
        
        SharedPreferences prefs = context.getSharedPreferences(Constants.USER_LOGIN_STATUS, MODE_PRIVATE);
        return prefs.getBoolean(PREF_USE_EXTERNAL_PLAYER, false);
    }


}
