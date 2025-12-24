package com.files.codes.utils;

import com.files.codes.model.phim4k.Phim4kDetailResponse;
import com.files.codes.model.phim4k.Phim4kResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface KKPhim4kApiService {
    
    @GET("danh-sach/phim-moi-cap-nhat")
    Call<Phim4kResponse> getLatestMovies(@Query("page") int page);
    
    @GET("phim/{slug}")
    Call<Phim4kDetailResponse> getMovieDetail(@Path("slug") String slug);
    
    @GET("v1/api/tim-kiem")
    Call<Phim4kResponse> searchMovies(@Query("keyword") String keyword);
}
