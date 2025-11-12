package com.files.codes.model;

public class WatchHistoryItem {
    private String videoId;
    private String videosId;
    private String title;
    private String description;
    private String posterUrl;
    private String thumbnailUrl;
    private String type; // "movie" or "tv"
    private String isTvseries;
    private String isPaid;
    private String release;
    private String runtime;
    private String videoQuality;
    private long watchedPosition; // Position in milliseconds
    private long totalDuration; // Total duration in milliseconds
    private long timestamp; // When it was watched
    private int progress; // Progress percentage (0-100)

    public WatchHistoryItem() {
    }

    public WatchHistoryItem(String videoId, String videosId, String title, String posterUrl, 
                           String type, long watchedPosition, long totalDuration) {
        this.videoId = videoId;
        this.videosId = videosId;
        this.title = title;
        this.posterUrl = posterUrl;
        this.type = type;
        this.watchedPosition = watchedPosition;
        this.totalDuration = totalDuration;
        this.timestamp = System.currentTimeMillis();
        this.progress = totalDuration > 0 ? (int) ((watchedPosition * 100) / totalDuration) : 0;
    }

    // Getters and Setters
    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getVideosId() {
        return videosId;
    }

    public void setVideosId(String videosId) {
        this.videosId = videosId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIsTvseries() {
        return isTvseries;
    }

    public void setIsTvseries(String isTvseries) {
        this.isTvseries = isTvseries;
    }

    public String getIsPaid() {
        return isPaid;
    }

    public void setIsPaid(String isPaid) {
        this.isPaid = isPaid;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public String getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(String videoQuality) {
        this.videoQuality = videoQuality;
    }

    public long getWatchedPosition() {
        return watchedPosition;
    }

    public void setWatchedPosition(long watchedPosition) {
        this.watchedPosition = watchedPosition;
        // Update progress when position changes
        this.progress = totalDuration > 0 ? (int) ((watchedPosition * 100) / totalDuration) : 0;
    }

    public long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(long totalDuration) {
        this.totalDuration = totalDuration;
        // Update progress when duration changes
        this.progress = totalDuration > 0 ? (int) ((watchedPosition * 100) / totalDuration) : 0;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    // Helper methods
    public String getFormattedPosition() {
        return formatTime(watchedPosition);
    }

    public String getFormattedDuration() {
        return formatTime(totalDuration);
    }

    public String getProgressText() {
        return getFormattedPosition() + " / " + getFormattedDuration();
    }

    private String formatTime(long timeMs) {
        long totalSeconds = timeMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    public boolean isCompleted() {
        return progress >= 90; // Consider 90% as completed
    }

    public boolean isJustStarted() {
        return progress < 5; // Less than 5% is just started
    }
}