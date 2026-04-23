package com.files.codes.utils.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.files.codes.AppConfig;
import com.files.codes.database.DatabaseHelper;
import com.files.codes.model.api.ApiService;
import com.files.codes.model.movieDetails.MovieSingleDetails;
import com.files.codes.model.subscription.User;
import com.files.codes.model.sync.SyncLinkResponse;
import com.files.codes.model.sync.WatchHistorySyncItem;
import com.files.codes.utils.PreferenceUtils;
import com.files.codes.utils.RetrofitClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Manager class Ä‘á»ƒ quáº£n lÃ½ Ä‘á»“ng bá»™ lá»‹ch sá»­ xem vá»›i metadata Ä‘áº§y Ä‘á»§
 */
public class WatchHistorySyncManager {
    private static final String TAG = "WatchHistorySyncManager";
    private static final String SYNC_BASE_URL = "https://json.phim4k.lol/";
    private static final String PREF_SYNC_USER_ID = "sync_user_id";
    private static final String PREF_SYNC_EMAIL = "sync_email";
    private static final String PREF_LAST_SYNC_TIME = "last_sync_time";
    private static final String PREF_LOCAL_HISTORY = "local_watch_history";
    
    private static WatchHistorySyncManager instance;
    private Context context;
    private WatchHistorySyncService syncService;
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private DatabaseHelper databaseHelper;
    
    
    public void deleteWatchHistoryItem(String videoId, final SyncCallback callback) {
        if (TextUtils.isEmpty(videoId)) {
            if (callback != null) {
                callback.onError("Video ID khong hop le");
            }
            return;
        }

        try {
            Map<String, WatchHistorySyncItem.WatchHistoryItem> localHistory = getLocalWatchHistory();
            boolean removed = false;

            if (localHistory.remove(videoId) != null) {
                removed = true;
            } else {
                List<String> keysToRemove = new ArrayList<>();
                for (Map.Entry<String, WatchHistorySyncItem.WatchHistoryItem> entry : localHistory.entrySet()) {
                    WatchHistorySyncItem.WatchHistoryItem value = entry.getValue();
                    if (value != null && videoId.equals(value.getVideoId())) {
                        keysToRemove.add(entry.getKey());
                    }
                }

                if (!keysToRemove.isEmpty()) {
                    for (String key : keysToRemove) {
                        localHistory.remove(key);
                    }
                    removed = true;
                }
            }

            if (removed) {
                saveLocalWatchHistory(localHistory);
                Log.i(TAG, "Deleted watch history locally for videoId=" + videoId);
            }

            if (!canAutoSync() || getSyncUserId() == null) {
                if (callback != null) {
                    if (removed) {
                        callback.onSuccess("Da xoa khoi lich su (local)");
                    } else {
                        callback.onError("Khong tim thay phim trong lich su");
                    }
                }
                return;
            }

            final boolean removedFinal = removed;
            String userId = getSyncUserId();
            Call<JsonObject> call = syncService.deleteWatchHistoryItem(userId, videoId);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful()) {
                        Log.i(TAG, "Deleted watch history on server for videoId=" + videoId);
                        if (callback != null) {
                            callback.onSuccess("Da xoa khoi lich su");
                        }
                    } else {
                        Log.w(TAG, "Server delete failed (" + response.code() + "), fallback to full sync upload");
                        syncWatchHistoryToServer(new SyncCallback() {
                            @Override
                            public void onSuccess(String message) {
                                if (callback != null) {
                                    if (removedFinal) {
                                        callback.onSuccess("Da xoa khoi lich su");
                                    } else {
                                        callback.onError("Khong tim thay phim trong lich su");
                                    }
                                }
                            }

                            @Override
                            public void onError(String error) {
                                if (callback != null) {
                                    if (removedFinal) {
                                        callback.onSuccess("Da xoa local, nhung dong bo server that bai");
                                    } else {
                                        callback.onError("Khong the xoa tren server: " + error);
                                    }
                                }
                            }
                        });
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.w(TAG, "Server delete request failed, fallback to full sync upload", t);
                    syncWatchHistoryToServer(new SyncCallback() {
                        @Override
                        public void onSuccess(String message) {
                            if (callback != null) {
                                if (removedFinal) {
                                    callback.onSuccess("Da xoa khoi lich su");
                                } else {
                                    callback.onError("Khong tim thay phim trong lich su");
                                }
                            }
                        }

                        @Override
                        public void onError(String error) {
                            if (callback != null) {
                                if (removedFinal) {
                                    callback.onSuccess("Da xoa local, nhung dong bo server that bai");
                                } else {
                                    callback.onError("Khong the xoa tren server: " + error);
                                }
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error deleting watch history item", e);
            if (callback != null) {
                callback.onError("Loi khi xoa lich su: " + e.getMessage());
            }
        }
    }
    public WatchHistorySyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.sharedPreferences = context.getSharedPreferences("watch_history_sync", Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.databaseHelper = new DatabaseHelper(context);
        initRetrofit();
    }
    
    public static synchronized WatchHistorySyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new WatchHistorySyncManager(context);
        }
        return instance;
    }
    
    private void initRetrofit() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.NONE);
        
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SYNC_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        syncService = retrofit.create(WatchHistorySyncService.class);
    }
    
    /**
     * Interface cho callback káº¿t quáº£ Ä‘á»“ng bá»™
     */
    public interface SyncCallback {
        void onSuccess(String message);
        void onError(String error);
    }
    
    /**
     * Láº¥y email ngÆ°á»i dÃ¹ng hiá»‡n táº¡i chá»‰ tá»« user Ä‘Ã£ Ä‘Äƒng nháº­p (strict approach nhÆ° backup)
     */
    public String getCurrentUserEmail() {
        try {
            // CHá»ˆ láº¥y email tá»« user Ä‘Ã£ Ä‘Äƒng nháº­p
            if (PreferenceUtils.isLoggedIn(context)) {
                User user = databaseHelper.getUserData();
                if (user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
                    return user.getEmail();
                }
            }
        } catch (Exception e) {
            // KhÃ´ng cÃ³ fallback - chá»‰ return null náº¿u khÃ´ng cÃ³ user login
        }
        return null;
    }
    
    /**
     * Kiá»ƒm tra xem cÃ³ thá»ƒ Ä‘á»“ng bá»™ tá»± Ä‘á»™ng khÃ´ng (cÃ³ email)
     */
    public boolean canAutoSync() {
        return getCurrentUserEmail() != null;
    }
    
