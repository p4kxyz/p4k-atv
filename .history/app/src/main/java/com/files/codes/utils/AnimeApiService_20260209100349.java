package com.files.codes.utils;

import com.files.codes.model.anime.AnimeSearchResponse;
import com.files.codes.model.anime.AnimeMetadataResponse;
import com.files.codes.model.anime.AnimeEpisodeResponse;
import com.files.codes.model.anime.AnimeStreamResponse;
import com.files.codes.model.anime.AnimeServerResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AnimeApiService {

    @GET("search")
    Call<AnimeSearchResponse> searchAnime(
        @Query("title") String title,
        @Query("mediaType") String mediaType,
        @Query("limit") int limit
    );

    @GET("metadata")
    Call<AnimeMetadataResponse> getMetadata(
        @Query("id") String id
    );

    @GET("stream/episodes")
    Call<AnimeEpisodeResponse> getEpisodes(
        @Query("id") String id,
        @Query("provider") String provider,
        @Query("server") String server
    );

    @GET("stream/episodes")
    Call<AnimeEpisodeResponse> getEpisodesAll(
        @Query("id") String id,
        @Query("provider") String provider
    );

    @GET("stream/episodes/servers")
    Call<AnimeServerResponse> getServers(
        @Query("id") String id,
        @Query("provider") String provider
    );

    @GET("stream/source")
    Call<AnimeStreamResponse> getStreamSource(
        @Query("episodeData") String episodeData,
        @Query("provider") String provider,
        @Query("server") String server
    );

    @GET("stream/source")
    Call<AnimeStreamResponse> getStreamSourceDefault(
        @Query("episodeData") String episodeData,
        @Query("provider") String provider
    );
}
