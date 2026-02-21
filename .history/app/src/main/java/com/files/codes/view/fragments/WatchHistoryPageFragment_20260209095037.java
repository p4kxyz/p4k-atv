package com.files.codes.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;

import com.files.codes.model.VideoContent;
import com.files.codes.model.sync.WatchHistorySyncItem;
import com.files.codes.utils.BackgroundHelper;
import com.files.codes.utils.PreferenceUtils;
import com.files.codes.utils.sync.WatchHistorySyncManager;
import com.files.codes.view.PlayerActivity;
import com.files.codes.view.HeroStyleVideoDetailsActivity;
import com.files.codes.view.fragments.testFolder.GridFragment;
import com.files.codes.view.fragments.testFolder.HomeNewActivity;
import com.files.codes.view.presenter.VerticalCardPresenter;

import java.util.ArrayList;
import java.util.List;

public class WatchHistoryPageFragment extends GridFragment {
    public static final String WATCH_HISTORY = "watch_history";
    private static final String TAG = "WatchHistoryPage";
    private static final int NUM_COLUMNS = 5;
    private BackgroundHelper bgHelper;
    private ArrayObjectAdapter mAdapter;
    private HomeNewActivity activity;
    private WatchHistorySyncManager syncManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (HomeNewActivity) getActivity();
        syncManager = new WatchHistorySyncManager(activity);

        setOnItemViewClickedListener(getDefaultItemViewClickedListener());
        setOnItemViewSelectedListener(getDefaultItemSelectedListener());

