package com.files.codes.view;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.files.codes.R;
import com.files.codes.view.fragments.ActorMoviesFragment;

public class ActorMoviesActivity extends FragmentActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        String actorName = getIntent().getStringExtra("actor_name");
        
        // Create fragment programmatically to avoid layout inflation issues
        ActorMoviesFragment fragment = ActorMoviesFragment.newInstance(actorName);
        getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, fragment)
                .commit();
    }
}