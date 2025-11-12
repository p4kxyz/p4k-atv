package com.files.codes.view.fragments.testFolder;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.files.codes.R;
import com.files.codes.database.DatabaseHelper;
import com.files.codes.utils.Constants;
import com.files.codes.utils.PreferenceUtils;
import com.files.codes.view.fragments.WatchHistoryFragment;

import java.util.List;

public class SignOutFragment extends GuidedStepSupportFragment {
    private static final int ACTION_ID_SIGN_OUT = 1;

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(getResources().getString(R.string.want_to_sign_out), "", "",null);
        return guidance;
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        GuidedAction action = new GuidedAction.Builder(getActivity())
                .id(ACTION_ID_SIGN_OUT)
                .title(getResources().getString(R.string.signout))

                .build();
        actions.add(action);

        GuidedAction actionCancel = new GuidedAction.Builder(getActivity())
                .id(GuidedAction.ACTION_ID_CANCEL)
                .title(android.R.string.cancel)
                .build();
        actions.add(actionCancel);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == ACTION_ID_SIGN_OUT){
            //signout
            signOut();
        }else if(action.getId() == GuidedAction.ACTION_ID_CANCEL){
            getFragmentManager().popBackStackImmediate();
        }
    }

    private void signOut() {
        DatabaseHelper databaseHelper = new DatabaseHelper(getContext());
        String userId = databaseHelper.getUserData().getUserId();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseAuth.getInstance().signOut();
        }
        if (userId != null) {
            SharedPreferences.Editor editor = getContext().getSharedPreferences(Constants.USER_LOGIN_STATUS, MODE_PRIVATE).edit();
            editor.putBoolean(Constants.USER_LOGIN_STATUS, false);
            editor.apply();

            databaseHelper.deleteUserData();
            PreferenceUtils.clearSubscriptionSavedData(getContext());
            
            // CRITICAL: Clear watch history cache to prevent user data leakage
            WatchHistoryFragment.clearWatchHistoryCache(getContext());

            startActivity(new Intent(getContext(), HomeNewActivity.class));
            getActivity().finish();
        }
    }
}
