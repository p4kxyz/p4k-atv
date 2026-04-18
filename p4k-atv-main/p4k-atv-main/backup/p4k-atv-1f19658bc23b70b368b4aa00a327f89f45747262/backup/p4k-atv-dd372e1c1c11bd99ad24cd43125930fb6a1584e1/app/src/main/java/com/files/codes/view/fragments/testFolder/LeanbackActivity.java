package com.files.codes.view.fragments.testFolder;

import android.content.Intent;

import androidx.fragment.app.FragmentActivity;

import com.files.codes.view.SearchActivity;

public class LeanbackActivity extends FragmentActivity {
    @Override
    public boolean onSearchRequested() {
        startActivity(new Intent(this, SearchActivity.class));
        return true;
    }
}
