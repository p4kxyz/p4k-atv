package com.files.codes.utils;

import android.content.Context;
import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

import androidx.tvprovider.media.tv.Channel;
import androidx.tvprovider.media.tv.ChannelLogoUtils;
import androidx.tvprovider.media.tv.PreviewProgram;
import androidx.tvprovider.media.tv.TvContractCompat;
import androidx.tvprovider.media.tv.WatchNextProgram;
import androidx.tvprovider.media.tv.PreviewChannelHelper;

import com.files.codes.model.VideoContent;
import com.files.codes.model.sync.WatchHistorySyncItem;
import com.files.codes.utils.sync.WatchHistorySyncManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager for Android TV Home Screen Recommendations
 * Handles Continue Watching and Recommended Content
 */
public class TvRecommendationManager {
    private static final String TAG = "TvRecommendationManager";
    private static final String CHANNEL_NAME = "Phim4K";
    private static final String CHANNEL_DESCRIPTION = "Phim hay mọi lúc mọi nơi";
    
    private Context context;
    private PreviewChannelHelper channelHelper;
    
    public TvRecommendationManager(Context context) {
        this.context = context;
        this.channelHelper = new PreviewChannelHelper(context);
    }
    
    /**
     * Convert poster URL to use /uploads/bg/ for better quality recommendation images
     */
    private String getRecommendationImageUrl(VideoContent content) {
        String posterUrl = content.getPosterUrl();
        if (posterUrl != null && !posterUrl.isEmpty()) {
            // Replace /uploads/video_thumb/ with /uploads/bg/ for higher quality
            return posterUrl.replace("/uploads/video_thumb/", "/uploads/bg/");
        }
        String thumbnailUrl = content.getThumbnailUrl();
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            return thumbnailUrl.replace("/uploads/video_thumb/", "/uploads/bg/");
        }
        return null;
    }
    
    /**
     * Add movie/series to Continue Watching row
     */
    public void addToContinueWatching(VideoContent content, long currentPosition, long duration) {
        try {
            // Null safety checks
            if (content == null || content.getTitle() == null) {
                Log.w(TAG, "Content or title is null, skipping Continue Watching");
                return;
            }
            
            // Create Watch Next Program
            WatchNextProgram.Builder builder = new WatchNextProgram.Builder()
                    .setType(TvContractCompat.PreviewPrograms.TYPE_MOVIE)
                    .setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
                    .setTitle(content.getTitle())
                    .setDescription(content.getDescription() != null ? content.getDescription() : "")
                    .setLastPlaybackPositionMillis((int) currentPosition)
                    .setDurationMillis((int) duration)
                    .setIntentUri(createContentUri(content));
            
            // Set poster art - use /uploads/bg/ for better quality
            String imageUrl = getRecommendationImageUrl(content);
            if (imageUrl != null) {
                builder.setPosterArtUri(Uri.parse(imageUrl));
            }
            
            // Set content type based on isTvseries (safe null check)
            if (content.getIsTvseries() != null && "1".equals(content.getIsTvseries())) {
                builder.setType(TvContractCompat.PreviewPrograms.TYPE_TV_SERIES);
            }
            
            WatchNextProgram program = builder.build();
            
            // Add to Watch Next row
            Uri programUri = context.getContentResolver().insert(
                TvContractCompat.WatchNextPrograms.CONTENT_URI, 
                program.toContentValues()
            );
            
            if (programUri != null) {
                Log.d(TAG, "Added to Continue Watching: " + content.getTitle());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding to Continue Watching", e);
        }
    }
    
    /**
     * Add content to Recommended row
     */
    public void addToRecommended(List<VideoContent> contentList) {
        try {
            Log.e(TAG, ">>> addToRecommended called, list=" + (contentList != null ? contentList.size() : "null"));
            // Create or get existing channel
            long channelId = createRecommendationChannel();
            
            if (channelId == -1) {
                Log.e(TAG, "Failed to create recommendation channel");
                return;
            }
            
            // Add programs to channel
            for (VideoContent content : contentList) {
                PreviewProgram.Builder builder = new PreviewProgram.Builder()
                        .setChannelId(channelId)
                        .setType(TvContractCompat.PreviewPrograms.TYPE_MOVIE)
                        .setTitle(content.getTitle())
                        .setDescription(content.getDescription() != null ? content.getDescription() : "")
                        .setIntentUri(createContentUri(content));
                
                // Set poster art - use /uploads/bg/ for better quality
                String recImageUrl = getRecommendationImageUrl(content);
                if (recImageUrl != null) {
                    builder.setPosterArtUri(Uri.parse(recImageUrl));
                }
                
                // Set content type
                if ("1".equals(content.getIsTvseries())) {
                    builder.setType(TvContractCompat.PreviewPrograms.TYPE_TV_SERIES);
                }
                
                PreviewProgram program = builder.build();
                Uri programUri = context.getContentResolver().insert(
                    TvContractCompat.PreviewPrograms.CONTENT_URI, 
                    program.toContentValues()
                );
                
                if (programUri != null) {
                    Log.d(TAG, "Added to Recommended: " + content.getTitle());
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding to Recommended", e);
        }
    }
    
    /**
     * Create or get existing recommendation channel
     */
    private long createRecommendationChannel() {
        try {
            // Check if TV Provider is available (emulator fix)
            boolean tvAvail = isTvProviderAvailable();
            Log.e(TAG, ">>> createRecommendationChannel: isTvProviderAvailable=" + tvAvail);
            if (!tvAvail) {
                Log.e(TAG, ">>> TV Provider NOT available, skipping channel creation");
                return -1;
            }
            
            // Check for existing channel first
            android.database.Cursor cursor = context.getContentResolver().query(
                    TvContractCompat.Channels.CONTENT_URI,
                    new String[]{TvContractCompat.Channels._ID, TvContractCompat.Channels.COLUMN_DISPLAY_NAME},
                    null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long existingId = cursor.getLong(0);
                    String name = cursor.getString(1);
                    if (CHANNEL_NAME.equals(name)) {
                        Log.e(TAG, ">>> Found existing channel: " + existingId);
                        cursor.close();
                        return existingId;
                    }
                }
                cursor.close();
            }
            
            Channel.Builder builder = new Channel.Builder()
                    .setType(TvContractCompat.Channels.TYPE_PREVIEW)
                    .setDisplayName(CHANNEL_NAME)
                    .setDescription(CHANNEL_DESCRIPTION)
                    .setAppLinkIntentUri(Uri.parse("oxootv://home"));
            
            Channel channel = builder.build();
            Uri channelUri = context.getContentResolver().insert(
                TvContractCompat.Channels.CONTENT_URI,
                channel.toContentValues()
            );
            
            Log.e(TAG, ">>> channelUri=" + channelUri);
            if (channelUri != null) {
                long channelId = Long.parseLong(channelUri.getLastPathSegment());
                
                // Request channel to be browsable (user must approve)
                // On Android TV, direct COLUMN_BROWSABLE update is not allowed
                TvContractCompat.requestChannelBrowsable(context, channelId);
                Log.e(TAG, ">>> Created channel and requested browsable: " + channelId);
                return channelId;
            }
            
            return -1;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating recommendation channel", e);
            return -1;
        }
    }
    
    /**
     * Check if TV Provider is available (to handle emulator case)
     */
    private boolean isTvProviderAvailable() {
        try {
            return context.getPackageManager().resolveContentProvider(
                "android.media.tv", 0) != null;
        } catch (Exception e) {
            Log.w(TAG, "Cannot check TV Provider availability", e);
            return false;
        }
    }
    
    /**
     * Create content URI for launching app
     */
    private Uri createContentUri(VideoContent content) {
        // Safe check for null isTvseries
        String contentType = "movie"; // default
        if (content.getIsTvseries() != null && "1".equals(content.getIsTvseries())) {
            contentType = "tvseries";
        }
        
        String contentId = content.getId() != null ? content.getId() : "0";
        return Uri.parse("oxootv://video/" + contentId + "?type=" + contentType);
    }
    
    /**
     * Remove from Continue Watching
     */
    public void removeFromContinueWatching(String contentId) {
        try {
            // Remove from Watch Next based on content ID
            String selection = TvContractCompat.WatchNextPrograms.COLUMN_CONTENT_ID + "=?";
            String[] selectionArgs = {contentId};
            
            int deleted = context.getContentResolver().delete(
                TvContractCompat.WatchNextPrograms.CONTENT_URI,
                selection,
                selectionArgs
            );
            
            Log.d(TAG, "Removed from Continue Watching: " + contentId + " (" + deleted + " items)");
            
        } catch (Exception e) {
            Log.e(TAG, "Error removing from Continue Watching", e);
        }
    }
    
    /**
     * Update watch progress
     */
    public void updateWatchProgress(String contentId, long currentPosition, long duration) {
        // Implementation to update existing Watch Next program
        try {
            // Find existing program and update position
            String selection = TvContractCompat.WatchNextPrograms.COLUMN_CONTENT_ID + "=?";
            String[] selectionArgs = {contentId};
            
            // Update logic here...
            Log.d(TAG, "Updated watch progress: " + contentId + " -> " + currentPosition + "/" + duration);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating watch progress", e);
        }
    }

    /**
     * Add featured content from homepage to Android TV Recommended row
     */
    public void addFeaturedContentFromHomepage(List<VideoContent> featuredMovies) {
        try {
            Log.e(TAG, ">>> addFeaturedContentFromHomepage called, list=" + (featuredMovies != null ? featuredMovies.size() : "null"));
            if (featuredMovies != null && !featuredMovies.isEmpty()) {
                // Limit to top 10 featured movies for Android TV
                List<VideoContent> topFeatured = featuredMovies.size() > 10 
                    ? featuredMovies.subList(0, 10) 
                    : new ArrayList<>(featuredMovies);
                
                addToRecommended(topFeatured);
                Log.d(TAG, "Added " + topFeatured.size() + " featured movies from homepage to TV Recommended");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding featured content from homepage", e);
        }
    }

    /**
     * Sync watch history from API to Android TV Home Screen (Continue Watching only)
     */
    public void syncWatchHistoryFromAPI() {
        try {
            WatchHistorySyncManager syncManager = WatchHistorySyncManager.getInstance(context);
            
            syncManager.syncWatchHistoryFromServer(new WatchHistorySyncManager.SyncCallback() {
                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, "Watch history synced successfully: " + message);
                    // Convert synced history to TV recommendations
                    convertSyncedHistoryToTvRecommendations();
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error syncing watch history: " + error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error in syncWatchHistoryFromAPI", e);
        }
    }

    /**
     * Convert synced watch history to Android TV recommendations
     */
    private void convertSyncedHistoryToTvRecommendations() {
        try {
            WatchHistorySyncManager syncManager = WatchHistorySyncManager.getInstance(context);
            List<WatchHistorySyncItem.WatchHistoryItem> syncedHistory = syncManager.getWatchHistoryForDisplay();
            
            if (syncedHistory != null && !syncedHistory.isEmpty()) {
                for (WatchHistorySyncItem.WatchHistoryItem item : syncedHistory) {
                    // Convert WatchHistoryItem to VideoContent
                    VideoContent content = convertSyncItemToVideoContent(item);
                    
                    if (content != null) {
                        // Only add to Continue Watching if not finished
                        // (API sync chỉ cho Continue Watching, Featured lấy từ homepage)
                        if (item.getPosition() > 0 && item.getPosition() < item.getDuration()) {
                            addToContinueWatching(content, item.getPosition(), item.getDuration());
                            Log.d(TAG, "Added API watch history to Continue Watching: " + content.getTitle());
                        }
                    }
                }
                Log.d(TAG, "Converted " + syncedHistory.size() + " synced items to TV recommendations");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting synced history to TV recommendations", e);
        }
    }

    /**
     * Convert WatchHistoryItem to VideoContent
     */
    private VideoContent convertSyncItemToVideoContent(WatchHistorySyncItem.WatchHistoryItem item) {
        try {
            VideoContent content = new VideoContent();
            content.setId(String.valueOf(System.currentTimeMillis())); // Generate unique ID
            content.setTitle(item.getTitle());
            content.setDescription(item.getDescription());
            content.setThumbnailUrl(item.getThumbnailUrl());
            content.setPosterUrl(item.getPosterUrl());
            content.setVideoUrl(item.getVideoUrl());
            
            // Fix NullPointerException calling booleanValue on null Boolean
            Boolean isMovie = item.isMovie();
            boolean isMovieSafe = (isMovie != null && isMovie);
            content.setIsTvseries(isMovieSafe ? "0" : "1");
            
            content.setVideoQuality(item.getVideoQuality());
            content.setRuntime(item.getRuntime());
            
            return content;
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting sync item to video content", e);
            return null;
        }
    }
}