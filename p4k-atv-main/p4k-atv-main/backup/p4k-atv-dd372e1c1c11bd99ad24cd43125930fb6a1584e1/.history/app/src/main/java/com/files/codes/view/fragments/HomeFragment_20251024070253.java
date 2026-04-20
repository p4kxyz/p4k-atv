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
import android.widget.FrameLayout;
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
import com.files.codes.view.presenter.HeroThumbnailPresenter;
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
    private Presenter cardPresenter;
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
    private HeroBannerPresenter heroBannerPresenter;
    private VideoContent currentHeroVideo; // Track current hero banner video
    private static final int HERO_BANNER_LOAD_MORE = 5;
    private boolean heroThumbnailsSetup = false; // Track if thumbnails grid listener is setup
    private View heroBannerViewReference = null; // Keep reference to hero banner view
    private List<VideoContent> currentFeaturedContent = null; // Current thumbnail list content
    private ArrayObjectAdapter currentThumbnailAdapter = null; // Current thumbnail adapter
    
    // 🚀 Predictive Preloading for Hero Banner Background
    private android.os.Handler preloadHandler = new android.os.Handler();
    private int lastPreloadedPosition = -1;
    private static final int PRELOAD_AHEAD_COUNT = 5; // Preload 5 items ahead in current row
    private static final int PRELOAD_NEXT_ROW_COUNT = 5; // Preload 5 items from next row
    
    // 🎯 Keep strong reference to Picasso Target to prevent GC
    private com.squareup.picasso.Target currentHeroTarget = null;
    
    // Category navigation for Hero Banner
    private List<HomeContent> allCategories = new ArrayList<>(); // All content categories
    private int currentCategoryIndex = 0; // Current category being displayed
    private int currentCategoryPage = 1; // Current page for API-based categories
    private boolean isLoadingMoreForCategory = false; // Track if currently loading more for category
    
    // Section-specific pagination tracking
    private java.util.HashMap<String, Integer> sectionPageMap = new java.util.HashMap<>();
    private java.util.HashMap<String, Boolean> sectionLoadingMap = new java.util.HashMap<>();
    private java.util.HashMap<String, Boolean> sectionHasMoreMap = new java.util.HashMap<>();
    
    // Pending hero banner data (for when view is not ready yet)
    private VideoContent pendingHeroBannerVideo = null;
    private List<VideoContent> pendingHeroBannerContent = null;

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
        // Inflate custom layout with fixed Hero Banner container
        View customView = inflater.inflate(R.layout.fragment_home_custom, container, false);
        
        // Get browse container and add the default fragment view
        FrameLayout browseContainer = customView.findViewById(R.id.browse_container);
        v = super.onCreateView(inflater, browseContainer, savedInstanceState);
        
        if (v != null && browseContainer != null) {
            browseContainer.addView(v);
            // Disable browse container focus (no rows, only Hero Banner)
            browseContainer.setFocusable(false);
            browseContainer.setFocusableInTouchMode(false);
            browseContainer.setVisibility(View.GONE); // Hide completely
        }
        
        // Hide the TitleView from Leanback framework (app name text in top-right)
        // Use postDelayed to hide only after Leanback has initialized SearchOrbView
        if (v != null) {
            v.postDelayed(new Runnable() {
                @Override
                public void run() {
                    hideOnlyTitleText(v);
                    setupLeanbackSearchOrb(v);
                }
            }, 500); // Wait 500ms for Leanback to fully initialize
        }
        
        return customView;
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // DISABLED: Don't hide TitleView - it may affect SearchOrbView animation
        /*
        // Try to hide TitleView again after view is fully created
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideTitleView(view);
                // Also try to hide it from activity root
                if (getActivity() != null) {
                    hideTitleView(getActivity().findViewById(android.R.id.content));
                }
            }
        }, 300); // Wait 300ms for Leanback to initialize
        */
        
        // If there's pending hero banner data, load it now that view is ready
        if (pendingHeroBannerVideo != null && pendingHeroBannerContent != null) {
            Log.d(TAG, "✅ View is now ready - loading pending hero banner");
            loadFixedHeroBanner(pendingHeroBannerVideo, pendingHeroBannerContent);
        }
    }
    
    /**
     * Hide only the TitleView TEXT (app name), keep the container for SearchOrbView animation
     */
    private void hideOnlyTitleText(View view) {
        if (view == null) return;
        
        String className = view.getClass().getName();
        
        // If it's a TextView inside TitleView, hide it
        if (view instanceof android.widget.TextView) {
            View parent = (View) view.getParent();
            if (parent != null && parent.getClass().getName().contains("TitleView")) {
                view.setVisibility(View.GONE);
                Log.d(TAG, "✅ Hidden TitleView TEXT only: " + className);
                return;
            }
        }
        
        // Continue searching recursively
        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            for (int i = 0; i < parent.getChildCount(); i++) {
                hideOnlyTitleText(parent.getChildAt(i));
            }
        }
    }
    
    /**
     * Recursively search for and hide TitleView (showing app name in top-right)
     */
    private void hideTitleView(View view) {
        if (view == null) return;
        
        // Check if this view's class name contains "TitleView" or "BrowseTitle"
        // BUT NOT TitleHelper (needed for SearchOrbView animation) or SearchOrb
        String className = view.getClass().getName();
        if ((className.contains("TitleView") || className.contains("BrowseTitle")) &&
            !className.contains("TitleHelper") && !className.contains("SearchOrb")) {
            view.setVisibility(View.GONE);
            Log.d(TAG, "✅ Hidden TitleView: " + className);
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
            
            // Use HeroThumbnailPresenter for ALL content sections (small thumbnails)
            cardPresenter = new HeroThumbnailPresenter();
            
            // HeroBannerPresenter for main hero banner (detail view)
            heroBannerPresenter = new HeroBannerPresenter(); // Netflix-style Hero Banner

            // Load initial batch of content (lazy loading)
            int itemsToLoad = Math.min(ITEMS_PER_PAGE, list.size());
            Log.d(TAG, "🚀 Loading initial content batch: " + itemsToLoad + " items");
            
            // 🎯 NEW APPROACH: Collect specific categories in order
            allCategories.clear();
            
            // Temporary storage for categories
            HomeContent watchHistoryCategory = null;
            HomeContent koreaCategory = null;
            HomeContent chinaCategory = null;
            HomeContent vietnamCategory = null;
            HomeContent animationCategory = null;
            HomeContent phimLeCategory = null;
            HomeContent phimBoCategory = null;
            HomeContent phim4kCategory = null;

            for (int i = 0; i < list.size(); i++) {
                HomeContent homeContent = list.get(i);

                if (homeContent.getType().equalsIgnoreCase("slider")) {
                    continue;
                } else if (homeContent.getType().equalsIgnoreCase("tv")) {
                    continue;
                } else if (homeContent.getType().equalsIgnoreCase("features_genre_and_movie")) {
                    // 🎬 HERO BANNER - Category 0: "Phim Hay Nha Ní"
                    allHeroBannerContent = new ArrayList<>(homeContent.getContent());
                    
                    if (!allHeroBannerContent.isEmpty()) {
                        VideoContent firstVideo = allHeroBannerContent.get(0);
                        if (firstVideo.getIsTvseries() != null && firstVideo.getIsTvseries().equals("1")) {
                            firstVideo.setType("tvseries");
                        } else {
                            firstVideo.setType("movie");
                        }
                        currentHeroVideo = firstVideo;
                        
                        // Rename title
                        homeContent.setTitle("Phim Hay Nha Ní");
                        allCategories.add(homeContent);
                        
                        loadFixedHeroBanner(firstVideo, allHeroBannerContent);
                        Log.d(TAG, "🎬 Category 0: " + homeContent.getTitle());
                    }
                    continue;
                } else if (homeContent.getType().equalsIgnoreCase("watch_history")) {
                    // Category 1: Watch History
                    homeContent.setTitle("📺 Tiếp Tục Xem");
                    watchHistoryCategory = homeContent;
                    Log.d(TAG, "📂 Watch History: " + homeContent.getTitle());
                } else if (homeContent.getType().equalsIgnoreCase("country")) {
                    // Check Country ID directly
                    String id = homeContent.getId();
                    Log.d(TAG, "🌍 Country ID found: " + id + " - Title: " + homeContent.getTitle());
                    
                    if ("13".equals(id)) {
                        // Hàn Quốc
                        homeContent.setTitle("Drama Xứ Kim Chi");
                        koreaCategory = homeContent;
                        Log.d(TAG, "📂 Korea (ID 13): " + homeContent.getTitle());
                    } else if ("15".equals(id)) {
                        // Trung Quốc
                        homeContent.setTitle("Trường Thiên Drama \"Tàu\"");
                        chinaCategory = homeContent;
                        Log.d(TAG, "📂 China (ID 15): " + homeContent.getTitle());
                    } else if ("5".equals(id)) {
                        // Việt Nam - Updated to "Xưởng phim Xứ Đông Lào"
                        homeContent.setTitle("Xưởng phim Xứ Đông Lào");
                        vietnamCategory = homeContent;
                        Log.d(TAG, "📂 Vietnam (ID 5): " + homeContent.getTitle());
                    }
                } else if (homeContent.getType().equalsIgnoreCase("genre")) {
                    // Check Genre ID for Animation
                    String id = homeContent.getId();
                    Log.d(TAG, "🎭 Genre ID found: " + id + " - Title: " + homeContent.getTitle());
                    
                    if ("13".equals(id)) {
                        // Animation
                        homeContent.setTitle("Xi Nê Tuổi Thơ");
                        animationCategory = homeContent;
                        Log.d(TAG, "📂 Animation (Genre ID 13): " + homeContent.getTitle());
                    }
                } else if (homeContent.getId().equalsIgnoreCase("2")) {
                    // Phim Lẻ (Movie)
                    homeContent.setTitle("Phim Lẻ Mới Nhất");
                    phimLeCategory = homeContent;
                    Log.d(TAG, "📂 Phim Lẻ: " + homeContent.getTitle());
                } else if (homeContent.getType().equalsIgnoreCase("tvseries")) {
                    // Phim Bộ
                    homeContent.setTitle("Phim Bộ Mới Nhất");
                    phimBoCategory = homeContent;
                    Log.d(TAG, "📂 Phim Bộ: " + homeContent.getTitle());
                } else if (homeContent.getType().equalsIgnoreCase("phim4k") || homeContent.getId().equalsIgnoreCase("phim4k_free1")) {
                    // Nguồn Phim Free 1
                    homeContent.setTitle("Nguồn Phim Free 1");
                    phim4kCategory = homeContent;
                    Log.d(TAG, "📂 Phim4k Free1: " + homeContent.getTitle());
                }
            }
            
            // 🔧 Create missing categories if not returned by server
            boolean userLoggedIn = PreferenceUtils.isLoggedIn(getContext());
            
            // Create Watch History if missing and user is logged in
            if (watchHistoryCategory == null && userLoggedIn) {
                watchHistoryCategory = new HomeContent();
                watchHistoryCategory.setId("watch_history");
                watchHistoryCategory.setType("watch_history");
                watchHistoryCategory.setTitle("📺 Tiếp Tục Xem");
                watchHistoryCategory.setContent(new ArrayList<>());
                Log.d(TAG, "🔧 Created Watch History category");
            }
            
            // Create Korea category if missing
            if (koreaCategory == null) {
                koreaCategory = new HomeContent();
                koreaCategory.setId("13");
                koreaCategory.setType("country");
                koreaCategory.setTitle("Drama Xứ Kim Chi");
                koreaCategory.setContent(new ArrayList<>());
                Log.d(TAG, "🔧 Created Korea category (ID 13)");
            }
            
            // Create China category if missing
            if (chinaCategory == null) {
                chinaCategory = new HomeContent();
                chinaCategory.setId("15");
                chinaCategory.setType("country");
                chinaCategory.setTitle("Trường Thiên Drama \"Tàu\"");
                chinaCategory.setContent(new ArrayList<>());
                Log.d(TAG, "🔧 Created China category (ID 15)");
            }
            
            // Create Vietnam category if missing
            if (vietnamCategory == null) {
                vietnamCategory = new HomeContent();
                vietnamCategory.setId("5");
                vietnamCategory.setType("country");
                vietnamCategory.setTitle("Xưởng phim Xứ Đông Lào");
                vietnamCategory.setContent(new ArrayList<>());
                Log.d(TAG, "🔧 Created Vietnam category (ID 5)");
            }
            
            // Create Animation category if missing
            if (animationCategory == null) {
                animationCategory = new HomeContent();
                animationCategory.setId("13");
                animationCategory.setType("genre");
                animationCategory.setTitle("Xi Nê Tuổi Thơ");
                animationCategory.setContent(new ArrayList<>());
                Log.d(TAG, "🔧 Created Animation category (Genre ID 13)");
            }
            
            // Create Phim Le category if missing
            if (phimLeCategory == null) {
                phimLeCategory = new HomeContent();
                phimLeCategory.setId("2");
                phimLeCategory.setType("movie");
                phimLeCategory.setTitle("Phim Lẻ Mới Nhất");
                phimLeCategory.setContent(new ArrayList<>());
                Log.d(TAG, "🔧 Created Phim Le category (ID 2)");
            }
            
            // Phim Bo and Phim4k are already handled above, but ensure they exist
            
            // Add categories in specific order (thứ tự mới từ user)
            // 1. Phim Hay Nha Ní (already added above)
            
            // 2. Tiếp Tục Xem (only if logged in)
            if (watchHistoryCategory != null && userLoggedIn) {
                allCategories.add(watchHistoryCategory);
                Log.d(TAG, "✅ Category 1: Tiếp Tục Xem");
            }
            
            // 3. Drama Xứ Kim Chi
            allCategories.add(koreaCategory);
            Log.d(TAG, "✅ Category 2: Drama Xứ Kim Chi");
            
            // 4. Trường Thiên Drama "Tàu"
            allCategories.add(chinaCategory);
            Log.d(TAG, "✅ Category 3: Trường Thiên Drama \"Tàu\"");
            
            // 5. Xưởng phim Xứ Đông Lào
            allCategories.add(vietnamCategory);
            Log.d(TAG, "✅ Category 4: Xưởng phim Xứ Đông Lào");
            
            // 6. Xi Nê Tuổi Thơ
            allCategories.add(animationCategory);
            Log.d(TAG, "✅ Category 5: Xi Nê Tuổi Thơ");
            
            // 7. Phim Lẻ Mới Nhất
            allCategories.add(phimLeCategory);
            Log.d(TAG, "✅ Category 6: Phim Lẻ Mới Nhất");
            
            // 8. Phim Bộ Mới Nhất
            if (phimBoCategory != null) {
                allCategories.add(phimBoCategory);
                Log.d(TAG, "✅ Category 7: Phim Bộ Mới Nhất");
            }
            
            // 9. Nguồn Phim Free 1
            if (phim4kCategory != null) {
                allCategories.add(phim4kCategory);
                Log.d(TAG, "✅ Category 8: Nguồn Phim Free 1");
            }
            
            Log.d(TAG, "✅ Total categories loaded: " + allCategories.size());
            Log.d(TAG, "🎯 Current category index: " + currentCategoryIndex);
            
            // DON'T add any rows - homepage only shows Hero Banner
            // User navigates categories using UP/DOWN in thumbnail list
            
            // Set empty adapter (no rows, only Hero Banner)
            setAdapter(rowsAdapter);
            getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());

            setCustomPadding();
        } else {
            Intent intent = new Intent(activity, ErrorActivity.class);
            startActivity(intent);
            activity.finish();
        }

    }

    private void setCustomPadding() {
        // NO PADDING needed - homepage only has Hero Banner, no scrollable rows
        // Hero Banner is fixed at top, no content below to scroll
        androidx.leanback.widget.VerticalGridView verticalGridView = getVerticalGridView();
        if (verticalGridView != null) {
            verticalGridView.setPadding(0, 0, 0, 0);
            Log.d(TAG, "✅ No padding - Hero Banner only mode");
        }
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

                // 🎬 UPDATE HERO BANNER THUMBNAIL LIST when row changes
                if (row instanceof ListRow) {
                    ListRow listRow = (ListRow) row;
                    long rowId = listRow.getHeaderItem().getId();
                    
                    // Skip Hero Banner row itself (ID = 0)
                    if (rowId != 0) {
                        // Extract videos from this row and update thumbnail list
                        ArrayObjectAdapter rowAdapter = (ArrayObjectAdapter) listRow.getAdapter();
                        if (rowAdapter != null && rowAdapter.size() > 0) {
                            List<VideoContent> rowVideos = new ArrayList<>();
                            for (int i = 0; i < rowAdapter.size(); i++) {
                                Object rowItem = rowAdapter.get(i);
                                if (rowItem instanceof VideoContent) {
                                    rowVideos.add((VideoContent) rowItem);
                                }
                            }
                            
                            // Update thumbnail list if we have videos
                            if (rowVideos.size() > 0) {
                                updateHeroThumbnailList(rowVideos);
                                Log.d(TAG, "🔄 Updated Hero Banner thumbnail list with " + rowVideos.size() + " videos from row: " + listRow.getHeaderItem().getName());
                            }
                        }
                    }
                }
                
                // 🎬 UPDATE HERO BANNER when thumbnail is focused
                if (item instanceof VideoContent) {
                    VideoContent focusedVideo = (VideoContent) item;
                    
                    // Skip if it's Hero Banner row itself (ID = 0)
                    boolean isHeroBannerRow = (row instanceof ListRow && ((ListRow) row).getHeaderItem().getId() == 0);
                    
                    if (!isHeroBannerRow) {
                        // This is a thumbnail from content sections - update Hero Banner
                        updateHeroBannerWithVideo(focusedVideo);
                    } else {
                        // This is Hero Banner row - check lazy loading
                        checkAndLoadMoreHeroBanner((ListRow) row, item);
                    }
                    
                    // SECTION-SPECIFIC LAZY LOADING
                    checkAndLoadMoreForSection(row, focusedVideo);
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
     * 🎯 Update Hero Banner to display selected video details
     */
    private void updateHeroBannerWithVideo(VideoContent video) {
        if (video == null || rowsAdapter == null || allHeroBannerContent == null) {
            return;
        }
        
        // Skip placeholder items
        if ("placeholder".equals(video.getId()) || "clear_button".equals(video.getType())) {
            return;
        }
        
        // Skip watch history marker items
        if (video.getDescription() != null && video.getDescription().startsWith("WATCH_HISTORY:")) {
            // Still update but clean the description first
            video = copyVideoWithoutHistoryMarker(video);
        }
        
        // Check if this video is already showing in hero banner
        if (currentHeroVideo != null && currentHeroVideo.getId().equals(video.getId())) {
            return; // Already showing this video
        }
        
        currentHeroVideo = video;
        
        // Update Fixed Hero Banner in fixed container
        try {
            if (getView() != null) {
                android.view.ViewGroup heroBannerContainer = getView().findViewById(R.id.hero_banner_fixed_container);
                if (heroBannerContainer != null && heroBannerContainer.getChildCount() > 0) {
                    View heroBannerView = heroBannerContainer.getChildAt(0);
                    updateFixedHeroBanner(heroBannerView, video, allHeroBannerContent);
                    Log.d(TAG, "🎯 Fixed Hero Banner updated to: " + video.getTitle());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error updating Fixed Hero Banner", e);
        }
    }
    
    /**
     * Create a copy of video without watch history marker in description
     */
    private VideoContent copyVideoWithoutHistoryMarker(VideoContent original) {
        VideoContent copy = new VideoContent();
        copy.setId(original.getId());
        copy.setVideosId(original.getVideosId());
        copy.setTitle(original.getTitle());
        copy.setThumbnailUrl(original.getThumbnailUrl());
        copy.setPosterUrl(original.getPosterUrl());
        copy.setType(original.getType());
        copy.setIsTvseries(original.getIsTvseries());
        copy.setRelease(original.getRelease());
        copy.setRuntime(original.getRuntime());
        copy.setVideoQuality(original.getVideoQuality());
        copy.setImdbRating(original.getImdbRating());
        copy.setIsPaid(original.getIsPaid());
        copy.setVideoUrl(original.getVideoUrl());
        
        // Don't copy description with watch history marker
        // Use empty string or fetch from API if needed
        copy.setDescription("");
        
        return copy;
    }
    
    /**
     * 🎬 REMOVED: Hero Banner lazy loading (not needed - shows 1 video at a time, updated on focus)
     */
    private void checkAndLoadMoreHeroBanner(ListRow heroBannerRow, Object selectedItem) {
        // Hero Banner no longer needs lazy loading
        // It displays 1 video at a time, updated when thumbnail is focused
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
        videoContent.setImdbRating(movie.getImdbRating()); // Add IMDb rating
        videoContent.setGenre(movie.getGenre()); // Add genre for tags
        
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
                            
                            // ✅ FIXED: Use posterUrl for background (large image), thumbnailUrl for thumbnail (small image)
                            String thumbnailUrl = historyItem.getThumbnailUrl();
                            String posterUrl = historyItem.getPosterUrl();
                            
                            if (posterUrl != null && !posterUrl.isEmpty()) {
                                // Use posterUrl for background/poster (large image)
                                videoContent.setPosterUrl(posterUrl);
                            } else if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                                // Fallback to thumbnail if no poster
                                videoContent.setPosterUrl(thumbnailUrl);
                            } else {
                                // No poster available
                                videoContent.setPosterUrl("");
                            }
                            
                            if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                                // Use thumbnailUrl for small card display
                                videoContent.setThumbnailUrl(thumbnailUrl);
                            } else if (posterUrl != null && !posterUrl.isEmpty()) {
                                // Fallback to poster if no thumbnail
                                videoContent.setThumbnailUrl(posterUrl);
                            } else {
                                // No thumbnail available
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
    
    /**
     * 🎬 Load Fixed Hero Banner into fixed container (part of Fragment, not Activity)
     */
    private void loadFixedHeroBanner(VideoContent initialVideo, List<VideoContent> featuredContent) {
        // Store data for later if view is not ready yet
        if (getView() == null) {
            Log.w(TAG, "⚠️ Fragment view is null - will retry when view is ready");
            // Store data to retry later
            pendingHeroBannerVideo = initialVideo;
            pendingHeroBannerContent = featuredContent;
            return;
        }
        
        Log.d(TAG, "🔍 loadFixedHeroBanner called with video: " + (initialVideo != null ? initialVideo.getTitle() : "null"));
        
        // Clear pending data since we're loading now
        pendingHeroBannerVideo = null;
        pendingHeroBannerContent = null;
        
        // Post to UI thread to ensure Fragment view is fully inflated
        getView().post(() -> {
            try {
                // Get hero banner container from Fragment's custom layout
                android.view.ViewGroup heroBannerContainer = getView().findViewById(R.id.hero_banner_fixed_container);
                
                if (heroBannerContainer == null) {
                    Log.e(TAG, "❌ Hero banner fixed container not found in Fragment!");
                    return;
                }
                
                Log.d(TAG, "✅ Hero banner container found in Fragment");
                
                // Make sure container is visible
                heroBannerContainer.setVisibility(View.VISIBLE);
                
                // Inflate hero banner layout
                android.view.LayoutInflater inflater = android.view.LayoutInflater.from(getActivity());
                View heroBannerView = inflater.inflate(R.layout.item_hero_banner, heroBannerContainer, false);
                
                Log.d(TAG, "✅ Hero banner view inflated");
                
                // Category switcher disabled - using standard FrameLayout like backup
                // if (heroBannerView instanceof com.files.codes.view.widget.CategorySwitcherFrameLayout) {
                //     ((com.files.codes.view.widget.CategorySwitcherFrameLayout) heroBannerView).setCategorySwitchListener(
                //         new com.files.codes.view.widget.CategorySwitcherFrameLayout.OnCategorySwitchListener() {
                //             @Override
                //             public void onSwitchNext() {
                //                 switchToNextCategory();
                //             }
                //             
                //             @Override
                //             public void onSwitchPrevious() {
                //                 switchToPreviousCategory();
                //             }
                //         }
                //     );
                //     Log.d(TAG, "✅ Category switch listener set");
                // }
                
                // Clear container and add hero banner view
                heroBannerContainer.removeAllViews();
                heroBannerContainer.addView(heroBannerView);
                
                Log.d(TAG, "✅ Hero banner view added to container");
                
                // Initialize hero banner with first video
                updateFixedHeroBanner(heroBannerView, initialVideo, featuredContent);
                
                Log.d(TAG, "✅ Fixed Hero Banner loaded successfully with title: " + initialVideo.getTitle());
            } catch (Exception e) {
                Log.e(TAG, "❌ Error loading fixed hero banner", e);
                e.printStackTrace();
            }
        });
    }
    
    /**
     * 🎯 Update Fixed Hero Banner with selected video
     */
    private void updateFixedHeroBanner(View heroBannerView, VideoContent video, List<VideoContent> featuredContent) {
        if (heroBannerView == null || video == null) return;
        
        try {
            // Find views
            android.widget.ImageView heroBackground = heroBannerView.findViewById(R.id.hero_background);
            
            // Fix ImageView height to exactly match screen height (1080px)
            if (heroBackground != null) {
                android.util.DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                int screenHeight = displayMetrics.heightPixels;
                android.view.ViewGroup.LayoutParams params = heroBackground.getLayoutParams();
                params.height = screenHeight;
                heroBackground.setLayoutParams(params);
            }
            
            android.widget.TextView heroTitle = heroBannerView.findViewById(R.id.hero_title);
            android.widget.TextView heroTitleLine2 = heroBannerView.findViewById(R.id.hero_title_line2);
            android.widget.TextView heroDescription = heroBannerView.findViewById(R.id.hero_description);
            android.widget.TextView heroImdbRating = heroBannerView.findViewById(R.id.hero_imdb_rating);
            android.widget.TextView heroYear = heroBannerView.findViewById(R.id.hero_year);
            android.widget.TextView heroQuality = heroBannerView.findViewById(R.id.hero_quality);
            android.widget.LinearLayout heroImdbContainer = heroBannerView.findViewById(R.id.hero_imdb_container);
            android.widget.LinearLayout heroQualityContainer = heroBannerView.findViewById(R.id.hero_quality_container);
            android.widget.Button playButton = heroBannerView.findViewById(R.id.hero_play_button);
            android.widget.Button favoriteButton = heroBannerView.findViewById(R.id.hero_favorite_button);
            com.files.codes.view.widget.CategorySwitchingHorizontalGridView thumbnailsGrid = heroBannerView.findViewById(R.id.hero_thumbnails_grid);
            
            // Load background
            String imageUrl = video.getPosterUrl();
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                imageUrl = video.getThumbnailUrl();
            }
            loadBackgroundImage(imageUrl, heroBackground);
            
            // Set title
            if (video.getTitle() != null) {
                String rawTitle = video.getTitle();
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(.+?)\\s*\\((\\d{4})\\)\\s*(.+)$");
                java.util.regex.Matcher matcher = pattern.matcher(rawTitle);
                
                if (matcher.find()) {
                    heroTitle.setText(matcher.group(1).trim().toUpperCase());
                    String englishTitle = matcher.group(3).trim();
                    if (!englishTitle.isEmpty()) {
                        heroTitleLine2.setText(addLetterSpacing(englishTitle.toUpperCase()));
                        heroTitleLine2.setVisibility(View.VISIBLE);
                    } else {
                        heroTitleLine2.setVisibility(View.GONE);
                    }
                } else {
                    heroTitle.setText(rawTitle.toUpperCase());
                    heroTitleLine2.setVisibility(View.GONE);
                }
            }
            
            // Set description
            if (video.getDescription() != null && !video.getDescription().isEmpty()) {
                String description = video.getDescription().trim();
                if (description.length() > 120) {
                    description = description.substring(0, 120);
                    int lastSpace = description.lastIndexOf(' ');
                    if (lastSpace > 80) {
                        description = description.substring(0, lastSpace);
                    }
                    description += "...";
                }
                heroDescription.setText(description);
            }
            
            // Set IMDb
            if (video.getImdbRating() != null && !video.getImdbRating().isEmpty()) {
                try {
                    double rating = Double.parseDouble(video.getImdbRating());
                    heroImdbRating.setText(String.format("%.1f", rating));
                    heroImdbContainer.setVisibility(View.VISIBLE);
                } catch (NumberFormatException e) {
                    heroImdbContainer.setVisibility(View.GONE);
                }
            } else {
                heroImdbContainer.setVisibility(View.GONE);
            }
            
            // Set year
            if (video.getRelease() != null && !video.getRelease().isEmpty()) {
                heroYear.setText(video.getRelease());
            }
            
            // Set quality
            if (video.getVideoQuality() != null && !video.getVideoQuality().isEmpty()) {
                String quality = video.getVideoQuality().trim();
                heroQuality.setText(quality);
                
                int backgroundRes;
                if (quality.equalsIgnoreCase("4K") || quality.equalsIgnoreCase("UHD")) {
                    backgroundRes = R.drawable.metadata_quality_4k;
                } else if (quality.equalsIgnoreCase("Full HD") || quality.equalsIgnoreCase("FHD") || quality.equalsIgnoreCase("FullHD")) {
                    backgroundRes = R.drawable.metadata_quality_fullhd;
                } else if (quality.equalsIgnoreCase("HD") || quality.equalsIgnoreCase("1080p")) {
                    backgroundRes = R.drawable.metadata_quality_hd;
                } else {
                    backgroundRes = R.drawable.metadata_quality_modern;
                }
                heroQualityContainer.setBackgroundResource(backgroundRes);
                heroQualityContainer.setVisibility(View.VISIBLE);
            } else {
                heroQualityContainer.setVisibility(View.GONE);
            }
            
            // Setup buttons
            final VideoContent currentVideo = video;
            playButton.setOnClickListener(v -> {
                if (currentHeroVideo == null) return;
                
                // Check if this is from Watch History based on current category
                boolean isFromWatchHistory = false;
                if (allCategories != null && currentCategoryIndex >= 0 && currentCategoryIndex < allCategories.size()) {
                    HomeContent currentCategory = allCategories.get(currentCategoryIndex);
                    isFromWatchHistory = "watch_history".equalsIgnoreCase(currentCategory.getType());
                    Log.d(TAG, "🎬 Play button - Current category: " + currentCategory.getTitle() + ", isWatchHistory: " + isFromWatchHistory);
                }
                
                // Use the same logic as thumbnail click with current hero video
                handleThumbnailClick(currentHeroVideo, isFromWatchHistory);
            });
            favoriteButton.setOnClickListener(v -> {
                // Implement favorite toggle
            });
            
            // Setup thumbnails grid with focus listener (only once)
            if (thumbnailsGrid != null && featuredContent != null && featuredContent.size() > 0 && !heroThumbnailsSetup) {
                // Store hero banner view reference
                heroBannerViewReference = heroBannerView;
                
                // Store initial featured content
                currentFeaturedContent = featuredContent;
                
                // Post to ensure view is fully laid out
                thumbnailsGrid.post(() -> {
                    try {
                        // Set single row for horizontal scrolling
                        thumbnailsGrid.setNumRows(1);
                        // NO setRowHeight - let it use wrap_content from layout
                        // This allows FrameLayout to maintain its 122x182dp size
                        thumbnailsGrid.setHorizontalSpacing(12);
                        
                        // Enable focus and navigation
                        thumbnailsGrid.setFocusable(true);
                        thumbnailsGrid.setFocusableInTouchMode(true);
                        thumbnailsGrid.setItemAlignmentOffset(0);
                        thumbnailsGrid.setItemAlignmentOffsetPercent(androidx.leanback.widget.HorizontalGridView.ITEM_ALIGN_OFFSET_PERCENT_DISABLED);
                        thumbnailsGrid.setWindowAlignmentOffset(0);
                        thumbnailsGrid.setWindowAlignmentOffsetPercent(20);
                        
                        // 🎯 Allow focus to escape LEFT/RIGHT at edges (don't trap focus)
                        thumbnailsGrid.setWindowAlignment(androidx.leanback.widget.BaseGridView.WINDOW_ALIGN_BOTH_EDGE);
                        thumbnailsGrid.setWindowAlignmentOffsetPercent(androidx.leanback.widget.BaseGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED);
                        
                        // Disable vertical scrolling to allow focus to escape
                        thumbnailsGrid.setFocusScrollStrategy(androidx.leanback.widget.BaseGridView.FOCUS_SCROLL_ITEM);
                        
                        // IMPORTANT: Don't use setOnKeyListener - it doesn't work with Leanback
                        // Instead, we'll handle category switching via a wrapper view
                        
                        // Create presenter with click listener
                        com.files.codes.view.presenter.HeroThumbnailPresenter thumbnailPresenter = 
                            new com.files.codes.view.presenter.HeroThumbnailPresenter();
                        
                        // Set click listener on presenter - check current category dynamically
                        thumbnailPresenter.setOnThumbnailClickListener(new com.files.codes.view.presenter.HeroThumbnailPresenter.OnThumbnailClickListener() {
                            @Override
                            public void onThumbnailClicked(VideoContent video, boolean isFromWatchHistory) {
                                // Always check current category when clicked, not when setup
                                boolean actuallyFromWatchHistory = false;
                                if (allCategories != null && currentCategoryIndex >= 0 && currentCategoryIndex < allCategories.size()) {
                                    HomeContent currentCategory = allCategories.get(currentCategoryIndex);
                                    actuallyFromWatchHistory = "watch_history".equalsIgnoreCase(currentCategory.getType());
                                    Log.d(TAG, "🎬 Thumbnail clicked - Current category: " + currentCategory.getTitle() + ", isWatchHistory: " + actuallyFromWatchHistory);
                                }
                                handleThumbnailClick(video, actuallyFromWatchHistory);
                            }
                        });
                        
                        // Set watch history flag if current category is watch history (for display purposes only)
                        boolean isWatchHistory = false;
                        if (allCategories != null && currentCategoryIndex >= 0 && currentCategoryIndex < allCategories.size()) {
                            HomeContent currentCategory = allCategories.get(currentCategoryIndex);
                            isWatchHistory = "watch_history".equalsIgnoreCase(currentCategory.getType());
                        }
                        thumbnailPresenter.setWatchHistoryCategory(isWatchHistory);
                        
                        // Lazy loading: Load initial 10 items
                        currentThumbnailAdapter = new ArrayObjectAdapter(thumbnailPresenter);
                        int initialLoadCount = Math.min(10, currentFeaturedContent.size());
                        for (int i = 0; i < initialLoadCount; i++) {
                            currentThumbnailAdapter.add(currentFeaturedContent.get(i));
                        }
                        Log.d(TAG, "🎬 Lazy loading: Initial " + initialLoadCount + " of " + currentFeaturedContent.size() + " thumbnails");
                        
                        androidx.leanback.widget.ItemBridgeAdapter bridgeAdapter = 
                            new androidx.leanback.widget.ItemBridgeAdapter(currentThumbnailAdapter);
                        thumbnailsGrid.setAdapter(bridgeAdapter);
                        
                        // Add focus listener to update Hero Banner when thumbnail is focused
                        thumbnailsGrid.setOnChildViewHolderSelectedListener(new androidx.leanback.widget.OnChildViewHolderSelectedListener() {
                            @Override
                            public void onChildViewHolderSelected(androidx.recyclerview.widget.RecyclerView parent, 
                                                                 androidx.recyclerview.widget.RecyclerView.ViewHolder child, 
                                                                 int position, int subposition) {
                                Log.d(TAG, "📍 Thumbnail grid focus changed - Position: " + position);
                                
                                if (currentFeaturedContent != null && position >= 0 && position < currentFeaturedContent.size()) {
                                    VideoContent selectedVideo = currentFeaturedContent.get(position);
                                    Log.d(TAG, "🎯 Thumbnail focused: " + selectedVideo.getTitle());
                                    
                                    // Update Hero Banner directly using stored reference
                                    if (heroBannerViewReference != null) {
                                        currentHeroVideo = selectedVideo;
                                        updateHeroBannerContent(heroBannerViewReference, selectedVideo);
                                    }
                                    
                                    // 🚀 Trigger predictive preloading of background images
                                    predictivePreloadBackgrounds(position);
                                    
                                    // Lazy loading: Load more items when approaching end
                                    int currentAdapterSize = currentThumbnailAdapter.size();
                                    if (position >= currentAdapterSize - 3 && currentAdapterSize < currentFeaturedContent.size()) {
                                        int loadMoreCount = Math.min(5, currentFeaturedContent.size() - currentAdapterSize);
                                        Log.d(TAG, "📦 Loading " + loadMoreCount + " more thumbnails from memory (total will be: " + (currentAdapterSize + loadMoreCount) + ")");
                                        
                                        // Post to avoid "Cannot call this method while RecyclerView is computing a layout or scrolling"
                                        final int startIndex = currentAdapterSize;
                                        final int count = loadMoreCount;
                                        parent.post(() -> {
                                            for (int i = 0; i < count; i++) {
                                                currentThumbnailAdapter.add(currentFeaturedContent.get(startIndex + i));
                                            }
                                        });
                                    } 
                                    // Check if we need to load next page from API (for country/genre categories)
                                    else if (position >= currentAdapterSize - 3 && currentAdapterSize >= currentFeaturedContent.size()) {
                                        // We've reached the end of loaded content, try to load next page
                                        if (!isLoadingMoreForCategory && allCategories != null && currentCategoryIndex < allCategories.size()) {
                                            HomeContent currentCategory = allCategories.get(currentCategoryIndex);
                                            String categoryType = currentCategory.getType().toLowerCase();
                                            
                                            if ("country".equals(categoryType)) {
                                                Log.d(TAG, "🌍 Loading next page for country category (page " + (currentCategoryPage + 1) + ")");
                                                loadCountryContentForHeroBanner(currentCategory, currentCategoryPage + 1, true);
                                            } else if ("genre".equals(categoryType)) {
                                                Log.d(TAG, "🎭 Loading next page for genre category (page " + (currentCategoryPage + 1) + ")");
                                                loadGenreContentForHeroBanner(currentCategory, currentCategoryPage + 1, true);
                                            }
                                        }
                                    }
                                }
                            }
                        });
                        
                        // Set category switch listener for DOWN/UP navigation
                        thumbnailsGrid.setCategorySwitchListener(new com.files.codes.view.widget.CategorySwitchingHorizontalGridView.OnCategorySwitchListener() {
                            @Override
                            public boolean onSwitchNext() {
                                Log.d(TAG, "⬇️ DOWN key intercepted - switching to next category");
                                switchToNextCategory();
                                return true; // Always consume DOWN key
                            }
                            
                            @Override
                            public boolean onSwitchPrevious() {
                                Log.d(TAG, "⬆️ UP key intercepted - checking if can switch to previous category");
                                return switchToPreviousCategory(); // Return false if at first category to allow focus escape
                            }
                        });
                        
                        // Mark as setup complete
                        heroThumbnailsSetup = true;
                        
                        // Auto focus on thumbnail grid (since no rows below)
                        thumbnailsGrid.postDelayed(() -> {
                            boolean focused = thumbnailsGrid.requestFocus();
                            thumbnailsGrid.setSelectedPosition(0);
                            Log.d(TAG, "🎯 Auto-focused on thumbnail grid - Success: " + focused);
                            
                            // Force focus if first attempt failed
                            if (!focused) {
                                thumbnailsGrid.postDelayed(() -> {
                                    thumbnailsGrid.requestFocus();
                                    Log.d(TAG, "🔄 Retry focus on thumbnail grid");
                                }, 200);
                            }
                        }, 500);
                        
                        Log.d(TAG, "✅ Thumbnails grid setup with " + currentFeaturedContent.size() + " items and focus listener");
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error setting up thumbnails grid", e);
                    }
                });
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error updating fixed hero banner", e);
        }
    }
    
    private String addLetterSpacing(String text) {
        if (text == null || text.trim().isEmpty()) return text;
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            result.append(text.charAt(i));
            if (i < text.length() - 1 && text.charAt(i) != ' ') {
                result.append(' ');
            }
        }
        return result.toString();
    }
    
    /**
     * 🖼️ Load background image with Cloudflare Image Resizing optimization
     */
    private void loadBackgroundImage(String url, android.widget.ImageView heroBackground) {
        if (url == null || url.trim().isEmpty()) {
            // Use default placeholder
            heroBackground.setImageResource(R.drawable.logo);
            return;
        }
        
        // Get screen width for optimal image sizing
        android.util.DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        
        // Calculate 16:9 aspect ratio height based on screen width
        // This ensures Cloudflare returns image with correct aspect ratio
        int targetWidth = screenWidth;
        int targetHeight = (int) (screenWidth / 16.0 * 9.0); // 16:9 ratio
        
        Log.d(TAG, "📐 Screen dimensions: " + screenWidth + "x" + displayMetrics.heightPixels + " px");
        Log.d(TAG, "📐 Target 16:9 size: " + targetWidth + "x" + targetHeight + " px");
        
        // Use Cloudflare Image Resizing with fit=cover to fill the banner
        String optimizedUrl = url;
        try {
            java.net.URL urlObj = new java.net.URL(url);
            String domain = urlObj.getProtocol() + "://" + urlObj.getHost();
            String path = urlObj.getPath() + (urlObj.getQuery() != null ? "?" + urlObj.getQuery() : "");
            
            // Cloudflare resize: maintain 16:9 aspect ratio, fit=cover to fill
            // This resizes and crops the image to fill the entire banner
            optimizedUrl = domain + "/cdn-cgi/image/width=" + targetWidth 
                         + ",height=" + targetHeight
                         + ",fit=cover,quality=90,format=auto" + path;
            
            Log.d(TAG, "🌐 ========== CLOUDFLARE URL ==========");
            Log.d(TAG, "🌐 Original: " + url);
            Log.d(TAG, "🌐 Cloudflare: " + optimizedUrl);
            Log.d(TAG, "🌐 Size: " + targetWidth + "x" + targetHeight + " px (fit=cover)");
            Log.d(TAG, "🌐 =====================================");
        } catch (Exception e) {
            optimizedUrl = url;
            Log.d(TAG, "⚠️ Using original URL: " + url);
        }
        
        // � Create Target and keep strong reference to prevent GC
        final String finalOptimizedUrl = optimizedUrl;
        currentHeroTarget = new com.squareup.picasso.Target() {
            @Override
            public void onBitmapLoaded(android.graphics.Bitmap bitmap, com.squareup.picasso.Picasso.LoadedFrom from) {
                Log.d(TAG, "🎨 ========== BITMAP LOADED (CURRENT APP) ==========");
                Log.d(TAG, "🎨 URL: " + finalOptimizedUrl);
                Log.d(TAG, "🎨 Bitmap size: " + bitmap.getWidth() + "x" + bitmap.getHeight() + " px");
                Log.d(TAG, "🎨 ImageView scaleType: " + heroBackground.getScaleType());
                Log.d(TAG, "🎨 ImageView size: " + heroBackground.getWidth() + "x" + heroBackground.getHeight() + " px");
                Log.d(TAG, "🎨 AdjustViewBounds: " + heroBackground.getAdjustViewBounds());
                Log.d(TAG, "🎨 ===================================================");
                
                // Set image directly (no animation)
                heroBackground.setImageBitmap(bitmap);
            }
            
            @Override
            public void onBitmapFailed(Exception e, android.graphics.drawable.Drawable errorDrawable) {
                Log.e(TAG, "❌ Bitmap load failed: " + e.getMessage());
                heroBackground.setImageDrawable(errorDrawable);
            }
            
            @Override
            public void onPrepareLoad(android.graphics.drawable.Drawable placeHolderDrawable) {
                heroBackground.setImageDrawable(placeHolderDrawable);
            }
        };
        
        // Load image into Target (strong reference prevents GC)
        com.squareup.picasso.Picasso.get()
            .load(optimizedUrl)
            .placeholder(R.drawable.logo)
            .error(R.drawable.logo)
            .into(currentHeroTarget);
    }
    
    /**
     * 🚀 Predictive Preloading: Load background images ahead of current position
     * Preloads images for smoother scrolling experience
     */
    private void predictivePreloadBackgrounds(int currentPosition) {
        if (currentFeaturedContent == null || currentFeaturedContent.isEmpty()) {
            return;
        }
        
        // Only preload every 3 positions to avoid excessive preloading
        if (currentPosition % 3 != 0 && lastPreloadedPosition >= 0) {
            return;
        }
        
        // Don't preload if we're too close to last preload position
        if (Math.abs(currentPosition - lastPreloadedPosition) < 3) {
            return;
        }
        
        lastPreloadedPosition = currentPosition;
        Log.d(TAG, "🚀 Predictive preload triggered at position: " + currentPosition);
        
        // Calculate preload range
        // Current row: preload 5 items ahead
        int currentRowEnd = Math.min(currentPosition + PRELOAD_AHEAD_COUNT, currentFeaturedContent.size());
        
        // Next row: assuming 6 items per row in grid, preload 5 items from next row
        int nextRowStart = currentPosition + 6;
        int nextRowEnd = Math.min(nextRowStart + PRELOAD_NEXT_ROW_COUNT, currentFeaturedContent.size());
        
        // Preload current row images
        preloadHandler.post(() -> {
            for (int i = currentPosition; i < currentRowEnd; i++) {
                if (i < currentFeaturedContent.size()) {
                    VideoContent video = currentFeaturedContent.get(i);
                    String imageUrl = getOptimizedImageUrl(video);
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        // Use Picasso.fetch() to preload into cache without displaying
                        com.squareup.picasso.Picasso.get().load(imageUrl).fetch();
                    }
                }
            }
            Log.d(TAG, "📥 Preloaded current row backgrounds: " + currentPosition + " to " + (currentRowEnd - 1));
        });
        
        // Preload next row images after a short delay
        preloadHandler.postDelayed(() -> {
            for (int i = nextRowStart; i < nextRowEnd; i++) {
                if (i < currentFeaturedContent.size()) {
                    VideoContent video = currentFeaturedContent.get(i);
                    String imageUrl = getOptimizedImageUrl(video);
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        com.squareup.picasso.Picasso.get().load(imageUrl).fetch();
                    }
                }
            }
            Log.d(TAG, "📥 Preloaded next row backgrounds: " + nextRowStart + " to " + (nextRowEnd - 1));
        }, 200); // 200ms delay for next row
    }
    
    /**
     * 🌐 Get optimized Cloudflare CDN image URL
     */
    private String getOptimizedImageUrl(VideoContent video) {
        if (video == null) {
            return null;
        }
        
        String imageUrl = video.getPosterUrl();
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            imageUrl = video.getThumbnailUrl();
        }
        
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }
        
        // Get screen dimensions
        android.util.DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int targetHeight = (int) (screenWidth / 16.0 * 9.0); // 16:9 ratio
        
        // Apply Cloudflare optimization
        try {
            java.net.URL urlObj = new java.net.URL(imageUrl);
            String domain = urlObj.getProtocol() + "://" + urlObj.getHost();
            String path = urlObj.getPath() + (urlObj.getQuery() != null ? "?" + urlObj.getQuery() : "");
            
            return domain + "/cdn-cgi/image/width=" + screenWidth 
                         + ",height=" + targetHeight
                         + ",fit=cover,quality=90,format=auto" + path;
        } catch (Exception e) {
            return imageUrl;
        }
    }
    
    /**
     * 🎨 Update Hero Banner content (background, title, description) only
     * Used by thumbnail grid focus listener for fast updates
     */
    private void updateHeroBannerContent(View heroBannerView, VideoContent video) {
        if (heroBannerView == null || video == null) return;
        
        try {
            // Find views
            android.widget.ImageView heroBackground = heroBannerView.findViewById(R.id.hero_background);
            android.widget.TextView heroTitle = heroBannerView.findViewById(R.id.hero_title);
            android.widget.TextView heroTitleLine2 = heroBannerView.findViewById(R.id.hero_title_line2);
            android.widget.TextView heroDescription = heroBannerView.findViewById(R.id.hero_description);
            android.widget.TextView heroImdbRating = heroBannerView.findViewById(R.id.hero_imdb_rating);
            android.widget.TextView heroYear = heroBannerView.findViewById(R.id.hero_year);
            android.widget.TextView heroQuality = heroBannerView.findViewById(R.id.hero_quality);
            android.widget.LinearLayout heroImdbContainer = heroBannerView.findViewById(R.id.hero_imdb_container);
            android.widget.LinearLayout heroQualityContainer = heroBannerView.findViewById(R.id.hero_quality_container);
            
            // Update background (no animation)
            String imageUrl = video.getPosterUrl();
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                imageUrl = video.getThumbnailUrl();
            }
            loadBackgroundImage(imageUrl, heroBackground);
            
            // Update text content directly (no animation)
            updateTextContent(heroTitle, heroTitleLine2, heroDescription, heroImdbRating, 
                            heroYear, heroQuality, heroImdbContainer, heroQualityContainer, video);
            
            Log.d(TAG, "✅ Hero Banner content updated: " + video.getTitle());
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error updating hero banner content", e);
        }
    }
    
    /**
     * 📝 Helper method to update text content
     */
    private void updateTextContent(android.widget.TextView heroTitle, android.widget.TextView heroTitleLine2,
                                   android.widget.TextView heroDescription, android.widget.TextView heroImdbRating,
                                   android.widget.TextView heroYear, android.widget.TextView heroQuality,
                                   android.widget.LinearLayout heroImdbContainer, android.widget.LinearLayout heroQualityContainer,
                                   VideoContent video) {
        // Update title
        if (video.getTitle() != null) {
            String rawTitle = video.getTitle();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(.+?)\\s*\\((\\d{4})\\)\\s*(.+)$");
            java.util.regex.Matcher matcher = pattern.matcher(rawTitle);
            
            if (matcher.find()) {
                heroTitle.setText(matcher.group(1).trim().toUpperCase());
                String englishTitle = matcher.group(3).trim();
                if (!englishTitle.isEmpty()) {
                    heroTitleLine2.setText(addLetterSpacing(englishTitle.toUpperCase()));
                    heroTitleLine2.setVisibility(View.VISIBLE);
                } else {
                    heroTitleLine2.setVisibility(View.GONE);
                }
            } else {
                heroTitle.setText(rawTitle.toUpperCase());
                heroTitleLine2.setVisibility(View.GONE);
            }
        }
        
        // Update description
        String description = video.getDescription();
        if (description != null && !description.isEmpty()) {
            // Check if this is from Watch History
            if (description.startsWith("WATCH_HISTORY:")) {
                // Parse watch position
                try {
                    String positionStr = description.substring("WATCH_HISTORY:".length());
                    long positionMs = Long.parseLong(positionStr);
                    
                    // Format time watched (convert ms to readable format)
                    long seconds = positionMs / 1000;
                    long minutes = seconds / 60;
                    long hours = minutes / 60;
                    
                    String timeWatched;
                    if (hours > 0) {
                        timeWatched = String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60);
                    } else {
                        timeWatched = String.format("%d:%02d", minutes, seconds % 60);
                    }
                    
                    // Show watch time info
                    String watchInfo = "⏱️ Đã xem: " + timeWatched;
                    
                    // Add video quality/file info if available
                    if (video.getVideoQuality() != null && !video.getVideoQuality().isEmpty()) {
                        watchInfo += " | 📹 " + video.getVideoQuality();
                    }
                    
                    heroDescription.setText(watchInfo);
                    heroDescription.setVisibility(View.VISIBLE);
                } catch (NumberFormatException e) {
                    // If parsing fails, hide description
                    heroDescription.setVisibility(View.GONE);
                }
            } else {
                // Regular description
                heroDescription.setText(description);
                heroDescription.setVisibility(View.VISIBLE);
            }
        } else {
            heroDescription.setVisibility(View.GONE);
        }
        
        // Update IMDb rating
        if (video.getImdbRating() != null && !video.getImdbRating().isEmpty()) {
            heroImdbRating.setText(video.getImdbRating());
            heroImdbContainer.setVisibility(View.VISIBLE);
        } else {
            heroImdbContainer.setVisibility(View.GONE);
        }
        
        // Update year
        if (video.getRelease() != null && !video.getRelease().isEmpty()) {
            heroYear.setText(video.getRelease());
            heroYear.setVisibility(View.VISIBLE);
        } else {
            heroYear.setVisibility(View.GONE);
        }
        
        // Update quality
        if (video.getVideoQuality() != null && !video.getVideoQuality().isEmpty()) {
            String quality = video.getVideoQuality();
            heroQuality.setText(quality);
            
            int backgroundRes = R.drawable.metadata_quality_modern; // Default
            if (quality.equalsIgnoreCase("4K") || quality.equalsIgnoreCase("UHD")) {
                backgroundRes = R.drawable.metadata_quality_4k;
            } else if (quality.equalsIgnoreCase("Full HD") || quality.equalsIgnoreCase("FHD") || quality.equalsIgnoreCase("FullHD")) {
                backgroundRes = R.drawable.metadata_quality_fullhd;
            } else if (quality.equalsIgnoreCase("HD") || quality.equalsIgnoreCase("1080p")) {
                backgroundRes = R.drawable.metadata_quality_hd;
            }
            heroQualityContainer.setBackgroundResource(backgroundRes);
            heroQualityContainer.setVisibility(View.VISIBLE);
        } else {
            heroQualityContainer.setVisibility(View.GONE);
        }
    }
    
    /**
     * �🎯 Update Hero Banner Thumbnail List
     * Updates the thumbnail grid with new content from the focused row
     */
    private void updateHeroThumbnailList(List<VideoContent> newContent) {
        if (heroBannerViewReference == null || newContent == null || newContent.isEmpty()) {
            Log.w(TAG, "⚠️ Cannot update thumbnail list - missing view reference or content");
            return;
        }
        
        try {
            androidx.leanback.widget.HorizontalGridView thumbnailsGrid = 
                heroBannerViewReference.findViewById(R.id.hero_thumbnails_grid);
            
            if (thumbnailsGrid == null) {
                Log.e(TAG, "❌ Thumbnails grid not found");
                return;
            }
            
            // Update current featured content reference
            currentFeaturedContent = newContent;
            
            // 🚀 Reset predictive preload position when switching content
            lastPreloadedPosition = -1;
            
            // Add fade out animation before updating
            android.view.animation.Animation fadeOut = android.view.animation.AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);
            fadeOut.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
                @Override
                public void onAnimationStart(android.view.animation.Animation animation) {
                }
                
                @Override
                public void onAnimationEnd(android.view.animation.Animation animation) {
                    // After fade out, update content and fade in
                    updateThumbnailsContent(thumbnailsGrid, newContent);
                }
                
                @Override
                public void onAnimationRepeat(android.view.animation.Animation animation) {
                }
            });
            
            thumbnailsGrid.startAnimation(fadeOut);
            
            Log.d(TAG, "✅ Thumbnail list updating with animation - " + newContent.size() + " items");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error updating thumbnail list", e);
        }
    }
    
    /**
     * 🎨 Update thumbnails content with fade in animation
     */
    private void updateThumbnailsContent(androidx.leanback.widget.HorizontalGridView thumbnailsGrid, List<VideoContent> newContent) {
        try {
            // Create new adapter with click listener
            com.files.codes.view.presenter.HeroThumbnailPresenter thumbnailPresenter = 
                new com.files.codes.view.presenter.HeroThumbnailPresenter();
            
            // Set click listener
            thumbnailPresenter.setOnThumbnailClickListener(new com.files.codes.view.presenter.HeroThumbnailPresenter.OnThumbnailClickListener() {
                @Override
                public void onThumbnailClicked(VideoContent video, boolean isFromWatchHistory) {
                    handleThumbnailClick(video, isFromWatchHistory);
                }
            });
            
            // Set watch history flag if current category is watch history
            boolean isWatchHistory = false;
            if (allCategories != null && currentCategoryIndex >= 0 && currentCategoryIndex < allCategories.size()) {
                HomeContent currentCategory = allCategories.get(currentCategoryIndex);
                isWatchHistory = "watch_history".equalsIgnoreCase(currentCategory.getType());
            }
            thumbnailPresenter.setWatchHistoryCategory(isWatchHistory);
            
            // Create new adapter or update existing one
            if (currentThumbnailAdapter == null) {
                currentThumbnailAdapter = new ArrayObjectAdapter(thumbnailPresenter);
            } else {
                currentThumbnailAdapter.clear();
            }
            
            // Lazy loading: Load initial 10 items
            int initialLoadCount = Math.min(10, newContent.size());
            for (int i = 0; i < initialLoadCount; i++) {
                currentThumbnailAdapter.add(newContent.get(i));
            }
            Log.d(TAG, "🎬 Lazy loading on category switch: Initial " + initialLoadCount + " of " + newContent.size() + " thumbnails");
            
            // Update grid adapter
            androidx.leanback.widget.ItemBridgeAdapter bridgeAdapter = 
                new androidx.leanback.widget.ItemBridgeAdapter(currentThumbnailAdapter);
            thumbnailsGrid.setAdapter(bridgeAdapter);
            
            // Ensure grid remains focusable after adapter update
            thumbnailsGrid.setFocusable(true);
            thumbnailsGrid.setFocusableInTouchMode(true);
            
            // Apply fade in animation
            android.view.animation.Animation fadeIn = android.view.animation.AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
            thumbnailsGrid.startAnimation(fadeIn);
            
            // Request focus on first item to trigger selection listener
            thumbnailsGrid.post(() -> {
                thumbnailsGrid.requestFocus();
                thumbnailsGrid.setSelectedPosition(0);
            });
            
            // Update Hero Banner with first item from new list
            if (newContent.size() > 0) {
                VideoContent firstVideo = newContent.get(0);
                currentHeroVideo = firstVideo;
                updateHeroBannerContent(heroBannerViewReference, firstVideo);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error updating thumbnail list", e);
        }
    }
    
    /**
     * 🔄 Switch to next category when user presses DOWN
     */
    private void switchToNextCategory() {
        Log.d(TAG, "🔽 switchToNextCategory called - Current categories: " + (allCategories != null ? allCategories.size() : "null"));
        
        if (allCategories == null || allCategories.isEmpty()) {
            Log.w(TAG, "⚠️ No categories available to switch");
            return;
        }
        
        int oldIndex = currentCategoryIndex;
        // Move to next category (cycle to beginning if at end)
        currentCategoryIndex = (currentCategoryIndex + 1) % allCategories.size();
        Log.d(TAG, "📊 Category index: " + oldIndex + " → " + currentCategoryIndex);
        
        // Reset pagination for new category
        currentCategoryPage = 1;
        isLoadingMoreForCategory = false;
        
        loadCategoryContent();
    }
    
    /**
     * 🔄 Switch to previous category when user presses UP
     */
    private boolean switchToPreviousCategory() {
        Log.d(TAG, "🔼 switchToPreviousCategory called - Current categories: " + (allCategories != null ? allCategories.size() : "null"));
        
        if (allCategories == null || allCategories.isEmpty()) {
            Log.w(TAG, "⚠️ No categories available to switch");
            return false;
        }
        
        // 🎯 Don't cycle to end - allow focus to escape to search bar
        if (currentCategoryIndex <= 0) {
            Log.d(TAG, "🚫 Already at first category - allow focus to escape UP");
            return false; // Don't consume the UP key, let it go to search
        }
        
        int oldIndex = currentCategoryIndex;
        // Move to previous category
        currentCategoryIndex--;
        Log.d(TAG, "📊 Category index: " + oldIndex + " → " + currentCategoryIndex);
        
        // Reset pagination for new category
        currentCategoryPage = 1;
        isLoadingMoreForCategory = false;
        
        loadCategoryContent();
        return true;
    }
    
    /**
     * 🎬 Handle thumbnail click - either play video (watch history) or show details
     */
    private void handleThumbnailClick(VideoContent video, boolean isFromWatchHistory) {
        Log.d(TAG, "🖱️ Thumbnail clicked: " + video.getTitle() + " (from watch history: " + isFromWatchHistory + ")");
        Log.d(TAG, "   Video ID: " + video.getId());
        Log.d(TAG, "   Video Type: " + video.getType());
        Log.d(TAG, "   Video URL: " + video.getVideoUrl());
        Log.d(TAG, "   Description: " + video.getDescription());
        
        if (isFromWatchHistory) {
            // Parse watch position from description field
            long watchPosition = 0;
            if (video.getDescription() != null && video.getDescription().startsWith("WATCH_HISTORY:")) {
                try {
                    String positionStr = video.getDescription().substring("WATCH_HISTORY:".length());
                    watchPosition = Long.parseLong(positionStr);
                    Log.d(TAG, "📍 Resume position: " + watchPosition + "ms");
                } catch (NumberFormatException e) {
                    Log.e(TAG, "❌ Failed to parse watch position: " + video.getDescription(), e);
                }
            }
            
            // Check if video has URL
            String videoUrl = video.getVideoUrl();
            Log.d(TAG, "🔍 Checking video URL: " + (videoUrl != null ? videoUrl : "NULL"));
            
            if (videoUrl == null || videoUrl.isEmpty()) {
                // No video URL, try to fetch from server using video ID
                Log.w(TAG, "⚠️ No video URL stored, will open details page to fetch");
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Đang tải thông tin phim...", Toast.LENGTH_SHORT).show();
                }
                openVideoDetailsPage(video);
                return;
            }
            
            // Open PlayerActivity directly with resume position
            Intent intent = new Intent(getActivity(), com.files.codes.view.PlayerActivity.class);
            intent.putExtra("id", video.getId());
            
            // Determine content type
            String contentType = "movie";
            if (video.getIsTvseries() != null && video.getIsTvseries().equals("1")) {
                contentType = "tvseries";
            } else if (video.getType() != null && !video.getType().isEmpty()) {
                contentType = video.getType();
            }
            
            intent.putExtra("type", contentType);
            intent.putExtra("title", video.getTitle() != null ? video.getTitle() : "");
            intent.putExtra("poster", video.getPosterUrl() != null ? video.getPosterUrl() : "");
            intent.putExtra("thumbnail", video.getThumbnailUrl() != null ? video.getThumbnailUrl() : "");
            intent.putExtra("video_url", videoUrl);
            intent.putExtra("position", watchPosition);
            intent.putExtra("from_watch_history", true);
            
            Log.d(TAG, "▶️ Opening PlayerActivity - ID: " + video.getId() + ", Type: " + contentType + ", Position: " + watchPosition + "ms");
            startActivity(intent);
        } else {
            // Regular content - open details page
            openVideoDetailsPage(video);
        }
    }
    
    /**
     * 📄 Open VideoDetailsActivity for a video
     */
    private void openVideoDetailsPage(VideoContent video) {
        Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
        intent.putExtra("id", video.getId());
        
        // Determine type
        String contentType = "movie";
        if (video.getIsTvseries() != null && video.getIsTvseries().equals("1")) {
            contentType = "tvseries";
        } else if (video.getType() != null && !video.getType().isEmpty()) {
            contentType = video.getType();
        }
        
        intent.putExtra("type", contentType);
        intent.putExtra("thumbImage", video.getThumbnailUrl() != null ? video.getThumbnailUrl() : "");
        
        Log.d(TAG, "🎬 Opening VideoDetailsActivity - ID: " + video.getId() + ", Type: " + contentType);
        startActivity(intent);
    }
    
    /**
     * 📥 Load content for current category
     */
    private void loadCategoryContent() {
        if (allCategories == null || allCategories.isEmpty() || currentCategoryIndex < 0 || currentCategoryIndex >= allCategories.size()) {
            Log.e(TAG, "❌ Invalid category index: " + currentCategoryIndex);
            return;
        }
        
        HomeContent category = allCategories.get(currentCategoryIndex);
        Log.d(TAG, "🔄 Switching to category: " + category.getTitle() + " (type: " + category.getType() + ", id: " + category.getId() + ")");
        
        String categoryType = category.getType().toLowerCase();
        String categoryId = category.getId();
        
        // Check if content needs to be loaded dynamically
        if ("watch_history".equals(categoryType)) {
            // Load watch history content
            loadWatchHistoryForHeroBanner(category);
        } else if ("country".equals(categoryType)) {
            // Load country content dynamically
            loadCountryContentForHeroBanner(category);
        } else if ("genre".equals(categoryType)) {
            // Load genre content dynamically
            loadGenreContentForHeroBanner(category);
        } else if ("phim4k".equals(categoryType)) {
            // Load phim4k content dynamically
            loadPhim4kContentForHeroBanner(category);
        } else if ("movie".equals(categoryType) || "2".equals(categoryId)) {
            // Load movie content dynamically (Phim Lẻ Mới Nhất)
            loadMovieContentForHeroBanner(category);
        } else if ("tvseries".equals(categoryType)) {
            // Load tvseries content dynamically (Phim Bộ Mới Nhất)
            loadTvSeriesContentForHeroBanner(category);
        } else {
            // For other types (features_genre_and_movie), content is already loaded
            List<VideoContent> categoryVideos = category.getContent();
            if (categoryVideos != null && !categoryVideos.isEmpty()) {
                // Update thumbnail list with new category content
                updateHeroThumbnailList(categoryVideos);
                
                // Update category title in Hero Banner
                updateCategoryTitle(category.getTitle());
            } else {
                Log.w(TAG, "⚠️ No videos in category: " + category.getTitle());
                // Show placeholder
                List<VideoContent> placeholder = new ArrayList<>();
                VideoContent placeholderContent = new VideoContent();
                placeholderContent.setTitle("Đang tải " + category.getTitle() + "...");
                placeholder.add(placeholderContent);
                updateHeroThumbnailList(placeholder);
                updateCategoryTitle(category.getTitle());
            }
        }
    }
    
    /**
     * 🏷️ Update category title in Hero Banner
     */
    private void updateCategoryTitle(String categoryTitle) {
        if (heroBannerViewReference == null) return;
        
        try {
            android.widget.TextView titleView = heroBannerViewReference.findViewById(R.id.hero_thumbnails_title);
            if (titleView != null) {
                // Fade out old title
                android.view.animation.Animation fadeOut = android.view.animation.AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);
                fadeOut.setDuration(200);
                fadeOut.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(android.view.animation.Animation animation) {
                    }
                    
                    @Override
                    public void onAnimationEnd(android.view.animation.Animation animation) {
                        // Update text
                        titleView.setText(categoryTitle);
                        
                        // Fade in new title
                        android.view.animation.Animation fadeIn = android.view.animation.AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
                        fadeIn.setDuration(300);
                        titleView.startAnimation(fadeIn);
                    }
                    
                    @Override
                    public void onAnimationRepeat(android.view.animation.Animation animation) {
                    }
                });
                
                titleView.startAnimation(fadeOut);
                Log.d(TAG, "✅ Category title updated with animation: " + categoryTitle);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error updating category title", e);
        }
    }
    
    /**
     * 📺 Load Watch History content for Hero Banner
     */
    private void loadWatchHistoryForHeroBanner(HomeContent category) {
        Log.d(TAG, "📺 Loading Watch History for Hero Banner");
        
        // Check login status
        if (!PreferenceUtils.isLoggedIn(getContext())) {
            Log.w(TAG, "⚠️ User not logged in");
            updateCategoryTitle(category.getTitle());
            updateHeroThumbnailList(new ArrayList<>());
            return;
        }
        
        // Check if sync manager is available
        if (syncManager == null) {
            Log.e(TAG, "❌ WatchHistorySyncManager not initialized");
            updateCategoryTitle(category.getTitle());
            updateHeroThumbnailList(new ArrayList<>());
            return;
        }
        
        // Check if sync manager can auto sync
        if (!syncManager.canAutoSync()) {
            Log.w(TAG, "⚠️ Email không hợp lệ để sync lịch sử");
            updateCategoryTitle(category.getTitle());
            updateHeroThumbnailList(new ArrayList<>());
            return;
        }
        
        // Check if sync link exists
        if (syncManager.getSyncUserId() == null) {
            // Create sync link first
            String userEmail = syncManager.getCurrentUserEmail();
            syncManager.createSyncLink(userEmail, new WatchHistorySyncManager.SyncCallback() {
                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, "✅ Sync link created, now syncing from server");
                    syncWatchHistoryAndLoad(category);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "❌ Error creating sync link: " + error);
                    loadLocalWatchHistory(category);
                }
            });
        } else {
            // Sync link exists, sync from server
            syncWatchHistoryAndLoad(category);
        }
    }
    
    /**
     * 🔄 Sync watch history from server and load
     */
    private void syncWatchHistoryAndLoad(HomeContent category) {
        syncManager.syncWatchHistoryFromServer(new WatchHistorySyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "✅ Watch history synced from server");
                loadLocalWatchHistory(category);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error syncing watch history: " + error);
                loadLocalWatchHistory(category);
            }
        });
    }
    
    /**
     * 📂 Load local watch history
     */
    private void loadLocalWatchHistory(HomeContent category) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Get watch history data
                    List<WatchHistorySyncItem.WatchHistoryItem> historyItems = syncManager.getWatchHistoryForDisplay();
                    Log.d(TAG, "📂 Retrieved " + (historyItems != null ? historyItems.size() : "null") + " history items");
                    
                    if (historyItems != null && !historyItems.isEmpty()) {
                        // Convert to VideoContent
                        List<VideoContent> historyVideoContent = new ArrayList<>();
                        
                        for (WatchHistorySyncItem.WatchHistoryItem historyItem : historyItems) {
                            VideoContent videoContent = new VideoContent();
                            videoContent.setId(historyItem.getVideoId());
                            videoContent.setTitle(historyItem.getTitle());
                            
                            // Prioritize thumbnailUrl over posterUrl
                            String thumbnailUrl = historyItem.getThumbnailUrl();
                            String posterUrl = historyItem.getPosterUrl();
                            
                            if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                                videoContent.setPosterUrl(thumbnailUrl);
                                videoContent.setThumbnailUrl(thumbnailUrl);
                            } else if (posterUrl != null && !posterUrl.isEmpty()) {
                                videoContent.setPosterUrl(posterUrl);
                                videoContent.setThumbnailUrl(posterUrl);
                            }
                            
                            // Set correct type and isTvseries
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
                            
                            // Set video URL and position
                            String videoUrl = historyItem.getVideoUrl();
                            if (videoUrl != null && !videoUrl.isEmpty()) {
                                videoContent.setVideoUrl(videoUrl);
                            }
                            
                            // ✅ CRITICAL: Store watch position in description with WATCH_HISTORY prefix
                            long currentPos = historyItem.getCurrentPosition();
                            videoContent.setDescription("WATCH_HISTORY:" + currentPos);
                            Log.d(TAG, "📍 Set watch position for " + videoContent.getTitle() + ": " + currentPos + "ms");
                            
                            historyVideoContent.add(videoContent);
                        }
                        
                        // Update on UI thread
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateHeroThumbnailList(historyVideoContent);
                                    updateCategoryTitle(category.getTitle());
                                    Log.d(TAG, "✅ Watch history loaded: " + historyVideoContent.size() + " items");
                                }
                            });
                        }
                    } else {
                        // No history
                        Log.w(TAG, "⚠️ No watch history items");
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateHeroThumbnailList(new ArrayList<>());
                                    updateCategoryTitle(category.getTitle());
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error loading watch history", e);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateHeroThumbnailList(new ArrayList<>());
                                updateCategoryTitle(category.getTitle());
                            }
                        });
                    }
                }
            }
        }).start();
    }
    
    /**
     * 🌍 Load Country content for Hero Banner
     */
    private void loadCountryContentForHeroBanner(HomeContent category) {
        loadCountryContentForHeroBanner(category, 1, false);
    }
    
    private void loadCountryContentForHeroBanner(HomeContent category, int page, boolean appendMode) {
        String countryId = category.getId();
        Log.d(TAG, "🌍 Loading Country content: " + category.getTitle() + " (ID: " + countryId + ", Page: " + page + ", Append: " + appendMode + ")");
        
        if (appendMode && isLoadingMoreForCategory) {
            Log.d(TAG, "⚠️ Already loading more, skipping...");
            return;
        }
        
        if (appendMode) {
            isLoadingMoreForCategory = true;
        }
        
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService apiService = retrofit.create(ApiService.class);
        Call<List<Movie>> call = apiService.getMovieByCountry(AppConfig.getApiKey(), countryId, page);
        
        call.enqueue(new Callback<List<Movie>>() {
            @Override
            public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                List<VideoContent> countryVideos = new ArrayList<>();
                                for (Movie movie : response.body()) {
                                    VideoContent videoContent = convertMovieToVideoContent(movie);
                                    countryVideos.add(videoContent);
                                }
                                
                                if (appendMode && currentFeaturedContent != null) {
                                    // Append to existing list
                                    currentFeaturedContent.addAll(countryVideos);
                                    Log.d(TAG, "📦 Appended " + countryVideos.size() + " items, total now: " + currentFeaturedContent.size());
                                    
                                    // Add to adapter
                                    if (currentThumbnailAdapter != null) {
                                        for (VideoContent video : countryVideos) {
                                            currentThumbnailAdapter.add(video);
                                        }
                                    }
                                    
                                    currentCategoryPage = page;
                                } else {
                                    // Initial load - replace list
                                    updateHeroThumbnailList(countryVideos);
                                    currentCategoryPage = 1;
                                }
                                
                                updateCategoryTitle(category.getTitle());
                                isLoadingMoreForCategory = false;
                            }
                        });
                    }
                } else {
                    Log.w(TAG, "⚠️ No data for country: " + countryId + " page " + page);
                    isLoadingMoreForCategory = false;
                    if (!appendMode && getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateHeroThumbnailList(new ArrayList<>());
                                updateCategoryTitle(category.getTitle());
                            }
                        });
                    }
                }
            }
            
            @Override
            public void onFailure(Call<List<Movie>> call, Throwable t) {
                Log.e(TAG, "❌ Error loading country content: " + t.getMessage());
                isLoadingMoreForCategory = false;
            }
        });
    }
    
    /**
     * 🎭 Load Genre content for Hero Banner
     */
    private void loadGenreContentForHeroBanner(HomeContent category) {
        loadGenreContentForHeroBanner(category, 1, false);
    }
    
    private void loadGenreContentForHeroBanner(HomeContent category, int page, boolean appendMode) {
        String genreId = category.getId();
        Log.d(TAG, "🎭 Loading Genre content: " + category.getTitle() + " (ID: " + genreId + ", Page: " + page + ", Append: " + appendMode + ")");
        
        if (appendMode && isLoadingMoreForCategory) {
            Log.d(TAG, "⚠️ Already loading more, skipping...");
            return;
        }
        
        if (appendMode) {
            isLoadingMoreForCategory = true;
        }
        
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService apiService = retrofit.create(ApiService.class);
        Call<List<Movie>> call = apiService.getMovieByGenre(AppConfig.getApiKey(), genreId, page);
        
        call.enqueue(new Callback<List<Movie>>() {
            @Override
            public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                List<VideoContent> genreVideos = new ArrayList<>();
                                for (Movie movie : response.body()) {
                                    VideoContent videoContent = convertMovieToVideoContent(movie);
                                    genreVideos.add(videoContent);
                                }
                                
                                if (appendMode && currentFeaturedContent != null) {
                                    // Append to existing list
                                    currentFeaturedContent.addAll(genreVideos);
                                    Log.d(TAG, "📦 Appended " + genreVideos.size() + " items, total now: " + currentFeaturedContent.size());
                                    
                                    // Add to adapter
                                    if (currentThumbnailAdapter != null) {
                                        for (VideoContent video : genreVideos) {
                                            currentThumbnailAdapter.add(video);
                                        }
                                    }
                                    
                                    currentCategoryPage = page;
                                } else {
                                    // Initial load - replace list
                                    updateHeroThumbnailList(genreVideos);
                                    currentCategoryPage = 1;
                                }
                                
                                updateCategoryTitle(category.getTitle());
                                isLoadingMoreForCategory = false;
                            }
                        });
                    }
                } else {
                    Log.w(TAG, "⚠️ No data for genre: " + genreId + " page " + page);
                    isLoadingMoreForCategory = false;
                    if (!appendMode && getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateHeroThumbnailList(new ArrayList<>());
                                updateCategoryTitle(category.getTitle());
                            }
                        });
                    }
                }
            }
            
            @Override
            public void onFailure(Call<List<Movie>> call, Throwable t) {
                Log.e(TAG, "❌ Error loading genre content: " + t.getMessage());
                isLoadingMoreForCategory = false;
            }
        });
    }
    
    /**
     * 🎬 Load Phim4k content for Hero Banner
     */
    private void loadPhim4kContentForHeroBanner(HomeContent category) {
        Log.d(TAG, "🎬 Loading Phim4k content: " + category.getTitle());
        
        // Use Phim4kClient with callback
        Phim4kClient.getInstance().getLatestMovies(1, new Phim4kClient.Phim4kCallback() {
            @Override
            public void onSuccess(List<VideoContent> videoContents) {
                if (videoContents != null && !videoContents.isEmpty()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Make sure types are set correctly
                                for (VideoContent videoContent : videoContents) {
                                    if (videoContent.getType() == null) {
                                        if (videoContent.getIsTvseries() != null && videoContent.getIsTvseries().equals("1")) {
                                            videoContent.setType("tvseries");
                                        } else {
                                            videoContent.setType("movie");
                                        }
                                    }
                                }
                                
                                updateHeroThumbnailList(videoContents);
                                updateCategoryTitle(category.getTitle());
                                Log.d(TAG, "✅ Phim4k content loaded: " + videoContents.size() + " videos");
                            }
                        });
                    }
                } else {
                    Log.w(TAG, "⚠️ No Phim4k content available");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateHeroThumbnailList(new ArrayList<>());
                                updateCategoryTitle(category.getTitle());
                            }
                        });
                    }
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error loading Phim4k content: " + error);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateHeroThumbnailList(new ArrayList<>());
                            updateCategoryTitle(category.getTitle());
                        }
                    });
                }
            }
        });
    }
    
    /**
     * 🎬 Load Movie content for Hero Banner (Phim Lẻ Mới Nhất)
     */
    private void loadMovieContentForHeroBanner(HomeContent category) {
        Log.d(TAG, "🎬 Loading Movie content: " + category.getTitle());
        
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService apiService = retrofit.create(ApiService.class);
        Call<List<Movie>> call = apiService.getMovies(AppConfig.getApiKey(), 1);
        
        call.enqueue(new Callback<List<Movie>>() {
            @Override
            public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                List<VideoContent> movieVideos = new ArrayList<>();
                                for (Movie movie : response.body()) {
                                    VideoContent videoContent = convertMovieToVideoContent(movie);
                                    videoContent.setType("movie");
                                    videoContent.setIsTvseries("0");
                                    movieVideos.add(videoContent);
                                }
                                updateHeroThumbnailList(movieVideos);
                                updateCategoryTitle(category.getTitle());
                                Log.d(TAG, "✅ Movie content loaded: " + movieVideos.size() + " movies");
                            }
                        });
                    }
                } else {
                    Log.w(TAG, "⚠️ No Movie content available");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateHeroThumbnailList(new ArrayList<>());
                                updateCategoryTitle(category.getTitle());
                            }
                        });
                    }
                }
            }
            
            @Override
            public void onFailure(Call<List<Movie>> call, Throwable t) {
                Log.e(TAG, "❌ Error loading Movie content: " + t.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateHeroThumbnailList(new ArrayList<>());
                            updateCategoryTitle(category.getTitle());
                        }
                    });
                }
            }
        });
    }
    
    /**
     * 📺 Load TvSeries content for Hero Banner (Phim Bộ Mới Nhất)
     */
    private void loadTvSeriesContentForHeroBanner(HomeContent category) {
        Log.d(TAG, "📺 Loading TvSeries content: " + category.getTitle());
        
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService apiService = retrofit.create(ApiService.class);
        Call<List<Movie>> call = apiService.getTvSeries(AppConfig.getApiKey(), 1);
        
        call.enqueue(new Callback<List<Movie>>() {
            @Override
            public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                List<VideoContent> tvSeriesVideos = new ArrayList<>();
                                for (Movie series : response.body()) {
                                    VideoContent videoContent = convertMovieToVideoContent(series);
                                    videoContent.setType("tvseries");
                                    videoContent.setIsTvseries("1");
                                    tvSeriesVideos.add(videoContent);
                                }
                                updateHeroThumbnailList(tvSeriesVideos);
                                updateCategoryTitle(category.getTitle());
                                Log.d(TAG, "✅ TvSeries content loaded: " + tvSeriesVideos.size() + " series");
                            }
                        });
                    }
                } else {
                    Log.w(TAG, "⚠️ No TvSeries content available");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateHeroThumbnailList(new ArrayList<>());
                                updateCategoryTitle(category.getTitle());
                            }
                        });
                    }
                }
            }
            
            @Override
            public void onFailure(Call<List<Movie>> call, Throwable t) {
                Log.e(TAG, "❌ Error loading TvSeries content: " + t.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateHeroThumbnailList(new ArrayList<>());
                            updateCategoryTitle(category.getTitle());
                        }
                    });
                }
            }
        });
    }
}

