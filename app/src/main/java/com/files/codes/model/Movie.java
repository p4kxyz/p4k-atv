package com.files.codes.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.JsonAdapter;
import com.files.codes.utils.GenreDeserializer;

import java.io.Serializable;

@Entity(tableName = "movie_table")
public class Movie implements Serializable {
    @PrimaryKey()
    @NonNull
    private int id;

    @ColumnInfo(name = "videos_id")
    @SerializedName("videos_id")
    @Expose
    private String videosId;

    @ColumnInfo(name = "title")
    @SerializedName("title")
    @Expose
    private String title;

    @ColumnInfo(name = "description")
    @SerializedName("description")
    @Expose
    private String description;

    @ColumnInfo(name = "slug")
    @SerializedName("slug")
    @Expose
    private String slug;

    @ColumnInfo(name = "release")
    @SerializedName("release")
    @Expose
    private String release;

    @ColumnInfo(name = "is_tvseries")
    @SerializedName("is_tvseries")
    @Expose
    private String isTvseries;

    @ColumnInfo(name = "runtime")
    @SerializedName("runtime")
    @Expose
    private String runtime;

    @ColumnInfo(name = "video_quality")
    @SerializedName("video_quality")
    @Expose
    private String videoQuality;

    @ColumnInfo(name = "thumbnail_url")
    @SerializedName("thumbnail_url")
    @Expose
    private String thumbnailUrl;

    @ColumnInfo(name = "poster_url")
    @SerializedName("poster_url")
    @Expose
    private String posterUrl;

    @ColumnInfo(name = "is_paid")
    @SerializedName("is_paid")
    @Expose
    private String isPaid;

    @ColumnInfo(name = "imdb_rating")
    @SerializedName("imdb_rating")
    @Expose
    private String imdbRating;

    @ColumnInfo(name = "genre")
    @SerializedName("genre")
    @Expose
    @JsonAdapter(GenreDeserializer.class)
    private String genre; // Genre string (comma-separated)

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getVideosId() {
        return videosId;
    }

    public void setVideosId(String videosId) {
        this.videosId = videosId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public String getIsTvseries() {
        return isTvseries;
    }

    public void setIsTvseries(String isTvseries) {
        this.isTvseries = isTvseries;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public String getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(String videoQuality) {
        this.videoQuality = videoQuality;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getIsPaid() {
        return isPaid;
    }

    public void setIsPaid(String isPaid) {
        this.isPaid = isPaid;
    }

    public String getImdbRating() {
        return imdbRating;
    }

    public void setImdbRating(String imdbRating) {
        this.imdbRating = imdbRating;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }
}
