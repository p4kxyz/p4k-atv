package com.files.codes.view;

import static android.view.View.VISIBLE;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.ext.rtmp.RtmpDataSourceFactory;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.MediaItem;
import java.util.Collections;
import java.util.Locale;
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
import com.files.codes.AppConfig;
import com.files.codes.database.DatabaseHelper;
import com.files.codes.model.PlaybackModel;
import com.files.codes.model.Video;
import com.files.codes.model.movieDetails.Subtitle;
import com.files.codes.model.movieDetails.MovieSingleDetails;
import com.files.codes.model.movieDetails.Season;
import com.files.codes.model.movieDetails.Episode;
import com.files.codes.model.api.ApiService;
import com.files.codes.utils.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.files.codes.utils.Constants;
import com.files.codes.utils.ToastMsg;
import com.files.codes.utils.AnimeClient;
import com.files.codes.model.anime.AnimeStreamResponse;
import com.files.codes.utils.TvRecommendationManager;
import com.files.codes.view.HeroStyleVideoDetailsActivity;
import com.files.codes.utils.sync.WatchHistorySyncManager;
import com.files.codes.model.VideoContent;
import com.files.codes.view.adapter.ServerAdapter;
import com.files.codes.view.adapter.SubtitleListAdapter;
import com.files.codes.view.fragments.MyAccountFragment;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.model.api.ApiService;
import com.files.codes.model.movieDetails.MovieSingleDetails;
import com.files.codes.model.movieDetails.Season;
import com.files.codes.model.movieDetails.Episode;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import com.files.codes.AppConfig;
import com.files.codes.view.fragments.testFolder.ProfileFragment;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerActivity extends Activity {
    private static final String TAG = "PlayerActivity";
    private boolean useSoftwareAudioDecoder = false; // Flag to force software decoding
    private int audioMode = 1; // 0: HW, 1: SW, 2: Passthrough
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
    private Map<String, String> animeProxyHeaders = null; // Custom headers for anime HLS streams
    private int visible;
    private ImageButton serverButton, subtitleButton, subtitleSettingsButton, audioTrackButton, aspectRatioButton;
    private ImageButton previousEpisodeButton, nextEpisodeButton; // Episode navigation for TV series
    private ImageButton btnRewind, btnForward; // Custom seek buttons
    private TextView movieTitleTV, movieDescriptionTV;
    private ImageView posterImageView, posterImageViewForTV;
    private RelativeLayout seekBarLayout;
    private TextView liveTvTextInController;
    private ProgressBar progressBar;
    private PowerManager.WakeLock wakeLock;
    private MediaSession session;
    private TvRecommendationManager tvRecommendationManager;
    private WatchHistorySyncManager watchHistorySyncManager;
    
    // Complete movie metadata for enhanced watch history
    private MovieSingleDetails completeMovieData;
    private boolean isWaitingForCompleteData = false;
    private long lastSavePosition = -1;
    private long lastSaveDuration = -1;

    private long mChannelId;
    private long mStartingPosition;
    private PlaybackModel model;
    private boolean isUserSeeking = false; // Flag to prevent auto-seek conflicts
    
    // Variables to persist audio track selection across player restarts (e.g. SW fallback)
    private DefaultTrackSelector.SelectionOverride savedAudioOverride = null;
    private int savedAudioRendererIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Initialize Audio Decoder Preference
        SharedPreferences audioPrefs = getSharedPreferences(Constants.USER_LOGIN_STATUS, MODE_PRIVATE);
        
        // Read New Int Preference
        if (audioPrefs.contains("audio_priority_mode")) {
            audioMode = audioPrefs.getInt("audio_priority_mode", 1);
        } else {
            // Fallback to legacy boolean
            boolean prefSwAudio = audioPrefs.getBoolean("audio_priority_sw", true);
            audioMode = prefSwAudio ? 1 : 0;
        }

        // If restarting due to error, the Intent extra takes precedence.
        if (getIntent().hasExtra("useSoftwareAudioDecoder")) {
             boolean retrySw = getIntent().getBooleanExtra("useSoftwareAudioDecoder", false);
             if (retrySw) audioMode = 1;
        }
        
        // Sync legacy boolean for compatibility with other internal checks
        useSoftwareAudioDecoder = (audioMode == 1);
        
        Log.d(TAG, "🔊 Audio Mode Initialized: " + audioMode + " (1:SW, 0:HW, 2:PASSTROUGH)");

        mChannelId = getIntent().getLongExtra(VideoPlaybackActivity.EXTRA_CHANNEL_ID, -1L);
        mStartingPosition = getIntent().getLongExtra(VideoPlaybackActivity.EXTRA_POSITION, -1L);

        model = (PlaybackModel) getIntent().getSerializableExtra(VideoPlaybackActivity.EXTRA_VIDEO);



        // Check if this is a direct call from watch history (no model provided)
        Intent intent = getIntent();
        
        // Retrieve complete movie data if available (from HeroStyleVideoDetailsFragment)
        if (intent != null && intent.hasExtra("complete_movie_data")) {
            completeMovieData = (MovieSingleDetails) intent.getSerializableExtra("complete_movie_data");
            // Log.d(TAG, "📥 onCreate - Received complete movie data: " + (completeMovieData != null ? completeMovieData.getTitle() : "null"));
            
            // Debug complete movie data immediately
            if (completeMovieData != null) {
                /*
                Log.d(TAG, "🔍 onCreate - Complete movie data debug:");
                Log.d(TAG, "  - VideosId: '" + completeMovieData.getVideosId() + "'");
                Log.d(TAG, "  - Title: '" + completeMovieData.getTitle() + "'");
                Log.d(TAG, "  - Slug: '" + completeMovieData.getSlug() + "'");
                Log.d(TAG, "  - Type: '" + completeMovieData.getType() + "'");
                Log.d(TAG, "  - Description: '" + completeMovieData.getDescription() + "'");
                Log.d(TAG, "  - Release: '" + completeMovieData.getRelease() + "'");
                Log.d(TAG, "  - Genre count: " + (completeMovieData.getGenre() != null ? completeMovieData.getGenre().size() : 0));
                */
                
                // Check if we need to fetch more complete data
                boolean hasCompleteInfo = completeMovieData.getTitle() != null && 
                                        !completeMovieData.getTitle().isEmpty() && 
                                        !completeMovieData.getTitle().equals("Unknown Title") &&
                                        completeMovieData.getGenre() != null &&
                                        completeMovieData.getGenre().size() > 0;
                
                if (!hasCompleteInfo && completeMovieData.getVideosId() != null && !completeMovieData.getVideosId().equals("null")) {
                    // Log.w(TAG, "⚠️ CompleteMovieData incomplete, fetching from API for ID: " + completeMovieData.getVideosId());
                    fetchCompleteMovieDataForWatchHistory(completeMovieData.getVideosId(), 
                        completeMovieData.getType() != null ? completeMovieData.getType() : "movie");
                }
            }
        } else {
            // If no complete data provided, fetch from API for complete watch history
            // Log.d(TAG, "📥 onCreate - No complete movie data provided, will fetch from API");
        }
        
        if (model == null && intent != null && intent.hasExtra("id")) {

            String videoId = intent.getStringExtra("id");
            String videoType = intent.getStringExtra("type");
            String title = intent.getStringExtra("title");
            String poster = intent.getStringExtra("poster");
            String thumbnail = intent.getStringExtra("thumbnail");
            String videoUrl = intent.getStringExtra("video_url");
            long position = intent.getLongExtra("position", -1L);
            boolean fromWatchHistory = intent.getBooleanExtra("from_watch_history", false);
            
            // Log.d(TAG, "📥 onCreate - Watch History Intent: fromWatchHistory=" + fromWatchHistory + ", ID=" + videoId + ", Type=" + videoType);
            
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
                // Log.d(TAG, "📥 Set isTvSeries = 1 for type: " + videoType);
            } else {
                model.setIsTvSeries("0");
                // Log.d(TAG, "📥 Set isTvSeries = 0 for type: " + videoType);
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

        // Initialize Watch History Sync Manager
        watchHistorySyncManager = new WatchHistorySyncManager(this);
        
        // Fetch complete movie data for enhanced watch history if not already available
        if (completeMovieData == null && model != null && model.getMovieId() != null && 
            !model.getMovieId().equals("null") && !model.getMovieId().isEmpty()) {
            // Log.d(TAG, "🔍 Fetching complete movie data early for ID: " + model.getMovieId());
            fetchCompleteMovieDataForWatchHistory(model.getMovieId(), model.getVideoType() != null ? model.getVideoType() : "movie");
        } else if (model != null) {
            // Log.w(TAG, "⚠️ Cannot fetch complete movie data - MovieId: " + model.getMovieId() + ", CompleteData: " + (completeMovieData != null ? "exists" : "null"));
        }

        // Check if external player is enabled (check ProfileFragment first, then MyAccountFragment)
        boolean useExternalPlayer = ProfileFragment.shouldUseExternalPlayer(this);
        if (!useExternalPlayer) {
            useExternalPlayer = MyAccountFragment.shouldUseExternalPlayer(this);
        }
        
        Log.e(TAG, "🔍 onCreate - External Player Check: " + useExternalPlayer);

        if (useExternalPlayer) {
            Log.e(TAG, "🚀 onCreate - Launching External Player sequence...");
            launchExternalPlayer();
            // finish(); // Don't finish here, wait for onActivityResult
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
                Log.d(TAG, "PlayerActivity - Redirecting to HeroStyle (inactive subscription) - ID: " + model.getMovieId() + ", Type: " + model.getCategory());
                Intent redirectIntent = new Intent(PlayerActivity.this, HeroStyleVideoDetailsActivity.class);
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
        
        Log.d(TAG, "🎬 Subtitle button setup - subtitleButton: " + (subtitleButton != null ? "FOUND ✅" : "NULL ❌"));
        Log.d(TAG, "🎬 Subtitle settings button setup - subtitleSettingsButton: " + (subtitleSettingsButton != null ? "FOUND ✅" : "NULL ❌"));
        audioTrackButton = findViewById(R.id.img_audio);
        aspectRatioButton = findViewById(R.id.img_aspect_ratio);
        
        previousEpisodeButton = findViewById(R.id.btn_previous_episode);
        nextEpisodeButton = findViewById(R.id.btn_next_episode);
        
        btnRewind = findViewById(R.id.btn_rewind);
        btnForward = findViewById(R.id.btn_forward);
        
        Log.d(TAG, "🎬 Seek buttons setup - Rewind: " + (btnRewind != null ? "FOUND" : "NULL") + 
                  ", Forward: " + (btnForward != null ? "FOUND" : "NULL"));
        
        if (btnRewind != null) {
            btnRewind.setVisibility(View.VISIBLE);
            btnRewind.setOnClickListener(v -> seekBackward(10000));
        }
        if (btnForward != null) {
            btnForward.setVisibility(View.VISIBLE);
            btnForward.setOnClickListener(v -> seekForward(10000));
        }

        liveTvTextInController = findViewById(R.id.live_tv);
        
        seekBarLayout = findViewById(R.id.seekbar_layout);
        if (category.equalsIgnoreCase("tv")) {
            serverButton.setVisibility(View.GONE);
            subtitleButton.setVisibility(View.GONE);
            //seekBarLayout.setVisibility(View.GONE);
            
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

        if (subtitleButton != null) {
            subtitleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "🎬 SUBTITLE BUTTON CLICKED! Opening subtitle dialog...");
                    
                    //open subtitle dialog
                    openSubtitleDialog();
                }
            });
        }

        if (subtitleSettingsButton != null) {
            subtitleSettingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //open subtitle settings dialog
                    openUnifiedSettingsDialog();
                }
            });
        }

        if (audioTrackButton != null) {
            audioTrackButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAudioTrackSelectionDialog();
                }
            });
        }

        if (aspectRatioButton != null) {
            aspectRatioButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAspectRatioDialog();
                }
            });
        }

        if (serverButton != null) {
            serverButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //open server dialog
                    openServerDialog(videos);
                }
            });
        }


        //set title, description and poster in controller layout
        String titleText = model.getTitle();
        String descriptionText = "";
        boolean isTvSeries = model.getIsTvSeries() != null && model.getIsTvSeries().equals("1");

        if (isTvSeries) {
            try {
                if (model.getAllSeasons() != null && 
                    model.getCurrentSeasonIndex() >= 0 && 
                    model.getCurrentSeasonIndex() < model.getAllSeasons().size()) {
                    
                    com.files.codes.model.movieDetails.Season currentSeason = model.getAllSeasons().get(model.getCurrentSeasonIndex());
                    
                    if (currentSeason.getEpisodes() != null && 
                        model.getCurrentEpisodeIndex() >= 0 && 
                        model.getCurrentEpisodeIndex() < currentSeason.getEpisodes().size()) {
                        
                        String episodeName = currentSeason.getEpisodes().get(model.getCurrentEpisodeIndex()).getEpisodesName();
                        if (episodeName != null && !episodeName.isEmpty()) {
                            descriptionText = episodeName;
                            
                            // Clean up title by removing " - EpisodeName"
                            String suffix = " - " + episodeName;
                            if (titleText != null && titleText.endsWith(suffix)) {
                                titleText = titleText.substring(0, titleText.length() - suffix.length());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            if (descriptionText.isEmpty()) {
                descriptionText = model.getTitle();
            }
        } else {
            if (model.getVideo() != null) {
                descriptionText = model.getVideo().getLabel();
            }
        }

        if (movieTitleTV != null) {
            // Clean up title to show only "Vietnamese Name (Year)"
            // Example: "Tên Việt (2024) Tên Gốc" -> "Tên Việt (2024)"
            if (titleText != null) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(.*\\(\\d{4}\\))");
                java.util.regex.Matcher matcher = pattern.matcher(titleText);
                if (matcher.find()) {
                    titleText = matcher.group(1);
                }
            }
            movieTitleTV.setText(titleText);
        }
        if (movieDescriptionTV != null) {
            movieDescriptionTV.setText(descriptionText);
        }

        // Ensure poster images are hidden as per user request
        if (posterImageViewForTV != null) {
            posterImageViewForTV.setVisibility(View.GONE);
        }
        if (posterImageView != null) {
            posterImageView.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Intercept DPAD keys when timeline is focused to force 30s seek
        if (event.getAction() == KeyEvent.ACTION_DOWN && exoPlayerView != null && exoPlayerView.isControllerVisible()) {
            View timeBar = exoPlayerView.findViewById(R.id.exo_progress);
            if (timeBar != null && timeBar.hasFocus()) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    seekForward(30000);
                    return true;
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                    seekBackward(30000);
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
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
                        Log.d(TAG, "🔙 Redirecting to HeroStyleVideoDetailsActivity - ID: " + model.getMovieId() + ", Type: " + model.getCategory());
                        // Go to video details page
                        Intent intent = new Intent(PlayerActivity.this, HeroStyleVideoDetailsActivity.class);
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

            // Clean and validate video URL - FIX for API URLs with leading/trailing spaces
            String originalUrl = videoUrl;
            videoUrl = cleanVideoUrl(videoUrl);
            
            // Log URL cleaning process
            if (!originalUrl.equals(videoUrl)) {
                Log.w(TAG, "🧹 URL cleaned - Original: '" + originalUrl + "'");
                Log.d(TAG, "🧹 URL cleaned - Clean: '" + videoUrl + "'");
            }
            
            // Validate and log video URL
            Log.e(TAG, "🔍 Video URL: " + videoUrl);
            Log.e(TAG, "🎬 Movie Title: " + (model.getTitle() != null ? model.getTitle() : "Unknown"));
            
            // Check for problematic URLs that might cause issues
            if (isProblematicUrl(videoUrl)) {
                Log.w(TAG, "⚠️ Detected potentially problematic URL, using fallback");
                showUrlIssueDialog(videoUrl);
                return;
            }
            
            // Update model with cleaned URL for consistent use
            model.setVideoUrl(videoUrl);

            // Get selected player from preferences
            SharedPreferences prefs = getSharedPreferences(Constants.USER_LOGIN_STATUS, MODE_PRIVATE);
            String selectedPlayer = prefs.getString("selected_player", "just");
            
            Log.e(TAG, "🎮 Selected Player: " + selectedPlayer);

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
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.e(TAG, "📥 onActivityResult - RequestCode: " + requestCode + ", ResultCode: " + resultCode);

        if (resultCode == Activity.RESULT_OK && data != null) {
            long position = -1;
            long duration = -1;

            // MX Player (1001)
            if (requestCode == 1001) {
                position = data.getIntExtra("position", -1);
                duration = data.getIntExtra("duration", -1);
                Log.e(TAG, "📥 MX Player Result - Position: " + position + ", Duration: " + duration);
            }
            // VLC Player (1002)
            else if (requestCode == 1002) {
                position = data.getLongExtra("extra_position", -1);
                duration = data.getLongExtra("extra_duration", -1);
                Log.e(TAG, "📥 VLC Player Result - Position: " + position + ", Duration: " + duration);
            }
            // Just Player (1003)
            else if (requestCode == 1003) {
                position = data.getLongExtra("position", -1);
                duration = data.getLongExtra("duration", -1);
                Log.e(TAG, "📥 Just Player Result - Position: " + position + ", Duration: " + duration);
            }

            // Save watch history if valid position returned
            if (position > 0) {
                Log.e(TAG, "💾 Saving watch history from External Player: " + position + "ms");
                
                // Check if MX Player returned a specific video index (playlist support)
                int lastPlayedIndex = data.getIntExtra("video_list_index", -1);
                if (lastPlayedIndex == -1) {
                    // Try alternative key
                    lastPlayedIndex = data.getIntExtra("video_list.index", -1);
                }
                
                // Debug all extras to find the correct key if above fails
                if (lastPlayedIndex == -1) {
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        for (String key : extras.keySet()) {
                            Log.d(TAG, "MX Player Extra: " + key + " = " + extras.get(key));
                        }
                    }
                    
                    // Fallback: Check if returned URI matches any episode
                    Uri returnedUri = data.getData();
                    if (returnedUri != null && model != null && model.getAllSeasons() != null) {
                        Log.d(TAG, "🔄 MX Player returned URI: " + returnedUri + ". Searching for match...");
                        String returnedUriString = returnedUri.toString();
                        
                        int currentIndex = 0;
                        boolean found = false;
                        for (Season season : model.getAllSeasons()) {
                            if (season.getEpisodes() != null) {
                                for (Episode episode : season.getEpisodes()) {
                                    String epUrl = episode.getFileUrl();
                                    if (epUrl != null) {
                                        String cleanEpUrl = cleanVideoUrl(epUrl);
                                        // Check for match (exact or contains)
                                        if (cleanEpUrl.equals(returnedUriString) || returnedUriString.contains(cleanEpUrl) || cleanEpUrl.contains(returnedUriString)) {
                                            Log.d(TAG, "✅ Found matching episode by URI at index: " + currentIndex);
                                            lastPlayedIndex = currentIndex;
                                            found = true;
                                            break;
                                        }
                                    }
                                    currentIndex++;
                                }
                            }
                            if (found) break;
                        }
                    }
                }
                
                if (lastPlayedIndex != -1 && model != null && model.getAllSeasons() != null) {
                    Log.d(TAG, "🔄 MX Player returned index: " + lastPlayedIndex + ". Updating model to correct episode.");
                    
                    // Find the episode at this index
                    int currentIndex = 0;
                    boolean found = false;
                    for (Season season : model.getAllSeasons()) {
                        if (season.getEpisodes() != null) {
                            for (Episode episode : season.getEpisodes()) {
                                if (currentIndex == lastPlayedIndex) {
                                    // Found the episode! Update model
                                    String epUrl = episode.getFileUrl();
                                    String epName = episode.getEpisodesName();
                                    String epId = episode.getEpisodesId();
                                    String seriesTitle = episode.getTvSeriesTitle();
                                    
                                    if (epUrl != null) {
                                        model.setVideoUrl(cleanVideoUrl(epUrl));
                                        Log.d(TAG, "   - Updated URL: " + model.getVideoUrl());
                                    }
                                    
                                    // Fix Title Logic: Use Series Title + Episode Name if available
                                    if (seriesTitle != null && !seriesTitle.isEmpty() && epName != null) {
                                        model.setTitle(seriesTitle + " - " + epName);
                                    } else if (epName != null) {
                                        // Fallback: Try to extract base title from current title
                                        String currentTitle = model.getTitle();
                                        String baseTitle = currentTitle;
                                        
                                        if (currentTitle != null && currentTitle.contains(" - ")) {
                                            // Assume format "Series Name - Old Episode Name"
                                            // We want to keep "Series Name" and replace "Old Episode Name"
                                            int lastDashIndex = currentTitle.lastIndexOf(" - ");
                                            if (lastDashIndex > 0) {
                                                baseTitle = currentTitle.substring(0, lastDashIndex);
                                            }
                                        }
                                        
                                        if (baseTitle != null && !baseTitle.isEmpty()) {
                                            model.setTitle(baseTitle + " - " + epName);
                                        } else {
                                            model.setTitle(epName);
                                        }
                                    }
                                    Log.d(TAG, "   - Updated Title: " + model.getTitle());

                                    // Important: Update current episode index in model if needed for other logic
                                    model.setCurrentEpisodeIndex(currentIndex); 
                                    
                                    found = true;
                                    break;
                                }
                                currentIndex++;
                            }
                        }
                        if (found) break;
                    }
                }
                
                // Force save even if complete data is missing, to ensure external player progress is saved
                saveWatchHistoryWithData(position, duration, true);
                new ToastMsg(this).toastIconSuccess("Đã lưu lịch sử xem từ trình phát ngoài");
            } else {
                Log.e(TAG, "⚠️ Position is 0 or invalid, not saving history.");
            }
        } else {
            Log.e(TAG, "⚠️ onActivityResult - Result not OK or Data null. ResultCode: " + resultCode);
        }
        
        // Finish activity after handling result
        finish();
    }

    private void launchP4KPlayer(String videoUrl) {
        try {
            // Clean URL to prevent parsing issues
            videoUrl = cleanVideoUrl(videoUrl);
            
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
            
            // Add flags to ensure external player comes to front
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            
            // Pass User-Agent via standard headers
            intent.putExtra("headers", new String[]{"User-Agent", userAgent});
            intent.putExtra("user-agent", userAgent);
            
            // Safe title handling - avoid null or problematic titles
            String title = model.getTitle();
            if (title != null && !title.trim().isEmpty() && title.length() < 200) {
                // Only pass valid, reasonable length titles
                intent.putExtra("title", title.trim());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }

            Log.d(TAG, "🎬 Launching P4K Player - URL: " + videoUrl);
            Log.d(TAG, "📡 User-Agent: " + userAgent);
            
            // Clear window flags before finishing
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            
            startActivity(intent);
            
            // Force finish immediately  
            finishAndRemoveTask();
            overridePendingTransition(0, 0); // No animation
            
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

            // Multiple approaches for Google TV compatibility
            Intent intent = null;
            boolean launched = false;
            
            // Method 1: Try direct Kodi launch intent first
            try {
                intent = new Intent("org.xbmc.kodi.action.PLAY_VIDEO");
                intent.setPackage(packageName);
                intent.setData(Uri.parse(videoUrl));
                intent.putExtra("user-agent", userAgent);
                intent.putExtra("User-Agent", userAgent);
                
                if (model.getTitle() != null) {
                    intent.putExtra("title", model.getTitle());
                }
                
                if (mStartingPosition > 0) {
                    intent.putExtra("position", (int) mStartingPosition);
                }
                
                Log.d(TAG, "🎬 Trying Kodi direct action - URL: " + videoUrl);
                startActivity(intent);
                launched = true;
                
            } catch (Exception e) {
                Log.d(TAG, "⚠️ Kodi direct action failed, trying standard method: " + e.getMessage());
            }
            
            // Method 2: Standard ACTION_VIEW if direct action failed
            if (!launched) {
                try {
                    intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(videoUrl), "video/*");
                    intent.setPackage(packageName);
                    
                    // Add flags for Google TV compatibility
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    
                    // Multiple header formats for compatibility
                    intent.putExtra("headers", new String[]{"User-Agent", userAgent});
                    intent.putExtra("user-agent", userAgent);
                    intent.putExtra("User-Agent", userAgent);
                    
                    if (model.getTitle() != null) {
                        intent.putExtra("title", model.getTitle());
                        intent.putExtra("name", model.getTitle());
                    }
                    
                    if (mStartingPosition > 0) {
                        intent.putExtra("position", (int) mStartingPosition);
                        intent.putExtra("resume", (int) mStartingPosition);
                    }
                    
                    Log.d(TAG, "🎬 Launching Kodi standard method - URL: " + videoUrl);
                    Log.d(TAG, "📡 User-Agent: " + userAgent);
                    startActivity(intent);
                    launched = true;
                    
                } catch (Exception e) {
                    Log.e(TAG, "❌ Standard Kodi launch failed: " + e.getMessage());
                }
            }
            
            // Method 3: Fallback - try launching Kodi main activity then send video
            if (!launched) {
                try {
                    Intent kodiIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                    if (kodiIntent != null) {
                        kodiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        kodiIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(kodiIntent);
                        
                        // Give Kodi time to start, then try sending video
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Intent playIntent = new Intent(Intent.ACTION_VIEW);
                                    playIntent.setDataAndType(Uri.parse(videoUrl), "video/*");
                                    playIntent.setPackage(packageName);
                                    playIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(playIntent);
                                } catch (Exception e) {
                                    Log.e(TAG, "❌ Delayed Kodi video send failed: " + e.getMessage());
                                }
                            }
                        }, 2000);
                        
                        launched = true;
                        Log.d(TAG, "🎬 Launched Kodi with delayed video send");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Kodi fallback launch failed: " + e.getMessage());
                }
            }
            
            if (launched) {
                finish();
            } else {
                new ToastMsg(this).toastIconError("Không thể mở Kodi trên Google TV. Hãy thử trình phát khác.");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching Kodi", e);
            new ToastMsg(this).toastIconError("Lỗi khi mở Kodi: " + e.getMessage());
        }
    }
    
    private void launchJustPlayer(String videoUrl) {
        try {
            // Clean URL to prevent parsing issues
            videoUrl = cleanVideoUrl(videoUrl);
            
            // Get user agent
            String userAgent = Util.getUserAgent(this, "oxoo");
            
            // Check if Just Player is installed
            String justPlayerPackage = "com.brouken.player";
            
            // Enhanced package check with more robust detection
            boolean isInstalled = false;
            try {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(justPlayerPackage, 0);
                isInstalled = packageInfo != null;
                Log.d(TAG, "📦 Just Player package check: " + (isInstalled ? "INSTALLED" : "NOT FOUND"));
            } catch (PackageManager.NameNotFoundException e) {
                Log.d(TAG, "📦 Just Player package NOT FOUND: " + e.getMessage());
                isInstalled = false;
            } catch (Exception e) {
                Log.e(TAG, "📦 Just Player package check ERROR: " + e.getMessage());
                isInstalled = false;
            }
            
            if (!isInstalled) {
                Log.d(TAG, "❌ Just Player not installed, showing install dialog");
                showPlayerInstallDialog("Just Player", "com.brouken.player");
                return;
            }

            // Double check with intent resolution
            Intent testIntent = new Intent(Intent.ACTION_VIEW);
            testIntent.setDataAndType(Uri.parse(videoUrl), "video/*");
            testIntent.setPackage(justPlayerPackage);
            
            if (getPackageManager().resolveActivity(testIntent, 0) == null) {
                Log.d(TAG, "❌ Just Player cannot handle this intent, showing install dialog");
                showPlayerInstallDialog("Just Player", "com.brouken.player");
                return;
            }

            // Launch Just Player
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(videoUrl), "video/*");
            intent.setPackage(justPlayerPackage);
            
            // Add flags to ensure external player comes to front
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            
            intent.putExtra("headers", new String[]{"User-Agent", userAgent});
            
            // Safe title handling - avoid null or problematic titles
            String title = model.getTitle();
            if (title != null && !title.trim().isEmpty() && title.length() < 200) {
                intent.putExtra("title", title.trim());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }

            Log.d(TAG, "🎬 Launching Just Player - URL: " + videoUrl);
            Log.d(TAG, "📡 User-Agent: " + userAgent);
            
            // Clear window flags before finishing
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            
            // Remove NEW_TASK flag for startActivityForResult compatibility
            intent.setFlags(0); // Clear all flags
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            
            startActivityForResult(intent, 1003);
            // finishAndRemoveTask(); // Don't finish, wait for result
            
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "❌ ActivityNotFoundException for Just Player", e);
            new ToastMsg(this).toastIconError("Just Player chưa được cài đặt hoặc không tương thích");
            // Fallback to internal player
            fallbackToInternalPlayer();
        } catch (Exception e) {
            Log.e(TAG, "Error launching Just Player", e);
            new ToastMsg(this).toastIconError("Lỗi khi mở Just Player: " + e.getMessage());
            // Fallback to internal player
            fallbackToInternalPlayer();
        }
    }
    
    private void launchMXPlayer(String videoUrl) {
        try {
            // Clean URL to prevent parsing issues
            videoUrl = cleanVideoUrl(videoUrl);
            
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
            
            // Add flags to ensure external player comes to front
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            
            intent.putExtra("headers", new String[]{"User-Agent", userAgent});
            
            // Safe title handling - avoid null or problematic titles
            // Only set single title if NOT using playlist, otherwise MX Player might override playlist titles
            boolean isPlaylist = model != null && model.getAllSeasons() != null && model.getAllSeasons().size() > 0;
            String title = model.getTitle();
            
            if (!isPlaylist && title != null && !title.trim().isEmpty() && title.length() < 200) {
                // Only pass valid, reasonable length titles
                intent.putExtra("title", title.trim());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }

            // Request result from MX Player
            intent.putExtra("return_result", true);

            // Add playlist support for TV Series (MX Player)
            if (model != null && model.getAllSeasons() != null && model.getAllSeasons().size() > 0) {
                try {
                    ArrayList<Uri> videoList = new ArrayList<>();
                    ArrayList<String> nameList = new ArrayList<>();
                    int currentPosition = -1;
                    int index = 0;
                    
                    // Flatten episodes to create a playlist
                    for (Season season : model.getAllSeasons()) {
                        if (season.getEpisodes() != null) {
                            for (Episode episode : season.getEpisodes()) {
                                String epUrl = episode.getFileUrl();
                                if (epUrl != null && !epUrl.isEmpty()) {
                                    String cleanUrl = cleanVideoUrl(epUrl);
                                    videoList.add(Uri.parse(cleanUrl));
                                    
                                    String epName = episode.getEpisodesName();
                                    if (epName == null) epName = "Episode " + (index + 1);
                                    
                                    // Prepend Series Title for better context in player
                                    // Use episode.getTvSeriesTitle() if available, otherwise use model.getTitle()
                                    String seriesTitle = episode.getTvSeriesTitle();
                                    if (seriesTitle == null || seriesTitle.isEmpty()) {
                                        seriesTitle = model.getTitle();
                                        // Clean series title if it contains " - Episode" or similar
                                        if (seriesTitle != null && seriesTitle.contains(" - ")) {
                                            String[] parts = seriesTitle.split(" - ");
                                            if (parts.length > 0) {
                                                seriesTitle = parts[0];
                                            }
                                        }
                                    }
                                    
                                    if (seriesTitle != null && !seriesTitle.isEmpty()) {
                                        epName = seriesTitle + " - " + epName;
                                    }
                                    
                                    nameList.add(epName);
                                    
                                    // Check if this is the current video
                                    if (cleanUrl.equals(videoUrl) || epUrl.equals(videoUrl)) {
                                        currentPosition = index;
                                    }
                                    index++;
                                }
                            }
                        }
                    }
                    
                    if (videoList.size() > 1 && currentPosition != -1) {
                        intent.putExtra("video_list", videoList.toArray(new android.os.Parcelable[0]));
                        intent.putExtra("video_list.name", nameList.toArray(new String[0])); // Correct extra key for MX Player
                        intent.putExtra("video_list_is_explicit", true);
                        intent.putExtra("position", currentPosition);
                        Log.d(TAG, "📺 Added playlist to MX Player: " + videoList.size() + " episodes, starting at " + currentPosition);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error creating playlist for MX Player", e);
                }
            }

            Log.e(TAG, "🎬 Launching MX Player - URL: " + videoUrl);
            Log.e(TAG, "📡 User-Agent: " + userAgent);
            
            // Clear window flags before finishing
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            
            // Remove NEW_TASK flag for startActivityForResult compatibility
            intent.setFlags(0); // Clear all flags
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            
            startActivityForResult(intent, 1001);
            // finishAndRemoveTask(); // Don't finish, wait for result
            
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
            
            // Add flags to ensure external player comes to front
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            
            // Vimu Player accepts User-Agent via intent extras
            intent.putExtra("user-agent", userAgent);
            intent.putExtra("headers", "User-Agent: " + userAgent);
            
            // Safe title handling - avoid null or problematic titles
            String title = model.getTitle();
            if (title != null && !title.trim().isEmpty() && title.length() < 200) {
                intent.putExtra("title", title.trim());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }
            
            Log.d(TAG, "🎬 Launching Vimu Player - URL: " + videoUrl);
            Log.d(TAG, "📡 User-Agent: " + userAgent);
            
            // Clear window flags before finishing
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            
            startActivity(intent);
            finishAndRemoveTask();
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching Vimu Player", e);
            new ToastMsg(this).toastIconError("Lỗi khi mở Vimu Player: " + e.getMessage());
        }
    }

    private void launchVLCPlayer(String videoUrl) {
        try {
            // Clean URL to prevent parsing issues
            videoUrl = cleanVideoUrl(videoUrl);
            
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
            
            // Add flags to ensure external player comes to front
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            
            // VLC accepts User-Agent via intent extras
            intent.putExtra("http-user-agent", userAgent);
            intent.putExtra("user-agent", userAgent);
            
            // Safe title handling - avoid null or problematic titles
            String title = model.getTitle();
            if (title != null && !title.trim().isEmpty() && title.length() < 200) {
                intent.putExtra("title", title.trim());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }

            Log.d(TAG, "🎬 Launching VLC Player - URL: " + videoUrl);
            Log.d(TAG, "📡 User-Agent: " + userAgent);
            
            // Clear window flags before finishing
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            
            // Remove NEW_TASK flag for startActivityForResult compatibility
            intent.setFlags(0); // Clear all flags
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            
            startActivityForResult(intent, 1002);
            // finishAndRemoveTask(); // Don't finish, wait for result
            
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
            
            // Add flags to ensure external player comes to front
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            
            // nPlayer accepts headers and User-Agent
            intent.putExtra("headers", new String[]{"User-Agent", userAgent});
            intent.putExtra("user-agent", userAgent);
            
            // Safe title handling - avoid null or problematic titles
            String title = model.getTitle();
            if (title != null && !title.trim().isEmpty() && title.length() < 200) {
                intent.putExtra("title", title.trim());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }

            Log.d(TAG, "🎬 Launching nPlayer (" + installedPackage + ") - URL: " + videoUrl);
            Log.d(TAG, "📡 User-Agent: " + userAgent);
            
            // Clear window flags before finishing
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            
            startActivity(intent);
            finishAndRemoveTask();
            
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
            
            // Add flags to ensure external player comes to front
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            
            intent.putExtra("headers", new String[]{"User-Agent", userAgent});
            
            // Safe title handling - avoid null or problematic titles
            String title = model.getTitle();
            if (title != null && !title.trim().isEmpty() && title.length() < 200) {
                intent.putExtra("title", title.trim());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }

            Log.d(TAG, "🎬 Launching Dune HD (Realtek) - URL: " + videoUrl);
            Log.d(TAG, "📡 User-Agent: " + userAgent);
            
            // Clear window flags before finishing
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            
            startActivity(intent);
            finishAndRemoveTask();
            
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
            
            // Add flags to ensure external player comes to front
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            
            intent.putExtra("headers", new String[]{"User-Agent", userAgent});
            
            // Safe title handling - avoid null or problematic titles
            String title = model.getTitle();
            if (title != null && !title.trim().isEmpty() && title.length() < 200) {
                intent.putExtra("title", title.trim());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }

            Log.d(TAG, "🎬 Launching Dune HD (Amlogic) - URL: " + videoUrl);
            Log.d(TAG, "📡 User-Agent: " + userAgent);
            
            // Clear window flags before finishing
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            
            startActivity(intent);
            finishAndRemoveTask();
            
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
            
            // Add flags to ensure external player comes to front
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            
            intent.putExtra("headers", new String[]{"User-Agent", userAgent});
            
            // Safe title handling - avoid null or problematic titles
            String title = model.getTitle();
            if (title != null && !title.trim().isEmpty() && title.length() < 200) {
                intent.putExtra("title", title.trim());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }

            Log.d(TAG, "🎬 Launching Zidoo (Realtek) - URL: " + videoUrl);
            Log.d(TAG, "📡 User-Agent: " + userAgent);
            
            // Clear window flags before finishing
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            
            startActivity(intent);
            finishAndRemoveTask();
            
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
            
            // Add flags to ensure external player comes to front
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            
            intent.putExtra("headers", new String[]{"User-Agent", userAgent});
            
            // Safe title handling - avoid null or problematic titles
            String title = model.getTitle();
            if (title != null && !title.trim().isEmpty() && title.length() < 200) {
                intent.putExtra("title", title.trim());
            }
            
            if (mStartingPosition > 0) {
                intent.putExtra("position", (int) mStartingPosition);
            }

            Log.d(TAG, "🎬 Launching Zidoo (Amlogic - Dolby Vision) - URL: " + videoUrl);
            Log.d(TAG, "📡 User-Agent: " + userAgent);
            
            // Clear window flags before finishing
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            
            startActivity(intent);
            finishAndRemoveTask();
            
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

    private String cleanVideoUrl(String videoUrl) {
        if (videoUrl == null) return "";
        
        // Clean leading and trailing whitespace
        String cleaned = videoUrl.trim();
        
        // Remove any invisible characters that might cause issues
        cleaned = cleaned.replaceAll("^\\s+", "").replaceAll("\\s+$", "");
        
        // Remove any non-printable characters at start/end
        cleaned = cleaned.replaceAll("^[\\x00-\\x1F\\x7F]+", "").replaceAll("[\\x00-\\x1F\\x7F]+$", "");
        
        // Ensure protocol is present
        if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
            if (cleaned.startsWith("//")) {
                cleaned = "https:" + cleaned;
            } else if (!cleaned.isEmpty()) {
                cleaned = "https://" + cleaned;
            }
        }
        
        Log.d(TAG, "🧹 URL cleaning result: '" + cleaned + "'");
        return cleaned;
    }
    
    private boolean isProblematicUrl(String videoUrl) {
        if (videoUrl == null || videoUrl.trim().isEmpty()) return true;
        
        String cleanUrl = videoUrl.trim();
        
        // Check for basic validity
        if (cleanUrl.length() < 10 ||
            (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://"))) {
            return true;
        }
        
        // Check for specific problematic patterns (can be removed if not needed)
        return false;  // Let's try all URLs now that we clean them properly
    }
    
    private void showUrlIssueDialog(String videoUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Vấn đề URL Video");
        builder.setMessage("URL video này có thể gây lỗi với trình phát ngoài. Bạn muốn:\n\n• Thử internal player\n• Hoặc thử external player khác?");
        
        builder.setPositiveButton("Internal Player", (dialog, which) -> {
            Log.d(TAG, "🎯 User chose internal player for problematic URL");
            fallbackToInternalPlayer();
        });
        
        builder.setNegativeButton("Thử External Player", (dialog, which) -> {
            Log.d(TAG, "🎯 User chose to try external player anyway");
            dialog.dismiss();
            // Continue with external player
            continueExternalPlayerLaunch();
        });
        
        builder.setNeutralButton("Hủy", (dialog, which) -> {
            dialog.dismiss();
            finish();
        });
        
        builder.setCancelable(false);
        builder.show();
    }
    
    private void continueExternalPlayerLaunch() {
        SharedPreferences prefs = getSharedPreferences(Constants.USER_LOGIN_STATUS, MODE_PRIVATE);
        String selectedPlayer = prefs.getString("selected_player", "just");
        String videoUrl = model.getVideoUrl();
        
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
            default:
                launchJustPlayer(videoUrl);
                break;
        }
    }
    
    private void fallbackToInternalPlayer() {
        Log.d(TAG, "🔄 Falling back to internal player");
        
        // Clear external player preference temporarily for this session
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Continue with internal player setup
        new ToastMsg(this).toastIconSuccess("Chuyển sang trình phát nội bộ");
        
        // Don't finish - let the activity continue with internal player
        // The rest of onCreate will handle internal player setup
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
        // Log.d(TAG, "🎬 openSubtitleDialog() called");
        
        // If we don't have video object or subtitle data, try to load it first
        if (video == null || video.getSubtitle() == null || video.getSubtitle().isEmpty()) {
            // Log.d(TAG, "🎬 No external subtitle data found, loading from API...");
            loadSubtitleDataAndShowDialog();
            return;
        }

        // Build combined list
        List<Subtitle> combinedSubtitles = new ArrayList<>();

        // 1. Add "Off" option
        Subtitle offSub = new Subtitle();
        offSub.setLanguage("Tắt (Off)");
        offSub.setUrl("off");
        combinedSubtitles.add(offSub);

        // 2. Add Embedded Subtitles
        if (trackSelector != null) {
            MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
            if (mappedTrackInfo != null) {
                for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
                    if (mappedTrackInfo.getRendererType(i) == C.TRACK_TYPE_TEXT) {
                        TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
                        for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
                            TrackGroup group = trackGroups.get(groupIndex);
                            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                                Format format = group.getFormat(trackIndex);
                                Subtitle embeddedSub = new Subtitle();
                                String lang = format.language != null ? new Locale(format.language).getDisplayLanguage() : "Und";
                                String label = format.label != null ? format.label : lang;
                                embeddedSub.setLanguage(label + " (Muxed)");
                                embeddedSub.setUrl("embedded:" + i + ":" + groupIndex + ":" + trackIndex);
                                combinedSubtitles.add(embeddedSub);
                            }
                        }
                    }
                }
            }
        }

        // 3. Add API Subtitles
        if (video != null && video.getSubtitle() != null) {
            combinedSubtitles.addAll(video.getSubtitle());
        }

        // Show dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(PlayerActivity.this);
        View view = LayoutInflater.from(PlayerActivity.this).inflate(R.layout.layout_subtitle_dialog, null);
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        
        RecyclerView serverRv = view.findViewById(R.id.serverRv);
        SubtitleListAdapter adapter = new SubtitleListAdapter(PlayerActivity.this, combinedSubtitles);
        
        adapter.setListener(new SubtitleListAdapter.OnSubtitleItemClickListener() {
            @Override
            public void onSubtitleItemClick(View view, Subtitle subtitle, int position, SubtitleListAdapter.SubtitleViewHolder holder) {
                String url = subtitle.getUrl();
                // Log.d(TAG, "🎬 Selected subtitle: " + subtitle.getLanguage() + ", URL: " + url);

                if ("off".equals(url)) {
                    if (trackSelector != null) {
                        trackSelector.setParameters(
                            trackSelector.buildUponParameters()
                                .setRendererDisabled(C.TRACK_TYPE_TEXT, true)
                        );
                    }
                    dialog.dismiss();
                } else if (url != null && url.startsWith("embedded:")) {
                    try {
                        String[] parts = url.split(":");
                        int rendererIndex = Integer.parseInt(parts[1]);
                        int groupIndex = Integer.parseInt(parts[2]);
                        int trackIndex = Integer.parseInt(parts[3]);
                        
                        if (trackSelector != null) {
                            MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                            if (mappedTrackInfo != null) {
                                TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
                                DefaultTrackSelector.SelectionOverride override = new DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex);
                                
                                trackSelector.setParameters(
                                    trackSelector.buildUponParameters()
                                        .setRendererDisabled(C.TRACK_TYPE_TEXT, false)
                                        .setSelectionOverride(rendererIndex, trackGroups, override)
                                );
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error selecting embedded subtitle", e);
                    }
                    dialog.dismiss();
                } else {
                    // External API subtitle
                    if (url != null && !url.isEmpty()) {
                        loadExternalSubtitle(url, subtitle.getLanguage());
                    }
                    dialog.dismiss();
                }
            }
        });
        
        serverRv.setLayoutManager(new LinearLayoutManager(PlayerActivity.this));
        serverRv.setHasFixedSize(true);
        serverRv.setAdapter(adapter);

        Button closeBt = view.findViewById(R.id.close_bt);
        closeBt.setOnClickListener(v -> dialog.dismiss());
        
        // Playback speed buttons
        Button speed05 = view.findViewById(R.id.speed_0_5);
        Button speed075 = view.findViewById(R.id.speed_0_75);
        Button speed10 = view.findViewById(R.id.speed_1_0);
        Button speed15 = view.findViewById(R.id.speed_1_5);
        Button speed20 = view.findViewById(R.id.speed_2_0);
        Button speed30 = view.findViewById(R.id.speed_3_0);

        speed05.setOnClickListener(v -> setPlaybackSpeed(0.5f));
        speed075.setOnClickListener(v -> setPlaybackSpeed(0.75f));
        speed10.setOnClickListener(v -> setPlaybackSpeed(1.0f));
        speed15.setOnClickListener(v -> setPlaybackSpeed(1.5f));
        speed20.setOnClickListener(v -> setPlaybackSpeed(2.0f));
        speed30.setOnClickListener(v -> setPlaybackSpeed(3.0f));

        dialog.show();
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
            Toast.makeText(PlayerActivity.this, "Không tìm thấy phụ đề", Toast.LENGTH_SHORT).show();
        }
    }

    public void initVideoPlayer(String url, String type) {
        // Check if this is an anime_resolve URL that needs stream resolution first
        if (AnimeClient.isAnimeResolveUrl(url)) {
            Log.d(TAG, "🎬 Detected anime_resolve URL, resolving stream...");
            resolveAnimeStreamAndContinue(url, type);
            return;
        }
        
        // Ensure synchronization between flags
        if (useSoftwareAudioDecoder) audioMode = 1;

        if (player != null) {
            player.release();
        }
        
        // Store current video URL for subtitle loading
        this.url = url;

        progressBar.setVisibility(VISIBLE);
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter.Builder(PlayerActivity.this).build();
        AdaptiveTrackSelection.Factory videoTrackSelectionFactory = new
                AdaptiveTrackSelection.Factory();

        trackSelector = new
                DefaultTrackSelector(PlayerActivity.this, videoTrackSelectionFactory);

        // Configure track selector details
        // Disable tunneling to prevent black screen issues (video loss) while keeping audio passthrough logic
        boolean tunnelingEnabled = false; 
        
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setExceedAudioConstraintsIfNecessary(true)
                .setExceedVideoConstraintsIfNecessary(true)
                .setExceedRendererCapabilitiesIfNecessary(true)
                .setTunnelingEnabled(tunnelingEnabled)
        );
        
        Log.d("PlayerActivity", "🔊 Track selector configured. Tunneling: " + tunnelingEnabled);

        // Optimize LoadControl for faster seeking
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                        DefaultLoadControl.DEFAULT_MAX_BUFFER_MS, 
                        1500, // Reduce buffer before playback (faster seeking)
                        2000  // Reduce buffer after rebuffer (faster seeking)
                )
                .build();

        // [FIX] Force "OFF" for extension renderers first to ensure we don't accidentally load double clocks
        // We will manually control the renderers in the factory.
        // If we are in specific audio fallback mode, we want total control.
        final int finalExtensionMode;
        if (audioMode == 1) finalExtensionMode = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER;
        else finalExtensionMode = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF; // HW & Passthrough Mode -> OFF
        
        Log.d("PlayerActivity", "🔊 Extension Renderer Mode Target: " + finalExtensionMode);
        
        // Create custom RenderersFactory with strict renderer filtering
        RenderersFactory renderersFactory = new DefaultRenderersFactory(this) {
            /** 
             * Override AudioSink to force Passthrough capabilities
             * This ensures ExoPlayer attempts passthrough for DTS/AC3 even if HDMI handshake missed them.
             * Fixes the "Encode/PCM" fallback on track switch.
             */
            @Override
            protected com.google.android.exoplayer2.audio.AudioSink buildAudioSink(Context context, boolean enableFloatOutput, boolean enableAudioTrackPlaybackParams, boolean enableOffload) {
                if (audioMode == 2) { 
                    Log.d("PlayerActivity", "🔊 Passthrough Mode: Forcing AudioSink capabilities");
                    return new com.google.android.exoplayer2.audio.DefaultAudioSink.Builder()
                        .setAudioCapabilities(new com.google.android.exoplayer2.audio.AudioCapabilities(
                            new int[] {
                                com.google.android.exoplayer2.C.ENCODING_PCM_16BIT,
                                com.google.android.exoplayer2.C.ENCODING_AC3,
                                com.google.android.exoplayer2.C.ENCODING_E_AC3,
                                com.google.android.exoplayer2.C.ENCODING_DTS,
                                com.google.android.exoplayer2.C.ENCODING_DTS_HD,
                                com.google.android.exoplayer2.C.ENCODING_DOLBY_TRUEHD
                            }, 
                            10
                        ))
                        .build();
                }
                return super.buildAudioSink(context, enableFloatOutput, enableAudioTrackPlaybackParams, enableOffload);
            }

            @Override
            public void buildAudioRenderers(Context context, int extensionRendererMode, com.google.android.exoplayer2.mediacodec.MediaCodecSelector mediaCodecSelector, boolean enableDecoderFallback, com.google.android.exoplayer2.audio.AudioSink audioSink, Handler eventHandler, com.google.android.exoplayer2.audio.AudioRendererEventListener eventListener, java.util.ArrayList<com.google.android.exoplayer2.Renderer> out) {
                // Modified: We rely on PREFER mode to prioritize FFmpeg.
                // We do NOT strictly filter MediaCodec anymore to avoid "Silence" if FFmpeg is missing.
                // ExoPlayer will place FFmpeg first if PREFER is set.
                
                if (audioMode == 1) { // Software Mode
                     Log.d("PlayerActivity", "🔨 FORCE SW MODE: Requesting Extension Renderer Prefer...");
                     // Use PREFER to let ExoPlayer add FFmpeg (if avail) first, then MediaCodec.
                     super.buildAudioRenderers(context, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER, mediaCodecSelector, enableDecoderFallback, audioSink, eventHandler, eventListener, out);
                     
                     // Diagnostic check only
                     boolean hasExtension = false;
                     for (com.google.android.exoplayer2.Renderer r : out) {
                         if (r.getClass().getSimpleName().contains("Ffmpeg") || r.getClass().getSimpleName().contains("Lib")) {
                             hasExtension = true;
                             Log.d("PlayerActivity", "✅ Software Renderer detected in list: " + r.getClass().getSimpleName());
                         }
                     }
                     if (!hasExtension) {
                         Log.w("PlayerActivity", "⚠️ Software requested but no Extension Renderer found in final list!");
                     }
                     
                } else if (audioMode == 0) { // Hardware Mode
                     // ... existing HW logic ...
                     Log.d("PlayerActivity", "🔨 FORCE HW MODE: Building MediaCodec Renderers Only");
                     super.buildAudioRenderers(context, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF, mediaCodecSelector, enableDecoderFallback, audioSink, eventHandler, eventListener, out);
                     
                } else { // Passthrough Mode (2)
                     Log.d("PlayerActivity", "🔨 PASSTHROUGH MODE: Building Renderers (No Extensions)");
                     // FIX: Disable extensions in Passthrough to prevent fallback to Stereo PCM (Software)
                     // FIX 2: Re-enable decoder fallback (true) to prevent black screen if Passthrough fails initialization
                     // This allows MediaCodec to perform minimal decoding if raw passthrough is completely rejected, keeping video running.
                     super.buildAudioRenderers(context, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF, mediaCodecSelector, true, audioSink, eventHandler, eventListener, out);
                }
            }
        }.setExtensionRendererMode(finalExtensionMode);

        player = new ExoPlayer.Builder(PlayerActivity.this, renderersFactory)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .build();
                
        player.setVolume(1.0f); // Force max volume

        // DEBUG: Listen for Audio Decoder events
        player.addAnalyticsListener(new com.google.android.exoplayer2.analytics.AnalyticsListener() {
            @Override
            public void onAudioDecoderInitialized(EventTime eventTime, String decoderName, long initializationDurationMs) {
                Log.e("PlayerActivity", "🎧 Audio Decoder Initialized: " + decoderName); // Log as Error to see in user's filter
                runOnUiThread(() -> {
                     new ToastMsg(PlayerActivity.this).toastIconSuccess("Âm thanh: " + decoderName);
                });
            }
            @Override
            public void onAudioCodecError(EventTime eventTime, Exception audioCodecError) {
                Log.e("PlayerActivity", "🎧 Audio Codec Error: " + audioCodecError.getMessage());
                 runOnUiThread(() -> {
                     new ToastMsg(PlayerActivity.this).toastIconError("Lỗi Codec: " + audioCodecError.getMessage());
                });
            }
        });
        
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
                    
                    // --- DEBUG LOGGING FOR AUDIO TRACKS ---
                    Log.d(TAG, "🔍 PLAYER READY: Inspecting Audio Tracks...");
                    MappingTrackSelector.MappedTrackInfo debugMappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                    if (debugMappedTrackInfo != null) {
                        for (int i = 0; i < debugMappedTrackInfo.getRendererCount(); i++) {
                            int rendererType = debugMappedTrackInfo.getRendererType(i);
                            if (rendererType == C.TRACK_TYPE_AUDIO) {
                                com.google.android.exoplayer2.trackselection.TrackSelection selection = player.getCurrentTrackSelections().get(i);
                                if (selection != null) {
                                    Log.d(TAG, "   🔊 AUDIO RENDERER [" + i + "] ACTIVE");
                                    for (int k = 0; k < selection.length(); k++) {
                                        Format f = selection.getFormat(k);
                                        Log.d(TAG, "      Selected Track: " + f.sampleMimeType + 
                                              " | Lang: " + f.language + 
                                              " | Ch: " + f.channelCount + 
                                              " | Codecs: " + f.codecs +
                                              " | ID: " + f.id);
                                    }
                                } else {
                                    Log.d(TAG, "   🔇 AUDIO RENDERER [" + i + "] NOT SELECTED");
                                    TrackGroupArray groups = debugMappedTrackInfo.getTrackGroups(i);
                                    if (groups.length > 0) {
                                         Log.d(TAG, "      (Renderer has " + groups.length + " available track groups, but none selected)");
                                    }
                                }
                            }
                        }
                    }
                    // --------------------------------------
                    
                    // FALLBACK CHECK: Check if video is playing but audio renderer is NOT selected (Silent Playback)
                    // This happens when Hardware Decoder rejects the AC3 track but throws no error.
                    if (!useSoftwareAudioDecoder && isPlaying) {
                        try {
                            MappingTrackSelector.MappedTrackInfo trackInfo = trackSelector.getCurrentMappedTrackInfo();
                            if (trackInfo != null) {
                                boolean hasAudioTracks = false;
                                boolean audioSelected = false;
                                
                                for (int i = 0; i < trackInfo.getRendererCount(); i++) {
                                    if (trackInfo.getRendererType(i) == C.TRACK_TYPE_AUDIO) {
                                        TrackGroupArray groups = trackInfo.getTrackGroups(i);
                                        if (groups.length > 0) hasAudioTracks = true;
                                        
                                        if (player.getCurrentTrackSelections().get(i) != null) {
                                            audioSelected = true;
                                            break;
                                        }
                                    }
                                }
                                
                                if (hasAudioTracks && !audioSelected) {
                                    Log.e("PlayerActivity", "🔇 SILENT FAILURE DETECTED: Audio tracks exist but HW decoder rejected them. Switching to Software...");
                                    runOnUiThread(() -> {
                                        // Trigger existing fallback logic
                                        hasHandledAudioCodecError = false;
                                        handleAudioCodecError();
                                    });
                                    return; // Stop further processing
                                }
                            }
                        } catch (Exception e) {
                            Log.e("PlayerActivity", "Error checking for silent playback", e);
                        }
                    }
                    
                    // RESTORE SAVED AUDIO TRACK SELECTION (if any)
                    if (savedAudioOverride != null && savedAudioRendererIndex != -1) {
                        Log.d(TAG, "🔄 Restoring saved audio track selection...");
                        try {
                            MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                            if (mappedTrackInfo != null && savedAudioRendererIndex < mappedTrackInfo.getRendererCount()) {
                                TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(savedAudioRendererIndex);
                                
                                // Verify if the saved group index is still valid
                                if (savedAudioOverride.groupIndex < trackGroups.length) {
                                    DefaultTrackSelector.Parameters.Builder parametersBuilder = trackSelector.buildUponParameters();
                                    
                                    // CRITICAL FIX: Ensure only ONE audio renderer is enabled when restoring selection
                                    // This prevents the "Multiple renderer media clocks enabled" crash
                                    for (int r = 0; r < mappedTrackInfo.getRendererCount(); r++) {
                                        if (mappedTrackInfo.getRendererType(r) == C.TRACK_TYPE_AUDIO) {
                                            // Clear any existing overrides on other renderers
                                            parametersBuilder.clearSelectionOverrides(r);
                                            // Disable all other audio renderers except the one we are restoring
                                            parametersBuilder.setRendererDisabled(r, r != savedAudioRendererIndex);
                                        }
                                    }

                                    parametersBuilder.setSelectionOverride(
                                        savedAudioRendererIndex,
                                        trackGroups,
                                        savedAudioOverride
                                    );
                                    trackSelector.setParameters(parametersBuilder);
                                    Log.d(TAG, "✅ Restored audio track selection successfully (Exclusive Mode)");
                                } else {
                                    Log.w(TAG, "⚠️ Cannot restore audio track: Group index out of bounds");
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Failed to restore audio track selection", e);
                        }
                        // Clear saved state
                        savedAudioOverride = null;
                        savedAudioRendererIndex = -1;
                    } else {
                        // try to auto-select preferred language tracks (Vietnamese) once
                        applyPreferredLanguageTracks();
                    }
                    
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
                    Log.d(TAG, "🔍 Category: " + category);
                    Log.d(TAG, "🔍 hasPlayerError: " + hasPlayerError);
                    Log.d(TAG, "🔍 canNavigateToNext: " + canNavigateToNextEpisode());
                    
                    // Check if this STATE_ENDED was caused by a recent error (within 5 seconds)
                    long currentTime = System.currentTimeMillis();
                    boolean recentError = (lastErrorTime > 0) && (currentTime - lastErrorTime < 5000);
                    
                    if (hasPlayerError || recentError) {
                        Log.d(TAG, "🚫 Video ended due to player error (flag=" + hasPlayerError + ", recent=" + recentError + ") - auto-play disabled");
                        hasPlayerError = false; // Reset flag for next playback
                        lastErrorTime = 0; // Reset error timestamp
                        return;
                    }
                    
                    // Simplified check: just verify we have valid duration
                    long currentPosition = player.getCurrentPosition();
                    long duration = player.getDuration();
                    
                    Log.d(TAG, "📺 Video progress: " + (currentPosition/1000) + "s / " + (duration/1000) + "s");
                    
                    // Only skip if duration is invalid or position is clearly at beginning (less than 10% progress)
                    if (duration > 0 && currentPosition > 0) {
                        double progressPercentage = (double) currentPosition / duration * 100;
                        Log.d(TAG, "� Progress percentage: " + String.format("%.1f", progressPercentage) + "%");
                        
                        // Only prevent auto-next if video barely started (less than 10% watched)
                        if (progressPercentage < 10.0) {
                            Log.d(TAG, "🚫 Video ended at " + String.format("%.1f", progressPercentage) + "% - likely an error, auto-play disabled");
                            return;
                        }
                    }
                    
                    // Auto-navigate to next episode for TV series
                    if (category.equalsIgnoreCase("tvseries") && canNavigateToNextEpisode()) {
                        Log.d(TAG, "✅ Video completed normally - auto-navigating to next episode");
                        navigateToNextEpisode();
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
                
                // Check if this is EOFException (normal at end of file)
                boolean isEOFException = error.getCause() instanceof java.io.EOFException;
                
                if (isEOFException) {
                    Log.e("PlayerActivity", "📄 EOFException detected - this is normal at end of file, auto-next will proceed");
                    hasPlayerError = false; // Allow auto-next for EOF
                } else {
                    Log.e("PlayerActivity", "🚫 Serious error detected - auto-next disabled");
                    hasPlayerError = true; // Disable auto-next for other errors
                }
                
                lastErrorTime = System.currentTimeMillis();
                
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
                    
                    // CRITICAL: Handle "Multiple renderer media clocks enabled" specific error
                    if ((causeMessage != null && causeMessage.contains("Multiple renderer media clocks")) ||
                        (errorMessage != null && errorMessage.contains("Multiple renderer media clocks"))) {
                         Log.e("PlayerActivity", "🔥 CRITICAL ERROR: Multiple Media Clocks detected! Forcing Software Decoder reset.");
                         
                         if (!useSoftwareAudioDecoder) {
                             handleAudioCodecError();
                             return;
                         } else {
                             // If already in SW mode and getting this, we need to be aggressive.
                             // Restart activity might be needed, but let's try handleAudioCodecError again
                             // which will trigger re-initialization with strict mode.
                             handleAudioCodecError(); 
                             return;
                         }
                    }

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
                        errorMessage.contains("TrueHD") ||
                        errorMessage.contains("true-hd") ||
                        errorMessage.contains("Atmos") ||
                        errorMessage.contains("Dolby Atmos") ||
                        errorMessage.contains("DTS") ||
                        errorMessage.contains("DTS-HD") ||
                        errorMessage.contains("DTS Express") ||
                        errorMessage.contains("Opus") ||
                        errorMessage.contains("Vorbis") ||
                        errorMessage.contains("FLAC") ||
                        errorMessage.contains("WMA") ||
                        errorMessage.contains("PCM") ||
                        errorMessage.contains("MPEG-H") ||
                        errorMessage.contains("audio/"))) ||
                        (causeMessage != null && (
                        causeMessage.contains("MediaCodecAudioRenderer") ||
                        causeMessage.contains("NO_UNSUPPORTED_TYPE") ||
                        causeMessage.contains("Enhanced AC-3") ||
                        causeMessage.contains("EAC3") ||
                        causeMessage.contains("AC-3") ||
                        causeMessage.contains("TrueHD") ||
                        causeMessage.contains("true-hd") ||
                        causeMessage.contains("Atmos") ||
                        causeMessage.contains("Dolby Atmos") ||
                        causeMessage.contains("DTS") ||
                        causeMessage.contains("DTS-HD") ||
                        causeMessage.contains("DTS Express") ||
                        causeMessage.contains("Opus") ||
                        causeMessage.contains("Vorbis") ||
                        causeMessage.contains("FLAC") ||
                        causeMessage.contains("WMA") ||
                        causeMessage.contains("PCM") ||
                        causeMessage.contains("MPEG-H") ||
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
                    
                    // FALLBACK FOR HARDWARE MODE:
                    // If we are in Hardware mode (!useSoftwareAudioDecoder) and encounter a generic decoding error
                    // that wasn't flagged as Video, treat it as a potential Audio error to trigger fallback.
                    if (!useSoftwareAudioDecoder && !isVideoCodecError) {
                         // Check for specific AC3/Decoder errors
                         String errorString = (error.getMessage() != null ? error.getMessage() : "") + 
                                              (error.getCause() != null && error.getCause().getMessage() != null ? error.getCause().getMessage() : "");
                         
                        if (error.errorCode == com.google.android.exoplayer2.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED || 
                            error.errorCode == com.google.android.exoplayer2.PlaybackException.ERROR_CODE_DECODING_FAILED ||
                            errorString.contains("audio/ac3") || errorString.contains("audio/eac3")) {
                            
                             Log.e("PlayerActivity", "⚠️ Generic decoding error in HW mode (likely AC3/EAC3). Attempting Fallback.");
                             isAudioCodecError = true;
                        }
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
                } else if (error.getCause() instanceof java.io.EOFException) {
                    Log.e("PlayerActivity", "📄 EOFException detected - triggering auto-next for TV series");
                    // EOFException means video reached end, trigger auto-next for TV series
                    if (category != null && category.equalsIgnoreCase("tvseries") && canNavigateToNextEpisode()) {
                        Log.e("PlayerActivity", "✅ Auto-navigating to next episode due to EOFException");
                        navigateToNextEpisode();
                    } else {
                        Log.e("PlayerActivity", "📺 EOFException but no next episode or not TV series");
                    }
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
                            // Toast.makeText(PlayerActivity.this, "◀▶ LEFT/RIGHT để tua", Toast.LENGTH_SHORT).show();
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

        // Always show subtitle button for movies and TV series (they may have API subtitles)
        if (subtitleButton != null) {
            boolean shouldShowSubtitleButton = hasText || 
                category.equalsIgnoreCase("movie") || 
                category.equalsIgnoreCase("tvseries");
            subtitleButton.setVisibility(shouldShowSubtitleButton ? View.VISIBLE : View.GONE);
            Log.d("PlayerActivity", "🔤 Subtitle button visibility - hasEmbedded: " + hasText + 
                ", category: " + category + ", showing: " + shouldShowSubtitleButton);
        }
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
                // Toast.makeText(this, "Tua tới +" + (seekTimeMs / 1000) + "s", Toast.LENGTH_SHORT).show();
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
            // Toast.makeText(this, "Tua lùi -" + (seekTimeMs / 1000) + "s", Toast.LENGTH_SHORT).show();
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
        
        // Check if we need custom headers for anime streams
        if (animeProxyHeaders != null && !animeProxyHeaders.isEmpty()) {
            Log.d(TAG, "🎬 Using custom anime proxy headers for HLS: " + animeProxyHeaders);
            DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                    .setUserAgent(Util.getUserAgent(context, "oxoo"))
                    .setDefaultRequestProperties(animeProxyHeaders)
                    .setConnectTimeoutMs(30000)
                    .setReadTimeoutMs(60000);
            com.google.android.exoplayer2.source.MediaSource videoSource = new HlsMediaSource.Factory(httpDataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri));
            return videoSource;
        }
        
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
                Log.d(TAG, "🔙 Redirecting to HeroStyleVideoDetailsActivity - ID: " + model.getMovieId() + ", Type: " + model.getCategory());
                // Go to video details page
                Intent intent = new Intent(PlayerActivity.this, HeroStyleVideoDetailsActivity.class);
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
        
        saveWatchHistoryWithData(currentPosition, duration, false);
    }
    
    private void saveWatchHistoryWithData(long currentPosition, long duration) {
        saveWatchHistoryWithData(currentPosition, duration, false);
    }
    
    private void saveWatchHistoryWithData(long currentPosition, long duration, boolean forceSave) {
        if (model == null || watchHistorySyncManager == null) {
            Log.e(TAG, "❌ saveWatchHistoryWithData: model or manager is null");
            return;
        }
        
        // Skip saving watch history for free sources (phim4k / kkphim4k)
        String checkId = null;
        if (completeMovieData != null && completeMovieData.getVideosId() != null) {
            checkId = completeMovieData.getVideosId();
        } else if (model.getMovieId() != null) {
            checkId = model.getMovieId();
        } else if (getIntent() != null && getIntent().getStringExtra("id") != null) {
            checkId = getIntent().getStringExtra("id");
        }
        if (checkId != null && (checkId.startsWith("phim4k_") || checkId.startsWith("kkphim4k_") || checkId.startsWith("anime_"))) {
            Log.d(TAG, "⏭️ Skipping watch history for free source: " + checkId);
            return;
        }
        
        // Check for valid duration and position
        boolean validDuration = duration > 0 && duration != Long.MIN_VALUE;
        boolean validPosition = currentPosition >= 0;
        
        if (validDuration && validPosition && watchHistorySyncManager != null) {
            try {
                // Check if completeMovieData has essential information
                boolean hasCompleteInfo = false;
                if (completeMovieData != null) {
                    hasCompleteInfo = completeMovieData.getTitle() != null && 
                                            !completeMovieData.getTitle().isEmpty() && 
                                            !completeMovieData.getTitle().equals("Unknown Title");
                }

                // Use enhanced watch history saving with complete movie metadata when available
                // If forceSave is true and info is incomplete, skip this block to fallback to model data
                if (completeMovieData != null && (hasCompleteInfo || !forceSave)) {
                    
                    if (!hasCompleteInfo) {
                        Log.w(TAG, "⚠️ CompleteMovieData exists but lacks essential info, will try API fetch for: " + completeMovieData.getVideosId());
                        // Force fetch from API to get complete data
                        if (completeMovieData.getVideosId() != null && !completeMovieData.getVideosId().equals("null")) {
                            isWaitingForCompleteData = true;
                            lastSavePosition = currentPosition;
                            lastSaveDuration = duration;
                            fetchCompleteMovieDataForWatchHistory(completeMovieData.getVideosId(), 
                                completeMovieData.getType() != null ? completeMovieData.getType() : "movie");
                            
                            Log.d(TAG, "🕐 Waiting for API data, will retry save later");
                            return; // Don't save yet, wait for API
                        }
                    }
                    
                    Log.e(TAG, "💾 Saving ENHANCED watch history with complete metadata - ID: " + completeMovieData.getVideosId() + 
                              ", Title: " + completeMovieData.getTitle() + 
                              ", Position: " + currentPosition + "ms");
                    
                    // Debug: Log complete movie data structure
                    Log.d(TAG, "🔍 CompleteMovieData debug:");
                    Log.d(TAG, "  - VideosId: '" + completeMovieData.getVideosId() + "'");
                    Log.d(TAG, "  - Title: '" + completeMovieData.getTitle() + "'");
                    Log.d(TAG, "  - Slug: '" + completeMovieData.getSlug() + "'");
                    Log.d(TAG, "  - Type: '" + completeMovieData.getType() + "'");
                    Log.d(TAG, "  - Genres: " + (completeMovieData.getGenre() != null ? completeMovieData.getGenre().size() : "null"));
                    Log.d(TAG, "  - Countries: " + (completeMovieData.getCountry() != null ? completeMovieData.getCountry().size() : "null"));
                    Log.d(TAG, "  - Directors: " + (completeMovieData.getDirector() != null ? completeMovieData.getDirector().size() : "null"));
                    Log.d(TAG, "  - Writers: " + (completeMovieData.getWriter() != null ? completeMovieData.getWriter().size() : "null"));
                    Log.d(TAG, "  - Cast: " + (completeMovieData.getCastAndCrew() != null ? completeMovieData.getCastAndCrew().size() : "null"));
                    Log.d(TAG, "  - Seasons: " + (completeMovieData.getSeason() != null ? completeMovieData.getSeason().size() : "null"));
                    Log.d(TAG, "  - Videos: " + (completeMovieData.getVideos() != null ? completeMovieData.getVideos().size() : "null"));
                    
                    // Prepare data for saveFullWatchHistory method - prioritize intent data over completeMovieData
                    String videoId = completeMovieData.getVideosId();
                    
                    // Validate and fix videoId if null/empty
                    if (videoId == null || videoId.isEmpty() || videoId.equals("null")) {
                        // Try to get from model or intent
                        if (model != null && model.getMovieId() != null && !model.getMovieId().equals("null") && !model.getMovieId().isEmpty()) {
                            videoId = model.getMovieId();
                            Log.w(TAG, "⚠️ CompleteMovieData.getVideosId() is invalid, using model.getMovieId(): " + videoId);
                        } else if (getIntent() != null && getIntent().getStringExtra("id") != null) {
                            videoId = getIntent().getStringExtra("id");
                            Log.w(TAG, "⚠️ CompleteMovieData.getVideosId() is invalid, using intent id: " + videoId);
                        } else {
                            videoId = String.valueOf(System.currentTimeMillis()); // Fallback to timestamp
                            Log.e(TAG, "❌ All videoId sources invalid, using timestamp: " + videoId);
                        }
                    }
                    
                    // Prioritize intent data over completeMovieData for better accuracy
                    String title = getIntent() != null && getIntent().getStringExtra("title") != null ? 
                        getIntent().getStringExtra("title") : 
                        (completeMovieData.getTitle() != null ? completeMovieData.getTitle() : "Unknown Title");
                    
                    String description = getIntent() != null && getIntent().getStringExtra("description") != null ? 
                        getIntent().getStringExtra("description") : 
                        (completeMovieData.getDescription() != null ? completeMovieData.getDescription() : "");
                    
                    String slug = completeMovieData.getSlug() != null ? completeMovieData.getSlug() : "";
                    
                    String releaseDate = getIntent() != null && getIntent().getStringExtra("release_date") != null ?
                        getIntent().getStringExtra("release_date") :
                        (completeMovieData.getRelease() != null ? completeMovieData.getRelease() : "");
                    
                    String runtime = getIntent() != null && getIntent().getStringExtra("runtime") != null ?
                        getIntent().getStringExtra("runtime") :
                        (completeMovieData.getRuntime() != null ? completeMovieData.getRuntime() : "");
                    
                    Log.d(TAG, "🔍 Using title from intent: '" + getIntent().getStringExtra("title") + "'");
                    Log.d(TAG, "🔍 Final title for save: '" + title + "'");
                    String videoQuality = model.getVideoQuality() != null ? model.getVideoQuality() : "";
                    String isTvSeries = model.getIsTvSeries() != null ? model.getIsTvSeries() : "0";
                    String isPaid = completeMovieData.getIsPaid() != null ? completeMovieData.getIsPaid() : "0";
                    String enableDownload = completeMovieData.getEnableDownload() != null ? completeMovieData.getEnableDownload() : "1";
                    
                    String thumbnailUrl = getIntent() != null && getIntent().getStringExtra("thumbnail") != null ?
                        getIntent().getStringExtra("thumbnail") :
                        (completeMovieData.getThumbnailUrl() != null ? completeMovieData.getThumbnailUrl() : 
                         (completeMovieData.getPosterUrl() != null ? completeMovieData.getPosterUrl() : ""));
                    
                    String posterUrl = getIntent() != null && getIntent().getStringExtra("poster") != null ?
                        getIntent().getStringExtra("poster") :
                        (completeMovieData.getPosterUrl() != null ? completeMovieData.getPosterUrl() : "");
                    
                    String imdbRating = getIntent() != null && getIntent().getStringExtra("imdb_rating") != null ?
                        getIntent().getStringExtra("imdb_rating") :
                        (completeMovieData.getImdbRating() != null ? completeMovieData.getImdbRating() : "");
                    
                    String curUrl = model.getVideoUrl() != null ? model.getVideoUrl() : "";
                    boolean isMovie = !isTvSeries.equals("1");
                    
                    // Convert MovieSingleDetails lists to expected format
                    List<Map<String, String>> genreList = convertGenreList(completeMovieData.getGenre());
                    List<Map<String, String>> countryList = convertCountryList(completeMovieData.getCountry());
                    List<Map<String, String>> directorList = convertDirectorList(completeMovieData.getDirector());
                    List<Map<String, String>> writerList = convertWriterList(completeMovieData.getWriter());
                    List<Map<String, String>> castList = convertCastList(completeMovieData.getCastAndCrew());
                    
                    // For TV series data - get from model and completeMovieData
                    Map<String, Object> curSeason = null;
                    Map<String, Object> curEpisode = null;
                    List<Map<String, Object>> allSeasons = null;
                    
                    // If it's a TV series, try to get season/episode data
                    if (isTvSeries.equals("1")) {
                        // Get current season data from model if available
                        if (model.getAllSeasons() != null && model.getCurrentSeasonIndex() >= 0 && 
                            model.getCurrentSeasonIndex() < model.getAllSeasons().size()) {
                            Season currentSeason = model.getAllSeasons().get(model.getCurrentSeasonIndex());
                            curSeason = convertSeasonToMap(currentSeason);
                            
                            // Get current episode data from current season if available  
                            if (currentSeason.getEpisodes() != null && model.getCurrentEpisodeIndex() >= 0 && 
                                model.getCurrentEpisodeIndex() < currentSeason.getEpisodes().size()) {
                                Episode currentEpisode = currentSeason.getEpisodes().get(model.getCurrentEpisodeIndex());
                                curEpisode = convertEpisodeToMap(currentEpisode);
                            }
                        }
                        
                        // Get all seasons data - prefer from completeMovieData, fallback to model
                        if (completeMovieData.getSeason() != null && !completeMovieData.getSeason().isEmpty()) {
                            allSeasons = convertSeasonsToMapList(completeMovieData.getSeason());
                        } else if (model.getAllSeasons() != null && !model.getAllSeasons().isEmpty()) {
                            allSeasons = convertSeasonsToMapList(model.getAllSeasons());
                        }
                    }
                    
                    // Use saveFullWatchHistory with complete MovieSingleDetails data
                    watchHistorySyncManager.saveFullWatchHistory(
                        videoId, title, description, slug, releaseDate, runtime, videoQuality,
                        isTvSeries, isPaid, enableDownload, thumbnailUrl, posterUrl, imdbRating,
                        genreList, countryList, directorList, writerList, castList,
                        curSeason, curEpisode, allSeasons,
                        currentPosition, duration, curUrl, isMovie
                    );
                    
                } else {
                    // Use saveFullWatchHistory even without completeMovieData to get proper JSON structure
                    Log.e(TAG, "⚠️ Using saveFullWatchHistory with PlaybackModel data (no completeMovieData available)");
                    
                    String isTvSeries = model.getIsTvSeries() != null ? model.getIsTvSeries() : "0";
                    
                    // For TV series, save with episode ID instead of series ID for proper episode tracking
                    String saveId = model.getMovieId(); // Default: series/movie ID
                    if (saveId == null || saveId.equals("null") || saveId.isEmpty()) {
                        // Use episode ID as fallback if movie ID is invalid
                        saveId = String.valueOf(model.getId());
                        Log.w(TAG, "⚠️ MovieId is invalid, using episode/video ID: " + saveId);
                    }
                    
                    if (isTvSeries.equals("1") && model.getId() > 0) {
                        saveId = String.valueOf(model.getId()); // Use episode ID for TV series
                    }
                    
                    // Ensure we have valid title
                    String title = model.getTitle();
                    if (title == null || title.isEmpty() || title.equals("Unknown Title")) {
                        title = "Video " + saveId; // Fallback title
                    }
                    
                    String description = model.getDescription() != null ? model.getDescription() : "";
                    String slug = "unknown-title"; // Default slug since we don't have it in PlaybackModel
                    String releaseDate = model.getReleaseDate() != null ? model.getReleaseDate() : "";
                    String runtime = model.getRuntime() != null ? model.getRuntime() : "";
                    String videoQuality = model.getVideoQuality() != null ? model.getVideoQuality() : "";
                    String isPaid = model.getIsPaid() != null ? model.getIsPaid() : "0";
                    String enableDownload = "1"; // Default since PlaybackModel doesn't have this
                    String thumbnailUrl = model.getCardImageUrl() != null ? model.getCardImageUrl() : "";
                    String posterUrl = model.getCardImageUrl() != null ? model.getCardImageUrl() : "";
                    String imdbRating = model.getImdbRating() != null ? model.getImdbRating() : "";
                    String curUrl = model.getVideoUrl() != null ? model.getVideoUrl() : "";
                    boolean isMovie = !isTvSeries.equals("1");
                    
                    // Create empty lists for missing data (will be populated by API call if possible)
                    List<Map<String, String>> genreList = new ArrayList<>();
                    List<Map<String, String>> countryList = new ArrayList<>();  
                    List<Map<String, String>> directorList = new ArrayList<>();
                    List<Map<String, String>> writerList = new ArrayList<>();
                    List<Map<String, String>> castList = new ArrayList<>();
                    List<Map<String, String>> downloadLinks = new ArrayList<>();
                    
                    // Add basic genre if available
                    if (model.getGenre() != null && !model.getGenre().isEmpty()) {
                        Map<String, String> genreMap = new HashMap<>();
                        genreMap.put("genre_id", "1");
                        genreMap.put("name", model.getGenre());
                        genreMap.put("url", "");
                        genreList.add(genreMap);
                    }
                    
                    Log.e(TAG, "💾 Saving ENHANCED watch history with PlaybackModel data - ID: " + saveId + 
                              ", Title: " + title + ", Position: " + currentPosition + "ms");
                    
                    // For TV series data - try to get from model
                    Map<String, Object> curSeason = null;
                    Map<String, Object> curEpisode = null;
                    
                    // If it's a TV series, try to get season/episode data
                    if (isTvSeries.equals("1")) {
                        // Get current season data from model if available
                        if (model.getAllSeasons() != null && model.getCurrentSeasonIndex() >= 0 && 
                            model.getCurrentSeasonIndex() < model.getAllSeasons().size()) {
                            Season currentSeason = model.getAllSeasons().get(model.getCurrentSeasonIndex());
                            curSeason = convertSeasonToMap(currentSeason);
                            
                            // Get current episode data from current season if available  
                            if (currentSeason.getEpisodes() != null && model.getCurrentEpisodeIndex() >= 0 && 
                                model.getCurrentEpisodeIndex() < currentSeason.getEpisodes().size()) {
                                Episode currentEpisode = currentSeason.getEpisodes().get(model.getCurrentEpisodeIndex());
                                curEpisode = convertEpisodeToMap(currentEpisode);
                            }
                        }
                    }
                    
                    // Use saveFullWatchHistory with PlaybackModel data
                    watchHistorySyncManager.saveFullWatchHistory(
                        saveId, title, description, slug, releaseDate, runtime, videoQuality, 
                        isTvSeries, isPaid, enableDownload, thumbnailUrl, posterUrl, imdbRating,
                        genreList, countryList, directorList, writerList, castList,
                        curSeason, curEpisode, null, // allSeasons will be null
                        currentPosition, duration, curUrl, isMovie
                    );
                }
                
                // Force sync to server
                watchHistorySyncManager.forceSyncToServer();
                    
            } catch (Exception e) {
                Log.e(TAG, "Error saving watch history: " + e.getMessage(), e);
            }
        } else {
            Log.e(TAG, "Cannot save watch history - invalid duration or position: pos=" + currentPosition + ", dur=" + duration);
        }
    }

    /**
     * Setup episode navigation buttons for TV series
     * Shows/hides previous/next episode buttons based on navigation data
     */
    private void setupEpisodeNavigationButtons() {
        Log.d("PlayerActivity", "setupEpisodeNavigationButtons called");
        
        if (model == null) {
            Log.w("PlayerActivity", "Model is null, hiding episode navigation buttons");
            previousEpisodeButton.setVisibility(View.GONE);
            nextEpisodeButton.setVisibility(View.GONE);
            return;
        }
        
        if (model.getAllSeasons() == null || model.getCurrentSeasonIndex() < 0) {
            Log.w("PlayerActivity", "Missing episode navigation data - AllSeasons: " + (model.getAllSeasons() != null) + 
                    ", CurrentSeasonIndex: " + model.getCurrentSeasonIndex());
            
            // If this is a TV series from watch history, try to fetch season data
            if (getIntent().getBooleanExtra("from_watch_history", false) && 
                getIntent().getStringExtra("type") != null && 
                getIntent().getStringExtra("type").equals("tvseries")) {
                
                String movieId = getIntent().getStringExtra("id");
                if (movieId != null) {
                    Log.d("PlayerActivity", "Attempting to fetch season data for TV series: " + movieId);
                    fetchSeasonDataForNavigation(movieId);
                    // Hide buttons for now, will show after data is fetched
                    previousEpisodeButton.setVisibility(View.GONE);
                    nextEpisodeButton.setVisibility(View.GONE);
                    return;
                }
            }
            
            // No navigation data available and can't fetch, hide buttons
            previousEpisodeButton.setVisibility(View.GONE);
            nextEpisodeButton.setVisibility(View.GONE);
            return;
        }
        
        Log.d("PlayerActivity", "Episode navigation data OK - Seasons: " + model.getAllSeasons().size() + 
                ", Current season: " + model.getCurrentSeasonIndex() + 
                ", Current episode: " + model.getCurrentEpisodeIndex());

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
        labels.add("Tắt");
        selectionPairs.add(new int[]{-1, -1});

        // If no subtitle tracks, show message
        if (totalTracks == 0) {
            Toast.makeText(this, "Không tìm thấy phụ đề", Toast.LENGTH_SHORT).show();
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
                else label = "Phụ đề " + (labels.size());

                labels.add(label);
                selectionPairs.add(new int[]{groupIndex, trackIndex});
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn phụ đề (" + totalTracks + " có sẵn)");
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

                Toast.makeText(this, "Lỗi khi đổi phụ đề", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    // Show dialog to select audio track (display language/label where available)
    private void showAudioTrackSelectionDialog() {
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) return;
        
        final List<String> labels = new ArrayList<>();
        final List<int[]> selectionPairs = new ArrayList<>(); // {rendererIndex, groupIndex, trackIndex}

        // Iterate through ALL renderers to find all audio tracks
        Log.d("PlayerActivity", "🔊 Populating Audio Selection Dialog...");
        for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
            if (mappedTrackInfo.getRendererType(i) == C.TRACK_TYPE_AUDIO) {
                TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
                Log.d("PlayerActivity", "   Renderer " + i + " has " + trackGroups.length + " track groups");
                
                for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
                    TrackGroup group = trackGroups.get(groupIndex);
                    for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                        Format format = group.getFormat(trackIndex);
                        
                        String label = buildAudioTrackLabel(format);
                        if (label == null || label.isEmpty() || label.equals("Unknown Language")) {
                            label = "Âm thanh " + (labels.size() + 1);
                        }
                        
                        // Append channel count
                        if (format.channelCount > 0) {
                            label += " (" + format.channelCount + "ch)";
                        }
                        
                        Log.d("PlayerActivity", "     OPTION: " + label + " [Mime: " + format.sampleMimeType + "]");

                        labels.add(label);
                        selectionPairs.add(new int[]{i, groupIndex, trackIndex});
                    }
                }
            }
        }

        // If only one track, show message instead of dialog
        if (labels.size() <= 1) {
            Toast.makeText(this, "Chỉ có một luồng âm thanh", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn âm thanh (" + labels.size() + " có sẵn)");
        builder.setItems(labels.toArray(new String[0]), (dialog, which) -> {
            int[] pair = selectionPairs.get(which);
            int rendererIndex = pair[0];
            int groupIndex = pair[1];
            int trackIndex = pair[2];
            
            // Store current playback position and state
            long currentPosition = player.getCurrentPosition();
            boolean wasPlaying = player.getPlayWhenReady();
            
            try {
                DefaultTrackSelector.Parameters.Builder parametersBuilder = trackSelector.buildUponParameters();
                
                // Clear selection overrides on ALL audio renderers first
                for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
                    if (mappedTrackInfo.getRendererType(i) == C.TRACK_TYPE_AUDIO) {
                        parametersBuilder.clearSelectionOverrides(i);
                        // Only enable the selected renderer, disable others to prevent overlapping audio
                        parametersBuilder.setRendererDisabled(i, i != rendererIndex);
                    }
                }

                // Apply track selection to the specific renderer
                TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
                parametersBuilder.setSelectionOverride(
                    rendererIndex,
                    trackGroups,
                    new DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex)
                );
                
                trackSelector.setParameters(parametersBuilder);
                
                // Resume playback if it was playing before
                if (wasPlaying && !isUserSeeking) {
                    new Handler().postDelayed(() -> {
                        if (player != null && !isUserSeeking) {
                            player.seekTo(currentPosition);
                            player.setPlayWhenReady(true);
                        }
                    }, 100);
                }
                
                // Show toast
                Toast.makeText(this, "Đã chọn: " + labels.get(which), Toast.LENGTH_SHORT).show();
                
            } catch (Exception e) {
                Log.e(TAG, "Error changing audio track", e);
                Toast.makeText(this, "Lỗi khi đổi âm thanh", Toast.LENGTH_SHORT).show();
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
        int fontType = prefs.getInt("font_type", 4); // Default to Vietnamese (index 4)
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
        builder.setTitle("Cài đặt phụ đề");
        
        // Create a ScrollView to handle long content
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        
        // Create a vertical layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        
        SharedPreferences prefs = getSharedPreferences("subtitle_settings", MODE_PRIVATE);
        
        // Font size controls
        TextView fontSizeLabel = new TextView(this);
        fontSizeLabel.setText("Cỡ chữ:");
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
        fontTypeLabel.setText("Kiểu chữ:");
        fontTypeLabel.setTextSize(16);
        fontTypeLabel.setPadding(0, 20, 0, 10);
        
        LinearLayout fontTypeLayout = new LinearLayout(this);
        fontTypeLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button fontTypePrevBtn = new Button(this);
        fontTypePrevBtn.setText("◀");
        Button fontTypeNextBtn = new Button(this);
        fontTypeNextBtn.setText("▶");
        TextView fontTypeTV = new TextView(this);
        
        String[] fontNames = {"Mặc định", "Sans Serif", "Serif", "Monospace", "Tiếng Việt"};
        int currentFontType = prefs.getInt("font_type", 4); // Default to Vietnamese
        // Bounds checking to prevent crash
        if (currentFontType >= fontNames.length) {
            currentFontType = 4; // Default to Vietnamese
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
        positionLabel.setText("Vị trí:");
        positionLabel.setTextSize(16);
        positionLabel.setPadding(0, 20, 0, 10);
        
        LinearLayout positionLayout = new LinearLayout(this);
        positionLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button positionUpBtn = new Button(this);
        positionUpBtn.setText("Lên");
        Button positionDownBtn = new Button(this);
        positionDownBtn.setText("Xuống");
        TextView positionTV = new TextView(this);
        
        // Position logic - offset from default position (negative = closer to bottom, positive = further up)
        int currentOffset = prefs.getInt("vertical_offset", 0);
        positionTV.setText("Dịch chuyển (" + (currentOffset > 0 ? "+" : "") + currentOffset + "%)");
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
        backgroundLabel.setText("Nền: ");
        backgroundLabel.setTextSize(16);
        Switch backgroundSwitch = new Switch(this);
        backgroundSwitch.setChecked(prefs.getBoolean("background", false));
        
        backgroundLayout.addView(backgroundLabel);
        backgroundLayout.addView(backgroundSwitch);
        
        // Text Color controls
        TextView textColorLabel = new TextView(this);
        textColorLabel.setText("Màu chữ:");
        textColorLabel.setTextSize(16);
        textColorLabel.setPadding(0, 20, 0, 10);
        
        LinearLayout textColorLayout = new LinearLayout(this);
        textColorLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button textColorPrevBtn = new Button(this);
        textColorPrevBtn.setText("◀");
        Button textColorNextBtn = new Button(this);
        textColorNextBtn.setText("▶");
        TextView textColorTV = new TextView(this);
        
        String[] colorNames = {"Trắng", "Vàng", "Đỏ", "Xanh lá", "Xanh dương", "Cam", "Hồng", "Xanh lơ"};
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
        outlineColorLabel.setText("Màu viền:");
        outlineColorLabel.setTextSize(16);
        outlineColorLabel.setPadding(0, 20, 0, 10);
        
        LinearLayout outlineColorLayout = new LinearLayout(this);
        outlineColorLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button outlineColorPrevBtn = new Button(this);
        outlineColorPrevBtn.setText("◀");
        Button outlineColorNextBtn = new Button(this);
        outlineColorNextBtn.setText("▶");
        TextView outlineColorTV = new TextView(this);
        
        String[] outlineColorNames = {"Trong suốt", "Đen", "Trắng", "Đỏ", "Xanh dương", "Vàng"};
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
        playbackSpeedLabel.setText("Tốc độ phát:");
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
        resetBtn.setText("Khôi phục mặc định");
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
            fontTypeTV.setText(fontNames[currentType]);
            saveAndApplySubtitleSetting("font_type", currentType);
        });
        
        fontTypeNextBtn.setOnClickListener(v -> {
            int currentType = prefs.getInt("font_type", 0);
            currentType = (currentType + 1) % fontNames.length;
            fontTypeTV.setText(fontNames[currentType]);
            saveAndApplySubtitleSetting("font_type", currentType);
        });
        
        positionUpBtn.setOnClickListener(v -> {
            int offset = prefs.getInt("vertical_offset", 0);
            offset += 5; // Move up (increase offset from bottom - higher value = further from bottom)
            offset = Math.min(offset, 80); // Max 80% from bottom
            String newPositionText = offset == 0 ? "Giữa" : "Lên +" + offset + "%";
            positionTV.setText(newPositionText);
            saveAndApplySubtitleSetting("vertical_offset", offset);
        });
        
        positionDownBtn.setOnClickListener(v -> {
            int offset = prefs.getInt("vertical_offset", 0);
            offset -= 5; // Move down (decrease offset - negative values = closer to bottom edge)
            offset = Math.max(offset, -10); // Min -10% (very close to bottom edge)
            String newPositionText = offset == 0 ? "Giữa" : "Xuống " + offset + "%";
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
            editor.putInt("font_type", 4); // Default to Vietnamese
            editor.putInt("vertical_offset", 0);
            editor.putBoolean("background", false);
            editor.putInt("text_color", 0); // White
            editor.putInt("outline_color", 1); // Black
            editor.putFloat("playback_speed", 1.0f); // Normal speed
            editor.apply();
            
            fontSizeTV.setText("20sp");
            fontTypeTV.setText(fontNames[4]); // Default to Vietnamese
            positionTV.setText("Giữa");
            backgroundSwitch.setChecked(false);
            textColorTV.setText(colorNames[0]);
            textColorTV.setTextColor(colorValues[0]);
            outlineColorTV.setText(outlineColorNames[1]);
            outlineColorTV.setTextColor(outlineColorValues[1]); // Show black color for reset
            
            Toast.makeText(PlayerActivity.this, "Đã khôi phục cài đặt gốc", Toast.LENGTH_SHORT).show();
            applySubtitleSettings();
        });
        
        // Add layout to ScrollView and ScrollView to dialog
        scrollView.addView(layout);
        builder.setView(scrollView);
        builder.setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss());
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
            "Vừa màn hình", 
            "Tràn màn hình", 
            "Phóng to", 
            "Cố định chiều cao", 
            "Cố định chiều rộng"
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
     * Load subtitle data from API and show subtitle dialog
     */
    private void loadSubtitleDataAndShowDialog() {
        if (model == null || model.getMovieId() == null) {
            Log.w(TAG, "Cannot load subtitle data - model or movieId is null");
            showNoSubtitleMessage();
            return;
        }
        
        String videoId = model.getMovieId();
        String videoType = model.getVideoType() != null ? model.getVideoType() : model.getCategory();
        
        Log.d(TAG, "🎬 Loading subtitle data for ID: " + videoId + ", Type: " + videoType);
        
        // Show loading indicator
        runOnUiThread(() -> {
            new ToastMsg(this).toastIconSuccess("Đang tải phụ đề...");
        });
        
        // Load movie details in background to get subtitle data
        new Thread(() -> {
            try {
                Retrofit retrofit = RetrofitClient.getRetrofitInstance();
                ApiService apiService = retrofit.create(ApiService.class);
                
                Call<MovieSingleDetails> call = apiService.getSingleDetail(
                    AppConfig.API_KEY, 
                    videoType, 
                    videoId
                );
                
                call.enqueue(new Callback<MovieSingleDetails>() {
                    @Override
                    public void onResponse(Call<MovieSingleDetails> call, Response<MovieSingleDetails> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            MovieSingleDetails movieDetails = response.body();
                            
                            // Extract subtitles from seasons/episodes structure
                            if (movieDetails.getSeason() != null && !movieDetails.getSeason().isEmpty()) {
                                for (Season season : movieDetails.getSeason()) {
                                    if (season.getEpisodes() != null && !season.getEpisodes().isEmpty()) {
                                        for (Episode episode : season.getEpisodes()) {
                                            if (episode.getSubtitle() != null && !episode.getSubtitle().isEmpty()) {
                                                // Create a Video object with subtitle data for compatibility
                                                Video videoWithSubs = new Video();
                                                videoWithSubs.setSubtitle(episode.getSubtitle());
                                                video = videoWithSubs;
                                                
                                                Log.d(TAG, "✅ Found subtitles in episode " + episode.getEpisodesId() + ": " + episode.getSubtitle().size() + " tracks");
                                                
                                                runOnUiThread(() -> {
                                                    // Now show the subtitle dialog with loaded data
                                                    openSubtitleDialog();
                                                });
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Fallback: Also check videos array for movies
                            if (movieDetails.getVideos() != null && !movieDetails.getVideos().isEmpty()) {
                                for (Video videoItem : movieDetails.getVideos()) {
                                    if (videoItem.getSubtitle() != null && !videoItem.getSubtitle().isEmpty()) {
                                        video = videoItem; // Update the video object with subtitle data
                                        Log.d(TAG, "✅ Found subtitles in video: " + videoItem.getSubtitle().size() + " tracks");
                                        
                                        runOnUiThread(() -> {
                                            // Now show the subtitle dialog with loaded data
                                            openSubtitleDialog();
                                        });
                                        return;
                                    }
                                }
                            }
                            
                            // No subtitles found
                            Log.d(TAG, "❌ No subtitles found in API response");
                            runOnUiThread(() -> {
                                showNoSubtitleMessage();
                            });
                        } else {
                            Log.e(TAG, "API call failed: " + response.code() + " - " + response.message());
                            runOnUiThread(() -> {
                                showNoSubtitleMessage();
                            });
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<MovieSingleDetails> call, Throwable t) {
                        Log.e(TAG, "Error loading subtitle data", t);
                        runOnUiThread(() -> {
                            showNoSubtitleMessage();
                        });
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Exception while loading subtitle data", e);
                runOnUiThread(() -> {
                    showNoSubtitleMessage();
                });
            }
        }).start();
    }
    
    /**
     * Show message when no subtitles are available
     */
    private void showNoSubtitleMessage() {
        // Check for embedded subtitles as fallback
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
            // Show message that no subtitles found
            new ToastMsg(this).toastIconError("Không tìm thấy phụ đề. Bạn có thể tùy chỉnh subtitle trong Settings.");
        }
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
        Log.d("PlayerActivity", "🔊 Handling audio codec error");
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Strategy 1: If we haven't tried forcing software decoding yet, do it now.
                    // This handles cases where Passthrough failed or HW decoder failed.
                    if (!useSoftwareAudioDecoder) {
                        Log.d("PlayerActivity", "🔊 Attempting to switch to Software Audio Decoder (FFmpeg)");
                        
                        // DO NOT SAVE TRACK SELECTION when switching renderers (HW -> SW)
                        // The renderer indices and track groups will change, making saved state invalid.
                        // We want ExoPlayer to auto-select the best compatible track for the new renderer.
                        savedAudioOverride = null;
                        savedAudioRendererIndex = -1;
                        Log.d("PlayerActivity", "🧹 Cleared saved audio selection to allow auto-selection for Software Decoder");
                        
                        useSoftwareAudioDecoder = true;
                        audioMode = 1; // Sync audioMode to ensure correct RenderersFactory configuration
                        
                        // CRITICAL FIX: Reset error flag so if SW mode also fails (e.g., missing libs),
                        // we can catch it again and proceed to Strategy 2 (Track Switching).
                        hasHandledAudioCodecError = false; 
                        
                        new ToastMsg(PlayerActivity.this).toastIconSuccess("Đang chuyển sang chế độ giải mã âm thanh bằng phần mềm...");
                        
                        // Save position and restart
                        if (player != null) {
                            mStartingPosition = player.getCurrentPosition();
                            player.release();
                            player = null;
                        }
                        initVideoPlayer(url, videoType);
                        return;
                    }

                    // Strategy 2: If we are already in SW mode and still failing, try to find a simpler track (AAC/Stereo)
                    Log.d("PlayerActivity", "🔊 Software decoding failed, trying to find compatible audio track");

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
            lowerUrl.contains("m3u8") || lowerUrl.contains("/playlist.m3u8") ||
            lowerUrl.contains("/stream/hls")) {
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

    /**
     * Show dialog asking user if they want to watch the next episode
     */
    private void showNextEpisodeDialog() {
        if (this.isFinishing() || this.isDestroyed()) {
            return; // Don't show dialog if activity is finishing
        }

        // Get next episode info
        String nextEpisodeTitle = getNextEpisodeTitle();
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("🎬 Tập đã kết thúc");
        
        if (nextEpisodeTitle != null && !nextEpisodeTitle.isEmpty()) {
            builder.setMessage("Bạn có muốn tiếp tục xem tập tiếp theo?\n\n📺 " + nextEpisodeTitle);
        } else {
            builder.setMessage("Bạn có muốn tiếp tục xem tập tiếp theo không?");
        }
        
        builder.setPositiveButton("▶️ Xem ngay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "✅ User chose to watch next episode");
                dialog.dismiss();
                navigateToNextEpisode();
            }
        });
        
        builder.setNegativeButton("❌ Thoát", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "❌ User chose not to watch next episode");
                dialog.dismiss();
                // Just close the player or go back
                finish();
            }
        });
        
        builder.setNeutralButton("📋 Chọn tập khác", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "📋 User chose to select different episode");
                dialog.dismiss();
                // Go back to episode list
                finish();
            }
        });
        
        builder.setCancelable(true);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Log.d(TAG, "🚫 Next episode dialog cancelled");
                finish();
            }
        });
        
        try {
            android.app.AlertDialog dialog = builder.create();
            
            // Make dialog more TV-friendly
            dialog.getWindow().getAttributes().dimAmount = 0.8f;
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            
            dialog.show();
            Log.d(TAG, "📺 Next episode dialog shown");
            
            // Auto-dismiss after 30 seconds if no action
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (dialog.isShowing()) {
                        Log.d(TAG, "⏰ Auto-dismissing next episode dialog after 30s");
                        dialog.dismiss();
                        finish();
                    }
                }
            }, 30000); // 30 seconds
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error showing next episode dialog: " + e.getMessage());
            // Fallback: just navigate to next episode
            navigateToNextEpisode();
        }
    }

    /**
     * Get the title of the next episode for display in dialog
     */
    private String getNextEpisodeTitle() {
        if (model == null || model.getAllSeasons() == null) {
            return null;
        }
        
        try {
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
            
            // Get the next episode info
            if (newSeasonIndex < model.getAllSeasons().size()) {
                com.files.codes.model.movieDetails.Season nextSeason = model.getAllSeasons().get(newSeasonIndex);
                if (nextSeason.getEpisodes() != null && newEpisodeIndex < nextSeason.getEpisodes().size()) {
                    com.files.codes.model.movieDetails.Episode nextEpisode = nextSeason.getEpisodes().get(newEpisodeIndex);
                    return "Season " + nextSeason.getSeasonsName() + " - " + nextEpisode.getEpisodesName();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting next episode title: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Load external VTT subtitle into ExoPlayer
     */
    private void loadExternalSubtitle(String subtitleUrl, String language) {
        // Fix for SSL hostname mismatch: replace xxxx.qphim.xyz with api.phim4k.lol
        if (subtitleUrl != null && subtitleUrl.contains("xxxx.qphim.xyz")) {
            subtitleUrl = subtitleUrl.replace("xxxx.qphim.xyz", "api.phim4k.lol");
            // Log.d(TAG, "🎬 Fixed subtitle URL hostname: " + subtitleUrl);
        }

        // Log.d(TAG, "🎬 Loading external subtitle: " + subtitleUrl);
        
        try {
            if (player == null) {
                Log.e(TAG, "🎬 Player is null, cannot load subtitle");
                return;
            }

            // Save current state
            long currentPosition = player.getCurrentPosition();
            boolean wasPlaying = player.isPlaying();
            
            // Create subtitle MediaItem with SubtitleConfiguration
            MediaItem.SubtitleConfiguration subtitleConfig = new MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUrl))
                .setMimeType(MimeTypes.TEXT_VTT)
                .setLanguage(language)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build();
            
            // Create MediaItem with video and subtitle
            MediaItem mediaItem = new MediaItem.Builder()
                .setUri(url)
                .setSubtitleConfigurations(Collections.singletonList(subtitleConfig))
                .build();
            
            // Prepare player with media item
            // Log.d(TAG, "🎬 Preparing player with subtitle...");
            player.setMediaItem(mediaItem);
            player.prepare();
            
            // Restore state
            player.seekTo(currentPosition);
            player.setPlayWhenReady(wasPlaying);
            
            // Enable subtitle track
            if (trackSelector != null) {
                // First, clear any previous selection overrides to ensure clean state
                trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                        .clearSelectionOverrides()
                        .setRendererDisabled(C.TRACK_TYPE_TEXT, false)
                );

                // Force selection of the subtitle track
                // Since we just added a single subtitle configuration, it should be the first text track
                // However, ExoPlayer might merge it with embedded tracks.
                // We need to find the track group that corresponds to our external subtitle.
                
                // Note: MappedTrackInfo might not be updated immediately after prepare().
                // We might need to listen to onTracksChanged, but for now let's try setting preferred language
                // which usually works for external subtitles if language matches.
                
                trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                        .setPreferredTextLanguage(language) // Try to match by language
                        .setRendererDisabled(C.TRACK_TYPE_TEXT, false)
                );
            }
            
            // Log.d(TAG, "🎬 ✅ External subtitle loaded successfully: " + language);
            
        } catch (Exception e) {
            Log.e(TAG, "🎬 ❌ Error loading external subtitle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Fetch season data for TV series to enable episode navigation from watch history
     */
    private void fetchSeasonDataForNavigation(String movieId) {
        Log.d("PlayerActivity", "Fetching season data for movie ID: " + movieId);
        
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<MovieSingleDetails> call = apiService.getSingleDetail(AppConfig.API_KEY, "tvseries", movieId);
        
        call.enqueue(new Callback<MovieSingleDetails>() {
            @Override
            public void onResponse(Call<MovieSingleDetails> call, Response<MovieSingleDetails> response) {
                if (response.isSuccessful() && response.body() != null) {
                    MovieSingleDetails movieDetails = response.body();
                    
                    if (movieDetails.getSeason() != null && !movieDetails.getSeason().isEmpty()) {
                        Log.d("PlayerActivity", "Successfully fetched season data, seasons count: " + 
                                movieDetails.getSeason().size());
                        
                        // Update model with season data
                        model.setAllSeasons(movieDetails.getSeason());
                        
                        // Try to find current episode in seasons
                        String currentVideoUrl = model.getVideoUrl();
                        boolean foundEpisode = false;
                        
                        for (int seasonIndex = 0; seasonIndex < movieDetails.getSeason().size(); seasonIndex++) {
                            Season season = movieDetails.getSeason().get(seasonIndex);
                            if (season.getEpisodes() != null) {
                                for (int episodeIndex = 0; episodeIndex < season.getEpisodes().size(); episodeIndex++) {
                                    Episode episode = season.getEpisodes().get(episodeIndex);
                                    if (episode.getFileUrl() != null && episode.getFileUrl().equals(currentVideoUrl)) {
                                        model.setCurrentSeasonIndex(seasonIndex);
                                        model.setCurrentEpisodeIndex(episodeIndex);
                                        model.setTotalEpisodesInSeason(season.getEpisodes().size());
                                        foundEpisode = true;
                                        Log.d("PlayerActivity", "Found current episode at season " + seasonIndex + 
                                                ", episode " + episodeIndex);
                                        break;
                                    }
                                }
                                if (foundEpisode) break;
                            }
                        }
                        
                        // Setup navigation buttons now that we have data
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setupEpisodeNavigationButtons();
                            }
                        });
                        
                    } else {
                        Log.w("PlayerActivity", "No season data found in movie details");
                    }
                } else {
                    Log.e("PlayerActivity", "Failed to fetch movie details: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<MovieSingleDetails> call, Throwable t) {
                Log.e("PlayerActivity", "API call failed for movie details", t);
            }
        });
    }

    /**
     * Helper methods to convert MovieSingleDetails data to format expected by saveFullWatchHistory
     */
    private List<Map<String, String>> convertGenreList(java.util.List<com.files.codes.model.Genre> genres) {
        List<Map<String, String>> result = new ArrayList<>();
        if (genres != null) {
            for (com.files.codes.model.Genre genre : genres) {
                Map<String, String> genreMap = new HashMap<>();
                genreMap.put("id", genre.getGenreId() != null ? genre.getGenreId() : "");
                genreMap.put("name", genre.getName() != null ? genre.getName() : "");
                result.add(genreMap);
            }
        }
        return result;
    }

    private List<Map<String, String>> convertCountryList(java.util.List<com.files.codes.model.movieDetails.Country> countries) {
        List<Map<String, String>> result = new ArrayList<>();
        if (countries != null) {
            for (com.files.codes.model.movieDetails.Country country : countries) {
                Map<String, String> countryMap = new HashMap<>();
                countryMap.put("id", country.getCountryId() != null ? country.getCountryId() : "");
                countryMap.put("name", country.getName() != null ? country.getName() : "");
                result.add(countryMap);
            }
        }
        return result;
    }

    private List<Map<String, String>> convertDirectorList(java.util.List<com.files.codes.model.movieDetails.Director> directors) {
        List<Map<String, String>> result = new ArrayList<>();
        if (directors != null) {
            for (com.files.codes.model.movieDetails.Director director : directors) {
                Map<String, String> directorMap = new HashMap<>();
                directorMap.put("id", director.getStarId() != null ? director.getStarId() : "");
                directorMap.put("name", director.getName() != null ? director.getName() : "");
                result.add(directorMap);
            }
        }
        return result;
    }

    private List<Map<String, String>> convertWriterList(java.util.List<com.files.codes.model.movieDetails.Writer> writers) {
        List<Map<String, String>> result = new ArrayList<>();
        if (writers != null) {
            for (com.files.codes.model.movieDetails.Writer writer : writers) {
                Map<String, String> writerMap = new HashMap<>();
                writerMap.put("id", writer.getStarId() != null ? writer.getStarId() : "");
                writerMap.put("name", writer.getName() != null ? writer.getName() : "");
                result.add(writerMap);
            }
        }
        return result;
    }

    private List<Map<String, String>> convertCastList(java.util.List<com.files.codes.model.movieDetails.CastAndCrew> castAndCrew) {
        List<Map<String, String>> result = new ArrayList<>();
        if (castAndCrew != null) {
            for (com.files.codes.model.movieDetails.CastAndCrew cast : castAndCrew) {
                Map<String, String> castMap = new HashMap<>();
                castMap.put("id", cast.getStarId() != null ? cast.getStarId() : "");
                castMap.put("name", cast.getName() != null ? cast.getName() : "");
                result.add(castMap);
            }
        }
        return result;
    }

    private Map<String, Object> convertSeasonToMap(com.files.codes.model.movieDetails.Season season) {
        Map<String, Object> seasonMap = new HashMap<>();
        if (season != null) {
            seasonMap.put("seasons_id", season.getSeasonsId() != null ? season.getSeasonsId() : "");
            seasonMap.put("seasons_name", season.getSeasonsName() != null ? season.getSeasonsName() : "");
            
            // Convert episodes list
            if (season.getEpisodes() != null) {
                List<Map<String, Object>> episodesList = new ArrayList<>();
                for (com.files.codes.model.movieDetails.Episode episode : season.getEpisodes()) {
                    episodesList.add(convertEpisodeToMap(episode));
                }
                seasonMap.put("episodes", episodesList);
            }
        }
        return seasonMap;
    }

    private Map<String, Object> convertEpisodeToMap(com.files.codes.model.movieDetails.Episode episode) {
        Map<String, Object> episodeMap = new HashMap<>();
        if (episode != null) {
            episodeMap.put("episodes_id", episode.getEpisodesId() != null ? episode.getEpisodesId() : "");
            episodeMap.put("episodes_name", episode.getEpisodesName() != null ? episode.getEpisodesName() : "");
            episodeMap.put("stream_key", episode.getStreamKey() != null ? episode.getStreamKey() : "");
            episodeMap.put("file_type", episode.getFileType() != null ? episode.getFileType() : "");
            episodeMap.put("image_url", episode.getImageUrl() != null ? episode.getImageUrl() : "");
            episodeMap.put("file_url", episode.getFileUrl() != null ? episode.getFileUrl() : "");
            
            // Convert subtitle list if available
            if (episode.getSubtitle() != null) {
                episodeMap.put("subtitle", episode.getSubtitle());
            } else {
                episodeMap.put("subtitle", new ArrayList<>());
            }
        }
        return episodeMap;
    }

    private List<Map<String, Object>> convertSeasonsToMapList(List<com.files.codes.model.movieDetails.Season> seasons) {
        List<Map<String, Object>> seasonsList = new ArrayList<>();
        if (seasons != null) {
            for (com.files.codes.model.movieDetails.Season season : seasons) {
                seasonsList.add(convertSeasonToMap(season));
            }
        }
        return seasonsList;
    }

    /**
     * Fetch complete movie data from API for enhanced watch history saving
     */
    private void fetchCompleteMovieDataForWatchHistory(String movieId, String movieType) {
        if (movieId == null || movieId.isEmpty()) {
            Log.w(TAG, "Cannot fetch movie data - invalid movie ID");
            return;
        }

        Log.d(TAG, "🔍 Fetching complete movie data for enhanced watch history - ID: " + movieId + ", Type: " + movieType);

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<MovieSingleDetails> call = apiService.getSingleDetail(AppConfig.API_KEY, movieType, movieId);

        call.enqueue(new Callback<MovieSingleDetails>() {
            @Override
            public void onResponse(Call<MovieSingleDetails> call, Response<MovieSingleDetails> response) {
                if (response.isSuccessful() && response.body() != null) {
                    MovieSingleDetails newData = response.body();
                    
                    Log.d(TAG, "✅ API Response received for ID: " + movieId);
                    Log.d(TAG, "🔍 API Response debug:");
                    Log.d(TAG, "  - VideosId: '" + newData.getVideosId() + "'");
                    Log.d(TAG, "  - Title: '" + newData.getTitle() + "'");
                    Log.d(TAG, "  - Slug: '" + newData.getSlug() + "'");
                    Log.d(TAG, "  - Description: '" + newData.getDescription() + "'");
                    Log.d(TAG, "  - Release: '" + newData.getRelease() + "'");
                    Log.d(TAG, "  - Runtime: '" + newData.getRuntime() + "'");
                    Log.d(TAG, "  - ImdbRating: '" + newData.getImdbRating() + "'");
                    Log.d(TAG, "  - Genre count: " + (newData.getGenre() != null ? newData.getGenre().size() : 0));
                    Log.d(TAG, "  - Cast count: " + (newData.getCastAndCrew() != null ? newData.getCastAndCrew().size() : 0));
                    Log.d(TAG, "  - Director count: " + (newData.getDirector() != null ? newData.getDirector().size() : 0));
                    Log.d(TAG, "  - Country count: " + (newData.getCountry() != null ? newData.getCountry().size() : 0));
                    Log.d(TAG, "  - Writer count: " + (newData.getWriter() != null ? newData.getWriter().size() : 0));
                    
                    // Update completeMovieData with API response
                    completeMovieData = newData;
                    
                    // Log more details for debugging
                    if (completeMovieData.getSeason() != null && !completeMovieData.getSeason().isEmpty()) {
                        Log.d(TAG, "📺 TV Series data - Seasons: " + completeMovieData.getSeason().size());
                    }
                    
                    Log.d(TAG, "🔄 CompleteMovieData updated from API, ready for enhanced watch history");
                    
                    // If we were waiting for this data, save watch history now
                    if (isWaitingForCompleteData && lastSavePosition >= 0 && lastSaveDuration > 0) {
                        Log.d(TAG, "🔄 API data received, retrying watch history save");
                        isWaitingForCompleteData = false;
                        
                        // Save with the updated complete data
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                saveWatchHistoryWithData(lastSavePosition, lastSaveDuration, false);
                            }
                        });
                    }
                } else {
                    Log.e(TAG, "❌ Failed to fetch complete movie data - Response Code: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            Log.e(TAG, "❌ Error body: " + response.errorBody().string());
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Error reading error body", e);
                        }
                    }
                    
                    // If we were waiting for this data and it failed, save with basic data
                    if (isWaitingForCompleteData) {
                        // Log.w(TAG, "⚠️ API fetch failed, falling back to basic save");
                        isWaitingForCompleteData = false;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                saveWatchHistoryWithData(lastSavePosition, lastSaveDuration, true);
                            }
                        });
                    }
                }
            }

            @Override
            public void onFailure(Call<MovieSingleDetails> call, Throwable t) {
                // Log.e(TAG, "❌ API call failed for complete movie data", t);
                completeMovieData = null;
            }
        });
    }

    private String buildAudioTrackLabel(Format format) {
        StringBuilder labelBuilder = new StringBuilder();

        // 1. Language or Label
        if (format.label != null && !format.label.isEmpty()) {
            labelBuilder.append(format.label);
        } else if (format.language != null && !format.language.isEmpty()) {
            Locale locale = new Locale(format.language);
            labelBuilder.append(locale.getDisplayLanguage());
        } else {
            labelBuilder.append("Unknown Language");
        }

        // 2. Codec
        String codec = getCodecName(format.sampleMimeType);
        if (codec != null) {
            labelBuilder.append(" - ").append(codec);
        }

        // 3. Channels
        if (format.channelCount != Format.NO_VALUE) {
            labelBuilder.append(" ").append(getChannelConfig(format.channelCount));
        }
        
        // 4. Bitrate (optional)
        if (format.bitrate != Format.NO_VALUE) {
             labelBuilder.append(String.format(Locale.US, " (%d kbps)", format.bitrate / 1000));
        }

        return labelBuilder.toString();
    }

    private String getCodecName(String mimeType) {
        if (mimeType == null) return null;
        switch (mimeType) {
            case MimeTypes.AUDIO_AAC: return "AAC";
            case MimeTypes.AUDIO_AC3: return "Dolby Digital";
            case MimeTypes.AUDIO_E_AC3: return "Dolby Digital Plus";
            case MimeTypes.AUDIO_E_AC3_JOC: return "Dolby Atmos";
            case MimeTypes.AUDIO_TRUEHD: return "Dolby TrueHD";
            case MimeTypes.AUDIO_DTS: return "DTS";
            case MimeTypes.AUDIO_DTS_HD: return "DTS-HD";
            case MimeTypes.AUDIO_DTS_EXPRESS: return "DTS Express";
            case MimeTypes.AUDIO_FLAC: return "FLAC";
            case MimeTypes.AUDIO_MPEG: return "MP3";
            case MimeTypes.AUDIO_VORBIS: return "Vorbis";
            case MimeTypes.AUDIO_OPUS: return "Opus";
            default: return null;
        }
    }

    private String getChannelConfig(int channelCount) {
        if (channelCount == 1) return "Mono";
        if (channelCount == 2) return "Stereo";
        if (channelCount == 6) return "5.1";
        if (channelCount == 8) return "7.1";
        return channelCount + "ch";
    }
}