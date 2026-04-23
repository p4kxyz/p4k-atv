package com.files.codes.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Local storage for recent search queries.
 */
public class SearchHistoryManager {
    private static final String TAG = "SearchHistoryManager";
    private static final String PREF_NAME = "search_history_store";
    private static final String PREF_KEY_HISTORY = "recent_search_queries";
    private static final int MAX_HISTORY_ITEMS = 10;
    private static final String WORKER_BASE_URL = "https://ks.phim4k.lol/v1/search-history";
    private static final String LIST_KEY = "REPLACE_WITH_APP_LIST_KEY";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static SearchHistoryManager instance;

    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final Gson gson = new Gson();
    private final OkHttpClient httpClient = new OkHttpClient();

    public static synchronized SearchHistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new SearchHistoryManager(context.getApplicationContext());
        }
        return instance;
    }

    private SearchHistoryManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public interface SyncCallback {
        void onSuccess();
        void onError(String error);
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
        pushAddToServer(normalizedQuery);
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
            pushDeleteQueryToServer(normalizedQuery);
        }
        return removed;
    }

    public synchronized void clearAll() {
        sharedPreferences.edit().remove(PREF_KEY_HISTORY).apply();
        pushClearAllToServer();
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

    public void syncFromServer(final SyncCallback callback) {
        if (!canSyncWithServer()) {
            if (callback != null) {
                callback.onError("User is not logged in or list key is missing");
            }
            return;
        }

        Request request = withAuthHeaders(new Request.Builder())
                .url(WORKER_BASE_URL)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                Log.w(TAG, "syncFromServer failed", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                if (!response.isSuccessful()) {
                    if (callback != null) {
                        callback.onError("HTTP " + response.code());
                    }
                    response.close();
                    return;
                }

                String body = response.body() != null ? response.body().string() : "";
                response.close();

                try {
                    JsonObject root = gson.fromJson(body, JsonObject.class);
                    JsonArray itemsArray = root != null ? root.getAsJsonArray("items") : null;
                    List<SearchHistoryEntry> remoteItems = parseRemoteItems(itemsArray);

                    synchronized (SearchHistoryManager.this) {
                        trimToLimit(remoteItems);
                        saveHistory(remoteItems);
                    }

                    if (callback != null) {
                        callback.onSuccess();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to parse server history", e);
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                }
            }
        });
    }

    private List<SearchHistoryEntry> parseRemoteItems(JsonArray itemsArray) {
        List<SearchHistoryEntry> result = new ArrayList<>();
        if (itemsArray == null) {
            return result;
        }

        for (JsonElement element : itemsArray) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject obj = element.getAsJsonObject();
            String query = obj.has("query") && !obj.get("query").isJsonNull() ? obj.get("query").getAsString() : "";
            long ts = obj.has("timestamp") && !obj.get("timestamp").isJsonNull() ? obj.get("timestamp").getAsLong() : System.currentTimeMillis();

            String normalizedQuery = normalizeQuery(query);
            if (!TextUtils.isEmpty(normalizedQuery)) {
                result.add(new SearchHistoryEntry(normalizedQuery, ts));
            }
        }

        Collections.sort(result, new Comparator<SearchHistoryEntry>() {
            @Override
            public int compare(SearchHistoryEntry left, SearchHistoryEntry right) {
                return Long.compare(right.getTimestamp(), left.getTimestamp());
            }
        });

        return result;
    }

    private boolean canSyncWithServer() {
        return PreferenceUtils.isLoggedIn(context)
                && !TextUtils.isEmpty(PreferenceUtils.getUserEmail(context))
                && !TextUtils.isEmpty(LIST_KEY)
                && !"REPLACE_WITH_APP_LIST_KEY".equals(LIST_KEY);
    }

    private Request.Builder withAuthHeaders(Request.Builder builder) {
        String email = PreferenceUtils.getUserEmail(context);
        return builder
                .addHeader("x-user-email", email != null ? email : "")
                .addHeader("x-list-key", LIST_KEY)
                .addHeader("content-type", "application/json");
    }

    private void pushAddToServer(String query) {
        if (!canSyncWithServer()) {
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("query", query);
        payload.addProperty("email", PreferenceUtils.getUserEmail(context));
        payload.addProperty("listKey", LIST_KEY);

        Request request = withAuthHeaders(new Request.Builder())
                .url(WORKER_BASE_URL)
                .post(RequestBody.create(gson.toJson(payload), JSON))
                .build();

        httpClient.newCall(request).enqueue(new LoggingCallback("pushAddToServer"));
    }

    private void pushDeleteQueryToServer(final String normalizedQuery) {
        if (!canSyncWithServer()) {
            return;
        }

        Request getRequest = withAuthHeaders(new Request.Builder())
                .url(WORKER_BASE_URL)
                .get()
                .build();

        httpClient.newCall(getRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                Log.w(TAG, "pushDeleteQueryToServer: get failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                if (!response.isSuccessful()) {
                    Log.w(TAG, "pushDeleteQueryToServer: get failed code=" + response.code());
                    response.close();
                    return;
                }

                String body = response.body() != null ? response.body().string() : "";
                response.close();

                try {
                    JsonObject root = gson.fromJson(body, JsonObject.class);
                    JsonArray itemsArray = root != null ? root.getAsJsonArray("items") : null;
                    if (itemsArray == null) {
                        return;
                    }

                    for (JsonElement element : itemsArray) {
                        if (!element.isJsonObject()) continue;
                        JsonObject obj = element.getAsJsonObject();
                        String query = obj.has("query") && !obj.get("query").isJsonNull() ? obj.get("query").getAsString() : "";
                        String id = obj.has("id") && !obj.get("id").isJsonNull() ? obj.get("id").getAsString() : "";

                        if (!TextUtils.isEmpty(id) && normalizedQuery.equalsIgnoreCase(normalizeQuery(query))) {
                            Request delRequest = withAuthHeaders(new Request.Builder())
                                    .url(WORKER_BASE_URL + "/" + id)
                                    .delete()
                                    .build();
                            httpClient.newCall(delRequest).enqueue(new LoggingCallback("pushDeleteQueryToServer"));
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "pushDeleteQueryToServer: parse failed", e);
                }
            }
        });
    }

    private void pushClearAllToServer() {
        if (!canSyncWithServer()) {
            return;
        }

        Request request = withAuthHeaders(new Request.Builder())
                .url(WORKER_BASE_URL)
                .delete()
                .build();

        httpClient.newCall(request).enqueue(new LoggingCallback("pushClearAllToServer"));
    }

    private class LoggingCallback implements Callback {
        private final String source;

        LoggingCallback(String source) {
            this.source = source;
        }

        @Override
        public void onFailure(Call call, java.io.IOException e) {
            Log.w(TAG, source + " failed", e);
        }

        @Override
        public void onResponse(Call call, Response response) {
            response.close();
            if (!response.isSuccessful()) {
                Log.w(TAG, source + " http=" + response.code());
            }
        }
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