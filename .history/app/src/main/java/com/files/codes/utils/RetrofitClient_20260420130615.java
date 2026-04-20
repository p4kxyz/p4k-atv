package com.files.codes.utils;

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
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String API_EXTENSION = "v130/"; //v130
    private static final String API_USER_NAME = "admin";
    private static final String API_PASSWORD = "1234";

    private static Retrofit retrofit;

    public static Retrofit getRetrofitInstance() {
        Type movieListType = new TypeToken<List<Movie>>(){}.getType();
        
        Gson gson = new GsonBuilder()
                .setLenient()
                .registerTypeAdapter(movieListType, new FavoriteListDeserializer())
                .create();

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    HttpUrl url = request.url();
                    String path = url.encodedPath();

                    // Force-redirect legacy movie filter endpoints to /movies.
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
                    }

                    return chain.proceed(request);
                })
            .addInterceptor(new BasicAuthInterceptor(API_USER_NAME, API_PASSWORD))
            .build();

        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(AppConfig.API_SERVER_URL + API_EXTENSION)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(client)
                    .build();
        }
        return retrofit;
    }
}
