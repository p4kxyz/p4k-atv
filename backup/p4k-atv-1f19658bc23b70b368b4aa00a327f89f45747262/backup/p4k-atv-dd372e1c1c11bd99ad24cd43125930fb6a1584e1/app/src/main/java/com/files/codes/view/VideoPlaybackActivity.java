package com.files.codes.view;


import android.app.Activity;
import android.os.Bundle;

import com.files.codes.R;

public class VideoPlaybackActivity extends Activity {
    public static final String TAG = "VideoExampleActivity";
    private static final int MAKE_BROWSABLE_REQUEST_CODE = 9001;
    public static final String EXTRA_VIDEO = "com.oxootv.spagreen.recommendations.extra.MOVIE";
    public static final String EXTRA_CHANNEL_ID =
            "com.oxootv.spagreen.recommendations.extra.CHANNEL_ID";
    public static final String EXTRA_POSITION =
            "com.oxootv.spagreen.recommendations.extra.POSITION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_playback);
    }
}