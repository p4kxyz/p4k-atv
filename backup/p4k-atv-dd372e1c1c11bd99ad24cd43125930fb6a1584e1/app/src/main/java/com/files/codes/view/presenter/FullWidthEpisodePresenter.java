package com.files.codes.view.presenter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.leanback.widget.Presenter;

import com.files.codes.R;
import com.files.codes.database.DatabaseHelper;
import com.files.codes.model.PlaybackModel;
import com.files.codes.model.Video;
import com.files.codes.model.movieDetails.Episode;
import com.files.codes.utils.LoginAlertDialog;
import com.files.codes.utils.PaidDialog;
import com.files.codes.utils.PreferenceUtils;
import com.files.codes.utils.ToastMsg;
import com.files.codes.view.PlayerActivity;
import com.files.codes.view.VideoPlaybackActivity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Full-width presenter for TV Series episodes
 * Displays episodes in a single-line format: "number. Episode Name"
 */
public class FullWidthEpisodePresenter extends Presenter {
    private static Context mContext;
    private int seasonIndex = -1;
    private int pageStartIndex = 0;
    private java.util.List<com.files.codes.model.movieDetails.Season> allSeasons;

    // Default constructor for backward compatibility
    public FullWidthEpisodePresenter() {
    }

    // Constructor with navigation data
    public FullWidthEpisodePresenter(int seasonIndex, int pageStartIndex, 
                                     java.util.List<com.files.codes.model.movieDetails.Season> allSeasons) {
        this.seasonIndex = seasonIndex;
        this.pageStartIndex = pageStartIndex;
        this.allSeasons = allSeasons;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        mContext = parent.getContext();
        
        // Create full-width TextView
        TextView textView = new TextView(mContext);
        textView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            150 // Fixed height for consistent spacing
        ));
        textView.setPadding(40, 20, 40, 20);
        textView.setTextSize(18);
        textView.setTextColor(Color.WHITE);
        textView.setBackgroundColor(Color.parseColor("#1A1A1A"));
        textView.setFocusable(true);
        textView.setFocusableInTouchMode(true);
        textView.setSingleLine(true);
        textView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textView.setGravity(android.view.Gravity.CENTER); // Căn giữa
        
        return new ViewHolder(textView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        final Episode video = (Episode) item;
        TextView textView = (TextView) viewHolder.view;
        
        // Chỉ hiển thị tên tập (không có số)
        textView.setText(video.getEpisodesName());
        
        final DatabaseHelper db = new DatabaseHelper(mContext);

        // On click, play episode
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playEpisode(video, db);
            }
        });
        
        // Focus change effect
        textView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    textView.setBackgroundColor(Color.parseColor("#FF6B35"));
                    textView.setTextColor(Color.WHITE);
                } else {
                    textView.setBackgroundColor(Color.parseColor("#1A1A1A"));
                    textView.setTextColor(Color.WHITE);
                }
            }
        });
    }
    
    private String getEpisodeNumber(Episode episode) {
        // Try to extract number from episodesName
        String name = episode.getEpisodesName();
        if (name != null) {
            Pattern pattern = Pattern.compile("\\d+");
            Matcher matcher = pattern.matcher(name);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        
        // Fallback to episode ID
        String id = episode.getEpisodesId();
        if (id != null && id.matches("\\d+")) {
            return id;
        }
        
        return "0";
    }
    
    private void playEpisode(Episode video, DatabaseHelper db) {
        if (video.getIsPaid().equals("1")) {
            if (PreferenceUtils.isLoggedIn(mContext)) {
                String status = db.getActiveStatusData() != null ? db.getActiveStatusData().getStatus() : "inactive";
                if (status.equalsIgnoreCase("active")) {
                    launchPlayer(video);
                } else {
                    // Subscription not active
                    PreferenceUtils.updateSubscriptionStatus(mContext);
                    PaidDialog dialog = new PaidDialog(mContext);
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                    dialog.show();
                }
            } else {
                // User not logged in
                LoginAlertDialog dialog = new LoginAlertDialog(mContext);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                dialog.show();
            }
        } else {
            // Free content
            if (video.getFileType() != null && video.getFileType().equalsIgnoreCase("embed")) {
                new ToastMsg(mContext).toastIconError(mContext.getResources().getString(R.string.embed_not_supported));
                return;
            }
            launchPlayer(video);
        }
    }
    
    private void launchPlayer(Episode video) {
        PlaybackModel model = new PlaybackModel();
        
        // Use the TV series main ID for watch history tracking
        String seriesId = video.getVideosId();
        if (seriesId == null || seriesId.isEmpty()) {
            seriesId = video.getEpisodesId();
        }
        
        Log.e("FullWidthEpisodePresenter", "Playing episode: seriesId=" + seriesId + ", episodeId=" + video.getEpisodesId());
        
        model.setMovieId(seriesId);
        
        // Handle episode ID
        String episodeId = video.getEpisodesId();
        if (episodeId != null && !episodeId.matches("\\d+")) {
            model.setId((long) episodeId.hashCode());
        } else {
            try {
                model.setId(Long.parseLong(episodeId));
            } catch (NumberFormatException e) {
                Log.e("FullWidthEpisodePresenter", "Failed to parse episode ID: " + episodeId);
                model.setId((long) episodeId.hashCode());
            }
        }
        
        model.setTitle(video.getTvSeriesTitle());
        model.setDescription("Season: " + video.getSeasonName() + "; Episode: " + video.getEpisodesName());
        model.setVideoType("tvseries");
        model.setCategory("tvseries");
        model.setVideoUrl(video.getFileUrl());
        
        Video videoModel = new Video();
        videoModel.setSubtitle(video.getSubtitle());
        model.setVideo(videoModel);
        
        model.setCardImageUrl(video.getCardBackgroundUrl());
        model.setBgImageUrl(video.getImageUrl());
        model.setIsPaid(video.getIsPaid());
        model.setIsTvSeries("1");

        // Add episode navigation data if available
        if (allSeasons != null && seasonIndex >= 0) {
            model.setCurrentSeasonIndex(seasonIndex);
            model.setAllSeasons(allSeasons);
            
            // Set episode index from the episode object
            int episodeIndex = video.getEpisodeIndexInSeason();
            model.setCurrentEpisodeIndex(episodeIndex);
            
            // Get total episodes in current season
            if (allSeasons.get(seasonIndex).getEpisodes() != null) {
                int totalEpisodesInSeason = allSeasons.get(seasonIndex).getEpisodes().size();
                model.setTotalEpisodesInSeason(totalEpisodesInSeason);
            }
        }

        Intent intent = new Intent(mContext, PlayerActivity.class);
        intent.putExtra(VideoPlaybackActivity.EXTRA_VIDEO, model);
        mContext.startActivity(intent);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        // Clean up if needed
    }
}
