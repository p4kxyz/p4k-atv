package com.files.codes.utils.sync;

import android.content.Context;
import android.util.Log;

/**
 * Utility class để track và sync lịch sử xem KHI THOÁT phim
 * Chỉ sync khi người dùng thoát về page thông tin phim
 */
public class WatchHistoryTracker {
    
    private static final String TAG = "WatchHistoryTracker";
    private static final long MIN_WATCH_TIME = 30 * 1000; // 30 giây minimum để lưu
    
    private Context context;
    private WatchHistorySyncManager syncManager;
    private String currentMovieId;
    private String currentTitle;
    private String currentPosterUrl;
    private long currentPosition;
    private long totalDuration;
    private long watchStartTime;
    private boolean hasSignificantWatchTime = false;
    
    public WatchHistoryTracker(Context context) {
        this.context = context;
        this.syncManager = WatchHistorySyncManager.getInstance(context);
    }
    
    /**
     * Bắt đầu track một video
     */
    public void startTracking(String movieId, String title, String posterUrl, long duration) {
        this.currentMovieId = movieId;
        this.currentTitle = title;
        this.currentPosterUrl = posterUrl;
        this.totalDuration = duration;
        this.watchStartTime = System.currentTimeMillis();
        this.hasSignificantWatchTime = false;
        
        Log.d(TAG, "Started tracking: " + title);
    }
    
    /**
     * Cập nhật vị trí xem hiện tại
     * GỌI method này trong onUpdatePosition của ExoPlayer
     * KHÔNG sync ngay, chỉ cập nhật position
     */
    public void updatePosition(long position) {
        this.currentPosition = position;
        
        // Kiểm tra xem đã xem đủ lâu chưa
        long currentTime = System.currentTimeMillis();
        if (currentTime - watchStartTime >= MIN_WATCH_TIME) {
            hasSignificantWatchTime = true;
        }
        
        // Log để debug (không sync)
        if (position % 30000 == 0) { // Log mỗi 30s
            Log.d(TAG, "Position updated: " + formatTime(position) + "/" + formatTime(totalDuration));
        }
    }
    
    /**
     * QUAN TRỌNG: Gọi khi THOÁT khỏi video player về page thông tin phim
     * ĐÂY LÀ KHI DUY NHẤT sync dữ liệu lên server
     */
    public void onExitToMovieDetails() {
        if (shouldSaveToHistory()) {
            Log.d(TAG, "User exited to movie details - syncing watch history: " + currentTitle);
            Log.d(TAG, "Position: " + formatTime(currentPosition) + "/" + formatTime(totalDuration) + 
                      " (" + getWatchPercentage() + "%)");
            
            // Sync ngay khi thoát về movie details
            syncToServer();
        } else {
            Log.d(TAG, "Not saving - insufficient watch time for: " + currentTitle);
        }
        
        // Reset sau khi xử lý
        resetTracking();
    }
    
    /**
     * Đánh dấu phim đã xem xong (100%) - khi xem đến cuối
     */
    public void markAsCompleted() {
        if (currentMovieId != null) {
            Log.d(TAG, "Movie completed: " + currentTitle);
            this.currentPosition = totalDuration; // Set position = duration
            this.hasSignificantWatchTime = true;
            
            // Sync ngay khi hoàn thành
            syncToServer();
        }
    }
    
    /**
     * Sync dữ liệu lên server (chỉ gọi khi thoát hoặc hoàn thành)
     */
    private void syncToServer() {
        if (currentMovieId == null || !syncManager.canAutoSync()) {
            Log.w(TAG, "Cannot sync - missing data or not logged in");
            return;
        }
        
        Log.d(TAG, "Syncing to server: " + currentTitle + " at position " + formatTime(currentPosition));
        
        syncManager.autoAddWatchHistory(
            currentMovieId,
            currentTitle, 
            currentPosterUrl,
            currentPosition,
            totalDuration
        );
    }
    
    /**
     * Kiểm tra xem có nên lưu vào history không
     */
    private boolean shouldSaveToHistory() {
        return currentMovieId != null && 
               hasSignificantWatchTime && 
               currentPosition > 0 &&
               syncManager.canAutoSync();
    }
    
    /**
     * Reset tracking data
     */
    private void resetTracking() {
        currentMovieId = null;
        currentTitle = null;
        currentPosterUrl = null;
        currentPosition = 0;
        totalDuration = 0;
        watchStartTime = 0;
        hasSignificantWatchTime = false;
    }
    
    /**
     * Dừng tracking (cleanup khi destroy activity)
     */
    public void stopTracking() {
        Log.d(TAG, "Stopped tracking: " + (currentTitle != null ? currentTitle : "unknown"));
        resetTracking();
    }
    
    /**
     * Utility method: Chuyển đổi milliseconds thành format thời gian
     */
    public static String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%d:%02d", minutes, secs);
        }
    }
    
    /**
     * Utility method: Tính phần trăm đã xem
     */
    public int getWatchPercentage() {
        if (totalDuration <= 0) return 0;
        return (int) ((currentPosition * 100) / totalDuration);
    }
    
    /**
     * Kiểm tra xem đã xem đủ để lưu history chưa
     */
    public boolean isWorthSaving() {
        return hasSignificantWatchTime && currentPosition > 0;
    }
    
    /**
     * Get current watch info for debugging
     */
    public String getCurrentWatchInfo() {
        if (currentMovieId == null) return "Not tracking anything";
        
        return String.format("Tracking: %s\nPosition: %s/%s (%d%%)\nWatch time: %s\nWorth saving: %s",
            currentTitle,
            formatTime(currentPosition),
            formatTime(totalDuration), 
            getWatchPercentage(),
            formatTime(System.currentTimeMillis() - watchStartTime),
            isWorthSaving());
    }
}