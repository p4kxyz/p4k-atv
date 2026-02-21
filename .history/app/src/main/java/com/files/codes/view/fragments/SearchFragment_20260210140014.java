package com.files.codes.view.fragments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ObjectAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.Locale;

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
import com.files.codes.utils.AnimeClient;
import com.files.codes.utils.KKPhim4kClient;
import com.files.codes.utils.Phim4kClient;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.utils.SearchAdvancedHelper;
import com.files.codes.view.PlayerActivity;
import com.files.codes.view.HeroStyleVideoDetailsActivity;
import com.files.codes.view.VideoPlaybackActivity;
import com.files.codes.view.presenter.SearchCardPresenter;
import com.files.codes.view.presenter.TvSearchPresenter;

//import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


public class SearchFragment extends SearchSupportFragment implements SearchSupportFragment.SearchResultProvider, LoaderManager.LoaderCallbacks<Cursor> {
    private static final long SEARCH_DELAY_MS = 1000L;
    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;
    private String mQuery;
    private List<SearchContent> mItems = new ArrayList<>();
    private String tvHeader = "";
    private List<VideoContent> phim4kResults = new ArrayList<>();
    private List<VideoContent> kkPhim4kResults = new ArrayList<>();
    private List<VideoContent> animeResults = new ArrayList<>();


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
        setSpeechRecognitionCallback(null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Remove microphone button
        try {
            View speechOrb = findViewByName(view, "speech_orb");
            if (speechOrb != null && speechOrb.getParent() instanceof ViewGroup) {
                ((ViewGroup) speechOrb.getParent()).removeView(speechOrb);
            }
        } catch (Exception e) { /* ignore */ }
        
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
        } catch (Exception e) { /* ignore */ }
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
            // ignore
        }
        return null;
    }
    
    // setupSearchBarFocusStyle removed

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
        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i].equals(android.Manifest.permission.RECORD_AUDIO)) {
                grantResults[i] = android.content.pm.PackageManager.PERMISSION_DENIED;
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
        if (mRowsAdapter == null) {
            mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        }
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        loadQueryWithDelay(newQuery, 100000000);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        loadQueryWithDelay(query, 0);
        return true;
    }

    private void loadQueryWithDelay(String query, long delay) {
        mHandler.removeCallbacks(mDelayedLoad);
        if (!TextUtils.isEmpty(query)) {
            mQuery = query;
            mHandler.postDelayed(mDelayedLoad, delay);
        }
    }

    private void getQueryData() {
        final String query = mQuery;
        
        SearchAdvancedHelper searchHelper = SearchAdvancedHelper.getInstance();
        searchHelper.setSearchQuery(query);
        
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService searchApi = retrofit.create(ApiService.class);
        
        String queryParam = query;
        Integer genreId = searchHelper.getSelectedGenreId();
        Integer countryId = searchHelper.getSelectedCountryId();
        Integer yearFrom = searchHelper.getYearFrom();
        Integer yearTo = searchHelper.getYearTo();
        
        Call<SearchModel> call;
        if (genreId != null || countryId != null || yearFrom != null || yearTo != null) {
            call = searchApi.getSearchDataAdvanced(AppConfig.API_KEY, queryParam, 1, "movieserieslive", 
                                                   genreId, countryId, yearFrom, yearTo);
        } else {
            call = searchApi.getSearchData(AppConfig.API_KEY, queryParam, 1, "movieserieslive");
        }
        
        call.enqueue(new Callback<SearchModel>() {
            @Override
            public void onResponse(Call<SearchModel> call,  Response<SearchModel> response) {
                mItems = new ArrayList<>();

                List<Movie> movieResult = new ArrayList<>();
                List<Movie> tvSeriesResult = new ArrayList<>();
                List<TvModel> tvResult = new ArrayList<>();

                if (response.code() == 200 && response.body() != null) {
                    if (response.body().getTvChannels() != null && response.body().getTvChannels().size() != 0) {
                        tvHeader = "Live TV";
                        tvResult.clear();
                        tvResult = response.body().getTvChannels();
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
                        movieResult.clear();
                        movieResult = response.body().getMovie();
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
                        tvSeriesResult.clear();
                        tvSeriesResult = response.body().getTvseries();
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

                    loadRows(movieResult, tvSeriesResult, tvResult);
                    searchPhim4k(query);
                    searchKKPhim4k(query);
                } else {
                    showErrorToast("Lỗi tìm kiếm: " + response.code());
                }

            }

            @Override
            public void onFailure(Call<SearchModel> call, Throwable t) {
                showErrorToast("Lỗi mạng: " + t.getLocalizedMessage());
            }
        });

    }
    
    private void searchPhim4k(String query) {
        phim4kResults.clear();
        
        Phim4kClient.getInstance().searchMovies(query, new Phim4kClient.Phim4kCallback() {
            @Override
            public void onSuccess(List<VideoContent> videoContents) {
                if (videoContents != null && !videoContents.isEmpty()) {
                    phim4kResults.clear();
                    phim4kResults.addAll(videoContents);
                    
                    for (VideoContent videoContent : videoContents) {
                        String id = videoContent.getVideosId() != null ? videoContent.getVideosId() : videoContent.getId();
                        String title = videoContent.getTitle();
                        String description = videoContent.getDescription();
                        String type = videoContent.getIsTvseries() != null && videoContent.getIsTvseries().equals("1") ? "tvseries" : "movie";
                        String streamUrl = videoContent.getStreamUrl() != null ? videoContent.getStreamUrl() : "";
                        String streamFrom = "Phim4k";
                        String thumbnailUrl = videoContent.getThumbnailUrl();
                        mItems.add(new SearchContent(id, title, description, type, streamUrl, streamFrom, thumbnailUrl));
                    }
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> addPhim4kRowToAdapter());
                    }
                }
            }

            @Override
            public void onError(String error) { }
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
                }
                if (tvAdapter.size() > 0) {
                    listRows.add(new ListRow(new HeaderItem(tvHeader), tvAdapter));
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
                    getView().postDelayed(() -> {
                        try {
                            View resultsView = getView();
                            if (resultsView != null) {
                                View resultsList = findViewByName(resultsView, "results");
                                if (resultsList == null) {
                                    resultsView.requestFocus();
                                } else {
                                    resultsList.requestFocus();
                                }
                            }
                        } catch (Exception e) {
                            // ignore
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
        if (phim4kResults.isEmpty()) return;
        
        for (int i = 0; i < mRowsAdapter.size(); i++) {
            Object row = mRowsAdapter.get(i);
            if (row instanceof ListRow) {
                ListRow listRow = (ListRow) row;
                if ("Nguồn free1".equals(listRow.getHeaderItem().getName())) {
                    return;
                }
            }
        }
        
        ArrayObjectAdapter phim4kAdapter = new ArrayObjectAdapter(new SearchCardPresenter());
        
        for (SearchContent searchContent : mItems) {
            if ("Phim4k".equals(searchContent.getStreamFrom())) {
                phim4kAdapter.add(searchContent);
            }
        }
        
        if (phim4kAdapter.size() > 0) {
            HeaderItem phim4kHeaderItem = new HeaderItem("Nguồn free1");
            ListRow phim4kRow = new ListRow(phim4kHeaderItem, phim4kAdapter);
            mRowsAdapter.add(phim4kRow);
        }
    }

    private void searchKKPhim4k(String query) {
        kkPhim4kResults.clear();
        
        KKPhim4kClient.getInstance().searchMovies(query, new KKPhim4kClient.KKPhim4kCallback() {
            @Override
            public void onSuccess(List<VideoContent> videoContents) {
                if (videoContents != null && !videoContents.isEmpty()) {
                    kkPhim4kResults.clear();
                    kkPhim4kResults.addAll(videoContents);
                    
                    for (VideoContent videoContent : videoContents) {
                        String id = videoContent.getVideosId() != null ? videoContent.getVideosId() : videoContent.getId();
                        String title = videoContent.getTitle();
                        String description = videoContent.getDescription();
                        String type = videoContent.getIsTvseries() != null && videoContent.getIsTvseries().equals("1") ? "tvseries" : "movie";
                        String streamUrl = videoContent.getStreamUrl() != null ? videoContent.getStreamUrl() : "";
                        String streamFrom = "KKPhim4k";
                        String thumbnailUrl = videoContent.getPosterUrl();
                        if (thumbnailUrl == null || thumbnailUrl.isEmpty()) {
                            thumbnailUrl = videoContent.getThumbnailUrl();
                        }
                        mItems.add(new SearchContent(id, title, description, type, streamUrl, streamFrom, thumbnailUrl));
                    }
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> addKKPhim4kRowToAdapter());
                    }
                }
            }

            @Override
            public void onError(String error) { }
        });
    }

    private void addKKPhim4kRowToAdapter() {
        if (kkPhim4kResults.isEmpty()) return;
        
        for (int i = 0; i < mRowsAdapter.size(); i++) {
            Object row = mRowsAdapter.get(i);
            if (row instanceof ListRow) {
                ListRow listRow = (ListRow) row;
                if ("Nguồn Free 2".equals(listRow.getHeaderItem().getName())) {
                    return;
                }
            }
        }
        
        ArrayObjectAdapter kkPhim4kAdapter = new ArrayObjectAdapter(new SearchCardPresenter());
        
        for (SearchContent searchContent : mItems) {
            if ("KKPhim4k".equals(searchContent.getStreamFrom())) {
                kkPhim4kAdapter.add(searchContent);
            }
        }
        
        if (kkPhim4kAdapter.size() > 0) {
            HeaderItem kkPhim4kHeaderItem = new HeaderItem("Nguồn Free 2");
            ListRow kkPhim4kRow = new ListRow(kkPhim4kHeaderItem, kkPhim4kAdapter);
            mRowsAdapter.add(kkPhim4kRow);
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
        if (query == null || query.isEmpty()) return;
        setSearchQuery(query, true);
    }
    
    /**
     * Refresh search with current query and updated filters
     * Called from SearchActivity when filters change
     */
    public void refreshSearch() {
        String currentQuery = mQuery;
        if (currentQuery == null || currentQuery.isEmpty()) return;
        
        mRowsAdapter.clear();
        getQueryData();
    }
}