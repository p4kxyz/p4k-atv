package com.files.codes.utils;

import android.util.Log;

import com.files.codes.model.Movie;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.files.codes.AppConfig;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String TAG = "RetrofitClient";
    private static final String REST_API_SEGMENT = "rest-api/";
    private static final String API_EXTENSION = "v130/"; //v130
    private static final String SAFE_BOOTSTRAP_SERVER_URL = "https://l.dramahay.xyz/";
    private static final String API_USER_NAME = "admin";
    private static final String API_PASSWORD = "1234";

    private static Retrofit retrofit;
    private static String lastBaseUrl;

    public static Retrofit getRetrofitInstance() {
        Type movieListType = new TypeToken<List<Movie>>(){}.getType();
        
        Gson gson = new GsonBuilder()
                .setLenient()
                .registerTypeAdapter(movieListType, new FavoriteListDeserializer())
                .create();

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    HttpUrl url = request.url();
                    String path = url.encodedPath();

                    if (path.endsWith("/content_by_genre_id") || path.endsWith("/content_by_country_id")) {
                        HttpUrl.Builder builder = url.newBuilder();
                        String id = url.queryParameter("id");

                        builder.encodedPath(path.replace("/content_by_genre_id", "/movies")
                                .replace("/content_by_country_id", "/movies"));
                        builder.removeAllQueryParameters("id");

                        if (id != null && !id.trim().isEmpty()) {
                            if (path.endsWith("/content_by_genre_id")) {
                                builder.addQueryParameter("genre_id", id);
                            } else {
                                builder.addQueryParameter("country_id", id);
                            }
                        }

                        HttpUrl rewrittenUrl = builder.build();
                        request = request.newBuilder().url(rewrittenUrl).build();
                        Log.e("API_TRACE", "REWRITE " + url + " -> " + rewrittenUrl);
                    }

                    Response response = chain.proceed(request);
                    return response;
                })
                .addInterceptor(new BasicAuthInterceptor(API_USER_NAME, API_PASSWORD))
                .build();

        String baseUrl = buildBaseUrl(AppConfig.getCurrentApiServerUrl());
        if (retrofit == null || !baseUrl.equals(lastBaseUrl)) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(client)
                    .build();
            lastBaseUrl = baseUrl;
        }
        return retrofit;
    }

    private static String buildBaseUrl(String rawServerUrl) {
        String serverUrl = rawServerUrl == null ? "" : rawServerUrl.trim();
        if (serverUrl.isEmpty()) {
            AdsRemoteConfigService.refreshAndWait(5000);
            serverUrl = AppConfig.getCurrentApiServerUrl().trim();
            if (serverUrl.isEmpty()) {
                AdsRemoteConfigService.restoreLastKnownConfig();
                serverUrl = AppConfig.getCurrentApiServerUrl().trim();
            }
            if (serverUrl.isEmpty()) {
                serverUrl = SAFE_BOOTSTRAP_SERVER_URL;
                Log.w(TAG, "Using bootstrap API URL because ads endpoint/cached config is unavailable");
            }
        }

        if (!serverUrl.endsWith("/")) {
            serverUrl += "/";
        }

        String lower = serverUrl.toLowerCase();
        if (lower.contains("/rest-api/v130/")) {
            return serverUrl;
        }
        if (lower.endsWith("/v130/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - "v130/".length());
            lower = serverUrl.toLowerCase();
        }
        if (!lower.contains("/rest-api/")) {
            serverUrl += REST_API_SEGMENT;
        }
        return serverUrl + API_EXTENSION;
    }
}
