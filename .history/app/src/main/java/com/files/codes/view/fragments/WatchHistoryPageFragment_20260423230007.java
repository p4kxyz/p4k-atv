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
                    vc.setIsPaid(item.getIsPaid());

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
                        vc.setStreamUrl(videoUrl);
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

    // Click -> show custom dialog
    private OnItemViewClickedListener getDefaultItemViewClickedListener() {
        return (viewHolder, item, rowViewHolder, row) -> {
            if (!(item instanceof VideoContent)) return;
            VideoContent vc = (VideoContent) item;

            String[] options = {"\u25B6  Xem ti\u1EBFp", "\uD83D\uDDD1  X\u00F3a kh\u1ECFi l\u1ECBch s\u1EED"};
            
            Runnable[] actions = new Runnable[] {
                () -> playVideo(vc),
                () -> deleteHistoryItem(vc)
            };

            android.app.Dialog dialog = buildCustomListDialog(vc.getTitle(), options, -1, actions, null);
            dialog.show();
        };
    }

    private void playVideo(VideoContent vc) {
        long currentPosition = 0;
        String desc = vc.getDescription();
        if (desc != null && desc.startsWith("WATCH_HISTORY:")) {
            try {
                currentPosition = Long.parseLong(desc.substring("WATCH_HISTORY:".length()));
            } catch (NumberFormatException e) {
                currentPosition = 0;
            }
        }
        
        android.content.Intent intent = new android.content.Intent(getActivity(), com.files.codes.view.PlayerActivity.class);
        intent.putExtra("type", vc.getType());
        intent.putExtra("id", vc.getId());
        if (currentPosition > 0) {
            intent.putExtra("position", currentPosition);
        }
        
        // Pass essential video details for PlayerActivity
        intent.putExtra("video_url", vc.getVideoUrl());
        intent.putExtra("title", vc.getTitle());
        intent.putExtra("poster", vc.getPosterUrl());
        intent.putExtra("thumbnail", vc.getThumbnailUrl());
        intent.putExtra("from_watch_history", true);
        
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void deleteHistoryItem(VideoContent vc) {
        if (syncManager != null) {
            syncManager.deleteWatchHistoryItem(vc.getId(), new WatchHistorySyncManager.SyncCallback() {
                @Override
                public void onSuccess(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            android.widget.Toast.makeText(getContext(), "Đã xóa khỏi lịch sử", android.widget.Toast.LENGTH_SHORT).show();
                            if (mAdapter != null) {
                                mAdapter.remove(vc);
                                mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
                            }
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            android.widget.Toast.makeText(getContext(), "Lỗi khi xóa: " + error, android.widget.Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        }
    }

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

    private int dp(int dp) {
        if (getActivity() == null) return dp;
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private android.app.Dialog buildCustomListDialog(
            String title, String[] items, int checkedIndex,
            Runnable[] actions, Runnable onCancel) {

        android.app.Dialog dialog = new android.app.Dialog(getActivity());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        if (onCancel != null) {
            dialog.setOnCancelListener(d -> onCancel.run());
        }

        android.widget.LinearLayout root = new android.widget.LinearLayout(getActivity());
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1E1E2E);
        root.setPadding(dp(20), dp(20), dp(20), dp(12));

        android.widget.TextView titleView = new android.widget.TextView(getActivity());
        titleView.setText(title);
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
        titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleView.setPadding(dp(4), 0, dp(4), dp(10));
        root.addView(titleView);

        android.view.View divider = new android.view.View(getActivity());
        divider.setBackgroundColor(0x55FFFFFF);
        android.widget.LinearLayout.LayoutParams divLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divLp.bottomMargin = dp(6);
        root.addView(divider, divLp);

        android.widget.ScrollView sv = new android.widget.ScrollView(getActivity());
        sv.setVerticalScrollBarEnabled(false);
        android.widget.LinearLayout ll = new android.widget.LinearLayout(getActivity());
        ll.setOrientation(android.widget.LinearLayout.VERTICAL);

        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            boolean isSelected = (checkedIndex >= 0 && i == checkedIndex);

            android.widget.TextView tv = new android.widget.TextView(getActivity());
            tv.setText(isSelected ? "✓  " + items[i] : items[i]);
            tv.setTextColor(isSelected ? 0xFF64B5F6 : 0xFFDDDDDD);
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
            tv.setPadding(dp(10), dp(13), dp(10), dp(13));
            tv.setFocusable(true);
            tv.setFocusableInTouchMode(false);
            tv.setClickable(true);
            tv.setBackground(null);

            tv.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackgroundColor(0x664FC3F7);
                    ((android.widget.TextView) v).setTextColor(0xFFFFFFFF);
                } else {
                    v.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                    boolean sel = (checkedIndex >= 0 && idx == checkedIndex);
                    ((android.widget.TextView) v).setTextColor(sel ? 0xFF64B5F6 : 0xFFDDDDDD);
                }
            });

            tv.setOnClickListener(v -> {
                dialog.dismiss();
                if (actions != null && idx < actions.length && actions[idx] != null) {
                    actions[idx].run();
                }
            });

            ll.addView(tv);

            android.view.View sep = new android.view.View(getActivity());
            sep.setBackgroundColor(0x22FFFFFF);
            ll.addView(sep, new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1));
        }
        sv.addView(ll);

        android.widget.LinearLayout.LayoutParams svLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        root.addView(sv, svLp);

        dialog.setContentView(root);
        android.view.Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout(dp(480), android.view.WindowManager.LayoutParams.WRAP_CONTENT);
            w.setGravity(android.view.Gravity.CENTER);
            w.setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

}
