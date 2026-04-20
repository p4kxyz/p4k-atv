package com.files.codes.utils;

import com.files.codes.model.phim4k.KKPhim4kDetailResponse;
import com.files.codes.model.phim4k.KKPhim4kResponse;

import com.files.codes.model.phim4k.KKPhim4kSearchResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface KKPhim4kApiService {
    
    @GET("danh-sach/phim-moi-cap-nhat")
    Call<KKPhim4kResponse> getLatestMovies(@Query("page") int page);
    
    @GET("phim/{slug}")
    Call<KKPhim4kDetailResponse> getMovieDetail(@Path("slug") String slug);
    
    @GET("v1/api/tim-kiem")
    Call<KKPhim4kSearchResponse> searchMovies(@Query("keyword") String keyword);
}
