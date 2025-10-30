package com.files.codes.view.fragments.testFolder;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.GuidedStepSupportFragment;

import com.files.codes.utils.PreferenceUtils;

public class ProfileActivity extends FragmentActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            if (PreferenceUtils.isLoggedIn(this)) {
                GuidedStepSupportFragment fragment = new ProfileFragment();
                GuidedStepSupportFragment.addAsRoot(this, fragment, android.R.id.content);
            }else {
                GuidedStepSupportFragment fragment = new LoginOrSignUpChooserFragment();
                GuidedStepSupportFragment.addAsRoot(this, fragment, android.R.id.content);
            }
        }
    }
}
