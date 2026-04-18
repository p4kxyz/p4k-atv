package com.files.codes.utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for Advanced Search functionality
 * Manages filters: Genre, Year, Country
 */
public class SearchAdvancedHelper {
    private static final String TAG = "SearchAdvancedHelper";
    
    // Filter storage
    private Map<Integer, String> genreMap = new HashMap<>();  // genre_id -> name
    private Map<Integer, String> countryMap = new HashMap<>();  // country_id -> name
    
    // Current filter values
    private String currentSearchQuery = "";
    private Integer selectedGenreId = null;
    private Integer selectedCountryId = null;
    private Integer yearFrom = null;
    private Integer yearTo = null;
    
    // Singleton
    private static SearchAdvancedHelper instance;
    
    public static synchronized SearchAdvancedHelper getInstance() {
        if (instance == null) {
            instance = new SearchAdvancedHelper();
        }
        return instance;
    }
    
    private SearchAdvancedHelper() {
        initializeDefaultData();
    }
    
    /**
     * Initialize default genres and countries
     */
    private void initializeDefaultData() {
        // Common genres
        genreMap.put(1, "Action");
        genreMap.put(2, "Comedy");
        genreMap.put(3, "Drama");
        genreMap.put(4, "Horror");
        genreMap.put(5, "Romance");
        genreMap.put(6, "Thriller");
        genreMap.put(7, "Sci-Fi");
        genreMap.put(8, "Animation");
        genreMap.put(9, "Documentary");
        genreMap.put(10, "Crime");
        
        // Common countries
        countryMap.put(1, "Vietnam");
        countryMap.put(2, "USA");
        countryMap.put(3, "Korea");
        countryMap.put(4, "Japan");
        countryMap.put(5, "Thailand");
        countryMap.put(6, "China");
        countryMap.put(7, "Taiwan");
        countryMap.put(8, "Hong Kong");
        countryMap.put(9, "India");
        countryMap.put(10, "Brazil");
    }
    
    // Getters
    public Map<Integer, String> getGenreMap() {
        return genreMap;
    }
    
    public Map<Integer, String> getCountryMap() {
        return countryMap;
    }
    
    public List<String> getGenreNames() {
        return new ArrayList<>(genreMap.values());
    }
    
    public List<String> getCountryNames() {
        return new ArrayList<>(countryMap.values());
    }
    
    // Filter setters
    public void setSearchQuery(String query) {
        this.currentSearchQuery = query;
        Log.d(TAG, "✅ Search query set: " + query);
    }
    
    public void setGenreFilter(Integer genreId) {
        this.selectedGenreId = genreId;
        Log.d(TAG, "✅ Genre filter set: " + (genreId != null ? genreMap.get(genreId) : "None"));
    }
    
    public void setCountryFilter(Integer countryId) {
        this.selectedCountryId = countryId;
        Log.d(TAG, "✅ Country filter set: " + (countryId != null ? countryMap.get(countryId) : "None"));
    }
    
    public void setYearRange(Integer from, Integer to) {
        this.yearFrom = from;
        this.yearTo = to;
        Log.d(TAG, "✅ Year range set: " + (from != null ? from : "Any") + " - " + (to != null ? to : "Any"));
    }
    
    // Filter getters
    public String getSearchQuery() {
        return currentSearchQuery;
    }
    
    public Integer getSelectedGenreId() {
        return selectedGenreId;
    }
    
    public Integer getSelectedCountryId() {
        return selectedCountryId;
    }
    
    public Integer getYearFrom() {
        return yearFrom;
    }
    
    public Integer getYearTo() {
        return yearTo;
    }
    
    /**
     * Build API URL parameters for advanced search
     */
    public String buildSearchParams() {
        StringBuilder params = new StringBuilder();
        
        if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
            params.append("q=").append(currentSearchQuery);
        }
        
        if (selectedGenreId != null) {
            if (params.length() > 0) params.append("&");
            params.append("genre_id=").append(selectedGenreId);
        }
        
        if (selectedCountryId != null) {
            if (params.length() > 0) params.append("&");
            params.append("country_id=").append(selectedCountryId);
        }
        
        if (yearFrom != null) {
            if (params.length() > 0) params.append("&");
            params.append("range_from=").append(yearFrom);
        }
        
        if (yearTo != null) {
            if (params.length() > 0) params.append("&");
            params.append("range_to=").append(yearTo);
        }
        
        Log.d(TAG, "🔗 Search params: " + params.toString());
        return params.toString();
    }
    
    /**
     * Clear all filters
     */
    public void clearFilters() {
        currentSearchQuery = "";
        selectedGenreId = null;
        selectedCountryId = null;
        yearFrom = null;
        yearTo = null;
        Log.d(TAG, "✅ All filters cleared");
    }
    
    /**
     * Get human-readable filter summary
     */
    public String getFilterSummary() {
        List<String> activeFilters = new ArrayList<>();
        
        if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
            activeFilters.add("Query: " + currentSearchQuery);
        }
        if (selectedGenreId != null) {
            activeFilters.add("Genre: " + genreMap.get(selectedGenreId));
        }
        if (selectedCountryId != null) {
            activeFilters.add("Country: " + countryMap.get(selectedCountryId));
        }
        if (yearFrom != null || yearTo != null) {
            String yearRange = (yearFrom != null ? yearFrom : "Any") + " - " + (yearTo != null ? yearTo : "Any");
            activeFilters.add("Year: " + yearRange);
        }
        
        return activeFilters.isEmpty() ? "No filters" : String.join(", ", activeFilters);
    }
}

