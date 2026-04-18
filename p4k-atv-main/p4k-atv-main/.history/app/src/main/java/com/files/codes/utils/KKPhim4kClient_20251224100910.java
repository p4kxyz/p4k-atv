package com.files.codes.utils;

import android.util.Log;

import com.files.codes.model.VideoContent;
import com.files.codes.model.phim4k.KKPhim4kDetailResponse;
import com.files.codes.model.phim4k.Phim4kMovie;
import com.files.codes.model.phim4k.KKPhim4kResponse;
import com.files.codes.model.phim4k.Phim4kEpisodeServer;

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

public class KKPhim4kClient {
    private static final String TAG = "KKPhim4kClient";
    private static final String BASE_URL = "https://kk.phim4k.lol/";
    private static KKPhim4kClient instance;
    private KKPhim4kApiService apiService;
    
    public interface KKPhim4kCallback {
        void onSuccess(List<VideoContent> videos);
        void onError(String error);
    }
    
    public interface KKPhim4kDetailCallback {
        void onSuccess(VideoContent video);
        void onError(String error);
    }
    
    public interface KKPhim4kDetailWithMovieCallback {
        void onSuccess(VideoContent video, Phim4kMovie movie);
        void onError(String error);
    }

    private KKPhim4kClient() {
        // Create logging interceptor
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

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

        apiService = retrofit.create(KKPhim4kApiService.class);
    }

    public static synchronized KKPhim4kClient getInstance() {
        if (instance == null) {
            instance = new KKPhim4kClient();
        }
        return instance;
    }

