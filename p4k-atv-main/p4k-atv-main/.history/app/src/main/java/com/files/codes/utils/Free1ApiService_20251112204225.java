package com.files.codes.utils;

import com.files.codes.model.phim4k.Phim4kDetailResponse;
import com.files.codes.model.phim4k.Phim4kResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface Free1ApiService {
    
    // Thông tin Phim & Danh sách tập phim
    @GET("phim/{slug}")
    Call<Phim4kDetailResponse> getMovieDetail(@Path("slug") String slug);
    
    // Thông tin dựa theo TMDB ID
    @GET("tmdb/{type}/{id}")
    Call<Phim4kDetailResponse> getMovieByTmdb(@Path("type") String type, @Path("id") String tmdbId);
    
    // Danh sách phim bộ
    @GET("v1/api/danh-sach/phim-bo")
    Call<Phim4kResponse> getTvSeries(
            @Query("page") int page,
            @Query("sort_field") String sortField,
            @Query("sort_type") String sortType,
            @Query("sort_lang") String sortLang,
            @Query("category") String category,
            @Query("country") String country,
            @Query("year") String year,
            @Query("limit") Integer limit
    );
    
    // Danh sách phim lẻ
    @GET("v1/api/danh-sach/phim-le")
    Call<Phim4kResponse> getMovies(
            @Query("page") int page,
            @Query("sort_field") String sortField,
            @Query("sort_type") String sortType,
            @Query("sort_lang") String sortLang,
            @Query("category") String category,
            @Query("country") String country,
            @Query("year") String year,
            @Query("limit") Integer limit
    );
    
    // Danh sách TV Shows
    @GET("v1/api/danh-sach/tv-shows")
    Call<Phim4kResponse> getTvShows(
            @Query("page") int page,
            @Query("sort_field") String sortField,
            @Query("sort_type") String sortType,
            @Query("sort_lang") String sortLang,
            @Query("category") String category,
            @Query("country") String country,
            @Query("year") String year,
            @Query("limit") Integer limit
    );
    
    // Danh sách hoạt hình
    @GET("v1/api/danh-sach/hoat-hinh")
    Call<Phim4kResponse> getAnimations(
            @Query("page") int page,
            @Query("sort_field") String sortField,
            @Query("sort_type") String sortType,
            @Query("sort_lang") String sortLang,
            @Query("category") String category,
            @Query("country") String country,
            @Query("year") String year,
            @Query("limit") Integer limit
    );
    
    // Tìm kiếm phim
    @GET("v1/api/tim-kiem")
    Call<Phim4kResponse> searchMovies(
            @Query("keyword") String keyword,
            @Query("page") int page,
            @Query("sort_field") String sortField,
            @Query("sort_type") String sortType,
            @Query("sort_lang") String sortLang,
            @Query("category") String category,
            @Query("country") String country,
            @Query("year") String year,
            @Query("limit") Integer limit
    );
    
    // Simplified methods for easy use
    @GET("v1/api/danh-sach/phim-le")
    Call<Phim4kResponse> getLatestMovies(@Query("page") int page);
    
    @GET("v1/api/tim-kiem")
    Call<Phim4kResponse> searchSimple(@Query("keyword") String keyword, @Query("page") int page);
}