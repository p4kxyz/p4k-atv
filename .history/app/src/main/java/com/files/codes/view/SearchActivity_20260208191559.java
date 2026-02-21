package com.files.codes.view;


import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.files.codes.AppConfig;
import com.files.codes.R;
import com.files.codes.model.Country;
import com.files.codes.model.Genre;
import com.files.codes.model.api.ApiService;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.utils.SearchAdvancedHelper;
import com.files.codes.view.fragments.SearchFragment;
import com.files.codes.view.fragments.testFolder.LeanbackActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class SearchActivity extends LeanbackActivity {
    private static final String TAG = "SearchActivity";
    
    private SearchFragment searchFragment;
    private boolean keyboardJustHidden = false;
    
    // Filter UI components
    private LinearLayout filterPanel;
    private Button btnFilterGenre;
    private Button btnFilterCountry;
    private Button btnFilterYear;
    private Button btnFilterClear;
    private TextView tvActiveFilters;
    
    // Filter data
    private SearchAdvancedHelper searchHelper;
    private List<Genre> genreList = new ArrayList<>();
    private List<String> genreNames = new ArrayList<>();
    private List<String> genreIds = new ArrayList<>();
    private List<Country> countryList = new ArrayList<>();
    private List<String> countryNames = new ArrayList<>();
    private List<String> countryIds = new ArrayList<>();
    
    // Selected filters
    private Integer selectedGenreId = null;
    private String selectedGenreName = "Tất Cả Thể Loại";
    private Integer selectedCountryId = null;
    private String selectedCountryName = "Tất Cả Quốc Gia";
    private Integer selectedYearFrom = null;
    private Integer selectedYearTo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        
        // Log.d(TAG, "✅ SearchActivity created with integrated filters");
        
        searchFragment = (SearchFragment) getSupportFragmentManager().findFragmentById(R.id.search_fragment);
        searchHelper = SearchAdvancedHelper.getInstance();
        
        // Initialize filter UI
        initFilterViews();
        loadGenresFromAPI();
        loadCountriesFromAPI();
        setupFilterButtons();
        
        // Check if there's a search query from intent
        handleSearchIntent(getIntent());
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleSearchIntent(intent);
    }
    
    private void handleSearchIntent(Intent intent) {
        if (intent == null) return;
        
        String query = null;
        
        // Kiểm tra các cách truyền query khác nhau
        if (intent.hasExtra("query")) {
            query = intent.getStringExtra("query");
        } else if (intent.hasExtra("search_query")) {
            query = intent.getStringExtra("search_query");
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra("query");
        }
        
        // Log.d(TAG, "🔍 handleSearchIntent - Query received: " + query);
        
        if (query != null && !query.isEmpty() && searchFragment != null) {
            // Post delayed to ensure fragment is fully initialized
            final String finalQuery = query;
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Log.d(TAG, "🎯 Setting search query: " + finalQuery);
                    searchFragment.setSearchQuery(finalQuery);
                }
            }, 300);
        }
    }
    
    private void initFilterViews() {
        filterPanel = findViewById(R.id.filter_panel);
        btnFilterGenre = findViewById(R.id.btn_filter_genre);
        btnFilterCountry = findViewById(R.id.btn_filter_country);
        btnFilterYear = findViewById(R.id.btn_filter_year);
        btnFilterClear = findViewById(R.id.btn_filter_clear);
        tvActiveFilters = findViewById(R.id.tv_active_filters);
        
        filterPanel.setVisibility(View.VISIBLE);
    }
    
    private void setupFilterButtons() {
        btnFilterGenre.setOnClickListener(v -> showTvFilterDialog(
            "Chọn Thể Loại", genreNames, getSelectedGenreIndex(), (index) -> {
                selectedGenreName = genreNames.get(index);
                String genreIdStr = genreIds.get(index);
                selectedGenreId = (genreIdStr != null) ? Integer.parseInt(genreIdStr) : null;
                searchHelper.setGenreFilter(selectedGenreId);
                updateFilterButtonStates();
                updateActiveFiltersDisplay();
                triggerNewSearch();
            }));
        
        btnFilterCountry.setOnClickListener(v -> showTvFilterDialog(
            "Chọn Quốc Gia", countryNames, getSelectedCountryIndex(), (index) -> {
                selectedCountryName = countryNames.get(index);
                String countryIdStr = countryIds.get(index);
                selectedCountryId = (countryIdStr != null) ? Integer.parseInt(countryIdStr) : null;
                searchHelper.setCountryFilter(selectedCountryId);
                updateFilterButtonStates();
                updateActiveFiltersDisplay();
                triggerNewSearch();
            }));
        
        btnFilterYear.setOnClickListener(v -> showYearFilterDialog());
        
        btnFilterClear.setOnClickListener(v -> clearAllFilters());
    }
    
    private int getSelectedGenreIndex() {
        if (selectedGenreId == null) return 0;
        String id = String.valueOf(selectedGenreId);
        for (int i = 0; i < genreIds.size(); i++) {
            if (id.equals(genreIds.get(i))) return i;
        }
        return 0;
    }
    
    private int getSelectedCountryIndex() {
        if (selectedCountryId == null) return 0;
        String id = String.valueOf(selectedCountryId);
        for (int i = 0; i < countryIds.size(); i++) {
            if (id.equals(countryIds.get(i))) return i;
        }
        return 0;
    }
    
    // ========== TV-Optimized Filter Dialog ==========
    
    private interface OnFilterSelected {
        void onSelected(int index);
    }
    
    private void showTvFilterDialog(String title, List<String> options, int selectedIndex, OnFilterSelected callback) {
        if (options == null || options.isEmpty()) {
            Toast.makeText(this, "Đang tải dữ liệu...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_tv_filter);
        dialog.setCancelable(true);
        
        // Set fullscreen + focus support for TV D-pad
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // Ensure dialog window can receive focus for D-pad
            dialog.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        }
        
        TextView titleView = dialog.findViewById(R.id.tv_filter_dialog_title);
        titleView.setText(title);
        
        ListView listView = dialog.findViewById(R.id.lv_filter_options);
        
        TvFilterAdapter adapter = new TvFilterAdapter(this, options, selectedIndex);
        adapter.setOnItemClickCallback(position -> {
            callback.onSelected(position);
            dialog.dismiss();
        });
        listView.setAdapter(adapter);
        
        // Also keep ListView item click as fallback
        listView.setOnItemClickListener((parent, view, position, id) -> {
            callback.onSelected(position);
            dialog.dismiss();
        });
        
        // Focus management for D-pad navigation
        listView.setItemsCanFocus(true);
        
        // Scroll to selected item and set focus for D-pad
        final int focusPosition = Math.max(0, selectedIndex);
        listView.post(() -> {
            listView.requestFocus();
            listView.setSelection(focusPosition);
            listView.postDelayed(() -> {
                // Try to focus the specific item
                int visiblePos = focusPosition - listView.getFirstVisiblePosition();
                if (visiblePos >= 0 && visiblePos < listView.getChildCount()) {
                    View item = listView.getChildAt(visiblePos);
                    if (item != null) {
                        item.requestFocus();
                        item.setSelected(true);
                    }
                } else {
                    // Fallback: focus first visible item
                    if (listView.getChildCount() > 0) {
                        listView.getChildAt(0).requestFocus();
                    }
                }
            }, 200);
        });
        
        // Back key to dismiss  
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                dialog.dismiss();
                return true;
            }
            return false;
        });
        
        // Click outside panel to dismiss
        View overlay = dialog.findViewById(R.id.filter_dialog_overlay);
        if (overlay != null) {
            overlay.setOnClickListener(v -> dialog.dismiss());
        }
        
        dialog.show();
    }
    
    private void showYearFilterDialog() {
        List<String> yearOptions = new ArrayList<>();
        yearOptions.add("Tất Cả Năm");
        yearOptions.add("2025");
        yearOptions.add("2024");
        yearOptions.add("2023");
        yearOptions.add("2022");
        yearOptions.add("2021");
        yearOptions.add("2020");
        yearOptions.add("2015 - 2019");
        yearOptions.add("2010 - 2014");
        yearOptions.add("2000 - 2009");
        yearOptions.add("Trước 2000");
        
        int selectedYearIndex = getSelectedYearIndex();
        
        showTvFilterDialog("Chọn Năm", yearOptions, selectedYearIndex, (index) -> {
            switch (index) {
                case 0:
                    selectedYearFrom = null;
                    selectedYearTo = null;
                    break;
                case 1:
                    selectedYearFrom = 2025;
                    selectedYearTo = 2025;
                    break;
                case 2:
                    selectedYearFrom = 2024;
                    selectedYearTo = 2024;
                    break;
                case 3:
                    selectedYearFrom = 2023;
                    selectedYearTo = 2023;
                    break;
                case 4:
                    selectedYearFrom = 2022;
                    selectedYearTo = 2022;
                    break;
                case 5:
                    selectedYearFrom = 2021;
                    selectedYearTo = 2021;
                    break;
                case 6:
                    selectedYearFrom = 2020;
                    selectedYearTo = 2020;
                    break;
                case 7:
                    selectedYearFrom = 2015;
                    selectedYearTo = 2019;
                    break;
                case 8:
                    selectedYearFrom = 2010;
                    selectedYearTo = 2014;
                    break;
                case 9:
                    selectedYearFrom = 2000;
                    selectedYearTo = 2009;
                    break;
                case 10:
                    selectedYearFrom = 1950;
                    selectedYearTo = 1999;
                    break;
            }
            
            searchHelper.setYearRange(selectedYearFrom, selectedYearTo);
            updateFilterButtonStates();
            updateActiveFiltersDisplay();
            triggerNewSearch();
        });
    }
    
    private int getSelectedYearIndex() {
        if (selectedYearFrom == null && selectedYearTo == null) return 0;
        if (selectedYearFrom != null && selectedYearTo != null && selectedYearFrom.equals(selectedYearTo)) {
            switch (selectedYearFrom) {
                case 2025: return 1;
                case 2024: return 2;
                case 2023: return 3;
                case 2022: return 4;
                case 2021: return 5;
                case 2020: return 6;
            }
        }
        if (selectedYearFrom != null && selectedYearTo != null) {
            if (selectedYearFrom == 2015 && selectedYearTo == 2019) return 7;
            if (selectedYearFrom == 2010 && selectedYearTo == 2014) return 8;
            if (selectedYearFrom == 2000 && selectedYearTo == 2009) return 9;
            if (selectedYearFrom == 1950 && selectedYearTo == 1999) return 10;
        }
        return 0;
    }
    
    // ========== TV Filter Adapter ==========
    
    private static class TvFilterAdapter extends BaseAdapter {
        private final Context context;
        private final List<String> options;
        private final int selectedIndex;
        private OnItemClickCallback clickCallback;
        
        interface OnItemClickCallback {
            void onItemClicked(int position);
        }
        
        TvFilterAdapter(Context context, List<String> options, int selectedIndex) {
            this.context = context;
            this.options = options;
            this.selectedIndex = selectedIndex;
        }
        
        void setOnItemClickCallback(OnItemClickCallback callback) {
            this.clickCallback = callback;
        }
        
        @Override
        public int getCount() { return options.size(); }
        
        @Override
        public String getItem(int position) { return options.get(position); }
        
        @Override
        public long getItemId(int position) { return position; }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.item_tv_filter_option, parent, false);
            }
            
            TextView optionText = view.findViewById(R.id.tv_filter_option_text);
            TextView checkMark = view.findViewById(R.id.tv_filter_check);
            
            optionText.setText(options.get(position));
            
            boolean isSelected = (position == selectedIndex);
            view.setSelected(isSelected);
            checkMark.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
            
            if (isSelected) {
                optionText.setTextColor(Color.WHITE);
            } else if (position == 0) {
                // "All" option in slightly different color
                optionText.setTextColor(Color.parseColor("#BBBBBB"));
            } else {
                optionText.setTextColor(Color.parseColor("#EEEEEE"));
            }
            
            // Direct click listener on item view - most reliable for TV D-pad
            view.setOnClickListener(v -> {
                if (clickCallback != null) {
                    clickCallback.onItemClicked(position);
                }
            });
            
            return view;
        }
    }
    
    // ========== Filter Button State Management ==========
    
    private void updateFilterButtonStates() {
        // Genre button
        if (selectedGenreId != null) {
            btnFilterGenre.setText("Thể Loại\n" + selectedGenreName);
            btnFilterGenre.setActivated(true);
        } else {
            btnFilterGenre.setText("Thể Loại\nTất cả");
            btnFilterGenre.setActivated(false);
        }
        
        // Country button
        if (selectedCountryId != null) {
            btnFilterCountry.setText("Quốc Gia\n" + selectedCountryName);
            btnFilterCountry.setActivated(true);
        } else {
            btnFilterCountry.setText("Quốc Gia\nTất cả");
            btnFilterCountry.setActivated(false);
        }
        
        // Year button
        if (selectedYearFrom != null || selectedYearTo != null) {
            String yearText;
            if (selectedYearFrom != null && selectedYearTo != null && selectedYearFrom.equals(selectedYearTo)) {
                yearText = String.valueOf(selectedYearFrom);
            } else if (selectedYearFrom != null && selectedYearTo != null) {
                if (selectedYearFrom == 1950) {
                    yearText = "Trước 2000";
                } else {
                    yearText = selectedYearFrom + "-" + selectedYearTo;
                }
            } else {
                yearText = "Tất cả";
            }
            btnFilterYear.setText("Năm\n" + yearText);
            btnFilterYear.setActivated(true);
        } else {
            btnFilterYear.setText("Năm\nTất cả");
            btnFilterYear.setActivated(false);
        }
    }
    
    private void loadGenresFromAPI() {
        // Log.d(TAG, "📂 Loading genres from API...");
        
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService apiService = retrofit.create(ApiService.class);
        
        Call<List<Genre>> call = apiService.getAllGenres(AppConfig.API_KEY);
        call.enqueue(new Callback<List<Genre>>() {
            @Override
            public void onResponse(Call<List<Genre>> call, Response<List<Genre>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    genreList = response.body();
                    genreNames.clear();
                    genreIds.clear();
                    genreNames.add("All Genres");
                    genreIds.add(null);
                    
                    for (Genre genre : genreList) {
                        genreNames.add(genre.getName());
                        genreIds.add(genre.getGenreId());
                    }
                    
                    // Log.d(TAG, "✅ Loaded " + genreList.size() + " genres from API");
                } else {
                    // Log.e(TAG, "❌ Failed to load genres");
                    setupFallbackGenres();
                }
            }

    @Override
            public void onFailure(Call<List<Genre>> call, Throwable t) {
                // Log.e(TAG, "❌ Error loading genres: " + t.getMessage());
                setupFallbackGenres();
            }
        });
    }
    
    private void setupFallbackGenres() {
        genreNames.clear();
        genreIds.clear();
        genreNames.add("Tất Cả Thể Loại");
        genreIds.add(null);
        genreNames.add("Action");
        genreIds.add("1");
        genreNames.add("Comedy");
        genreIds.add("2");
        genreNames.add("Drama");
        genreIds.add("3");
        // Log.d(TAG, "⚠️ Using fallback genres");
    }
    
    private void loadCountriesFromAPI() {
        // Log.d(TAG, "🌍 Loading countries from API...");
        
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService apiService = retrofit.create(ApiService.class);
        
        Call<List<Country>> call = apiService.getAllCountries(AppConfig.API_KEY);
        call.enqueue(new Callback<List<Country>>() {
            @Override
            public void onResponse(Call<List<Country>> call, Response<List<Country>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    countryList = response.body();
                    countryNames.clear();
                    countryIds.clear();
                    countryNames.add("Tất Cả Quốc Gia");
                    countryIds.add(null);
                    
                    for (Country country : countryList) {
                        countryNames.add(country.getName());
                        countryIds.add(country.getCountryId());
                    }
                    
                    // Log.d(TAG, "✅ Loaded " + countryList.size() + " countries from API");
                } else {
                    // Log.e(TAG, "❌ Failed to load countries");
                    setupFallbackCountries();
                }
    }

    @Override
            public void onFailure(Call<List<Country>> call, Throwable t) {
                // Log.e(TAG, "❌ Error loading countries: " + t.getMessage());
                setupFallbackCountries();
            }
        });
    }
    
    private void setupFallbackCountries() {
        countryNames.clear();
        countryIds.clear();
        countryNames.add("Tất Cả Quốc Gia");
        countryIds.add(null);
        countryNames.add("Vietnam");
        countryIds.add("1");
        countryNames.add("USA");
        countryIds.add("2");
        countryNames.add("Korea");
        countryIds.add("3");
        // Log.d(TAG, "⚠️ Using fallback countries");
    }
    
    
    
    private void clearAllFilters() {
        selectedGenreId = null;
        selectedGenreName = "Tất Cả Thể Loại";
        selectedCountryId = null;
        selectedCountryName = "Tất Cả Quốc Gia";
        selectedYearFrom = null;
        selectedYearTo = null;
        
        searchHelper.clearFilters();
        
        Toast.makeText(this, "Đã xóa tất cả bộ lọc", Toast.LENGTH_SHORT).show();
        
        updateFilterButtonStates();
        updateActiveFiltersDisplay();
        triggerNewSearch();
    }
    
    private void updateActiveFiltersDisplay() {
        List<String> activeFilters = new ArrayList<>();
        
        if (selectedGenreId != null) {
            activeFilters.add(selectedGenreName);
        }
        if (selectedCountryId != null) {
            activeFilters.add(selectedCountryName);
        }
        if (selectedYearFrom != null || selectedYearTo != null) {
            if (selectedYearFrom != null && selectedYearTo != null && selectedYearFrom.equals(selectedYearTo)) {
                activeFilters.add(String.valueOf(selectedYearFrom));
            } else if (selectedYearFrom != null && selectedYearTo != null) {
                if (selectedYearFrom == 1950) {
                    activeFilters.add("< 2000");
                } else {
                    activeFilters.add(selectedYearFrom + "-" + selectedYearTo);
                }
            }
        }
        
        if (activeFilters.isEmpty()) {
            tvActiveFilters.setVisibility(View.GONE);
        } else {
            tvActiveFilters.setVisibility(View.VISIBLE);
            tvActiveFilters.setText("Đang lọc:\n" + String.join("\n", activeFilters));
        }
    }
    
    private void triggerNewSearch() {
        // Trigger search fragment to refresh with new filters
        if (searchFragment != null) {
            // Re-submit current query with new filters
            new android.os.Handler().postDelayed(new Runnable() {
    @Override
                public void run() {
                    searchFragment.refreshSearch();
                }
            }, 100);
        }
    }

    @Override
    public void onBackPressed() {
        // Ấn BACK 1 lần là thoát luôn
        finish();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Chặn BACK trước khi Leanback xử lý (tránh bị 2 lần back)
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            finish();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}