    public void getLatestMovies(int page, KKPhim4kCallback callback) {
        Log.d(TAG, "Fetching latest movies from KKPhim4k, page: " + page);
        
        Call<KKPhim4kResponse> call = apiService.getLatestMovies(page);
        call.enqueue(new Callback<KKPhim4kResponse>() {
            @Override
            public void onResponse(Call<KKPhim4kResponse> call, Response<KKPhim4kResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    KKPhim4kResponse phim4kResponse = response.body();
                    if (phim4kResponse.getStatus()) {
                        List<VideoContent> videos = convertPhim4kMoviesToVideoContents(phim4kResponse.getItems());
                        Log.d(TAG, "Successfully fetched " + videos.size() + " movies from KKPhim4k");
                        callback.onSuccess(videos);
                    } else {
                        callback.onError("API returned unsuccessful status");
                    }
                } else {
                    callback.onError("Response failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<KKPhim4kResponse> call, Throwable t) {
                Log.e(TAG, "Failed to fetch movies from KKPhim4k: " + t.getMessage());
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getMovieDetail(String slug, KKPhim4kDetailCallback callback) {
        Log.d(TAG, "Fetching movie detail from KKPhim4k: " + slug);
        
        Call<KKPhim4kDetailResponse> call = apiService.getMovieDetail(slug);
        call.enqueue(new Callback<KKPhim4kDetailResponse>() {
            @Override
            public void onResponse(Call<KKPhim4kDetailResponse> call, Response<KKPhim4kDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    KKPhim4kDetailResponse detailResponse = response.body();
                    if (detailResponse.getStatus()) {
                        Phim4kMovie movie = detailResponse.getMovie();
                        if (movie != null && detailResponse.getEpisodes() != null) {
                            movie.setEpisodes(detailResponse.getEpisodes());
                        }
                        VideoContent video = convertPhim4kMovieToVideoContent(movie);
                        Log.d(TAG, "Successfully fetched movie detail from KKPhim4k: " + video.getTitle());
                        callback.onSuccess(video);
                    } else {
                        callback.onError("API returned unsuccessful status");
                    }
                } else {
                    callback.onError("Response failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<KKPhim4kDetailResponse> call, Throwable t) {
                Log.e(TAG, "Failed to fetch movie detail from KKPhim4k: " + t.getMessage());
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getMovieDetailWithMovie(String slug, KKPhim4kDetailWithMovieCallback callback) {
        Log.d(TAG, "Fetching movie detail with movie data from KKPhim4k: " + slug);
        
        Call<KKPhim4kDetailResponse> call = apiService.getMovieDetail(slug);
        call.enqueue(new Callback<KKPhim4kDetailResponse>() {
            @Override
            public void onResponse(Call<KKPhim4kDetailResponse> call, Response<KKPhim4kDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    KKPhim4kDetailResponse detailResponse = response.body();
                    if (detailResponse.getStatus()) {
                        Phim4kMovie movie = detailResponse.getMovie();
                        if (movie != null && detailResponse.getEpisodes() != null) {
                            movie.setEpisodes(detailResponse.getEpisodes());
                        }
                        VideoContent video = convertPhim4kMovieToVideoContent(movie);
                        Log.d(TAG, "Successfully fetched movie detail with movie data from KKPhim4k: " + video.getTitle());
                        callback.onSuccess(video, movie);
                    } else {
                        callback.onError("API returned unsuccessful status");
                    }
                } else {
                    callback.onError("Response failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<KKPhim4kDetailResponse> call, Throwable t) {
                Log.e(TAG, "Failed to fetch movie detail with movie data from KKPhim4k: " + t.getMessage());
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void searchMovies(String keyword, KKPhim4kCallback callback) {
        Log.d(TAG, "Searching movies on KKPhim4k: " + keyword);
        
        Call<KKPhim4kResponse> call = apiService.searchMovies(keyword);
        call.enqueue(new Callback<KKPhim4kResponse>() {
            @Override
            public void onResponse(Call<KKPhim4kResponse> call, Response<KKPhim4kResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    KKPhim4kResponse phim4kResponse = response.body();
                    if (phim4kResponse.getStatus()) {
                        List<VideoContent> videos = convertPhim4kMoviesToVideoContents(phim4kResponse.getItems());
                        Log.d(TAG, "Search found " + videos.size() + " movies on KKPhim4k");
                        callback.onSuccess(videos);
                    } else {
                        callback.onError("API returned unsuccessful status");
                    }
                } else {
                    callback.onError("Response failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<KKPhim4kResponse> call, Throwable t) {
                Log.e(TAG, "Failed to search movies on KKPhim4k: " + t.getMessage());
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    private List<VideoContent> convertPhim4kMoviesToVideoContents(List<Phim4kMovie> phim4kMovies) {
        List<VideoContent> videos = new ArrayList<>();
        
        if (phim4kMovies == null) {
            return videos;
        }

        for (Phim4kMovie movie : phim4kMovies) {
            VideoContent video = convertPhim4kMovieToVideoContent(movie);
            videos.add(video);
        }
        
        return videos;
    }

    private VideoContent convertPhim4kMovieToVideoContent(Phim4kMovie movie) {
        VideoContent video = new VideoContent();
        
        // Basic info mapping to VideoContent properties
        video.setId("kkphim4k_" + movie.getSlug());
        video.setVideosId("kkphim4k_" + movie.getSlug());
        video.setTitle(movie.getName());
        video.setDescription(movie.getDescription());
        video.setSlug(movie.getSlug());
        video.setIsPaid("0"); // KKPhim4k is free
        
        // Handle release year
        if (movie.getYear() > 0) {
            video.setRelease(String.valueOf(movie.getYear()));
        } else {
            video.setRelease(movie.getCreatedTime() != null && movie.getCreatedTime().length() >= 4 ? movie.getCreatedTime().substring(0, 4) : "2024");
        }
        
        video.setRuntime(movie.getTime());
        video.setVideoQuality(movie.getQuality());
        video.setThumbnailUrl(movie.getThumbUrl());
        video.setPosterUrl(movie.getPosterUrl());
        
        // TV Series detection
        boolean isTvSeries = false;
        if (movie.getTmdb() != null && "tv".equalsIgnoreCase(movie.getTmdb().getType())) {
            isTvSeries = true;
        } else if (movie.getTotalEpisodes() > 1) {
            isTvSeries = true;
        }
        
        if (isTvSeries) {
            video.setIsTvseries("1");
            video.setType("tvseries");
        } else {
            video.setIsTvseries("0");
            video.setType("movie");
        }
        
        // Stream info from episodes
        if (movie.getEpisodes() != null && !movie.getEpisodes().isEmpty()) {
            Phim4kEpisodeServer firstServer = movie.getEpisodes().get(0);
            video.setStreamFrom("KKPhim4k - " + firstServer.getServerName());
            video.setStreamLabel("KKPhim4k");
            
            // Get first episode URL as default
            if (!firstServer.getItems().isEmpty()) {
                video.setStreamUrl(firstServer.getItems().get(0).getLink());
            }
        } else {
            video.setStreamFrom("KKPhim4k");
            video.setStreamLabel("KKPhim4k");
            video.setStreamUrl(""); // Will need to fetch episode details
        }
        
        Log.d(TAG, "Converted KKPhim4k movie: " + movie.getName() + " - Type: " + video.getType());
        
        return video;
    }
}
