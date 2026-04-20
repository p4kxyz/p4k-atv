package com.files.codes.utils;

import android.content.Context;
import android.util.Log;

import com.files.codes.model.VideoContent;
import com.files.codes.model.phim4k.Phim4kDetailResponse;
import com.files.codes.model.phim4k.Phim4kEpisode;
import com.files.codes.model.phim4k.Phim4kEpisodeServer;
import com.files.codes.model.phim4k.Phim4kMovie;
import com.files.codes.model.phim4k.Phim4kResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Phim4kClient {
    private static final String TAG = "Phim4kClient";
    private static final String BASE_URL = "https://free2.phim4k.lol/api/";
    private static Phim4kClient instance;
    private Phim4kApiService apiService;
    
    public interface Phim4kCallback {
        void onSuccess(List<VideoContent> videos);
        void onError(String error);
    }
    
    public interface Phim4kDetailCallback {
        void onSuccess(VideoContent video);
        void onError(String error);
    }
    
    public interface Phim4kDetailWithMovieCallback {
        void onSuccess(VideoContent video, Phim4kMovie movie);
        void onError(String error);
    }

    private Phim4kClient() {
        // Create logging interceptor
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.NONE);

        // Create OkHttpClient
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging);

        // Create Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(Phim4kApiService.class);
    }

    public static synchronized Phim4kClient getInstance() {
        if (instance == null) {
            instance = new Phim4kClient();
        }
        return instance;
    }

    public void getLatestMovies(int page, Phim4kCallback callback) {
        Log.d(TAG, "Fetching latest movies from phim4k, page: " + page);
        
        Call<Phim4kResponse> call = apiService.getLatestMovies(page);
        call.enqueue(new Callback<Phim4kResponse>() {
            @Override
            public void onResponse(Call<Phim4kResponse> call, Response<Phim4kResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Phim4kResponse phim4kResponse = response.body();
                    if ("success".equals(phim4kResponse.getStatus())) {
                        List<VideoContent> videos = convertPhim4kMoviesToVideoContents(phim4kResponse.getItems());
                        Log.d(TAG, "Successfully fetched " + videos.size() + " movies from phim4k");
                        callback.onSuccess(videos);
                    } else {
                        callback.onError("API returned unsuccessful status");
                    }
                } else {
                    callback.onError("Response failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Phim4kResponse> call, Throwable t) {
                Log.e(TAG, "Failed to fetch movies from phim4k: " + t.getMessage());
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getMovieDetail(String slug, Phim4kDetailCallback callback) {
        Log.d(TAG, "Fetching movie detail from phim4k: " + slug);
        
        Call<Phim4kDetailResponse> call = apiService.getMovieDetail(slug);
        call.enqueue(new Callback<Phim4kDetailResponse>() {
            @Override
            public void onResponse(Call<Phim4kDetailResponse> call, Response<Phim4kDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Phim4kDetailResponse detailResponse = response.body();
                    if ("success".equals(detailResponse.getStatus())) {
                        VideoContent video = convertPhim4kMovieToVideoContent(detailResponse.getMovie());
                        Log.d(TAG, "Successfully fetched movie detail from phim4k: " + video.getTitle());
                        callback.onSuccess(video);
                    } else {
                        callback.onError("API returned unsuccessful status");
                    }
                } else {
                    callback.onError("Response failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Phim4kDetailResponse> call, Throwable t) {
                Log.e(TAG, "Failed to fetch movie detail from phim4k: " + t.getMessage());
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getMovieDetailWithMovie(String slug, Phim4kDetailWithMovieCallback callback) {
        Log.d(TAG, "Fetching movie detail with movie data from phim4k: " + slug);
        
        Call<Phim4kDetailResponse> call = apiService.getMovieDetail(slug);
        call.enqueue(new Callback<Phim4kDetailResponse>() {
            @Override
            public void onResponse(Call<Phim4kDetailResponse> call, Response<Phim4kDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Phim4kDetailResponse detailResponse = response.body();
                    if ("success".equals(detailResponse.getStatus())) {
                        VideoContent video = convertPhim4kMovieToVideoContent(detailResponse.getMovie());
                        Log.d(TAG, "Successfully fetched movie detail with movie data from phim4k: " + video.getTitle());
                        callback.onSuccess(video, detailResponse.getMovie());
                    } else {
                        callback.onError("API returned unsuccessful status");
                    }
                } else {
                    callback.onError("Response failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Phim4kDetailResponse> call, Throwable t) {
                Log.e(TAG, "Failed to fetch movie detail with movie data from phim4k: " + t.getMessage());
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void searchMovies(String keyword, Phim4kCallback callback) {
        Log.d(TAG, "Searching movies on phim4k: " + keyword);
        
        Call<Phim4kResponse> call = apiService.searchMovies(keyword);
        call.enqueue(new Callback<Phim4kResponse>() {
            @Override
            public void onResponse(Call<Phim4kResponse> call, Response<Phim4kResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Phim4kResponse phim4kResponse = response.body();
                    if ("success".equals(phim4kResponse.getStatus())) {
                        List<VideoContent> videos = convertPhim4kMoviesToVideoContents(phim4kResponse.getItems());
                        Log.d(TAG, "Search found " + videos.size() + " movies on phim4k");
                        callback.onSuccess(videos);
                    } else {
                        callback.onError("API returned unsuccessful status");
                    }
                } else {
                    callback.onError("Response failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Phim4kResponse> call, Throwable t) {
                Log.e(TAG, "Failed to search movies on phim4k: " + t.getMessage());
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    private List<VideoContent> convertPhim4kMoviesToVideoContents(List<Phim4kMovie> phim4kMovies) {
        List<VideoContent> videos = new ArrayList<>();
        
        for (Phim4kMovie movie : phim4kMovies) {
            VideoContent video = convertPhim4kMovieToVideoContent(movie);
            videos.add(video);
        }
        
        return videos;
    }

    private VideoContent convertPhim4kMovieToVideoContent(Phim4kMovie movie) {
        VideoContent video = new VideoContent();
        
        // Basic info mapping to VideoContent properties
        video.setId("phim4k_" + movie.getSlug());
        video.setVideosId("phim4k_" + movie.getSlug());
        video.setTitle(movie.getName());
        video.setDescription(movie.getDescription());
        video.setSlug(movie.getSlug());
        video.setIsPaid("0"); // Phim4k is free
        video.setRelease(movie.getCreatedTime() != null && movie.getCreatedTime().length() >= 4 ? movie.getCreatedTime().substring(0, 4) : "2024");
        video.setRuntime(movie.getTime());
        video.setVideoQuality(movie.getQuality());
        video.setThumbnailUrl(movie.getThumbUrl());
        video.setPosterUrl(movie.getPosterUrl());
        
        // TV Series detection
        if (movie.getTotalEpisodes() > 1) {
            video.setIsTvseries("1");
        } else {
            video.setIsTvseries("0");
        }
        
        // Stream info from episodes
        if (movie.getEpisodes() != null && !movie.getEpisodes().isEmpty()) {
            Phim4kEpisodeServer firstServer = movie.getEpisodes().get(0);
            video.setStreamFrom("Phim4k - " + firstServer.getServerName());
            video.setStreamLabel("Phim4k");
            
            // Get first episode URL as default
            if (!firstServer.getItems().isEmpty()) {
                video.setStreamUrl(firstServer.getItems().get(0).getLink());
            }
        } else {
            video.setStreamFrom("Phim4k");
            video.setStreamLabel("Phim4k");
            video.setStreamUrl(""); // Will need to fetch episode details
        }
        
        // Set type for UI
        video.setType(movie.getTotalEpisodes() > 1 ? "tvseries" : "movie");
        
        Log.d(TAG, "Converted phim4k movie: " + movie.getName() + " - Episodes: " + movie.getTotalEpisodes());
        
        return video;
    }
}