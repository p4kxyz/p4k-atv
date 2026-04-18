package com.files.codes.utils;

import com.files.codes.model.phim4k.Phim4kDetailResponse;
import com.files.codes.model.phim4k.Phim4kResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface Free1ApiService {
    
    @GET("v1/api/danh-sach/phim-moi")
    Call<Phim4kResponse> getLatestMovies(@Query("page") int page);
    
    @GET("phim/{slug}")
    Call<Phim4kDetailResponse> getMovieDetail(@Path("slug") String slug);
    
    @GET("v1/api/tim-kiem")
    Call<Phim4kResponse> searchMovies(@Query("keyword") String keyword);
    
    @GET("v1/api/danh-sach/phim-le")
    Call<Phim4kResponse> getMovies(@Query("page") int page);
    
    @GET("v1/api/danh-sach/phim-bo")
    Call<Phim4kResponse> getTvSeries(@Query("page") int page);
}