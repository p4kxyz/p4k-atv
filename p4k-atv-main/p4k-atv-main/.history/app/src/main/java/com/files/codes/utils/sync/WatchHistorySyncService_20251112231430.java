package com.files.codes.utils.sync;

import com.files.codes.model.sync.SyncLinkResponse;
import com.files.codes.model.sync.WatchHistorySyncItem;
import com.google.gson.JsonObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * API Service interface cho đồng bộ lịch sử xem
 */
public interface WatchHistorySyncService {
    
    /**
     * Tạo/lấy link đồng bộ từ email
     */
    @POST("get-link")
    Call<SyncLinkResponse> getSyncLink(@Body JsonObject request);
    
    /**
     * Tạo link đồng bộ từ userId
     */
    @POST("user-json/{id}/create")
    Call<SyncLinkResponse> createSyncLink(@Path("id") String userId);
    
    /**
     * Ghi dữ liệu lịch sử xem lên server
     */
    @POST("user-json/{id}/write")
    Call<JsonObject> writeWatchHistory(
        @Path("id") String userId,
        @Body Map<String, WatchHistorySyncItem> watchHistoryData
    );
    
    /**
     * Đọc toàn bộ dữ liệu lịch sử xem từ server
     */
    @GET("user-json/{id}/read")
    Call<Map<String, WatchHistorySyncItem>> readWatchHistory(@Path("id") String userId);
    
    /**
     * Xóa một item cụ thể trong lịch sử xem
     */
    @DELETE("user-json/{id}/delete/{itemId}")
    Call<JsonObject> deleteWatchHistoryItem(
        @Path("id") String userId,
        @Path("itemId") String itemId
    );
    
    /**
     * Xóa toàn bộ dữ liệu lịch sử xem
     */
    @DELETE("user-json/{id}/clear")
    Call<JsonObject> clearWatchHistory(@Path("id") String userId);
}