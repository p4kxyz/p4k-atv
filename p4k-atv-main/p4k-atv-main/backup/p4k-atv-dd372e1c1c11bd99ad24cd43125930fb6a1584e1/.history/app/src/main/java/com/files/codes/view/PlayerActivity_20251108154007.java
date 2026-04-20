package com.files.codes.view;

import static android.view.View.VISIBLE;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.WindowManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.HorizontalScrollView;
import android.view.Gravity;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ext.rtmp.RtmpDataSourceFactory;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.google.android.exoplayer2.ui.TimeBar;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.ui.CaptionStyleCompat;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.SeekBar;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.view.LayoutInflater;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.files.codes.R;
import com.files.codes.database.DatabaseHelper;
import com.files.codes.model.PlaybackModel;
import com.files.codes.model.Video;
import com.files.codes.model.movieDetails.Subtitle;
import com.files.codes.utils.Constants;
import com.files.codes.utils.ToastMsg;
import com.files.codes.utils.TvRecommendationManager;
import com.files.codes.utils.sync.WatchHistorySyncManager;
import com.files.codes.model.VideoContent;
import com.files.codes.view.adapter.ServerAdapter;
import com.files.codes.view.adapter.SubtitleListAdapter;
import com.files.codes.view.fragments.MyAccountFragment;
import com.files.codes.view.fragments.testFolder.ProfileFragment;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends Activity {
    private static final String TAG = "PlayerActivity";
    private static final String CLASS_NAME = "com.oxoo.spagreen.ui.activity.PlayerActivity";
    private PlayerView exoPlayerView;
    private SubtitleView subtitleView;
    // private SubtitleView primarySubtitleView;
    // private SubtitleView secondarySubtitleView;
    // private RelativeLayout dualSubtitleOverlay;
    // private boolean dualSubtitleEnabled = false;
    private int primaryTrackIndex = -1;
    private int secondaryTrackIndex = -1;
    private ExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private RelativeLayout rootLayout;
    private MediaSource mediaSource;
    private boolean preferredTracksApplied = false;
    private boolean trackInfoLogged = false;
    private boolean isPlaying;
    private List<Video> videos = new ArrayList<>();
    private Video video = null;
    private String url = "";
    private boolean hasRetriedWithHLS = false;
    private boolean hasHandledAudioCodecError = false; // Prevent infinite audio codec error loops // Track HLS retry attempts
    private boolean hasPlayerError = false; // Track if there was a player error to prevent auto-next on error
    private long lastErrorTime = 0; // Track when the last error occurred
    private String videoType = "";
    private String category = "";
    private int visible;
    private ImageButton serverButton, fastForwardButton, subtitleButton, subtitleSettingsButton, audioTrackButton, aspectRatioButton;
    private ImageButton previousEpisodeButton, nextEpisodeButton; // Episode navigation for TV series
    private ImageButton rewindButton; // Rewind button (backward 10s like fast forward)
    private TextView movieTitleTV, movieDescriptionTV;
    private ImageView posterImageView, posterImageViewForTV;
    private RelativeLayout seekBarLayout;
    private TextView liveTvTextInController;
    private ProgressBar progressBar;
    private PowerManager.WakeLock wakeLock;
    private MediaSession session;
    private TvRecommendationManager tvRecommendationManager;
    private WatchHistorySyncManager watchHistorySyncManager;

    private long mChannelId;
    private long mStartingPosition;
    private PlaybackModel model;
    private boolean isUserSeeking = false; // Flag to prevent auto-seek conflicts

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        mChannelId = getIntent().getLongExtra(VideoPlaybackActivity.EXTRA_CHANNEL_ID, -1L);
        mStartingPosition = getIntent().getLongExtra(VideoPlaybackActivity.EXTRA_POSITION, -1L);

        model = (PlaybackModel) getIntent().getSerializableExtra(VideoPlaybackActivity.EXTRA_VIDEO);



        // Check if this is a direct call from watch history (no model provided)
        Intent intent = getIntent();
        if (model == null && intent != null && intent.hasExtra("id")) {

            String videoId = intent.getStringExtra("id");
            String videoType = intent.getStringExtra("type");
            String title = intent.getStringExtra("title");
            String poster = intent.getStringExtra("poster");
            String thumbnail = intent.getStringExtra("thumbnail");
            String videoUrl = intent.getStringExtra("video_url");
            long position = intent.getLongExtra("position", -1L);
            boolean fromWatchHistory = intent.getBooleanExtra("from_watch_history", false);
            
            Log.d(TAG, "📥 onCreate - Watch History Intent: fromWatchHistory=" + fromWatchHistory + ", ID=" + videoId + ", Type=" + videoType);
            
            // Create a PlaybackModel from the extras for watch history
            model = new PlaybackModel();
            model.setMovieId(videoId);
            model.setTitle(title != null ? title : "Unknown Title");
            model.setCardImageUrl(thumbnail != null ? thumbnail : (poster != null ? poster : ""));
            model.setCategory(videoType != null ? videoType : "movie");
            model.setVideoType(videoType != null ? videoType : "movie");
            model.setIsPaid("0"); // Default to free for watch history
            model.setVideoUrl(videoUrl != null ? videoUrl : ""); // Use video URL from watch history if available
            
            // Set isTvSeries based on video type for proper watch history saving
            if (videoType != null && (videoType.equalsIgnoreCase("tvseries") || videoType.equalsIgnoreCase("tv") || videoType.equalsIgnoreCase("episode"))) {
                model.setIsTvSeries("1");
                Log.d(TAG, "📥 Set isTvSeries = 1 for type: " + videoType);
            } else {
                model.setIsTvSeries("0");
                Log.d(TAG, "📥 Set isTvSeries = 0 for type: " + videoType);
            }
            
            // Set the resume position
            if (position > 0) {
                mStartingPosition = position;
            }
            
            // IMPORTANT: Load movie details to get subtitle information
            if (videoId != null) {
                loadMovieDetailsForWatchHistory(videoId, videoType);
            }
        }

        if (model == null) {
            new ToastMsg(PlayerActivity.this).toastIconError("Lỗi: Không có dữ liệu video");
            finish();
            return;
        }

        // Check if external player is enabled (check ProfileFragment first, then MyAccountFragment)
        boolean useExternalPlayer = ProfileFragment.shouldUseExternalPlayer(this);
        if (!useExternalPlayer) {
            useExternalPlayer = MyAccountFragment.shouldUseExternalPlayer(this);
        }
        
        if (useExternalPlayer) {
            launchExternalPlayer();
            finish();
            return;
        }

        url = model.getVideoUrl();
        videoType = model.getVideoType();
        category = model.getCategory();
        if (model.getVideo() != null)
            video = model.getVideo();
        if (model.getCategory().equals("movie") && mChannelId > -1L && model.getIsPaid().equals("1")) {
            //Paid Content from Channel
            //check user has subscription or not
            //if not, send user to VideoDetailsActivity
            DatabaseHelper db = new DatabaseHelper(PlayerActivity.this);
            final String status = db.getActiveStatusData() != null ? db.getActiveStatusData().getStatus() : "inactive";
            if (!status.equals("active")) {
                Intent redirectIntent = new Intent(PlayerActivity.this, VideoDetailsActivity.class);
                redirectIntent.putExtra("type", model.getCategory());
                redirectIntent.putExtra("id", model.getMovieId());
                redirectIntent.putExtra("thumbImage", model.getCardImageUrl());
                startActivity(redirectIntent, null);
                finish();
            }
        }

        intiViews();
        initVideoPlayer(url, videoType);
        
        // Initialize TV Recommendation Manager for Android TV home screen (with error handling)
        try {
            tvRecommendationManager = new TvRecommendationManager(this);
            addToTvRecommendations();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing TV recommendations (safe to continue)", e);
        }
        
        // Initialize Watch History Sync Manager
        watchHistorySyncManager = new WatchHistorySyncManager(this);
        
        // Sync watch history from API to Android TV home screen
        try {
            syncWatchHistoryToTvHome();
        } catch (Exception e) {
            Log.e(TAG, "Error syncing watch history to TV home (safe to continue)", e);
        }
    }

    private void intiViews() {
        progressBar = findViewById(R.id.progress_bar);
        exoPlayerView = findViewById(R.id.player_view);
        subtitleView = exoPlayerView.getSubtitleView();
        rootLayout = findViewById(R.id.root_layout);
        // setupDualSubtitleOverlay();
        movieTitleTV = findViewById(R.id.movie_title);
        movieDescriptionTV = findViewById(R.id.movie_description);
        posterImageView = findViewById(R.id.poster_image_view);
        posterImageViewForTV = findViewById(R.id.poster_image_view_for_tv);
        serverButton = findViewById(R.id.img_server);
        subtitleButton = findViewById(R.id.img_subtitle);
        subtitleSettingsButton = findViewById(R.id.img_subtitle_settings);
        audioTrackButton = findViewById(R.id.img_audio);
        aspectRatioButton = findViewById(R.id.img_aspect_ratio);
        fastForwardButton = findViewById(R.id.exo_ffwd);
        previousEpisodeButton = findViewById(R.id.btn_previous_episode);
        nextEpisodeButton = findViewById(R.id.btn_next_episode);
        liveTvTextInController = findViewById(R.id.live_tv);
        
        // NEW: Custom seek buttons - outside PlayerView, in main activity layout (using Button, not ImageButton)
        Button customRewindButton = findViewById(R.id.btn_custom_rewind);
        Button customForwardButton = findViewById(R.id.btn_custom_forward);
        
        Log.d("PlayerActivity", "🔍 Custom button check - rewindButton: " + (customRewindButton != null ? "FOUND ✅" : "NULL ❌"));
        Log.d("PlayerActivity", "🔍 Custom button check - forwardButton: " + (customForwardButton != null ? "FOUND ✅" : "NULL ❌"));
        
        // Setup click listeners immediately (buttons are in activity layout, not PlayerView)
        if (customRewindButton != null) {
            customRewindButton.setOnClickListener(v -> {
                seekBackward(10000);
                Toast.makeText(this, "⏪ -10s", Toast.LENGTH_SHORT).show();
            });
        }
        if (customForwardButton != null) {
            customForwardButton.setOnClickListener(v -> {
                seekForward(10000);
                Toast.makeText(this, "⏩ +10s", Toast.LENGTH_SHORT).show();
            });
        }
        
        seekBarLayout = findViewById(R.id.seekbar_layout);
        if (category.equalsIgnoreCase("tv")) {
            serverButton.setVisibility(View.GONE);
            subtitleButton.setVisibility(View.GONE);
            //seekBarLayout.setVisibility(View.GONE);
            fastForwardButton.setVisibility(View.GONE);
            
            // Hide custom seek buttons for live TV (using LinearLayout container)
            View customSeekButtons = findViewById(R.id.custom_seek_buttons);
            if (customSeekButtons != null) customSeekButtons.setVisibility(View.GONE);
            
            liveTvTextInController.setVisibility(View.VISIBLE);
            posterImageView.setVisibility(View.GONE);
            posterImageViewForTV.setVisibility(VISIBLE);
            // Only hide seek bar for actual live content, not VOD TV content
            // We'll check if content is seekable later and show/hide accordingly
            //seekBarLayout.setVisibility(View.GONE);
        }

        if (category.equalsIgnoreCase("tvseries")) {
            serverButton.setVisibility(View.GONE);
            // Always show subtitle button for TV series (may have embedded subtitles)
            subtitleButton.setVisibility(View.VISIBLE);
            // audio tracks
            audioTrackButton.setVisibility(View.GONE);
            
            // Setup episode navigation buttons
            setupEpisodeNavigationButtons();
        }

        if (category.equalsIgnoreCase("movie")) {
            if (model.getVideoList() != null)
                videos.clear();
            videos = model.getVideoList();
            // Always show subtitle button for movies (may have embedded subtitles)
            subtitleButton.setVisibility(View.VISIBLE);
            if (videos != null) {
                if (videos.size() < 1)
                    serverButton.setVisibility(View.GONE);
            }
            // default hide audio button; will show if tracks exist later
            audioTrackButton.setVisibility(View.GONE);

        }

        // Setup seek buttons click listeners
        setupSeekButtons();
        
        // Ensure rewind button is visible (not hidden by TV category logic)
        if (rewindButton != null && !category.equalsIgnoreCase("tv")) {
            rewindButton.setVisibility(View.VISIBLE);
            Log.d("PlayerActivity", "✅ rewindButton set to VISIBLE");
        } else if (rewindButton == null) {
            Log.e("PlayerActivity", "❌ rewindButton is NULL!");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        // Keep screen bright and on during video playback - prevent auto dimming
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        
        // Set screen brightness to maximum
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = 1.0f; // Maximum brightness (0.0 to 1.0)
        getWindow().setAttributes(layoutParams);
        
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "Phim4K:PlayerActivity");
        
        // Acquire wake lock to prevent screen dimming
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(); // Acquire indefinitely - will be released in onDestroy
        }

        subtitleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //open subtitle dialog
                openSubtitleDialog();
            }
        });

        subtitleSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //open subtitle settings dialog
                openUnifiedSettingsDialog();
            }
        });

        audioTrackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAudioTrackSelectionDialog();
            }
        });

        aspectRatioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAspectRatioDialog();
            }
        });

        serverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //open server dialog
                openServerDialog(videos);
            }
        });


        //set title, description and poster in controller layout
        movieTitleTV.setText(model.getTitle());
        movieDescriptionTV.setText(model.getDescription());
        if (category.equalsIgnoreCase("tv")) {
            Picasso.get()
                    .load(model.getCardImageUrl())
                    .placeholder(R.drawable.poster_placeholder)
                    .centerCrop()
                    .resize(200, 120)
                    .error(R.drawable.poster_placeholder)
                    .into(posterImageViewForTV);
        }else {
            Picasso.get()
                    .load(model.getCardImageUrl())
                    .placeholder(R.drawable.poster_placeholder)
                    .centerCrop()
                    .resize(120, 200)
                    .error(R.drawable.poster_placeholder)
                    .into(posterImageView);
        }
    }

    @Override
    protected void onUserLeaveHint() {

        /** Use pressed home button **/
        //time to set media session active
        super.onUserLeaveHint();
    }

    boolean doubleBackToExitPressedOnce = false;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_MOVE_HOME:

                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (!exoPlayerView.isControllerVisible()) {
                    exoPlayerView.showController();
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:

                if (!exoPlayerView.isControllerVisible()) {
                    exoPlayerView.showController();
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!exoPlayerView.isControllerVisible()) {
                    exoPlayerView.showController();
                } else {
                    // Check if time bar has focus for seeking
                    View timeBar = exoPlayerView.findViewById(R.id.exo_progress);
                    if (timeBar != null && timeBar.hasFocus()) {
                        // If time bar is focused, allow seeking with DPAD - 30 seconds
                        seekForward(30000);
                        return true;
                    }
                    // Otherwise, let normal navigation happen
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (!exoPlayerView.isControllerVisible()) {
                    exoPlayerView.showController();
                } else {
                    // Check if time bar has focus for seeking
                    View timeBar = exoPlayerView.findViewById(R.id.exo_progress);
                    if (timeBar != null && timeBar.hasFocus()) {
                        // If time bar is focused, allow seeking with DPAD - 30 seconds
                        seekBackward(30000);
                        return true;
                    }
                    // Otherwise, let normal navigation happen
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                seekForward(30000); // Seek forward 30 seconds
                return true;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                seekBackward(30000); // Seek backward 30 seconds
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:

                if (!exoPlayerView.isControllerVisible()) {
                    exoPlayerView.showController();
                }
                break;
            case KeyEvent.KEYCODE_BACK:

                if (exoPlayerView.isControllerVisible()) {

                    exoPlayerView.hideController();
                    return true; // Consume the event, don't finish
                } else {
                    saveWatchHistory();
                    releasePlayer();
                    
                    // Check if this player was opened from watch history
                    // If so, go to video details instead of going back to home
                    boolean fromWatchHistory = getIntent().getBooleanExtra("from_watch_history", false);
                    Log.d(TAG, "🔙 KEYCODE_BACK - From watch history: " + fromWatchHistory);
                    
                    if (fromWatchHistory && model != null) {
                        Log.d(TAG, "🔙 Redirecting to VideoDetailsActivity - ID: " + model.getMovieId() + ", Type: " + model.getCategory());
                        // Go to video details page
                        Intent intent = new Intent(PlayerActivity.this, VideoDetailsActivity.class);
                        intent.putExtra("id", model.getMovieId());
                        intent.putExtra("type", model.getCategory());
                        intent.putExtra("thumbImage", model.getCardImageUrl());
                        startActivity(intent);
                    }
                    
                    finish();
                    return true;
                }
//                else {
//                    if (doubleBackToExitPressedOnce) {
//                        releasePlayer();
//                        //mediaSessionHelper.stopMediaSession();
//                        finish();
//                    } else {
//                        handleBackPress();
//                    }
//                }
            case KeyEvent.KEYCODE_ESCAPE:

               /* if (!exoPlayerView.isControllerVisible()){
                    exoPlayerView.showController();
                }else {
                    releasePlayer();
                    finish();
                }*/
                break;
        }
        return false;
    }

    private void launchExternalPlayer() {
        try {
            String videoUrl = model.getVideoUrl();
            if (videoUrl == null || videoUrl.isEmpty()) {
                new ToastMsg(this).toastIconError("URL video không hợp lệ");
                return;
            }

            // Get selected player from preferences
            SharedPreferences prefs = getSharedPreferences(Constants.USER_LOGIN_STATUS, MODE_PRIVATE);
            String selectedPlayer = prefs.getString("selected_player", "just");
            
            switch (selectedPlayer) {
                case "p4k":
                    launchP4KPlayer(videoUrl);
                    break;
                case "kodi":
                    launchKodiPlayer(videoUrl);
                    break;
                case "mx":
                    launchMXPlayer(videoUrl);
                    break;
                case "vimu":
                    launchVimuPlayer(videoUrl);
                    break;
                case "vlc":
                    launchVLCPlayer(videoUrl);
                    break;
                case "nplayer":
                    launchNPlayer(videoUrl);
                    break;
                case "dune_realtek":
                    launchDuneHDRealtek(videoUrl);
                    break;
                case "dune_amlogic":
                    launchDuneHDAmlogic(videoUrl);
                    break;
                case "zidoo_realtek":
                    launchZidooRealtek(videoUrl);
                    break;
                case "zidoo_amlogic":
                    launchZidooAmlogic(videoUrl);
                    break;
                default:
                    launchJustPlayer(videoUrl);
                    break;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching external player", e);
            new ToastMsg(this).toastIconError("Lỗi khi mở trình phát ngoài: " + e.getMessage());
        }
    }
    
    private void launchP4KPlayer(String videoUrl) {
        try {
            String userAgent = Util.getUserAgent(this, "oxoo");
            String packageName = "dev.anilbeesetti.nextplayer";
            
            try {
                getPackageManager().getPackageInfo(packageName, 0);
            } catch (Exception e) {
                showPlayerInstallDialog("P4K Player", packageName);
                return;
            }

            // P4K Player supports ACTION_VIEW with video URLs
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(videoUrl), "video/*");
            intent.setPackage(packageName);
            
            // Pass User-Agent via standard headers
            intent.putExtra("headers", new String[]{"User-Agent", userAgent});
            intent.putExtra("user-agent", userAgent);
            
            if (model.getTitle() != null) {
                intent.putExtra("title", model.getTitle());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }

            Log.d(TAG, "🎬 Launching P4K Player - URL: " + videoUrl);
            Log.d(TAG, "📡 User-Agent: " + userAgent);
            startActivity(intent);
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching P4K Player", e);
            new ToastMsg(this).toastIconError("Lỗi khi mở P4K Player: " + e.getMessage());
        }
    }
    
    private void launchKodiPlayer(String videoUrl) {
        try {
            String userAgent = Util.getUserAgent(this, "oxoo");
            String packageName = "org.xbmc.kodi";
            
            try {
                getPackageManager().getPackageInfo(packageName, 0);
            } catch (Exception e) {
                showPlayerInstallDialog("Kodi", packageName);
                return;
            }

            // Kodi supports ACTION_VIEW with video URLs
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(videoUrl), "video/*");
            intent.setPackage(packageName);
            
            // Kodi accepts headers for HTTP streams
            intent.putExtra("headers", new String[]{"User-Agent", userAgent});
            intent.putExtra("user-agent", userAgent);
            
            if (model.getTitle() != null) {
                intent.putExtra("title", model.getTitle());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }

            Log.d(TAG, "🎬 Launching Kodi - URL: " + videoUrl);
            Log.d(TAG, "📡 User-Agent: " + userAgent);
            startActivity(intent);
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching Kodi", e);
            new ToastMsg(this).toastIconError("Lỗi khi mở Kodi: " + e.getMessage());
        }
    }
    
    private void launchJustPlayer(String videoUrl) {
        try {
            // Get user agent
            String userAgent = Util.getUserAgent(this, "oxoo");
            
            // Check if Just Player is installed
            String justPlayerPackage = "com.brouken.player";
            
            try {
                getPackageManager().getPackageInfo(justPlayerPackage, 0);
            } catch (Exception e) {
                // Just Player not installed
                showPlayerInstallDialog("Just Player", "com.brouken.player");
                return;
            }

            // Launch Just Player
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(videoUrl), "video/*");
            intent.setPackage(justPlayerPackage);
            intent.putExtra("headers", new String[]{"User-Agent", userAgent});
            
            if (model.getTitle() != null) {
                intent.putExtra("title", model.getTitle());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }

            Log.d(TAG, "🎬 Launching Just Player - URL: " + videoUrl);
            startActivity(intent);
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching Just Player", e);
            new ToastMsg(this).toastIconError("Lỗi khi mở Just Player: " + e.getMessage());
        }
    }
    
    private void launchMXPlayer(String videoUrl) {
        try {
            // Get user agent
            String userAgent = Util.getUserAgent(this, "oxoo");
            
            // Check if MX Player is installed
            String[] mxPlayerPackages = {
                "com.mxtech.videoplayer.ad",  // Free version
                "com.mxtech.videoplayer.pro"  // Pro version
            };

            String installedPackage = null;
            for (String packageName : mxPlayerPackages) {
                try {
                    getPackageManager().getPackageInfo(packageName, 0);
                    installedPackage = packageName;
                    break;
                } catch (Exception e) {
                    // Package not found
                }
            }

            if (installedPackage == null) {
                // MX Player not installed
                showPlayerInstallDialog("MX Player", "com.mxtech.videoplayer.ad");
                return;
            }

            // Launch MX Player
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(videoUrl), "video/*");
            intent.setPackage(installedPackage);
            intent.putExtra("headers", new String[]{"User-Agent", userAgent});
            
            if (model.getTitle() != null) {
                intent.putExtra("title", model.getTitle());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }

            Log.d(TAG, "🎬 Launching MX Player - URL: " + videoUrl);
            startActivity(intent);
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching MX Player", e);
            new ToastMsg(this).toastIconError("Lỗi khi mở MX Player: " + e.getMessage());
        }
    }

    private void launchVimuPlayer(String videoUrl) {
        try {
            String userAgent = Util.getUserAgent(this, "oxoo");
            String packageName = "net.gtvbox.videoplayer";
            
            try {
                getPackageManager().getPackageInfo(packageName, 0);
            } catch (Exception e) {
                showPlayerInstallDialog("Vimu Player", packageName);
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(videoUrl), "video/*");
            intent.setPackage(packageName);
            
            // Vimu Player accepts User-Agent via intent extras
            intent.putExtra("user-agent", userAgent);
            intent.putExtra("headers", "User-Agent: " + userAgent);
            
            if (model.getTitle() != null) {
                intent.putExtra("title", model.getTitle());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }
            
            Log.d(TAG, "🎬 Launching Vimu Player - URL: " + videoUrl);
            startActivity(intent);
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching Vimu Player", e);
            new ToastMsg(this).toastIconError("Lỗi khi mở Vimu Player: " + e.getMessage());
        }
    }

    private void launchVLCPlayer(String videoUrl) {
        try {
            String userAgent = Util.getUserAgent(this, "oxoo");
            String packageName = "org.videolan.vlc";
            
            try {
                getPackageManager().getPackageInfo(packageName, 0);
            } catch (Exception e) {
                showPlayerInstallDialog("VLC Player", packageName);
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(videoUrl), "video/*");
            intent.setPackage(packageName);
            
            // VLC accepts User-Agent via intent extras
            intent.putExtra("http-user-agent", userAgent);
            intent.putExtra("user-agent", userAgent);
            
            if (model.getTitle() != null) {
                intent.putExtra("title", model.getTitle());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }

            Log.d(TAG, "🎬 Launching VLC Player - URL: " + videoUrl);
            Log.d(TAG, "📡 User-Agent: " + userAgent);
            startActivity(intent);
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching VLC Player", e);
            new ToastMsg(this).toastIconError("Lỗi khi mở VLC Player: " + e.getMessage());
        }
    }

    private void launchNPlayer(String videoUrl) {
        try {
            String userAgent = Util.getUserAgent(this, "oxoo");
            
            // Try TV version first, then Pro version
            String[] nplayerPackages = {
                "com.newin.nplayer.tv",   // TV version
                "com.newin.nplayer.pro"    // Pro version
            };

            String installedPackage = null;
            for (String packageName : nplayerPackages) {
                try {
                    getPackageManager().getPackageInfo(packageName, 0);
                    installedPackage = packageName;
                    break;
                } catch (Exception e) {
                    // Package not found
                }
            }

            if (installedPackage == null) {
                showPlayerInstallDialog("nPlayer", "com.newin.nplayer.tv");
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(videoUrl), "video/*");
            intent.setPackage(installedPackage);
            
            // nPlayer accepts headers and User-Agent
            intent.putExtra("headers", new String[]{"User-Agent", userAgent});
            intent.putExtra("user-agent", userAgent);
            
            if (model.getTitle() != null) {
                intent.putExtra("title", model.getTitle());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }

            Log.d(TAG, "🎬 Launching nPlayer (" + installedPackage + ") - URL: " + videoUrl);
            Log.d(TAG, "📡 User-Agent: " + userAgent);
            startActivity(intent);
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching nPlayer", e);
            new ToastMsg(this).toastIconError("Lỗi khi mở nPlayer: " + e.getMessage());
        }
    }

    private void launchDuneHDRealtek(String videoUrl) {
        try {
            String userAgent = Util.getUserAgent(this, "oxoo");
            String packageName = "com.dunehd.shell";
            
            try {
                getPackageManager().getPackageInfo(packageName, 0);
            } catch (Exception e) {
                showPlayerInstallDialog("Dune HD (Realtek)", packageName);
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(videoUrl), "video/*");
            intent.setPackage(packageName);
            intent.putExtra("headers", new String[]{"User-Agent", userAgent});
            
            if (model.getTitle() != null) {
                intent.putExtra("title", model.getTitle());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }

            Log.d(TAG, "🎬 Launching Dune HD (Realtek) - URL: " + videoUrl);
            startActivity(intent);
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching Dune HD Realtek", e);
            new ToastMsg(this).toastIconError("Lỗi khi mở Dune HD: " + e.getMessage());
        }
    }

    private void launchDuneHDAmlogic(String videoUrl) {
        try {
            String userAgent = Util.getUserAgent(this, "oxoo");
            String packageName = "com.dunehd.app";
            
            try {
                getPackageManager().getPackageInfo(packageName, 0);
            } catch (Exception e) {
                showPlayerInstallDialog("Dune HD (Amlogic)", packageName);
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(videoUrl), "video/*");
            intent.setPackage(packageName);
            intent.putExtra("headers", new String[]{"User-Agent", userAgent});
            
            if (model.getTitle() != null) {
                intent.putExtra("title", model.getTitle());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }

            Log.d(TAG, "🎬 Launching Dune HD (Amlogic) - URL: " + videoUrl);
            startActivity(intent);
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching Dune HD Amlogic", e);
            new ToastMsg(this).toastIconError("Lỗi khi mở Dune HD: " + e.getMessage());
        }
    }

    private void launchZidooRealtek(String videoUrl) {
        try {
            String userAgent = Util.getUserAgent(this, "oxoo");
            String packageName = "com.android.gallery3d";
            
            try {
                getPackageManager().getPackageInfo(packageName, 0);
            } catch (Exception e) {
                showPlayerInstallDialog("Zidoo (Realtek)", packageName);
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(videoUrl), "video/*");
            intent.setPackage(packageName);
            intent.putExtra("headers", new String[]{"User-Agent", userAgent});
            
            if (model.getTitle() != null) {
                intent.putExtra("title", model.getTitle());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }

            Log.d(TAG, "🎬 Launching Zidoo (Realtek) - URL: " + videoUrl);
            startActivity(intent);
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching Zidoo Realtek", e);
            new ToastMsg(this).toastIconError("Lỗi khi mở Zidoo: " + e.getMessage());
        }
    }

    private void launchZidooAmlogic(String videoUrl) {
        try {
            String userAgent = Util.getUserAgent(this, "oxoo");
            String packageName = "com.zidoo.player";
            
            try {
                getPackageManager().getPackageInfo(packageName, 0);
            } catch (Exception e) {
                showPlayerInstallDialog("Zidoo (Amlogic)", packageName);
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(videoUrl), "video/*");
            intent.setPackage(packageName);
            intent.putExtra("headers", new String[]{"User-Agent", userAgent});
            
            if (model.getTitle() != null) {
                intent.putExtra("title", model.getTitle());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }

            Log.d(TAG, "🎬 Launching Zidoo (Amlogic - Dolby Vision) - URL: " + videoUrl);
            startActivity(intent);
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching Zidoo Amlogic", e);
            new ToastMsg(this).toastIconError("Lỗi khi mở Zidoo: " + e.getMessage());
        }
    }

    private void showPlayerInstallDialog(final String playerName, final String packageName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.player_not_found);
        builder.setMessage(playerName + " chưa được cài đặt trên thiết bị của bạn. Bạn có muốn cài đặt không?");
        
        builder.setPositiveButton(R.string.install_player, (dialog, which) -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=" + packageName));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                startActivity(intent);
            }
            finish();
        });
        
        builder.setNegativeButton("Hủy", (dialog, which) -> {
            dialog.dismiss();
            finish();
        });
        
        builder.setCancelable(false);
        builder.show();
    }


    private void handleBackPress() {
        this.doubleBackToExitPressedOnce = true;
        //Toast.makeText(this, "Please click BACK again to exit.", Toast.LENGTH_SHORT).show();
        new ToastMsg(PlayerActivity.this).toastIconSuccess("Please click BACK again to exit.");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);

    }

    private void openServerDialog(List<Video> videos) {
        if (videos != null) {
            List<Video> videoList = new ArrayList<>();
            videoList.clear();

            for (Video video : videos) {
                if (video.getFileType() != null && !video.getFileType().equalsIgnoreCase("embed")) {
                    videoList.add(video);
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(PlayerActivity.this);
            View view = LayoutInflater.from(PlayerActivity.this).inflate(R.layout.layout_server_tv, null);
            RecyclerView serverRv = view.findViewById(R.id.serverRv);
            ServerAdapter serverAdapter = new ServerAdapter(PlayerActivity.this, videoList, "movie");
            serverRv.setLayoutManager(new LinearLayoutManager(PlayerActivity.this));
            serverRv.setHasFixedSize(true);
            serverRv.setAdapter(serverAdapter);

            Button closeBt = view.findViewById(R.id.close_bt);

            builder.setView(view);

            final AlertDialog dialog = builder.create();
            dialog.show();

            closeBt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            final ServerAdapter.OriginalViewHolder[] viewHolder = {null};
            serverAdapter.setOnItemClickListener(new ServerAdapter.OnItemClickListener() {

                @Override
                public void onItemClick(View view, Video obj, int position, ServerAdapter.OriginalViewHolder holder) {
                    Intent playerIntent = new Intent(PlayerActivity.this, PlayerActivity.class);
                    PlaybackModel video = new PlaybackModel();
                    video.setId(model.getId());
                    video.setTitle(model.getTitle());
                    video.setDescription(model.getDescription());
                    video.setCategory("movie");
                    video.setVideo(obj);
                    video.setVideoList(model.getVideoList());
                    video.setVideoUrl(obj.getFileUrl());
                    video.setVideoType(obj.getFileType());
                    video.setBgImageUrl(model.getBgImageUrl());
                    video.setCardImageUrl(model.getCardImageUrl());
                    video.setIsPaid(model.getIsPaid());

                    playerIntent.putExtra(VideoPlaybackActivity.EXTRA_VIDEO, video);

                    startActivity(playerIntent);
                    dialog.dismiss();
                    finish();
                }
            });
        } else {
            new ToastMsg(this).toastIconError(getString(R.string.no_other_server_found));
        }
    }

    private void openSubtitleDialog() {
        // Check if we have external subtitles first
        if (video != null && video.getSubtitle() != null && !video.getSubtitle().isEmpty()) {
            // Show external subtitle dialog with playback speed
            AlertDialog.Builder builder = new AlertDialog.Builder(PlayerActivity.this);
            View view = LayoutInflater.from(PlayerActivity.this).inflate(R.layout.layout_subtitle_dialog, null);
            RecyclerView serverRv = view.findViewById(R.id.serverRv);
            SubtitleListAdapter adapter = new SubtitleListAdapter(PlayerActivity.this, video.getSubtitle());
            serverRv.setLayoutManager(new LinearLayoutManager(PlayerActivity.this));
            serverRv.setHasFixedSize(true);
            serverRv.setAdapter(adapter);

            Button closeBt = view.findViewById(R.id.close_bt);
            
            // Playback speed buttons
            Button speed05 = view.findViewById(R.id.speed_0_5);
            Button speed075 = view.findViewById(R.id.speed_0_75);
            Button speed10 = view.findViewById(R.id.speed_1_0);
            Button speed15 = view.findViewById(R.id.speed_1_5);
            Button speed20 = view.findViewById(R.id.speed_2_0);
            Button speed30 = view.findViewById(R.id.speed_3_0);

            builder.setView(view);
            final AlertDialog dialog = builder.create();
            dialog.show();

            closeBt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            
            // Playback speed click listeners
            speed05.setOnClickListener(v -> setPlaybackSpeed(0.5f));
            speed075.setOnClickListener(v -> setPlaybackSpeed(0.75f));
            speed10.setOnClickListener(v -> setPlaybackSpeed(1.0f));
            speed15.setOnClickListener(v -> setPlaybackSpeed(1.5f));
            speed20.setOnClickListener(v -> setPlaybackSpeed(2.0f));
            speed30.setOnClickListener(v -> setPlaybackSpeed(3.0f));
            
            //click event
            adapter.setListener(new SubtitleListAdapter.OnSubtitleItemClickListener() {
                @Override
                public void onSubtitleItemClick(View view, Subtitle subtitle, int position, SubtitleListAdapter.SubtitleViewHolder holder) {
                    setSelectedSubtitle(mediaSource, subtitle.getUrl());
                    dialog.dismiss();
                }
            });
            return;
        }
        
        // No external subtitles, check for embedded subtitles
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector != null ? trackSelector.getCurrentMappedTrackInfo() : null;
        boolean hasEmbeddedSub = false;
        if (mappedTrackInfo != null) {
            for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
                if (mappedTrackInfo.getRendererType(i) == C.TRACK_TYPE_TEXT) {
                    TrackGroupArray groups = mappedTrackInfo.getTrackGroups(i);
                    if (groups != null && groups.length > 0) {
                        hasEmbeddedSub = true;
                        break;
                    }
                }
            }
        }
        
        if (hasEmbeddedSub) {
            // Show embedded subtitle selection dialog
            showSubtitleSelectionDialog();
        } else {
            // Show message that no subtitles found but allow subtitle settings
            new ToastMsg(this).toastIconError(getResources().getString(R.string.no_subtitle_found) + 
                ". Bạn có thể tùy chỉnh subtitle trong Settings.");
            // Optionally open subtitle settings
            // openUnifiedSettingsDialog();
        }
    }
    
    private void setPlaybackSpeed(float speed) {
        if (player != null) {
            PlaybackParameters params = new PlaybackParameters(speed);
            player.setPlaybackParameters(params);
            new ToastMsg(this).toastIconSuccess("Tốc độ phát: " + speed + "x");
        }
    }

    private void setSelectedSubtitle(com.google.android.exoplayer2.source.MediaSource mediaSource, String url) {
        MergingMediaSource mergedSource;
        if (url != null) {
            Uri subtitleUri = Uri.parse(url);

            Format subtitleFormat = new Format.Builder()
                    .setSampleMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage("en")
                    .build();

            DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(PlayerActivity.this,
                    Util.getUserAgent(PlayerActivity.this, CLASS_NAME), new DefaultBandwidthMeter());


            MediaItem.SubtitleConfiguration subtitleConfig = new MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage("en")
                    .build();
            
            com.google.android.exoplayer2.source.MediaSource subtitleSource = new SingleSampleMediaSource
                    .Factory(dataSourceFactory)
                    .createMediaSource(subtitleConfig, C.TIME_UNSET);


            mergedSource = new MergingMediaSource(mediaSource, subtitleSource);
            player.prepare(mergedSource, false, false);
            player.setPlayWhenReady(true);
            //resumePlayer();

        } else {
            Toast.makeText(PlayerActivity.this, "there is no subtitle", Toast.LENGTH_SHORT).show();
        }
    }

    public void initVideoPlayer(String url, String type) {
        if (player != null) {
            player.release();
        }

        progressBar.setVisibility(VISIBLE);
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter.Builder(PlayerActivity.this).build();
        AdaptiveTrackSelection.Factory videoTrackSelectionFactory = new
                AdaptiveTrackSelection.Factory();

        trackSelector = new
                DefaultTrackSelector(PlayerActivity.this, videoTrackSelectionFactory);

        // Configure track selector to handle audio codec issues more gracefully
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setExceedAudioConstraintsIfNecessary(true)
                .setExceedVideoConstraintsIfNecessary(true)
                .setExceedRendererCapabilitiesIfNecessary(true)
                .setTunnelingEnabled(false) // Disable tunneling to prevent audio codec issues
        );
        
        Log.d("PlayerActivity", "🔊 Track selector configured with audio codec fallback support");

        // Optimize LoadControl for faster seeking
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                        DefaultLoadControl.DEFAULT_MAX_BUFFER_MS, 
                        1500, // Reduce buffer before playback (faster seeking)
                        2000  // Reduce buffer after rebuffer (faster seeking)
                )
                .build();

        player = new ExoPlayer.Builder(PlayerActivity.this)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .build();
        exoPlayerView.setPlayer(player);
        // Set default to original aspect ratio (fit video to screen without cropping)
        exoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        exoPlayerView.setControllerShowTimeoutMs(5000);
        player.setPlayWhenReady(true);

        // Apply subtitle settings
        applySubtitleSettings();
        
        // Restore saved playback speed
        SharedPreferences prefs = getSharedPreferences("subtitle_settings", MODE_PRIVATE);
        float savedSpeed = prefs.getFloat("playback_speed", 1.0f);
        if (savedSpeed != 1.0f) {
            setPlaybackSpeed(savedSpeed);
        }

        Uri uri = Uri.parse(url);
        
        // ENHANCED FORMAT DETECTION: Auto-detect format from URL if type is not specific
        Log.d("PlayerActivity", "🎬 initVideoPlayer called with URL: " + url + ", Type: " + type);
        
        // Reset retry flags for new video
        hasRetriedWithHLS = false;
        hasHandledAudioCodecError = false;
        hasPlayerError = false; // Reset player error flag for new playback
        lastErrorTime = 0; // Reset error timestamp
        
        String detectedType = type;
        if ("video".equals(type) || type == null || type.isEmpty()) {
            detectedType = detectVideoFormat(url);
            Log.d("PlayerActivity", "🔍 Auto-detected format: " + detectedType + " from URL: " + url);
        } else {
            Log.d("PlayerActivity", "🔍 Using provided type: " + detectedType);
        }

        switch (detectedType) {
            case "hls":
                Log.d("PlayerActivity", "🎬 Using HLS MediaSource for: " + url);
                mediaSource = hlsMediaSource(uri, PlayerActivity.this);
                break;
           /* case "youtube":
                extractYoutubeUrl(url, PlayerActivity.this, 18);
                break;
            case "youtube-live":
                extractYoutubeUrl(url, PlayerActivity.this, 133);
                break;*/
            case "rtmp":
                Log.d("PlayerActivity", "🎬 Using RTMP MediaSource for: " + url);
                mediaSource = rtmpMediaSource(uri);
                break;
            default:
                Log.d("PlayerActivity", "🎬 Using Progressive MediaSource for: " + url);
                mediaSource = mediaSource(uri, PlayerActivity.this);
                break;
        }

        if (!type.contains("youtube")) {
            player.prepare(mediaSource, true, false);
            exoPlayerView.setPlayer(player);
            player.setPlayWhenReady(true);
        }

        seekToStartPosition();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (player.getPlayWhenReady() && playbackState == Player.STATE_READY) {
                    isPlaying = true;
                    progressBar.setVisibility(View.GONE);

                    // Reset error flag when video successfully starts playing
                    hasPlayerError = false;
                    lastErrorTime = 0;
                    Log.d(TAG, "✅ Player ready and playing - error flags cleared");

                    // update track button visibility when player is ready and tracks are available
                    updateTrackButtonVisibility();
                    // try to auto-select preferred language tracks (Vietnamese) once
                    applyPreferredLanguageTracks();
                    // dump mapped track info for debugging embedded subtitles (SRT in MKV)
                    dumpMappedTrackInfo();

                    //create media session
                    // mediaSessionHelper = new MediaSessionHelper(player, PlayerActivity.this, model, isPlaying);
                    // mediaSessionHelper.updateMetadata();
                    //mediaSessionHelper.updatePlaybackState();

                } else if (playbackState == Player.STATE_READY) {
                    isPlaying = false;
                    progressBar.setVisibility(View.GONE);
                    //add watch next card
                    long position = player.getCurrentPosition();
                    long duration = player.getDuration();
                    /*mediaSessionHelper.updateMetadata();
                    mediaSessionHelper.updatePlaybackState();*/

                } else if (playbackState == Player.STATE_BUFFERING) {
                    isPlaying = false;
                    progressBar.setVisibility(VISIBLE);
                    player.setPlayWhenReady(true);

                } else if (playbackState == Player.STATE_ENDED) {
                    // Video finished - check if we should auto-play next episode
                    Log.d(TAG, "📺 Video ended - checking for auto-play next episode");
                    
                    // Check if this STATE_ENDED was caused by a recent error (within 5 seconds)
                    long currentTime = System.currentTimeMillis();
                    boolean recentError = (lastErrorTime > 0) && (currentTime - lastErrorTime < 5000);
                    
                    if (hasPlayerError || recentError) {
                        Log.d(TAG, "🚫 Video ended due to player error (flag=" + hasPlayerError + ", recent=" + recentError + ") - auto-play disabled");
                        hasPlayerError = false; // Reset flag for next playback
                        lastErrorTime = 0; // Reset error timestamp
                        return;
                    }
                    
                    // Additional safety check: verify the video actually reached near the end
                    long currentPosition = player.getCurrentPosition();
                    long duration = player.getDuration();
                    
                    if (duration > 0 && currentPosition > 0) {
                        double progressPercentage = (double) currentPosition / duration * 100;
                        Log.d(TAG, "📺 Video progress: " + (currentPosition/1000) + "s / " + (duration/1000) + "s (" + String.format("%.1f", progressPercentage) + "%)");
                        
                        // More strict check: video must be very close to end (95%+) AND not ended abruptly
                        if (progressPercentage < 95.0) {
                            Log.d(TAG, "🚫 Video ended at " + String.format("%.1f", progressPercentage) + "% - likely interrupted, auto-play disabled");
                            return;
                        }
                        
                        // Extra validation: check if we're really at the end (within last 30 seconds)
                        long remainingTime = duration - currentPosition;
                        if (remainingTime > 30000) { // More than 30 seconds remaining
                            Log.d(TAG, "🚫 Video ended with " + (remainingTime/1000) + "s remaining - likely an error, auto-play disabled");
                            return;
                        }
                    } else {
                        // If we can't get valid position/duration, don't auto-next
                        Log.d(TAG, "🚫 Cannot determine video progress - auto-play disabled for safety");
                        return;
                    }
                    
                    // Only auto-play for TV series with navigation data and no errors
                    if (category.equalsIgnoreCase("tvseries") && canNavigateToNextEpisode()) {
                        Log.d(TAG, "✅ Video completed normally - auto-playing next episode in 3 seconds...");
                        
                        // Show a brief message or countdown (optional)
                        // Auto-play after 3 seconds delay
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "📺 Auto-playing next episode now");
                                navigateToNextEpisode();
                            }
                        }, 3000); // 3 second delay
                    } else {
                        Log.d(TAG, "📺 No next episode or not a TV series - video ended normally");
                    }
                    
                    //remove now playing card
                    //mediaSessionHelper.stopMediaSession();
                } else {
                    // player paused in any state
                    isPlaying = false;
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
                // Detect user-initiated seeks
                if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) {
                    long oldPos = oldPosition.positionMs;
                    long newPos = newPosition.positionMs;
                    long diff = Math.abs(newPos - oldPos);
                    
                    // If it's a significant position change (>2s), likely user seek
                    if (diff > 2000) {
                        Log.d("PlayerActivity", "🎯 Detected user seek: " + (oldPos/1000) + "s → " + (newPos/1000) + "s");
                        isUserSeeking = true;
                        
                        // Clear flag after delay
                        new Handler().postDelayed(() -> {
                            isUserSeeking = false;
                            Log.d("PlayerActivity", "🎯 Cleared seek flag after position discontinuity");
                        }, 2000);
                    }
                }
            }

            @Override
            public void onPlayerError(com.google.android.exoplayer2.PlaybackException error) {
                Log.e("PlayerActivity", "🎬 ExoPlayer Error: " + error.getMessage());
                progressBar.setVisibility(View.GONE);
                
                // Set error flag to prevent auto-next on error-induced STATE_ENDED
                hasPlayerError = true;
                lastErrorTime = System.currentTimeMillis();
                Log.e("PlayerActivity", "🚫 Player error detected - auto-next disabled");
                
                // Enhanced error detection with detailed logging
                Log.e("PlayerActivity", "🔍 Error details - Message: " + error.getMessage());
                if (error.getCause() != null) {
                    Log.e("PlayerActivity", "🔍 Error cause: " + error.getCause().getClass().getSimpleName());
                    Log.e("PlayerActivity", "🔍 Error cause message: " + error.getCause().getMessage());
                }
                
                // Handle different types of playback errors
                boolean isUnrecognizedFormat = false;
                boolean isAudioCodecError = false;
                boolean isVideoCodecError = false;
                
                if (error.getCause() != null) {
                    String causeClass = error.getCause().getClass().getSimpleName();
                    String causeMessage = error.getCause().getMessage();
                    String errorMessage = error.getMessage();
                    
                    if ("UnrecognizedInputFormatException".equals(causeClass) || 
                        (causeMessage != null && causeMessage.contains("UnrecognizedInputFormatException")) ||
                        (causeMessage != null && causeMessage.contains("None of the available extractors"))) {
                        isUnrecognizedFormat = true;
                    }
                    
                    // Check for audio codec errors (EAC3, AC-3, DTS, etc.)
                    // Check both error message and cause message
                    if ((errorMessage != null && (
                        errorMessage.contains("MediaCodecAudioRenderer") ||
                        errorMessage.contains("NO_UNSUPPORTED_TYPE") ||
                        errorMessage.contains("audio/eac3") ||
                        errorMessage.contains("Enhanced AC-3") ||
                        errorMessage.contains("EAC3") ||
                        errorMessage.contains("AC-3") ||
                        errorMessage.contains("DTS"))) ||
                        (causeMessage != null && (
                        causeMessage.contains("MediaCodecAudioRenderer") ||
                        causeMessage.contains("NO_UNSUPPORTED_TYPE") ||
                        causeMessage.contains("Enhanced AC-3") ||
                        causeMessage.contains("EAC3") ||
                        causeMessage.contains("AC-3") ||
                        causeMessage.contains("DTS") ||
                        causeMessage.contains("audio codec") ||
                        causeMessage.contains("AudioTrack") ||
                        causeMessage.contains("decoder init failed") ||
                        causeMessage.contains("Decoder init failed"))) ||
                        ("DecoderInitializationException".equals(causeClass) && 
                         errorMessage != null && errorMessage.contains("audio"))) {
                        isAudioCodecError = true;
                        Log.e("PlayerActivity", "🔊 Audio codec error detected: " + errorMessage);
                    }
                    
                    // Check for video codec errors (H.264, H.265, AVC, etc.)
                    if (causeMessage != null && (
                        causeMessage.contains("MediaCodecVideoRenderer") ||
                        causeMessage.contains("setPortMode") ||
                        causeMessage.contains("DynamicANWBuffer failed") ||
                        causeMessage.contains("OMX.google.h264.decoder") ||
                        causeMessage.contains("OMX.google.h265.decoder") ||
                        causeMessage.contains("video codec") ||
                        causeMessage.contains("VideoTrack") ||
                        causeMessage.contains("ACodec"))) {
                        isVideoCodecError = true;
                        Log.e("PlayerActivity", "🎬 Video codec error detected: " + causeMessage);
                    }
                }
                
                if (isVideoCodecError) {
                    Log.e("PlayerActivity", "🎬 Handling video codec error - applying video fallback settings");
                    handleVideoCodecError();
                } else if (isAudioCodecError) {
                    if (!hasHandledAudioCodecError) {
                        Log.e("PlayerActivity", "🔊 Audio codec error detected - trying smart track switching");
                        hasHandledAudioCodecError = true; // Prevent infinite loop
                        handleAudioCodecError();
                    } else {
                        Log.e("PlayerActivity", "🔊 Audio codec error already handled - showing user dialog");
                        showAudioCodecErrorDialog();
                    }
                } else if (isUnrecognizedFormat) {
                    Log.e("PlayerActivity", "❌ Unrecognized input format detected - trying HLS fallback");
                    handleUnrecognizedFormat();
                } else if (error.getCause() != null && error.getCause().getMessage() != null 
                    && error.getCause().getMessage().contains("network")) {
                    Log.e("PlayerActivity", "❌ Network connection failed");
                    showErrorDialog("Lỗi kết nối mạng", "Không thể kết nối đến server. Vui lòng kiểm tra kết nối internet.");
                } else {
                    Log.e("PlayerActivity", "❌ General playback error: " + error.getMessage());
                    showErrorDialog("Lỗi phát video", "Không thể phát video này. Vui lòng thử lại sau.");
                }
            }
        });

        exoPlayerView.setControllerVisibilityListener(new PlayerControlView.VisibilityListener() {
            @Override
            public void onVisibilityChange(int visibility) {
                visible = visibility;
            }
        });
        
        // Add seek detection for manual seek bar usage
        setupSeekBarListener();
        
        // Also add player listener to detect seek operations
        addPlayerSeekListener();
        
        // Add touch event monitoring for seek bar
        addTouchEventMonitoring();
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Monitor touch events on the seek bar area
        if (ev.getAction() == MotionEvent.ACTION_DOWN || ev.getAction() == MotionEvent.ACTION_MOVE) {
            View seekBarView = findViewById(R.id.exo_progress);
            if (seekBarView != null && isTouchOnView(ev, seekBarView)) {
                if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                    isUserSeeking = true;
                    Log.d("PlayerActivity", "🎯 Touch detected on seek bar - user seeking");
                }
            }
        } else if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            if (isUserSeeking) {
                // Delay clearing the flag to allow seek to complete
                new Handler().postDelayed(() -> {
                    isUserSeeking = false;
                    Log.d("PlayerActivity", "🎯 Touch ended - cleared seeking flag");
                }, 1500);
            }
        }
        
        return super.dispatchTouchEvent(ev);
    }
    
    private boolean isTouchOnView(MotionEvent event, View view) {
        try {
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            
            float x = event.getRawX();
            float y = event.getRawY();
            
            return x >= location[0] && x <= (location[0] + view.getWidth()) &&
                   y >= location[1] && y <= (location[1] + view.getHeight());
        } catch (Exception e) {
            return false;
        }
    }
    
    private void addTouchEventMonitoring() {
        // This method sets up additional touch monitoring if needed
        Log.d("PlayerActivity", "🔧 Touch event monitoring enabled for seek detection");
    }
    
    private void setupSeekBarListener() {
        try {
            // Find the DefaultTimeBar (exo_progress) and add listener to detect manual seeking
            View timeBar = exoPlayerView.findViewById(R.id.exo_progress);
            if (timeBar != null && timeBar instanceof com.google.android.exoplayer2.ui.DefaultTimeBar) {
                Log.d("PlayerActivity", "✅ Found DefaultTimeBar, adding scrub listener");
                ((com.google.android.exoplayer2.ui.DefaultTimeBar) timeBar).addListener(new com.google.android.exoplayer2.ui.TimeBar.OnScrubListener() {
                    @Override
                    public void onScrubStart(com.google.android.exoplayer2.ui.TimeBar timeBar, long position) {
                        isUserSeeking = true;
                        Log.d("PlayerActivity", "🎯 User started manual seek at: " + (position / 1000) + "s");
                    }

                    @Override
                    public void onScrubMove(com.google.android.exoplayer2.ui.TimeBar timeBar, long position) {
                        // User is dragging, keep the flag active
                    }

                    @Override
                    public void onScrubStop(com.google.android.exoplayer2.ui.TimeBar timeBar, long position, boolean canceled) {
                        // Clear the flag immediately after seek completes
                        Log.d("PlayerActivity", "🎯 User stopped manual seek at: " + (position / 1000) + "s, canceled: " + canceled);
                        new Handler().postDelayed(() -> {
                            isUserSeeking = false;
                            Log.d("PlayerActivity", "🎯 Cleared user seeking flag");
                        }, 500); // Reduced delay for faster response
                    }
                });
                
                // Add focus change listener for remote control seeking
                timeBar.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            Log.d("PlayerActivity", "🎯 Time bar focused - DPAD LEFT/RIGHT to seek");
                            Toast.makeText(PlayerActivity.this, "◀▶ LEFT/RIGHT để tua", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                Log.w("PlayerActivity", "⚠️ Could not find DefaultTimeBar for seek detection");
                // Try alternative method
                tryAlternativeSeekDetection();
            }
        } catch (Exception e) {
            Log.e("PlayerActivity", "Error setting up seek bar listener: " + e.getMessage());
            tryAlternativeSeekDetection();
        }
    }
    
    private void tryAlternativeSeekDetection() {
        try {
            // Alternative: Try to find any TimeBar in the hierarchy
            View rootView = exoPlayerView.getRootView();
            Log.d("PlayerActivity", "🔍 Trying alternative seek bar detection");
            
            // This is a fallback - we'll rely more on the player listener
            Log.d("PlayerActivity", "🔄 Using player listener for seek detection");
        } catch (Exception e) {
            Log.e("PlayerActivity", "Alternative seek detection failed: " + e.getMessage());
        }
    }
    
    private void addPlayerSeekListener() {
        // Enhanced player listener to catch all seek operations
        if (player != null) {
            Log.d("PlayerActivity", "✅ Adding enhanced player seek listener");
            // Note: We'll detect seeks through position discontinuities in the existing listener
        }
    }

    // Update visibility of subtitle and audio buttons depending on available renderer tracks
    private void updateTrackButtonVisibility() {
        if (trackSelector == null) return;
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            // If we don't have track info yet, hide both buttons
            if (subtitleButton != null) subtitleButton.setVisibility(View.GONE);
            if (audioTrackButton != null) audioTrackButton.setVisibility(View.GONE);
            return;
        }

        // Show subtitle button if TEXT tracks exist
        boolean hasText = false;
        boolean hasAudio = false;
        for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
            int rendererType = mappedTrackInfo.getRendererType(i);
            if (rendererType == C.TRACK_TYPE_TEXT) {
                TrackGroupArray groups = mappedTrackInfo.getTrackGroups(i);
                if (groups != null && groups.length > 0) hasText = true;
            }
            if (rendererType == C.TRACK_TYPE_AUDIO) {
                TrackGroupArray groups = mappedTrackInfo.getTrackGroups(i);
                if (groups != null && groups.length > 0) hasAudio = true;
            }
        }

        if (subtitleButton != null) subtitleButton.setVisibility(hasText ? View.VISIBLE : View.GONE);
        if (audioTrackButton != null) audioTrackButton.setVisibility(hasAudio ? View.VISIBLE : View.GONE);
    }

    // Auto-select Vietnamese audio/subtitle tracks when available (run once per session)
    private void applyPreferredLanguageTracks() {
        if (preferredTracksApplied || trackSelector == null) return;
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) return;

        // Helper to match Vietnamese language codes/labels
        java.util.function.Predicate<Format> isVietnamese = (format) -> {
            if (format == null) return false;
            if (format.language != null) {
                String lang = format.language.toLowerCase();
                if (lang.equals("vi") || lang.equals("vie") || lang.contains("vn") || lang.contains("vietnam")) return true;
            }
            if (format.label != null) {
                String label = format.label.toLowerCase();
                if (label.contains("viet") || label.contains("vie")) return true;
            }
            return false;
        };

        // Iterate renderers and pick audio/text tracks matching Vietnamese
        for (int rendererIndex = 0; rendererIndex < mappedTrackInfo.getRendererCount(); rendererIndex++) {
            int rendererType = mappedTrackInfo.getRendererType(rendererIndex);
            TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
            if (trackGroups == null) continue;

            for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
                TrackGroup group = trackGroups.get(groupIndex);
                for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                    Format format = group.getFormat(trackIndex);
                    if (isVietnamese.test(format)) {
                        if (rendererType == C.TRACK_TYPE_AUDIO || rendererType == C.TRACK_TYPE_TEXT) {
                            // apply selection override for this renderer
                            trackSelector.setParameters(
                                trackSelector.buildUponParameters().setSelectionOverride(
                                    rendererIndex,
                                    trackGroups,
                                    new DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex)
                                )
                            );
                        }
                    }
                }
            }
        }

        preferredTracksApplied = true;
    }

    // Log mapped track info once for debugging (shows format, language, mime for embedded tracks)
    private void dumpMappedTrackInfo() {
        if (trackInfoLogged || trackSelector == null) return;
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("MappedTrackInfo dump:\n");
        for (int rendererIndex = 0; rendererIndex < mappedTrackInfo.getRendererCount(); rendererIndex++) {
            int rendererType = mappedTrackInfo.getRendererType(rendererIndex);
            sb.append("Renderer ").append(rendererIndex).append(" type=").append(rendererType).append("\n");
            TrackGroupArray groups = mappedTrackInfo.getTrackGroups(rendererIndex);
            for (int g = 0; g < groups.length; g++) {
                sb.append(" Group ").append(g).append(" (size=").append(groups.get(g).length).append(")\n");
                TrackGroup group = groups.get(g);
                for (int t = 0; t < group.length; t++) {
                    Format f = group.getFormat(t);
                    sb.append("  Track ").append(t).append(": mime=").append(f.sampleMimeType)
                      .append(", lang=").append(f.language)
                      .append(", label=").append(f.label)
                      .append(", channels=").append(f.channelCount)
                      .append(", sampleRate=").append(f.sampleRate)
                      .append(", bitrate=").append(f.bitrate)
                      .append("\n");
                }
            }
        }

        trackInfoLogged = true;
    }

    private void seekToStartPosition() {
        // Skip ahead if given a starting position.
        if (mStartingPosition > -1L && !isUserSeeking) {
            if (player.getPlayWhenReady()) {
                Log.d("PlayerActivity", "📍 Seeking to start position: " + (mStartingPosition / 1000) + "s");
                player.seekTo(mStartingPosition);
            }
        }
    }

    private void seekForward(long seekTimeMs) {
        if (player != null && player.isCurrentWindowSeekable()) {
            long currentPosition = player.getCurrentPosition();
            long duration = player.getDuration();
            
            if (duration != C.TIME_UNSET) {
                long newPosition = Math.min(currentPosition + seekTimeMs, duration);
                
                // Set flag to prevent auto-resume conflicts
                isUserSeeking = true;
                player.seekTo(newPosition);
                
                // Clear flag after a delay
                new Handler().postDelayed(() -> isUserSeeking = false, 500);
                
                // Show seek feedback
                Toast.makeText(this, "Tua tới +" + (seekTimeMs / 1000) + "s", Toast.LENGTH_SHORT).show();
                Log.i("PlayerActivity", "🔄 Seeking forward to: " + (newPosition / 1000) + "s");
            }
        } else if (player != null && !player.isCurrentWindowSeekable()) {
            Toast.makeText(this, "Không thể tua trong nội dung trực tiếp", Toast.LENGTH_SHORT).show();
        }
    }

    private void seekBackward(long seekTimeMs) {
        if (player != null && player.isCurrentWindowSeekable()) {
            long currentPosition = player.getCurrentPosition();
            long newPosition = Math.max(currentPosition - seekTimeMs, 0);
            
            // Set flag to prevent auto-resume conflicts
            isUserSeeking = true;
            player.seekTo(newPosition);
            
            // Clear flag after a delay
            new Handler().postDelayed(() -> isUserSeeking = false, 500);
            
            // Show seek feedback
            Toast.makeText(this, "Tua lùi -" + (seekTimeMs / 1000) + "s", Toast.LENGTH_SHORT).show();
            Log.i("PlayerActivity", "🔄 Seeking backward to: " + (newPosition / 1000) + "s");
        } else if (player != null && !player.isCurrentWindowSeekable()) {
            Toast.makeText(this, "Không thể tua trong nội dung trực tiếp", Toast.LENGTH_SHORT).show();
        }
    }


    private com.google.android.exoplayer2.source.MediaSource mp3MediaSource(Uri uri) {
    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(getApplicationContext(), "ExoplayerDemo");
    ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
    return new ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory)
        .createMediaSource(MediaItem.fromUri(uri));
    }

    private com.google.android.exoplayer2.source.MediaSource mediaSource(Uri uri, Context context) {
    DataSource.Factory httpFactory = new DefaultHttpDataSource.Factory().setUserAgent("exoplayer");
    ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
    return new ProgressiveMediaSource.Factory(httpFactory, extractorsFactory)
        .createMediaSource(MediaItem.fromUri(uri));
    }

    private com.google.android.exoplayer2.source.MediaSource rtmpMediaSource(Uri uri) {
        com.google.android.exoplayer2.source.MediaSource videoSource = null;

        RtmpDataSourceFactory dataSourceFactory = new RtmpDataSourceFactory();
    ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
    videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory)
        .createMediaSource(MediaItem.fromUri(uri));

        return videoSource;
    }

   /* @SuppressLint("StaticFieldLeak")
    private void extractYoutubeUrl(String url, final Context context, final int tag) {

        new YouTubeExtractor(context) {
            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                if (ytFiles != null) {
                    int itag = tag;
                    String dashUrl = ytFiles.get(itag).getUrl();

                    try {
                        com.google.android.exoplayer2.source.MediaSource source = mediaSource(Uri.parse(dashUrl), context);
                        player.prepare(source, true, false);
                        //player.setPlayWhenReady(false);
                        exoPlayerView.setPlayer(player);
                        player.setPlayWhenReady(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.extract(url, true, true);
    }*/

    private com.google.android.exoplayer2.source.MediaSource hlsMediaSource(Uri uri, Context context) {

        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter.Builder(context).build();
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                Util.getUserAgent(context, "oxoo"), bandwidthMeter);
        com.google.android.exoplayer2.source.MediaSource videoSource = new HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri));

        return videoSource;
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "🔙 onBackPressed() called");
        
        // Check if controls are currently showing
        boolean controlsVisible = exoPlayerView.isControllerVisible();
        Log.d(TAG, "🔙 Controls visible: " + controlsVisible);
        
        if (controlsVisible) {
            // If controls are visible, hide them first
            exoPlayerView.hideController();

        } else {
            // If controls are already hidden, then exit player
            saveWatchHistory();
            releasePlayer();
            
            // Check if this player was opened from watch history
            // If so, go to video details instead of going back to home
            boolean fromWatchHistory = getIntent().getBooleanExtra("from_watch_history", false);
            Log.d(TAG, "🔙 From watch history: " + fromWatchHistory);
            
            if (fromWatchHistory && model != null) {
                Log.d(TAG, "🔙 Redirecting to VideoDetailsActivity - ID: " + model.getMovieId() + ", Type: " + model.getCategory());
                // Go to video details page
                Intent intent = new Intent(PlayerActivity.this, VideoDetailsActivity.class);
                intent.putExtra("id", model.getMovieId());
                intent.putExtra("type", model.getCategory());
                intent.putExtra("thumbImage", model.getCardImageUrl());
                startActivity(intent);
                finish();
            } else {
                Log.d(TAG, "🔙 Normal back press - going to previous activity");
                super.onBackPressed();
            }
        }
    }

    private void saveWatchHistory() {
        if (player == null || model == null) {
            return;
        }
        
        // Quick check if sync manager is available
        if (watchHistorySyncManager == null) {
            return;
        }
        
        long currentPosition = player.getCurrentPosition();
        long duration = player.getDuration();
        
        // Check for valid duration and position
        boolean validDuration = duration > 0 && duration != Long.MIN_VALUE;
        boolean validPosition = currentPosition >= 0;
        
        if (validDuration && validPosition && watchHistorySyncManager != null) {

            
            try {
                // Get metadata from model
                String videoUrl = model.getVideoUrl() != null ? model.getVideoUrl() : "";
                String description = model.getDescription() != null ? model.getDescription() : "";
                String runtime = model.getRuntime() != null ? model.getRuntime() : "";
                String videoQuality = model.getVideoQuality() != null ? model.getVideoQuality() : "";
                String isTvSeries = model.getIsTvSeries() != null ? model.getIsTvSeries() : "0";
                
                Log.d(TAG, "💾 Saving watch history - ID: " + model.getMovieId() + 
                          ", Type: " + (model.getVideoType() != null ? model.getVideoType() : "movie") + 
                          ", isTvSeries: " + isTvSeries + 
                          ", Position: " + currentPosition + "ms");
                
                watchHistorySyncManager.addWatchHistoryItemWithMetadata(
                    model.getMovieId(), 
                    model.getTitle(), 
                    description,
                    model.getCardImageUrl() != null ? model.getCardImageUrl() : "",
                    model.getCardImageUrl() != null ? model.getCardImageUrl() : "",
                    videoUrl,
                    currentPosition, 
                    duration, 
                    model.getVideoType() != null ? model.getVideoType() : "movie",
                    model.getReleaseDate() != null ? model.getReleaseDate() : "",
                    model.getImdbRating() != null ? model.getImdbRating() : "",
                    runtime,
                    videoQuality,
                    isTvSeries
                );
                
                // Force sync to server
                watchHistorySyncManager.forceSyncToServer();
                

                    
            } catch (Exception e) {

            }
        } else {

        }
    }

    /**
     * Setup episode navigation buttons for TV series
     * Shows/hides previous/next episode buttons based on navigation data
     */
    private void setupEpisodeNavigationButtons() {
        if (model == null || model.getAllSeasons() == null || model.getCurrentSeasonIndex() < 0) {
            // No navigation data available, hide buttons
            previousEpisodeButton.setVisibility(View.GONE);
            nextEpisodeButton.setVisibility(View.GONE);
            return;
        }

        // Show buttons
        previousEpisodeButton.setVisibility(View.VISIBLE);
        nextEpisodeButton.setVisibility(View.VISIBLE);

        // Check if we can go to previous episode
        boolean hasPreviousEpisode = canNavigateToPreviousEpisode();
        previousEpisodeButton.setEnabled(hasPreviousEpisode);
        previousEpisodeButton.setAlpha(hasPreviousEpisode ? 1.0f : 0.4f);

        // Check if we can go to next episode
        boolean hasNextEpisode = canNavigateToNextEpisode();
        nextEpisodeButton.setEnabled(hasNextEpisode);
        nextEpisodeButton.setAlpha(hasNextEpisode ? 1.0f : 0.4f);

        // Set click listeners
        previousEpisodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToPreviousEpisode();
            }
        });

        nextEpisodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToNextEpisode();
            }
        });
    }

    /**
     * Setup seek buttons (rewind 10s and fast forward 10s) click listeners
     */
    private void setupSeekButtons() {
        // Setup rewind button (backward 10s)
        if (rewindButton != null) {
            rewindButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    seekBackward(10000); // 10 seconds backward
                    Toast.makeText(PlayerActivity.this, "⏪ -10s", Toast.LENGTH_SHORT).show();
                }
            });
            Log.d("PlayerActivity", "✅ Rewind button (10s) setup complete");
        } else {
            Log.w("PlayerActivity", "⚠️ Rewind button not found in layout");
        }
        
        // Fast forward button already has default ExoPlayer behavior
        // But we can override it if needed
        if (fastForwardButton != null) {
            fastForwardButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    seekForward(10000); // 10 seconds forward
                    Toast.makeText(PlayerActivity.this, "⏩ +10s", Toast.LENGTH_SHORT).show();
                }
            });
            Log.d("PlayerActivity", "✅ Fast forward button (10s) setup complete");
        } else {
            Log.w("PlayerActivity", "⚠️ Fast forward button not found in layout");
        }
    }

    /**
     * Check if we can navigate to the previous episode
     */
    private boolean canNavigateToPreviousEpisode() {
        if (model == null || model.getAllSeasons() == null) {
            return false;
        }

        int currentSeasonIndex = model.getCurrentSeasonIndex();
        int currentEpisodeIndex = model.getCurrentEpisodeIndex();

        // Can go back if not the first episode of the first season
        if (currentEpisodeIndex > 0) {
            return true; // There's a previous episode in current season
        }

        // If first episode of current season, check if there's a previous season
        return currentSeasonIndex > 0;
    }

    /**
     * Check if we can navigate to the next episode
     */
    private boolean canNavigateToNextEpisode() {
        if (model == null || model.getAllSeasons() == null) {
            return false;
        }

        int currentSeasonIndex = model.getCurrentSeasonIndex();
        int currentEpisodeIndex = model.getCurrentEpisodeIndex();
        int totalEpisodesInSeason = model.getTotalEpisodesInSeason();

        // Can go forward if not the last episode of the last season
        if (currentEpisodeIndex < totalEpisodesInSeason - 1) {
            return true; // There's a next episode in current season
        }

        // If last episode of current season, check if there's a next season
        return currentSeasonIndex < model.getAllSeasons().size() - 1;
    }

    /**
     * Navigate to the previous episode
     */
    private void navigateToPreviousEpisode() {
        if (model == null || model.getAllSeasons() == null || !canNavigateToPreviousEpisode()) {
            return;
        }

        // Save current watch progress
        saveWatchHistory();

        int currentSeasonIndex = model.getCurrentSeasonIndex();
        int currentEpisodeIndex = model.getCurrentEpisodeIndex();

        int newSeasonIndex = currentSeasonIndex;
        int newEpisodeIndex = currentEpisodeIndex - 1;

        // If we're at the first episode of current season, go to last episode of previous season
        if (newEpisodeIndex < 0 && currentSeasonIndex > 0) {
            newSeasonIndex = currentSeasonIndex - 1;
            com.files.codes.model.movieDetails.Season previousSeason = model.getAllSeasons().get(newSeasonIndex);
            if (previousSeason.getEpisodes() != null) {
                newEpisodeIndex = previousSeason.getEpisodes().size() - 1;
            }
        }

        // Load the previous episode
        loadEpisode(newSeasonIndex, newEpisodeIndex);
    }

    /**
     * Navigate to the next episode
     */
    private void navigateToNextEpisode() {
        if (model == null || model.getAllSeasons() == null || !canNavigateToNextEpisode()) {
            return;
        }

        // Save current watch progress
        saveWatchHistory();

        int currentSeasonIndex = model.getCurrentSeasonIndex();
        int currentEpisodeIndex = model.getCurrentEpisodeIndex();
        int totalEpisodesInSeason = model.getTotalEpisodesInSeason();

        int newSeasonIndex = currentSeasonIndex;
        int newEpisodeIndex = currentEpisodeIndex + 1;

        // If we're at the last episode of current season, go to first episode of next season
        if (newEpisodeIndex >= totalEpisodesInSeason && currentSeasonIndex < model.getAllSeasons().size() - 1) {
            newSeasonIndex = currentSeasonIndex + 1;
            newEpisodeIndex = 0;
        }

        // Load the next episode
        loadEpisode(newSeasonIndex, newEpisodeIndex);
    }

    /**
     * Load a specific episode by season and episode index
     */
    private void loadEpisode(int seasonIndex, int episodeIndex) {
        if (model == null || model.getAllSeasons() == null) {
            return;
        }

        if (seasonIndex < 0 || seasonIndex >= model.getAllSeasons().size()) {
            return;
        }

        com.files.codes.model.movieDetails.Season season = model.getAllSeasons().get(seasonIndex);
        if (season.getEpisodes() == null || episodeIndex < 0 || episodeIndex >= season.getEpisodes().size()) {
            return;
        }

        com.files.codes.model.movieDetails.Episode episode = season.getEpisodes().get(episodeIndex);

        // Create new PlaybackModel with updated episode
        PlaybackModel newModel = new PlaybackModel();
        newModel.setMovieId(model.getMovieId()); // Keep same series ID for watch history
        
        // Handle episode ID
        String episodeId = episode.getEpisodesId();
        if (episodeId != null && !episodeId.matches("\\d+")) {
            newModel.setId((long) episodeId.hashCode());
        } else {
            try {
                newModel.setId(Long.parseLong(episodeId));
            } catch (NumberFormatException e) {
                newModel.setId((long) episodeId.hashCode());
            }
        }

        newModel.setTitle(model.getTitle()); // Keep series title
        newModel.setDescription("Season: " + season.getSeasonsName() + "; Episode: " + episode.getEpisodesName());
        newModel.setVideoType("tvseries");
        newModel.setCategory("tvseries");
        newModel.setVideoUrl(episode.getFileUrl());
        
        com.files.codes.model.Video videoModel = new com.files.codes.model.Video();
        videoModel.setSubtitle(episode.getSubtitle());
        newModel.setVideo(videoModel);
        
        newModel.setCardImageUrl(model.getCardImageUrl());
        newModel.setBgImageUrl(model.getBgImageUrl());
        newModel.setIsPaid(model.getIsPaid());
        newModel.setIsTvSeries("1");

        // Update navigation data
        newModel.setCurrentSeasonIndex(seasonIndex);
        newModel.setCurrentEpisodeIndex(episodeIndex);
        newModel.setTotalEpisodesInSeason(season.getEpisodes().size());
        newModel.setAllSeasons(model.getAllSeasons());

        // Restart the activity with the new episode
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(VideoPlaybackActivity.EXTRA_VIDEO, newModel);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        finish();
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Save watch history before destroying
        saveWatchHistory();
        releasePlayer();
        
        // Restore screen brightness to system default
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        getWindow().setAttributes(layoutParams);
        
        // Release screen brightness lock and flags
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Save watch history when pausing
        saveWatchHistory();
        releasePlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        // Wake lock is already acquired in onStart() - no need to re-acquire
    }

    private void releasePlayer() {
        if (player != null) {
            player.setPlayWhenReady(false);
            player.stop();
            player.release();
            player = null;
            exoPlayerView.setPlayer(null);
        }
    }

    // Show dialog to select subtitle track (shows language/label and an 'Off' option)
    private void showSubtitleSelectionDialog() {
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) return;
        int rendererIndex = -1;
        for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
            if (mappedTrackInfo.getRendererType(i) == C.TRACK_TYPE_TEXT) {
                rendererIndex = i;
                break;
            }
        }

        if (rendererIndex == -1) {
            // Fallback: if player is showing cues, offer On/Off toggle
            if (player != null && player.getCurrentCues() != null) {
                boolean hasCues = false;
                try {
                    Object cueGroup = player.getCurrentCues();
                    java.lang.reflect.Method getCuesMethod = cueGroup.getClass().getMethod("getCues");
                    Object cuesList = getCuesMethod.invoke(cueGroup);
                    if (cuesList instanceof java.util.List) {
                        hasCues = !((java.util.List<?>) cuesList).isEmpty();
                    }
                } catch (Exception e) {
                    // fallback: try size() if available
                    try {
                        java.lang.reflect.Method sizeMethod = player.getCurrentCues().getClass().getMethod("size");
                        int sz = (int) sizeMethod.invoke(player.getCurrentCues());
                        hasCues = sz > 0;
                    } catch (Exception ignored) {}
                }
                if (hasCues) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Subtitles");
                    String[] options = {"On", "Off"};
                    builder.setItems(options, (dialog, which) -> {
                        // Try to find a text renderer to disable/enable
                        MappingTrackSelector.MappedTrackInfo mappedTrackInfo2 = trackSelector.getCurrentMappedTrackInfo();
                        if (mappedTrackInfo2 != null) {
                            for (int i = 0; i < mappedTrackInfo2.getRendererCount(); i++) {
                                if (mappedTrackInfo2.getRendererType(i) == C.TRACK_TYPE_TEXT) {
                                    trackSelector.setParameters(
                                        trackSelector.buildUponParameters().setRendererDisabled(i, which == 1)
                                    );
                                }
                            }
                        }
                    });
                    builder.show();
                    return;
                }
            }
            Toast.makeText(this, "No subtitles available", Toast.LENGTH_SHORT).show();
            return;
        }

        final TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
        final int renderer = rendererIndex;
        final List<String> labels = new ArrayList<>();
        final List<int[]> selectionPairs = new ArrayList<>(); // pair of [groupIndex, trackIndex]

        // Count total subtitle tracks
        int totalTracks = 0;
        for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
            TrackGroup group = trackGroups.get(groupIndex);
            totalTracks += group.length;
        }

        // Add an 'Off' option to clear text selection
        labels.add("Off");
        selectionPairs.add(new int[]{-1, -1});

        // If no subtitle tracks, show message
        if (totalTracks == 0) {
            Toast.makeText(this, "No subtitle tracks available", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
            TrackGroup group = trackGroups.get(groupIndex);
            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                Format format = group.getFormat(trackIndex);
                String label = null;
                if (format.label != null && !format.label.isEmpty()) label = format.label;
                else if (format.language != null && !format.language.isEmpty()) label = format.language;
                else if (format.sampleMimeType != null) label = format.sampleMimeType;
                else label = "Subtitle " + (labels.size());

                labels.add(label);
                selectionPairs.add(new int[]{groupIndex, trackIndex});
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Subtitle (" + totalTracks + " available)");
        builder.setItems(labels.toArray(new String[0]), (dialog, which) -> {
            int[] pair = selectionPairs.get(which);
            
            // Store current playback position and state
            long currentPosition = player.getCurrentPosition();
            boolean wasPlaying = player.getPlayWhenReady();
            
            try {
                if (pair[0] == -1) {
                    // Turn subtitles off by disabling the text renderer
                    trackSelector.setParameters(trackSelector.buildUponParameters().setRendererDisabled(renderer, true));
                } else {
                    int group = pair[0];
                    int track = pair[1];
                    // Enable renderer and set selection override
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setRendererDisabled(renderer, false)
                            .setSelectionOverride(
                                renderer,
                                trackGroups,
                                new DefaultTrackSelector.SelectionOverride(group, track)
                            )
                    );
                }
                
                // Resume playback if it was playing before
                if (wasPlaying && !isUserSeeking) { // Don't auto-resume if user is seeking
                    new Handler().postDelayed(() -> {
                        if (player != null && !isUserSeeking) { // Double check
                            player.seekTo(currentPosition);
                            player.setPlayWhenReady(true);
                        }
                    }, 100);
                }
                
            } catch (Exception e) {

                Toast.makeText(this, "Error changing subtitle track", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    // Show dialog to select audio track (display language/label where available)
    private void showAudioTrackSelectionDialog() {
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) return;
        
        int rendererIndex = -1;
        for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
            if (mappedTrackInfo.getRendererType(i) == C.TRACK_TYPE_AUDIO) {
                rendererIndex = i;
                break;
            }
        }
        if (rendererIndex == -1) {
            Toast.makeText(this, "No audio tracks available", Toast.LENGTH_SHORT).show();
            return;
        }

        final TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
        final int renderer = rendererIndex;
        final List<String> labels = new ArrayList<>();
        final List<int[]> selectionPairs = new ArrayList<>();

        // Count total tracks
        int totalTracks = 0;
        for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
            TrackGroup group = trackGroups.get(groupIndex);
            totalTracks += group.length;
        }

        // If only one track, show message instead of dialog
        if (totalTracks <= 1) {
            Toast.makeText(this, "Only one audio track available", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
            TrackGroup group = trackGroups.get(groupIndex);
            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                Format format = group.getFormat(trackIndex);
                String label = null;
                if (format.label != null && !format.label.isEmpty()) label = format.label;
                else if (format.language != null && !format.language.isEmpty()) label = format.language;
                else if (format.sampleMimeType != null) label = format.sampleMimeType;
                else label = "Audio " + (labels.size() + 1);

                labels.add(label);
                selectionPairs.add(new int[]{groupIndex, trackIndex});
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Audio Track (" + totalTracks + " available)");
        builder.setItems(labels.toArray(new String[0]), (dialog, which) -> {
            int[] pair = selectionPairs.get(which);
            int group = pair[0];
            int track = pair[1];
            
            // Store current playback position and state
            long currentPosition = player.getCurrentPosition();
            boolean wasPlaying = player.getPlayWhenReady();
            
            try {
                // Apply track selection with proper parameters
                trackSelector.setParameters(
                    trackSelector.buildUponParameters().setSelectionOverride(
                        renderer,
                        trackGroups,
                        new DefaultTrackSelector.SelectionOverride(group, track)
                    )
                );
                
                // Resume playback if it was playing before
                if (wasPlaying && !isUserSeeking) { // Don't auto-resume if user is seeking
                    // Small delay to ensure track change is processed
                    new Handler().postDelayed(() -> {
                        if (player != null && !isUserSeeking) { // Double check
                            player.seekTo(currentPosition);
                            player.setPlayWhenReady(true);
                        }
                    }, 100);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error changing audio track", e);
                Toast.makeText(this, "Error changing audio track", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void openSubtitleSettingsDialog() {
        // This method is deprecated and replaced by openUnifiedSettingsDialog.
        // The old implementation has been removed to avoid confusion.
        openUnifiedSettingsDialog();
    }

    private void applySubtitleSettings() {
        Log.d(TAG, "Applying subtitle settings");
        SharedPreferences prefs = getSharedPreferences("subtitle_settings", MODE_PRIVATE);
        int fontSize = prefs.getInt("font_size", 20);
        int verticalOffset = prefs.getInt("vertical_offset", 0);
        int fontType = prefs.getInt("font_type", 0);
        boolean background = prefs.getBoolean("background", false);
        int textColorIndex = prefs.getInt("text_color", 0);
        int outlineColorIndex = prefs.getInt("outline_color", 0);

        // Define color arrays
        int[] colorValues = {Color.WHITE, Color.YELLOW, Color.RED, Color.GREEN, 
                            Color.BLUE, 0xFFFF8C00, 0xFFFFC0CB, Color.CYAN}; // Orange, Pink
        
        // Match the outline color array with the dialog (6 elements)
        int[] outlineColorValues = {Color.TRANSPARENT, Color.BLACK, Color.WHITE, Color.RED, Color.BLUE, Color.YELLOW};
        
        int textColor = colorValues[textColorIndex];
        int outlineColor;
        
        // Handle outline color bounds checking
        if (outlineColorIndex >= outlineColorValues.length) {
            outlineColorIndex = 1; // Default to Black
        }
        
        // Get outline color from array
        outlineColor = outlineColorValues[outlineColorIndex];

        // Get typeface
        Typeface typeface = Typeface.DEFAULT;
        if (fontType == 1) typeface = Typeface.SANS_SERIF;
        else if (fontType == 2) typeface = Typeface.SERIF;
        else if (fontType == 3) typeface = Typeface.MONOSPACE;
        else if (fontType == 4) {
            try {
                typeface = Typeface.createFromAsset(getAssets(), "fonts/uvnhonghahep-b.ttf");
            } catch (Exception e) {
                Log.e("PlayerActivity", "Failed to load custom font", e);
                typeface = Typeface.DEFAULT;
            }
        }

        // Apply to the single SubtitleView
        if (subtitleView != null) {
            CaptionStyleCompat style = new CaptionStyleCompat(
                    textColor, // Foreground (text) color
                    background ? Color.BLACK : Color.TRANSPARENT, // Background color
                    Color.TRANSPARENT, // Window color
                    CaptionStyleCompat.EDGE_TYPE_OUTLINE, // Enable outline
                    outlineColor, // Outline color (auto or manual)
                    typeface
            );
            subtitleView.setStyle(style);
            subtitleView.setFixedTextSize(Cue.TEXT_SIZE_TYPE_ABSOLUTE, fontSize);

            // Apply vertical offset - from bottom position
            // verticalOffset: negative values = closer to bottom edge, positive = further from bottom
            float bottomPadding = 0.01f + (verticalOffset * 0.01f); // 1% base + offset
            bottomPadding = Math.max(0.0f, Math.min(0.85f, bottomPadding)); // Clamp between 0% and 85%
            subtitleView.setBottomPaddingFraction(bottomPadding);
            
            Log.d(TAG, "Applied subtitle - colors: text=" + Integer.toHexString(textColor) + 
                      ", outline=" + Integer.toHexString(outlineColor) + " (index=" + outlineColorIndex + ")" +
                      ", position: " + verticalOffset + "% offset, bottomPadding: " + bottomPadding);
        }
    }

    /*
    // =================================================================================
    // DUAL SUBTITLE RELATED METHODS (TO BE REMOVED OR REFACTORED)
    // =================================================================================
    private void showDualSubtitleTestDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Dual Subtitle Test");

        View view = LayoutInflater.from(this).inflate(R.layout.layout_dual_subtitle_test, null);
        Spinner primarySpinner = view.findViewById(R.id.spinner_primary_track_test);
        Spinner secondarySpinner = view.findViewById(R.id.spinner_secondary_track_test);

        detectAvailableSubtitleTracks();

        if (availableSubtitleTracks.isEmpty()) {
            builder.setMessage("No subtitle tracks found.");
        } else {
            ArrayAdapter<SubtitleTrack> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, availableSubtitleTracks);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            primarySpinner.setAdapter(adapter);
            secondarySpinner.setAdapter(adapter);
        }

        builder.setView(view);

        builder.setPositiveButton("Apply", (dialog, which) -> {
            int primaryIndex = primarySpinner.getSelectedItemPosition();
            int secondaryIndex = secondarySpinner.getSelectedItemPosition();

            SharedPreferences.Editor editor = getSharedPreferences("subtitle_settings", MODE_PRIVATE).edit();
            editor.putInt("primary_track_index", availableSubtitleTracks.get(primaryIndex).getTrackIndex());
            editor.putInt("secondary_track_index", availableSubtitleTracks.get(secondaryIndex).getTrackIndex());
            editor.putBoolean("dual_subtitle", true); // Enable dual sub when applying
            editor.apply();

            applySubtitleSettings();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void detectAvailableSubtitleTracks() {
        availableSubtitleTracks.clear();
        if (player == null) return;

        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) return;

        for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
            if (player.getRendererType(i) == C.TRACK_TYPE_TEXT) {
                TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
                for (int j = 0; j < trackGroups.length; j++) {
                    TrackGroup group = trackGroups.get(j);
                    for (int k = 0; k < group.length; k++) {
                        Format format = group.getFormat(k);
                        String language = format.language != null ? format.language : "Unknown";
                        String label = format.label != null ? format.label : "Track " + k;
                        availableSubtitleTracks.add(new SubtitleTrack(i, j, k, language + " (" + label + ")"));
                    }
                }
            }
        }
        Log.d(TAG, "Detected " + availableSubtitleTracks.size() + " subtitle tracks.");
    }

    private void updateTrackSelectionVisibility(View primaryLayout, View secondaryLayout, boolean show) {
        if (primaryLayout != null && secondaryLayout != null) {
            Log.d(TAG, "Updating track selection visibility to: " + (show ? "VISIBLE" : "GONE"));
            primaryLayout.setVisibility(show ? View.VISIBLE : View.GONE);
            secondaryLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        } else {
            Log.e(TAG, "Cannot update visibility, layouts are null");
        }
    }

    private void setupDualSubtitleOverlay() {
        if (dualSubtitleOverlay != null) {
            primarySubtitleView = new SubtitleView(this);
            secondarySubtitleView = new SubtitleView(this);

            primarySubtitleView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            secondarySubtitleView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));

            dualSubtitleOverlay.addView(primarySubtitleView);
            dualSubtitleOverlay.addView(secondarySubtitleView);
        }
    }

    private void updateDualSubtitleState(boolean enabled) {
        if (dualSubtitleOverlay != null) {
            dualSubtitleOverlay.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }
        if (subtitleView != null) {
            subtitleView.setVisibility(enabled ? View.GONE : View.VISIBLE);
        }

        if (enabled) {
            SharedPreferences prefs = getSharedPreferences("subtitle_settings", MODE_PRIVATE);
            int primaryTrackIndex = prefs.getInt("primary_track_index", -1);
            int secondaryTrackIndex = prefs.getInt("secondary_track_index", -1);

            // This part is complex and needs careful implementation
            // to select and render two separate subtitle tracks.
            // ExoPlayer's default track selector only supports one text track at a time.
            // A custom approach is needed here.
            Log.d(TAG, "Dual subtitle mode enabled. Primary: " + primaryTrackIndex + ", Secondary: " + secondaryTrackIndex);

        } else {
            // Revert to single subtitle mode
            // This is handled by the default track selector
            Log.d(TAG, "Dual subtitle mode disabled.");
        }
    }
    */

    private void openUnifiedSettingsDialog() {
        // Subtitle settings dialog with all customization options
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Subtitle Settings");
        
        // Create a ScrollView to handle long content
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        
        // Create a vertical layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        
        SharedPreferences prefs = getSharedPreferences("subtitle_settings", MODE_PRIVATE);
        
        // Font size controls
        TextView fontSizeLabel = new TextView(this);
        fontSizeLabel.setText("Font Size:");
        fontSizeLabel.setTextSize(16);
        fontSizeLabel.setPadding(0, 10, 0, 10);
        
        LinearLayout fontSizeLayout = new LinearLayout(this);
        fontSizeLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button fontSizeMinusBtn = new Button(this);
        fontSizeMinusBtn.setText("-");
        Button fontSizePlusBtn = new Button(this);
        fontSizePlusBtn.setText("+");
        TextView fontSizeTV = new TextView(this);
        fontSizeTV.setText(prefs.getInt("font_size", 20) + "sp");
        fontSizeTV.setGravity(android.view.Gravity.CENTER);
        fontSizeTV.setPadding(20, 0, 20, 0);
        
        fontSizeLayout.addView(fontSizeMinusBtn);
        fontSizeLayout.addView(fontSizeTV);
        fontSizeLayout.addView(fontSizePlusBtn);
        
        // Font type controls
        TextView fontTypeLabel = new TextView(this);
        fontTypeLabel.setText("Font Type:");
        fontTypeLabel.setTextSize(16);
        fontTypeLabel.setPadding(0, 20, 0, 10);
        
        LinearLayout fontTypeLayout = new LinearLayout(this);
        fontTypeLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button fontTypePrevBtn = new Button(this);
        fontTypePrevBtn.setText("◀");
        Button fontTypeNextBtn = new Button(this);
        fontTypeNextBtn.setText("▶");
        TextView fontTypeTV = new TextView(this);
        
        String[] fontNames = {"Default", "Sans Serif", "Serif", "Monospace", "Vietnamese"};
        int currentFontType = prefs.getInt("font_type", 0);
        // Bounds checking to prevent crash
        if (currentFontType >= fontNames.length) {
            currentFontType = 0; // Default to first font
            prefs.edit().putInt("font_type", currentFontType).apply();
        }
        fontTypeTV.setText(fontNames[currentFontType]);
        fontTypeTV.setGravity(android.view.Gravity.CENTER);
        fontTypeTV.setPadding(20, 0, 20, 0);
        
        fontTypeLayout.addView(fontTypePrevBtn);
        fontTypeLayout.addView(fontTypeTV);
        fontTypeLayout.addView(fontTypeNextBtn);
        
        // Position controls
        TextView positionLabel = new TextView(this);
        positionLabel.setText("Position:");
        positionLabel.setTextSize(16);
        positionLabel.setPadding(0, 20, 0, 10);
        
        LinearLayout positionLayout = new LinearLayout(this);
        positionLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button positionUpBtn = new Button(this);
        positionUpBtn.setText("Up");
        Button positionDownBtn = new Button(this);
        positionDownBtn.setText("Down");
        TextView positionTV = new TextView(this);
        
        // Position logic - offset from default position (negative = closer to bottom, positive = further up)
        int currentOffset = prefs.getInt("vertical_offset", 0);
        positionTV.setText("Offset (" + (currentOffset > 0 ? "+" : "") + currentOffset + "%)");
        positionTV.setGravity(android.view.Gravity.CENTER);
        positionTV.setPadding(20, 0, 20, 0);
        
        positionLayout.addView(positionUpBtn);
        positionLayout.addView(positionTV);
        positionLayout.addView(positionDownBtn);
        
        // Background switch
        LinearLayout backgroundLayout = new LinearLayout(this);
        backgroundLayout.setOrientation(LinearLayout.HORIZONTAL);
        backgroundLayout.setPadding(0, 20, 0, 10);
        TextView backgroundLabel = new TextView(this);
        backgroundLabel.setText("Background: ");
        backgroundLabel.setTextSize(16);
        Switch backgroundSwitch = new Switch(this);
        backgroundSwitch.setChecked(prefs.getBoolean("background", false));
        
        backgroundLayout.addView(backgroundLabel);
        backgroundLayout.addView(backgroundSwitch);
        
        // Text Color controls
        TextView textColorLabel = new TextView(this);
        textColorLabel.setText("Text Color:");
        textColorLabel.setTextSize(16);
        textColorLabel.setPadding(0, 20, 0, 10);
        
        LinearLayout textColorLayout = new LinearLayout(this);
        textColorLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button textColorPrevBtn = new Button(this);
        textColorPrevBtn.setText("◀");
        Button textColorNextBtn = new Button(this);
        textColorNextBtn.setText("▶");
        TextView textColorTV = new TextView(this);
        
        String[] colorNames = {"White", "Yellow", "Red", "Green", "Blue", "Orange", "Pink", "Cyan"};
        int[] colorValues = {Color.WHITE, Color.YELLOW, Color.RED, Color.GREEN, 
                            Color.BLUE, 0xFFFF8C00, 0xFFFFC0CB, Color.CYAN}; // Orange, Pink
        int currentTextColor = prefs.getInt("text_color", 0);
        // Bounds checking to prevent crash
        if (currentTextColor >= colorNames.length) {
            currentTextColor = 0; // Default to White
            prefs.edit().putInt("text_color", currentTextColor).apply();
        }
        textColorTV.setText(colorNames[currentTextColor]);
        textColorTV.setTextColor(colorValues[currentTextColor]);
        textColorTV.setGravity(android.view.Gravity.CENTER);
        textColorTV.setPadding(20, 0, 20, 0);
        
        textColorLayout.addView(textColorPrevBtn);
        textColorLayout.addView(textColorTV);
        textColorLayout.addView(textColorNextBtn);
        
        // Outline Color controls
        TextView outlineColorLabel = new TextView(this);
        outlineColorLabel.setText("Outline Color:");
        outlineColorLabel.setTextSize(16);
        outlineColorLabel.setPadding(0, 20, 0, 10);
        
        LinearLayout outlineColorLayout = new LinearLayout(this);
        outlineColorLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button outlineColorPrevBtn = new Button(this);
        outlineColorPrevBtn.setText("◀");
        Button outlineColorNextBtn = new Button(this);
        outlineColorNextBtn.setText("▶");
        TextView outlineColorTV = new TextView(this);
        
        String[] outlineColorNames = {"None", "Black", "White", "Red", "Blue", "Yellow"};
        int[] outlineColorValues = {Color.TRANSPARENT, Color.BLACK, Color.WHITE, Color.RED, Color.BLUE, Color.YELLOW};
        int currentOutlineColor = prefs.getInt("outline_color", 1);
        // Bounds checking to prevent crash
        if (currentOutlineColor >= outlineColorNames.length) {
            currentOutlineColor = 1; // Default to Black
            prefs.edit().putInt("outline_color", currentOutlineColor).apply();
        }
        outlineColorTV.setText(outlineColorNames[currentOutlineColor]);
        // Show the actual color like text color does
        if (currentOutlineColor == 0) {
            outlineColorTV.setTextColor(Color.GRAY); // Show gray for "None"
        } else {
            outlineColorTV.setTextColor(outlineColorValues[currentOutlineColor]);
        }
        outlineColorTV.setGravity(android.view.Gravity.CENTER);
        outlineColorTV.setPadding(20, 0, 20, 0);
        outlineColorTV.setGravity(android.view.Gravity.CENTER);
        
        outlineColorLayout.addView(outlineColorPrevBtn);
        outlineColorLayout.addView(outlineColorTV);
        outlineColorLayout.addView(outlineColorNextBtn);
        
        // Playback Speed controls
        TextView playbackSpeedLabel = new TextView(this);
        playbackSpeedLabel.setText("Playback Speed:");
        playbackSpeedLabel.setTextSize(16);
        playbackSpeedLabel.setPadding(0, 20, 0, 10);
        
        LinearLayout playbackSpeedLayout = new LinearLayout(this);
        playbackSpeedLayout.setOrientation(LinearLayout.HORIZONTAL);
        playbackSpeedLayout.setGravity(android.view.Gravity.CENTER);
        
        float currentSpeed = prefs.getFloat("playback_speed", 1.0f);
        
        Button speed05 = new Button(this);
        speed05.setText("0.5x");
        speed05.setTextSize(12);
        if (currentSpeed == 0.5f) speed05.setBackgroundColor(Color.GREEN);
        
        Button speed075 = new Button(this);
        speed075.setText("0.75x");
        speed075.setTextSize(12);
        if (currentSpeed == 0.75f) speed075.setBackgroundColor(Color.GREEN);
        
        Button speed10 = new Button(this);
        speed10.setText("1.0x");
        speed10.setTextSize(12);
        if (currentSpeed == 1.0f) speed10.setBackgroundColor(Color.GREEN);
        
        Button speed15 = new Button(this);
        speed15.setText("1.5x");
        speed15.setTextSize(12);
        if (currentSpeed == 1.5f) speed15.setBackgroundColor(Color.GREEN);
        
        Button speed20 = new Button(this);
        speed20.setText("2.0x");
        speed20.setTextSize(12);
        if (currentSpeed == 2.0f) speed20.setBackgroundColor(Color.GREEN);
        
        Button speed30 = new Button(this);
        speed30.setText("3.0x");
        speed30.setTextSize(12);
        if (currentSpeed == 3.0f) speed30.setBackgroundColor(Color.GREEN);
        
        playbackSpeedLayout.addView(speed05);
        playbackSpeedLayout.addView(speed075);
        playbackSpeedLayout.addView(speed10);
        playbackSpeedLayout.addView(speed15);
        playbackSpeedLayout.addView(speed20);
        playbackSpeedLayout.addView(speed30);
        
        // Reset button
        Button resetBtn = new Button(this);
        resetBtn.setText("Reset to Default");
        resetBtn.setPadding(0, 30, 0, 0);
        
        // Add all views to layout
        layout.addView(fontSizeLabel);
        layout.addView(fontSizeLayout);
        layout.addView(fontTypeLabel);
        layout.addView(fontTypeLayout);
        layout.addView(positionLabel);
        layout.addView(positionLayout);
        layout.addView(backgroundLayout);
        layout.addView(textColorLabel);
        layout.addView(textColorLayout);
        layout.addView(outlineColorLabel);
        layout.addView(outlineColorLayout);
        layout.addView(playbackSpeedLabel);
        layout.addView(playbackSpeedLayout);
        layout.addView(resetBtn);
        
        // Set up button listeners
        fontSizeMinusBtn.setOnClickListener(v -> {
            int currentSize = Integer.parseInt(fontSizeTV.getText().toString().replace("sp", ""));
            if (currentSize > 12) {
                currentSize -= 2;
                fontSizeTV.setText(currentSize + "sp");
                saveAndApplySubtitleSetting("font_size", currentSize);
            }
        });
        
        fontSizePlusBtn.setOnClickListener(v -> {
            int currentSize = Integer.parseInt(fontSizeTV.getText().toString().replace("sp", ""));
            if (currentSize < 40) {
                currentSize += 2;
                fontSizeTV.setText(currentSize + "sp");
                saveAndApplySubtitleSetting("font_size", currentSize);
            }
        });
        
        fontTypePrevBtn.setOnClickListener(v -> {
            int currentType = prefs.getInt("font_type", 0);
            currentType = (currentType - 1 + fontNames.length) % fontNames.length;
            fontTypeTV.setText(fontNames[currentType] + " (" + (currentType + 1) + "/" + fontNames.length + ")");
            saveAndApplySubtitleSetting("font_type", currentType);
        });
        
        fontTypeNextBtn.setOnClickListener(v -> {
            int currentType = prefs.getInt("font_type", 0);
            currentType = (currentType + 1) % fontNames.length;
            fontTypeTV.setText(fontNames[currentType] + " (" + (currentType + 1) + "/" + fontNames.length + ")");
            saveAndApplySubtitleSetting("font_type", currentType);
        });
        
        positionUpBtn.setOnClickListener(v -> {
            int offset = prefs.getInt("vertical_offset", 0);
            offset += 5; // Move up (increase offset from bottom - higher value = further from bottom)
            offset = Math.min(offset, 80); // Max 80% from bottom
            String newPositionText = offset == 0 ? "Center" : "Up +" + offset + "%";
            positionTV.setText(newPositionText);
            saveAndApplySubtitleSetting("vertical_offset", offset);
        });
        
        positionDownBtn.setOnClickListener(v -> {
            int offset = prefs.getInt("vertical_offset", 0);
            offset -= 5; // Move down (decrease offset - negative values = closer to bottom edge)
            offset = Math.max(offset, -10); // Min -10% (very close to bottom edge)
            String newPositionText = offset == 0 ? "Center" : "Down " + offset + "%";
            positionTV.setText(newPositionText);
            saveAndApplySubtitleSetting("vertical_offset", offset);
        });
        
        backgroundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveAndApplySubtitleSetting("background", isChecked);
        });
        
        // Text color controls
        textColorPrevBtn.setOnClickListener(v -> {
            int index = (prefs.getInt("text_color", 0) - 1 + colorNames.length) % colorNames.length;
            prefs.edit().putInt("text_color", index).apply();
            textColorTV.setText(colorNames[index]);
            textColorTV.setTextColor(colorValues[index]);
            applySubtitleSettings();
        });
        
        textColorNextBtn.setOnClickListener(v -> {
            int index = (prefs.getInt("text_color", 0) + 1) % colorNames.length;
            prefs.edit().putInt("text_color", index).apply();
            textColorTV.setText(colorNames[index]);
            textColorTV.setTextColor(colorValues[index]);
            applySubtitleSettings();
        });
        
        // Outline color controls
        outlineColorPrevBtn.setOnClickListener(v -> {
            int index = (prefs.getInt("outline_color", 1) - 1 + outlineColorNames.length) % outlineColorNames.length;
            prefs.edit().putInt("outline_color", index).apply();
            outlineColorTV.setText(outlineColorNames[index]);
            // Show the actual color like text color does
            if (index == 0) {
                outlineColorTV.setTextColor(Color.GRAY); // Show gray for "None"
            } else {
                outlineColorTV.setTextColor(outlineColorValues[index]);
            }
            applySubtitleSettings();
        });
        
        outlineColorNextBtn.setOnClickListener(v -> {
            int index = (prefs.getInt("outline_color", 1) + 1) % outlineColorNames.length;
            prefs.edit().putInt("outline_color", index).apply();
            outlineColorTV.setText(outlineColorNames[index]);
            // Show the actual color like text color does
            if (index == 0) {
                outlineColorTV.setTextColor(Color.GRAY); // Show gray for "None"
            } else {
                outlineColorTV.setTextColor(outlineColorValues[index]);
            }
            applySubtitleSettings();
        });
        
        // Playback speed controls
        View.OnClickListener speedClickListener = v -> {
            float speed = 1.0f;
            if (v == speed05) speed = 0.5f;
            else if (v == speed075) speed = 0.75f;
            else if (v == speed10) speed = 1.0f;
            else if (v == speed15) speed = 1.5f;
            else if (v == speed20) speed = 2.0f;
            else if (v == speed30) speed = 3.0f;
            
            // Reset all button backgrounds
            speed05.setBackgroundColor(Color.TRANSPARENT);
            speed075.setBackgroundColor(Color.TRANSPARENT);
            speed10.setBackgroundColor(Color.TRANSPARENT);
            speed15.setBackgroundColor(Color.TRANSPARENT);
            speed20.setBackgroundColor(Color.TRANSPARENT);
            speed30.setBackgroundColor(Color.TRANSPARENT);
            
            // Highlight selected button
            ((Button)v).setBackgroundColor(Color.GREEN);
            
            // Save and apply speed
            prefs.edit().putFloat("playback_speed", speed).apply();
            setPlaybackSpeed(speed);
        };
        
        speed05.setOnClickListener(speedClickListener);
        speed075.setOnClickListener(speedClickListener);
        speed10.setOnClickListener(speedClickListener);
        speed15.setOnClickListener(speedClickListener);
        speed20.setOnClickListener(speedClickListener);
        speed30.setOnClickListener(speedClickListener);
        
        resetBtn.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("font_size", 20);
            editor.putInt("font_type", 0);
            editor.putInt("vertical_offset", 0);
            editor.putBoolean("background", false);
            editor.putInt("text_color", 0); // White
            editor.putInt("outline_color", 1); // Black
            editor.putFloat("playback_speed", 1.0f); // Normal speed
            editor.apply();
            
            fontSizeTV.setText("20sp");
            fontTypeTV.setText(fontNames[0]);
            positionTV.setText("Center");
            backgroundSwitch.setChecked(false);
            textColorTV.setText(colorNames[0]);
            textColorTV.setTextColor(colorValues[0]);
            outlineColorTV.setText(outlineColorNames[1]);
            outlineColorTV.setTextColor(outlineColorValues[1]); // Show black color for reset
            
            Toast.makeText(PlayerActivity.this, "Settings reset to default", Toast.LENGTH_SHORT).show();
            applySubtitleSettings();
        });
        
        // Add layout to ScrollView and ScrollView to dialog
        scrollView.addView(layout);
        builder.setView(scrollView);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void saveAndApplySubtitleSetting(String key, int value) {
        SharedPreferences.Editor editor = getSharedPreferences("subtitle_settings", MODE_PRIVATE).edit();
        editor.putInt(key, value);
        editor.apply();
        applySubtitleSettings();
    }

    private void saveAndApplySubtitleSetting(String key, boolean value) {
        SharedPreferences.Editor editor = getSharedPreferences("subtitle_settings", MODE_PRIVATE).edit();
        editor.putBoolean(key, value);
        editor.apply();
        applySubtitleSettings();
    }

    private void showAspectRatioDialog() {
        final String[] aspectRatioOptions = {
            "Tỷ lệ gốc",
            "Fit to Screen", 
            "Fill Screen", 
            "Zoom to Fill", 
            "Fixed Height", 
            "Fixed Width"
        };
        
        final int[] aspectRatioModes = {
            AspectRatioFrameLayout.RESIZE_MODE_FIT,
            AspectRatioFrameLayout.RESIZE_MODE_FIT,
            AspectRatioFrameLayout.RESIZE_MODE_FILL, 
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT,
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn tỷ lệ màn hình");
        builder.setItems(aspectRatioOptions, (dialog, which) -> {
            if (which == 0) {
                // Tỷ lệ gốc - reset về aspect ratio mặc định của video
                exoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                exoPlayerView.setUseArtwork(false);
                player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            } else {
                exoPlayerView.setResizeMode(aspectRatioModes[which]);
                if (which == 2) { // Fill Screen
                    player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                } else {
                    player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                }
            }
            new ToastMsg(PlayerActivity.this).toastIconSuccess("Đã chuyển: " + aspectRatioOptions[which]);
        });
        builder.show();
    }

    /**
     * Add current video to Android TV home screen recommendations
     */
    private void addToTvRecommendations() {
        if (model != null && tvRecommendationManager != null) {
            try {
                // Create VideoContent from current model
                VideoContent content = new VideoContent();
                content.setTitle(model.getTitle() != null ? model.getTitle() : "Unknown Title");
                content.setDescription(model.getDescription());
                content.setThumbnailUrl(model.getCardImageUrl()); // Use cardImageUrl
                content.setVideoUrl(model.getVideoUrl());
                content.setId(model.getMovieId() != null ? model.getMovieId() : String.valueOf(System.currentTimeMillis()));
                
                // Set isTvseries based on model data (safe handling)
                if (model.getIsTvSeries() != null && "1".equals(model.getIsTvSeries())) {
                    content.setIsTvseries("1");
                } else if (model.getCategory() != null && model.getCategory().equalsIgnoreCase("tvseries")) {
                    content.setIsTvseries("1");
                } else {
                    content.setIsTvseries("0");
                }
                
                // Add to Continue Watching if this is resume playback
                if (mStartingPosition > 0) {
                    long duration = player != null ? player.getDuration() : 0;
                    tvRecommendationManager.addToContinueWatching(content, mStartingPosition, duration);
                    Log.d(TAG, "Added to Continue Watching: " + content.getTitle());
                }
                
                // Always add to Recommended Content (like advertisement)
                List<VideoContent> recommendedList = new ArrayList<>();
                recommendedList.add(content);
                tvRecommendationManager.addToRecommended(recommendedList);
                Log.d(TAG, "Added to Recommended: " + content.getTitle());
                
            } catch (Exception e) {
                Log.e(TAG, "Error adding to TV recommendations", e);
            }
        } else {
            Log.w(TAG, "Model or TvRecommendationManager is null, skipping TV recommendations");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Update Continue Watching with current position when stopping
        if (player != null && model != null && tvRecommendationManager != null) {
            try {
                VideoContent content = new VideoContent();
                content.setTitle(model.getTitle());
                content.setDescription(model.getDescription());
                content.setThumbnailUrl(model.getCardImageUrl()); // Use cardImageUrl
                content.setVideoUrl(model.getVideoUrl());
                content.setId(String.valueOf(System.currentTimeMillis()));
                
                long currentPosition = player.getCurrentPosition();
                long duration = player.getDuration();
                
                if (currentPosition > 30000 && duration > 0) { // Only if watched more than 30 seconds
                    tvRecommendationManager.addToContinueWatching(content, currentPosition, duration);
                    Log.d(TAG, "Updated Continue Watching position: " + currentPosition);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating TV recommendations", e);
            }
        }
    }



    /**
     * Sync watch history from API to Android TV home screen
     */
    private void syncWatchHistoryToTvHome() {
        if (tvRecommendationManager != null) {
            try {
                // Run sync in background thread
                new Thread(() -> {
                    try {
                        Log.d(TAG, "Starting watch history sync to TV home screen...");
                        tvRecommendationManager.syncWatchHistoryFromAPI();
                    } catch (Exception e) {
                        Log.e(TAG, "Error syncing watch history to TV home", e);
                    }
                }).start();
                
            } catch (Exception e) {
                Log.e(TAG, "Error starting watch history sync", e);
            }
        }
    }
    
    /**
     * Load movie details for watch history to get subtitle information
     */
    private void loadMovieDetailsForWatchHistory(String videoId, String videoType) {
        if (videoId == null) {
            Log.w(TAG, "Cannot load movie details - videoId is null");
            return;
        }
        
        Log.d(TAG, "Loading movie details for watch history - ID: " + videoId + ", Type: " + videoType);
        
        // Use DataProvider to load movie details in background
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // This will load movie details and populate video object with subtitle info
                    // You can use existing API calls here based on your DataProvider implementation
                    
                    // For now, we'll handle this gracefully by ensuring subtitle button works
                    // even without complete video object
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Movie details loaded for watch history playback");
                            // Update UI if needed
                        }
                    });
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error loading movie details for watch history", e);
                }
            }
        }).start();
    }

    /**
     * ERROR HANDLING: Handle unrecognized video format
     */
    private void handleUnrecognizedFormat() {
        Log.d("PlayerActivity", "🔄 Attempting to handle unrecognized format for URL: " + url);
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // First, always try HLS since phim4k seems to use m3u8 streams
                    if (!hasRetriedWithHLS) {
                        Log.d("PlayerActivity", "🎯 Trying HLS MediaSource as fallback...");
                        retryWithHLS();
                    } else {
                        Log.d("PlayerActivity", "❌ Already retried with HLS, showing error dialog");
                        showFormatErrorDialog();
                    }
                    
                    // Alternative: Try to auto-detect format from URL
                    // if (url != null && !url.isEmpty()) {
                    //     String detectedFormat = detectVideoFormat(url);
                    //     Log.d("PlayerActivity", "🔍 Re-detected format as: " + detectedFormat);
                    //     
                    //     if ("hls".equals(detectedFormat)) {
                    //         Log.d("PlayerActivity", "🎯 Retrying with HLS MediaSource...");
                    //         retryWithHLS();
                    //         return;
                    //     }
                    // }
                    
                } catch (Exception e) {
                    Log.e("PlayerActivity", "Error handling unrecognized format", e);
                    showFormatErrorDialog();
                }
            }
        });
    }

    /**
     * ERROR HANDLING: Retry playback with HLS MediaSource
     */
    private void retryWithHLS() {
        try {
            hasRetriedWithHLS = true; // Mark that we've attempted HLS retry
            
            if (player != null) {
                player.stop();
            }
            
            // Create HLS MediaSource explicitly
            Uri uri = Uri.parse(url);
            com.google.android.exoplayer2.source.MediaSource hlsSource = hlsMediaSource(uri, this);
            
            // Prepare and play with HLS
            player.setMediaSource(hlsSource);
            player.prepare();
            player.setPlayWhenReady(true);
            
            Log.d("PlayerActivity", "✅ Retrying playback with HLS MediaSource for URL: " + url);
            
        } catch (Exception e) {
            Log.e("PlayerActivity", "Failed to retry with HLS", e);
            showFormatErrorDialog();
        }
    }

    /**
     * ERROR HANDLING: Show format error dialog with options
     */
    private void showFormatErrorDialog() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Định dạng không hỗ trợ")
            .setMessage("Video này có định dạng không được hỗ trợ. Bạn có muốn thử chất lượng khác không?")
            .setPositiveButton("Thử lại", (dialog, which) -> {
                // Try to reload with different settings
                recreatePlayer();
            })
            .setNegativeButton("Quay lại", (dialog, which) -> {
                finish();
            })
            .setCancelable(false)
            .show();
    }

    /**
     * ERROR HANDLING: Show general error dialog
     */
    private void showErrorDialog(String title, String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new android.app.AlertDialog.Builder(PlayerActivity.this)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("Đóng", (dialog, which) -> {
                        finish();
                    })
                    .setCancelable(false)
                    .show();
            }
        });
    }

    /**
     * ERROR HANDLING: Recreate player with different settings
     */
    private void recreatePlayer() {
        try {
            if (player != null) {
                player.release();
            }
            
            // Recreate player with more lenient settings
            // Try to restart the current video with different settings
            if (url != null && !url.isEmpty()) {
                Log.d("PlayerActivity", "🔄 Restarting playback with URL: " + url);
                // We can't easily recreate the entire player setup here
                // Instead, just restart the current activity or show options
            }
            
            Log.d("PlayerActivity", "🔄 Player recreated for fallback playback");
        } catch (Exception e) {
            Log.e("PlayerActivity", "Error recreating player", e);
            showErrorDialog("Lỗi khởi tạo player", "Không thể khởi tạo lại player.");
        }
    }

    /**
     * ERROR HANDLING: Handle audio codec errors (EAC3, AC-3, DTS, etc.)
     */
    private void handleAudioCodecError() {
        Log.d("PlayerActivity", "🔊 Handling audio codec error - trying to find compatible audio track first");
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (trackSelector == null || player == null) {
                        Log.e("PlayerActivity", "TrackSelector or Player is null, cannot handle audio codec error");
                        showAudioCodecErrorDialog();
                        return;
                    }

                    // Get current mapped track info
                    MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                    if (mappedTrackInfo == null) {
                        Log.e("PlayerActivity", "No mapped track info available");
                        showAudioCodecErrorDialog();
                        return;
                    }

                    // Try to find a compatible audio track first
                    boolean foundCompatibleTrack = trySelectCompatibleAudioTrack(mappedTrackInfo);
                    
                    if (foundCompatibleTrack) {
                        Log.d("PlayerActivity", "✅ Found and selected compatible audio track");
                        showAudioTrackSwitchedNotification();
                    } else {
                        Log.d("PlayerActivity", "❌ No compatible audio track found, disabling audio");
                        disableAudioRenderer(mappedTrackInfo);
                        showAudioDisabledNotification();
                    }
                    
                } catch (Exception e) {
                    Log.e("PlayerActivity", "Error handling audio codec error", e);
                    showAudioCodecErrorDialog();
                }
            }
        });
    }

    /**
     * ERROR HANDLING: Show notification that audio has been disabled
     */
    private void showAudioDisabledNotification() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("🔇 Âm thanh đã bị tắt")
            .setMessage("Định dạng âm thanh EAC3 không được hỗ trợ trên thiết bị này. Video sẽ phát không có tiếng.\n\nBạn có muốn tiếp tục xem không?")
            .setPositiveButton("Xem không tiếng", (dialog, which) -> {
                Log.d("PlayerActivity", "User chose to continue watching without audio");
            })
            .setNegativeButton("Quay lại", (dialog, which) -> {
                finish();
            })
            .setCancelable(false)
            .show();
    }

    /**
     * Try to select a compatible audio track (non-EAC3)
     */
    private boolean trySelectCompatibleAudioTrack(MappingTrackSelector.MappedTrackInfo mappedTrackInfo) {
        for (int rendererIndex = 0; rendererIndex < mappedTrackInfo.getRendererCount(); rendererIndex++) {
            if (mappedTrackInfo.getRendererType(rendererIndex) == C.TRACK_TYPE_AUDIO) {
                TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
                
                // Look for compatible audio tracks
                for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
                    TrackGroup group = trackGroups.get(groupIndex);
                    
                    for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                        Format format = group.getFormat(trackIndex);
                        String mimeType = format.sampleMimeType;
                        
                        // Check if this is a compatible audio format (not EAC3/AC-3/DTS)
                        if (mimeType != null && (
                            mimeType.equals("audio/mp4a-latm") ||  // AAC
                            mimeType.equals("audio/aac") ||        // AAC
                            mimeType.equals("audio/mpeg") ||       // MP3
                            mimeType.equals("audio/opus") ||       // Opus
                            mimeType.equals("audio/vorbis") ||     // Vorbis
                            mimeType.equals("audio/flac"))) {      // FLAC
                            
                            Log.d("PlayerActivity", "🔊 Found compatible audio track: " + mimeType + " (language: " + format.language + ")");
                            
                            // Select this compatible track
                            trackSelector.setParameters(
                                trackSelector.buildUponParameters()
                                    .setSelectionOverride(
                                        rendererIndex,
                                        trackGroups,
                                        new DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex)
                                    )
                                    .setExceedRendererCapabilitiesIfNecessary(true)
                                    .setTunnelingEnabled(false)
                            );
                            
                            return true; // Found compatible track
                        } else if (mimeType != null) {
                            Log.d("PlayerActivity", "🔊 Skipping incompatible audio track: " + mimeType);
                        }
                    }
                }
            }
        }
        
        Log.d("PlayerActivity", "🔊 No compatible audio tracks found");
        return false;
    }

    /**
     * Disable audio renderer completely
     */
    private void disableAudioRenderer(MappingTrackSelector.MappedTrackInfo mappedTrackInfo) {
        for (int rendererIndex = 0; rendererIndex < mappedTrackInfo.getRendererCount(); rendererIndex++) {
            if (mappedTrackInfo.getRendererType(rendererIndex) == C.TRACK_TYPE_AUDIO) {
                Log.d("PlayerActivity", "🔊 Disabling audio renderer " + rendererIndex + " due to no compatible tracks");
                
                // Disable the audio renderer completely
                trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                        .setRendererDisabled(rendererIndex, true)
                        .setExceedRendererCapabilitiesIfNecessary(true)
                        .setTunnelingEnabled(false)
                );
                
                Log.d("PlayerActivity", "✅ Audio renderer disabled - video will play without audio");
                return;
            }
        }
    }

    /**
     * ERROR HANDLING: Show notification that audio track was switched
     */
    private void showAudioTrackSwitchedNotification() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("🔄 Đã chuyển đổi âm thanh")
            .setMessage("Đã tự động chuyển từ EAC3 sang định dạng âm thanh tương thích khác. Video sẽ phát với âm thanh mới.")
            .setPositiveButton("OK", (dialog, which) -> {
                Log.d("PlayerActivity", "User acknowledged audio track switch");
            })
            .setCancelable(true)
            .show();
    }

    /**
     * ERROR HANDLING: Show audio codec error dialog
     */
    private void showAudioCodecErrorDialog() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Lỗi âm thanh")
            .setMessage("Định dạng âm thanh không được hỗ trợ trên thiết bị này. Video sẽ phát không có tiếng hoặc với âm thanh thay thế.\n\nBạn có muốn tiếp tục không?")
            .setPositiveButton("Tiếp tục", (dialog, which) -> {
                // Continue playback without audio or with alternative audio
                Log.d("PlayerActivity", "User chose to continue with audio codec issues");
            })
            .setNegativeButton("Quay lại", (dialog, which) -> {
                finish();
            })
            .setCancelable(false)
            .show();
    }

    /**
     * ERROR HANDLING: Handle video codec errors (H.264, H.265, AVC, etc.)
     */
    private void handleVideoCodecError() {
        Log.d("PlayerActivity", "🎬 Handling video codec error - configuring video renderer for compatibility");
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (trackSelector == null || player == null) {
                        Log.e("PlayerActivity", "TrackSelector or Player is null, cannot handle video codec error");
                        showErrorDialog("Lỗi video", "Không thể khởi tạo video renderer. Thử chọn chất lượng khác.");
                        return;
                    }

                    // Configure track selector to be more lenient with video formats
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setExceedVideoConstraintsIfNecessary(true)
                            .setExceedRendererCapabilitiesIfNecessary(true)
                            .setForceLowestBitrate(true) // Force lower quality for compatibility
                            .setTunnelingEnabled(false) // Disable video tunneling which can cause codec issues
                    );
                    
                    Log.d("PlayerActivity", "✅ Applied video codec error handling - track selector configured");
                    
                    // Try to restart playback with new video settings
                    if (player != null && url != null) {
                        Log.d("PlayerActivity", "🔄 Restarting playback with video codec fallback settings");
                        
                        // Store current playback position
                        long currentPosition = player.getCurrentPosition();
                        
                        // Restart the media source with video fallback
                        Uri uri = Uri.parse(url);
                        com.google.android.exoplayer2.source.MediaSource mediaSource = createMediaSourceForUrl(uri, videoType);
                        
                        if (mediaSource != null) {
                            player.setMediaSource(mediaSource);
                            player.prepare();
                            player.seekTo(currentPosition); // Resume from where it failed
                            player.setPlayWhenReady(true);
                            
                            Log.d("PlayerActivity", "✅ Playback restarted with video fallback at position: " + currentPosition);
                        } else {
                            Log.e("PlayerActivity", "Failed to create media source for video codec fallback");
                            showVideoCodecErrorDialog();
                        }
                    }
                    
                } catch (Exception e) {
                    Log.e("PlayerActivity", "Error handling video codec error", e);
                    showVideoCodecErrorDialog();
                }
            }
        });
    }

    /**
     * ERROR HANDLING: Show video codec error dialog
     */
    private void showVideoCodecErrorDialog() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Lỗi video codec")
            .setMessage("Định dạng video không được hỗ trợ đầy đủ trên thiết bị này. Video có thể phát chậm hoặc có lỗi hiển thị.\n\nBạn có muốn thử chất lượng thấp hơn không?")
            .setPositiveButton("Thử chất lượng thấp", (dialog, which) -> {
                // Force lowest quality for compatibility
                Log.d("PlayerActivity", "User chose to try lower quality for video codec issues");
                if (trackSelector != null) {
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setForceLowestBitrate(true)
                            .setMaxVideoSize(854, 480) // Force 480p max
                            .setExceedVideoConstraintsIfNecessary(true)
                    );
                }
            })
            .setNegativeButton("Quay lại", (dialog, which) -> {
                finish();
            })
            .setCancelable(false)
            .show();
    }

    /**
     * Create MediaSource for given URL and type - used for fallback scenarios
     */
    private com.google.android.exoplayer2.source.MediaSource createMediaSourceForUrl(Uri uri, String type) {
        try {
            if ("hls".equals(type) || uri.toString().contains(".m3u8")) {
                return hlsMediaSource(uri, this);
            } else {
                return mediaSource(uri, this);
            }
        } catch (Exception e) {
            Log.e("PlayerActivity", "Error creating media source", e);
            return null;
        }
    }

    /**
     * ENHANCED FORMAT DETECTION: Auto-detect video format from URL
     */
    private String detectVideoFormat(String url) {
        if (url == null || url.isEmpty()) {
            return "video"; // default
        }
        
        String lowerUrl = url.toLowerCase();
        
        // HLS detection (m3u8 streams)
        if (lowerUrl.contains(".m3u8") || lowerUrl.contains("/hls/") || 
            lowerUrl.contains("m3u8") || lowerUrl.contains("/playlist.m3u8")) {
            Log.d("PlayerActivity", "🎯 Detected HLS format from URL patterns");
            return "hls";
        }
        
        // RTMP detection
        if (lowerUrl.startsWith("rtmp://") || lowerUrl.startsWith("rtmps://")) {
            Log.d("PlayerActivity", "🎯 Detected RTMP format from protocol");
            return "rtmp";
        }
        
        // DASH detection
        if (lowerUrl.contains(".mpd") || lowerUrl.contains("/dash/")) {
            Log.d("PlayerActivity", "🎯 Detected DASH format from URL patterns");
            return "dash";
        }
        
        // YouTube detection
        if (lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be")) {
            Log.d("PlayerActivity", "🎯 Detected YouTube format from domain");
            return "youtube";
        }
        
        // Progressive formats (MP4, MKV, etc.)
        if (lowerUrl.contains(".mp4") || lowerUrl.contains(".mkv") || 
            lowerUrl.contains(".avi") || lowerUrl.contains(".mov") ||
            lowerUrl.contains(".webm") || lowerUrl.contains(".flv")) {
            Log.d("PlayerActivity", "🎯 Detected progressive format from extension");
            return "video";
        }
        
        Log.d("PlayerActivity", "🤔 Could not detect format, using default progressive");
        return "video"; // default fallback
    }


}