package com.files.codes.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FavoriteResponse {
    @SerializedName("data")
    @Expose
    private List<Movie> data;

    @SerializedName("movies")
    @Expose
    private List<Movie> movies;

    public List<Movie> getData() {
        // Try data first, then movies
        if (data != null && !data.isEmpty()) {
            return data;
        }
        return movies;
    }

    public void setData(List<Movie> data) {
        this.data = data;
    }

    public List<Movie> getMovies() {
        return movies;
    }

    public void setMovies(List<Movie> movies) {
        this.movies = movies;
    }
}