    /**
     * Thiáº¿t láº­p email Ä‘á»“ng bá»™ thá»§ cÃ´ng (cho phÃ©p sync vá»›i email báº¥t ká»³)
     */
    public void setSyncEmail(String email) {
        if (email != null && !email.trim().isEmpty()) {
            String userId = generateUserId(email);
            saveSyncInfo(userId, email);
        }
    }
    
    /**
     * XÃ³a thÃ´ng tin Ä‘á»“ng bá»™
     */
    public void clearSyncInfo() {
        sharedPreferences.edit()
                .remove(PREF_SYNC_USER_ID)
                .remove(PREF_SYNC_EMAIL)
                .apply();
    }
    
    /**
     * Táº¡o link Ä‘á»“ng bá»™ tá»« email hiá»‡n táº¡i (tá»± Ä‘á»™ng)
     */
    public void createAutoSyncLink(SyncCallback callback) {
        String email = getCurrentUserEmail();
        if (email != null) {
            createSyncLink(email, callback);
        } else {
            callback.onError("KhÃ´ng tÃ¬m tháº¥y email ngÆ°á»i dÃ¹ng. Vui lÃ²ng Ä‘Äƒng nháº­p láº¡i.");
        }
    }
    
    /**
     * Táº¡o link Ä‘á»“ng bá»™ tá»« email
     */
    public void createSyncLink(String email, SyncCallback callback) {
        if (TextUtils.isEmpty(email)) {
            callback.onError("Email khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng");
            return;
        }
        
        // Táº¡o User ID tá»« email (hash MD5)
        String userId = generateUserId(email);
        
        Call<SyncLinkResponse> call = syncService.createSyncLink(userId);
        call.enqueue(new Callback<SyncLinkResponse>() {
            @Override
            public void onResponse(Call<SyncLinkResponse> call, Response<SyncLinkResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SyncLinkResponse linkResponse = response.body();
                    
                    // LÆ°u thÃ´ng tin sync
                    saveSyncInfo(userId, email);
                    
                    callback.onSuccess("Link Ä‘á»“ng bá»™ Ä‘Ã£ Ä‘Æ°á»£c táº¡o thÃ nh cÃ´ng");
                } else {
                    String errorMsg = "KhÃ´ng thá»ƒ táº¡o link Ä‘á»“ng bá»™. MÃ£ lá»—i: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            errorMsg += " - " + response.errorBody().string();
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(Call<SyncLinkResponse> call, Throwable t) {
                String errorMsg = "Lá»—i káº¿t ná»‘i";
                if (t.getMessage() != null) {
                    errorMsg += ": " + t.getMessage();
                }
                callback.onError(errorMsg);
            }
        });
    }
    
    /**
     * Äá»“ng bá»™ lá»‹ch sá»­ xem tá»« server vá» local
     */
    public void syncWatchHistoryFromServer(SyncCallback callback) {
        String userId = getInternalSyncUserId();
        if (userId == null) {
            callback.onError("ChÆ°a thiáº¿t láº­p Ä‘á»“ng bá»™. Vui lÃ²ng táº¡o link Ä‘á»“ng bá»™ trÆ°á»›c.");
            return;
        }
        
        Log.e(TAG, "syncWatchHistoryFromServer: Starting sync for user ID: " + userId);
        
        Call<Map<String, WatchHistorySyncItem>> call = syncService.readWatchHistory(userId);
        call.enqueue(new Callback<Map<String, WatchHistorySyncItem>>() {
            @Override
            public void onResponse(Call<Map<String, WatchHistorySyncItem>> call, Response<Map<String, WatchHistorySyncItem>> response) {
                Log.e(TAG, "syncWatchHistoryFromServer: Response code: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, WatchHistorySyncItem> serverHistory = response.body();
                    Log.e(TAG, "syncWatchHistoryFromServer: Received " + serverHistory.size() + " items from server");
                    
                    try {
                        // Chuyá»ƒn Ä‘á»•i server data thÃ nh local format vá»›i Ä‘áº§y Ä‘á»§ metadata
                        Map<String, WatchHistorySyncItem.WatchHistoryItem> localHistoryList = new HashMap<>();
                        
                        for (Map.Entry<String, WatchHistorySyncItem> entry : serverHistory.entrySet()) {
                            WatchHistorySyncItem serverItem = entry.getValue();
                            String videoId = entry.getKey();
                            
                            WatchHistorySyncItem.WatchHistoryItem localItem = new WatchHistorySyncItem.WatchHistoryItem();
                            localItem.setVideoId(videoId);
                            localItem.setTitle(serverItem.getTitle());
                            localItem.setDescription(serverItem.getDescription());
                            localItem.setPosterUrl(serverItem.getPosterUrl());
                            localItem.setThumbnailUrl(serverItem.getThumbnailUrl());
                            localItem.setCurrentPosition(serverItem.getPosition());
                            localItem.setTotalDuration(serverItem.getDuration());
                            localItem.setLastWatched(serverItem.getCreatedAt());
                            localItem.setVideoUrl(serverItem.getCurUrl());
                            localItem.setVideoType("movie");
                            localItem.setIsTvSeries(serverItem.getIsTvseries());
                            localItem.setReleaseDate(serverItem.getRelease());
                            localItem.setImdbRating(serverItem.getImdbRating());
                            localItem.setRuntime(serverItem.getRuntime());
                            localItem.setVideoQuality(serverItem.getVideoQuality());
                            
                            // Extract episode/file info based on movie type
                            if ("1".equals(serverItem.getIsTvseries())) {
                                // PHIM Bá»˜: Extract episode info
                                if (serverItem.getCurEpisode() != null) {
                                    localItem.setEpisodeName(serverItem.getCurEpisode().getEpisodesName());
                                    if (serverItem.getCurEpisode().getEpisodesId() != null) {
                                        try {
                                            localItem.setEpisodeNumber(Integer.parseInt(serverItem.getCurEpisode().getEpisodesId()));
                                        } catch (NumberFormatException e) {
                                            Log.w(TAG, "Could not parse episode number from: " + serverItem.getCurEpisode().getEpisodesId());
                                        }
                                    }
                                }
                                if (serverItem.getCurSeason() != null) {
                                    localItem.setSeasonName(serverItem.getCurSeason().getSeasonsName());
                                    if (serverItem.getCurSeason().getSeasonsId() != null) {
                                        try {
                                            localItem.setSeasonNumber(Integer.parseInt(serverItem.getCurSeason().getSeasonsId()));
                                        } catch (NumberFormatException e) {
                                            Log.w(TAG, "Could not parse season number from: " + serverItem.getCurSeason().getSeasonsId());
                                        }
                                    }
                                }
                            } else {
                                // PHIM Láºº: Extract file name from URL
                                String videoUrl = serverItem.getCurUrl();
                                if (!TextUtils.isEmpty(videoUrl)) {
                                    // Extract file name from URL path
                                    String fileName = videoUrl.substring(videoUrl.lastIndexOf('/') + 1);
                                    if (!TextUtils.isEmpty(fileName)) {
                                        localItem.setFileName(fileName);
                                    }
                                }
                            }
                            
                            // Extract video URL from curUrl or curVideo
                            String videoUrl = serverItem.getCurUrl();
                            if (TextUtils.isEmpty(videoUrl) && serverItem.getCurVideo() != null) {
                                // Extract URL from Video object if available
                                videoUrl = serverItem.getCurVideo().getFileUrl();
                            }
                            if (!TextUtils.isEmpty(videoUrl)) {
                                Log.e(TAG, "syncWatchHistoryFromServer: Found video URL from curUrl: " + videoUrl);
                                localItem.setVideoUrl(videoUrl);
                            }
                            
                            localHistoryList.put(videoId, localItem);
                            Log.e(TAG, "syncWatchHistoryFromServer: Added item: " + serverItem.getTitle() + 
                                      " with thumbnail: " + serverItem.getThumbnailUrl() + 
                                      ", videoUrl: " + videoUrl + 
                                      ", position: " + serverItem.getPosition() + "ms from key: " + videoId);
                        }
                        
                        // LÆ°u vÃ o SharedPreferences
                        saveLocalWatchHistory(localHistoryList);
                        updateLastSyncTime();
                        
                        Log.e(TAG, "syncWatchHistoryFromServer: Total local items: " + localHistoryList.size());
                        Log.e(TAG, "syncWatchHistoryFromServer: Saved to local storage successfully");
                        callback.onSuccess("ÄÃ£ Ä‘á»“ng bá»™ " + localHistoryList.size() + " má»¥c lá»‹ch sá»­ tá»« server");
                    } catch (Exception e) {
                        Log.e(TAG, "syncWatchHistoryFromServer: Error saving to local", e);
                        callback.onError("Lá»—i lÆ°u dá»¯ liá»‡u local: " + e.getMessage());
                    }
                } else {
                    Log.e(TAG, "syncWatchHistoryFromServer: Response not successful or body null");
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        Log.e(TAG, "syncWatchHistoryFromServer: Error body: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "syncWatchHistoryFromServer: Error reading error body", e);
                    }
                    callback.onError("Lá»—i API: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<Map<String, WatchHistorySyncItem>> call, Throwable t) {
                Log.e(TAG, "syncWatchHistoryFromServer: Network failure", t);
                callback.onError("Lá»—i máº¡ng: " + t.getMessage());
            }
        });
    }
    
    /**
     * Äá»“ng bá»™ lá»‹ch sá»­ xem lÃªn server
     */
    public void syncWatchHistoryToServer(SyncCallback callback) {
        String userId = getInternalSyncUserId();
        if (userId == null) {
            callback.onError("ChÆ°a thiáº¿t láº­p Ä‘á»“ng bá»™. Vui lÃ²ng táº¡o link Ä‘á»“ng bá»™ trÆ°á»›c.");
            return;
        }
        
        Log.e(TAG, "syncWatchHistoryToServer: Starting sync for user ID: " + userId);
        
        // Láº¥y dá»¯ liá»‡u lá»‹ch sá»­ xem local
        Map<String, WatchHistorySyncItem.WatchHistoryItem> localHistory = getLocalWatchHistory();
        
        if (localHistory.isEmpty()) {
            Log.e(TAG, "syncWatchHistoryToServer: No local history to sync");
            callback.onSuccess("KhÃ´ng cÃ³ dá»¯ liá»‡u Ä‘á»ƒ Ä‘á»“ng bá»™.");
            return;
        }
        
        Log.e(TAG, "syncWatchHistoryToServer: Preparing " + localHistory.size() + " items for upload");
        
        // Chuyá»ƒn Ä‘á»•i sang format API cáº§n
        Map<String, WatchHistorySyncItem> apiFormat = convertToApiFormat(localHistory);
        Call<JsonObject> call = syncService.writeWatchHistory(userId, apiFormat);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                Log.e(TAG, "syncWatchHistoryToServer: Response code: " + response.code());
                
                if (response.isSuccessful()) {
                    updateLastSyncTime();
                    Log.e(TAG, "syncWatchHistoryToServer: Successfully synced " + localHistory.size() + " items to server");
                    
                    if (response.body() != null) {
                        Log.e(TAG, "syncWatchHistoryToServer: Server response: " + response.body().toString());
                    }
                    
                    callback.onSuccess("ÄÃ£ Ä‘á»“ng bá»™ " + localHistory.size() + " phim lÃªn server.");
                } else {
                    Log.e(TAG, "syncWatchHistoryToServer: Failed to sync - Response code: " + response.code());
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        Log.e(TAG, "syncWatchHistoryToServer: Error body: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "syncWatchHistoryToServer: Error reading error body", e);
                    }
                    callback.onError("KhÃ´ng thá»ƒ Ä‘á»“ng bá»™ lÃªn server. MÃ£ lá»—i: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "syncWatchHistoryToServer: Network failure while syncing to server", t);
                callback.onError("Lá»—i káº¿t ná»‘i: " + t.getMessage());
            }
        });
    }
    
    /**
     * Láº¥y dá»¯ liá»‡u lá»‹ch sá»­ xem tá»« local storage
     */
    public Map<String, WatchHistorySyncItem.WatchHistoryItem> getLocalWatchHistory() {
        Map<String, WatchHistorySyncItem.WatchHistoryItem> result = new HashMap<>();
        
        try {
            String historyJson = sharedPreferences.getString(PREF_LOCAL_HISTORY, "{}");
            Log.d(TAG, "Loading local watch history from SharedPreferences...");
            
            if (!TextUtils.isEmpty(historyJson) && !historyJson.equals("{}") && !historyJson.equals("[]")) {
                // Try to read as Map (new format) first
                try {
                    Type mapType = new TypeToken<Map<String, WatchHistorySyncItem.WatchHistoryItem>>(){}.getType();
                    Map<String, WatchHistorySyncItem.WatchHistoryItem> mapData = gson.fromJson(historyJson, mapType);
                    if (mapData != null && !mapData.isEmpty()) {
                        Log.d(TAG, "Loaded " + mapData.size() + " items from local storage (Map format)");
                        return mapData;
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Failed to parse as Map, trying List format", e);
                }
                
                // Fallback: try to read as List (old format) and convert to Map
                try {
                    Type listType = new TypeToken<List<WatchHistorySyncItem.WatchHistoryItem>>(){}.getType();
                    List<WatchHistorySyncItem.WatchHistoryItem> listData = gson.fromJson(historyJson, listType);
                    if (listData != null) {
                        for (WatchHistorySyncItem.WatchHistoryItem item : listData) {
                            if (item.getVideoId() != null) {
                                result.put(item.getVideoId(), item);
                            }
                        }
                        Log.d(TAG, "Loaded " + result.size() + " items from local storage (List format, converted to Map)");
                        
                        // Convert and save back as Map format for future use
                        if (!result.isEmpty()) {
                            saveLocalWatchHistory(result);
                        }
                        return result;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse local history as List", e);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading local watch history", e);
        }
        
        Log.d(TAG, "No local watch history found, returning empty map");
        return result;
    }
    
    /**
     * Kiá»ƒm tra xem sync Ä‘Ã£ Ä‘Æ°á»£c cáº¥u hÃ¬nh chÆ°a
     */
    public boolean isSyncConfigured() {
        return !TextUtils.isEmpty(getSyncUserId());
    }
    
    /**
     * ThÃªm hoáº·c cáº­p nháº­t má»™t má»¥c lá»‹ch sá»­ xem vá»›i Ä‘áº§y Ä‘á»§ metadata
     */
    public void addWatchHistoryItem(String videoId, String title, String posterUrl, String thumbnailUrl,
                                   long currentPosition, long totalDuration, String videoType) {
        addWatchHistoryItemWithMetadata(videoId, title, "", posterUrl, thumbnailUrl, "", 
                                      currentPosition, totalDuration, videoType, "", "", "", "", "");
    }
    
    /**
     * ThÃªm hoáº·c cáº­p nháº­t má»™t má»¥c lá»‹ch sá»­ xem vá»›i Ä‘áº§y Ä‘á»§ metadata
     */
    public void addWatchHistoryItemWithMetadata(String videoId, String title, String description, 
                                               String posterUrl, String thumbnailUrl, String videoUrl,
                                               long currentPosition, long totalDuration, String videoType,
                                               String releaseDate, String imdbRating, String runtime, 
                                               String videoQuality, String isTvSeries) {
        if (TextUtils.isEmpty(videoId) || TextUtils.isEmpty(title)) {
            Log.w(TAG, "Cannot add watch history item - missing required data: videoId=" + videoId + ", title=" + title);
            return;
        }
        
        Log.e(TAG, "Adding watch history item with metadata: videoId=" + videoId + ", title=" + title + 
                  ", position=" + currentPosition + ", duration=" + totalDuration + ", type=" + videoType);
        
        try {
            // Get existing local history
            Map<String, WatchHistorySyncItem.WatchHistoryItem> localHistory = getLocalWatchHistory();
            
            // Create or update the watch history item
            WatchHistorySyncItem.WatchHistoryItem historyItem = localHistory.get(videoId);
            if (historyItem == null) {
                historyItem = new WatchHistorySyncItem.WatchHistoryItem();
                historyItem.setVideoId(videoId);
            }
            
            // Update all item data including metadata
            historyItem.setTitle(title);
            historyItem.setDescription(description);
            historyItem.setPosterUrl(posterUrl);
            historyItem.setThumbnailUrl(thumbnailUrl);
            historyItem.setVideoUrl(videoUrl);
            historyItem.setVideoType(videoType);
            historyItem.setCurrentPosition(currentPosition);
            historyItem.setTotalDuration(totalDuration);
            historyItem.setLastWatched(System.currentTimeMillis());
            
        // Set additional metadata
        historyItem.setReleaseDate(releaseDate);
        historyItem.setImdbRating(imdbRating);
        historyItem.setRuntime(runtime);
        historyItem.setVideoQuality(videoQuality);
        historyItem.setIsTvSeries(isTvSeries);
        
        // If metadata is empty, try to fetch from video details
        if (TextUtils.isEmpty(description) || TextUtils.isEmpty(releaseDate)) {
            Log.e(TAG, "Missing metadata for videoId " + videoId + ", trying to fetch from API");
            
            // Try to determine video type for API call
            String apiVideoType = "movie"; // Default to movie
            if ("tvseries".equalsIgnoreCase(videoType) || "tv".equalsIgnoreCase(videoType) || 
                "episode".equalsIgnoreCase(videoType)) {
                apiVideoType = "tvseries";
            }
            
            // Only fetch for non-phim4k content as they use different API
            if (!videoId.startsWith("phim4k_")) {
                fetchMovieMetadata(videoId, apiVideoType, historyItem, localHistory);
            }
        }            // Calculate progress percentage
            if (totalDuration > 0) {
                float progressPercentage = (float) ((currentPosition * 100) / totalDuration);
                historyItem.setProgressPercentage(progressPercentage);
            }
            
            // Update local history map
            localHistory.put(videoId, historyItem);
            
            // Save to SharedPreferences
            saveLocalWatchHistory(localHistory);
            
            Log.e(TAG, "Added/updated watch history item with metadata: " + title + " at " + currentPosition + "ms");
            
            // Auto sync to server if configured
            if (isSyncConfigured()) {
                Log.e(TAG, "Sync is configured, calling syncToServer()");
                syncToServer(new SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Log.d(TAG, "Auto sync successful: " + message);
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "Auto sync failed: " + error);
                    }
                });
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding watch history item with metadata", e);
        }
    }
    
    /**
     * Force sync ngay láº­p tá»©c lÃªn server (cho khi thoÃ¡t player)
     */
    public void forceSyncToServer() {
        String userId = getInternalSyncUserId();
        if (TextUtils.isEmpty(userId)) {
            Log.e(TAG, "forceSyncToServer: No user ID found, cannot sync to server");
            return;
        }
        
        Log.e(TAG, "forceSyncToServer: Starting immediate sync to server");
        
        // Gá»i method sync
        syncWatchHistoryToServer(new SyncCallback() {
            @Override
            public void onSuccess(String message) {
                Log.e(TAG, "forceSyncToServer: Immediate sync successful - " + message);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "forceSyncToServer: Immediate sync failed - " + error);
            }
        });
    }
    
    /**
     * LÆ°u lá»‹ch sá»­ xem local vÃ o SharedPreferences
     */
    private void saveLocalWatchHistory(Map<String, WatchHistorySyncItem.WatchHistoryItem> localHistory) {
        try {
            String json = gson.toJson(localHistory);
            sharedPreferences.edit()
                    .putString(PREF_LOCAL_HISTORY, json)
                    .apply();
            Log.d(TAG, "Local watch history saved with " + localHistory.size() + " items");
        } catch (Exception e) {
            Log.e(TAG, "Error saving local watch history", e);
        }
    }
    
    /**
     * Äá»“ng bá»™ lá»‹ch sá»­ xem local lÃªn server
     */
    private void syncToServer(SyncCallback callback) {
        String userId = getInternalSyncUserId();
        if (TextUtils.isEmpty(userId)) {
            Log.e(TAG, "syncToServer: No user ID found, cannot sync to server");
            return;
        }
        
        Log.e(TAG, "syncToServer: Starting auto sync to server for user ID: " + userId);
        
        // Gá»i method public Ä‘á»ƒ Ä‘á»“ng bá»™
        syncWatchHistoryToServer(callback);
    }
    
    /**
     * Chuyá»ƒn Ä‘á»•i WatchHistoryItem sang format API chuáº©n theo máº«u server
     */
    private Map<String, WatchHistorySyncItem> convertToApiFormat(Map<String, WatchHistorySyncItem.WatchHistoryItem> localHistory) {
        Map<String, WatchHistorySyncItem> result = new HashMap<>();
        for (Map.Entry<String, WatchHistorySyncItem.WatchHistoryItem> entry : localHistory.entrySet()) {
            WatchHistorySyncItem.WatchHistoryItem item = entry.getValue();
            WatchHistorySyncItem apiItem = new WatchHistorySyncItem();
            
            // Basic tracking info
            apiItem.setPosition(item.getCurrentPosition());
            apiItem.setDuration(item.getTotalDuration());
            apiItem.setCreatedAt(item.getLastWatched() > 0 ? item.getLastWatched() : System.currentTimeMillis());
            apiItem.setVideosId(item.getVideoId());
            
            // Movie/Series basic metadata
            apiItem.setTitle(item.getTitle() != null ? item.getTitle() : "");
            apiItem.setDescription(item.getDescription() != null ? item.getDescription() : "");
            apiItem.setSlug(generateSlug(item.getTitle()));
            apiItem.setRelease(item.getReleaseDate() != null ? item.getReleaseDate() : "");
            apiItem.setRuntime(item.getRuntime() != null ? item.getRuntime() : null);
            apiItem.setVideoQuality(item.getVideoQuality() != null ? item.getVideoQuality() : "HD");
            apiItem.setIsTvseries(item.getIsTvSeries() != null ? item.getIsTvSeries() : (item.isMovie() ? "0" : "1"));
            apiItem.setIsPaid("0"); // Default free
            apiItem.setEnableDownload("1");
            
            // Generate thumbnail and poster URLs using videos_id
            String videosId = item.getVideoId();
            String thumbnailUrl = item.getThumbnailUrl();
            String posterUrl = item.getPosterUrl();
            
            // If URLs are empty or default, generate from videos_id
            if (videosId != null && !videosId.isEmpty()) {
                if (thumbnailUrl == null || thumbnailUrl.isEmpty()) {
                    thumbnailUrl = "https://api.phim4k.lol/uploads/video_thumb/" + videosId + ".jpg";
                }
                if (posterUrl == null || posterUrl.isEmpty()) {
                    posterUrl = "https://api.phim4k.lol/uploads/poster_image/" + videosId + ".jpg";
                }
            }
            
            apiItem.setThumbnailUrl(thumbnailUrl != null ? thumbnailUrl : "");
            apiItem.setPosterUrl(posterUrl != null ? posterUrl : "");
            apiItem.setImdbRating(item.getImdbRating() != null ? item.getImdbRating() : "7.0");
            
            // Determine if movie or TV series
            boolean isMovie = "0".equals(apiItem.getIsTvseries());
            apiItem.setIsMovie(isMovie);
            
            if (isMovie) {
                // MOVIE FORMAT: Use curVideo structure
                if (item.getVideoUrl() != null && !item.getVideoUrl().isEmpty()) {
                    WatchHistorySyncItem.Video curVideo = new WatchHistorySyncItem.Video();
                    curVideo.setVideoFileId("0");
                    curVideo.setLabel(item.getFileName() != null ? item.getFileName() : "HD Quality");
                    curVideo.setStreamKey(null);
                    curVideo.setFileType("mkv");
                    curVideo.setFileUrl(item.getVideoUrl());
                    curVideo.setSubtitle(new ArrayList<>());
                    apiItem.setCurVideo(curVideo);
                    
                    // Create videos array with same video
                    List<WatchHistorySyncItem.Video> videos = new ArrayList<>();
                    videos.add(curVideo);
                    apiItem.setVideos(videos);
                }
                
                // Movies don't need curUrl, curSeason, curEpisode
                apiItem.setCurUrl(null);
                apiItem.setCurSeason(null);
                apiItem.setCurEpisode(null);
                apiItem.setSeason(null);
                
            } else {
                // TV SERIES FORMAT: Use curUrl, curSeason, curEpisode structure
                apiItem.setCurUrl(item.getVideoUrl() != null ? item.getVideoUrl() : "");
                
                // Create curSeason structure for TV series
                if (item.getSeasonName() != null) {
                    WatchHistorySyncItem.Season curSeason = new WatchHistorySyncItem.Season();
                    curSeason.setSeasonsId(String.valueOf(item.getSeasonNumber() > 0 ? item.getSeasonNumber() : 1));
                    curSeason.setSeasonsName(item.getSeasonName());
                    
                    // Create episodes list for current season
                    List<WatchHistorySyncItem.Episode> episodes = new ArrayList<>();
                    WatchHistorySyncItem.Episode currentEp = new WatchHistorySyncItem.Episode();
                    currentEp.setEpisodesId(String.valueOf(item.getEpisodeNumber() > 0 ? item.getEpisodeNumber() : 1));
                    currentEp.setEpisodesName(item.getEpisodeName() != null ? item.getEpisodeName() : "Episode " + (item.getEpisodeNumber() > 0 ? item.getEpisodeNumber() : 1));
                    currentEp.setStreamKey("");
                    currentEp.setFileType("mkv");
                    currentEp.setImageUrl(item.getThumbnailUrl());
                    currentEp.setFileUrl(item.getVideoUrl() != null ? item.getVideoUrl() : "");
                    currentEp.setSubtitle(new ArrayList<>());
                    episodes.add(currentEp);
                    
                    curSeason.setEpisodes(episodes);
                    apiItem.setCurSeason(curSeason);
                    
                    // Set curEpisode
                    apiItem.setCurEpisode(currentEp);
                    
                    // Create season array
                    List<WatchHistorySyncItem.Season> seasons = new ArrayList<>();
                    seasons.add(curSeason);
                    apiItem.setSeason(seasons);
                }
                
                // TV series don't need curVideo or videos array
                apiItem.setCurVideo(null);
                apiItem.setVideos(null);
            }
            
            // Create default genre, cast, etc.
            // Default genre
            List<WatchHistorySyncItem.Genre> defaultGenre = new ArrayList<>();
            WatchHistorySyncItem.Genre genre = new WatchHistorySyncItem.Genre();
            genre.setGenreId("1");
            genre.setName("Phim Hay");
            genre.setUrl("");
            defaultGenre.add(genre);
            apiItem.setGenre(defaultGenre);
            
            // Default country
            List<WatchHistorySyncItem.Country> countries = new ArrayList<>();
            WatchHistorySyncItem.Country country = new WatchHistorySyncItem.Country();
            country.setCountryId("1");
            country.setName("Quá»‘c Táº¿");
            country.setUrl("");
            countries.add(country);
            apiItem.setCountry(countries);
            
            // Default cast, director, writer arrays
            apiItem.setDirector(new ArrayList<>());
            apiItem.setWriter(new ArrayList<>());
            apiItem.setCast(new ArrayList<>());
            apiItem.setCastAndCrew(new ArrayList<>());
            apiItem.setDownloadLinks(new ArrayList<>());
            
            result.put(entry.getKey(), apiItem);
            Log.e(TAG, "convertToApiFormat: Converted " + (isMovie ? "MOVIE" : "TV SERIES") + " - " + 
                      item.getTitle() + " with position " + item.getCurrentPosition() + "ms");
        }
        return result;
    }
    
    /**
     * Generate slug from title
     */
    private String generateSlug(String title) {
        if (title == null || title.isEmpty()) {
            return "unknown";
        }
        
        return title.toLowerCase()
                    .replaceAll("[Ã Ã¡áº¡áº£Ã£Ã¢áº§áº¥áº­áº©áº«Äƒáº±áº¯áº·áº³áºµ]", "a")
                    .replaceAll("[Ã¨Ã©áº¹áº»áº½Ãªá»áº¿á»‡á»ƒá»…]", "e")
                    .replaceAll("[Ã¬Ã­á»‹á»‰Ä©]", "i")
                    .replaceAll("[Ã²Ã³á»á»ÃµÃ´á»“á»‘á»™á»•á»—Æ¡á»á»›á»£á»Ÿá»¡]", "o")
                    .replaceAll("[Ã¹Ãºá»¥á»§Å©Æ°á»«á»©á»±á»­á»¯]", "u")
                    .replaceAll("[á»³Ã½á»µá»·á»¹]", "y")
                    .replaceAll("[Ä‘]", "d")
                    .replaceAll("[^a-z0-9\\s-]", "")
                    .replaceAll("\\s+", "-")
                    .replaceAll("-+", "-")
                    .replaceAll("^-|-$", "");
    }
    
    // Helper methods
    private String generateUserId(String email) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(email.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(Integer.toHexString((b & 0xFF) | 0x100), 1, 3);
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            return String.valueOf(email.hashCode());
        }
    }
    
    private void saveSyncInfo(String userId, String email) {
        sharedPreferences.edit()
                .putString(PREF_SYNC_USER_ID, userId)
                .putString(PREF_SYNC_EMAIL, email)
                .apply();
    }
    
    private String getInternalSyncUserId() {
        String userId = sharedPreferences.getString(PREF_SYNC_USER_ID, null);
        if (userId == null) {
            String email = getCurrentUserEmail();
            if (email != null) {
                userId = generateUserId(email);
                saveSyncInfo(userId, email);
            }
        }
        return userId;
    }
    
    private String getCurrentUserId() {
        return getInternalSyncUserId();
    }
    
    private void updateLastSyncTime() {
        sharedPreferences.edit()
                .putLong(PREF_LAST_SYNC_TIME, System.currentTimeMillis())
                .apply();
    }
    
    /**
     * Get sync email (public method for compatibility)
     */
    public String getSyncEmail() {
        return sharedPreferences.getString(PREF_SYNC_EMAIL, null);
    }
    
    /**
     * Get sync user ID (public method for compatibility)
     */
    public String getSyncUserId() {
        String userId = sharedPreferences.getString(PREF_SYNC_USER_ID, null);
        if (userId == null) {
            String email = getCurrentUserEmail();
            if (email != null) {
                userId = generateUserId(email);
                saveSyncInfo(userId, email);
            }
        }
        return userId;
    }
    
    /**
     * Get watch history for display (compatibility method)
     */
    public List<WatchHistorySyncItem.WatchHistoryItem> getWatchHistoryForDisplay() {
        Map<String, WatchHistorySyncItem.WatchHistoryItem> historyMap = getLocalWatchHistory();
        List<WatchHistorySyncItem.WatchHistoryItem> result = new ArrayList<>(historyMap.values());
        // Sort by lastWatched descending (newest first)
        java.util.Collections.sort(result, new java.util.Comparator<WatchHistorySyncItem.WatchHistoryItem>() {
            @Override
            public int compare(WatchHistorySyncItem.WatchHistoryItem a, WatchHistorySyncItem.WatchHistoryItem b) {
                return Long.compare(b.getLastWatched(), a.getLastWatched());
            }
        });
        return result;
    }
    
    /**
     * Auto add watch history (compatibility method)
     */
    public void autoAddWatchHistory(String videoId, String title, String posterUrl, long position, long duration) {
        addWatchHistoryItem(videoId, title, posterUrl, "", position, duration, "movie");
    }
    
    /**
     * Auto sync on app start (compatibility method)
     */
    public void autoSyncOnAppStart() {
        if (canAutoSync()) {
            Log.d(TAG, "Auto syncing on app start");
            syncWatchHistoryFromServer(new SyncCallback() {
                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, "Auto sync on start successful: " + message);
                }
                
                @Override
                public void onError(String error) {
                    Log.w(TAG, "Auto sync on start failed: " + error);
                }
            });
        }
    }
    
    /**
     * Clear server watch history (placeholder - not implemented yet)
     */
    public void clearServerWatchHistory(SyncCallback callback) {
        callback.onError("Clear server history not implemented yet");
    }
    
    /**
     * Clear local watch history
     */
    public void clearLocalWatchHistory(SyncCallback callback) {
        try {
            sharedPreferences.edit().remove(PREF_LOCAL_HISTORY).apply();
            callback.onSuccess("Local watch history cleared");
        } catch (Exception e) {
            callback.onError("Failed to clear local history: " + e.getMessage());
        }
    }
    
    /**
     * Fetch metadata from API and update watch history item
     */
    private void fetchMovieMetadata(String videoId, String videoType, 
                                   WatchHistorySyncItem.WatchHistoryItem historyItem,
                                   Map<String, WatchHistorySyncItem.WatchHistoryItem> localHistory) {
        try {
            Retrofit retrofit = RetrofitClient.getRetrofitInstance();
            ApiService apiService = retrofit.create(ApiService.class);
            
            Call<MovieSingleDetails> call = apiService.getSingleDetail(AppConfig.API_KEY, videoType, videoId);
            call.enqueue(new Callback<MovieSingleDetails>() {
                @Override
                public void onResponse(Call<MovieSingleDetails> call, Response<MovieSingleDetails> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        MovieSingleDetails details = response.body();
                        
                        Log.e(TAG, "Successfully fetched metadata for videoId: " + videoId);
                        Log.e(TAG, "Title: " + details.getTitle());
                        Log.e(TAG, "Description: " + details.getDescription());
                        Log.e(TAG, "Release: " + details.getRelease());
                        Log.e(TAG, "Runtime: " + details.getRuntime());
                        Log.e(TAG, "VideoQuality: " + details.getVideoQuality());
                        
                        // Update metadata if they were empty
                        if (TextUtils.isEmpty(historyItem.getDescription()) && !TextUtils.isEmpty(details.getDescription())) {
                            historyItem.setDescription(details.getDescription());
                        }
                        
                        if (TextUtils.isEmpty(historyItem.getReleaseDate()) && !TextUtils.isEmpty(details.getRelease())) {
                            historyItem.setReleaseDate(details.getRelease());
                        }
                        
                        if (TextUtils.isEmpty(historyItem.getRuntime()) && !TextUtils.isEmpty(details.getRuntime())) {
                            historyItem.setRuntime(details.getRuntime());
                        }
                        
                        if (TextUtils.isEmpty(historyItem.getVideoQuality()) && !TextUtils.isEmpty(details.getVideoQuality())) {
                            historyItem.setVideoQuality(details.getVideoQuality());
                        }
                        
                        if (TextUtils.isEmpty(historyItem.getVideoUrl()) && details.getVideos() != null && !details.getVideos().isEmpty()) {
                            // Set the first available video URL
                            historyItem.setVideoUrl(details.getVideos().get(0).getFileUrl());
                        }
                        
                        // Update the local history with the enhanced metadata
                        localHistory.put(videoId, historyItem);
                        saveLocalWatchHistory(localHistory);
                        
                        Log.e(TAG, "Successfully updated metadata for videoId: " + videoId);
                        
                    } else {
                        Log.w(TAG, "Failed to fetch metadata for videoId: " + videoId + 
                               ", Response code: " + response.code());
                    }
                }
                
                @Override
                public void onFailure(Call<MovieSingleDetails> call, Throwable t) {
                    Log.e(TAG, "Error fetching metadata for videoId: " + videoId, t);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Exception while fetching metadata for videoId: " + videoId, e);
        }
    }
    
    /**
     * XÃ³a toÃ n bá»™ lá»‹ch sá»­ xem (local + server)
     */
    public void clearAllWatchHistory(SyncCallback callback) {
        Log.i(TAG, "Clearing all watch history (local + server)");
        
        // 1. Clear local database first
        clearLocalWatchHistory();
        
        // 2. Clear server data if sync is available
        if (canAutoSync() && getSyncUserId() != null) {
            String userId = getSyncUserId();
            Call<JsonObject> call = syncService.clearWatchHistory(userId);
            
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful()) {
                        Log.i(TAG, "Successfully cleared server watch history");
                        if (callback != null) {
                            callback.onSuccess("ÄÃ£ xÃ³a toÃ n bá»™ lá»‹ch sá»­ xem thÃ nh cÃ´ng");
                        }
                    } else {
                        Log.e(TAG, "Failed to clear server watch history: " + response.code());
                        if (callback != null) {
                            callback.onError("XÃ³a lá»‹ch sá»­ trÃªn server tháº¥t báº¡i. ÄÃ£ xÃ³a lá»‹ch sá»­ local.");
                        }
                    }
                }
                
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e(TAG, "Error clearing server watch history", t);
                    if (callback != null) {
                        callback.onError("Lá»—i khi xÃ³a lá»‹ch sá»­ trÃªn server. ÄÃ£ xÃ³a lá»‹ch sá»­ local.");
                    }
                }
            });
        } else {
            Log.i(TAG, "Sync not available, only cleared local watch history");
            if (callback != null) {
                callback.onSuccess("ÄÃ£ xÃ³a lá»‹ch sá»­ local (khÃ´ng cÃ³ sync)");
            }
        }
    }
    

    
    /**
     * Retrieve full watch history data for a specific video ID
     */
    public Map<String, Object> getFullWatchHistoryItem(String videoId) {
        try {
            Map<String, WatchHistorySyncItem.WatchHistoryItem> localHistory = getLocalWatchHistory();
            if (localHistory.containsKey(videoId)) {
                WatchHistorySyncItem.WatchHistoryItem item = localHistory.get(videoId);
                
                // Convert to Map structure similar to mau.json
                Map<String, Object> fullData = new HashMap<>();
                fullData.put("position", item.getCurrentPosition());
                fullData.put("duration", item.getTotalDuration());
                fullData.put("curUrl", item.getVideoUrl());
                fullData.put("createdAt", item.getCreatedAt());
                fullData.put("videos_id", item.getVideoId());
                fullData.put("title", item.getTitle());
                fullData.put("description", item.getDescription());
                fullData.put("slug", item.getSlug());
                fullData.put("release", item.getReleaseDate());
                fullData.put("runtime", item.getRuntime());
                fullData.put("video_quality", item.getVideoQuality());
                fullData.put("is_tvseries", item.getIsTvSeries());
                fullData.put("is_paid", item.getIsPaid());
                fullData.put("enable_download", item.getEnableDownload());
                fullData.put("thumbnail_url", item.getThumbnailUrl());
                fullData.put("poster_url", item.getPosterUrl());
                fullData.put("imdb_rating", item.getImdbRating());
                fullData.put("isMovie", item.getIsMovie());
                
                // Parse JSON strings back to objects
                if (item.getGenre() != null) {
                    try {
                        fullData.put("genre", gson.fromJson(item.getGenre(), List.class));
                    } catch (Exception e) {
                        Log.w(TAG, "Error parsing genre JSON", e);
                    }
                }
                if (item.getCountry() != null) {
                    try {
                        fullData.put("country", gson.fromJson(item.getCountry(), List.class));
                    } catch (Exception e) {
                        Log.w(TAG, "Error parsing country JSON", e);
                    }
                }
                if (item.getCurSeason() != null) {
                    try {
                        fullData.put("curSeason", gson.fromJson(item.getCurSeason(), Map.class));
                    } catch (Exception e) {
                        Log.w(TAG, "Error parsing curSeason JSON", e);
                    }
                }
                if (item.getCurEpisode() != null) {
                    try {
                        fullData.put("curEpisode", gson.fromJson(item.getCurEpisode(), Map.class));
                    } catch (Exception e) {
                        Log.w(TAG, "Error parsing curEpisode JSON", e);
                    }
                }
                if (item.getAllSeasons() != null) {
                    try {
                        fullData.put("season", gson.fromJson(item.getAllSeasons(), List.class));
                    } catch (Exception e) {
                        Log.w(TAG, "Error parsing allSeasons JSON", e);
                    }
                }
                
                return fullData;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting full watch history item", e);
        }
        return null;
    }

    /**
     * Save full watch history with complete movie details (like mau.json structure)
     * Should be called from HeroStyleVideoDetailsActivity with complete movie data
     */
    public void saveFullWatchHistory(String videoId, String title, String description, String slug,
                                   String releaseDate, String runtime, String videoQuality,
                                   String isTvSeries, String isPaid, String enableDownload,
                                   String thumbnailUrl, String posterUrl, String imdbRating,
                                   List<Map<String, String>> genre, List<Map<String, String>> country,
                                   List<Map<String, String>> director, List<Map<String, String>> writer,
                                   List<Map<String, String>> cast, Map<String, Object> curSeason,
                                   Map<String, Object> curEpisode, List<Map<String, Object>> allSeasons,
                                   long position, long duration, String curUrl, boolean isMovie) {
        
        if (!canAutoSync()) {
            Log.w(TAG, "Cannot save full watch history - not logged in or sync not available");
            return;
        }
        
        try {
            Map<String, WatchHistorySyncItem.WatchHistoryItem> localHistory = getLocalWatchHistory();
            
            WatchHistorySyncItem.WatchHistoryItem historyItem = localHistory.get(videoId);
            if (historyItem == null) {
                historyItem = new WatchHistorySyncItem.WatchHistoryItem();
                historyItem.setVideoId(videoId);
            }
            
            // Update with full movie details
            historyItem.setTitle(title);
            historyItem.setDescription(description);
            historyItem.setSlug(slug);
            historyItem.setReleaseDate(releaseDate);
            historyItem.setRuntime(runtime);
            historyItem.setVideoQuality(videoQuality);
            historyItem.setIsTvSeries(isTvSeries);
            historyItem.setIsPaid(isPaid);
            historyItem.setEnableDownload(enableDownload);
            historyItem.setThumbnailUrl(thumbnailUrl);
            historyItem.setPosterUrl(posterUrl);
            historyItem.setImdbRating(imdbRating);
            historyItem.setVideoType(isMovie ? "movie" : "tvseries");
            historyItem.setVideoUrl(curUrl);
            historyItem.setCurrentPosition(position);
            historyItem.setTotalDuration(duration);
            historyItem.setLastWatched(System.currentTimeMillis());
            
            // Convert lists to JSON strings for storage
            Gson gson = new Gson();
            historyItem.setGenre(gson.toJson(genre));
            historyItem.setCountry(gson.toJson(country));
            historyItem.setDirector(gson.toJson(director));
            historyItem.setWriter(gson.toJson(writer));
            historyItem.setCast(gson.toJson(cast));
            
            // Store season/episode data for TV series
            if (!isMovie && curSeason != null) {
                historyItem.setCurSeason(gson.toJson(curSeason));
                if (curEpisode != null) {
                    historyItem.setCurEpisode(gson.toJson(curEpisode));
                }
                if (allSeasons != null) {
                    historyItem.setAllSeasons(gson.toJson(allSeasons));
                }
            }
            
            // Save to local storage
            localHistory.put(videoId, historyItem);
            saveLocalWatchHistory(localHistory);
            
            Log.e(TAG, "ðŸŽ¬ Saved full watch history LOCALLY: " + title + " at position " + position + "ms");
            
            // Sync to server
            syncWatchHistoryToServer(new SyncCallback() {
                @Override
                public void onSuccess(String message) {
                    Log.e(TAG, "âœ… Full watch history synced to SERVER successfully: " + title);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "âŒ Failed to sync full watch history to SERVER: " + error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "âŒ Error saving full watch history", e);
        }
    }

    /**
     * XÃ³a toÃ n bá»™ lá»‹ch sá»­ xem trong SharedPreferences local
     */
    private void clearLocalWatchHistory() {
        try {
            // XÃ³a local watch history tá»« SharedPreferences
            sharedPreferences.edit().remove(PREF_LOCAL_HISTORY).apply();
            
            Log.i(TAG, "Cleared all local watch history from SharedPreferences");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing local watch history", e);
        }
    }
}
