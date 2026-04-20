package com.files.codes.model.phim4k;

import com.google.gson.annotations.SerializedName;

public class Phim4kDetailResponse {
    @SerializedName("status")
    private String status;
    
    @SerializedName("movie")
    private Phim4kMovie movie;

    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Phim4kMovie getMovie() { return movie; }
    public void setMovie(Phim4kMovie movie) { this.movie = movie; }
}