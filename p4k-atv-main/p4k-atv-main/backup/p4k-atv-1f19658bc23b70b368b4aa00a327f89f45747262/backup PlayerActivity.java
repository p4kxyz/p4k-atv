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

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
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
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.files.codes.R;
import com.files.codes.database.DatabaseHelper;
import com.files.codes.model.PlaybackModel;
import com.files.codes.model.Video;
import com.files.codes.model.movieDetails.Subtitle;
import com.files.codes.utils.ToastMsg;
import com.files.codes.utils.TvRecommendationManager;
import com.files.codes.model.VideoContent;
import com.files.codes.view.adapter.ServerAdapter;
import com.files.codes.view.adapter.SubtitleListAdapter;
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
    private String videoType = "";
    private String category = "";
    private int visible;
    private ImageButton serverButton, fastForwardButton, subtitleButton, subtitleSettingsButton, audioTrackButton, aspectRatioButton;
    private TextView movieTitleTV, movieDescriptionTV;
    private ImageView posterImageView, posterImageViewForTV;
    private RelativeLayout seekBarLayout;
    private TextView liveTvTextInController;
    private ProgressBar progressBar;
    private PowerManager.WakeLock wakeLock;
    private MediaSession session;
    private TvRecommendationManager tvRecommendationManager;

    private long mChannelId;
    private long mStartingPosition;
    private PlaybackModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        mChannelId = getIntent().getLongExtra(VideoPlaybackActivity.EXTRA_CHANNEL_ID, -1L);
        mStartingPosition = getIntent().getLongExtra(VideoPlaybackActivity.EXTRA_POSITION, -1L);

        model = (PlaybackModel) getIntent().getSerializableExtra(VideoPlaybackActivity.EXTRA_VIDEO);


        assert model != null;
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
                Intent intent = new Intent(PlayerActivity.this, VideoDetailsActivity.class);
                intent.putExtra("type", model.getCategory());
                intent.putExtra("id", model.getMovieId());
                intent.putExtra("thumbImage", model.getCardImageUrl());
                startActivity(intent, null);
                finish();
            }
        }

        intiViews();
        initVideoPlayer(url, videoType);
        
        // Initialize TV Recommendation Manager for Android TV home screen
        tvRecommendationManager = new TvRecommendationManager(this);
        addToTvRecommendations();
        
        // Sync watch history from API to Android TV home screen
        syncWatchHistoryToTvHome();
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
        liveTvTextInController = findViewById(R.id.live_tv);
        seekBarLayout = findViewById(R.id.seekbar_layout);
        if (category.equalsIgnoreCase("tv")) {
            serverButton.setVisibility(View.GONE);
            subtitleButton.setVisibility(View.GONE);
            //seekBarLayout.setVisibility(View.GONE);
            fastForwardButton.setVisibility(View.GONE);
            liveTvTextInController.setVisibility(View.VISIBLE);
            posterImageView.setVisibility(View.GONE);
            posterImageViewForTV.setVisibility(VISIBLE);
            seekBarLayout.setVisibility(View.GONE);
        }

        if (category.equalsIgnoreCase("tvseries")) {
            serverButton.setVisibility(View.GONE);
            //hide subtitle button if there is no subtitle
            if (video != null) {
                if (video.getSubtitle().isEmpty()) {
                    subtitleButton.setVisibility(View.GONE);
                }
            } else {
                subtitleButton.setVisibility(View.GONE);
            }
            // audio tracks
            audioTrackButton.setVisibility(View.GONE);
        }

        if (category.equalsIgnoreCase("movie")) {
            if (model.getVideoList() != null)
                videos.clear();
            videos = model.getVideoList();
            //hide subtitle button if there is no subtitle
            if (video != null) {
                if (video.getSubtitle().isEmpty()) {
                    subtitleButton.setVisibility(View.GONE);
                }
            } else {
                subtitleButton.setVisibility(View.GONE);
            }
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
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "OXOO-TV:PlayerActivity");
        
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
        Log.e("RemoteKey", "DPAD_HOME");
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
                Log.e("RemoteKey", "DPAD_DOWN");
                if (!exoPlayerView.isControllerVisible()) {
                    exoPlayerView.showController();
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                Log.e("RemoteKey", "DPAD_RIGHT");
                if (!exoPlayerView.isControllerVisible()) {
                    exoPlayerView.showController();
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Log.e("RemoteKey", "DPAD_LEFT");
                if (!exoPlayerView.isControllerVisible()) {
                    exoPlayerView.showController();
                }
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                Log.e("RemoteKey", "DPAD_CENTER");
                if (!exoPlayerView.isControllerVisible()) {
                    exoPlayerView.showController();
                }
                break;
            case KeyEvent.KEYCODE_BACK:
                Log.e("RemoteKey", "DPAD_BACK");
                if (exoPlayerView.isControllerVisible()) {
                    Log.d("PlayerActivity", "Back pressed - hiding controls");
                    exoPlayerView.hideController();
                    return true; // Consume the event, don't finish
                } else {
                    Log.d("PlayerActivity", "Back pressed - exiting player");
                    releasePlayer();
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
                Log.e("RemoteKey", "DPAD_ESCAPE");
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
        if (video != null) {
            if (!video.getSubtitle().isEmpty()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(PlayerActivity.this);
                View view = LayoutInflater.from(PlayerActivity.this).inflate(R.layout.layout_subtitle_dialog, null);
                RecyclerView serverRv = view.findViewById(R.id.serverRv);
                SubtitleListAdapter adapter = new SubtitleListAdapter(PlayerActivity.this, video.getSubtitle());
                serverRv.setLayoutManager(new LinearLayoutManager(PlayerActivity.this));
                serverRv.setHasFixedSize(true);
                serverRv.setAdapter(adapter);

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
                //click event
                adapter.setListener(new SubtitleListAdapter.OnSubtitleItemClickListener() {
                    @Override
                    public void onSubtitleItemClick(View view, Subtitle subtitle, int position, SubtitleListAdapter.SubtitleViewHolder holder) {
                        setSelectedSubtitle(mediaSource, subtitle.getUrl());
                        dialog.dismiss();
                    }
                });

            } else {
                // Nếu không có subtitle rời, kiểm tra subtitle nhúng trong player
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
                    showSubtitleSelectionDialog();
                } else {
                    new ToastMsg(this).toastIconError(getResources().getString(R.string.no_subtitle_found));
                }
            }
        } else {
            new ToastMsg(this).toastIconError(getResources().getString(R.string.no_subtitle_found));
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

        player = new ExoPlayer.Builder(PlayerActivity.this)
                .setTrackSelector(trackSelector)
                .build();
        exoPlayerView.setPlayer(player);
        // Set default to original aspect ratio (fit video to screen without cropping)
        exoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        exoPlayerView.setControllerShowTimeoutMs(5000);
        player.setPlayWhenReady(true);

        // Apply subtitle settings
        applySubtitleSettings();

        Uri uri = Uri.parse(url);

        switch (type) {
            case "hls":
                mediaSource = hlsMediaSource(uri, PlayerActivity.this);
                break;
           /* case "youtube":
                extractYoutubeUrl(url, PlayerActivity.this, 18);
                break;
            case "youtube-live":
                extractYoutubeUrl(url, PlayerActivity.this, 133);
                break;*/
            case "rtmp":
                mediaSource = rtmpMediaSource(uri);
                break;
            default:
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
                    //remove now playing card
                    //mediaSessionHelper.stopMediaSession();
                } else {
                    // player paused in any state
                    isPlaying = false;
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        exoPlayerView.setControllerVisibilityListener(new PlayerControlView.VisibilityListener() {
            @Override
            public void onVisibilityChange(int visibility) {
                visible = visibility;
            }
        });
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
        Log.d(TAG, sb.toString());
        trackInfoLogged = true;
    }

    private void seekToStartPosition() {
        // Skip ahead if given a starting position.
        if (mStartingPosition > -1L) {
            if (player.getPlayWhenReady()) {
                Log.d("VideoFragment", "Is prepped, seeking to " + mStartingPosition);
                player.seekTo(mStartingPosition);
            }
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
        // Check if controls are currently showing
        boolean controlsVisible = exoPlayerView.isControllerVisible();
        
        if (controlsVisible) {
            // If controls are visible, hide them first
            exoPlayerView.hideController();
            Log.d(TAG, "Back pressed - hiding controls");
        } else {
            // If controls are already hidden, then exit player
            Log.d(TAG, "Back pressed - exiting player");
            releasePlayer();
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
                if (wasPlaying) {
                    new Handler().postDelayed(() -> {
                        if (player != null) {
                            player.seekTo(currentPosition);
                            player.setPlayWhenReady(true);
                        }
                    }, 100);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error changing subtitle track", e);
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
                if (wasPlaying) {
                    // Small delay to ensure track change is processed
                    new Handler().postDelayed(() -> {
                        if (player != null) {
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
                    Color.WHITE,
                    background ? Color.BLACK : Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    CaptionStyleCompat.EDGE_TYPE_NONE,
                    Color.WHITE,
                    typeface
            );
            subtitleView.setStyle(style);
            subtitleView.setFixedTextSize(Cue.TEXT_SIZE_TYPE_ABSOLUTE, fontSize);

            // Apply vertical offset - from bottom position
            // verticalOffset: negative values = closer to bottom edge, positive = further from bottom
            float bottomPadding = 0.01f + (verticalOffset * 0.01f); // 1% base + offset
            bottomPadding = Math.max(0.0f, Math.min(0.85f, bottomPadding)); // Clamp between 0% and 85%
            subtitleView.setBottomPaddingFraction(bottomPadding);
            
            Log.d(TAG, "Applied subtitle position: " + verticalOffset + "% offset, bottomPadding: " + bottomPadding);
        }
    }


    // =================================================================================
    // DUAL SUBTITLE RELATED METHODS (TO BE REMOVED OR REFACTORED)
    // =================================================================================

    /*
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
            saveAndApplySubtitleSetting("vertical_offset", offset);
            positionTV.setText("Offset (" + (offset > 0 ? "+" : "") + offset + "%)");
        });
        
        positionDownBtn.setOnClickListener(v -> {
            int offset = prefs.getInt("vertical_offset", 0);
            offset -= 5; // Move down (decrease offset - negative values = closer to bottom edge)
            offset = Math.max(offset, -10); // Min -10% (very close to bottom edge)
            saveAndApplySubtitleSetting("vertical_offset", offset);
            positionTV.setText("Offset (" + (offset > 0 ? "+" : "") + offset + "%)");
        });
        
        backgroundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveAndApplySubtitleSetting("background", isChecked);
        });
        
        resetBtn.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("font_size", 20);
            editor.putInt("font_type", 0);
            editor.putInt("vertical_offset", 0);
            editor.putBoolean("background", false);
            editor.apply();
            
            // Update UI
            fontSizeTV.setText("20sp");
            fontTypeTV.setText(fontNames[0]);
            positionTV.setText("Offset (0%)");
            backgroundSwitch.setChecked(false);
            
            applySubtitleSettings();
        });
        
        builder.setView(layout);
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
                content.setTitle(model.getTitle());
                content.setDescription(model.getDescription());
                content.setThumbnailUrl(model.getCardImageUrl()); // Use cardImageUrl
                content.setVideoUrl(model.getVideoUrl());
                content.setId(String.valueOf(System.currentTimeMillis()));
                
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


}