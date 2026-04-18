package com.files.codes.utils;

import com.files.codes.model.Movie;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.files.codes.AppConfig;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String REST_API_SEGMENT = "rest-api/";
    private static final String API_EXTENSION = "v130/"; //v130
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
                .addInterceptor(new BasicAuthInterceptor(API_USER_NAME, API_PASSWORD)).build();

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
            throw new IllegalStateException("Remote API URL is not loaded from ads endpoint yet");
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
