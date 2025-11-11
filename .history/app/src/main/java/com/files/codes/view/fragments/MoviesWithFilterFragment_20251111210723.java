package com.files.codes.view.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.files.codes.model.CountryModel;
import com.files.codes.model.Genre;
import com.files.codes.model.Movie;
import com.files.codes.model.MovieList;
import com.files.codes.model.api.ApiService;
import com.files.codes.utils.NetworkInst;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.view.ErrorActivity;
import com.files.codes.view.VideoDetailsActivity;
import com.files.codes.view.HeroStyleVideoDetailsActivity;
import com.files.codes.view.fragments.testFolder.GridFragment;
import com.files.codes.view.fragments.testFolder.HomeNewActivity;
import com.files.codes.view.presenter.VerticalCardPresenter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Movies Fragment with integrated filter bar on top
 */
public class MoviesWithFilterFragment extends GridFragment {
    public static final String MOVIE = "movie";
    private static final String TAG = "MoviesWithFilter";
    private static final int NUM_COLUMNS = 8;
    
    private int pageCount = 2;
    private boolean dataAvailable = true;
    private List<Movie> movies = new ArrayList<>();
    private ArrayObjectAdapter mAdapter;
    private HomeNewActivity activity;
    private MovieViewModel movieViewModel;
    // Filter UI components
    private LinearLayout filterPanel;
    private Button btnFilterGenre;
    private Button btnFilterCountry;
    private Button btnFilterYear;
    private Button btnClearFilters;
    private TextView tvActiveFilters;
    
    // Filter data
    private List<Genre> genreList = new ArrayList<>();
    private List<String> genreNames = new ArrayList<>();
    private List<String> genreIds = new ArrayList<>();
    
    private List<CountryModel> countryList = new ArrayList<>();
    private List<String> countryNames = new ArrayList<>();
    private List<String> countryIds = new ArrayList<>();
    
    // Filter state
    private String filterGenreId = null;
    private String filterCountryId = null;
    private boolean isFilterActive = false;
    private String selectedGenreName = "Tất Cả";
    private String selectedCountryName = "Tất Cả";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        activity = (HomeNewActivity) getActivity();
        
        setOnItemViewClickedListener(getDefaultItemViewClickedListener());
        setOnItemViewSelectedListener(getDefaultItemSelectedListener());

