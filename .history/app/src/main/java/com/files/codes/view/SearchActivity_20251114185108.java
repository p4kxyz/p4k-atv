package com.files.codes.view;


import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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
    private Button btnAdvancedFilters;
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
        
        Log.d(TAG, "✅ SearchActivity created with integrated filters");
        
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
        
        // Xử lý encoding cho tiếng Việt
        if (query != null) {
            try {
                // Thử decode nếu query bị encode sai
                byte[] queryBytes = query.getBytes("ISO-8859-1");
                String decodedQuery = new String(queryBytes, "UTF-8");
                if (decodedQuery.length() < query.length()) {
                    query = decodedQuery;
                    Log.d(TAG, "🔄 Query decoded from ISO to UTF-8: " + query);
                }
            } catch (Exception e) {
                Log.d(TAG, "⚠️ Query encoding conversion failed, using original: " + e.getMessage());
            }
        }
        
        Log.d(TAG, "🔍 handleSearchIntent - Query received: '" + query + "'");
        Log.d(TAG, "🔍 Query length: " + (query != null ? query.length() : "null"));
        Log.d(TAG, "🔍 Query bytes: " + (query != null ? java.util.Arrays.toString(query.getBytes()) : "null"));
        
        if (query != null && !query.isEmpty() && searchFragment != null) {
            // Post delayed to ensure fragment is fully initialized
            final String finalQuery = query.trim(); // Trim whitespace
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "🎯 Setting search query: '" + finalQuery + "'");
                    searchFragment.setSearchQuery(finalQuery);
                }
            }, 300);
        }
    }
    
    private void initFilterViews() {
        filterPanel = findViewById(R.id.filter_panel);
        btnAdvancedFilters = findViewById(R.id.btn_advanced_filters);
        tvActiveFilters = findViewById(R.id.tv_active_filters);
        
        // Filter panel is always visible (left side)
        filterPanel.setVisibility(View.VISIBLE);
        Log.d(TAG, "✅ Filter panel always visible (left sidebar)");
    }
    
    private void setupFilterButtons() {
        // Single button opens advanced filter menu
        btnAdvancedFilters.setOnClickListener(v -> showAdvancedFilterMenu());
    }
    
    private void showAdvancedFilterMenu() {
        String[] filterOptions = {
            "📂 Chọn Thể Loại",
            "🌍 Chọn Quốc Gia", 
            "📅 Chọn Năm",
            "🗑️ Xóa Tất Cả Bộ Lọc"
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚙️ Bộ Lọc Nâng Cao");
        builder.setItems(filterOptions, (dialog, which) -> {
            switch (which) {
                case 0: // Genre
                    showGenreDialog();
                    break;
                case 1: // Country
                    showCountryDialog();
                    break;
                case 2: // Year
                    showYearDialog();
                    break;
                case 3: // Clear all
                    clearAllFilters();
                    break;
            }
        });
        builder.setNegativeButton("Đóng", null);
        builder.show();
    }
    
    private void loadGenresFromAPI() {
        Log.d(TAG, "📂 Loading genres from API...");
        
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
                    
                    Log.d(TAG, "✅ Loaded " + genreList.size() + " genres from API");
                } else {
                    Log.e(TAG, "❌ Failed to load genres");
                    setupFallbackGenres();
                }
            }

    @Override
            public void onFailure(Call<List<Genre>> call, Throwable t) {
                Log.e(TAG, "❌ Error loading genres: " + t.getMessage());
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
        Log.d(TAG, "⚠️ Using fallback genres");
    }
    
    private void loadCountriesFromAPI() {
        Log.d(TAG, "🌍 Loading countries from API...");
        
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
                    
                    Log.d(TAG, "✅ Loaded " + countryList.size() + " countries from API");
                } else {
                    Log.e(TAG, "❌ Failed to load countries");
                    setupFallbackCountries();
                }
    }

    @Override
            public void onFailure(Call<List<Country>> call, Throwable t) {
                Log.e(TAG, "❌ Error loading countries: " + t.getMessage());
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
        Log.d(TAG, "⚠️ Using fallback countries");
    }
    
    private void showGenreDialog() {
        if (genreNames.isEmpty()) {
            Toast.makeText(this, "Đang tải thể loại...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📂 Chọn Thể Loại");
        builder.setItems(genreNames.toArray(new String[0]), (dialog, which) -> {
            selectedGenreName = genreNames.get(which);
            String genreIdStr = genreIds.get(which);
            selectedGenreId = (genreIdStr != null) ? Integer.parseInt(genreIdStr) : null;
            
            searchHelper.setGenreFilter(selectedGenreId);
            
            Log.d(TAG, "Genre selected: " + selectedGenreName + " (ID: " + selectedGenreId + ")");
            updateActiveFiltersDisplay();
            triggerNewSearch();
        });
        builder.show();
    }
    
    private void showCountryDialog() {
        if (countryNames.isEmpty()) {
            Toast.makeText(this, "Đang tải quốc gia...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🌍 Chọn Quốc Gia");
        builder.setItems(countryNames.toArray(new String[0]), (dialog, which) -> {
            selectedCountryName = countryNames.get(which);
            String countryIdStr = countryIds.get(which);
            selectedCountryId = (countryIdStr != null) ? Integer.parseInt(countryIdStr) : null;
            
            searchHelper.setCountryFilter(selectedCountryId);
            
            Log.d(TAG, "Country selected: " + selectedCountryName + " (ID: " + selectedCountryId + ")");
            updateActiveFiltersDisplay();
            triggerNewSearch();
        });
        builder.show();
    }
    
    private void showYearDialog() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        List<String> yearOptions = new ArrayList<>();
        yearOptions.add("Tất Cả Năm");
        yearOptions.add("2025");
        yearOptions.add("2024");
        yearOptions.add("2023");
        yearOptions.add("2022");
        yearOptions.add("2021");
        yearOptions.add("2020");
        yearOptions.add("2015-2019");
        yearOptions.add("2010-2014");
        yearOptions.add("2000-2009");
        yearOptions.add("Trước 2000");
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📅 Chọn Năm");
        builder.setItems(yearOptions.toArray(new String[0]), (dialog, which) -> {
            switch (which) {
                case 0: // All Years
                    selectedYearFrom = null;
                    selectedYearTo = null;
                    break;
                case 1: // 2025
                    selectedYearFrom = 2025;
                    selectedYearTo = 2025;
                    break;
                case 2: // 2024
                    selectedYearFrom = 2024;
                    selectedYearTo = 2024;
                    break;
                case 3: // 2023
                    selectedYearFrom = 2023;
                    selectedYearTo = 2023;
                    break;
                case 4: // 2022
                    selectedYearFrom = 2022;
                    selectedYearTo = 2022;
                    break;
                case 5: // 2021
                    selectedYearFrom = 2021;
                    selectedYearTo = 2021;
                    break;
                case 6: // 2020
                    selectedYearFrom = 2020;
                    selectedYearTo = 2020;
                    break;
                case 7: // 2015-2019
                    selectedYearFrom = 2015;
                    selectedYearTo = 2019;
                    break;
                case 8: // 2010-2014
                    selectedYearFrom = 2010;
                    selectedYearTo = 2014;
                    break;
                case 9: // 2000-2009
                    selectedYearFrom = 2000;
                    selectedYearTo = 2009;
                    break;
                case 10: // Before 2000
                    selectedYearFrom = 1950;
                    selectedYearTo = 1999;
                    break;
            }
            
            searchHelper.setYearRange(selectedYearFrom, selectedYearTo);
            Log.d(TAG, "Year range selected: " + selectedYearFrom + " - " + selectedYearTo);
            updateActiveFiltersDisplay();
            triggerNewSearch();
        });
        builder.show();
    }
    
    private void clearAllFilters() {
        selectedGenreId = null;
        selectedGenreName = "Tất Cả Thể Loại";
        selectedCountryId = null;
        selectedCountryName = "Tất Cả Quốc Gia";
        selectedYearFrom = null;
        selectedYearTo = null;
        
        searchHelper.clearFilters();
        
        Log.d(TAG, "✅ All filters cleared");
        Toast.makeText(this, "✨ Đã xóa tất cả bộ lọc", Toast.LENGTH_SHORT).show();
        
        updateActiveFiltersDisplay();
        triggerNewSearch();
    }
    
    private void updateActiveFiltersDisplay() {
        List<String> activeFilters = new ArrayList<>();
        
        if (selectedGenreId != null) {
            activeFilters.add("Thể loại: " + selectedGenreName);
        }
        if (selectedCountryId != null) {
            activeFilters.add("Quốc gia: " + selectedCountryName);
        }
        if (selectedYearFrom != null || selectedYearTo != null) {
            String yearText = "Năm: ";
            if (selectedYearFrom != null && selectedYearTo != null && selectedYearFrom.equals(selectedYearTo)) {
                yearText += selectedYearFrom;
            } else {
                yearText += (selectedYearFrom != null ? selectedYearFrom : "...") + " - " + (selectedYearTo != null ? selectedYearTo : "...");
            }
            activeFilters.add(yearText);
        }
        
        if (activeFilters.isEmpty()) {
            tvActiveFilters.setVisibility(View.GONE);
        } else {
            tvActiveFilters.setVisibility(View.VISIBLE);
            tvActiveFilters.setText("✨ Đang lọc: " + String.join(", ", activeFilters));
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
        // Thoát activity ngay lập tức, không cần kiểm tra bàn phím
        super.onBackPressed();
    }
}