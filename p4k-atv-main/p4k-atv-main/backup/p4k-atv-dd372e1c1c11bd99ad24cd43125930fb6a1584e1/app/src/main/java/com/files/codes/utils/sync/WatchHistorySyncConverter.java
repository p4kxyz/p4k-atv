package com.files.codes.utils.sync;

import android.content.Context;
import android.util.Log;

import com.files.codes.model.WatchHistoryItem;
import com.files.codes.model.sync.WatchHistorySyncItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class để convert giữa local WatchHistoryItem và sync WatchHistorySyncItem
 */
public class WatchHistorySyncConverter {
    private static final String TAG = "WatchHistorySyncConverter";
    
    /**
     * Convert từ local WatchHistoryItem sang sync format
     */
    public static WatchHistorySyncItem convertToSyncItem(WatchHistoryItem localItem) {
        WatchHistorySyncItem syncItem = new WatchHistorySyncItem();
        
        syncItem.setVideosId(localItem.getVideosId());
        syncItem.setTitle(localItem.getTitle());
        syncItem.setDescription(localItem.getDescription());
        syncItem.setPosterUrl(localItem.getPosterUrl());
        syncItem.setThumbnailUrl(localItem.getThumbnailUrl());
        syncItem.setPosition(localItem.getWatchedPosition());
        syncItem.setDuration(localItem.getTotalDuration());
        syncItem.setCreatedAt(localItem.getTimestamp());
        syncItem.setIsTvseries(localItem.getIsTvseries());
        syncItem.setIsPaid(localItem.getIsPaid());
        syncItem.setRelease(localItem.getRelease());
        syncItem.setRuntime(localItem.getRuntime());
        syncItem.setVideoQuality(localItem.getVideoQuality());
        syncItem.setMovie(!"1".equals(localItem.getIsTvseries()));
        
        return syncItem;
    }
    
    /**
     * Convert từ sync WatchHistorySyncItem sang local format
     */
    public static WatchHistoryItem convertFromSyncItem(WatchHistorySyncItem syncItem) {
        WatchHistoryItem localItem = new WatchHistoryItem();
        
        localItem.setVideosId(syncItem.getVideosId());
        localItem.setTitle(syncItem.getTitle());
        localItem.setDescription(syncItem.getDescription());
        localItem.setPosterUrl(syncItem.getPosterUrl());
        localItem.setThumbnailUrl(syncItem.getThumbnailUrl());
        localItem.setWatchedPosition(syncItem.getPosition());
        localItem.setTotalDuration(syncItem.getDuration());
        localItem.setTimestamp(syncItem.getCreatedAt());
        localItem.setIsTvseries(syncItem.getIsTvseries());
        localItem.setIsPaid(syncItem.getIsPaid());
        localItem.setRelease(syncItem.getRelease());
        localItem.setRuntime(syncItem.getRuntime());
        localItem.setVideoQuality(syncItem.getVideoQuality());
        localItem.setType(syncItem.isMovie() ? "movie" : "tv");
        
        // Calculate progress
        if (syncItem.getDuration() > 0) {
            int progress = (int) ((syncItem.getPosition() * 100) / syncItem.getDuration());
            localItem.setProgress(progress);
        }
        
        return localItem;
    }
    
    /**
     * Convert list of local items to sync format map
     */
    public static Map<String, WatchHistorySyncItem> convertLocalListToSyncMap(List<WatchHistoryItem> localItems) {
        Map<String, WatchHistorySyncItem> syncMap = new HashMap<>();
        
        for (WatchHistoryItem localItem : localItems) {
            String key = localItem.getVideosId();
            if (key != null && !key.isEmpty()) {
                syncMap.put(key, convertToSyncItem(localItem));
            }
        }
        
        Log.d(TAG, "Converted " + localItems.size() + " local items to " + syncMap.size() + " sync items");
        return syncMap;
    }
    
    /**
     * Merge sync items với local items
     * Quy tắc: item mới hơn (theo timestamp) sẽ được giữ lại
     */
    public static void mergeWithLocalItems(Context context, Map<String, WatchHistorySyncItem> syncItems) {
        // TODO: Implement actual database operations
        // Đây là logic mẫu, cần thay thế bằng Room database operations thực tế
        
        Log.d(TAG, "Merging " + syncItems.size() + " sync items with local database");
        
        for (Map.Entry<String, WatchHistorySyncItem> entry : syncItems.entrySet()) {
            String videoId = entry.getKey();
            WatchHistorySyncItem syncItem = entry.getValue();
            
            // TODO: Check if item exists in local database
            // WatchHistoryItem localItem = watchHistoryDao.getByVideoId(videoId);
            
            // if (localItem == null || syncItem.getCreatedAt() > localItem.getTimestamp()) {
            //     // Insert or update local database
            //     WatchHistoryItem newLocalItem = convertFromSyncItem(syncItem);
            //     watchHistoryDao.insertOrUpdate(newLocalItem);
            //     Log.d(TAG, "Updated local item: " + videoId);
            // }
        }
    }
    
    /**
     * Get local watch history (placeholder implementation)
     * TODO: Replace with actual Room database query
     */
    public static Map<String, WatchHistorySyncItem> getLocalWatchHistoryForSync(Context context) {
        // TODO: Implement actual database query
        // List<WatchHistoryItem> localItems = watchHistoryDao.getAllWatchHistory();
        // return convertLocalListToSyncMap(localItems);
        
        // Placeholder implementation
        return new HashMap<>();
    }
}