        // Setup grid
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);

        mAdapter = new ArrayObjectAdapter(new VerticalCardPresenter(MOVIE));
        setAdapter(mAdapter);

        // Get data from local database
        movieViewModel = new ViewModelProvider(getActivity()).get(MovieViewModel.class);
        movieViewModel.getMovieLiveData().observe(getActivity(), new Observer<MovieList>() {
            @Override
            public void onChanged(MovieList movieList) {
                if (movieList != null){
                    populateView(movieList.getMovieList());
                }
            }
        });
        
        // Load filter data
        loadGenresFromAPI();
        loadCountriesFromAPI();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.e(TAG, "🔴 onCreateView - Getting super's view first!");
        // MUST call super to let GridFragment setup properly
        return super.onCreateView(inflater, container, savedInstanceState);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        Log.e(TAG, "🔴 onViewCreated - Injecting filter bar!");
        
        // Now inject filter bar into the view hierarchy
        if (view.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view.getParent();
            Log.e(TAG, "🔴 Parent class: " + parent.getClass().getSimpleName());
            
            // Remove view from parent temporarily
            parent.removeView(view);
            
            // Create wrapper with filter bar
            LinearLayout wrapper = new LinearLayout(getContext());
            wrapper.setOrientation(LinearLayout.VERTICAL);
            wrapper.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ));
            
            // Create and add filter bar
            LinearLayout filterBar = createFilterBarProgrammatically();
            wrapper.addView(filterBar);
            
            // Add original view back
            ViewGroup.LayoutParams originalParams = view.getLayoutParams();
            if (originalParams == null) {
                originalParams = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                );
            }
            wrapper.addView(view, originalParams);
            
            // Add wrapper back to parent
            parent.addView(wrapper);
            
            Log.e(TAG, "✅ Filter bar injected successfully!");
        } else {
            Log.e(TAG, "❌ View has no parent!");
        }
    }
    
    private LinearLayout createFilterBarProgrammatically() {
        Context ctx = getContext();
        
        LinearLayout filterBar = new LinearLayout(ctx);
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        filterBar.setLayoutParams(barParams);
        filterBar.setOrientation(LinearLayout.HORIZONTAL);
        filterBar.setBackgroundColor(0xFF2196F3); // Blue
        filterBar.setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4));
        filterBar.setElevation(dpToPx(2));
        filterBar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        // Title
        TextView title = new TextView(ctx);
        title.setText("BỘ LỌC:");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(12);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMarginEnd(dpToPx(8));
        title.setLayoutParams(titleParams);
        filterBar.addView(title);
        
        // Genre button
        btnFilterGenre = createFilterButtonSimple("Thể Loại");
        btnFilterGenre.setOnClickListener(v -> showGenreFilterDialog());
        filterBar.addView(btnFilterGenre);
        
        // Country button
        btnFilterCountry = createFilterButtonSimple("Quốc Gia");
        btnFilterCountry.setOnClickListener(v -> showCountryFilterDialog());
        filterBar.addView(btnFilterCountry);
        
        // Year button
        btnFilterYear = createFilterButtonSimple("Năm");
        btnFilterYear.setOnClickListener(v -> showYearFilterDialog());
        filterBar.addView(btnFilterYear);
        
        // Clear button
        btnClearFilters = new Button(ctx);
        btnClearFilters.setText("Xóa Lọc");
        btnClearFilters.setTextColor(0xFFFFFFFF);
        btnClearFilters.setTextSize(11);
        btnClearFilters.setBackgroundColor(0xFFE53935); // Red
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            dpToPx(32)
        );
        clearParams.setMarginStart(dpToPx(8));
        btnClearFilters.setLayoutParams(clearParams);
        btnClearFilters.setOnClickListener(v -> clearAllFilters());
        filterBar.addView(btnClearFilters);
        
        // Active filters text
        tvActiveFilters = new TextView(ctx);
        tvActiveFilters.setTextColor(0xFFFFFFFF);
        tvActiveFilters.setTextSize(11);
        LinearLayout.LayoutParams activeParams = new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1.0f
        );
        activeParams.setMarginStart(dpToPx(12));
        tvActiveFilters.setLayoutParams(activeParams);
        tvActiveFilters.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
        filterBar.addView(tvActiveFilters);
        
        Log.e(TAG, "✅ Filter bar created programmatically!");
        return filterBar;
    }
    
    private Button createFilterButtonSimple(String text) {
        Button btn = new Button(getContext());
        btn.setText(text);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTextSize(11);
        btn.setBackgroundColor(0xFF4CAF50); // Green
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            dpToPx(32)
        );
        params.setMarginEnd(dpToPx(6));
        btn.setLayoutParams(params);
        btn.setFocusable(true);
        btn.setFocusableInTouchMode(true);
        return btn;
    }
    
    private LinearLayout createFilterBar() {
        Context ctx = getContext();
        
        // Main filter panel
        LinearLayout panel = new LinearLayout(ctx);
        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        panel.setLayoutParams(panelParams);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setBackgroundColor(0xFF2196F3); // Blue background to make it visible
        panel.setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4)); // Reduced padding: 4dp top/bottom
        panel.setElevation(dpToPx(2)); // Reduced elevation
        panel.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        // Title
        TextView title = new TextView(ctx);
        title.setText("🔍 Bộ Lọc:");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(12); // Reduced from 14
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMarginEnd(dpToPx(8)); // Reduced from 12
        title.setLayoutParams(titleParams);
        panel.addView(title);
        
        // Genre button
        btnFilterGenre = createFilterButton("🎬 Thể Loại", 100);
        panel.addView(btnFilterGenre);
        
        // Country button
        btnFilterCountry = createFilterButton("🌍 Quốc Gia", 100);
        panel.addView(btnFilterCountry);
        
        // Year button
        btnFilterYear = createFilterButton("📅 Năm", 80);
        panel.addView(btnFilterYear);
        
        // Clear button
        btnClearFilters = createClearButton();
        panel.addView(btnClearFilters);
        
        // Active filter display
        tvActiveFilters = new TextView(ctx);
        tvActiveFilters.setTextColor(0xFFFFC107);
        tvActiveFilters.setTextSize(11);
        tvActiveFilters.setTypeface(null, android.graphics.Typeface.BOLD);
        tvActiveFilters.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        tvActiveFilters.setBackgroundColor(0xFF2a2a2a);
        tvActiveFilters.setVisibility(View.GONE);
        panel.addView(tvActiveFilters);
        
        // Setup click listeners
        setupFilterButtons();
        
        Log.d(TAG, "🎨 Filter bar created programmatically");
        
        return panel;
    }
    
    private Button createFilterButton(String text, int minWidthDp) {
        Context ctx = getContext();
        Button btn = new Button(ctx);
        btn.setText(text);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTextSize(11); // Reduced from 12
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        btn.setBackgroundColor(0xFF4CAF50); // Green background
        btn.setMinWidth(dpToPx(minWidthDp));
        btn.setFocusable(true);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            dpToPx(32) // Reduced from 40 to 32dp height
        );
        params.setMarginEnd(dpToPx(6)); // Reduced from 8
        btn.setLayoutParams(params);
        
        return btn;
    }
    
    private Button createClearButton() {
        Context ctx = getContext();
        Button btn = new Button(ctx);
        btn.setText("✖️ Xóa");
        btn.setTextColor(0xFFFFFFFF);
        btn.setTextSize(11); // Reduced from 12
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        btn.setBackgroundColor(0xFFE53935); // Red background
        btn.setMinWidth(dpToPx(70)); // Reduced from 80
        btn.setFocusable(true);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            dpToPx(32) // Reduced from 40 to 32dp height
        );
        params.setMarginEnd(dpToPx(8)); // Reduced from 12
        btn.setLayoutParams(params);
        
        return btn;
    }
    
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    private void setupFilterButtons() {
        // Genre filter button
        btnFilterGenre.setOnClickListener(v -> showGenreFilterDialog());
        
        // Country filter button
        btnFilterCountry.setOnClickListener(v -> showCountryFilterDialog());
        
        // Year filter button (not implemented)
        btnFilterYear.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Bộ lọc năm sẽ được thêm sau", Toast.LENGTH_SHORT).show();
        });
        
        // Clear filters button
        btnClearFilters.setOnClickListener(v -> clearAllFilters());
    }
    
    private void showGenreFilterDialog() {
        if (genreNames.isEmpty()) {
            Toast.makeText(getContext(), "Đang tải danh sách thể loại...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Chọn Thể Loại");
        
        String[] genreArray = genreNames.toArray(new String[0]);
        
        builder.setItems(genreArray, (dialog, which) -> {
            if (which == 0) {
                // "Tất Cả" selected
                applyGenreFilter(null, "Tất Cả");
            } else {
                // Specific genre selected
                String selectedId = genreIds.get(which);
                String selectedName = genreNames.get(which);
                applyGenreFilter(selectedId, selectedName);
            }
        });
        
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }
    
    private void showCountryFilterDialog() {
        if (countryNames.isEmpty()) {
            Toast.makeText(getContext(), "Đang tải danh sách quốc gia...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Chọn Quốc Gia");
        
        String[] countryArray = countryNames.toArray(new String[0]);
        
        builder.setItems(countryArray, (dialog, which) -> {
            if (which == 0) {
                // "Tất Cả" selected
                applyCountryFilter(null, "Tất Cả");
            } else {
                // Specific country selected
                String selectedId = countryIds.get(which);
                String selectedName = countryNames.get(which);
                applyCountryFilter(selectedId, selectedName);
            }
        });
        
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }
    
    private void showYearFilterDialog() {
        Toast.makeText(getContext(), "Tính năng lọc theo năm đang phát triển", Toast.LENGTH_SHORT).show();
    }
    
    private void applyGenreFilter(String genreId, String genreName) {
        Log.d(TAG, "🎬 Applying genre filter: " + genreName + " (ID: " + genreId + ")");
        this.filterGenreId = genreId;
        this.filterCountryId = null;
        this.selectedGenreName = genreName;
        this.isFilterActive = (genreId != null);
        refreshMovies();
        updateActiveFiltersDisplay();
    }
    
    private void applyCountryFilter(String countryId, String countryName) {
        Log.d(TAG, "🌍 Applying country filter: " + countryName + " (ID: " + countryId + ")");
        this.filterCountryId = countryId;
        this.filterGenreId = null;
        this.selectedCountryName = countryName;
        this.isFilterActive = (countryId != null);
        refreshMovies();
        updateActiveFiltersDisplay();
    }
    
    private void clearAllFilters() {
        Log.d(TAG, "✖️ Clearing all filters");
        
        this.filterGenreId = null;
        this.filterCountryId = null;
        this.selectedGenreName = "Tất Cả";
        this.selectedCountryName = "Tất Cả";
        this.isFilterActive = false;
        
        refreshMovies();
        updateActiveFiltersDisplay();
        
        Toast.makeText(getContext(), "Đã xóa bộ lọc", Toast.LENGTH_SHORT).show();
    }
    
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
    
    private void updateActiveFiltersDisplay() {
        if (filterGenreId != null || filterCountryId != null) {
            String filterText = "";
            if (filterGenreId != null) {
                filterText = "🎬 " + selectedGenreName;
            } else if (filterCountryId != null) {
                filterText = "🌍 " + selectedCountryName;
            }
            tvActiveFilters.setText(filterText);
            tvActiveFilters.setVisibility(View.VISIBLE);
        } else {
            tvActiveFilters.setVisibility(View.GONE);
        }
    }
    
    // ========== GRID FRAGMENT METHODS (from MoviesFragment) ==========
    
    private OnItemViewClickedListener getDefaultItemViewClickedListener() {
        return new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder viewHolder, Object o,
                                      RowPresenter.ViewHolder viewHolder2, Row row) {
                Movie movie = (Movie) o;
                Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
                intent.putExtra("id", movie.getVideosId());
                intent.putExtra("type", "movie");
                intent.putExtra("thumbImage", movie.getThumbnailUrl());

                ImageView imageView = ((ImageCardView) viewHolder.view).getMainImageView();
                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                        imageView, VideoDetailsFragment.TRANSITION_NAME).toBundle();
                startActivity(intent, bundle);
            }
        };
    }

    protected OnItemViewSelectedListener getDefaultItemSelectedListener() {
        return new OnItemViewSelectedListener() {
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, final Object item,
                                       RowPresenter.ViewHolder rowViewHolder, Row row) {
                // Pagination
                if (dataAvailable) {
                    int itemPos = mAdapter.indexOf(item);
                    if (itemPos == movies.size() - 1) {
                        pageCount++;
                        fetchMovieData(pageCount);
                    }
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

        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        
        Call<List<Movie>> call;
        
        // Check if filter is active
        if (isFilterActive) {
            if (filterGenreId != null) {
                Log.d(TAG, "🔍 Fetching movies by genre: " + filterGenreId + ", page: " + pageCount);
                call = api.getMovieByGenre(AppConfig.API_KEY, filterGenreId, pageCount);
            } else if (filterCountryId != null) {
                Log.d(TAG, "🔍 Fetching movies by country: " + filterCountryId + ", page: " + pageCount);
                call = api.getMovieByCountry(AppConfig.API_KEY, filterCountryId, pageCount);
            } else {
                call = api.getMovies(AppConfig.API_KEY, pageCount);
            }
        } else {
            call = api.getMovies(AppConfig.API_KEY, pageCount);
        }
        
        call.enqueue(new Callback<List<Movie>>() {
            @Override
            public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                if (response.code() == 200) {
                    Log.e(TAG, "onResponse: " + response.code());
                    List<Movie> movieList = response.body();
                    if (movieList.size() <= 0) {
                        dataAvailable = false;
                        if (pageCount != 2) {
                            Toast.makeText(activity, getResources().getString(R.string.no_more_data_found), Toast.LENGTH_SHORT).show();
                        }
                    }
                    populateView(movieList);
                }
            }

            @Override
            public void onFailure(Call<List<Movie>> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(activity, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateView(List<Movie> movieList){
        if (movieList != null && movieList.size() > 0){
            for (Movie movie : movieList) {
                mAdapter.add(movie);
            }
            mAdapter.notifyArrayItemRangeChanged(movieList.size() - 1, movieList.size() + movies.size());
            movies.addAll(movieList);
            setAdapter(mAdapter);
            getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        movies = new ArrayList<>();
        pageCount = 1;
        dataAvailable = true;
    }
    
    // ========== API CALLS ==========
    
    private void loadGenresFromAPI() {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        
        Call<List<Genre>> call = api.getGenres(AppConfig.API_KEY, 1);
        call.enqueue(new Callback<List<Genre>>() {
            @Override
            public void onResponse(Call<List<Genre>> call, Response<List<Genre>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    genreList = response.body();
                    
                    // Add "Tất Cả" option at first position
                    genreNames.add("Tất Cả Thể Loại");
                    genreIds.add(null);
                    
                    for (Genre genre : genreList) {
                        genreNames.add(genre.getName());
                        genreIds.add(genre.getGenreId());
                    }
                    
                    Log.d(TAG, "✅ Loaded " + genreList.size() + " genres");
                }
            }
            
            @Override
            public void onFailure(Call<List<Genre>> call, Throwable t) {
                Log.e(TAG, "❌ Failed to load genres: " + t.getMessage());
            }
        });
    }
    
    private void loadCountriesFromAPI() {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        
        Call<List<CountryModel>> call = api.getAllCountry(AppConfig.API_KEY);
        call.enqueue(new Callback<List<CountryModel>>() {
            @Override
            public void onResponse(Call<List<CountryModel>> call, Response<List<CountryModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    countryList = response.body();
                    
                    // Add "Tất Cả" option at first position
                    countryNames.add("Tất Cả Quốc Gia");
                    countryIds.add(null);
                    
                    for (CountryModel country : countryList) {
                        countryNames.add(country.getName());
                        countryIds.add(country.getCountryId());
                    }
                    
                    Log.d(TAG, "✅ Loaded " + countryList.size() + " countries");
                }
            }
            
            @Override
            public void onFailure(Call<List<CountryModel>> call, Throwable t) {
                Log.e(TAG, "❌ Failed to load countries: " + t.getMessage());
            }
        });
    }
}
