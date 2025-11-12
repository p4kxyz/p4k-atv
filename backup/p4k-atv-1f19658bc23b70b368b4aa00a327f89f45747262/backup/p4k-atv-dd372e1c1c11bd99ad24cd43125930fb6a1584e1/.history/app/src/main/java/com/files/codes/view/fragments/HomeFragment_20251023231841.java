package com.files.codes.view.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.core.app.ActivityOptionsCompat;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.files.codes.AppConfig;
import com.files.codes.CardPresenter;
import com.files.codes.R;
import com.files.codes.database.DatabaseHelper;
import com.files.codes.model.Channel;
import com.files.codes.model.HomeContent;
import com.files.codes.model.HomeContentList;
import com.files.codes.model.HomeResponse;
import com.files.codes.model.GenreWithMovies;
import com.files.codes.model.Movie;
import com.files.codes.model.PlaybackModel;
import com.files.codes.model.VideoContent;
import com.files.codes.model.api.ApiService;
import com.files.codes.utils.Constants;
import com.files.codes.utils.DataProvider;
import com.files.codes.utils.LoginAlertDialog;
import com.files.codes.utils.PaidDialog;
import com.files.codes.utils.Phim4kClient;
import com.files.codes.utils.PreferenceUtils;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.utils.Utils;
import com.files.codes.model.sync.WatchHistorySyncItem;
import com.files.codes.utils.sync.WatchHistorySyncManager;
import com.files.codes.utils.TvRecommendationManager;
import com.files.codes.view.ErrorActivity;
import com.files.codes.view.HomeActivity;
import com.files.codes.view.fragments.testFolder.HomeNewActivity;
import com.files.codes.view.PlayerActivity;
import com.files.codes.view.VideoDetailsActivity;
import com.files.codes.view.VideoPlaybackActivity;
import com.files.codes.view.presenter.SliderCardPresenter;
import com.files.codes.view.presenter.HeroBannerPresenter;
// Removed unused imports: TvPresenter
import com.files.codes.viewmodel.HomeContentViewModel;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class HomeFragment extends RowsSupportFragment {
    private final String TAG = HomeActivity.class.getSimpleName();
    //private BackgroundHelper bgHelper;
    private ArrayObjectAdapter rowsAdapter;
    private CardPresenter cardPresenter;
    private WatchHistorySyncManager syncManager;
    private TvRecommendationManager tvRecommendationManager;
    private View v;
    private HomeNewActivity activity;
    private HomeContentViewModel homeContentViewModel;
    
    // Lazy Loading Variables
    private int currentPage = 1;
    private boolean isLoadingMore = false;
    private boolean hasMoreData = true;
    private List<HomeContent> allHomeContent = new ArrayList<>();
    private static final int ITEMS_PER_PAGE = 10;
    
    // Hero Banner Variables
    private ArrayObjectAdapter heroBannerAdapter;
    private List<VideoContent> allHeroBannerContent;
    private int heroBannerLoadedCount = 0;
    private static final int HERO_BANNER_INITIAL_LOAD = 5;
    private static final int HERO_BANNER_LOAD_MORE = 5;
    
    // Section-specific pagination tracking
    private java.util.HashMap<String, Integer> sectionPageMap = new java.util.HashMap<>();
    private java.util.HashMap<String, Boolean> sectionLoadingMap = new java.util.HashMap<>();
    private java.util.HashMap<String, Boolean> sectionHasMoreMap = new java.util.HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //bgHelper = new BackgroundHelper(getActivity());
        activity = (HomeNewActivity) getActivity();
        //activity.showLogo();
        setOnItemViewClickedListener(getDefaultItemViewClickedListener());
        setOnItemViewSelectedListener(getDefaultItemSelectedListener());
        
        // Remove browse fragment padding for full-width hero banner
        getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
        
        // Initialize Watch History Sync Manager and TV Recommendation Manager
        syncManager = WatchHistorySyncManager.getInstance(getContext());
        tvRecommendationManager = new TvRecommendationManager(getContext());

        //home content live data
        homeContentViewModel = new ViewModelProvider(getActivity()).get(HomeContentViewModel.class);
        homeContentViewModel.getHomeContentLiveData().observe(getActivity(), new Observer<HomeContentList>() {
            @Override
            public void onChanged(HomeContentList homeContentList) {
                if (homeContentList != null){
                    loadRows(homeContentList.getHomeContentList());
                     }else {
                    loadHomeContentDataFromServer();
                }
            }
        });


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = super.onCreateView(inflater, container, savedInstanceState);
        
        // Hide the TitleView from Leanback framework (app name text in top-right)
        // Search through the view hierarchy to find and hide it
        if (v != null) {
            v.post(new Runnable() {
                @Override
                public void run() {
                    hideTitleView(v);
                }
            });
        }
        
        return v;
    }
    
    /**
     * Recursively search for and hide TitleView (showing app name in top-right)
     */
    private void hideTitleView(View view) {
        if (view == null) return;
        
        // Check if this view's class name contains "TitleView" or "BrowseTitle"
        String className = view.getClass().getName();
        if (className.contains("TitleView") || className.contains("BrowseTitle")) {
            view.setVisibility(View.GONE);
            Log.d(TAG, "Hidden TitleView: " + className);
            return;
        }
        
        // If it's a ViewGroup, search children recursively
        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            for (int i = 0; i < parent.getChildCount(); i++) {
                hideTitleView(parent.getChildAt(i));
            }
        }
    }

    private void loadHomeContentDataFromServer() {
        loadHomeContentFromAPI(1); // Load first page initially
    }
    
    /**
     * LAZY LOADING: Load home content from API with pagination support
     */
    private void loadHomeContentFromAPI(int page) {
        Log.d(TAG, "🌐 Loading home content from API - Page: " + page);
        
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        Call<HomeResponse> call = api.getHomeContent(AppConfig.API_KEY);
        call.enqueue(new Callback<HomeResponse>() {
            @Override
            public void onResponse(Call<HomeResponse> call, Response<HomeResponse> response) {
                if (response.code() == 200 && response.body() != null) {
                    HomeResponse homeResponse = response.body();

                    // Create list structure to maintain compatibility with existing logic
                    List<HomeContent> homeContents = convertHomeResponseToList(homeResponse);

                    if (homeContents.size() > 0) {
                        if (page == 1) {
                            // First load - initialize everything
                            HomeContentList list = new HomeContentList();
                            list.setHomeContentId(1);
                            list.setHomeContentList(homeContents);
                            homeContentViewModel.insert(list);

                            //save latest movies in constant file for temporary
                            // to add/update channel
                            if (homeContents.size() > 2 && homeContents.get(2).getContent() != null) {
                                Constants.movieList.clear();
                                Constants.movieList = homeContents.get(2).getContent();
                            }
                        } else {
                            // Subsequent loads - append to existing content
                            if (allHomeContent != null) {
                                allHomeContent.addAll(homeContents);
                                
                                // Add new rows to adapter
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            for (HomeContent content : homeContents) {
                                                addHomeContentRow(content, allHomeContent.size());
                                            }
                                            isLoadingMore = false;
                                            Log.d(TAG, "✅ Appended " + homeContents.size() + " more content sections");
                                        }
                                    });
                                }
                            }
                        }
                    } else {
                        // No more data available
                        hasMoreData = false;
                        isLoadingMore = false;
                        if (page == 1) {
                            Toast.makeText(getContext(), getResources().getString(R.string.no_data_found), Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "📝 No more home content available from API");
                        }
                    }
                } else {
                    isLoadingMore = false;
                    if (page == 1) {
                        Toast.makeText(getContext(), getContext().getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<HomeResponse> call, Throwable t) {
                t.printStackTrace();
                isLoadingMore = false;
                if (page == 1) {
                    Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private List<HomeContent> convertHomeResponseToList(HomeResponse response) {
        List<HomeContent> homeContents = new ArrayList<>();
        
        // 1. Add Genre Featured from features_genre_and_movie (🎬 Phim Nổi Bật)
        if (response.getFeaturesGenreAndMovie() != null && !response.getFeaturesGenreAndMovie().isEmpty()) {
            HomeContent genreFeatured = new HomeContent();
            genreFeatured.setId("genre_featured");
            genreFeatured.setType("features_genre_and_movie");
            genreFeatured.setTitle("🎬 Phim Nổi Bật");
            genreFeatured.setDescription("Những bộ phim và series đáng xem nhất");
            
            // Extract movies from first genre (F)
            GenreWithMovies firstGenre = response.getFeaturesGenreAndMovie().get(0);
            if (firstGenre != null && firstGenre.getVideos() != null && !firstGenre.getVideos().isEmpty()) {
                List<VideoContent> genreVideos = firstGenre.getVideos();
                // Set isTvseries for featured content (mixed content, prefer API data or smart detection)
                setIsTvseriesForContent(genreVideos, "features_genre_and_movie", "Phim Hay nha Lỵ");
                genreFeatured.setContent(genreVideos);
                
                // Sync featured content to Android TV home screen
                if (tvRecommendationManager != null) {
                    tvRecommendationManager.addFeaturedContentFromHomepage(genreVideos);
                }
            } else {
                // Fallback: Use latest movies if genre videos are empty
                if (response.getLatestMovies() != null && !response.getLatestMovies().isEmpty()) {
                    // Take first 15 movies as featured for better slider experience
                    List<VideoContent> featuredMovies = response.getLatestMovies().subList(0, 
                        Math.min(15, response.getLatestMovies().size()));
                    // Set isTvseries = "0" for fallback movies (from latest movies)
                    setIsTvseriesForContent(featuredMovies, "movie", "Featured Movies");
                    genreFeatured.setContent(featuredMovies);
                    
                    // Sync fallback featured content to Android TV home screen
                    if (tvRecommendationManager != null) {
                        tvRecommendationManager.addFeaturedContentFromHomepage(featuredMovies);
                    }
                }
            }
            
            homeContents.add(genreFeatured);
        }
        
        // 2. Add Watch History Section (only show when user is logged in)
        boolean isLoggedIn = PreferenceUtils.isLoggedIn(getContext());
        
        if (isLoggedIn) {
            HomeContent watchHistoryContent = new HomeContent();
            watchHistoryContent.setId("watch_history");
            watchHistoryContent.setType("watch_history");
            watchHistoryContent.setTitle("📺 Tiếp tục xem");
            watchHistoryContent.setContent(new ArrayList<>()); // Will be loaded dynamically from sync manager
            homeContents.add(watchHistoryContent);
        } else {
        }
        
        // 3. Add Drama xứ Kim Chi (Hàn Quốc - ID: 13)
        HomeContent koreaContent = new HomeContent();
        koreaContent.setId("13");
        koreaContent.setType("country");
        koreaContent.setTitle("Drama xứ Kim Chi");
        koreaContent.setContent(new ArrayList<>()); // Will be loaded dynamically
        homeContents.add(koreaContent);
        
        // 4. Add Trường thiên Drama Tàu (Trung Quốc - ID: 15)  
        HomeContent chinaContent = new HomeContent();
        chinaContent.setId("15");
        chinaContent.setType("country");
        chinaContent.setTitle("Trường thiên Drama Tàu");
        chinaContent.setContent(new ArrayList<>()); // Will be loaded dynamically
        homeContents.add(chinaContent);
        
        // 5. Add Xưởng phim xứ Đông Lào (Việt Nam - ID: 5)
        HomeContent vietnamContent = new HomeContent();
        vietnamContent.setId("5");
        vietnamContent.setType("country");
        vietnamContent.setTitle("Xưởng phim xứ Đông Lào");
        vietnamContent.setContent(new ArrayList<>()); // Will be loaded dynamically
        homeContents.add(vietnamContent);
        
        // 6. Add Xi nê Tuổi thơ (Hoạt hình - ID: 13)
        HomeContent animationContent = new HomeContent();
        animationContent.setId("13");
        animationContent.setType("genre");
        animationContent.setTitle("Xi nê Tuổi thơ");
        animationContent.setContent(new ArrayList<>()); // Will be loaded dynamically
        homeContents.add(animationContent);
        
        // 7. Add Latest Movies
        if (response.getLatestMovies() != null) {
            HomeContent movies = new HomeContent();
            movies.setId("2");
            movies.setType("movie");
            movies.setTitle(getString(R.string.latest_movie));
            
            // Set isTvseries = "0" for all items in Movies section
            List<VideoContent> movieContent = response.getLatestMovies();
            setIsTvseriesForContent(movieContent, "movie", "Latest Movies");
            movies.setContent(movieContent);
            homeContents.add(movies);
        }
        
        // 8. Add Latest TV Series
        if (response.getLatestTvseries() != null) {
            HomeContent tvseries = new HomeContent();
            tvseries.setId("3");
            tvseries.setType("tvseries");
            tvseries.setTitle(getString(R.string.latest_tv_series));
            
            // Set isTvseries = "1" for all items in TV Series section
            List<VideoContent> tvSeriesContent = response.getLatestTvseries();
            setIsTvseriesForContent(tvSeriesContent, "tvseries", "Latest TV Series");
            tvseries.setContent(tvSeriesContent);
            homeContents.add(tvseries);
        }
        
        // 9. Add Phim4k Movies Section at the bottom (Nguồn free1)
        HomeContent phim4kContent = new HomeContent();
        phim4kContent.setId("phim4k_free1");
        phim4kContent.setType("phim4k");
        phim4kContent.setTitle("Nguồn free1");
        phim4kContent.setContent(new ArrayList<>()); // Will be loaded dynamically
        homeContents.add(phim4kContent);
        
        return homeContents;
    }

    private VideoContent createPlaceholderContent(String message) {
        VideoContent placeholder = new VideoContent();
        placeholder.setId("placeholder");
        placeholder.setTitle(message);
        placeholder.setDescription("Nội dung sẽ được tải sau");
        placeholder.setType("placeholder");
        placeholder.setPosterUrl(""); // Empty poster URL
        placeholder.setThumbnailUrl(""); // Empty thumbnail URL
        placeholder.setIsTvseries("0"); // Default as movie
        placeholder.setIsPaid("0"); // Default as free
        return placeholder;
    }

    /**
     * Helper method to set isTvseries field for content based on section type
     * @param content List of VideoContent to update
     * @param sectionType Type of section (country, genre, tvseries, movie, etc.)
     * @param sectionTitle Title of section for logging
     */
    private void setIsTvseriesForContent(List<VideoContent> content, String sectionType, String sectionTitle) {
        if (content == null) return;
        
        for (VideoContent item : content) {
            if (item != null) {
                // Set based on section type - FORCE override for specific sections
                String isTvseries = "0"; // Default as movie
                
                if ("tvseries".equals(sectionType)) {
                    isTvseries = "1"; // TV Series section - ALWAYS set as TV series
                } else if ("movie".equals(sectionType)) {
                    isTvseries = "0"; // Movie section - ALWAYS set as movie
                } else {
                    // For other sections, check existing API data first
                    if (item.getIsTvseries() != null && !item.getIsTvseries().isEmpty()) {
                        // Keep existing value from API
                        isTvseries = item.getIsTvseries();
                    } else {
                        // For country/genre sections, check title or other indicators
                        String title = item.getTitle() != null ? item.getTitle().toLowerCase() : "";
                        if (title.contains("tập") || title.contains("season") || title.contains("episode")) {
                            isTvseries = "1"; // Likely TV series
                        } else {
                            isTvseries = "0"; // Default as movie
                        }
                    }
                }
                
                item.setIsTvseries(isTvseries);
            }
        }
    }

    private void loadRows(List<HomeContent> list) {
        if (list != null) {
            // Store all content for lazy loading
            allHomeContent.clear();
            allHomeContent.addAll(list);
            
            // Initialize pagination
            currentPage = 1;
            hasMoreData = true;
            isLoadingMore = false;
            
            // Check if watch_history section exists and add it if user is logged in
            boolean hasWatchHistorySection = false;
            for (HomeContent content : list) {
                if ("watch_history".equalsIgnoreCase(content.getType()) || 
                    "watch_history".equalsIgnoreCase(content.getId())) {
                    hasWatchHistorySection = true;
                    break;
                }
            }
            
            boolean isLoggedIn = PreferenceUtils.isLoggedIn(getContext());
            
            if (!hasWatchHistorySection && isLoggedIn) {
                HomeContent watchHistoryContent = new HomeContent();
                watchHistoryContent.setId("watch_history");
                watchHistoryContent.setType("watch_history");
                watchHistoryContent.setTitle("📺 Tiếp tục xem");
                watchHistoryContent.setContent(new ArrayList<>());
                // Add after genre featured (position 1, after features_genre_and_movie at position 0)
                if (list.size() > 1) {
                    list.add(1, watchHistoryContent); // Insert at position 1
                } else {
                    list.add(watchHistoryContent); // Add at end if list is small
                }
            } else if (!isLoggedIn) {
                // User is not logged in, skipping Watch History section for cached data
            } else {
                // Watch History section already exists in cached data
            }
            
            // Check if phim4k section exists, if not add it
            boolean hasPhim4kSection = false;
            for (HomeContent content : list) {
                if ("phim4k".equalsIgnoreCase(content.getType()) || 
                    "phim4k_free1".equalsIgnoreCase(content.getId())) {
                    hasPhim4kSection = true;
                    break;
                }
            }
            
            if (!hasPhim4kSection) {
                HomeContent phim4kContent = new HomeContent();
                phim4kContent.setId("phim4k_free1");
                phim4kContent.setType("phim4k");
                phim4kContent.setTitle("Nguồn free1");
                phim4kContent.setContent(new ArrayList<>());
                list.add(phim4kContent);
            }
            
            // ✅ Use DEFAULT ListRowPresenter with focus effect for all rows
            ListRowPresenter listRowPresenter = new ListRowPresenter();
            // Hero banner will handle its own focus behavior in HeroBannerPresenter
            
            rowsAdapter = new ArrayObjectAdapter(listRowPresenter);
            cardPresenter = new CardPresenter();
            HeroBannerPresenter heroBannerPresenter = new HeroBannerPresenter(); // Netflix-style Hero Banner

            // Load initial batch of content (lazy loading)
            int itemsToLoad = Math.min(ITEMS_PER_PAGE, list.size());
            Log.d(TAG, "🚀 Loading initial content batch: " + itemsToLoad + " items");

            for (int i = 0; i < itemsToLoad; i++) {
                HomeContent homeContent = list.get(i);
                ArrayObjectAdapter listRowAdapter;
                HeaderItem header;

                if (homeContent.getType().equalsIgnoreCase("slider")) {
                    // Skip original slider content
                    continue;
                } else if (homeContent.getType().equalsIgnoreCase("tv")) {
                    // Skip TV Channels - không hiển thị TV channels nữa
                    continue;
                } else if (homeContent.getType().equalsIgnoreCase("features_genre_and_movie")) {
                    // 🎬 NETFLIX-STYLE HERO BANNER - Phim nổi bật từ features_genre_and_movie
                    // LAZY LOADING: Only load first 5 items initially, load more on scroll
                    listRowAdapter = new ArrayObjectAdapter(heroBannerPresenter);
                    header = new HeaderItem(0, ""); // Empty title for hero banner (no header)
                    
                    // Store reference for lazy loading
                    heroBannerAdapter = listRowAdapter;
                    allHeroBannerContent = new ArrayList<>(homeContent.getContent());
                    heroBannerLoadedCount = 0; // Will be incremented when adding items below
                    
                    // Set ALL content for thumbnails with lazy loading support
                    heroBannerPresenter.setFeaturedContent(allHeroBannerContent);
                    
                    int maxHeroItems = Math.min(HERO_BANNER_INITIAL_LOAD, allHeroBannerContent.size());
                    Log.d(TAG, "🎬 Creating Hero Banner - Will load " + maxHeroItems + " of " + allHeroBannerContent.size() + " items initially");
                    Log.d(TAG, "🎬 Featured thumbnails: ALL " + allHeroBannerContent.size() + " items available (lazy loaded)");
                } else if (homeContent.getType().equalsIgnoreCase("country")) {
                    // Country sections (Korea, China, Vietnam)
                    listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                    header = new HeaderItem(homeContent.getId().hashCode(), homeContent.getTitle());
                    // Load content dynamically based on country ID
                    loadContentForCountrySection(homeContent, listRowAdapter);
                } else if (homeContent.getType().equalsIgnoreCase("genre")) {
                    // Genre sections (Animation)
                    listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                    header = new HeaderItem(homeContent.getId().hashCode(), homeContent.getTitle());
                    // Load content dynamically based on genre ID
                    loadContentForGenreSection(homeContent, listRowAdapter);
                } else if (homeContent.getType().equalsIgnoreCase("phim4k")) {
                    // Phim4k section - load content dynamically
                    listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                    header = new HeaderItem(homeContent.getId().hashCode(), homeContent.getTitle());
                    // Load phim4k content dynamically
                    loadPhim4kContent(homeContent, listRowAdapter);
                    // Add the row immediately for phim4k since content will be loaded dynamically
                    rowsAdapter.add(new ListRow(header, listRowAdapter));
                    continue; // Skip the normal content processing for phim4k
                } else if (homeContent.getType().equalsIgnoreCase("watch_history")) {
                    // Watch History section - load content from sync manager
                    listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                    header = new HeaderItem("watch_history".hashCode(), homeContent.getTitle());
                    // Load watch history content dynamically
                    loadWatchHistoryContent(homeContent, listRowAdapter);
                    // Add the row immediately for watch history since content will be loaded dynamically
                    rowsAdapter.add(new ListRow(header, listRowAdapter));
                    continue; // Skip the normal content processing for watch_history
                }else if (homeContent.getId().equalsIgnoreCase("2")){
                    listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                    header = new HeaderItem(2, homeContent.getTitle());
                }else if (homeContent.getType().equalsIgnoreCase("tvseries")){
                    listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                    header = new HeaderItem(3, homeContent.getTitle());
                } else {
                    listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                    header = new HeaderItem(i, list.get(i).getTitle());
                }
                //for (int j = 0; j < NUM_COLS; j++) {
                if (list.get(i).getContent() != null && list.get(i).getContent().size() > 0) {
                    for (int j = 0; j < list.get(i).getContent().size(); j++) {
                        VideoContent videoContent = list.get(i).getContent().get(j);
                        if (list.get(i).getType().equalsIgnoreCase("tv")) {
                            videoContent.setType("tv");
                        } else if (list.get(i).getType().equalsIgnoreCase("movie")) {
                            videoContent.setType("movie");
                        } else if (list.get(i).getType().equalsIgnoreCase("tvseries")) {
                            videoContent.setType("tvseries");
                            videoContent.setIsTvseries("1"); // FORCE set as TV series
                        } else if (list.get(i).getType().equalsIgnoreCase("slider")) {
                            if (videoContent.getIsTvseries().equals("1")) {
                                videoContent.setType("tvseries");
                            } else if (videoContent.getIsTvseries().equals("0")) {
                                videoContent.setType("movie");
                            }
                        } else if (list.get(i).getType().equalsIgnoreCase("features_genre_and_movie")) {
                            // 🎬 Handle Hero Banner content type detection
                            if (videoContent.getIsTvseries() != null && videoContent.getIsTvseries().equals("1")) {
                                videoContent.setType("tvseries");
                            } else if (videoContent.getIsTvseries() != null && videoContent.getIsTvseries().equals("0")) {
                                videoContent.setType("movie");
                            } else {
                                // Default to movie if type is unclear
                                videoContent.setType("movie");
                            }
                        } else if (list.get(i).getType().equalsIgnoreCase("country")) {
                            // Handle country content - will be loaded dynamically
                            videoContent.setType("placeholder");
                        } else if (list.get(i).getType().equalsIgnoreCase("genre")) {
                            // Handle genre content - will be loaded dynamically
                            videoContent.setType("placeholder");
                        } else if (list.get(i).getType().equalsIgnoreCase("phim4k")) {
                            // Handle phim4k content - detect type from content
                            if (videoContent.getIsTvseries() != null && videoContent.getIsTvseries().equals("1")) {
                                videoContent.setType("tvseries");
                            } else {
                                videoContent.setType("movie");
                            }
                        } else {
                            // Default case - set based on isTvseries field
                            if (videoContent.getIsTvseries() != null && videoContent.getIsTvseries().equals("1")) {
                                videoContent.setType("tvseries");
                            } else {
                                videoContent.setType("movie");
                            }
                        }

                        // 🎬 LAZY LOADING: Limit Hero Banner to first 5 items initially
                        if (list.get(i).getType().equalsIgnoreCase("features_genre_and_movie")) {
                            if (j >= HERO_BANNER_INITIAL_LOAD) {
                                break; // Stop after initial items for Hero Banner
                            }
                            heroBannerLoadedCount++; // Track how many items loaded
                        }

                        listRowAdapter.add(videoContent);
                }
                } // Close if content check
                rowsAdapter.add(new ListRow(header, listRowAdapter));
            }

            setAdapter(rowsAdapter);
            getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());

            setCustomPadding();
            
            // Debug pagination maps
            Log.d(TAG, "🗺️ PAGINATION SETUP COMPLETE:");
            for (String sectionId : sectionPageMap.keySet()) {
                Log.d(TAG, "   Section ID: " + sectionId + " - Page: " + sectionPageMap.get(sectionId) + " - HasMore: " + sectionHasMoreMap.get(sectionId));
            }
        } else {
            Intent intent = new Intent(activity, ErrorActivity.class);
            startActivity(intent);
            activity.finish();
        }

    }

    private void setCustomPadding() {
        // RESET padding to 0 - viền đen là do BrowseFragment default margin
        // Sẽ xử lý bằng cách khác
    }

    // click listener
    private OnItemViewClickedListener getDefaultItemViewClickedListener() {
        return new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder viewHolder, Object o,
                                      RowPresenter.ViewHolder viewHolder2, Row row) {

                if (o == null || !(o instanceof VideoContent)) {
                    return; // Safety check
                }

                VideoContent videoContent = (VideoContent) o;
                
                if (videoContent.getId() == null || videoContent.getType() == null) {
                    return;
                }
                
                // Check if this is clear history button
                if ("clear_history_button".equals(videoContent.getId()) || "clear_button".equals(videoContent.getType())) {
                    showClearHistoryConfirmDialog();
                    return;
                }
                
                // Don't process placeholder items
                if ("placeholder".equals(videoContent.getId()) || "placeholder".equals(videoContent.getType())) {
                    return;
                }
                
                // Check if this is a watch history item
                String description = videoContent.getDescription();
                if (description != null && description.startsWith("WATCH_HISTORY:")) {
                    try {
                        // Extract current position from description
                        String positionStr = description.substring("WATCH_HISTORY:".length());
                        long currentPosition = 0;
                        try {
                            currentPosition = Long.parseLong(positionStr);
                        } catch (NumberFormatException e) {
                            // Failed to parse current position
                        }
                        
                        // Determine correct type for watch history item
                        String contentType = "movie"; // default
                        if (videoContent.getIsTvseries() != null && videoContent.getIsTvseries().equals("1")) {
                            contentType = "tvseries";
                        } else if (videoContent.getType() != null) {
                            contentType = videoContent.getType();
                        }
                        
                        Log.d("HomeFragment", "📺 Watch History Type - isTvseries: " + videoContent.getIsTvseries() + ", type: " + videoContent.getType() + " → " + contentType);
                        
                        // Check if video has valid URL before going to player
                        String videoUrl = videoContent.getVideoUrl();
                        if (videoUrl == null || videoUrl.trim().isEmpty()) {
                            Toast.makeText(getActivity(), "Video chưa có link phát, đang chuyển đến trang chi tiết...", Toast.LENGTH_SHORT).show();
                            
                            // Go to details page instead of player
                            Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
                            intent.putExtra("id", videoContent.getId());
                            intent.putExtra("type", contentType);
                            intent.putExtra("thumbImage", videoContent.getThumbnailUrl() != null ? videoContent.getThumbnailUrl() : "");
                            startActivity(intent);
                            return;
                        }
                        
                        // Go directly to player for watch history items with valid URL
                        Intent intent = new Intent(getActivity(), com.files.codes.view.PlayerActivity.class);
                        intent.putExtra("id", videoContent.getId());
                        intent.putExtra("type", contentType);
                        intent.putExtra("title", videoContent.getTitle());
                        intent.putExtra("poster", videoContent.getPosterUrl());
                        intent.putExtra("thumbnail", videoContent.getThumbnailUrl());
                        intent.putExtra("video_url", videoUrl); // Thêm video URL
                        intent.putExtra("position", currentPosition); // Resume from last position
                        intent.putExtra("from_watch_history", true); // Flag để biết đến từ watch history
                        
                        Log.d("HomeFragment", "🎬 Opening player from watch history - ID: " + videoContent.getId() + ", Position: " + currentPosition);
                        
                        startActivity(intent);
                        return;
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), "Lỗi khi mở video từ lịch sử xem", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                
                // Check if it's a TV series based on isTvseries field
                String isTvseries = videoContent.getIsTvseries() != null ? videoContent.getIsTvseries() : "0";
                boolean isActuallyTvSeries = isTvseries.equals("1");
                
                if (isActuallyTvSeries) {
                    // For TV Series - go to episode selection screen
                    Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
                    intent.putExtra("id", videoContent.getId());
                    intent.putExtra("type", "tvseries");
                    intent.putExtra("thumbImage", videoContent.getThumbnailUrl() != null ? videoContent.getThumbnailUrl() : "");

                    //poster transition
                    ImageView imageView = ((ImageCardView) viewHolder.view).getMainImageView();
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                            imageView, VideoDetailsFragment.TRANSITION_NAME).toBundle();

                    startActivity(intent, bundle);
                } else {
                    // For Movies - check subscription and go to player
                    
                    DatabaseHelper db = new DatabaseHelper(getContext());
                    String status = db.getActiveStatusData() != null ? db.getActiveStatusData().getStatus() : "inactive";
                    String isPaid = videoContent.getIsPaid() != null ? videoContent.getIsPaid() : "0";
                    
                    if (isPaid.equals("1")) {
                        if (PreferenceUtils.isLoggedIn(getActivity())) {
                            if (status.equals("active")) {
                                // Go to VideoDetailsActivity for paid movies too
                                Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
                                intent.putExtra("id", videoContent.getId());
                                intent.putExtra("type", "movie");
                                intent.putExtra("thumbImage", videoContent.getThumbnailUrl() != null ? videoContent.getThumbnailUrl() : "");

                                //poster transition
                                ImageView imageView = ((ImageCardView) viewHolder.view).getMainImageView();
                                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                                        imageView, VideoDetailsFragment.TRANSITION_NAME).toBundle();

                                startActivity(intent, bundle);
                            } else {
                                //saved data is not valid, because it was saved more than 2 hours ago
                                PreferenceUtils.updateSubscriptionStatus(getActivity());
                                PaidDialog dialog = new PaidDialog(getContext());
                                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                                dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                                dialog.show();
                            }
                        } else {
                            // user is not logged in
                            // show an alert dialog
                            LoginAlertDialog dialog = new LoginAlertDialog(getActivity());
                            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                            dialog.show();
                        }
                    } else {
                        // Free movie - go to VideoDetailsActivity
                        Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
                        intent.putExtra("id", videoContent.getId());
                        intent.putExtra("type", "movie");
                        intent.putExtra("thumbImage", videoContent.getThumbnailUrl() != null ? videoContent.getThumbnailUrl() : "");

                        //poster transition
                        ImageView imageView = ((ImageCardView) viewHolder.view).getMainImageView();
                        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                                imageView, VideoDetailsFragment.TRANSITION_NAME).toBundle();

                        startActivity(intent, bundle);
                    }
                }
            }
        };
    }

    //listener for setting blur background each time when the item will select.
    protected OnItemViewSelectedListener getDefaultItemSelectedListener() {
        return new OnItemViewSelectedListener() {
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, final Object item,
                                       RowPresenter.ViewHolder rowViewHolder, Row row) {

                // 🎬 HERO BANNER LAZY LOADING - Load more when scrolling near the end
                if (row instanceof ListRow && ((ListRow) row).getHeaderItem().getId() == 0) {
                    // This is the Hero Banner row (ID = 0)
                    checkAndLoadMoreHeroBanner((ListRow) row, item);
                }

                // SECTION-SPECIFIC LAZY LOADING - Primary loading logic
                if (rowsAdapter != null && item instanceof VideoContent) {
                    checkAndLoadMoreForSection(row, (VideoContent) item);
                }

                // Background helper for visual effects
                if (item instanceof VideoContent) {
                    //bgHelper = new BackgroundHelper(getActivity());
                    //bgHelper.prepareBackgroundManager();
                    //bgHelper.startBackgroundTimer(((VideoContent) item).getPosterUrl());
                } else if (item instanceof Channel) {
                    //bgHelper = new BackgroundHelper(getActivity());
                    // bgHelper.prepareBackgroundManager();
                    //bgHelper.startBackgroundTimer(((Channel) item).getPosterUrl());
                }
            }
        };
    }
    
    /**
     * 🎬 Check and load more Hero Banner items when scrolling near the end
     */
    private void checkAndLoadMoreHeroBanner(ListRow heroBannerRow, Object selectedItem) {
        if (heroBannerAdapter == null || allHeroBannerContent == null) {
            return; // Not initialized yet
        }
        
        // Find position of selected item
        int currentPosition = -1;
        for (int i = 0; i < heroBannerAdapter.size(); i++) {
            Object item = heroBannerAdapter.get(i);
            if (item == selectedItem) {
                currentPosition = i;
                break;
            }
        }
        
        // Load more when user reaches the last 2 items
        if (currentPosition >= heroBannerAdapter.size() - 2) {
            loadMoreHeroBannerItems();
        }
    }
    
    /**
     * 🎬 Load more Hero Banner items (next batch)
     */
    private void loadMoreHeroBannerItems() {
        if (heroBannerAdapter == null || allHeroBannerContent == null) {
            return;
        }
        
        // Check if there are more items to load
        if (heroBannerLoadedCount >= allHeroBannerContent.size()) {
            Log.d(TAG, "🎬 Hero Banner: All items already loaded (" + heroBannerLoadedCount + "/" + allHeroBannerContent.size() + ")");
            return;
        }
        
        final int itemsToLoad = Math.min(HERO_BANNER_LOAD_MORE, allHeroBannerContent.size() - heroBannerLoadedCount);
        Log.d(TAG, "🎬 Hero Banner: Loading " + itemsToLoad + " more items (current: " + heroBannerLoadedCount + "/" + allHeroBannerContent.size() + ")");
        
        // CRITICAL: Use Handler.post() to add items AFTER scroll completes
        // This prevents "Cannot call this method while RecyclerView is computing a layout or scrolling" crash
        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < itemsToLoad; i++) {
                    VideoContent video = allHeroBannerContent.get(heroBannerLoadedCount);
                    
                    // Set type correctly
                    if (video.getIsTvseries() != null && video.getIsTvseries().equals("1")) {
                        video.setType("tvseries");
                    } else {
                        video.setType("movie");
                    }
                    
                    heroBannerAdapter.add(video);
                    heroBannerLoadedCount++;
                }
                
                Log.d(TAG, "🎬 Hero Banner: Loaded " + itemsToLoad + " items. Total: " + heroBannerLoadedCount + "/" + allHeroBannerContent.size());
            }
        });
    }
    
    /**
     * Check if we need to load more content for a specific section when user scrolls near the end
     */
    private void checkAndLoadMoreForSection(Row row, VideoContent selectedItem) {
        if (!(row instanceof ListRow)) return;
        
        ListRow listRow = (ListRow) row;
        String sectionTitle = listRow.getHeaderItem().getName();
        ArrayObjectAdapter sectionAdapter = (ArrayObjectAdapter) listRow.getAdapter();
        
        Log.d(TAG, "🔍 checkAndLoadMoreForSection: " + sectionTitle + " - Adapter size: " + sectionAdapter.size());
        
        // Find current item position in this section
        int currentPosition = -1;
        for (int i = 0; i < sectionAdapter.size(); i++) {
            Object item = sectionAdapter.get(i);
            if (item instanceof VideoContent && 
                ((VideoContent) item).getId().equals(selectedItem.getId())) {
                currentPosition = i;
                break;
            }
        }
        
        Log.d(TAG, "👆 Selected position: " + currentPosition + "/" + sectionAdapter.size());
        
        // Load more when user is at the LAST item (same logic as MoviesFragment/TvSeriesFragment)
        if (currentPosition == sectionAdapter.size() - 1) {
            String sectionId = getSectionIdFromTitle(sectionTitle);
            if (sectionId != null && !isPlaceholder(sectionAdapter.get(sectionAdapter.size() - 1))) {
                Log.d(TAG, "🔄 Triggering section lazy load - Position: " + currentPosition + "/" + sectionAdapter.size() + " for " + sectionTitle);
                loadMoreForSpecificSection(sectionId, sectionTitle, sectionAdapter);
            } else {
                Log.d(TAG, "❌ Cannot load more: sectionId=" + sectionId + ", isPlaceholder=" + isPlaceholder(sectionAdapter.get(sectionAdapter.size() - 1)));
            }
        }
    }
    
    /**
     * Load more content for a specific section
     */
    private void loadMoreForSpecificSection(String sectionId, String sectionTitle, ArrayObjectAdapter adapter) {
        // Check if already loading
        Boolean isLoading = sectionLoadingMap.get(sectionId);
        if (isLoading != null && isLoading) {
            Log.d(TAG, "⏳ Already loading for section: " + sectionTitle);
            return;
        }
        
        // Check if has more data
        Boolean hasMore = sectionHasMoreMap.get(sectionId);
        Log.d(TAG, "🎯 Section " + sectionTitle + " (ID: " + sectionId + ") - HasMore: " + hasMore);
        if (hasMore != null && !hasMore) {
            Log.d(TAG, "🚫 No more data for section: " + sectionTitle);
            return;
        }
        
        // Get current page for this section
        Integer currentPage = sectionPageMap.get(sectionId);
        if (currentPage == null) {
            currentPage = 1;
        }
        int nextPage = currentPage + 1;
        
        // Mark as loading
        sectionLoadingMap.put(sectionId, true);
        
        Log.d(TAG, "🔄 Loading more for section: " + sectionTitle + " - Page: " + nextPage);
        
        // Add loading indicator
        adapter.add(createPlaceholderContent("Đang tải thêm " + sectionTitle + "..."));
        
        // Load based on section type
        if (sectionId.contains("country") || sectionId.equals("13") || sectionId.equals("15") || sectionId.equals("5")) {
            loadMoreCountryContent(sectionId, sectionTitle, adapter, nextPage);
        } else if (sectionId.contains("genre") || sectionId.equals("13")) {
            loadMoreGenreContent(sectionId, sectionTitle, adapter, nextPage);
        } else if (sectionId.equals("phim4k_free1")) {
            loadMorePhim4kContent(sectionId, sectionTitle, adapter, nextPage);
        } else if (sectionId.equals("2") || sectionTitle.contains("Latest Movies") || sectionTitle.contains("Phim Mới nhất")) {
            loadMoreLatestMovies(sectionId, sectionTitle, adapter, nextPage);
        } else if (sectionId.equals("3") || sectionTitle.contains("Latest Tv Series") || sectionTitle.contains("Latest TV Series") || sectionTitle.contains("Phim Bộ Mới nhất")) {
            loadMoreLatestTVSeries(sectionId, sectionTitle, adapter, nextPage);
        } else {
            Log.d(TAG, "❌ Unknown section type for: " + sectionTitle + " (ID: " + sectionId + ")");
            sectionLoadingMap.put(sectionId, false);
        }
    }
    
    /**
     * Load more country-specific content
     */
    private void loadMoreCountryContent(String sectionId, String sectionTitle, ArrayObjectAdapter adapter, int page) {
        String countryId = getCountryId(sectionId);
        if (countryId == null) {
            sectionLoadingMap.put(sectionId, false);
            return;
        }
        
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService apiService = retrofit.create(ApiService.class);
        Call<List<Movie>> call = apiService.getMovieByCountry(
            AppConfig.getApiKey(), countryId, page);
        
        call.enqueue(new Callback<List<Movie>>() {
            @Override
            public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Remove loading indicator
                            int lastIndex = adapter.size() - 1;
                            if (lastIndex >= 0 && isPlaceholder(adapter.get(lastIndex))) {
                                adapter.removeItems(lastIndex, 1);
                            }
                            
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                // Add new content
                                for (Movie movie : response.body()) {
                                    VideoContent videoContent = convertMovieToVideoContent(movie);
                                    adapter.add(videoContent);
                                }
                                
                                // Update pagination info
                                sectionPageMap.put(sectionId, page);
                                sectionHasMoreMap.put(sectionId, response.body().size() >= 20); // Assume page size is 20
                                
                                Log.d(TAG, "✅ Loaded " + response.body().size() + " more items for " + sectionTitle);
                            } else {
                                // No more data
                                sectionHasMoreMap.put(sectionId, false);
                                Log.d(TAG, "📝 No more data for " + sectionTitle);
                            }
                            
                            sectionLoadingMap.put(sectionId, false);
                        }
                    });
                }
            }
            
            @Override
            public void onFailure(Call<List<Movie>> call, Throwable t) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Remove loading indicator
                            int lastIndex = adapter.size() - 1;
                            if (lastIndex >= 0 && isPlaceholder(adapter.get(lastIndex))) {
                                adapter.removeItems(lastIndex, 1);
                            }
                            sectionLoadingMap.put(sectionId, false);
                        }
                    });
                }
            }
        });
    }
    
    /**
     * Load more genre-specific content
     */
    private void loadMoreGenreContent(String sectionId, String sectionTitle, ArrayObjectAdapter adapter, int page) {
        String genreId = getGenreId(sectionId);
        if (genreId == null) {
            sectionLoadingMap.put(sectionId, false);
            return;
        }
        
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService apiService = retrofit.create(ApiService.class);
        Call<List<Movie>> call = apiService.getMovieByGenre(
            AppConfig.getApiKey(), genreId, page);
        
        call.enqueue(new Callback<List<Movie>>() {
            @Override
            public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Remove loading indicator
                            int lastIndex = adapter.size() - 1;
                            if (lastIndex >= 0 && isPlaceholder(adapter.get(lastIndex))) {
                                adapter.removeItems(lastIndex, 1);
                            }
                            
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                // Add new content
                                for (Movie movie : response.body()) {
                                    VideoContent videoContent = convertMovieToVideoContent(movie);
                                    adapter.add(videoContent);
                                }
                                
                                // Update pagination info
                                sectionPageMap.put(sectionId, page);
                                sectionHasMoreMap.put(sectionId, response.body().size() >= 20);
                                
                                Log.d(TAG, "✅ Loaded " + response.body().size() + " more items for " + sectionTitle);
                            } else {
                                sectionHasMoreMap.put(sectionId, false);
                                Log.d(TAG, "📝 No more data for " + sectionTitle);
                            }
                            
                            sectionLoadingMap.put(sectionId, false);
                        }
                    });
                }
            }
            
            @Override
            public void onFailure(Call<List<Movie>> call, Throwable t) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Remove loading indicator
                            int lastIndex = adapter.size() - 1;
                            if (lastIndex >= 0 && isPlaceholder(adapter.get(lastIndex))) {
                                adapter.removeItems(lastIndex, 1);
                            }
                            sectionLoadingMap.put(sectionId, false);
                        }
                    });
                }
            }
        });
    }
    
    /**
     * Load more Phim4k content
     */
    private void loadMorePhim4kContent(String sectionId, String sectionTitle, ArrayObjectAdapter adapter, int page) {
        Log.d(TAG, "🎬 loadMorePhim4kContent - Page: " + page + " for section: " + sectionTitle);
        Phim4kClient.getInstance().getLatestMovies(page, new Phim4kClient.Phim4kCallback() {
            @Override
            public void onSuccess(List<VideoContent> videoContents) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Remove loading indicator
                            int lastIndex = adapter.size() - 1;
                            if (lastIndex >= 0 && isPlaceholder(adapter.get(lastIndex))) {
                                adapter.removeItems(lastIndex, 1);
                            }
                            
                            if (videoContents != null && !videoContents.isEmpty()) {
                                // Add new content
                                for (VideoContent videoContent : videoContents) {
                                    if (videoContent.getType() == null) {
                                        if (videoContent.getIsTvseries() != null && videoContent.getIsTvseries().equals("1")) {
                                            videoContent.setType("tvseries");
                                        } else {
                                            videoContent.setType("movie");
                                        }
                                    }
                                    adapter.add(videoContent);
                                }
                                
                                // Update pagination info
                                sectionPageMap.put(sectionId, page);
                                sectionHasMoreMap.put(sectionId, videoContents.size() >= 20);
                                
                                Log.d(TAG, "✅ Loaded " + videoContents.size() + " more Phim4k items - HasMore: " + (videoContents.size() >= 20));
                            } else {
                                sectionHasMoreMap.put(sectionId, false);
                                Log.d(TAG, "📝 No more Phim4k data");
                            }
                            
                            sectionLoadingMap.put(sectionId, false);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Remove loading indicator
                            int lastIndex = adapter.size() - 1;
                            if (lastIndex >= 0 && isPlaceholder(adapter.get(lastIndex))) {
                                adapter.removeItems(lastIndex, 1);
                            }
                            sectionLoadingMap.put(sectionId, false);
                        }
                    });
                }
            }
        });
    }
    
    /**
     * Load more Latest Movies
     */
    private void loadMoreLatestMovies(String sectionId, String sectionTitle, ArrayObjectAdapter adapter, int page) {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService apiService = retrofit.create(ApiService.class);
        // Use getMovies() API for pagination since there's no specific getLatestMovies() with pagination
        Call<List<Movie>> call = apiService.getMovies(AppConfig.getApiKey(), page);
        
        call.enqueue(new Callback<List<Movie>>() {
            @Override
            public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Remove loading indicator
                            int lastIndex = adapter.size() - 1;
                            if (lastIndex >= 0 && isPlaceholder(adapter.get(lastIndex))) {
                                adapter.removeItems(lastIndex, 1);
                            }
                            
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                for (Movie movie : response.body()) {
                                    VideoContent videoContent = convertMovieToVideoContent(movie);
                                    videoContent.setType("movie");
                                    videoContent.setIsTvseries("0");
                                    adapter.add(videoContent);
                                }
                                
                                sectionPageMap.put(sectionId, page);
                                sectionHasMoreMap.put(sectionId, response.body().size() >= 20);
                                
                                Log.d(TAG, "✅ Loaded " + response.body().size() + " more latest movies");
                            } else {
                                sectionHasMoreMap.put(sectionId, false);
                            }
                            
                            sectionLoadingMap.put(sectionId, false);
                        }
                    });
                }
            }
            
            @Override
            public void onFailure(Call<List<Movie>> call, Throwable t) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int lastIndex = adapter.size() - 1;
                            if (lastIndex >= 0 && isPlaceholder(adapter.get(lastIndex))) {
                                adapter.removeItems(lastIndex, 1);
                            }
                            sectionLoadingMap.put(sectionId, false);
                        }
                    });
                }
            }
        });
    }
    
    /**
     * Load more Latest TV Series
     */
    private void loadMoreLatestTVSeries(String sectionId, String sectionTitle, ArrayObjectAdapter adapter, int page) {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService apiService = retrofit.create(ApiService.class);
        // Use getTvSeries() API for pagination since there's no specific getLatestTvSeries() with pagination
        Call<List<Movie>> call = apiService.getTvSeries(AppConfig.getApiKey(), page);
        
        call.enqueue(new Callback<List<Movie>>() {
            @Override
            public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Remove loading indicator
                            int lastIndex = adapter.size() - 1;
                            if (lastIndex >= 0 && isPlaceholder(adapter.get(lastIndex))) {
                                adapter.removeItems(lastIndex, 1);
                            }
                            
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                for (Movie movie : response.body()) {
                                    VideoContent videoContent = convertMovieToVideoContent(movie);
                                    videoContent.setType("tvseries");
                                    videoContent.setIsTvseries("1");
                                    adapter.add(videoContent);
                                }
                                
                                sectionPageMap.put(sectionId, page);
                                sectionHasMoreMap.put(sectionId, response.body().size() >= 20);
                                
                                Log.d(TAG, "✅ Loaded " + response.body().size() + " more latest TV series");
                            } else {
                                sectionHasMoreMap.put(sectionId, false);
                            }
                            
                            sectionLoadingMap.put(sectionId, false);
                        }
                    });
                }
            }
            
            @Override
            public void onFailure(Call<List<Movie>> call, Throwable t) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int lastIndex = adapter.size() - 1;
                            if (lastIndex >= 0 && isPlaceholder(adapter.get(lastIndex))) {
                                adapter.removeItems(lastIndex, 1);
                            }
                            sectionLoadingMap.put(sectionId, false);
                        }
                    });
                }
            }
        });
    }
    
    /**
     * Helper method to get section ID from title
     */
    private String getSectionIdFromTitle(String title) {
        Log.d(TAG, "🔍 getSectionIdFromTitle: " + title);
        if (title.contains("Drama xứ Kim Chi")) return "13";  // Cùng ID với genre mapping
        if (title.contains("Trường thiên Drama Tàu")) return "15";  // Cùng ID với genre mapping
        if (title.contains("Xưởng phim xứ Đông Lào")) return "5";   // Cùng ID với genre mapping
        if (title.contains("Xi nê Tuổi thơ")) return "13";          // Genre ID
        if (title.contains("Latest Movies") || title.contains("Phim Mới nhất")) {
            Log.d(TAG, "📱 Matched Latest Movies section");
            return "2";
        }
        // Fix TV Series matching - cần match cả "Latest Tv Series" và "Latest TV Series"
        if (title.contains("Latest Tv Series") || title.contains("Latest TV Series") || title.contains("Phim Bộ Mới nhất")) {
            Log.d(TAG, "📺 Matched Latest TV Series section");
            return "3";
        }
        if (title.contains("Nguồn free1")) return "phim4k_free1";
        Log.d(TAG, "❌ No match found for title: " + title);
        return null;
    }
    
    /**
     * Check if an item is a placeholder
     */
    private boolean isPlaceholder(Object item) {
        if (item instanceof VideoContent) {
            VideoContent content = (VideoContent) item;
            return "placeholder".equals(content.getId()) || 
                   (content.getTitle() != null && content.getTitle().startsWith("Đang tải"));
        }
        return false;
    }
    
    private void loadContentForCountrySection(HomeContent homeContent, ArrayObjectAdapter adapter) {
        String countryId = getCountryId(homeContent.getId());
        if (countryId != null) {
            // Initialize pagination for this section
            sectionPageMap.put(homeContent.getId(), 1);
            sectionLoadingMap.put(homeContent.getId(), false);
            sectionHasMoreMap.put(homeContent.getId(), true);
            
            // Add placeholder first
            adapter.add(createPlaceholderContent("Đang tải " + homeContent.getTitle() + "..."));
            
            // Load real content
            Retrofit retrofit = RetrofitClient.getRetrofitInstance();
            ApiService apiService = retrofit.create(ApiService.class);
            Call<List<Movie>> call = apiService.getMovieByCountry(
                AppConfig.getApiKey(), countryId, 1);
            
            call.enqueue(new Callback<List<Movie>>() {
                @Override
                public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        // Run on UI thread to ensure proper updates
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Clear placeholder
                                    adapter.clear();
                                    // Add real content
                                    for (Movie movie : response.body()) {
                                        VideoContent videoContent = convertMovieToVideoContent(movie);
                                        adapter.add(videoContent);
                                    }
                                    
                                    // Update pagination info
                                    sectionHasMoreMap.put(homeContent.getId(), response.body().size() >= 20);
                                    
                                    // Notify adapter of changes
                                    adapter.notifyArrayItemRangeChanged(0, adapter.size());
                                }
                            });
                        }
                    } else {
                        // No data available
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.clear();
                                    adapter.add(createPlaceholderContent("Không có dữ liệu " + homeContent.getTitle()));
                                    sectionHasMoreMap.put(homeContent.getId(), false);
                                }
                            });
                        }
                    }
                }
                
                @Override
                public void onFailure(Call<List<Movie>> call, Throwable t) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.clear();
                                adapter.add(createPlaceholderContent("Lỗi tải " + homeContent.getTitle()));
                                sectionHasMoreMap.put(homeContent.getId(), false);
                            }
                        });
                    }
                }
            });
        }
    }
    
    private void loadContentForGenreSection(HomeContent homeContent, ArrayObjectAdapter adapter) {
        String genreId = getGenreId(homeContent.getId());
        if (genreId != null) {
            // Initialize pagination for this section
            sectionPageMap.put(homeContent.getId(), 1);
            sectionLoadingMap.put(homeContent.getId(), false);
            sectionHasMoreMap.put(homeContent.getId(), true);
            
            // Add placeholder first
            adapter.add(createPlaceholderContent("Đang tải " + homeContent.getTitle() + "..."));
            
            // Load real content
            Retrofit retrofit = RetrofitClient.getRetrofitInstance();
            ApiService apiService = retrofit.create(ApiService.class);
            Call<List<Movie>> call = apiService.getMovieByGenre(
                AppConfig.getApiKey(), genreId, 1);
            
            call.enqueue(new Callback<List<Movie>>() {
                @Override
                public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        // Run on UI thread to ensure proper updates
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Clear placeholder
                                    adapter.clear();
                                    // Add real content
                                    for (Movie movie : response.body()) {
                                        VideoContent videoContent = convertMovieToVideoContent(movie);
                                        adapter.add(videoContent);
                                    }
                                    
                                    // Update pagination info
                                    sectionHasMoreMap.put(homeContent.getId(), response.body().size() >= 20);
                                    
                                    // Notify adapter of changes
                                    adapter.notifyArrayItemRangeChanged(0, adapter.size());
                                }
                            });
                        }
                    } else {
                        // No data available
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.clear();
                                    adapter.add(createPlaceholderContent("Không có dữ liệu " + homeContent.getTitle()));
                                    sectionHasMoreMap.put(homeContent.getId(), false);
                                }
                            });
                        }
                    }
                }
                
                @Override
                public void onFailure(Call<List<Movie>> call, Throwable t) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.clear();
                                adapter.add(createPlaceholderContent("Lỗi tải " + homeContent.getTitle()));
                                sectionHasMoreMap.put(homeContent.getId(), false);
                            }
                        });
                    }
                }
            });
        }
    }
    
    private String getCountryId(String sectionId) {
        switch (sectionId) {
            case "13": // Korea content (Drama xứ Kim Chi)
                return "13"; 
            case "15": // China content (Trường thiên Drama Tàu)
                return "15"; 
            case "5":  // Vietnam content (Xưởng phim xứ Đông Lào)
                return "5";  
            case "korea_content":
                return "13";
            case "china_content":
                return "15";
            case "vietnam_content":
                return "5";
            default:
                return null;
        }
    }
    
    private String getGenreId(String sectionId) {
        switch (sectionId) {
            case "animation_content":
                return "13"; // Animation genre ID
            default:
                return null;
        }
    }
    
    private VideoContent convertMovieToVideoContent(Movie movie) {
        VideoContent videoContent = new VideoContent();
        videoContent.setId(String.valueOf(movie.getId()));
        videoContent.setVideosId(movie.getVideosId());
        videoContent.setTitle(movie.getTitle());
        videoContent.setDescription(movie.getDescription());
        videoContent.setThumbnailUrl(movie.getThumbnailUrl());
        videoContent.setPosterUrl(movie.getPosterUrl());
        videoContent.setIsTvseries(movie.getIsTvseries());
        videoContent.setRelease(movie.getRelease());
        videoContent.setRuntime(movie.getRuntime());
        videoContent.setVideoQuality(movie.getVideoQuality());
        
        // Set Type field based on is_tvseries
        if (movie.getIsTvseries() != null && movie.getIsTvseries().equals("1")) {
            videoContent.setType("tv");
        } else {
            videoContent.setType("movie");
        }
        
        // Set other required fields for proper functionality
        videoContent.setIsPaid(movie.getIsPaid() != null ? movie.getIsPaid() : "0");
        
        return videoContent;
    }
    
    private void loadPhim4kContent(HomeContent homeContent, ArrayObjectAdapter adapter) {
        // Initialize pagination for this section
        sectionPageMap.put(homeContent.getId(), 1);
        sectionLoadingMap.put(homeContent.getId(), false);
        sectionHasMoreMap.put(homeContent.getId(), true);
        
        // Add placeholder first
        adapter.add(createPlaceholderContent("Đang tải Phim4k..."));
        
        // Use Phim4kClient directly with callback
        Phim4kClient.getInstance().getLatestMovies(1, new Phim4kClient.Phim4kCallback() {
            @Override
            public void onSuccess(List<VideoContent> videoContents) {
                if (videoContents != null && !videoContents.isEmpty()) {
                    
                    // Run on UI thread to ensure proper updates
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Clear placeholder
                                adapter.clear();
                                // Add real content
                                for (VideoContent videoContent : videoContents) {
                                    // Make sure type is set correctly
                                    if (videoContent.getType() == null) {
                                        if (videoContent.getIsTvseries() != null && videoContent.getIsTvseries().equals("1")) {
                                            videoContent.setType("tvseries");
                                        } else {
                                            videoContent.setType("movie");
                                        }
                                    }
                                    adapter.add(videoContent);
                                }
                                
                                // Update pagination info - for Phim4k, always assume more data initially
                                // since their API might return less than 20 items per page
                                sectionHasMoreMap.put(homeContent.getId(), true);
                                Log.d(TAG, "🎬 Phim4k initial data - Size: " + videoContents.size() + ", HasMore: true (forced for pagination)");
                                
                                // Notify adapter of changes
                                adapter.notifyArrayItemRangeChanged(0, adapter.size());
                            }
                        });
                    }
                } else {
                    // Phim4k returned empty result
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.clear();
                                adapter.add(createPlaceholderContent("Không có phim từ Phim4k"));
                                sectionHasMoreMap.put(homeContent.getId(), false);
                            }
                        });
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.clear();
                            adapter.add(createPlaceholderContent("Lỗi tải Phim4k: " + error));
                            sectionHasMoreMap.put(homeContent.getId(), false);
                        }
                    });
                }
            }
        });
    }
    
    private void loadWatchHistoryContent(HomeContent homeContent, ArrayObjectAdapter adapter) {
        // Check login status and email availability
        if (!PreferenceUtils.isLoggedIn(getContext())) {
            adapter.add(createPlaceholderContent("Vui lòng đăng nhập để xem lịch sử"));
            return;
        }
        
        if (syncManager != null) {
            // Check if sync manager can auto sync (has valid email)
            if (!syncManager.canAutoSync()) {
                adapter.add(createPlaceholderContent("Email không hợp lệ để sync lịch sử"));
                return;
            }
            
            String userEmail = syncManager.getCurrentUserEmail();
            
            // Check if sync link exists (user ID available)
            if (syncManager.getSyncUserId() == null) {
                // Create sync link first, then sync
                syncManager.createSyncLink(userEmail, new WatchHistorySyncManager.SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        // Now sync from server
                        syncFromServerAndLoad(adapter);
                    }
                    
                    @Override
                    public void onError(String error) {
                        // Try to load local watch history as fallback
                        loadLocalWatchHistoryFallback(adapter);
                    }
                });
            } else {
                // Sync link exists, directly sync from server
                syncFromServerAndLoad(adapter);
            }
        } else {
            adapter.add(createPlaceholderContent("Lịch sử xem không khả dụng"));
        }
    }

    private void loadHistoryForDisplay(ArrayObjectAdapter adapter) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Get watch history data using the correct method
                    List<WatchHistorySyncItem.WatchHistoryItem> historyItems = syncManager.getWatchHistoryForDisplay();
                    
                    if (historyItems != null && !historyItems.isEmpty()) {
                        // Convert watch history items to VideoContent
                        List<VideoContent> historyVideoContent = new ArrayList<>();
                        
                        for (WatchHistorySyncItem.WatchHistoryItem historyItem : historyItems) {
                            VideoContent videoContent = new VideoContent();
                            videoContent.setId(historyItem.getVideoId());
                            videoContent.setTitle(historyItem.getTitle());
                            
                            // Prioritize thumbnailUrl over posterUrl for watch history
                            String thumbnailUrl = historyItem.getThumbnailUrl();
                            String posterUrl = historyItem.getPosterUrl();
                            
                            if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                                // Use thumbnail as both poster and thumbnail for compact display
                                videoContent.setPosterUrl(thumbnailUrl);
                                videoContent.setThumbnailUrl(thumbnailUrl);
                            } else if (posterUrl != null && !posterUrl.isEmpty()) {
                                // Fallback to poster if no thumbnail
                                videoContent.setPosterUrl(posterUrl);
                                videoContent.setThumbnailUrl(posterUrl);
                            } else {
                                // No image available
                                videoContent.setPosterUrl("");
                                videoContent.setThumbnailUrl("");
                            }
                            
                            // Set correct type and isTvseries from watch history item
                            // Check isTvSeries field first (more reliable)
                            String isTvSeries = historyItem.getIsTvSeries();
                            String videoType;
                            
                            if (isTvSeries != null && isTvSeries.equals("1")) {
                                videoType = "tvseries";
                                videoContent.setIsTvseries("1");
                            } else if (historyItem.getVideoType() != null) {
                                videoType = historyItem.getVideoType();
                                videoContent.setIsTvseries("tvseries".equals(videoType) || "tv".equals(videoType) ? "1" : "0");
                            } else {
                                videoType = "movie";
                                videoContent.setIsTvseries("0");
                            }
                            
                            videoContent.setType(videoType);
                            
                            // Set video URL from watch history
                            String videoUrl = historyItem.getVideoUrl();
                            if (videoUrl != null && !videoUrl.isEmpty()) {
                                videoContent.setVideoUrl(videoUrl);
                            }
                            
                            // Mark as watch history item and store current position
                            long currentPos = historyItem.getCurrentPosition();
                            videoContent.setDescription("WATCH_HISTORY:" + currentPos);
                            
                            historyVideoContent.add(videoContent);
                        }
                        
                        // Update UI on main thread
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.clear();
                                    
                                    // Thêm nút "Xóa tất cả" đầu tiên nếu có lịch sử
                                    if (!historyVideoContent.isEmpty()) {
                                        adapter.add(createClearHistoryButton());
                                    }
                                    
                                    for (VideoContent content : historyVideoContent) {
                                        adapter.add(content);
                                    }
                                }
                            });
                        }
                    } else {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.clear();
                                    adapter.add(createPlaceholderContent("Chưa có lịch sử xem"));
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.clear();
                                adapter.add(createPlaceholderContent("Lỗi tải lịch sử xem"));
                            }
                        });
                    }
                }
            }
        }).start();
    }
    
    private void syncFromServerAndLoad(ArrayObjectAdapter adapter) {
        // First sync from server, then load for display
        syncManager.syncWatchHistoryFromServer(new WatchHistorySyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                loadHistoryForDisplay(adapter);
            }
            
            @Override
            public void onError(String error) {
                // If sync fails, try to load local history as fallback
                loadLocalWatchHistoryFallback(adapter);
            }
        });
    }
    
    private void loadLocalWatchHistoryFallback(ArrayObjectAdapter adapter) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (syncManager != null) {
                        // Try to get local watch history data
                        List<WatchHistorySyncItem.WatchHistoryItem> historyItems = syncManager.getWatchHistoryForDisplay();
                        Log.e("HomeFragment", "Retrieved " + (historyItems != null ? historyItems.size() : "null") + " local history items");
                        
                        if (historyItems != null && !historyItems.isEmpty()) {
                            // Convert watch history items to VideoContent
                            List<VideoContent> historyVideoContent = new ArrayList<>();
                            
                            for (WatchHistorySyncItem.WatchHistoryItem historyItem : historyItems) {
                                VideoContent videoContent = new VideoContent();
                                videoContent.setId(historyItem.getVideoId());
                                videoContent.setTitle(historyItem.getTitle());
                                videoContent.setPosterUrl(historyItem.getPosterUrl());
                                // Use thumbnailUrl if available, otherwise fallback to posterUrl
                                String thumbnailUrl = historyItem.getThumbnailUrl() != null ? 
                                        historyItem.getThumbnailUrl() : historyItem.getPosterUrl();
                                videoContent.setThumbnailUrl(thumbnailUrl);
                                videoContent.setType("movie"); // Default type
                                
                                // Mark as watch history item and store current position
                                videoContent.setDescription("WATCH_HISTORY:" + historyItem.getCurrentPosition());
                                
                                historyVideoContent.add(videoContent);
                            }
                            
                            // Update UI on main thread
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        adapter.clear();
                                        for (VideoContent content : historyVideoContent) {
                                            adapter.add(content);
                                        }
                                        Log.e("HomeFragment", "Updated UI with " + historyVideoContent.size() + " local history items");
                                    }
                                });
                            }
                        } else {
                            // No local history found either
                            Log.e("HomeFragment", "No local watch history found");
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        adapter.clear();
                                        adapter.add(createPlaceholderContent("Chưa có lịch sử xem (Offline)"));
                                    }
                                });
                            }
                        }
                    } else {
                        Log.e("HomeFragment", "SyncManager is null, cannot load local history");
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.clear();
                                    adapter.add(createPlaceholderContent("Lịch sử xem không khả dụng"));
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e("HomeFragment", "Error loading local watch history", e);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.clear();
                                adapter.add(createPlaceholderContent("Lỗi tải lịch sử xem local"));
                            }
                        });
                    }
                }
            }
        }).start();
    }

    /**
     * Tạo item "Xóa tất cả lịch sử" cho section lịch sử xem
     */
    private VideoContent createClearHistoryButton() {
        VideoContent clearButton = new VideoContent();
        clearButton.setId("clear_history_button");
        clearButton.setTitle("XÓA TẤT CẢ");
        clearButton.setDescription("Xóa toàn bộ lịch sử xem");
        // Sử dụng một placeholder image màu đỏ để làm nút xóa
        clearButton.setThumbnailUrl("https://via.placeholder.com/300x450/FF4444/FFFFFF?text=XÓA+TẤT+CẢ"); 
        clearButton.setVideoUrl(""); // Không có video URL
        clearButton.setType("clear_button");
        clearButton.setRelease(""); 
        clearButton.setRuntime(""); 
        return clearButton;
    }

    /**
     * Hiển thị dialog xác nhận xóa lịch sử
     */
    private void showClearHistoryConfirmDialog() {
        if (getActivity() == null) return;
        
        android.app.AlertDialog.Builder builder = 
            new android.app.AlertDialog.Builder(getActivity());
        builder.setTitle("⚠️ Xác nhận xóa lịch sử")
                .setMessage("Bạn có chắc muốn xóa TOÀN BỘ lịch sử xem?\n\n🔸 Sẽ xóa lịch sử trên thiết bị này\n🔸 Sẽ xóa lịch sử trên server (nếu có đồng bộ)\n\n❌ Thao tác này không thể hoàn tác!")
                .setPositiveButton("XÓA TẤT CẢ", (dialog, which) -> {
                    clearAllWatchHistory();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Thực hiện xóa toàn bộ lịch sử xem
     */
    private void clearAllWatchHistory() {
        if (syncManager == null) {
            Toast.makeText(getActivity(), "Lỗi: Không thể xóa lịch sử", Toast.LENGTH_SHORT).show();
            return;
        }

        syncManager.clearAllWatchHistory(new WatchHistorySyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getActivity(), "✅ " + message, Toast.LENGTH_SHORT).show();
                        // Refresh lại HomeFragment để cập nhật danh sách
                        refreshWatchHistorySection();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getActivity(), "❌ Lỗi: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    /**
     * Refresh lại section lịch sử xem sau khi clear
     */
    private void refreshWatchHistorySection() {
        if (rowsAdapter != null) {
            // Tìm watch history section trong adapter và reload
            for (int i = 0; i < rowsAdapter.size(); i++) {
                Object item = rowsAdapter.get(i);
                if (item instanceof ListRow) {
                    ListRow row = (ListRow) item;
                    if (row.getHeaderItem().getName().equals("Lịch sử xem")) {
                        // Tạo lại adapter cho section này
                        ArrayObjectAdapter historyAdapter = new ArrayObjectAdapter(cardPresenter);
                        HomeContent watchHistoryContent = new HomeContent();
                        watchHistoryContent.setId("watch_history");
                        watchHistoryContent.setType("watch_history");
                        watchHistoryContent.setTitle("Lịch sử xem");
                        
                        loadWatchHistoryContent(watchHistoryContent, historyAdapter);
                        
                        // Update row
                        HeaderItem header = new HeaderItem("watch_history".hashCode(), "Lịch sử xem");
                        ListRow newRow = new ListRow(header, historyAdapter);
                        rowsAdapter.replace(i, newRow);
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * LAZY LOADING: Load more home content when user scrolls near the end
     */
    private void loadMoreHomeContent() {
        if (isLoadingMore || !hasMoreData) {
            return;
        }
        
        isLoadingMore = true;
        currentPage++;
        
        // For now, simulate loading more content with a delay
        // In a real scenario, you would call API with pagination
        Log.d(TAG, "🔄 Loading more home content - Page: " + currentPage);
        
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    // Option 1: Load from API (if API supports pagination)
                    // loadHomeContentFromAPI(currentPage);
                    
                    // Option 2: Load from cached content (current implementation)
                    if (allHomeContent != null) {
                        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
                        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allHomeContent.size());
                        
                        if (startIndex >= allHomeContent.size()) {
                            // No more data to load
                            hasMoreData = false;
                            isLoadingMore = false;
                            Log.d(TAG, "📝 No more home content to load");
                            return;
                        }
                        
                        // Add next batch of content
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    for (int i = startIndex; i < endIndex; i++) {
                                        HomeContent homeContent = allHomeContent.get(i);
                                        addHomeContentRow(homeContent, i);
                                    }
                                    
                                    // Check if we've loaded all content
                                    if (endIndex >= allHomeContent.size()) {
                                        hasMoreData = false;
                                        Log.d(TAG, "✅ All home content loaded");
                                    }
                                    
                                    isLoadingMore = false;
                                    Log.d(TAG, "✅ Loaded batch " + currentPage + " - Items: " + (endIndex - startIndex));
                                }
                            });
                        }
                    } else {
                        isLoadingMore = false;
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error loading more home content", e);
                    isLoadingMore = false;
                }
            }
        }, 800); // 800ms delay for smooth loading experience
    }
    
    /**
     * Add individual home content row to adapter
     */
    private void addHomeContentRow(HomeContent homeContent, int index) {
        try {
            ArrayObjectAdapter listRowAdapter;
            HeaderItem header;
            CardPresenter cardPresenter = new CardPresenter();
            SliderCardPresenter sliderCardPresenter = new SliderCardPresenter();
            
            if (homeContent.getType().equalsIgnoreCase("slider")) {
                // Skip original slider content
                return;
            } else if (homeContent.getType().equalsIgnoreCase("tv")) {
                // Skip TV Channels - không hiển thị TV channels nữa
                return;
            } else if (homeContent.getType().equalsIgnoreCase("features_genre_and_movie")) {
                // Load Genre Featured as Slider Banner (phim nổi bật từ features_genre_and_movie)
                listRowAdapter = new ArrayObjectAdapter(sliderCardPresenter);
                header = new HeaderItem(0, homeContent.getTitle());
            } else if (homeContent.getType().equalsIgnoreCase("country")) {
                // Country sections (Korea, China, Vietnam)
                listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                header = new HeaderItem(homeContent.getId().hashCode(), homeContent.getTitle());
                // Load content dynamically based on country ID
                loadContentForCountrySection(homeContent, listRowAdapter);
            } else if (homeContent.getType().equalsIgnoreCase("genre")) {
                // Genre sections (Animation)
                listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                header = new HeaderItem(homeContent.getId().hashCode(), homeContent.getTitle());
                // Load content dynamically based on genre ID
                loadContentForGenreSection(homeContent, listRowAdapter);
            } else if (homeContent.getType().equalsIgnoreCase("phim4k")) {
                // Phim4k section - load content dynamically
                listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                header = new HeaderItem(homeContent.getId().hashCode(), homeContent.getTitle());
                // Load phim4k content dynamically
                loadPhim4kContent(homeContent, listRowAdapter);
            } else if (homeContent.getType().equalsIgnoreCase("watch_history")) {
                // Watch History section
                listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                header = new HeaderItem(homeContent.getId().hashCode(), homeContent.getTitle());
                loadWatchHistoryContent(homeContent, listRowAdapter);
            } else {
                // Regular content sections
                listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                header = new HeaderItem(index, homeContent.getTitle());
            }
            
                // Add content to row adapter
                if (homeContent.getContent() != null && homeContent.getContent().size() > 0) {
                    // Initialize pagination for sections with existing content
                    if (homeContent.getType().equalsIgnoreCase("movie")) {
                        Log.d(TAG, "🎬 Initializing pagination for Latest Movies section (ID: " + homeContent.getId() + ")");
                        sectionPageMap.put(homeContent.getId(), 1);
                        sectionLoadingMap.put(homeContent.getId(), false);
                        sectionHasMoreMap.put(homeContent.getId(), true);
                    } else if (homeContent.getType().equalsIgnoreCase("tvseries")) {
                        Log.d(TAG, "📺 Initializing pagination for Latest TV Series section (ID: " + homeContent.getId() + ")");
                        sectionPageMap.put(homeContent.getId(), 1);
                        sectionLoadingMap.put(homeContent.getId(), false);
                        sectionHasMoreMap.put(homeContent.getId(), true);
                    }
                    
                    for (VideoContent videoContent : homeContent.getContent()) {
                        if (homeContent.getType().equalsIgnoreCase("tv")) {
                            videoContent.setType("tv");
                        } else if (homeContent.getType().equalsIgnoreCase("movie")) {
                            videoContent.setType("movie");
                            videoContent.setIsTvseries("0");
                        } else if (homeContent.getType().equalsIgnoreCase("tvseries")) {
                            videoContent.setType("tvseries");
                            videoContent.setIsTvseries("1");
                        }
                        listRowAdapter.add(videoContent);
                    }
                }            // Add row to main adapter if it has content
            if (listRowAdapter.size() > 0) {
                ListRow listRow = new ListRow(header, listRowAdapter);
                rowsAdapter.add(listRow);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error adding home content row: " + homeContent.getTitle(), e);
        }
    }
}