        setupFragment();
    }

    private void setupFragment() {
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);
        mAdapter = new ArrayObjectAdapter(new VerticalCardPresenter(WATCH_HISTORY));
        setAdapter(mAdapter);
        loadWatchHistory();
    }

    @Override
    public void onResume() {
        super.onResume();
        activity = (HomeNewActivity) getActivity();
        if (activity != null) {
            activity.setOrbsVisibility(false);
        }
        // Reload data when returning (e.g. after playing a video)
        if (syncManager != null && mAdapter != null) {
            loadWatchHistory();
        }
    }

    private void loadWatchHistory() {
        if (!PreferenceUtils.isLoggedIn(getContext())) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để xem lịch sử", Toast.LENGTH_SHORT).show();
            return;
        }

        if (syncManager == null) return;

        if (!syncManager.canAutoSync()) {
            Toast.makeText(getContext(), "Email không hợp lệ để sync lịch sử", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sync from server first, then load
        if (syncManager.getSyncUserId() == null) {
            String email = syncManager.getCurrentUserEmail();
            syncManager.createSyncLink(email, new WatchHistorySyncManager.SyncCallback() {
                @Override
                public void onSuccess(String message) {
                    syncAndDisplay();
                }

                @Override
                public void onError(String error) {
                    // Fallback to local
                    displayLocalHistory();
                }
            });
        } else {
            syncAndDisplay();
        }
    }

    private void syncAndDisplay() {
        syncManager.syncWatchHistoryFromServer(new WatchHistorySyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                displayLocalHistory();
            }

            @Override
            public void onError(String error) {
                displayLocalHistory();
            }
        });
    }

    private void displayLocalHistory() {
        new Thread(() -> {
            try {
                List<WatchHistorySyncItem.WatchHistoryItem> historyItems = syncManager.getWatchHistoryForDisplay();

                if (historyItems == null || historyItems.isEmpty()) {
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            mAdapter.clear();
                            Toast.makeText(getContext(), "Chưa có lịch sử xem", Toast.LENGTH_SHORT).show();
                        });
                    }
                    return;
                }

                List<VideoContent> videoContents = new ArrayList<>();
                for (WatchHistorySyncItem.WatchHistoryItem item : historyItems) {
                    VideoContent vc = new VideoContent();
                    vc.setId(item.getVideoId());
                    vc.setTitle(item.getTitle());

                    String thumbnailUrl = item.getThumbnailUrl();
                    String posterUrl = item.getPosterUrl();

                    // PosterUrl for card display
                    if (posterUrl != null && !posterUrl.isEmpty()) {
                        vc.setPosterUrl(posterUrl);
                    } else if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                        vc.setPosterUrl(thumbnailUrl);
                    } else {
                        vc.setPosterUrl("");
                    }

                    // ThumbnailUrl
                    if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                        vc.setThumbnailUrl(thumbnailUrl);
                    } else if (posterUrl != null && !posterUrl.isEmpty()) {
                        vc.setThumbnailUrl(posterUrl);
                    } else {
                        vc.setThumbnailUrl("");
                    }

                    // Type
                    String isTvSeries = item.getIsTvSeries();
                    if (isTvSeries != null && isTvSeries.equals("1")) {
                        vc.setType("tvseries");
                        vc.setIsTvseries("1");
                    } else {
                        vc.setType("movie");
                        vc.setIsTvseries("0");
                    }

                    // Video URL and position stored in description
                    String videoUrl = item.getVideoUrl();
                    if (videoUrl != null && !videoUrl.isEmpty()) {
                        vc.setVideoUrl(videoUrl);
                    }
                    long currentPos = item.getCurrentPosition();
                    vc.setDescription("WATCH_HISTORY:" + currentPos);

                    videoContents.add(vc);
                }

                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        mAdapter.clear();
                        for (VideoContent vc : videoContents) {
                            mAdapter.add(vc);
                        }
                        Log.d(TAG, "Displayed " + videoContents.size() + " watch history items");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading watch history", e);
            }
        }).start();
    }

    // Click → play with resume
    private OnItemViewClickedListener getDefaultItemViewClickedListener() {
        return (viewHolder, item, rowViewHolder, row) -> {
            if (!(item instanceof VideoContent)) return;
            VideoContent vc = (VideoContent) item;

            // Parse position from description
            long currentPosition = 0;
            String desc = vc.getDescription();
            if (desc != null && desc.startsWith("WATCH_HISTORY:")) {
                try {
                    currentPosition = Long.parseLong(desc.substring("WATCH_HISTORY:".length()));
                } catch (NumberFormatException e) {
                    currentPosition = 0;
                }
            }

            String contentType = "movie";
            if (vc.getIsTvseries() != null && vc.getIsTvseries().equals("1")) {
                contentType = "tvseries";
            } else if (vc.getType() != null) {
                contentType = vc.getType();
            }

            String videoUrl = vc.getVideoUrl();
            if (videoUrl == null || videoUrl.trim().isEmpty()) {
                // No URL → go to details page
                Intent intent = new Intent(getActivity(), HeroStyleVideoDetailsActivity.class);
                intent.putExtra("id", vc.getId());
                intent.putExtra("type", contentType);
                intent.putExtra("thumbImage", vc.getThumbnailUrl() != null ? vc.getThumbnailUrl() : "");
                startActivity(intent);
                return;
            }

            // Go directly to player
            Intent intent = new Intent(getActivity(), PlayerActivity.class);
            intent.putExtra("id", vc.getId());
            intent.putExtra("type", contentType);
            intent.putExtra("title", vc.getTitle());
            intent.putExtra("poster", vc.getPosterUrl());
            intent.putExtra("thumbnail", vc.getThumbnailUrl());
            intent.putExtra("video_url", videoUrl);
            intent.putExtra("position", currentPosition);
            intent.putExtra("from_watch_history", true);
            startActivity(intent);
        };
    }

    // Background blur on selection
    private OnItemViewSelectedListener getDefaultItemSelectedListener() {
        return (itemViewHolder, item, rowViewHolder, row) -> {
            if (item instanceof VideoContent) {
                bgHelper = new BackgroundHelper(getActivity());
                bgHelper.prepareBackgroundManager();
                VideoContent vc = (VideoContent) item;
                String bgUrl = vc.getPosterUrl() != null && !vc.getPosterUrl().isEmpty()
                        ? vc.getPosterUrl() : vc.getThumbnailUrl();
                bgHelper.startBackgroundTimer(bgUrl);
            }
        };
    }
}
