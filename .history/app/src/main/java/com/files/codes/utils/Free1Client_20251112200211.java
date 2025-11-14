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

public class Free1Client {
    private static final String TAG = "Free1Client";
    private static final String BASE_URL = "https://free1.phim4k.lol/";
    private static Free1Client instance;
    private Free1ApiService apiService;
    
    public interface Free1Callback {
        void onSuccess(List<VideoContent> videos);
        void onError(String error);
    }
    
    public interface Free1DetailCallback {
        void onSuccess(VideoContent video);
        void onError(String error);
    }
    
    public interface Free1DetailWithMovieCallback {
        void onSuccess(VideoContent video, Phim4kMovie movie);
        void onError(String error);
    }

    private Free1Client() {
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

        apiService = retrofit.create(Free1ApiService.class);
    }

    public static synchronized Free1Client getInstance() {
        if (instance == null) {
            instance = new Free1Client();
        }
        return instance;
    }

    public void getLatestMovies(int page, Free1Callback callback) {
        Log.d(TAG, "Fetching latest movies from free1, page: " + page);
        
        Call<Phim4kResponse> call = apiService.getLatestMovies(page);
        call.enqueue(new Callback<Phim4kResponse>() {
            @Override
            public void onResponse(Call<Phim4kResponse> call, Response<Phim4kResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Phim4kResponse phim4kResponse = response.body();
                    if ("success".equals(phim4kResponse.getStatus())) {
                        List<VideoContent> videos = convertPhim4kMoviesToVideoContents(phim4kResponse.getItems());
                        Log.d(TAG, "Successfully fetched " + videos.size() + " movies from free1");
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
                Log.e(TAG, "Failed to fetch movies from free1: " + t.getMessage());
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getMovies(int page, Free1Callback callback) {
        Log.d(TAG, "Fetching movies from free1, page: " + page);
        
        Call<Phim4kResponse> call = apiService.getMovies(page);
        call.enqueue(new Callback<Phim4kResponse>() {
            @Override
            public void onResponse(Call<Phim4kResponse> call, Response<Phim4kResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Phim4kResponse phim4kResponse = response.body();
                    if ("success".equals(phim4kResponse.getStatus())) {
                        List<VideoContent> videos = convertPhim4kMoviesToVideoContents(phim4kResponse.getItems());
                        Log.d(TAG, "Successfully fetched " + videos.size() + " movies from free1");
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
                Log.e(TAG, "Failed to fetch movies from free1: " + t.getMessage());
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getTvSeries(int page, Free1Callback callback) {
        Log.d(TAG, "Fetching TV series from free1, page: " + page);
        
        Call<Phim4kResponse> call = apiService.getTvSeries(page);
        call.enqueue(new Callback<Phim4kResponse>() {
            @Override
            public void onResponse(Call<Phim4kResponse> call, Response<Phim4kResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Phim4kResponse phim4kResponse = response.body();
                    if ("success".equals(phim4kResponse.getStatus())) {
                        List<VideoContent> videos = convertPhim4kMoviesToVideoContents(phim4kResponse.getItems());
                        Log.d(TAG, "Successfully fetched " + videos.size() + " TV series from free1");
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
                Log.e(TAG, "Failed to fetch TV series from free1: " + t.getMessage());
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void searchMovies(String keyword, Free1Callback callback) {
        Log.d(TAG, "Searching movies on free1 with keyword: " + keyword);
        
        Call<Phim4kResponse> call = apiService.searchMovies(keyword);
        call.enqueue(new Callback<Phim4kResponse>() {
            @Override
            public void onResponse(Call<Phim4kResponse> call, Response<Phim4kResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Phim4kResponse phim4kResponse = response.body();
                    if ("success".equals(phim4kResponse.getStatus())) {
                        List<VideoContent> videos = convertPhim4kMoviesToVideoContents(phim4kResponse.getItems());
                        Log.d(TAG, "Successfully searched " + videos.size() + " movies from free1");
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
                Log.e(TAG, "Failed to search movies on free1: " + t.getMessage());
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getMovieDetail(String slug, Free1DetailWithMovieCallback callback) {
        Log.d(TAG, "Fetching movie detail from free1 for slug: " + slug);
        
        Call<Phim4kDetailResponse> call = apiService.getMovieDetail(slug);
        call.enqueue(new Callback<Phim4kDetailResponse>() {
            @Override
            public void onResponse(Call<Phim4kDetailResponse> call, Response<Phim4kDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Phim4kDetailResponse detailResponse = response.body();
                    if ("success".equals(detailResponse.getStatus()) && detailResponse.getMovie() != null) {
                        Phim4kMovie movie = detailResponse.getMovie();
                        VideoContent video = convertPhim4kMovieToVideoContent(movie);
                        Log.d(TAG, "Successfully fetched movie detail from free1: " + movie.getName());
                        callback.onSuccess(video, movie);
                    } else {
                        callback.onError("API returned unsuccessful status or no movie data");
                    }
                } else {
                    callback.onError("Response failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Phim4kDetailResponse> call, Throwable t) {
                Log.e(TAG, "Failed to fetch movie detail from free1: " + t.getMessage());
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getMovieDetailWithMovie(String slug, Free1DetailWithMovieCallback callback) {
        getMovieDetail(slug, callback);
    }

    private List<VideoContent> convertPhim4kMoviesToVideoContents(List<Phim4kMovie> movies) {
        List<VideoContent> videos = new ArrayList<>();
        for (Phim4kMovie movie : movies) {
            videos.add(convertPhim4kMovieToVideoContent(movie));
        }
        return videos;
    }

    private VideoContent convertPhim4kMovieToVideoContent(Phim4kMovie movie) {
        VideoContent video = new VideoContent();
        video.setVideos_id(movie.getId());
        video.setTitle(movie.getName());
        video.setDescription(movie.getContent());
        video.setPosterUrl(movie.getPosterUrl());
        video.setThumbnailUrl(movie.getThumbUrl());
        video.setGenre(movie.getCategoryString());
        video.setVideoType(movie.getType());
        video.setReleaseDate(String.valueOf(movie.getYear()));
        video.setDuration(movie.getTime());
        video.setVideoQuality(movie.getQuality());
        video.setVideoLanguage(movie.getLang());
        video.setIsTvseries("series".equals(movie.getType()));
        
        // Set episodes if available
        if (movie.getEpisodes() != null && !movie.getEpisodes().isEmpty()) {
            List<String> episodeUrls = new ArrayList<>();
            for (Phim4kEpisode episode : movie.getEpisodes()) {
                if (episode.getServerData() != null && !episode.getServerData().isEmpty()) {
                    for (Phim4kEpisodeServer server : episode.getServerData()) {
                        if (server.getLinkM3u8() != null && !server.getLinkM3u8().isEmpty()) {
                            episodeUrls.add(server.getLinkM3u8());
                        }
                    }
                }
            }
            if (!episodeUrls.isEmpty()) {
                video.setVideoUrl(episodeUrls.get(0)); // Use first available URL
            }
        }
        
        return video;
    }
}