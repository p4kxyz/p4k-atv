package com.files.codes.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.files.codes.R;
import com.files.codes.view.fragments.HeroStyleMovieDetailsFragment;

public class HeroStyleVideoDetailsActivity extends FragmentActivity {
    
    private static final String TAG = "HeroStyleVideoDetails";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hero_style_video_details);
        
        // Get data from intent
        Intent intent = getIntent();
        String videoId = intent.getStringExtra("id");
        String videoType = intent.getStringExtra("type");
        
        Log.d(TAG, "Opening hero-style details for ID: " + videoId + ", Type: " + videoType);
        
        // Create and add fragment
        if (savedInstanceState == null) {
            HeroStyleMovieDetailsFragment fragment = HeroStyleMovieDetailsFragment.newInstance(videoId, videoType);
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        }
    }
}