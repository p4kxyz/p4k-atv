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

        String serverUrl = AppConfig.getCurrentApiServerUrl();
        if (serverUrl == null) {
            serverUrl = "";
        }
        if (!serverUrl.endsWith("/")) {
            serverUrl += "/";
        }
        String baseUrl = serverUrl + API_EXTENSION;
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
}
