package com.files.codes.view.fragments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ObjectAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.files.codes.AppConfig;
import com.files.codes.R;
import com.files.codes.model.Movie;
import com.files.codes.model.PlaybackModel;
import com.files.codes.model.SearchContent;
import com.files.codes.model.SearchModel;
import com.files.codes.model.TvModel;
import com.files.codes.model.VideoContent;
import com.files.codes.model.api.ApiService;
import com.files.codes.utils.KKPhim4kClient;
import com.files.codes.utils.Phim4kClient;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.utils.SearchAdvancedHelper;
import com.files.codes.utils.Utils;
import com.files.codes.view.PlayerActivity;
import com.files.codes.view.VideoDetailsActivity;
import com.files.codes.view.HeroStyleVideoDetailsActivity;
import com.files.codes.view.VideoPlaybackActivity;
import com.files.codes.view.presenter.SearchCardPresenter;
import com.files.codes.view.presenter.TvSearchPresenter;

//import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


public class SearchFragment extends SearchSupportFragment implements SearchSupportFragment.SearchResultProvider, LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "SearchFragment";
    private static final long SEARCH_DELAY_MS = 1000L;
    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;
    private int page_number = 1;
    private String mQuery;
    private List<SearchContent> mItems = new ArrayList<>();
    private List<SearchModel> searchList = new ArrayList<>();
    private String tvHeader = "";
    private String tvSeriesHeader = "";
    private String movieHeader = "";
    private String phim4kHeader = "";
    private List<VideoContent> phim4kResults = new ArrayList<>();
    private String kkPhim4kHeader = "";
    private List<VideoContent> kkPhim4kResults = new ArrayList<>();


    private final Runnable mDelayedLoad = new Runnable() {
        @Override
        public void run() {
            //loadRows();
            getQueryData();

        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setSearchResultProvider(this);
        setOnItemViewClickedListener(getDefaultItemViewClickedListener());
        
        // ✅ Disable microphone button completely
        setSpeechRecognitionCallback(null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Log.d(TAG, "SearchFragment onViewCreated - Voice search disabled");
        
        // ✅ Immediately remove microphone button without delay
        try {
            View speechOrb = findViewByName(view, "speech_orb");
            if (speechOrb != null && speechOrb.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) speechOrb.getParent();
                parent.removeView(speechOrb);
                // Log.d(TAG, "✅ Speech orb button immediately removed");
            } else {
                // Log.d(TAG, "⚠️ Could not find speech orb to remove");
            }
        } catch (Exception e) {
            // Log.w(TAG, "Error removing speech orb: " + e.getMessage());
        }
        
        // Setup navigation to filter panel (left side)
        try {
            View searchBar = findViewByName(view, "lb_search_bar");
            View searchEditText = findViewByName(view, "lb_search_text_editor");
            if (searchBar != null && getActivity() != null) {
                View filterButton = getActivity().findViewById(R.id.btn_filter_genre);
                if (filterButton != null) {
                    if (searchEditText != null) {
                        searchEditText.setNextFocusLeftId(R.id.btn_filter_genre);
                    }
                    searchBar.setNextFocusLeftId(R.id.btn_filter_genre);
                }
            }
        } catch (Exception e) {
            // Log.w(TAG, "Error setting up search navigation: " + e.getMessage());
        }
        
        // Search bar focus styling removed - keep default Leanback behavior
    }
    
    // Helper method to find view by resource name
    private View findViewByName(View root, String resourceName) {
        try {
            // Check if this view matches
            String name = root.getResources().getResourceEntryName(root.getId());
            if (name != null && name.contains(resourceName)) {
                return root;
            }
            
            // Recursively check children
            if (root instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) root;
                for (int i = 0; i < group.getChildCount(); i++) {
                    View child = group.getChildAt(i);
                    if (child != null) {
                        String childName = null;
                        try {
                            childName = child.getResources().getResourceEntryName(child.getId());
                        } catch (Exception e) {
                            // Resource ID might not exist
                        }
                        
                        if (childName != null && childName.contains(resourceName)) {
                            return child;
                        }
                        
                        View found = findViewByName(child, resourceName);
                        if (found != null) {
                            return found;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error in findViewByName: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * TV-optimized search bar:
     * - White border when search bar is focused
     * - Uses descendantFocusability to block children initially
     * - Press ENTER/OK → unblock children, focus EditText for typing
     * - Press BACK while typing → block children again, return to bar
     */
    private void setupSearchBarFocusStyle(View rootView) {
        try {
            final View searchBar = findViewByName(rootView, "lb_search_bar");
            final View searchEditTextView = findViewByName(rootView, "lb_search_text_editor");
            
            if (searchBar == null || !(searchBar instanceof ViewGroup)) return;
            final ViewGroup searchBarGroup = (ViewGroup) searchBar;
            
            // Apply focus border to search bar container
            searchBarGroup.setBackgroundResource(R.drawable.search_bar_focus_bg);
            searchBarGroup.setPadding(16, 8, 16, 8);
            searchBarGroup.setFocusable(true);
            searchBarGroup.setFocusableInTouchMode(true);
            
            // Block descendant focus — search bar itself receives focus
            // D-pad can navigate TO search bar, but not INTO EditText
            rootView.postDelayed(() -> {
                searchBarGroup.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                // Move focus to search bar itself (away from EditText)
                searchBarGroup.requestFocus();
            }, 150);
            
            if (searchEditTextView instanceof EditText) {
                final EditText editText = (EditText) searchEditTextView;
                
                // ENTER/OK on search bar → unblock descendants, focus EditText
                searchBarGroup.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                        searchBarGroup.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
                        editText.requestFocus();
                        editText.setSelection(editText.getText().length());
                        return true;
                    }
                    return false;
                });
                
                // BACK while in EditText → block descendants again, return to bar
                editText.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        keyCode == KeyEvent.KEYCODE_BACK) {
                        editText.clearFocus();
                        searchBarGroup.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                        searchBarGroup.requestFocus();
                        return true;
                    }
                    return false;
                });
            }
            
            // Global focus listener: highlight search bar when it or child has focus
            searchBarGroup.getViewTreeObserver().addOnGlobalFocusChangeListener(
                (oldFocus, newFocus) -> {
                    boolean hasFocus = newFocus == searchBarGroup || 
                        (newFocus != null && isDescendantOf(newFocus, searchBarGroup));
                    searchBarGroup.setSelected(hasFocus);
                });
                
        } catch (Exception e) {
            Log.w(TAG, "Error setting up search bar focus style: " + e.getMessage());
        }
    }
    
    /** Check if a view is a descendant of a parent view */
    private boolean isDescendantOf(View child, View parent) {
        if (child == parent) return true;
        View current = child.getParent() instanceof View ? (View) child.getParent() : null;
        while (current != null) {
            if (current == parent) return true;
            current = current.getParent() instanceof View ? (View) current.getParent() : null;
        }
        return false;
    }

    // click listener
    private OnItemViewClickedListener getDefaultItemViewClickedListener() {
        return new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder viewHolder, Object o,
                                      RowPresenter.ViewHolder viewHolder2, Row row) {

                SearchContent searchContent = (SearchContent) o;
                switch (searchContent.getType()) {
                    case "tv": {
                        Intent intent = new Intent(getActivity(), PlayerActivity.class);
                        PlaybackModel video = new PlaybackModel();
                        video.setId(Long.parseLong(searchContent.getId()));
                        video.setTitle(searchContent.getTitle());
                        video.setDescription(searchContent.getDescription());
                        video.setCategory("tv");
                        video.setVideoUrl(searchContent.getStreamUrl());
                        video.setVideoType(searchContent.getStreamFrom());
                        video.setBgImageUrl(searchContent.getThumbnailUrl());
                        video.setCardImageUrl(searchContent.getThumbnailUrl());

                        intent.putExtra(VideoPlaybackActivity.EXTRA_VIDEO, video);
                        startActivity(intent);
                        break;
                    }
                    case "tvseries": {
                        Log.d(TAG, "SearchFragment - Opening tvseries: " + searchContent.getTitle() + " (ID: " + searchContent.getId() + ")");
                        Intent intent = new Intent(getActivity(), HeroStyleVideoDetailsActivity.class);
                        intent.putExtra("id", searchContent.getId());
                        intent.putExtra("type", "tvseries");
                        intent.putExtra("thumbImage", searchContent.getThumbnailUrl());
                        // Add source info if it's from Phim4k
                        if ("Phim4k".equals(searchContent.getStreamFrom())) {
                            intent.putExtra("source", "phim4k");
                        }
                        startActivity(intent);
                        break;
                    }
                    case "movie": {
                        Log.d(TAG, "SearchFragment - Opening movie: " + searchContent.getTitle() + " (ID: " + searchContent.getId() + ")");
                        Intent intent = new Intent(getActivity(), HeroStyleVideoDetailsActivity.class);
                        intent.putExtra("id", searchContent.getId());
                        intent.putExtra("type", "movie");
                        intent.putExtra("thumbImage", searchContent.getThumbnailUrl());
                        // Add source info if it's from Phim4k
                        if ("Phim4k".equals(searchContent.getStreamFrom())) {
                            intent.putExtra("source", "phim4k");
                        }
                        startActivity(intent);
                        break;
                    }
                }
            }
        };
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        // ✅ Deny microphone permission to prevent SpeechRecognizer initialization
        // This prevents voice search feature from initializing
        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i].equals(android.Manifest.permission.RECORD_AUDIO)) {
                // Force deny microphone permission
                grantResults[i] = android.content.pm.PackageManager.PERMISSION_DENIED;
                Log.d(TAG, "✅ Microphone permission denied - voice search disabled");
            }
        }
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    public boolean hasResults() {
        return mRowsAdapter.size() > 0;
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        // Log.e(TAG, "getResultsAdapter - returning adapter with " + mRowsAdapter.size() + " items");
        
        // Ensure the adapter is properly initialized and has data
        if (mRowsAdapter == null) {
            // Log.e(TAG, "mRowsAdapter is null, initializing new adapter");
            mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        }
        
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        // Log.e(TAG, String.format("Search Query Text Change %s", newQuery));
        // Log.e(TAG, "onQueryTextChange: " + newQuery );
        loadQueryWithDelay(newQuery, 100000000);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        // Log.e(TAG, String.format("Search Query Text Submit %s", query));
        // No need to delay(wait) loadQuery, since the query typing has completed.
        loadQueryWithDelay(query, 0);
        return true;
    }

    private void loadQueryWithDelay(String query, long delay) {
        mHandler.removeCallbacks(mDelayedLoad);
        if (!TextUtils.isEmpty(query) && !query.equals("")) {
            mQuery = query;
            mHandler.postDelayed(mDelayedLoad, delay);
            // Log.e(TAG,  "Handler started");
        }
    }

    private void getQueryData() {
        final String query = mQuery;
        // Log.d(TAG, "=== STARTING SEARCH ===");
        // Log.d(TAG, "Query: '" + query + "'");
        
        // ✅ Get search helper and set query
        SearchAdvancedHelper searchHelper = SearchAdvancedHelper.getInstance();
        searchHelper.setSearchQuery(query);
        
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService searchApi = retrofit.create(ApiService.class);
        
        // ✅ Build search call with advanced filters
        String queryParam = query;
        Integer genreId = searchHelper.getSelectedGenreId();
        Integer countryId = searchHelper.getSelectedCountryId();
        Integer yearFrom = searchHelper.getYearFrom();
        Integer yearTo = searchHelper.getYearTo();
        
        // Create query with filters
        String fullQuery = query;
        if (genreId != null) {
            // Log.d(TAG, "📂 Genre filter: " + searchHelper.getGenreMap().get(genreId));
        }
        if (countryId != null) {
            // Log.d(TAG, "🌍 Country filter: " + searchHelper.getCountryMap().get(countryId));
        }
        if (yearFrom != null || yearTo != null) {
            // Log.d(TAG, "📅 Year range: " + (yearFrom != null ? yearFrom : "Any") + " - " + (yearTo != null ? yearTo : "Any"));
        }
        
        // ✅ Use advanced search with all filters
        Call<SearchModel> call;
        if (genreId != null || countryId != null || yearFrom != null || yearTo != null) {
            // Use advanced search with filters
            call = searchApi.getSearchDataAdvanced(AppConfig.API_KEY, queryParam, page_number, "movieserieslive", 
                                                   genreId, countryId, yearFrom, yearTo);
            // Log.d(TAG, "🔍 Using ADVANCED search with filters");
            
            // ✅ Log full request URL for debugging
            String debugUrl = "search?q=" + queryParam + "&page=" + page_number + "&type=movieserieslive";
            if (genreId != null) debugUrl += "&genre_id=" + genreId;
            if (countryId != null) debugUrl += "&country_id=" + countryId;
            if (yearFrom != null) debugUrl += "&range_from=" + yearFrom;
            if (yearTo != null) debugUrl += "&range_to=" + yearTo;
            // Log.d(TAG, "🌐 REQUEST URL: /rest-api/v130/" + debugUrl);
        } else {
            // Use basic search
            call = searchApi.getSearchData(AppConfig.API_KEY, queryParam, page_number, "movieserieslive");
            // Log.d(TAG, "🔍 Using BASIC search (no filters)");
            // Log.d(TAG, "🌐 REQUEST URL: /rest-api/v130/search?q=" + queryParam + "&page=" + page_number + "&type=movieserieslive");
        }
        
        call.enqueue(new Callback<SearchModel>() {
            @Override
            public void onResponse(Call<SearchModel> call,  Response<SearchModel> response) {
                // Log.d(TAG, "✅ API Response received - Code: " + response.code());
                // Log.d(TAG, "📡 Response URL: " + call.request().url().toString());
                
                mItems = new ArrayList<>();

                List<Movie> movieResult = new ArrayList<>();
                List<Movie> tvSeriesResult = new ArrayList<>();
                List<TvModel> tvResult = new ArrayList<>();

                if (response.code() == 200 && response.body() != null) {
                    // Log.d(TAG, "Response body is not null");
                    
                    if (response.body().getTvChannels() != null && response.body().getTvChannels().size() != 0) {
                        tvHeader = "Live TV";
                        tvResult.clear();
                        tvResult = response.body().getTvChannels();
                        // Log.d(TAG, "Found " + tvResult.size() + " Live TV channels");
                        for (TvModel video : tvResult) {
                            String id = video.getLiveTvId();
                            String title = video.getTvName();
                            String description = video.getDescription();
                            String type = "tv";
                            String streamUrl = video.getStreamUrl();
                            String streamFrom = video.getStreamFrom();
                            String thumbnailUrl = video.getPosterUrl();
                            SearchContent searchContent = new SearchContent(id, title, description, type, streamUrl, streamFrom, thumbnailUrl);
                            mItems.add(searchContent);
                        }
                    }

                    if (response.body().getMovie() != null && response.body().getMovie().size() != 0) {
                        movieHeader = "Movies";
                        movieResult.clear();
                        movieResult = response.body().getMovie();
                        // Log.d(TAG, "Found " + movieResult.size() + " movies");
                        for (Movie video : movieResult) {
                            String id = video.getVideosId();
                            String title = video.getTitle();
                            String description = video.getDescription();
                            String type = "movie";
                            String streamUrl = "";
                            String streamFrom = "";
                            String thumbnailUrl = video.getThumbnailUrl();
                            SearchContent searchContent = new SearchContent(id, title, description, type, streamUrl, streamFrom, thumbnailUrl);
                            mItems.add(searchContent);
                        }
                    }

                    if (response.body().getTvseries() != null && response.body().getTvseries().size() != 0) {
                        tvSeriesHeader = "TV Series";
                        tvSeriesResult.clear();
                        tvSeriesResult = response.body().getTvseries();
                        // Log.d(TAG, "Found " + tvSeriesResult.size() + " TV series");
                        for (Movie video : tvSeriesResult) {
                            String id = video.getVideosId();
                            String title = video.getTitle();
                            String description = video.getDescription();
                            String type = "tvseries";
                            String streamUrl = "";
                            String streamFrom = "";
                            String thumbnailUrl = video.getPosterUrl();
                            SearchContent searchContent = new SearchContent(id, title, description, type, streamUrl, streamFrom, thumbnailUrl);
                            mItems.add(searchContent);
                        }
                    }

                    // Log.d(TAG, "Total items from main API: " + mItems.size());
                    // Log.d(TAG, "🔍 Active filters: " + searchHelper.getFilterSummary());
                    
                    loadRows(movieResult, tvSeriesResult, tvResult);
                    
                    // Also search phim4k (text only)
                    searchPhim4k(query);
                    searchKKPhim4k(query);
                } else {
                    // Log.e(TAG, "❌ API Response error - Code: " + response.code() + ", Body: " + (response.body() == null ? "null" : "not null"));
                    showErrorToast("Lỗi tìm kiếm: " + response.code());
                }

            }

            @Override
            public void onFailure(Call<SearchModel> call, Throwable t) {
                // Log.e(TAG, "❌ Network error: " + t.getLocalizedMessage(), t);
                showErrorToast("Lỗi mạng: " + t.getLocalizedMessage());
            }
        });

    }
    
    private void searchPhim4k(String query) {
        // Log.d(TAG, "🔍 Searching Phim4k for: '" + query + "'");
        
        // Clear previous phim4k results
        phim4kResults.clear();
        
        Phim4kClient.getInstance().searchMovies(query, new Phim4kClient.Phim4kCallback() {
            @Override
            public void onSuccess(List<VideoContent> videoContents) {
                if (videoContents != null && !videoContents.isEmpty()) {
                    // Log.d(TAG, "✅ Phim4k found " + videoContents.size() + " results");
                    phim4kHeader = "Nguồn free1";
                    phim4kResults.clear();
                    phim4kResults.addAll(videoContents);
                    
                    // Convert to SearchContent and add to mItems
                    for (VideoContent videoContent : videoContents) {
                        String id = videoContent.getVideosId() != null ? videoContent.getVideosId() : videoContent.getId();
                        String title = videoContent.getTitle();
                        String description = videoContent.getDescription();
                        // Use normal types instead of phim4k_ prefix to avoid click issues
                        String type = videoContent.getIsTvseries() != null && videoContent.getIsTvseries().equals("1") ? "tvseries" : "movie";
                        String streamUrl = videoContent.getStreamUrl() != null ? videoContent.getStreamUrl() : "";
                        String streamFrom = "Phim4k";
                        String thumbnailUrl = videoContent.getThumbnailUrl();
                        
                        SearchContent searchContent = new SearchContent(id, title, description, type, streamUrl, streamFrom, thumbnailUrl);
                        mItems.add(searchContent);
                    }
                    
                    // Log.d(TAG, "✅ Phim4k items converted, total mItems: " + mItems.size());
                    
                    // Re-run loadRows with updated data
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Log.d(TAG, "📺 Adding Phim4k row to adapter (UI thread)");
                                addPhim4kRowToAdapter();
                            }
                        });
                    }
                } else {
                    // Log.d(TAG, "⚠️ Phim4k returned no results or null");
                }
            }

            @Override
            public void onError(String error) {
                // Log.e(TAG, "❌ Phim4k error: " + error);
            }
        });
    }


    @SuppressLint("StaticFieldLeak")
    private void loadRows(final List<Movie> movieResult, final List<Movie> tvSeriesResult, final List<TvModel> tvResult) {
        // offload processing from the UI thread
        new AsyncTask<String, Void, List<ListRow>>() {
            private final String query = mQuery;

            @Override
            protected void onPreExecute() {
                mRowsAdapter.clear();
            }

            @Override
            protected List<ListRow> doInBackground(String... params) {
                final List<SearchContent> result = new ArrayList<>();

                for (SearchContent video : mItems) {
                    // Main logic of search is here.
                    // Just check that "query" is contained in Title or Description or not. (NOTE: excluded studio information here)
                    if (video.getTitle().toLowerCase(Locale.ENGLISH).contains(query.toLowerCase(Locale.ENGLISH))
                            || video.getDescription().toLowerCase(Locale.ENGLISH).contains(query.toLowerCase(Locale.ENGLISH))) {
                        result.add(video);
                    }
                }

                List<ListRow> listRows = new ArrayList<>();

                // Combine Movies and TV Series into one adapter
                ArrayObjectAdapter moviesAndSeriesAdapter = new ArrayObjectAdapter(new SearchCardPresenter());
                ArrayObjectAdapter tvAdapter = new ArrayObjectAdapter(new TvSearchPresenter());

                for (SearchContent video : mItems) {
                    if (video.getType().equalsIgnoreCase("movie") || video.getType().equalsIgnoreCase("tvseries")) {
                        moviesAndSeriesAdapter.add(video);
                    } else if (video.getType().equalsIgnoreCase("tv")) {
                        tvAdapter.add(video);
                    }
                }

                // Only add rows if they have content
                if (moviesAndSeriesAdapter.size() > 0) {
                    listRows.add(new ListRow(new HeaderItem("Premium"), moviesAndSeriesAdapter));
                    // Log.d(TAG, "✅ Added Premium row with " + moviesAndSeriesAdapter.size() + " items");
                }
                if (tvAdapter.size() > 0) {
                    listRows.add(new ListRow(new HeaderItem(tvHeader), tvAdapter));
                    // Log.d(TAG, "✅ Added Live TV row with " + tvAdapter.size() + " items");
                }

                return listRows;
            }

            @Override
            protected void onPostExecute(List<ListRow> listRow) {
                if (getActivity() == null || !isAdded()) {
                    return;
                }
                
                for (ListRow listRow1 : listRow) {
                    mRowsAdapter.add(listRow1);
                }
                
                // Just notify the adapter - let SearchSupportFragment handle the UI
                mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
                
                // ✅ Auto-move focus to results so user can navigate with down arrow
                if (mRowsAdapter.size() > 0) {
                    getView().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // Move focus from search bar to results
                                View resultsView = getView();
                                if (resultsView != null) {
                                    // Find results container (usually contains the rows)
                                    View resultsList = findViewByName(resultsView, "results");
                                    if (resultsList == null) {
                                        // Try alternative: just request focus on parent
                                        resultsView.requestFocus();
                                    } else {
                                        resultsList.requestFocus();
                                    }
                                    // Log.d(TAG, "✅ Focus moved to results - user can now navigate with arrow keys");
                                }
                            } catch (Exception e) {
                                // Log.w(TAG, "Could not move focus to results: " + e.getMessage());
                            }
                        }
                    }, 100);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    private void loadChannelRows(List<Movie> movieResult, List<Movie> tvSeriesResult, List<TvModel> tvResult) {

        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        SearchCardPresenter searchCardPresenter = new SearchCardPresenter();
        HeaderItem header;

        if (movieResult.size() != 0) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(searchCardPresenter);

            for (Movie movie : movieResult) {
                listRowAdapter.add(movie);
            }

            rowsAdapter.add(new ListRow(new HeaderItem(0, "Movies"), listRowAdapter));

        }

        if (tvSeriesResult.size() != 0) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(searchCardPresenter);

            for (Movie movie : tvSeriesResult) {
                listRowAdapter.add(movie);
            }
            rowsAdapter.add(new ListRow(new HeaderItem(0, "Tv Series"), listRowAdapter));
        }

        if (tvResult.size() != 0) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(searchCardPresenter);

            for (TvModel tvModel : tvResult) {
                listRowAdapter.add(tvModel);
            }
            rowsAdapter.add(new ListRow(new HeaderItem(0, "Live TV"), listRowAdapter));
        }

        mRowsAdapter.add(rowsAdapter);
    }

    // The name for the entire content provider.
    public static final String CONTENT_AUTHORITY = "com.mytv.tvbox.tvleanback";
    // Base of all URIs that will be used to contact the content provider.

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);


    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
         Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath("video").build();
         return new CursorLoader(getActivity(), CONTENT_URI, null, "", new String[]{}, null);

    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {

    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }
    
    private void addPhim4kRowToAdapter() {
        if (phim4kResults.isEmpty()) {
            // Log.d(TAG, "⚠️ Phim4k results is empty, skipping");
            return;
        }
        
        // Log.d(TAG, "🔄 Building Phim4k row with " + phim4kResults.size() + " items");
        
        // Check if phim4k row already exists to avoid duplicates
        for (int i = 0; i < mRowsAdapter.size(); i++) {
            Object row = mRowsAdapter.get(i);
            if (row instanceof ListRow) {
                ListRow listRow = (ListRow) row;
                if ("Nguồn free1".equals(listRow.getHeaderItem().getName())) {
                    // Log.d(TAG, "ℹ️ Phim4k row already exists, skipping duplicate");
                    return;
                }
            }
        }
        
        // Create phim4k adapter
        ArrayObjectAdapter phim4kAdapter = new ArrayObjectAdapter(new SearchCardPresenter());
        
        // Add phim4k results to adapter (identify by streamFrom = "Phim4k")
        int addedCount = 0;
        for (SearchContent searchContent : mItems) {
            if ("Phim4k".equals(searchContent.getStreamFrom())) {
                phim4kAdapter.add(searchContent);
                addedCount++;
            }
        }
        
        // Add phim4k row to main adapter
        if (phim4kAdapter.size() > 0) {
            HeaderItem phim4kHeaderItem = new HeaderItem("Nguồn free1");
            ListRow phim4kRow = new ListRow(phim4kHeaderItem, phim4kAdapter);
            mRowsAdapter.add(phim4kRow);
            
            // Log.d(TAG, "✅ Phim4k row added successfully with " + addedCount + " items");
        } else {
            // Log.d(TAG, "⚠️ No Phim4k items to add (possibly already displayed)");
        }
    }

    private void searchKKPhim4k(String query) {
        // Log.d(TAG, "🔍 Searching KKPhim4k for: '" + query + "'");
        
        // Clear previous kkphim4k results
        kkPhim4kResults.clear();
        
        KKPhim4kClient.getInstance().searchMovies(query, new KKPhim4kClient.KKPhim4kCallback() {
            @Override
            public void onSuccess(List<VideoContent> videoContents) {
                if (videoContents != null && !videoContents.isEmpty()) {
                    // Log.d(TAG, "✅ KKPhim4k found " + videoContents.size() + " results");
                    kkPhim4kHeader = "Nguồn Free 2";
                    kkPhim4kResults.clear();
                    kkPhim4kResults.addAll(videoContents);
                    
                    // Convert to SearchContent and add to mItems
                    for (VideoContent videoContent : videoContents) {
                        String id = videoContent.getVideosId() != null ? videoContent.getVideosId() : videoContent.getId();
                        String title = videoContent.getTitle();
                        String description = videoContent.getDescription();
                        // Use normal types instead of kkphim4k_ prefix to avoid click issues
                        String type = videoContent.getIsTvseries() != null && videoContent.getIsTvseries().equals("1") ? "tvseries" : "movie";
                        String streamUrl = videoContent.getStreamUrl() != null ? videoContent.getStreamUrl() : "";
                        String streamFrom = "KKPhim4k";
                        // Use Poster URL for KKPhim4k search results as requested
                        String thumbnailUrl = videoContent.getPosterUrl();
                        if (thumbnailUrl == null || thumbnailUrl.isEmpty()) {
                            thumbnailUrl = videoContent.getThumbnailUrl();
                        }
                        
                        SearchContent searchContent = new SearchContent(id, title, description, type, streamUrl, streamFrom, thumbnailUrl);
                        mItems.add(searchContent);
                    }
                    
                    // Log.d(TAG, "✅ KKPhim4k items converted, total mItems: " + mItems.size());
                    
                    // Re-run loadRows with updated data
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Log.d(TAG, "📺 Adding KKPhim4k row to adapter (UI thread)");
                                addKKPhim4kRowToAdapter();
                            }
                        });
                    }
                } else {
                    // Log.d(TAG, "⚠️ KKPhim4k returned no results or null");
                }
            }

            @Override
            public void onError(String error) {
                // Log.e(TAG, "❌ KKPhim4k error: " + error);
            }
        });
    }

    private void addKKPhim4kRowToAdapter() {
        if (kkPhim4kResults.isEmpty()) {
            // Log.d(TAG, "⚠️ KKPhim4k results is empty, skipping");
            return;
        }
        
        // Log.d(TAG, "🔄 Building KKPhim4k row with " + kkPhim4kResults.size() + " items");
        
        // Check if kkphim4k row already exists to avoid duplicates
        for (int i = 0; i < mRowsAdapter.size(); i++) {
            Object row = mRowsAdapter.get(i);
            if (row instanceof ListRow) {
                ListRow listRow = (ListRow) row;
                if ("Nguồn Free 2".equals(listRow.getHeaderItem().getName())) {
                    // Log.d(TAG, "ℹ️ KKPhim4k row already exists, skipping duplicate");
                    return;
                }
            }
        }
        
        // Create kkphim4k adapter
        ArrayObjectAdapter kkPhim4kAdapter = new ArrayObjectAdapter(new SearchCardPresenter());
        
        // Add kkphim4k results to adapter (identify by streamFrom = "KKPhim4k")
        int addedCount = 0;
        for (SearchContent searchContent : mItems) {
            if ("KKPhim4k".equals(searchContent.getStreamFrom())) {
                kkPhim4kAdapter.add(searchContent);
                addedCount++;
            }
        }
        
        // Add kkphim4k row to main adapter
        if (kkPhim4kAdapter.size() > 0) {
            HeaderItem kkPhim4kHeaderItem = new HeaderItem("Nguồn Free 2");
            ListRow kkPhim4kRow = new ListRow(kkPhim4kHeaderItem, kkPhim4kAdapter);
            mRowsAdapter.add(kkPhim4kRow);
            
            // Log.d(TAG, "✅ KKPhim4k row added successfully with " + addedCount + " items");
        } else {
            // Log.d(TAG, "⚠️ No KKPhim4k items to add (possibly already displayed)");
        }
    }

    private void showErrorToast(String message) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Programmatically set search query and trigger search
     * Called from SearchActivity when query is passed via Intent
     */
    public void setSearchQuery(String query) {
        if (query == null || query.isEmpty()) {
            Log.d(TAG, "⚠️ setSearchQuery called with empty query");
            return;
        }
        
        Log.d(TAG, "✅ setSearchQuery called with: " + query);
        
        // Set the query in SearchSupportFragment
        setSearchQuery(query, true);
        
        // The search will be automatically triggered by SearchSupportFragment
        // through onQueryTextSubmit callback
    }
    
    /**
     * Refresh search with current query and updated filters
     * Called from SearchActivity when filters change
     */
    public void refreshSearch() {
        String currentQuery = mQuery;
        if (currentQuery == null || currentQuery.isEmpty()) {
            Log.d(TAG, "⚠️ refreshSearch: No query to refresh");
            return;
        }
        
        Log.d(TAG, "🔄 Refreshing search with query: " + currentQuery);
        
        // Clear current results
        mRowsAdapter.clear();
        
        // Re-execute search with new filters
        getQueryData();
    }
}