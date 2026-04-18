package com.files.codes.model.api;

import com.files.codes.model.free1.Free1MovieDetail;
import com.files.codes.model.free1.Free1SearchResponse;
import com.files.codes.model.free1.Free1ListResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * API Service cho free1.phim4k.lol
 * Nguồn phim miễn phí với đầy đủ tính năng tìm kiếm và phân loại
 */
public interface Free1ApiService {
    
    /**
     * Lấy thông tin chi tiết phim theo slug
     * GET https://free1.phim4k.lol/phim/{slug}
     */
    @GET("phim/{slug}")
    Call<Free1MovieDetail> getMovieBySlug(@Path("slug") String slug);
    
    /**
     * Lấy thông tin phim theo TMDB ID
     * GET https://free1.phim4k.lol/tmdb/{type}/{id}
     * @param type "tv" hoặc "movie"
     * @param id TMDB ID
     */
    @GET("tmdb/{type}/{id}")
    Call<Free1MovieDetail> getMovieByTmdb(@Path("type") String type, @Path("id") String id);
    
    /**
     * Lấy danh sách phim theo loại
     * GET https://free1.phim4k.lol/v1/api/danh-sach/{type_list}
     * @param typeList phim-bo, phim-le, tv-shows, hoat-hinh, phim-vietsub, phim-thuyet-minh, phim-long-tieng
     */
    @GET("v1/api/danh-sach/{type_list}")
    Call<Free1ListResponse> getMovieList(
            @Path("type_list") String typeList,
            @Query("page") Integer page,
            @Query("sort_field") String sortField,
            @Query("sort_type") String sortType,
            @Query("sort_lang") String sortLang,
            @Query("category") String category,
            @Query("country") String country,
            @Query("year") Integer year,
            @Query("limit") Integer limit
    );
    
    /**
     * Tìm kiếm phim
     * GET https://free1.phim4k.lol/v1/api/tim-kiem
     */
    @GET("v1/api/tim-kiem")
    Call<Free1SearchResponse> searchMovies(
            @Query("keyword") String keyword,
            @Query("page") Integer page,
            @Query("sort_field") String sortField,
            @Query("sort_type") String sortType,
            @Query("sort_lang") String sortLang,
            @Query("category") String category,
            @Query("country") String country,
            @Query("year") Integer year,
            @Query("limit") Integer limit
    );
    
    // Các method tiện ích với tham số mặc định
    
    /**
     * Lấy danh sách phim bộ mới nhất
     */
    @GET("v1/api/danh-sach/phim-bo")
    Call<Free1ListResponse> getLatestTvSeries(
            @Query("page") Integer page,
            @Query("sort_field") String sortField,
            @Query("sort_type") String sortType,
            @Query("limit") Integer limit
    );
    
    /**
     * Lấy danh sách phim lẻ mới nhất
     */
    @GET("v1/api/danh-sach/phim-le")
    Call<Free1ListResponse> getLatestMovies(
            @Query("page") Integer page,
            @Query("sort_field") String sortField,
            @Query("sort_type") String sortType,
            @Query("limit") Integer limit
    );
    
    /**
     * Lấy danh sách hoạt hình
     */
    @GET("v1/api/danh-sach/hoat-hinh")
    Call<Free1ListResponse> getAnimations(
            @Query("page") Integer page,
            @Query("sort_field") String sortField,
            @Query("sort_type") String sortType,
            @Query("limit") Integer limit
    );
    
    /**
     * Tìm kiếm đơn giản
     */
    @GET("v1/api/tim-kiem")
    Call<Free1SearchResponse> searchSimple(
            @Query("keyword") String keyword,
            @Query("page") Integer page,
            @Query("limit") Integer limit
    );
}