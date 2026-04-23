package com.files.codes.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Local storage for recent search queries.
 */
public class SearchHistoryManager {
    private static final String PREF_NAME = "search_history_store";
    private static final String PREF_KEY_HISTORY = "recent_search_queries";
    private static final int MAX_HISTORY_ITEMS = 10;

    private static SearchHistoryManager instance;

    private final SharedPreferences sharedPreferences;
    private final Gson gson = new Gson();

    public static synchronized SearchHistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new SearchHistoryManager(context.getApplicationContext());
        }
        return instance;
    }

    private SearchHistoryManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public synchronized List<SearchHistoryEntry> getRecentSearches() {
        List<SearchHistoryEntry> items = loadHistory();
        if (items.isEmpty()) {
            return items;
        }

        Collections.sort(items, new Comparator<SearchHistoryEntry>() {
            @Override
            public int compare(SearchHistoryEntry left, SearchHistoryEntry right) {
                return Long.compare(right.getTimestamp(), left.getTimestamp());
            }
        });

        if (items.size() > MAX_HISTORY_ITEMS) {
            trimToLimit(items);
            saveHistory(items);
        }

        return items;
    }

    public synchronized void addSearchQuery(String query) {
        String normalizedQuery = normalizeQuery(query);
        if (TextUtils.isEmpty(normalizedQuery)) {
            return;
        }

        List<SearchHistoryEntry> items = loadHistory();
        Iterator<SearchHistoryEntry> iterator = items.iterator();
        while (iterator.hasNext()) {
            SearchHistoryEntry entry = iterator.next();
            if (entry != null && normalizedQuery.equalsIgnoreCase(normalizeQuery(entry.getQuery()))) {
                iterator.remove();
            }
        }

        items.add(0, new SearchHistoryEntry(normalizedQuery, System.currentTimeMillis()));
        trimToLimit(items);
        saveHistory(items);
    }

    public synchronized boolean removeSearchQuery(String query) {
        String normalizedQuery = normalizeQuery(query);
        if (TextUtils.isEmpty(normalizedQuery)) {
            return false;
        }

        List<SearchHistoryEntry> items = loadHistory();
        boolean removed = false;
        Iterator<SearchHistoryEntry> iterator = items.iterator();
        while (iterator.hasNext()) {
            SearchHistoryEntry entry = iterator.next();
            if (entry != null && normalizedQuery.equalsIgnoreCase(normalizeQuery(entry.getQuery()))) {
                iterator.remove();
                removed = true;
            }
        }

        if (removed) {
            saveHistory(items);
        }
        return removed;
    }

    public synchronized void clearAll() {
        sharedPreferences.edit().remove(PREF_KEY_HISTORY).apply();
    }

    public synchronized boolean isEmpty() {
        return loadHistory().isEmpty();
    }

    private List<SearchHistoryEntry> loadHistory() {
        String json = sharedPreferences.getString(PREF_KEY_HISTORY, "");
        if (TextUtils.isEmpty(json)) {
            return new ArrayList<>();
        }

        try {
            Type listType = new TypeToken<List<SearchHistoryEntry>>() {}.getType();
            List<SearchHistoryEntry> items = gson.fromJson(json, listType);
            return items != null ? items : new ArrayList<SearchHistoryEntry>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveHistory(List<SearchHistoryEntry> items) {
        sharedPreferences.edit()
                .putString(PREF_KEY_HISTORY, gson.toJson(items))
                .apply();
    }

    private void trimToLimit(List<SearchHistoryEntry> items) {
        while (items.size() > MAX_HISTORY_ITEMS) {
            items.remove(items.size() - 1);
        }
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }
        return query.trim().replaceAll("\\s+", " ");
    }

    public static class SearchHistoryEntry {
        @SerializedName("query")
        private String query;

        @SerializedName("timestamp")
        private long timestamp;

        public SearchHistoryEntry() {
        }

        public SearchHistoryEntry(String query, long timestamp) {
            this.query = query;
            this.timestamp = timestamp;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}