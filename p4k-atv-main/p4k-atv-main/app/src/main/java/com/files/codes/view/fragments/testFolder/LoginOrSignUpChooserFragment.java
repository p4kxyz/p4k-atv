package com.files.codes.view.fragments.testFolder;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import com.files.codes.R;
import com.files.codes.view.LoginChooserActivity;
import com.files.codes.view.SignUpActivity;

import java.util.List;

public class LoginOrSignUpChooserFragment extends GuidedStepSupportFragment {
    private static final int ACTION_ID_POSITIVE = 1;
    private static final int ACTION_ID_NEGATIVE = ACTION_ID_POSITIVE + 1;

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(getResources().getString(R.string.login_or_signup_first) ,"", getResources().getString(R.string.app_name), null);
        return guidance;
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        GuidedAction action = new GuidedAction.Builder()
                .id(ACTION_ID_POSITIVE)
                .title(getString(R.string.login)).build();
        actions.add(action);
        action = new GuidedAction.Builder()
                .id(ACTION_ID_NEGATIVE)
                .title(getString(R.string.sign_up)).build();
        actions.add(action);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (ACTION_ID_POSITIVE == action.getId()){
            startActivity(new Intent(getContext(), LoginChooserActivity.class));
        }else {
            startActivity(new Intent(getContext(), SignUpActivity.class));
        }
    }
}
