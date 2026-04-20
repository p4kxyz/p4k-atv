package com.files.codes.utils;

import android.util.Log;

import com.files.codes.model.VideoContent;
import com.files.codes.model.anime.AnimeEpisodeItem;
import com.files.codes.model.anime.AnimeEpisodeResponse;
import com.files.codes.model.anime.AnimeMedia;
import com.files.codes.model.anime.AnimeMetadataResponse;
import com.files.codes.model.anime.AnimeProviderMapping;
import com.files.codes.model.anime.AnimeSearchResponse;
import com.files.codes.model.anime.AnimeServerResponse;
import com.files.codes.model.anime.AnimeStreamResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AnimeClient {
    private static final String TAG = "AnimeClient";
    private static final String BASE_URL = "https://api.animapper.net/api/v1/";
    public static final String ID_PREFIX = "anime_";

    private static AnimeClient instance;
    private AnimeApiService apiService;

    private AnimeClient() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(AnimeApiService.class);
    }

    public static synchronized AnimeClient getInstance() {
        if (instance == null) {
            instance = new AnimeClient();
        }
        return instance;
    }

    public AnimeApiService getApiService() {
        return apiService;
    }

    // ==================== Callbacks ====================

    public interface AnimeSearchCallback {
        void onSuccess(List<VideoContent> results);
        void onFailure(String error);
    }

    public interface AnimeMetadataCallback {
        void onSuccess(AnimeMedia metadata);
        void onFailure(String error);
    }

    public interface AnimeDetailWithMediaCallback {
        void onSuccess(VideoContent videoContent, AnimeMedia metadata);
        void onFailure(String error);
    }

    public interface AnimeEpisodesCallback {
        void onSuccess(List<AnimeEpisodeItem> episodes, String provider);
        void onFailure(String error);
    }

    public interface AnimeServersCallback {
        void onSuccess(List<String> servers, String provider);
        void onFailure(String error);
    }

    public interface AnimeStreamCallback {
        void onSuccess(AnimeStreamResponse streamResponse);
        void onFailure(String error);
    }

    // ==================== API Methods ====================

    /**
     * Search anime by title
     */
    public void searchAnime(String query, final AnimeSearchCallback callback) {
        apiService.searchAnime(query, "ANIME", 20).enqueue(new Callback<AnimeSearchResponse>() {
            @Override
            public void onResponse(Call<AnimeSearchResponse> call, Response<AnimeSearchResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<AnimeMedia> results = response.body().getResults();
                    List<VideoContent> videoContents = new ArrayList<>();
                    if (results != null) {
                        for (AnimeMedia media : results) {
                            videoContents.add(convertToVideoContent(media));
                        }
                    }
                    callback.onSuccess(videoContents);
                } else {
                    callback.onFailure("Search failed");
                }
            }

            @Override
            public void onFailure(Call<AnimeSearchResponse> call, Throwable t) {
                Log.e(TAG, "Search anime error: " + t.getMessage());
                callback.onFailure(t.getMessage());
            }
        });
    }

    /**
     * Get full metadata for an anime
     */
    public void getMetadata(String animeId, final AnimeMetadataCallback callback) {
        apiService.getMetadata(animeId).enqueue(new Callback<AnimeMetadataResponse>() {
            @Override
            public void onResponse(Call<AnimeMetadataResponse> call, Response<AnimeMetadataResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getResult());
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Metadata fetch failed";
                    callback.onFailure(msg);
                }
            }

            @Override
            public void onFailure(Call<AnimeMetadataResponse> call, Throwable t) {
                Log.e(TAG, "Metadata error: " + t.getMessage());
                callback.onFailure(t.getMessage());
            }
        });
    }

    /**
     * Get metadata and convert to VideoContent for detail page
     */
    public void getMetadataWithVideoContent(String animeId, final AnimeDetailWithMediaCallback callback) {
        apiService.getMetadata(animeId).enqueue(new Callback<AnimeMetadataResponse>() {
            @Override
            public void onResponse(Call<AnimeMetadataResponse> call, Response<AnimeMetadataResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AnimeMedia metadata = response.body().getResult();
                    VideoContent vc = convertToVideoContent(metadata);
                    callback.onSuccess(vc, metadata);
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Metadata fetch failed";
                    callback.onFailure(msg);
                }
            }

            @Override
            public void onFailure(Call<AnimeMetadataResponse> call, Throwable t) {
                Log.e(TAG, "Metadata error: " + t.getMessage());
                callback.onFailure(t.getMessage());
            }
        });
    }

    /**
     * Get available servers for a provider
     */
    public void getServers(String animeId, String provider, final AnimeServersCallback callback) {
        apiService.getServers(animeId, provider).enqueue(new Callback<AnimeServerResponse>() {
            @Override
            public void onResponse(Call<AnimeServerResponse> call, Response<AnimeServerResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> servers = response.body().getServers();
                    callback.onSuccess(servers != null ? servers : new ArrayList<>(), provider);
                } else {
                    callback.onFailure("Failed to get servers");
                }
            }

            @Override
            public void onFailure(Call<AnimeServerResponse> call, Throwable t) {
                Log.e(TAG, "Servers error: " + t.getMessage());
                callback.onFailure(t.getMessage());
            }
        });
    }

    /**
     * Get episodes for a specific provider and server
     */
    public void getEpisodes(String animeId, String provider, String server, final AnimeEpisodesCallback callback) {
        Call<AnimeEpisodeResponse> call;
        if (server != null && !server.isEmpty()) {
            call = apiService.getEpisodes(animeId, provider, server);
        } else {
            call = apiService.getEpisodesAll(animeId, provider);
        }
        call.enqueue(new Callback<AnimeEpisodeResponse>() {
            @Override
            public void onResponse(Call<AnimeEpisodeResponse> call, Response<AnimeEpisodeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AnimeEpisodeItem> episodes = response.body().getEpisodes();
                    callback.onSuccess(episodes != null ? episodes : new ArrayList<>(), provider);
                } else {
                    callback.onFailure("Failed to get episodes");
                }
            }

            @Override
            public void onFailure(Call<AnimeEpisodeResponse> call, Throwable t) {
                Log.e(TAG, "Episodes error: " + t.getMessage());
                callback.onFailure(t.getMessage());
            }
        });
    }

    /**
     * Get stream source URL for an episode
     */
    public void getStreamSource(String episodeData, String provider, String server, final AnimeStreamCallback callback) {
        Call<AnimeStreamResponse> call;
        if (server != null && !server.isEmpty()) {
            call = apiService.getStreamSource(episodeData, provider, server);
        } else {
            call = apiService.getStreamSourceDefault(episodeData, provider);
        }
        call.enqueue(new Callback<AnimeStreamResponse>() {
            @Override
            public void onResponse(Call<AnimeStreamResponse> call, Response<AnimeStreamResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure("Failed to get stream source");
                }
            }

            @Override
            public void onFailure(Call<AnimeStreamResponse> call, Throwable t) {
                Log.e(TAG, "Stream source error: " + t.getMessage());
                callback.onFailure(t.getMessage());
            }
        });
    }

    /**
     * Synchronous stream source resolution (for use on background thread)
     */
    public AnimeStreamResponse getStreamSourceSync(String episodeData, String provider, String server) {
        try {
            Call<AnimeStreamResponse> call;
            if (server != null && !server.isEmpty()) {
                call = apiService.getStreamSource(episodeData, provider, server);
            } else {
                call = apiService.getStreamSourceDefault(episodeData, provider);
            }
            Response<AnimeStreamResponse> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body();
            }
        } catch (Exception e) {
            Log.e(TAG, "Sync stream source error: " + e.getMessage());
        }
        return null;
    }

    // ==================== Conversion ====================

    /**
     * Convert AnimeMedia to VideoContent (for search results and browse)
     */
    public VideoContent convertToVideoContent(AnimeMedia media) {
        VideoContent video = new VideoContent();
        String animeIdStr = String.valueOf(media.getId());

        video.setId(ID_PREFIX + animeIdStr);
        video.setVideosId(ID_PREFIX + animeIdStr);

        // Title - prefer Vietnamese
        if (media.getTitles() != null) {
            video.setTitle(media.getTitles().getBestTitle());
        }

        // Description
        if (media.getDescriptions() != null) {
            String desc = media.getDescriptions().getBestTitle();
            if (desc != null && !desc.isEmpty()) {
                // Strip HTML tags if any
                desc = desc.replaceAll("<[^>]*>", "").replaceAll("&nbsp;", " ").trim();
                video.setDescription(desc);
            }
        }

        video.setIsPaid("0"); // Free source

        // Images
        if (media.getImages() != null) {
            video.setThumbnailUrl(media.getImages().getBestThumbnail());
            video.setPosterUrl(media.getImages().getBestPoster());
        }

        // Year
        video.setRelease(media.getYearString());

        // Duration
        video.setRuntime(media.getDurationString());

        // Quality
        video.setVideoQuality("HD");

        // Type
        if (media.isTvSeries()) {
            video.setIsTvseries("1");
            video.setType("tvseries");
        } else {
            video.setIsTvseries("0");
            video.setType("movie");
        }

        video.setStreamFrom("Anime");
        video.setStreamLabel("Anime");

        return video;
    }

    /**
     * Build anime_resolve URL for lazy stream resolution
     * Format: anime_resolve:{episodeData}|{provider}|{server}
     */
    public static String buildAnimeResolveUrl(String episodeData, String provider, String server) {
        return "anime_resolve:" + episodeData + "|" + provider + "|" + (server != null ? server : "");
    }

    /**
     * Parse anime_resolve URL components
     * Returns [episodeData, provider, server]
     */
    public static String[] parseAnimeResolveUrl(String url) {
        if (url == null || !url.startsWith("anime_resolve:")) return null;
        String data = url.substring("anime_resolve:".length());
        String[] parts = data.split("\\|", 3);
        if (parts.length < 2) return null;
        return new String[]{
            parts[0],  // episodeData
            parts[1],  // provider
            parts.length > 2 ? parts[2] : ""  // server
        };
    }

    /**
     * Check if URL needs anime stream resolution
     */
    public static boolean isAnimeResolveUrl(String url) {
        return url != null && url.startsWith("anime_resolve:");
    }

    /**
     * Get the first available streaming provider name from metadata
     */
    public static String getFirstProvider(AnimeMedia metadata) {
        if (metadata.getStreamingProviders() != null && !metadata.getStreamingProviders().isEmpty()) {
            return metadata.getStreamingProviders().keySet().iterator().next();
        }
        return null;
    }
}
