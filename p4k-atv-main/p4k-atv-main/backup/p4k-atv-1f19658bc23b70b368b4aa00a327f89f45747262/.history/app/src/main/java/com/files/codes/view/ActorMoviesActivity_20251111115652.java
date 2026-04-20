package com.files.codes.view;

import android.os.Bundle;

import androidx.leanback.app.BrowseActivity;

import com.files.codes.R;
import com.files.codes.view.fragments.ActorMoviesFragment;

public class ActorMoviesActivity extends FragmentActivity implements BrowseSupportFragment.FragmentHost {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actor_movies);
        
        String actorName = getIntent().getStringExtra("actor_name");
        
        if (savedInstanceState == null) {
            ActorMoviesFragment fragment = ActorMoviesFragment.newInstance(actorName);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.actor_movies_fragment, fragment)
                    .commit();
        }
    }

    @Override
    public void notifyViewCreated(androidx.leanback.app.BrowseSupportFragment fragment) {
        // Required by BrowseSupportFragment.FragmentHost
    }
}