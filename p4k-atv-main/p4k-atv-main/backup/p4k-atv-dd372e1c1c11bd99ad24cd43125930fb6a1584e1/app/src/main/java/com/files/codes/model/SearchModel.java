package com.files.codes.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class SearchModel implements Serializable {

    @SerializedName("tvseries")
    @Expose
    private List<Movie> tvseries = null;

    @SerializedName("movie")
    @Expose
    private List<Movie> movie = null;

    @SerializedName("tv_channels")
    @Expose
    private List<TvModel> tvChannels = null;

    public List<Movie> getMovie() {
        return movie;
    }

    public List<Movie> getTvseries() {
        return tvseries;
    }

    public void setTvseries(List<Movie> tvseries) {
        this.tvseries = tvseries;
    }

    public void setMovie(List<Movie> movie) {
        this.movie = movie;
    }

    public List<TvModel> getTvChannels() {
        return tvChannels;
    }

    public void setTvChannels(List<TvModel> tvChannels) {
        this.tvChannels = tvChannels;
    }
}