package com.files.codes.view.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.files.codes.AppConfig;
import com.files.codes.R;
import com.files.codes.database.movie.MovieViewModel;
import com.files.codes.model.Movie;
import com.files.codes.model.MovieList;
import com.files.codes.model.api.ApiService;
import com.files.codes.utils.BackgroundHelper;
import com.files.codes.utils.NetworkInst;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.view.ErrorActivity;
import com.files.codes.view.fragments.testFolder.HomeNewActivity;
import com.files.codes.view.VideoDetailsActivity;
import com.files.codes.view.fragments.testFolder.GridFragment;
import com.files.codes.view.presenter.VerticalCardPresenter;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MoviesFragment extends GridFragment {
    public static final String MOVIE = "movie";
    private static final String TAG = MoviesFragment.class.getSimpleName();
    private static final int NUM_COLUMNS = 5;
    private static final int IMAGE_PREFETCH_COUNT = 18;
    private int pageCount = 2;
    private boolean dataAvailable = true;
    private BackgroundHelper bgHelper;
    private List<Movie> movies = new ArrayList<>();
    private ArrayObjectAdapter mAdapter;
    private HomeNewActivity activity;
    private MovieViewModel movieViewModel;
    
    // Filter variables
    private String filterGenreId = null;
    private String filterCountryId = null;
    private Integer filterYearFrom = null;
    private Integer filterYearTo = null;
    private boolean isFilterActive = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "onCreate: " );
        super.onCreate(savedInstanceState);
        activity = (HomeNewActivity) getActivity();
        //activity.hideLogo();
        //setTitle(getResources().getString(R.string.movie));
        //bgHelper = new BackgroundHelper(getActivity());
        setOnItemViewClickedListener(getDefaultItemViewClickedListener());
        setOnItemViewSelectedListener(getDefaultItemSelectedListener());

        // setup
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);

        mAdapter = new ArrayObjectAdapter(new VerticalCardPresenter(MOVIE));
        setAdapter(mAdapter);

        //get data from local database
        movieViewModel = new ViewModelProvider(getActivity()).get(MovieViewModel.class);
        movieViewModel.getMovieLiveData().observe(getActivity(), new Observer<MovieList>() {
            @Override
            public void onChanged(MovieList movieList) {
                if (movieList != null){
                    populateView(movieList.getMovieList());
                }
            }
        });

    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Prevent Leanback from dimming any cards
        if (view != null) {
            view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                clearAllCardDimming(view);
            });
        }
    }
    
    private void clearAllCardDimming(android.view.View v) {
        if (v == null) return;
        v.setAlpha(1.0f);
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                android.view.View child = vg.getChildAt(i);
                child.setAlpha(1.0f);
                if (child instanceof android.view.ViewGroup) {
                    clearAllCardDimming(child);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof HomeNewActivity) {
             ((HomeNewActivity) getActivity()).setOrbsVisibility(false);
        }
    }

    // click listener
    private OnItemViewClickedListener getDefaultItemViewClickedListener() {
        return new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder viewHolder, Object o,
                                      RowPresenter.ViewHolder viewHolder2, Row row) {
                Movie movie = (Movie) o;
                Intent intent = new Intent(getActivity(), com.files.codes.view.HeroStyleVideoDetailsActivity.class);
                intent.putExtra("id", movie.getVideosId());
                intent.putExtra("type", "movie");
                intent.putExtra("thumbImage", movie.getThumbnailUrl());

                Log.d("MoviesFragment", "Opening HeroStyleVideoDetails for movie: " + movie.getTitle() + " (ID: " + movie.getVideosId() + ")");
                startActivity(intent);

            }
        };
    }

    // selected listener for setting blur background each time when the item will select.
    protected OnItemViewSelectedListener getDefaultItemSelectedListener() {
        return new OnItemViewSelectedListener() {
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, final Object item,
                                       RowPresenter.ViewHolder rowViewHolder, Row row) {
                // pagination - load when reaching last row (not last item)
                if (dataAvailable) {
                    int itemPos = mAdapter.indexOf(item);
                    // Load more when reaching items in the last row
                    if (itemPos >= movies.size() - NUM_COLUMNS) {
                        pageCount++;
                        dataAvailable = false; // Prevent multiple calls
                        fetchMovieData(pageCount);
                    }
                }
                if (item instanceof Movie) {
                    bgHelper = new BackgroundHelper(getActivity());
                    bgHelper.prepareBackgroundManager();
                    bgHelper.startBackgroundTimer(((Movie) item).getThumbnailUrl());
                }
            }
        };
    }

    public void fetchMovieData(int pageCount) {

        if (!new NetworkInst(activity).isNetworkAvailable()) {
            Intent intent = new Intent(activity, ErrorActivity.class);
            startActivity(intent);
            activity.finish();
            return;
        }

//        final SpinnerFragment mSpinnerFragment = new SpinnerFragment();
//        final FragmentManager fm = getFragmentManager();
//        fm.beginTransaction().add(R.id.custom_frame_layout, mSpinnerFragment).commit();

        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        
        Call<List<Movie>> call;
        
        // Check if filter is active
        if (isFilterActive) {
            if (filterGenreId != null) {
                // Filter by genre
                Log.d(TAG, "🔍 Fetching movies by genre: " + filterGenreId + ", page: " + pageCount);
                call = api.getMovieByGenre(AppConfig.API_KEY, filterGenreId, pageCount);
            } else if (filterCountryId != null) {
                // Filter by country
                Log.d(TAG, "🔍 Fetching movies by country: " + filterCountryId + ", page: " + pageCount);
                call = api.getMovieByCountry(AppConfig.API_KEY, filterCountryId, pageCount);
            } else {
                // No filter, get all movies
                call = api.getMovies(AppConfig.API_KEY, pageCount);
            }
        } else {
            // No filter, get all movies
            call = api.getMovies(AppConfig.API_KEY, pageCount);
        }
        
        call.enqueue(new Callback<List<Movie>>() {
            @Override
            public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                if (response.code() == 200) {
                    Log.e(TAG, "onResponse: " + response.code() );
                    List<Movie> movieList = response.body();
                    if (movieList.size() <= 0) {
                        dataAvailable = false;
                        if (pageCount != 2) {
                            Toast.makeText(activity, getResources().getString(R.string.no_more_data_found), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        dataAvailable = true; // Reset to allow next page load
                    }
                    populateView(movieList);
                    // hide the spinner
                   // fm.beginTransaction().remove(mSpinnerFragment).commitAllowingStateLoss();
                }
            }

            @Override
            public void onFailure(Call<List<Movie>> call, Throwable t) {
                t.printStackTrace();
                // hide the spinner
                Toast.makeText(activity, t.getMessage(), Toast.LENGTH_SHORT).show();
               // fm.beginTransaction().remove(mSpinnerFragment).commitAllowingStateLoss();
            }
        });
    }

    private void populateView(List<Movie> movieList){
        if (movieList  != null && movieList.size() > 0){
            for (Movie movie : movieList) {
                mAdapter.add(movie);
            }
            mAdapter.notifyArrayItemRangeChanged(movieList.size() - 1, movieList.size() + movies.size());
            movies.addAll(movieList);
            prefetchMovieImages(movieList);
            setAdapter(mAdapter);
            getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());

        }
    }

    private void prefetchMovieImages(List<Movie> movieList) {
        int limit = Math.min(movieList.size(), IMAGE_PREFETCH_COUNT);
        for (int i = 0; i < limit; i++) {
            Movie movie = movieList.get(i);
            String imageUrl = movie.getThumbnailUrl();
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                imageUrl = movie.getPosterUrl();
            }
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                Picasso.get().load(imageUrl).fetch();
            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        movies = new ArrayList<>();
        pageCount = 1;
        dataAvailable = true;

    }
    
    // ========== FILTER METHODS ==========
    
    /**
     * Apply genre filter
     */
    public void applyGenreFilter(String genreId, String genreName) {
        Log.d(TAG, "🎬 Applying genre filter: " + genreName + " (ID: " + genreId + ")");
        this.filterGenreId = genreId;
        this.filterCountryId = null; // Clear country filter
        this.isFilterActive = (genreId != null);
        refreshMovies();
    }
    
    /**
     * Apply country filter
     */
    public void applyCountryFilter(String countryId, String countryName) {
        Log.d(TAG, "🌍 Applying country filter: " + countryName + " (ID: " + countryId + ")");
        this.filterCountryId = countryId;
        this.filterGenreId = null; // Clear genre filter
        this.isFilterActive = (countryId != null);
        refreshMovies();
    }
    
    /**
     * Apply year filter (not implemented yet - needs backend support)
     */
    public void applyYearFilter(Integer yearFrom, Integer yearTo) {
        Log.d(TAG, "📅 Year filter requested: " + yearFrom + " - " + yearTo + " (Not implemented)");
        Toast.makeText(activity, "Bộ lọc năm sẽ được thêm trong phiên bản sau", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Clear all filters and reload all movies
     */
    public void clearFilters() {
        Log.d(TAG, "✖️ Clearing all filters");
        this.filterGenreId = null;
        this.filterCountryId = null;
        this.filterYearFrom = null;
        this.filterYearTo = null;
        this.isFilterActive = false;
        refreshMovies();
    }
    
    /**
     * Refresh movies list with current filter
     */
    private void refreshMovies() {
        // Clear current list
        movies.clear();
        mAdapter.clear();
        
        // Reset pagination
        pageCount = 1;
        dataAvailable = true;
        
        // Fetch new data
        fetchMovieData(pageCount);
        
        Log.d(TAG, "🔄 Movies refreshed with filters");
    }
    
    /**
     * Check if any filter is active
     */
    public boolean isFilterActive() {
        return isFilterActive;
    }
    
    /**
     * Get current filter info for display
     */
    public String getActiveFilterInfo() {
        if (filterGenreId != null) {
            return "Thể Loại";
        } else if (filterCountryId != null) {
            return "Quốc Gia";
        }
        return "";
    }

}
