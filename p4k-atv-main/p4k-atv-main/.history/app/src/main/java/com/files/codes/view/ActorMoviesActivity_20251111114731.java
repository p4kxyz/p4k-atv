package com.files.codes.view;

import android.app.Activity;
import android.os.Bundle;

import com.files.codes.R;
import com.files.codes.view.fragments.ActorMoviesFragment;

public class ActorMoviesActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actor_movies);
        
        String actorName = getIntent().getStringExtra("actor_name");
        
        if (savedInstanceState == null) {
            ActorMoviesFragment fragment = ActorMoviesFragment.newInstance(actorName);
            getFragmentManager().beginTransaction()
                    .replace(R.id.actor_movies_fragment, fragment)
                    .commit();
        }
    }
}