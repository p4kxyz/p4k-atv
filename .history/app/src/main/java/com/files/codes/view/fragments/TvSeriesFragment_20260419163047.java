package com.files.codes.view.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.files.codes.AppConfig;
import com.files.codes.R;
import com.files.codes.database.TvSeries.TvSeriesViewModel;
import com.files.codes.model.CountryModel;
import com.files.codes.model.Genre;
import com.files.codes.model.Movie;
import com.files.codes.model.MovieList;
import com.files.codes.model.api.ApiService;
import com.files.codes.utils.BackgroundHelper;
import com.files.codes.utils.NetworkInst;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.view.ErrorActivity;
import com.files.codes.view.VideoDetailsActivity;
import com.files.codes.view.fragments.testFolder.GridFragment;
import com.files.codes.view.fragments.testFolder.HomeNewActivity;
import com.files.codes.view.presenter.VerticalCardPresenter;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class TvSeriesFragment extends GridFragment {
    public static final String TV_SERIES = "tv_series";
    private static final String TAG = TvSeriesFragment.class.getSimpleName();
    private static final int NUM_COLUMNS = 5;
    private static final int IMAGE_PREFETCH_COUNT = 18;
    /*Page count is here 2
    Because for room database page 1 data is already loaded*/
    private int pageCount = 1;
    private boolean dataAvailable = true;
    private BackgroundHelper bgHelper;
    private List<Movie> movies = new ArrayList<>();
    private ArrayObjectAdapter mAdapter;
    private HomeNewActivity activity;
    private TvSeriesViewModel tvSeriesViewModel;
    private FilterItem filterItem = new FilterItem();

    // Filter data
    private List<Genre> genreList = new ArrayList<>();
    private List<CountryModel> countryList = new ArrayList<>();

    // Filter state
    private String filterGenreId = null;
    private String filterCountryId = null;
    private String filterYearFrom = null;
    private String filterYearTo = null;
    private boolean isFilterActive = false;
    private String selectedGenreName = "Tất Cả";
    private String selectedCountryName = "Tất Cả";
    private String selectedYearName = "Tất Cả";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (HomeNewActivity) getActivity();

        setOnItemViewClickedListener(getDefaultItemViewClickedListener());
        setOnItemViewSelectedListener(getDefaultItemSelectedListener());

        // setup
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter(androidx.leanback.widget.FocusHighlight.ZOOM_FACTOR_NONE, false);
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);

        ClassPresenterSelector presenterSelector = new ClassPresenterSelector();
        presenterSelector.addClassPresenter(FilterItem.class, new FilterCardPresenter());
        presenterSelector.addClassPresenter(SpacerItem.class, new SpacerPresenter());
        presenterSelector.addClassPresenter(Movie.class, new VerticalCardPresenter(TV_SERIES));
        mAdapter = new ArrayObjectAdapter(presenterSelector);
        mAdapter.add(filterItem);
        for (int i = 0; i < NUM_COLUMNS - 1; i++) mAdapter.add(new SpacerItem());
        setAdapter(mAdapter);

        loadGenresFromAPI();
        loadCountriesFromAPI();

        fetchTvSereisData(pageCount);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof com.files.codes.view.fragments.testFolder.HomeNewActivity) {
             ((com.files.codes.view.fragments.testFolder.HomeNewActivity) getActivity()).setOrbsVisibility(false);
        }
    }

    // click listener
    private OnItemViewClickedListener getDefaultItemViewClickedListener() {
        return new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder viewHolder, Object o,
                                      RowPresenter.ViewHolder viewHolder2, Row row) {

                if (o instanceof FilterItem) {
                    showMainFilterDialog();
                    return;
                }
                Movie movie = (Movie) o;

                Intent intent = new Intent(getActivity(), com.files.codes.view.HeroStyleVideoDetailsActivity.class);
                intent.putExtra("id", movie.getVideosId());
                intent.putExtra("type", "tvseries");
                intent.putExtra("thumbImage", movie.getThumbnailUrl());
                
                startActivity(intent);

            }
        };
    }

    // selected listener for setting blur background each time when the item will select.
    protected OnItemViewSelectedListener getDefaultItemSelectedListener() {
        return new OnItemViewSelectedListener() {
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, final Object item,
                                       RowPresenter.ViewHolder rowViewHolder, Row row) {
                // pagination - load when reaching last row
                if (item instanceof FilterItem || item instanceof SpacerItem) return;
                if (dataAvailable) {
                    int itemPos = mAdapter.indexOf(item);
                    if (itemPos >= mAdapter.size() - NUM_COLUMNS) {
                        dataAvailable = false;
                        pageCount++;
                        fetchTvSereisData(pageCount);
                    }
                } else {
                }

                //Log.d("iamge url: ------------------------------", itemPos+" : "+ movies.size());
                // change the background color when the item will select
                if (item instanceof Movie) {
                    bgHelper = new BackgroundHelper(getActivity());
                    bgHelper.prepareBackgroundManager();
                    bgHelper.startBackgroundTimer(((Movie) item).getThumbnailUrl());

                }

            }
        };
    }
    public void fetchTvSereisData(int pageCount) {
        if (!new NetworkInst(activity).isNetworkAvailable()) {
            Intent intent = new Intent(activity, ErrorActivity.class);
            startActivity(intent);
            activity.finish();
            return;
        }

        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        boolean isFilterActive = filterGenreId != null || filterCountryId != null
                || filterYearFrom != null || filterYearTo != null;
        Call<List<Movie>> call;
        if (isFilterActive) {
            call = api.getLatestTvSeries(AppConfig.API_KEY, 20,
                    filterGenreId, filterCountryId, filterYearFrom, filterYearTo, pageCount);
        } else {
            call = api.getTvSeries(AppConfig.API_KEY, pageCount);
        }
        call.enqueue(new Callback<List<Movie>>() {
            @Override
            public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                if (response.code() == 200) {
                    List<Movie> movieList = response.body();
                    if (movieList.size() == 0) {
                        dataAvailable = false;
                        if (pageCount != 1) {
                            Toast.makeText(activity, getResources().getString(R.string.no_data_found), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        dataAvailable = true;
                    }

                    populateView(movieList);

                }
            }

            @Override
            public void onFailure(Call<List<Movie>> call, Throwable t) {
                t.printStackTrace();
                dataAvailable = true;  // allow retry
            }
        });
    }

    private void populateView(List<Movie> tvSeriesList) {
        if (tvSeriesList == null || tvSeriesList.size() == 0) return;
        HashSet<String> existingIds = new HashSet<>();
        for (Movie m : movies) existingIds.add(m.getVideosId());
        int added = 0;
        for (Movie tvSeries : tvSeriesList) {
            if (!existingIds.contains(tvSeries.getVideosId())) {
                mAdapter.add(tvSeries);
                movies.add(tvSeries);
                added++;
            }
        }
        if (added == 0) {
            dataAvailable = false;  // all duplicates → last page
            return;
        }
        prefetchSeriesImages(tvSeriesList);
        mAdapter.notifyArrayItemRangeChanged(mAdapter.size() - added, added);
    }

    private void prefetchSeriesImages(List<Movie> seriesList) {
        int limit = Math.min(seriesList.size(), IMAGE_PREFETCH_COUNT);
        for (int i = 0; i < limit; i++) {
            Movie movie = seriesList.get(i);
            String imageUrl = movie.getThumbnailUrl();
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                imageUrl = movie.getPosterUrl();
            }
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                Picasso.get().load(imageUrl).fetch();
            }
        }
    }

    // ========== FILTER DIALOG ==========

    // ── Custom dark dialog (giống Cài đặt nhanh) ──────────────────────────
    private android.app.Dialog buildFilterDialog(
            String title, String[] items, int checkedIndex,
            Runnable[] actions, Runnable onCancel) {
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        if (onCancel != null) dialog.setOnCancelListener(d -> onCancel.run());

        android.widget.LinearLayout root = new android.widget.LinearLayout(requireContext());
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1E1E2E);
        root.setPadding(dp(20), dp(20), dp(20), dp(12));

        android.widget.TextView titleView = new android.widget.TextView(requireContext());
        titleView.setText(title);
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
        titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleView.setPadding(dp(4), 0, dp(4), dp(10));
        root.addView(titleView);

        android.view.View divider = new android.view.View(requireContext());
        divider.setBackgroundColor(0x55FFFFFF);
        android.widget.LinearLayout.LayoutParams divLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divLp.bottomMargin = dp(6);
        root.addView(divider, divLp);

        android.widget.ScrollView sv = new android.widget.ScrollView(requireContext());
        sv.setVerticalScrollBarEnabled(false);
        android.widget.LinearLayout ll = new android.widget.LinearLayout(requireContext());
        ll.setOrientation(android.widget.LinearLayout.VERTICAL);

        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            boolean isSelected = (checkedIndex >= 0 && i == checkedIndex);
            android.widget.TextView tv = new android.widget.TextView(requireContext());
            tv.setText(isSelected ? "✓  " + items[i] : "     " + items[i]);
            tv.setTextColor(isSelected ? 0xFF64B5F6 : 0xFFDDDDDD);
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
            tv.setPadding(dp(10), dp(13), dp(10), dp(13));
            tv.setFocusable(true);
            tv.setFocusableInTouchMode(false);
            tv.setClickable(true);
            tv.setBackground(null);
            tv.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackgroundColor(0x664FC3F7);
                    ((android.widget.TextView) v).setTextColor(0xFFFFFFFF);
                } else {
                    v.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                    boolean sel = (checkedIndex >= 0 && idx == checkedIndex);
                    ((android.widget.TextView) v).setTextColor(sel ? 0xFF64B5F6 : 0xFFDDDDDD);
                }
            });
            tv.setOnClickListener(v -> {
                dialog.dismiss();
                if (actions != null && idx < actions.length && actions[idx] != null)
                    actions[idx].run();
            });
            ll.addView(tv);
            android.view.View sep = new android.view.View(requireContext());
            sep.setBackgroundColor(0x22FFFFFF);
            ll.addView(sep, new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1));
        }
        sv.addView(ll);
        root.addView(sv, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(400)));

        dialog.setContentView(root);
        android.view.Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout(dp(480), android.view.WindowManager.LayoutParams.WRAP_CONTENT);
            w.setGravity(android.view.Gravity.CENTER);
            w.setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void showMainFilterDialog() {
        boolean hasFilter = filterGenreId != null || filterCountryId != null || filterYearFrom != null;
        String applyLabel = hasFilter ? "✅ Áp Dụng Bộ Lọc" : "✅ Áp Dụng (Tất Cả)";
        String[] items = {
            applyLabel,
            "Thể Loại:   " + selectedGenreName,
            "Quốc Gia:  " + selectedCountryName,
            "Năm:          " + selectedYearName,
            "— Xóa Tất Cả Bộ Lọc —"
        };
        Runnable[] actions = {
            this::applyFilterAndLoad,
            this::showGenreFilterDialog,
            this::showCountryFilterDialog,
            this::showYearFilterDialog,
            this::clearAllFilters
        };
        buildFilterDialog("⚙️ Bộ Lọc", items, -1, actions, null).show();
    }

    private void applyFilterAndLoad() {
        refreshTvSeries();
    }

    private void showGenreFilterDialog() {
        if (genreList.isEmpty()) {
            Toast.makeText(getContext(), "Đang tải danh sách thể loại...", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] arr = new String[genreList.size() + 1];
        arr[0] = "Tất Cả Thể Loại";
        for (int i = 0; i < genreList.size(); i++) {
            arr[i + 1] = genreList.get(i).getName();
        }
        Runnable[] actions = new Runnable[arr.length];
        actions[0] = () -> applyGenreFilter(null, "Tất Cả");
        for (int i = 1; i < arr.length; i++) {
            final Genre genre = genreList.get(i - 1);
            actions[i] = () -> applyGenreFilter(genre.getGenreId(), genre.getName());
        }
        buildFilterDialog("Chọn Thể Loại", arr, -1, actions, this::showMainFilterDialog).show();
    }

    private void showCountryFilterDialog() {
        if (countryList.isEmpty()) {
            Toast.makeText(getContext(), "Đang tải danh sách quốc gia...", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] arr = new String[countryList.size() + 1];
        arr[0] = "Tất Cả Quốc Gia";
        for (int i = 0; i < countryList.size(); i++) {
            arr[i + 1] = countryList.get(i).getName();
        }
        Runnable[] actions = new Runnable[arr.length];
        actions[0] = () -> applyCountryFilter(null, "Tất Cả");
        for (int i = 1; i < arr.length; i++) {
            final CountryModel country = countryList.get(i - 1);
            actions[i] = () -> applyCountryFilter(country.getCountryId(), country.getName());
        }
        buildFilterDialog("Chọn Quốc Gia", arr, -1, actions, this::showMainFilterDialog).show();
    }

    private void showYearFilterDialog() {
        String[] labels = {
            "Tất Cả Năm", "2026", "2025", "2024", "2023", "2022",
            "2021", "2020", "2015 - 2019", "2010 - 2014", "2000 - 2009", "Trước 2000"
        };
        String[] from = { null, "2026", "2025", "2024", "2023", "2022",
            "2021", "2020", "2015", "2010", "2000", "1900" };
        String[] to = { null, "2026", "2025", "2024", "2023", "2022",
            "2021", "2020", "2019", "2014", "2009", "1999" };
        Runnable[] actions = new Runnable[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            actions[i] = () -> applyYearFilter(from[idx], to[idx], labels[idx]);
        }
        buildFilterDialog("Chọn Năm", labels, -1, actions, this::showMainFilterDialog).show();
    }

    private void applyGenreFilter(String genreId, String name) {
        this.filterGenreId = genreId;
        this.selectedGenreName = name;
        updateFilterActive();
        updateActiveFiltersDisplay();
        showMainFilterDialog();
    }

    private void applyCountryFilter(String countryId, String name) {
        this.filterCountryId = countryId;
        this.selectedCountryName = name;
        updateFilterActive();
        updateActiveFiltersDisplay();
        showMainFilterDialog();
    }

    private void applyYearFilter(String from, String to, String label) {
        this.filterYearFrom = from;
        this.filterYearTo = to;
        this.selectedYearName = label;
        updateFilterActive();
        updateActiveFiltersDisplay();
        showMainFilterDialog();
    }

    private void updateFilterActive() {
        this.isFilterActive = (filterGenreId != null || filterCountryId != null || filterYearFrom != null);
    }

    private void clearAllFilters() {
        filterGenreId = null; filterCountryId = null;
        filterYearFrom = null; filterYearTo = null;
        selectedGenreName = "Tất Cả"; selectedCountryName = "Tất Cả"; selectedYearName = "Tất Cả";
        isFilterActive = false;
        refreshTvSeries();
        updateActiveFiltersDisplay();
        Toast.makeText(getContext(), "Đã xóa bộ lọc", Toast.LENGTH_SHORT).show();
    }

    private void refreshTvSeries() {
        movies.clear();
        mAdapter.clear();
        mAdapter.add(filterItem);
        for (int i = 0; i < NUM_COLUMNS - 1; i++) mAdapter.add(new SpacerItem());
        pageCount = 1;
        dataAvailable = true;
        fetchTvSereisData(pageCount);
    }

    private void updateActiveFiltersDisplay() {
        filterItem.genreText   = (filterGenreId   != null) ? "\uD83C\uDF9E\uFE0F " + selectedGenreName   : "";
        filterItem.countryText = (filterCountryId != null) ? "\uD83C\uDF0D " + selectedCountryName : "";
        filterItem.yearText    = (filterYearFrom  != null) ? "\uD83D\uDCC5 " + selectedYearName    : "";
        filterItem.activeFiltersText = filterItem.genreText + filterItem.countryText + filterItem.yearText;
        mAdapter.notifyItemRangeChanged(0, 1);
    }

    private void loadGenresFromAPI() {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        api.getGenres(AppConfig.API_KEY).enqueue(new Callback<List<Genre>>() {
            @Override
            public void onResponse(Call<List<Genre>> call, Response<List<Genre>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    genreList.clear();
                    genreList.addAll(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<Genre>> call, Throwable t) {
            }
        });
    }

    private void loadCountriesFromAPI() {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        api.getAllCountry(AppConfig.API_KEY).enqueue(new Callback<List<CountryModel>>() {
            @Override
            public void onResponse(Call<List<CountryModel>> call, Response<List<CountryModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    countryList.clear();
                    countryList.addAll(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<CountryModel>> call, Throwable t) {
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        movies = new ArrayList<>();
        pageCount = 1;
        dataAvailable = true;
    }

}
