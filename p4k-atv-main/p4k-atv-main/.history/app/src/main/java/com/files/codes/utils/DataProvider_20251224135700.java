package com.files.codes.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.files.codes.AppConfig;
import com.files.codes.R;

import com.files.codes.database.TvSeries.TvSeriesViewModel;
import com.files.codes.database.live_tv.LiveTvList;
import com.files.codes.database.live_tv.LiveTvViewModel;
import com.files.codes.database.movie.MovieViewModel;
import com.files.codes.model.HomeContent;
import com.files.codes.model.HomeContentList;
import com.files.codes.model.HomeResponse;
import com.files.codes.model.GenreWithMovies;
import com.files.codes.model.LiveTv;
import com.files.codes.model.Movie;
import com.files.codes.model.MovieList;
import com.files.codes.model.VideoContent;
import com.files.codes.model.Video;
import com.files.codes.model.api.ApiService;
import com.files.codes.model.config.Configuration;
import com.files.codes.viewmodel.HomeContentViewModel;
import com.files.codes.viewmodel.config.ConfigViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.processors.PublishProcessor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class DataProvider {
    private static final String TAG = "DataProvider";
    private Activity context;
    private PublishProcessor<Integer> paginator;
    private PublishProcessor<Integer> mPublishProcessor;
    private CompositeDisposable disposables;
    private int pageCount = 1;
    private int NUMBER_OF_THREADS = 4;
    private ExecutorService executorService;

    public DataProvider(Activity context, CompositeDisposable disposables) {
        this.context = context;
        this.disposables = disposables;
        paginator = PublishProcessor.create();
        mPublishProcessor = PublishProcessor.create();
        executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    }


    public void getAndSaveHomeContentDataFromServer(ViewModelStoreOwner activity) {
        // Log.e(TAG, "getAndSaveHomeContentDataFromServer: " );
        HomeContentViewModel homeContentViewModel = new ViewModelProvider(activity).get(HomeContentViewModel.class);
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Call<HomeResponse> call = api.getHomeContent(AppConfig.API_KEY);
                call.enqueue(new Callback<HomeResponse>() {
                    @Override
                    public void onResponse(Call<HomeResponse> call, Response<HomeResponse> response) {
                        // Log.e(TAG, "onResponse: response code: " +response.code());
                        if (response.code() == 200 && response.body() != null) {
                            // Convert HomeResponse to List<HomeContent> for compatibility
                            List<HomeContent> homeContents = convertHomeResponseToList(response.body());
                            if (homeContents.size() > 0) {
                                HomeContentList list = new HomeContentList();
                                list.setHomeContentId(1);
                                list.setHomeContentList(homeContents);
                                homeContentViewModel.insert(list);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<HomeResponse> call, Throwable t) {
                        // Log.e(TAG, "onFailure: " + t.getLocalizedMessage() );
                    }
                });

            }
        });
    }

    //config data
    public void loadConfigDataFromServer(ViewModelStoreOwner activity){
        ConfigViewModel viewModel = new ViewModelProvider(activity).get(ConfigViewModel.class);
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService apiService = retrofit.create(ApiService.class);
        Call<Configuration> call = apiService.getConfiguration(AppConfig.API_KEY);
        call.enqueue(new Callback<Configuration>() {
            @Override
            public void onResponse(Call<Configuration> call, Response<Configuration> response) {
                if (response.code() ==200 && response.body() != null){
                    Configuration configuration = response.body();
                    configuration.setId(1);
                    viewModel.insert(configuration);
                    // Log.e(TAG, "onResponse: config data updated" );
                }
            }

            @Override
            public void onFailure(Call<Configuration> call, Throwable t) {
                // Log.e(TAG, "onFailure: config data failed"  + t.getLocalizedMessage() );
            }
        });
    }

    public void getMoviesFromServer(ViewModelStoreOwner activity) {
        MovieViewModel viewModel = new ViewModelProvider(activity).get(MovieViewModel.class);
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Call<List<Movie>> call = api.getMovies(AppConfig.API_KEY, 1);
                call.enqueue(new Callback<List<Movie>>() {
                    @Override
                    public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                        if (response.code() == 200 && response.body() != null) {
                            MovieList movieList = new MovieList();
                            movieList.setId(1);
                            movieList.setMovieList(response.body ());
                            viewModel.insert(movieList);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Movie>> call, Throwable t) {

                    }
                });
            }
        });
    }

    private Retrofit getRetrofit(){
        return RetrofitClient.getRetrofitInstance();
    }

    public void getTvSeriesDataFromServer(ViewModelStoreOwner activity){
        TvSeriesViewModel viewModel = new ViewModelProvider(activity).get(TvSeriesViewModel.class);
        ApiService api = getRetrofit().create(ApiService.class);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Call<List<Movie>> call = api.getTvSeries(AppConfig.API_KEY, pageCount);
                call.enqueue(new Callback<List<Movie>>() {
                    @Override
                    public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                        if (response.code() == 200 && response.body() != null){
                            MovieList movieList = new MovieList();
                            movieList.setId(1);
                            movieList.setMovieList(response.body());
                            viewModel.insert(movieList);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Movie>> call, Throwable t) {
                        //failed to load TV Series data from server
                    }
                });
            }
        });
    }

    public void getLiveTvDataFromServer(ViewModelStoreOwner activity){
        LiveTvViewModel viewModel = new ViewModelProvider(activity).get(LiveTvViewModel.class);
        ApiService api = getRetrofit().create(ApiService.class);
        Call<List<LiveTv>> call = api.getLiveTvCategories(AppConfig.API_KEY);
        call.enqueue(new Callback<List<LiveTv>>() {
            @Override
            public void onResponse(Call<List<LiveTv>> call, Response<List<LiveTv>> response) {
                if (response.code() == 200 && response.body() != null){
                    LiveTvList liveTvList = new LiveTvList();
                    liveTvList.setId(1);
                    liveTvList.setLiveTvList(response.body());
                    viewModel.insert(liveTvList);
                }
            }

            @Override
            public void onFailure(Call<List<LiveTv>> call, Throwable t) {

            }
        });
    }

    private List<HomeContent> convertHomeResponseToList(HomeResponse response) {
        Context context = this.context;
        List<HomeContent> homeContents = new ArrayList<>();
        
        // 1. Phim Hay nha Lỵ (Genre Featured from features_genre_and_movie)
        if (response.getFeaturesGenreAndMovie() != null && !response.getFeaturesGenreAndMovie().isEmpty()) {
            HomeContent genreFeatured = new HomeContent();
            genreFeatured.setId("phim_hay");
            genreFeatured.setType("features_genre_and_movie");
            genreFeatured.setTitle("Phim Hay nha Lỵ");
            genreFeatured.setDescription("Phim nổi bật được tuyển chọn");
            
            // Extract movies from first genre (F)
            GenreWithMovies firstGenre = response.getFeaturesGenreAndMovie().get(0);
            if (firstGenre != null && firstGenre.getVideos() != null && !firstGenre.getVideos().isEmpty()) {
                genreFeatured.setContent(firstGenre.getVideos());
                Log.e(TAG, "Added Genre Featured with " + firstGenre.getVideos().size() + " movies");
            } else {
                // Fallback: Use latest movies if genre videos are empty
                Log.e(TAG, "Genre videos empty, using latest movies as fallback");
                if (response.getLatestMovies() != null && !response.getLatestMovies().isEmpty()) {
                    List<VideoContent> featuredMovies = response.getLatestMovies().subList(0, 
                        Math.min(10, response.getLatestMovies().size()));
                    genreFeatured.setContent(featuredMovies);
                    Log.e(TAG, "Added " + featuredMovies.size() + " featured movies from latest");
                }
            }
            homeContents.add(genreFeatured);
        }
        
        // 2. Drama xứ Kim Chi (Korean content)
        HomeContent koreanContent = new HomeContent();
        koreanContent.setId("korea_content");
        koreanContent.setType("country");
        koreanContent.setTitle("Drama xứ Kim Chi");
        koreanContent.setDescription("Phim quốc gia Hàn Quốc");
        koreanContent.setContent(new ArrayList<>()); // Empty list, will load dynamically
        homeContents.add(koreanContent);
        
        // 3. Trường thiên Drama Tàu (Chinese content)
        HomeContent chineseContent = new HomeContent();
        chineseContent.setId("china_content");
        chineseContent.setType("country");
        chineseContent.setTitle("Trường thiên Drama Tàu");
        chineseContent.setDescription("Phim quốc gia Trung Quốc");
        chineseContent.setContent(new ArrayList<>()); // Empty list, will load dynamically
        homeContents.add(chineseContent);
        
        // 4. Xưởng phim xứ Đông Lào (Vietnamese content)
        HomeContent vietnameseContent = new HomeContent();
        vietnameseContent.setId("vietnam_content");
        vietnameseContent.setType("country");
        vietnameseContent.setTitle("Xưởng phim xứ Đông Lào");
        vietnameseContent.setDescription("Phim Quốc gia Việt Nam");
        vietnameseContent.setContent(new ArrayList<>()); // Empty list, will load dynamically
        homeContents.add(vietnameseContent);
        
        // 5. Xi nê Tuổi thơ (Animation content)
        HomeContent animationContent = new HomeContent();
        animationContent.setId("animation_content");
        animationContent.setType("genre");
        animationContent.setTitle("Xi nê Tuổi thơ");
        animationContent.setDescription("Phim thể loại phim hoạt hình");
        animationContent.setContent(new ArrayList<>()); // Empty list, will load dynamically
        homeContents.add(animationContent);
        
        // 6. Latest Movies
        if (response.getLatestMovies() != null) {
            HomeContent movies = new HomeContent();
            movies.setId("latest_movies");
            movies.setType("movie");
            movies.setTitle(context.getString(R.string.latest_movie));
            movies.setContent(response.getLatestMovies());
            homeContents.add(movies);
        }
        
        // 7. Latest TV Series
        if (response.getLatestTvseries() != null) {
            HomeContent tvseries = new HomeContent();
            tvseries.setId("latest_tvseries");
            tvseries.setType("tvseries");
            tvseries.setTitle(context.getString(R.string.latest_tv_series));
            tvseries.setContent(response.getLatestTvseries());
            homeContents.add(tvseries);
        }
        
        return homeContents;
    }
    
    // Phim4k integration methods
    public void getPhim4kLatestMoviesFromServer(ViewModelStoreOwner activity, int page) {
        Log.d(TAG, "Getting latest movies from Phim4k, page: " + page);
        
        MovieViewModel viewModel = new ViewModelProvider(activity).get(MovieViewModel.class);
        executorService.execute(() -> {
            Phim4kClient.getInstance().getLatestMovies(page, new Phim4kClient.Phim4kCallback() {
                @Override
                public void onSuccess(List<VideoContent> videos) {
                    Log.d(TAG, "Successfully loaded " + videos.size() + " movies from Phim4k");
                    
                    // Convert VideoContent objects to Movie objects for compatibility
                    List<Movie> movies = convertVideoContentsToMovies(videos);
                    
                    if (!movies.isEmpty()) {
                        MovieList movieList = new MovieList();
                        movieList.setId(page); // Use page as ID for different batches
                        movieList.setMovieList(movies);
                        viewModel.insert(movieList);
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to load movies from Phim4k: " + error);
                }
            });
        });
    }
    
    public void searchPhim4kMovies(String keyword, Phim4kClient.Phim4kCallback callback) {
        Log.d(TAG, "Searching movies on Phim4k: " + keyword);
        
        executorService.execute(() -> {
            Phim4kClient.getInstance().searchMovies(keyword, callback);
        });
    }
    
    private List<Movie> convertVideoContentsToMovies(List<VideoContent> videos) {
        List<Movie> movies = new ArrayList<>();
        
        for (int i = 0; i < videos.size(); i++) {
            VideoContent video = videos.get(i);
            Movie movie = new Movie();
            
            // Map VideoContent properties to Movie properties (only using available setters)
            movie.setId(video.hashCode()); // Generate unique ID
            movie.setVideosId(video.getVideosId() != null ? video.getVideosId() : "phim4k_" + i);
            movie.setTitle(video.getTitle() != null ? video.getTitle() : "Unknown Title");
            movie.setDescription(video.getDescription() != null ? video.getDescription() : "");
            movie.setSlug(video.getSlug() != null ? video.getSlug() : "phim4k-" + i);
            movie.setRelease(video.getRelease() != null ? video.getRelease() : "2024");
            movie.setIsTvseries(video.getIsTvseries() != null ? video.getIsTvseries() : "0");
            movie.setRuntime(video.getRuntime() != null ? video.getRuntime() : "");
            movie.setVideoQuality(video.getVideoQuality() != null ? video.getVideoQuality() : "HD");
            movie.setThumbnailUrl(video.getThumbnailUrl() != null ? video.getThumbnailUrl() : "");
            movie.setPosterUrl(video.getPosterUrl() != null ? video.getPosterUrl() : "");
            movie.setIsPaid(video.getIsPaid() != null ? video.getIsPaid() : "0");
            
            movies.add(movie);
        }
        
        Log.d(TAG, "Converted " + videos.size() + " videos to " + movies.size() + " movies");
        return movies;
    }
    
    private static List<VideoContent> createPlaceholderContent(String message) {
        List<VideoContent> placeholderList = new ArrayList<>();
        VideoContent placeholder = new VideoContent();
        placeholder.setId("placeholder");
        placeholder.setVideosId("placeholder");
        placeholder.setTitle(message);
        placeholder.setDescription("Content will be loaded dynamically");
        placeholder.setThumbnailUrl("android.resource://com.files.codes.view/drawable/placeholder_image");
        placeholderList.add(placeholder);
        return placeholderList;
    }
}
